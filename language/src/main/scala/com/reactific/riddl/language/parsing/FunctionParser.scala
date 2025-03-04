/*
 * Copyright 2019 Ossum, Inc.
 *
 * SPDX-License-Identifier: Apache-2.0
 */

package com.reactific.riddl.language.parsing

import com.reactific.riddl.language.AST.*
import fastparse.*
import fastparse.ScalaWhitespace.*
import Terminals.*

/** Unit Tests For FunctionParser */
private[parsing] trait FunctionParser extends CommonParser with TypeParser with GherkinParser {

  private def functionOptions[X: P]: P[Seq[FunctionOption]] = {
    options[X, FunctionOption](StringIn(Options.tail_recursive).!) {
      case (loc, Options.tail_recursive, _) => TailRecursive(loc)
      case (_, _, _) => throw new RuntimeException("Impossible case")
    }
  }

  private def functionInclude[x: P]: P[Include[FunctionDefinition]] = {
    include[FunctionDefinition, x](functionDefinitions(_))
  }

  def input[u: P]: P[Aggregation] = {
    P(Keywords.requires ~ Punctuation.colon.? ~ aggregation)
  }

  def output[u: P]: P[Aggregation] = {
    P(Keywords.returns ~ Punctuation.colon.? ~ aggregation)
  }

  private def functionDefinitions[u: P]: P[Seq[FunctionDefinition]] = {
    P(typeDef | example | function | term | functionInclude).rep(0)
  }

  private def functionBody[u: P]: P[
    (
      Seq[FunctionOption],
      Option[Aggregation],
      Option[Aggregation],
      Seq[FunctionDefinition]
    )
  ] = {
    P(undefined(None).map { _ =>
      (Seq.empty[FunctionOption], None, None, Seq.empty[FunctionDefinition])
    } | (functionOptions ~ input.? ~ output.? ~ functionDefinitions))
  }

  /** Parses function literals, i.e.
    *
    * {{{
    *   function myFunction is {
    *     requires is Boolean
    *     yields is Integer
    *   }
    * }}}
    */
  def function[u: P]: P[Function] = {
    P(
      location ~ Keywords.function ~/ identifier ~ authorRefs ~ is ~ open ~
        functionBody ~ close ~ briefly ~ description
    ).map {
      case (
            loc,
            id,
            authors,
            (options, input, output, definitions),
            briefly,
            description
          ) =>
        val groups = definitions.groupBy(_.getClass)
        val types = mapTo[Type](groups.get(classOf[Type]))
        val examples = mapTo[Example](groups.get(classOf[Example]))
        val functions = mapTo[Function](groups.get(classOf[Function]))
        val terms = mapTo[Term](groups.get(classOf[Term]))
        val includes = mapTo[Include[FunctionDefinition]](groups.get(
          classOf[Include[FunctionDefinition]]
        ))
        Function(
          loc,
          id,
          input,
          output,
          types,
          functions,
          examples,
          authors,
          includes,
          options,
          terms,
          briefly,
          description
        )
    }
  }
}
