package com.yoppworks.ossum.riddl.language

import java.io.File
import java.nio.file.Path

import com.yoppworks.ossum.riddl.language.AST.Location
import fastparse.ParserInput
import fastparse.internal.Util

import scala.io.Source

/** Same as fastparse.IndexedParserInput but with Location support */
abstract class RiddlParserInput extends ParserInput {
  def origin: String
  def data: String
  def root: File

  override def apply(index: Int): Char = data.charAt(index)
  override def dropBuffer(index: Int): Unit = {}
  override def slice(from: Int, until: Int): String = data.slice(from, until)
  override def length: Int = data.length
  override def innerLength: Int = length
  override def isReachable(index: Int): Boolean = index < length

  def checkTraceable(): Unit = ()

  private[this] lazy val lineNumberLookup = Util.lineNumberLookup(data)

  private def lineOf(index: Int): Int = {
    lineNumberLookup.indexWhere(_ > index) match {
      case -1 => 0
      case n  => math.max(0, n - 1)
    }
  }

  def rangeOf(index: Int): (Int, Int) = {
    val line = lineOf(index)
    val start = lineNumberLookup(line)
    val end = lineNumberLookup(line + 1)
    start -> end
  }

  def rangeOf(loc: Location): (Int, Int) = {
    require(loc.line > 0)
    val start = lineNumberLookup(loc.line - 1)
    val end =
      if (lineNumberLookup.length == 1) { data.length }
      else { lineNumberLookup(loc.line) }
    start -> end
  }

  def location(index: Int, origin: String = ""): Location = {
    val line = lineOf(index)
    val col = index - lineNumberLookup(line)
    Location(line + 1, col + 1, origin)
  }

  def prettyIndex(index: Int): String = { location(index).toString }

  val nl: String = System.getProperty("line.separator")

  def annotateErrorLine(index: Location): String = {
    val (start, end) = rangeOf(index)
    val col = index.col - 1
    slice(start, end).stripTrailing() + nl + " ".repeat(col) + "^" + nl
  }
}

case class StringParserInput(
  data: String,
  origin: String = AST.defaultSourceName)
    extends RiddlParserInput {
  val root: File = new File(System.getProperty("user.dir"))
}

case class FileParserInput(file: File) extends RiddlParserInput {

  val data: String = {
    val source: Source = Source.fromFile(file)
    try { source.getLines().mkString("\n") }
    finally { source.close() }
  }
  val root: File = file.getParentFile
  def origin: String = file.getName
  def this(path: Path) = this(path.toFile)
}

case class SourceParserInput(source: Source, origin: String) extends RiddlParserInput {

  val data: String =
    try { source.mkString }
    finally { source.close() }
  val root: File = new File(System.getProperty("user.dir"))
}

object RiddlParserInput {

  implicit def apply(
    data: String
  ): RiddlParserInput = { StringParserInput(data) }

  implicit def apply(source: Source): RiddlParserInput = { SourceParserInput(source, source.descr) }
  implicit def apply(file: File): RiddlParserInput = { FileParserInput(file) }
}
