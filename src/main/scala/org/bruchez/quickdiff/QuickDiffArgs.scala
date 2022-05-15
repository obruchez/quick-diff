package org.bruchez.quickdiff

/** This case class represents the general parameters of a quick diff */
case class QuickDiffArgs(
    srcPath: Option[String] = None,
    dstPath: Option[String] = None,
    checkDates: Boolean = false,
    fullDiff: Boolean = false
) {
  def toStrings: Seq[String] = Seq(
    "Source path:      " + srcPath.getOrElse("-"),
    "Destination path: " + dstPath.getOrElse("-"),
    "Check dates:      " + (if (checkDates) "yes" else "no"),
    "Full diff:        " + (if (fullDiff) "yes" else "no")
  )
}

object QuickDiffArgs {
  val usage = "Usage: QuickDiff [--check-dates] [--full-diff] src-path dst-path"

  /** Parse a list of arguments (typically passed from a command line) */
  def parseArgs(
      argList: List[String],
      quickDiffArgs: QuickDiffArgs = QuickDiffArgs()
  ): Either[String, QuickDiffArgs] = {
    argList match {
      case Nil                     => Right(quickDiffArgs)
      case "--check-dates" :: tail => parseArgs(tail, quickDiffArgs.copy(checkDates = true))
      case "--full-diff" :: tail   => parseArgs(tail, quickDiffArgs.copy(fullDiff = true))
      case srcArg :: dstArg :: tail =>
        parseArgs(tail, quickDiffArgs.copy(srcPath = Some(srcArg), dstPath = Some(dstArg)))
      case arg :: tail => Left("Unexpected argument: " + arg)
    }
  }
}
