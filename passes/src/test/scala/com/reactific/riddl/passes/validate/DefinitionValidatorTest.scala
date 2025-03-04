/*
 * Copyright 2019 Ossum, Inc.
 *
 * SPDX-License-Identifier: Apache-2.0
 */

package com.reactific.riddl.passes.validate

import com.reactific.riddl.language.AST.Domain
import com.reactific.riddl.language.Messages.*
import com.reactific.riddl.language.parsing.RiddlParserInput

class DefinitionValidatorTest extends ValidatingTest {

  "Definition Validation" should {
    "warn when an identifier is less than 3 characters" in {
      val input = RiddlParserInput(
        """domain po is {
          |type Ba is String
          |}
          |""".stripMargin
      )
      parseAndValidateDomain(input, shouldFailOnErrors = false) {
        case (_: Domain, _, msgs: Seq[Message]) =>
          if msgs.isEmpty then {
            fail(
              "Identifiers with less than 3 characters should generate a warning"
            )
          } else {
            val styleWarnings = msgs.filter(_.isStyle)
            styleWarnings.size mustEqual 2
            assertValidationMessage(
              styleWarnings, StyleWarning, "Domain identifier 'po' is too short. The minimum length is 3"
            )
            assertValidationMessage(
              styleWarnings, StyleWarning, "Type identifier 'Ba' is too short. The minimum length is 3"
            )
          }
      }
    }
  }
}
