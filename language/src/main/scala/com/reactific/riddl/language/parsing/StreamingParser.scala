/*
 * Copyright 2019 Ossum, Inc.
 *
 * SPDX-License-Identifier: Apache-2.0
 */

package com.reactific.riddl.language.parsing

import com.reactific.riddl.language.AST.*
import com.reactific.riddl.language.ast.At
import fastparse.*
import fastparse.ScalaWhitespace.*
import Terminals.*

/** Unit Tests For StreamingParser */
private[parsing] trait StreamingParser
    extends ReferenceParser
    with HandlerParser {

  def inlet[u: P]: P[Inlet] = {
    P(
      location ~ Keywords.inlet ~ identifier ~ is ~
        typeRef ~/ briefly ~ description
    )./.map { tpl => (Inlet.apply _).tupled(tpl) }
  }

  def outlet[u: P]: P[Outlet] = {
    P(
      location ~ Keywords.outlet ~ identifier ~ is ~
        typeRef ~/ briefly ~ description
    )./.map { tpl => (Outlet.apply _).tupled(tpl) }
  }

  private def connectorOptions[X: P]: P[Seq[ConnectorOption]] = {
    options[X, ConnectorOption](
      StringIn(Options.persistent, Options.technology).!
    ) {
      case (loc, Options.persistent, _) => ConnectorPersistentOption(loc)
      case (loc, Options.technology, args) =>
        ConnectorTechnologyOption(loc, args)
      case (_, _, _) => throw new RuntimeException("Impossible case")
    }
  }

  def connector[u: P]: P[Connector] = {
    P(
      location ~ Keywords.connector ~/ identifier ~ is ~/ open ~
        connectorOptions ~
        (undefined((None, None, None)) |
          (
            (Keywords.flows ~ typeRef).? ~/
              Readability.from ~ outletRef ~/
              Readability.to ~ inletRef
          ).map { case (typ, out, in) =>
            (typ, Some(out), Some(in))
          }) ~ close ~/ briefly ~ description
    )./.map { case (loc, id, opts, (typ, out, in), brief, desc) =>
      Connector(loc, id, opts, typ, out, in, brief, desc)
    }
  }

  private def streamletInclude[u: P](
    minInlets: Int,
    maxInlets: Int,
    minOutlets: Int,
    maxOutlets: Int
  ): P[Include[StreamletDefinition]] = {
    include[StreamletDefinition, u](
      streamletDefinition(minInlets, maxInlets, minOutlets, maxOutlets)(_)
    )
  }

  private def streamletDefinition[u: P](
    minInlets: Int,
    maxInlets: Int,
    minOutlets: Int,
    maxOutlets: Int
  ): P[Seq[StreamletDefinition]] = {
    P(
      (inlet.rep(minInlets, " ", maxInlets) ~/
        outlet.rep(minOutlets, " ", maxOutlets) ~/
        (handler | term |
          streamletInclude(minInlets, maxInlets, minOutlets, maxOutlets))
          .rep(0)).map { case (inlets, outlets, definitions) =>
        inlets ++ outlets ++ definitions
      }
    )
  }

  private def streamletOptions[u: P]: P[Seq[StreamletOption]] = {
    options[u, StreamletOption](StringIn(Options.technology).!) {
      case (loc, Options.technology, args) =>
        StreamletTechnologyOption(loc, args)
      case (_, _, _) => throw new RuntimeException("Impossible case")
    }
  }

  private def streamletBody[u: P](
    minInlets: Int,
    maxInlets: Int,
    minOutlets: Int,
    maxOutlets: Int
  ): P[(Seq[StreamletOption], Seq[StreamletDefinition])] = {
    P(
      undefined((Seq.empty[StreamletOption], Seq.empty[StreamletDefinition])) |
        (streamletOptions ~
          streamletDefinition(minInlets, maxInlets, minOutlets, maxOutlets))
    )
  }

  private def keywordToKind(keyword: String, location: At): StreamletShape = {
    keyword match {
      case "source" => Source(location)
      case "sink"   => Sink(location)
      case "flow"   => Flow(location)
      case "merge"  => Merge(location)
      case "split"  => Split(location)
      case "router" => Router(location)
      case "void"   => Void(location)
    }
  }

  private def streamletTemplate[u: P](
    keyword: String,
    minInlets: Int = 0,
    maxInlets: Int = 0,
    minOutlets: Int = 0,
    maxOutlets: Int = 0
  ): P[Streamlet] = {
    P(
      location ~ keyword ~/ identifier ~ authorRefs ~ is ~ open ~
        streamletBody(minInlets, maxInlets, minOutlets, maxOutlets) ~ close ~
        briefly ~ description
    ).map { case (location, id, auths, (options, definitions), brief, desc) =>
      val groups = definitions.groupBy(_.getClass)
      val inlets = mapTo[Inlet](groups.get(classOf[Inlet]))
      val outlets = mapTo[Outlet](groups.get(classOf[Outlet]))
      val handlers = mapTo[Handler](groups.get(classOf[Handler]))
      val functions = mapTo[Function](groups.get(classOf[Function]))
      val types = mapTo[Type](groups.get(classOf[Type]))
      val terms = mapTo[Term](groups.get(classOf[Term]))
      val includes = mapTo[Include[StreamletDefinition]](
        groups.get(
          classOf[Include[StreamletDefinition]]
        )
      )
      Streamlet(
        location,
        id,
        keywordToKind(keyword, location),
        inlets,
        outlets,
        handlers,
        functions,
        types,
        includes,
        auths,
        options,
        terms,
        brief,
        desc
      )
    }
  }

  private val MaxStreamlets = 1000

  def source[u: P]: P[Streamlet] = {
    streamletTemplate(Keywords.source, minOutlets = 1, maxOutlets = 1)
  }

  def sink[u: P]: P[Streamlet] = {
    streamletTemplate(Keywords.sink, minInlets = 1, maxInlets = 1)
  }

  def flow[u: P]: P[Streamlet] = {
    streamletTemplate(
      Keywords.flow,
      minInlets = 1,
      maxInlets = 1,
      minOutlets = 1,
      maxOutlets = 1
    )
  }

  def split[u: P]: P[Streamlet] = {
    streamletTemplate(
      Keywords.split,
      minInlets = 1,
      maxInlets = 1,
      minOutlets = 2,
      maxOutlets = MaxStreamlets
    )
  }

  def merge[u: P]: P[Streamlet] = {
    streamletTemplate(
      Keywords.merge,
      minInlets = 2,
      maxInlets = MaxStreamlets,
      minOutlets = 1,
      maxOutlets = 1
    )
  }

  def router[u: P]: P[Streamlet] = {
    streamletTemplate(
      Keywords.router,
      minInlets = 2,
      maxInlets = MaxStreamlets,
      minOutlets = 2,
      maxOutlets = MaxStreamlets
    )
  }

  def void[u: P]: P[Streamlet] = { streamletTemplate(Keywords.void) }

  def streamlet[u: P]: P[Streamlet] =
    P(source | flow | sink | merge | split | router | void)

}
