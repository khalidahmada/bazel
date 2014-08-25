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

package com.google.devtools.build.lib.view.config;

import com.google.devtools.build.lib.syntax.Label;
import com.google.devtools.build.lib.view.TransitiveInfoProvider;

/**
 * A "configuration target" that asserts whether or not it matches the
 * configuration it's bound to.
 *
 * <p>This can be used, e.g., to declare a BUILD target that defines the
 * conditions which trigger a configurable attribute branch. In general,
 * this can be used to trigger for any user-configurable build behavior.
 */
public interface ConfigMatchingProvider extends TransitiveInfoProvider {

  /**
   * The target's label.
   */
  Label label();

  /**
   * Whether or not the configuration criteria defined by this target match
   * its actual configuration.
   */
  boolean matches();
}