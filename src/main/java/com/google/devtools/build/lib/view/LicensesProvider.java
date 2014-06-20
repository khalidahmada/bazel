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

package com.google.devtools.build.lib.view;

import com.google.common.base.Preconditions;
import com.google.devtools.build.lib.collect.nestedset.NestedSet;
import com.google.devtools.build.lib.packages.License;
import com.google.devtools.build.lib.syntax.Label;
import com.google.devtools.build.lib.view.config.BuildConfiguration;

import java.util.Objects;

/**
 * A {@link ConfiguredTarget} that has licensed targets in its transitive closure.
 */
public interface LicensesProvider extends TransitiveInfoProvider {

  /**
   * The set of label - license associations in the transitive closure.
   *
   * <p>Always returns an empty set if {@link BuildConfiguration#checkLicenses()} is false.
   */
  NestedSet<TargetLicense> getTransitiveLicenses();

  /**
   * License association for a particular target.
   */
  public static final class TargetLicense {

    private final Label label;
    private final License license;

    public TargetLicense(Label label, License license) {
      Preconditions.checkNotNull(label);
      Preconditions.checkNotNull(license);
      this.label = label;
      this.license = license;
    }

    /**
     * Returns the label of the associated target.
     */
    public Label getLabel() {
      return label;
    }

    /**
     * Returns the license for the target.
     */
    public License getLicense() {
      return license;
    }

    @Override
    public int hashCode() {
      return Objects.hash(label, license);
    }

    @Override
    public boolean equals(Object obj) {
      if (this == obj) {
        return true;
      }
      if (!(obj instanceof TargetLicense)) {
        return false;
      }
      TargetLicense other = (TargetLicense) obj;
      return label.equals(other.label) && license.equals(other.license);
    }

    @Override
    public String toString() {
      return label + " => " + license;
    }
  }
}
