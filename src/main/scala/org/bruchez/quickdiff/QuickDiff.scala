package org.bruchez.quickdiff

import java.io.File

// @todo implement "full diff" mode

/** This case class represents the general context in which two directories can be compared (using the diffs method) */
case class QuickDiff(checkDates: Boolean = false, fullDiff: Boolean = false) {
  /** @return the differences between two directories */
  def diffs(srcDir: File, dstDir: File): Either[String, Diffs] = {
    val (srcFiles, srcDirectories) = srcDir.listFiles().toSeq.partition(_.isFile)
    val (dstFiles, dstDirectories) = dstDir.listFiles().toSeq.partition(_.isFile)

    case class JavaFilePair(src: File, dst: File)

    /** @return Java files that are present in both the source and destination sequences (name-based comparison) */
    def commonJavaFilePairs(srcJavaFiles: Seq[File], dstJavaFiles: Seq[File]): Seq[JavaFilePair] = for {
      srcJavaFile <- srcJavaFiles
      dstJavaFile <- dstJavaFiles.find(_.getName == srcJavaFile.getName)
    } yield JavaFilePair(srcJavaFile, dstJavaFile)

    val commonFiles = commonJavaFilePairs(srcFiles, dstFiles)
    val commonDirectories = commonJavaFilePairs(srcDirectories, dstDirectories)

    /** Combine sub-diffs (i.e. diffs from sub-directories), stop at first error */
    @scala.annotation.tailrec
    def combineSubDiffs(
       subDirectoryPairs: List[JavaFilePair],
       errorOrCurrentDiffs: Either[String, Diffs] = Right(Diffs())): Either[String, Diffs] =
      errorOrCurrentDiffs match {
        // Stop at first error
        case Left(error) => Left(error)
        // No error => go on with next pair of sub-directories, if any
        case Right(currentDiffs) => subDirectoryPairs match {
          // No more sub-directories
          case Nil => Right(currentDiffs)
          // Sub-directories found => compute diffs and process next sub-directories (recursion)
          case subDirectoryPair :: otherSubDirectoryPairs => combineSubDiffs(
            otherSubDirectoryPairs,
            diffs(
              new File(srcDir, subDirectoryPair.src.getName),
              new File(dstDir, subDirectoryPair.dst.getName)).right.map(_ ++ currentDiffs)
          )
        } 
      }

    // Compute diffs from matching sub-directories
    combineSubDiffs(commonDirectories.toList).right map { subDirectoriesDiffs =>
      // Check the size of common files
      val (sameSizeFilePairs, differentSizeFilePairs) = commonFiles partition { filePair =>
        filePair.src.length == filePair.dst.length
      }

      case class JavaFileListPair(src: List[File] = Nil, dst: List[File] = Nil)

      // If "check date" mode enabled, check the date of the same-sized files
      val outOfDateFiles =
        if (checkDates) {
          sameSizeFilePairs.foldLeft(JavaFileListPair()) {
            (outOfDateFiles, filePair) => {
              val srcLastModified = filePair.src.lastModified
              val dstLastModified = filePair.dst.lastModified

              JavaFileListPair(
                if (srcLastModified < dstLastModified) filePair.src :: outOfDateFiles.src else outOfDateFiles.src,
                if (dstLastModified < srcLastModified) filePair.dst :: outOfDateFiles.dst else outOfDateFiles.dst
              )
            }
          }
        } else JavaFileListPair()

      /** @return Java files that should be present in the base parent directory but that are missing */
      def missingJavaFiles(
          baseParentDirectory: File,
          referenceJavaFiles: Seq[File],
          commonJavaFilePairs: Seq[JavaFilePair],
          commonJavaFilePairToReferenceJavaFile: JavaFilePair => File): Seq[File] =
        referenceJavaFiles filterNot { referenceJavaFile =>
          commonJavaFilePairs exists { commonJavaFilePair =>
            commonJavaFilePairToReferenceJavaFile(commonJavaFilePair).getName == referenceJavaFile.getName
          }
        } map { referenceJavaFile =>
          new File(baseParentDirectory, referenceJavaFile.getName)
        }

      // Compute diffs from the current directories
      val currentLevelDiffs = Diffs(
        differentFiles = differentSizeFilePairs.map(_.src),
        srcOutOfDateFiles = outOfDateFiles.src,
        srcMissingFiles = missingJavaFiles(srcDir, dstFiles, commonFiles, _.dst),
        srcMissingDirectories = missingJavaFiles(srcDir, dstDirectories, commonDirectories, _.dst),
        dstOutOfDateFiles = outOfDateFiles.dst,
        dstMissingFiles = missingJavaFiles(dstDir, srcFiles, commonFiles, _.src),
        dstMissingDirectories = missingJavaFiles(dstDir, srcDirectories, commonDirectories, _.src))

      // And combine them
      subDirectoriesDiffs ++ currentLevelDiffs
    }
  }
}

object QuickDiff {
  /** Return values */
  private val SysExitValNoDifference = 0
  private val SysExitValDifferences = 1
  private val SysExitValError = 2

  /** Parse arguments, compute differences, and print them */
  def main(args: Array[String]) {
    QuickDiffArgs.parseArgs(args.toList).fold(
      error => {
        Console.err.println(error)
        Console.err.println(QuickDiffArgs.usage)
        sys.exit(SysExitValError)
      },
      quickDiffArgs => {
        // Dump arguments
        quickDiffArgs.toStrings.foreach(println)
        println()

        // Validate source/destination paths and compute the differences between the two paths
        val diffsEither = for {
          srcFile <- directoryPathOptionAsFile(quickDiffArgs.srcPath, "Missing source path").right
          dstFile <- directoryPathOptionAsFile(quickDiffArgs.dstPath, "Missing destination path").right
          diffs <- QuickDiff(quickDiffArgs.checkDates, quickDiffArgs.fullDiff).diffs(srcFile, dstFile).right
        } yield diffs

        diffsEither.fold(
          error => {
            Console.err.println(error)
            Console.err.println(QuickDiffArgs.usage)
            sys.exit(SysExitValError)
          },
          diffs => {
            if (diffs.noDifference) println("No difference was detected") else diffs.toStrings.foreach(println)
            sys.exit(if (diffs.noDifference) SysExitValNoDifference else SysExitValDifferences)
          })
      }
    )
  }

  /** Validate a directory path and return a File instance in case of success */
  private def directoryPathOptionAsFile(
      pathOption: Option[String],
      emptyPathError: String): Either[String, File] = {
    pathOption map { path =>
      val directoryAsFile = new File(path)
      if (directoryAsFile.isDirectory) Right(directoryAsFile) else Left(path+" is not a valid directory")
    } getOrElse {
      Left(emptyPathError)
    }
  }
}
