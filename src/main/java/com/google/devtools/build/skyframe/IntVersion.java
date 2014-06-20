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

/**
 * Versioning scheme based on simple integers.
 */
final class IntVersion implements Version {

  private final int val;

  public IntVersion(int val) {
    this.val = val;
  }

  @Override
  public Relation relate(Version other) {
    if (other instanceof IntVersion) {
      int otherVal = ((IntVersion) other).val;
      if (this.val == otherVal) {
        return Relation.EQUAL;
      } else if (this.val < otherVal) {
        return Relation.ANCESTOR;
      } else {
        return Relation.DESCENDANT;
      }
    }
    return Relation.NONE;
  }

  public int getVal() {
    return val;
  }

  @Override
  public int hashCode() {
    return val;
  }

  @Override
  public boolean equals(Object obj) {
    if (obj instanceof IntVersion) {
      IntVersion other = (IntVersion) obj;
      return other.val == val;
    }
    return false;
  }
}
