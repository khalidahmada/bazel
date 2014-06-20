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
import com.google.devtools.build.lib.actions.Artifact;
import com.google.devtools.build.lib.actions.cache.DigestUtils;
import com.google.devtools.build.lib.vfs.FileStatus;
import com.google.devtools.build.lib.vfs.Path;

import java.io.IOException;
import java.util.Arrays;

import javax.annotation.Nullable;

/**
 * Stores the data of an artifact corresponding to a file. This file may be an ordinary file, in
 * which case we would expect to see a digest and size; a directory, in which case we would expect
 * to see an mtime; or an empty file, where we would expect to see a size (=0), mtime, and digest
 */
public class FileArtifactNode extends ArtifactNode {
  /** Data for Middleman artifacts that did not have data specified. */
  static final FileArtifactNode DEFAULT_MIDDLEMAN = new FileArtifactNode(null, 0, 0);

  @Nullable private final byte[] digest;
  private final long mtime;
  private final long size;

  private FileArtifactNode(byte[] digest, long size) {
    Preconditions.checkState(size >= 0, "size must be non-negative: %s %s", digest, size);
    this.digest = Preconditions.checkNotNull(digest, size);
    this.size = size;
    this.mtime = -1;
  }

  // Only used by empty files (non-null digest) and directories (null digest).
  private FileArtifactNode(byte[] digest, long mtime, long size) {
    Preconditions.checkState(mtime >= 0, "mtime must be non-negative: %s %s", mtime, size);
    Preconditions.checkState(size == 0, "size must be zero: %s %s", mtime, size);
    this.digest = digest;
    this.size = size;
    this.mtime = mtime;
  }

  static FileArtifactNode create(Artifact artifact) throws IOException {
    Path path = artifact.getPath();
    FileStatus stat = path.stat();
    boolean isFile = stat.isFile();
    return create(artifact, isFile, isFile ? stat.getSize() : 0, null);
  }

  static FileArtifactNode create(Artifact artifact, FileNode fileNode) throws IOException {
    boolean isFile = fileNode.isFile();
    return create(artifact, isFile, isFile ? fileNode.getSize() : 0,
        isFile ? fileNode.getDigest() : null);
  }

  static FileArtifactNode create(Artifact artifact, boolean isFile, long size,
      @Nullable byte[] digest) throws IOException {
    if (isFile && digest == null) {
      digest = DigestUtils.getDigestOrFail(artifact.getPath(), size);
    }
    if (!DigestUtils.useFileDigest(artifact, isFile, size)) {
      // In this case, we need to store the mtime because the action cache uses mtime to determine
      // if this artifact has changed. This is currently true for empty files and directories. We
      // do not optimize for this code path (by storing the mtime in a FileNode) because we do not
      // like it and may remove this special-casing for empty files in the future. We want this code
      // path to go away somehow too for directories (maybe by implementing FileSet
      // in Skyframe)
      return new FileArtifactNode(digest, artifact.getPath().getLastModifiedTime(), size);
    }
    Preconditions.checkState(digest != null, artifact);
    return new FileArtifactNode(digest, size);
  }

  static FileArtifactNode createMiddleman(byte[] digest) {
    Preconditions.checkNotNull(digest);
    // The Middleman artifact nodes have size 1 because we want their digests to be used. This hack
    // can be removed once empty files are digested.
    return new FileArtifactNode(digest, /*size=*/1);
  }

  @Nullable
  byte[] getDigest() {
    return digest;
  }

  /** Gets the size of the file. Directories have size 0. */
  long getSize() {
    return size;
  }

  /**
   * Gets last modified time of file. Should only be called if {@link DigestUtils#useFileDigest} was
   * false for this artifact -- namely, either it is a directory or an empty file. Note that since
   * we store directory sizes as 0, all files for which this method can be called have size 0.
   */
  long getModifiedTime() {
    Preconditions.checkState(size == 0, "%s %s %s", digest, mtime, size);
    return mtime;
  }

  @Override
  public int hashCode() {
    // Hash digest by content, not reference. Note that digest is the only array in this array.
    return Arrays.deepHashCode(new Object[] {size, mtime, digest});
  }

  /**
   * Two FileArtifactNodes will only compare equal if they have the same content. This differs
   * from the {@code Metadata#equivalence} method, which allows for comparison using mtime if
   * one object does not have a digest available.
   */
  @Override
  public boolean equals(Object other) {
    if (this == other) {
      return true;
    }
    if (!(other instanceof FileArtifactNode)) {
      return false;
    }
    FileArtifactNode that = (FileArtifactNode) other;
    return this.mtime == that.mtime && this.size == that.size
        && Arrays.equals(this.digest, that.digest);
  }
}
