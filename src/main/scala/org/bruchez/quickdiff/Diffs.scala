package org.bruchez.quickdiff

import java.io.File

/** This case class represents the sets of differences between two directories */
case class Diffs(
    differentFiles: Seq[File] = Nil,
    srcOutOfDateFiles: Seq[File] = Nil,
    srcMissingFiles: Seq[File] = Nil,
    srcMissingDirectories: Seq[File] = Nil,
    dstOutOfDateFiles: Seq[File] = Nil,
    dstMissingFiles: Seq[File] = Nil,
    dstMissingDirectories: Seq[File] = Nil) {
  /** @return true if and only if no difference has been detected between two directories */
  def noDifference: Boolean =
    differentFiles.isEmpty &&
    srcOutOfDateFiles.isEmpty &&
    srcMissingFiles.isEmpty &&
    srcMissingDirectories.isEmpty &&
    dstOutOfDateFiles.isEmpty &&
    dstMissingFiles.isEmpty &&
    dstMissingDirectories.isEmpty

  /** @return the differences between two directories as a sequence of human-readable strings */
  def toStrings: Seq[String] = {
    import Diffs.filesToStrings
    filesToStrings("Different files", differentFiles) ++
    filesToStrings("Out-of-date files in source path", srcOutOfDateFiles) ++
    filesToStrings("Files missing from source path", srcMissingFiles) ++
    filesToStrings("Directories missing from source path", srcMissingDirectories) ++
    filesToStrings("Out-of-date files in destination path", dstOutOfDateFiles) ++
    filesToStrings("Files missing from destination path", dstMissingFiles) ++
    filesToStrings("Directories missing from destination path", dstMissingDirectories)
  }

  /** @return the concatenation of two sets of differences */
  def ++(otherDiffs: Diffs): Diffs = Diffs(
    differentFiles = this.differentFiles ++ otherDiffs.differentFiles,
    srcOutOfDateFiles = this.srcOutOfDateFiles ++ otherDiffs.srcOutOfDateFiles,
    srcMissingFiles = this.srcMissingFiles ++ otherDiffs.srcMissingFiles,
    srcMissingDirectories = this.srcMissingDirectories ++ otherDiffs.srcMissingDirectories,
    dstOutOfDateFiles = this.dstOutOfDateFiles ++ otherDiffs.dstOutOfDateFiles,
    dstMissingFiles = this.dstMissingFiles ++ otherDiffs.dstMissingFiles,
    dstMissingDirectories = this.dstMissingDirectories ++ otherDiffs.dstMissingDirectories
  )
}

object Diffs {
  private def filesToStrings(header: String, files: Seq[File]): Seq[String] =
    if (files.isEmpty) Nil else Seq(header+":") ++ files.map(" - "+_.getAbsolutePath)
}
