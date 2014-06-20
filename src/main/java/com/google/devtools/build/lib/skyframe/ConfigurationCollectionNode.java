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

import com.google.common.base.Preconditions;
import com.google.devtools.build.lib.concurrent.ThreadSafety.Immutable;
import com.google.devtools.build.lib.concurrent.ThreadSafety.ThreadSafe;
import com.google.devtools.build.lib.view.config.BuildConfigurationCollection;
import com.google.devtools.build.skyframe.Node;
import com.google.devtools.build.skyframe.NodeKey;

/**
 * A Skyframe node representing a build configuration collection.
 */
@Immutable
@ThreadSafe
public class ConfigurationCollectionNode implements Node {
  /**
   * Key for ConfigurationCollectionNode.
   */
  public static final NodeKey CONFIGURATION_KEY =
      new NodeKey(NodeTypes.CONFIGURATION_COLLECTION, "");
  private final BuildConfigurationCollection configurationCollection;

  ConfigurationCollectionNode(BuildConfigurationCollection configurationCollection) {
    this.configurationCollection = Preconditions.checkNotNull(configurationCollection);
  }

  public BuildConfigurationCollection getConfigurationCollection() {
    return configurationCollection;
  }
}
