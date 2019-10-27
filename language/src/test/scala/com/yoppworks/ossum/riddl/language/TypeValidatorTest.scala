package com.yoppworks.ossum.riddl.language

import com.yoppworks.ossum.riddl.language.Validation._
import com.yoppworks.ossum.riddl.language.AST.DomainDef
import com.yoppworks.ossum.riddl.language.AST.TypeDef
import org.scalatest._

/** Unit Tests For TypeValidationTest */
class TypeValidatorTest extends ValidatingTest {

  "TypeValidator" should {
    "ensure type names start with capital letter" in {
      parseAndValidate[DomainDef](
        """domain foo {
          |type bar is String
          |}
          |""".stripMargin
      ) {
        case (_: DomainDef, msgs: Seq[ValidationMessage]) =>
          if (msgs.isEmpty)
            fail("Type 'bar' should have generated warning")
          else if (msgs.map(_.message).exists(_.contains("must start with"))) {
            succeed
          } else {
            fail("No such message")
          }
      }
    }
    "identify undefined type references" in {
      parseAndValidate[DomainDef](
        """
          |domain foo {
          |type Rename is Bar
          |type OneOrMore is many Bar
          |type ZeroOrMore is many optional Bar
          |type Optional is optional Bar
          |type Aggregate is {a: Bar, b: Foo}
          |type Alternation is ( Bar or Foo )
          |}
          |""".stripMargin
      ) {
        case (_: DomainDef, msgsAndWarnings: Seq[ValidationMessage]) =>
          val msgs = msgsAndWarnings.filter(_.kind == Error)
          assert(msgs.size == 8, "Should have 8 errors")
          assert(msgs.forall(_.kind == Error), "Should be an error")
          assert(
            msgs.forall(_.message.contains("not defined")),
            "Wrong message"
          )
      }
    }
  }
}
