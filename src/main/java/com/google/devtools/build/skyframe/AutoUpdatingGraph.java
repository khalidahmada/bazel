// Copyright 2014 Google Inc. All rights reserved.
//
// Licensed under the Apache License, Version 2.0 (the "License");
// you may not use this file except in compliance with the License.
// You may obtain a copy of the License at
//
//    http://www.apache.org/licenses/LICENSE-2.0
//
// Unless required by applicable law or agreed to in writing, software
// distributed under the License is distributed on an "AS IS" BASIS,
// WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
// See the License for the specific language governing permissions and
// limitations under the License.
package com.google.devtools.build.skyframe;

import com.google.common.annotations.VisibleForTesting;
import com.google.common.base.Predicate;
import com.google.devtools.build.lib.concurrent.ThreadSafety.ThreadHostile;
import com.google.devtools.build.lib.concurrent.ThreadSafety.ThreadSafe;
import com.google.devtools.build.lib.events.ErrorEventListener;

import java.io.PrintStream;
import java.util.Map;

import javax.annotation.Nullable;

/**
 * A graph, defined by a set of functions that can construct nodes from node keys.
 *
 * <p>The node constructor functions ({@link NodeBuilder}s) can declare dependencies on prerequisite
 * {@link Node}s. The {@link AutoUpdatingGraph} implementation makes sure that those are created
 * beforehand.
 *
 * <p>The graph caches previously computed node values. Arbitrary nodes can be invalidated between
 * calls to {@link #update}; they will be recreated the next time they are requested.
 */
public interface AutoUpdatingGraph {

  // TODO(bazel-team): Figure out how to handle node builders that block internally. Blocking
  // operations may need to be handled in another (bigger?) thread pool. Also, we should detect
  // the number of cores and use that as the thread-pool size for CPU-bound operations.
  // I just bumped this to 200 to get reasonable execution phase performance; that may cause
  // significant overhead for CPU-bound processes (i.e. analysis). [skyframe-analysis]
  public static int DEFAULT_THREAD_COUNT = 200;

  /**
   * Invalidates the cached values of the given nodes.
   *
   * <p>If a future call to {@link #update} requests a node that transitively depends on any of
   * these (or is one of these), they will be re-computed.
   */
  void invalidate(Iterable<NodeKey> diff);

  /**
   * Invalidates the cached values of any nodes in error.
   *
   * <p>If a future call to {@link #update} requests a node that transitively depends on any node
   * that was in an error state (or is one of these), they will be re-computed.
   */
  void invalidateErrors();

  /**
   * Ensures that after the next completed {@link #update} call the current values of any node
   * matching this predicate (and all nodes that transitively depend on them) will be removed from
   * the node cache. All nodes that were already marked dirty in the graph will also be deleted.
   *
   * <p>If a later call to {@link #update} requests some of the deleted nodes, those nodes will be
   * recomputed and the new values stored in the cache again.
   */
  void delete(Predicate<NodeKey> pred);

  /**
   * Injects the given nodes into the graph before the next {@link #update}.
   *
   * <p>If there are existing nodes with the same keys, they will be overwritten with the new values
   * and their transitive closure will be invalidated.
   *
   * <p>Overwriting nodes which have known dependencies is not allowed in order to prevent
   * conflation of injected nodes and derived nodes.
   */
  void inject(Map<NodeKey, ? extends Node> nodes);

  /**
   * Computes the transitive closure of a given set of nodes. See
   * {@link EagerInvalidator#invalidate}.
   */
  <T extends Node> UpdateResult<T> update(Iterable<NodeKey> roots, boolean keepGoing,
      int numThreads, ErrorEventListener reporter) throws InterruptedException;

  /**
   * Returns the nodes in the graph.
   *
   * <p>The returned map may be a live view of the graph.
   */
  Map<NodeKey, Node> getNodes();


  /**
   * Returns the done (without error) nodes in the graph.
   *
   * <p>The returned map may be a live view of the graph.
   */
  Map<NodeKey, Node> getDoneNodes();

  /**
   * Returns a node if and only if an earlier call to {@link #update} created it; null otherwise.
   *
   * <p>This method should only be used by tests that need to verify the presence of a node in the
   * graph after an {@link #update} call.
   */
  @VisibleForTesting
  @Nullable
  Node getExistingNodeForTesting(NodeKey key);

  /**
   * Returns an error if and only if an earlier call to {@link #update} created it; null otherwise.
   *
   * <p>This method should only be used by tests that need to verify the presence of an error in the
   * graph after an {@link #update} call.
   */
  @VisibleForTesting
  @Nullable
  ErrorInfo getExistingErrorForTesting(NodeKey key);

  @VisibleForTesting
  public void setGraphForTesting(InMemoryGraph graph);

  /**
   * Write the graph to the output stream. Not necessarily thread-safe. Use only for debugging
   * purposes.
   */
  @ThreadHostile
  void dump(PrintStream out);

  /**
   * Receiver to inform callers which nodes have been invalidated. Nodes may be invalidated and then
   * re-validated if they have been found not to be changed.
   */
  public interface NodeProgressReceiver {
    /**
     * New state of the node entry after evaluation.
     */
    enum EvaluationState {
      /** The node was successfully re-evaluated. */
      BUILT,
      /** The node is clean or re-validated. */
      CLEAN,
    }

    /**
     * New state of the node entry after invalidation.
     */
    enum InvalidationState {
      /** The node is dirty, although it might get re-validated again. */
      DIRTY,
      /** The node is dirty and got deleted, cannot get re-validated again. */
      DELETED,
    }

    /**
     * Notifies that {@code node} has been invalidated.
     *
     * <p>{@code state} indicates the new state of the node.
     *
     * <p>This method is not called on invalidation of nodes which do not have a value (usually
     * because they are in error).
     *
     * <p>May be called concurrently from multiple threads, possibly with the same {@code node}
     * object.
     */
    @ThreadSafe
    void invalidated(Node node, InvalidationState state);

    /**
     * Notifies that {@code nodeKey} is about to get queued for evaluation.
     *
     * <p>Note that we don't guarantee that it actually got enqueued or will, only that if
     * everything "goes well" (e.g. no interrupts happen) it will.
     *
     * <p>This guarantee is intentionally vague to encourage writing robust implementations.
     */
    @ThreadSafe
    void enqueueing(NodeKey nodeKey);

    /**
     * Notifies that {@code node} has been evaluated.
     *
     * <p>{@code state} indicates the new state of the node.
     *
     * <p>This method is not called if the node builder threw an error when building this node.
     */
    @ThreadSafe
    void evaluated(NodeKey nodeKey, Node node, EvaluationState state);
  }

  /**
   * Keeps track of already-emitted events. Users of the graph should instantiate an
   * {@code EmittedEventState} first and pass it to the graph during creation. This allows them to
   * determine whether or not to replay events.
   */
  public static class EmittedEventState extends NestedSetVisitor.VisitedState<TaggedEvents> {}
}
