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

import com.google.devtools.build.lib.packages.NoSuchPackageException;
import com.google.devtools.build.lib.packages.NoSuchTargetException;
import com.google.devtools.build.lib.packages.Package;
import com.google.devtools.build.lib.packages.Target;
import com.google.devtools.build.lib.pkgcache.LoadedPackageProvider;
import com.google.devtools.build.lib.pkgcache.PackageProvider;
import com.google.devtools.build.lib.pkgcache.TargetProvider;
import com.google.devtools.build.lib.syntax.Label;
import com.google.devtools.build.lib.vfs.Path;

/**
 * An environment to support creating BuildConfiguration instances in a hermetic fashion; all
 * accesses to packages or the file system <b>must</b> go through this interface, so that they can
 * be recorded for correct caching.
 */
public interface ConfigurationEnvironment {

  /**
   * Returns a target for the given label, loading it if necessary, and throwing an exception if it
   * does not exist.
   *
   * @see TargetProvider#getTarget
   */
  Target getTarget(Label label) throws NoSuchPackageException, NoSuchTargetException;

  /** Returns a path for the given file within the given package. */
  Path getPath(Package pkg, String fileName);

  /**
   * An implementation backed by a {@link PackageProvider} instance.
   */
  public static final class TargetProviderEnvironment implements ConfigurationEnvironment {

    private final LoadedPackageProvider loadedPackageProvider;

    public TargetProviderEnvironment(LoadedPackageProvider loadedPackageProvider) {
      this.loadedPackageProvider = loadedPackageProvider;
    }

    @Override
    public Target getTarget(Label label) throws NoSuchPackageException, NoSuchTargetException {
      return loadedPackageProvider.getLoadedTarget(label);
    }

    @Override
    public Path getPath(Package pkg, String fileName) {
      return pkg.getPackageDirectory().getRelative(fileName);
    }
  }
}
