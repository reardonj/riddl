package com.yoppworks.ossum.riddl.parser

import AST._
import fastparse._
import ScalaWhitespace._

/** Parsing rules for domains. */
trait DomainParser
    extends ChannelParser
    with ContextParser
    with InteractionParser
    with MessageParser
    with TypesParser {

  def domainDefinitions[_: P]: P[Definition] = {
    P(typeDef | anyMessageDef | channelDef | interactionDef | contextDef)
  }

  def domainDef[_: P]: P[DomainDef] = {
    P(
      location ~ "domain" ~/ identifier ~
        ("is" ~ "subdomain" ~ "of" ~/ identifier).? ~ "{" ~/
        typeDef.rep(0) ~
        channelDef.rep(0) ~
        interactionDef.rep(0) ~
        contextDef.rep(0) ~
        "}" ~ addendum
    ).map { tpl =>
      (DomainDef.apply _).tupled(tpl)
    }
  }

  def topLevelDomains[_: P]: P[Seq[DomainDef]] = {
    P(Start ~ P(domainDef).rep(0) ~ End)
  }
}
