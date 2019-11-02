package com.yoppworks.ossum.riddl.language

import java.io.File

import com.yoppworks.ossum.riddl.language.AST.RootContainer
import fastparse._
import ScalaWhitespace._

/** Top level parsing rules */
class TopLevelParser(rpi: RiddlParserInput) extends DomainParser {
  stack.push(rpi)

  def fileRoot[_: P]: P[RootContainer] = {
    P(Start ~ P(domainDef).rep(0) ~ End).map(RootContainer(_))
  }
}

case class FileParser(topFile: File)
    extends TopLevelParser(RiddlParserInput(topFile))

case class StringParser(content: String)
    extends TopLevelParser(RiddlParserInput(content))

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

  def parse(
    input: String
  ): Either[Seq[ParserError], RootContainer] = {
    val sp = StringParserInput(input)
    val tlp = new TopLevelParser(sp)
    tlp.expect(tlp.fileRoot(_))
  }
}
