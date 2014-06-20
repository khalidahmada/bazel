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

import com.google.common.base.Preconditions;

import javax.annotation.Nullable;

/**
 * A work queue -- takes {@link Runnable}s and runs them when requested.
 */
interface Scheduler {
  /**
   * Schedules a new action to be eventually done.
   */
  void schedule(Runnable action);

  /**
   * Runs the actions that have been scheduled. These actions can in turn schedule new actions,
   * which will be run as well.
   *
   * @throw SchedulerException wrapping a scheduled action's exception.
   */
  void run() throws SchedulerException;

  /**
   * Wrapper exception that {@link Runnable}s can throw, to be caught and handled
   * by callers of {@link #run}.
   */
  static class SchedulerException extends RuntimeException {
    private final NodeKey failedNode;
    private final ErrorInfo errorInfo;

    private SchedulerException(@Nullable Throwable cause, @Nullable ErrorInfo errorInfo,
        NodeKey failedNode) {
      super(errorInfo != null ? errorInfo.getException() : cause);
      this.errorInfo = errorInfo;
      this.failedNode = Preconditions.checkNotNull(failedNode, errorInfo);
    }

    /**
     * Returns a SchedulerException wrapping an expected error, e.g. an error describing an expected
     * build failure when trying to evaluate the given node, that should cause Skyframe to produce
     * useful error information to the user.
     */
    static SchedulerException ofError(ErrorInfo errorInfo, NodeKey failedNode) {
      Preconditions.checkNotNull(errorInfo);
      return new SchedulerException(errorInfo.getException(), errorInfo, failedNode);
    }

    /**
     * Returns a SchedulerException wrapping an InterruptedException, e.g. if the user interrupts
     * the build, that should cause Skyframe to exit as soon as possible.
     */
    static SchedulerException ofInterruption(InterruptedException cause, NodeKey failedNode) {
      return new SchedulerException(cause, null, failedNode);
    }

    NodeKey getFailedNode() {
      return failedNode;
    }

    @Nullable ErrorInfo getErrorInfo() {
      return errorInfo;
    }
  }
}
