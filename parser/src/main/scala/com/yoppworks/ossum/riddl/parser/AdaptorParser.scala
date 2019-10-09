package com.yoppworks.ossum.riddl.parser

import com.yoppworks.ossum.riddl.parser.AST.AdaptorDef
import fastparse._
import ScalaWhitespace._

/** Parser rules for Adaptors */
trait AdaptorParser extends CommonParser {

  def adaptorDef[_: P]: P[AdaptorDef] = {
    P(
      location ~ "adaptor" ~/ identifier ~ "for" ~/ domainRef.? ~/ contextRef ~
        addendum
    ).map { tpl =>
      (AdaptorDef.apply _).tupled(tpl)
    }
  }

}
