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
package com.google.devtools.build.lib.skyframe;

import com.google.devtools.build.lib.actions.Artifact;
import com.google.devtools.build.lib.actions.ArtifactOwner;
import com.google.devtools.build.lib.view.WorkspaceStatusAction;
import com.google.devtools.build.skyframe.NodeKey;
import com.google.devtools.build.skyframe.NodeType;

/**
 * Node that stores the workspace status artifacts and their generating action. There should be
 * only one of these nodes in the graph at any time.
 */
// TODO(bazel-team): This seems to be superfluous now, but it cannot be removed without making
// BuildVariableNode public instead of package-private
public class WorkspaceStatusNode extends ActionLookupNode {
  private final Artifact stableArtifact;
  private final Artifact volatileArtifact;

  // There should only ever be one BuildInfo node in the graph.
  public static final NodeKey NODE_KEY = new NodeKey(NodeTypes.BUILD_INFO, "BUILD_INFO");
  static final ArtifactOwner ARTIFACT_OWNER = new BuildInfoKey();

  public WorkspaceStatusNode(Artifact stableArtifact, Artifact volatileArtifact,
      WorkspaceStatusAction action) {
    super(action);
    this.stableArtifact = stableArtifact;
    this.volatileArtifact = volatileArtifact;
  }

  public Artifact getStableArtifact() {
    return stableArtifact;
  }

  public Artifact getVolatileArtifact() {
    return volatileArtifact;
  }

  private static class BuildInfoKey extends ActionLookupKey {
    @Override
    NodeType getType() {
      throw new UnsupportedOperationException();
    }

    @Override
    NodeKey getNodeKey() {
      return NODE_KEY;
    }
  }
}
