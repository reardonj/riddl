package com.yoppworks.ossum.riddl.language

import java.io.File
import java.nio.file.Path

import com.yoppworks.ossum.riddl.language.AST.RootContainer
import fastparse._
import ScalaWhitespace._
import com.yoppworks.ossum.riddl.language.Validation.ValidationMessage

/** Top level parsing rules */
class TopLevelParser(rpi: RiddlParserInput) extends DomainParser {
  stack.push(rpi)

  def fileRoot[_: P]: P[RootContainer] = { P(Start ~ P(domain).rep(0) ~ End).map(RootContainer(_)) }
}

case class FileParser(topFile: File) extends TopLevelParser(RiddlParserInput(topFile))

case class StringParser(content: String) extends TopLevelParser(RiddlParserInput(content))

object TopLevelParser {

  def parse(
    input: RiddlParserInput
  ): Either[Seq[ParserError], RootContainer] = {
    val tlp = new TopLevelParser(input)
    tlp.expect(tlp.fileRoot(_))
  }

  def parse(file: File): Either[Seq[ParserError], RootContainer] = {
    val fpi = FileParserInput(file)
    val tlp = new TopLevelParser(fpi)
    tlp.expect(tlp.fileRoot(_))
  }

  def parse(path: Path): Either[Seq[ParserError], RootContainer] = {
    val fpi = new FileParserInput(path)
    val tlp = new TopLevelParser(fpi)
    tlp.expect(tlp.fileRoot(_))
  }

  def parse(
    input: String,
    origin: String = AST.defaultSourceName
  ): Either[Seq[ParserError], RootContainer] = {
    val sp = StringParserInput(input, origin)
    val tlp = new TopLevelParser(sp)
    tlp.expect(tlp.fileRoot(_))
  }

}
