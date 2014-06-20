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

import com.google.devtools.build.lib.cmdline.LabelValidator;
import com.google.devtools.build.lib.concurrent.ThreadSafety.ThreadSafe;
import com.google.devtools.build.lib.events.ErrorEventListener;
import com.google.devtools.build.lib.packages.NoSuchPackageException;
import com.google.devtools.build.lib.packages.NoSuchTargetException;
import com.google.devtools.build.lib.packages.Package;
import com.google.devtools.build.lib.packages.Target;
import com.google.devtools.build.lib.pkgcache.PackageManager;
import com.google.devtools.build.lib.pkgcache.PathPackageLocator;
import com.google.devtools.build.lib.pkgcache.TargetPatternEvaluator;
import com.google.devtools.build.lib.pkgcache.TransitivePackageLoader;
import com.google.devtools.build.lib.skyframe.SkyframeExecutor.SkyframePackageLoader;
import com.google.devtools.build.lib.syntax.Label;
import com.google.devtools.build.lib.vfs.Path;
import com.google.devtools.build.lib.vfs.UnixGlob;
import com.google.devtools.build.skyframe.CyclesReporter;

import java.io.PrintStream;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;

/**
 * Skyframe-based package manager.
 *
 * <p>This is essentially a compatibility shim between the native Skyframe and non-Skyframe
 * parts of Blaze and should not be long-lived.
 */
class SkyframePackageManager implements PackageManager {

  private final SkyframePackageLoader packageLoader;
  private final SkyframeExecutor.SkyframeTransitivePackageLoader transitiveLoader;
  private final TargetPatternEvaluator patternEvaluator;
  private final AtomicReference<UnixGlob.FilesystemCalls> syscalls;
  private final AtomicReference<CyclesReporter> skyframeCyclesReporter;
  private final AtomicReference<PathPackageLocator> pkgLocator;
  private final AtomicInteger numPackagesLoaded;
  private final SkyframeExecutor skyframeExecutor;

  public SkyframePackageManager(SkyframePackageLoader packageLoader,
                                SkyframeExecutor.SkyframeTransitivePackageLoader transitiveLoader,
                                TargetPatternEvaluator patternEvaluator,
                                AtomicReference<UnixGlob.FilesystemCalls> syscalls,
                                AtomicReference<CyclesReporter> skyframeCyclesReporter,
                                AtomicReference<PathPackageLocator> pkgLocator,
                                AtomicInteger numPackagesLoaded,
                                SkyframeExecutor skyframeExecutor) {
    this.packageLoader = packageLoader;
    this.transitiveLoader = transitiveLoader;
    this.patternEvaluator = patternEvaluator;
    this.skyframeCyclesReporter = skyframeCyclesReporter;
    this.pkgLocator = pkgLocator;
    this.syscalls = syscalls;
    this.numPackagesLoaded = numPackagesLoaded;
    this.skyframeExecutor = skyframeExecutor;
  }

  @Override
  public Package getLoadedPackage(String packageName) throws NoSuchPackageException {
    return packageLoader.getLoadedPackage(packageName);
  }

  @ThreadSafe
  @Override
  public Package getPackage(ErrorEventListener listener, String packageName)
      throws NoSuchPackageException, InterruptedException {
    try {
      return packageLoader.getPackage(listener, packageName);
    } catch (NoSuchPackageException e) {
      if (e.getPackage() != null) {
        return e.getPackage();
      }
      throw e;
    }
  }

  @Override
  public Target getLoadedTarget(Label label) throws NoSuchPackageException, NoSuchTargetException {
    return getLoadedPackage(label.getPackageName()).getTarget(label.getName());
  }

  @Override
  public Target getTarget(ErrorEventListener listener, Label label)
      throws NoSuchPackageException, NoSuchTargetException, InterruptedException {
    return getPackage(listener, label.getPackageName()).getTarget(label.getName());
  }

  @Override
  public boolean isTargetCurrent(Target target) {
    Package pkg = target.getPackage();
    try {
      return getLoadedPackage(pkg.getName()) == pkg;
    } catch (NoSuchPackageException e) {
      return false;
    }
  }

  @Override
  public void partiallyClear() {
    packageLoader.partiallyClear();
  }

  @Override
  public PackageManagerStatistics getStatistics() {
    return new PackageManagerStatistics() {
      @Override
      public int getPackagesLoaded() {
        return numPackagesLoaded.get();
      }

      @Override
      public int getPackagesLookedUp() {
        return -1;
      }

      @Override
      public int getCacheSize() {
        return -1;
      }
    };
  }

  @Override
  public boolean loadedTargetMayHaveChanged(Label label) {
    // TODO(bazel-team): Be smarter here. In Skyframe, we have the means to declare explicit
    // dependencies on directories contained in filesets, so it is actually possible to do
    // correct incremental builds. [skyframe-analysis]
    return true;
  }

  @Override
  public boolean isPackage(String packageName) {
    return getBuildFileForPackage(packageName) != null;
  }

  @Override
  public void dump(PrintStream printStream) {
    skyframeExecutor.dumpPackages(printStream);
  }

  @ThreadSafe
  @Override
  public Path getBuildFileForPackage(String packageName) {
    // Note that this method needs to be thread-safe, as it is currently used concurrently by
    // legacy blaze code.
    if (packageLoader.isPackageDeleted(packageName)
        || LabelValidator.validatePackageName(packageName) != null) {
      return null;
    }
    // TODO(bazel-team): Use a PackageLookupNode here [skyframe-loading]
    // TODO(bazel-team): The implementation in PackageCache also checks for duplicate packages, see
    // BuildFileCache#getBuildFile [skyframe-loading]
    return pkgLocator.get().getPackageBuildFileNullable(packageName, syscalls);
  }

  @Override
  public PathPackageLocator getPackagePath() {
    return pkgLocator.get();
  }

  @Override
  public TransitivePackageLoader newTransitiveLoader() {
    return new SkyframeLabelVisitor(transitiveLoader, skyframeCyclesReporter);
  }

  @Override
  public TargetPatternEvaluator getTargetPatternEvaluator() {
    return patternEvaluator;
  }
}
