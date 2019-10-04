package com.yoppworks.ossum.riddl.parser

import com.yoppworks.ossum.riddl.parser.AST._
import fastparse._
import ScalaWhitespace._
import CommonParser._

import scala.collection.immutable.ListMap

/** Parsing rules for Type definitions */
object TypesParser {

  def predefinedType[_: P]: P[PredefinedType] = {
    P(
      StringIn(
        "String",
        "Number",
        "Boolean",
        "Id",
        "Date",
        "Time",
        "TimeStamp",
        "URL"
      )./
    ).!.map {
      case "Boolean" ⇒ AST.Boolean
      case "String" ⇒ AST.Strng
      case "Number" ⇒ AST.Number
      case "Id" ⇒ AST.Id
      case "Date" ⇒ AST.Date
      case "Time" ⇒ AST.Time
      case "TimeStamp" ⇒ AST.TimeStamp
      case "URL" ⇒ AST.URL
    }
  }

  def enumerationType[_: P]: P[Enumeration] = {
    P("any" ~/ "[" ~ identifier.rep(1, sep = ",".?) ~ "]").map { enums ⇒
      Enumeration(enums)
    }
  }

  def alternationType[_: P]: P[Alternation] = {
    P(
      "choose" ~/ identifier.rep(2, P("or" | "|"))
    ).map(_.map(TypeRef)).map(Alternation)
  }

  def typeExpression[_: P]: P[TypeExpression] = {
    P(cardinality(predefinedType | typeRef))
  }

  def cardinality[_: P](p: ⇒ P[TypeExpression]): P[TypeExpression] = {
    P(p ~ ("?".! | "*".! | "+".!).?).map {
      case (typ, Some("?")) ⇒ Optional(typ.id)
      case (typ, Some("+")) ⇒ OneOrMore(typ.id)
      case (typ, Some("*")) ⇒ ZeroOrMore(typ.id)
      case (typ, Some(_)) => typ
      case (typ, None) ⇒ typ
    }
  }

  def field[_: P]: P[(Identifier, TypeExpression)] = {
    P(identifier ~ is ~ typeExpression)
  }

  def fields[_: P]: P[Seq[(Identifier, TypeExpression)]] = {
    P(field.rep(1, P(",")))
  }

  def aggregationType[_: P]: P[Aggregation] = {
    P(
      "combine" ~/ "{" ~ fields ~ "}"
    ).map(types ⇒ Aggregation(ListMap[Identifier, TypeExpression](types: _*)))
  }

  def typeDefinitions[_: P]: P[TypeDefinition] = {
    P(
      enumerationType | alternationType | aggregationType
    )
  }

  def types[_: P]: P[Type] = {
    P(typeDefinitions | typeExpression)
  }

  def typeDef[_: P]: P[TypeDef] = {
    P(
      "type" ~ Index ~/ identifier ~ is ~ types ~ explanation
    ).map { tpl ⇒
      (TypeDef.apply _).tupled(tpl)
    }
  }

  def typeRef[_: P]: P[TypeRef] = {
    P(identifier).map { id ⇒
      TypeRef(id)
    }
  }
}
