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

package com.google.devtools.build.lib.events;

import com.google.common.base.Preconditions;

/**
 * An ErrorEventListener which delegates to another ErrorEventListener.
 * Primarily useful as a base class for extending behavior.
 */
public class DelegatingErrorEventListener implements ErrorEventListener {
  protected final ErrorEventListener delegate;

  public DelegatingErrorEventListener(ErrorEventListener delegate) {
    super();
    this.delegate = Preconditions.checkNotNull(delegate);
  }

  @Override
  public void warn(Location location, String message) {
    delegate.warn(location, message);
  }

  @Override
  public void error(Location location, String message) {
    delegate.error(location, message);
  }

  @Override
  public void info(Location location, String message) {
    delegate.info(location, message);
  }

  @Override
  public void progress(Location location, String message) {
    delegate.progress(location, message);
  }

  @Override
  public void report(EventKind kind, Location location, String message) {
    delegate.report(kind, location, message);
  }

  @Override
  public boolean showOutput(String tag) {
    return delegate.showOutput(tag);
  }
}
