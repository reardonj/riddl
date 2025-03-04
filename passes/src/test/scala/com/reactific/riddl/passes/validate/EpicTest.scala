/*
 * Copyright 2019 Ossum, Inc.
 *
 * SPDX-License-Identifier: Apache-2.0
 */

package com.reactific.riddl.passes.validate

import com.reactific.riddl.language.AST.{Domain, LiteralString}
import com.reactific.riddl.language.Messages
import com.reactific.riddl.language.parsing.RiddlParserInput

class EpicTest extends ValidatingTest {

  "Epic" should {
    "parse and validate a case-less example " in {
      val rpi = RiddlParserInput(
        """domain foo is {
          |  user Author is "human writer"
          |epic WritingABook is {
          |  user foo.Author wants "edit on the screen" so that
          |  "revise content more easily"
          |  shown by { http://example.com:80/path/to/WritingABook }
          |  case perfection is { ???  }
          |} described as "A simple authoring story"
          |} described as "a parsing convenience"
          |""".stripMargin
      )
      parseAndValidateDomain(rpi) {
        case (
              domain: Domain,
              rpi: RiddlParserInput,
              messages: Messages.Messages
            ) =>
          domain.epics mustNot be(empty)
          messages.isOnlyIgnorable mustBe true
          val story = domain.epics.head
          story.id.format mustBe "WritingABook"
          story.userStory mustNot be(empty)
          val us = story.userStory.get
          us mustNot be(empty)
          us.user.pathId.value mustBe Seq("foo", "Author")
          us.capability mustBe LiteralString((4, 25, rpi), "edit on the screen")
          us.benefit mustBe
            LiteralString((5, 3, rpi), "revise content more easily")
          story.shownBy mustNot be(empty)
          story.shownBy.head.toString mustBe
            "http://example.com:80/path/to/WritingABook"
          story.cases mustNot be(empty)
          val sc = story.cases.head
          sc.id.value mustBe "perfection"
      }
    }

    "parse and validate a full example" in {
      val rpi = RiddlParserInput(
        """domain ImprovingApp is {
          |context OrganizationContext {
          |  command CreateOrganization is {
          |    id: Id(OrganizationContext.Organization),
          |    name: String
          |  }
          |  result OrganizationInfo is {
          |    name: String,
          |    createdAt: TimeStamp
          |  }
          |
          |  entity Organization is { ??? } described as "nada"
          |
          |  projector OrganizationViews is {
          |   record SimpleView { a: Integer}
          |   handler Simple { ??? }
          |   term xyz
          |   brief "y"
          |  } described as "nada"
          |} described as "nada"
          |
          |application Improving_app is {
          |  group OrganizationPage is {
          |    input accept is {
          |      acquires command
          |        ImprovingApp.OrganizationContext.CreateOrganization
          |    }
          |    output show is {
          |      presents result
          |        ImprovingApp.OrganizationContext.OrganizationInfo
          |    }
          |  }
          |}
          |
          |author reid is {
          |  name: "Reid Spencer"
          |  email: "reid.spencer@ossum.biz"
          |} briefly "nada" described as "nada"
          |
          |user Owner is "a person"
          |
          |epic EstablishOrganization by author reid is {
          |  user ImprovingApp.Owner wants "to establish an organization" so that
          |  "they can conduct business as that organization"
          |  term 'conduct business' briefly
          |  "Any legal business activity supported by the terms of use."
          |
          |  case primary is {
          |    optional {
          |      step from user ImprovingApp.Owner "creates an Organization" to
          |        input ImprovingApp.Improving_app.OrganizationPage.accept
          |        briefly "create org",
          |      step from output ImprovingApp.Improving_app.OrganizationPage.show
          |        "presented" to user ImprovingApp.Owner
          |        briefly "organization added"
          |    }
          |  }
          |} briefly "A story about establishing an organization in Improving.app"
          |  described as "TBD"
          |} briefly "A placeholder" described by "Not important"
          |""".stripMargin
      )
      parseAndValidateDomain(rpi, shouldFailOnErrors = false) {
        case (domain: Domain, _: RiddlParserInput, msgs: Messages.Messages) =>
          domain mustNot be(empty)
          domain.epics mustNot be(empty)
          if msgs.nonEmpty then { info(msgs.format) }
          msgs.hasErrors mustBe false
      }
    }
    "handle parallel group" in {
      val rpi = RiddlParserInput(
        """domain ImprovingApp is {
          |context OrganizationContext {
          |  command CreateOrganization is {
          |    id: Id(OrganizationContext.Organization),
          |    name: String
          |  }
          |  result OrganizationInfo is {
          |    name: String,
          |    createdAt: TimeStamp
          |  }
          |
          |  entity Organization is { ??? } described as "nada"
          |
          |  projector OrganizationViews is {
          |   record SimpleView { a: Integer}
          |   handler Simple { ??? }
          |   term xyz
          |   brief "y"
          |  } described as "nada"
          |} described as "nada"
          |
          |application Improving_app is {
          |  group OrganizationPage is {
          |    input accept is {
          |      acquires command
          |        ImprovingApp.OrganizationContext.CreateOrganization
          |    }
          |    output show is {
          |      presents result
          |        ImprovingApp.OrganizationContext.OrganizationInfo
          |    }
          |  }
          |}
          |
          |author reid is {
          |  name: "Reid Spencer"
          |  email: "reid.spencer@ossum.biz"
          |} briefly "nada" described as "nada"
          |
          |user Owner is "a person"
          |
          |epic EstablishOrganization by author reid is {
          |  user ImprovingApp.Owner wants "to establish an organization" so that
          |  "they can conduct business as that organization"
          |  term 'conduct business' briefly
          |  "Any legal business activity supported by the terms of use."
          |
          |  case primary is {
          |    parallel {
          |      step from user ImprovingApp.Owner "creates an Organization" to
          |        input ImprovingApp.Improving_app.OrganizationPage.accept
          |        briefly "create org",
          |      step from output ImprovingApp.Improving_app.OrganizationPage.show
          |        "presented" to user ImprovingApp.Owner
          |        briefly "organization added"
          |     }
          |  }
          |} briefly "A story about establishing an organization in Improving.app"
          |  described as "TBD"
          |} briefly "A placeholder" described by "Not important"
          |""".stripMargin
      )
      parseAndValidateDomain(rpi, shouldFailOnErrors = false) {
        case (domain: Domain, _: RiddlParserInput, msgs: Messages.Messages) =>
          domain mustNot be(empty)
          domain.epics mustNot be(empty)
          if msgs.nonEmpty then { info(msgs.format) }
          msgs.hasErrors mustBe false
          succeed
      }
    }
    "handle optional group" in {
      val rpi = RiddlParserInput(
        """domain ImprovingApp is {
          |context OrganizationContext {
          |  command CreateOrganization is {
          |    id: Id(OrganizationContext.Organization),
          |    name: String
          |  }
          |  result OrganizationInfo is {
          |    name: String,
          |    createdAt: TimeStamp
          |  }
          |
          |  entity Organization is { ??? } described as "nada"
          |
          |  projector OrganizationViews is {
          |   record SimpleView { a: Integer}
          |   handler Simple { ??? }
          |   term xyz
          |   brief "y"
          |  } described as "nada"
          |} described as "nada"
          |
          |application Improving_app is {
          |  group OrganizationPage is {
          |    input accept is {
          |      acquires command
          |        ImprovingApp.OrganizationContext.CreateOrganization
          |    }
          |    output show is {
          |      presents result
          |        ImprovingApp.OrganizationContext.OrganizationInfo
          |    }
          |  }
          |}
          |
          |user Owner is "a person"
          |
          |epic EstablishOrganization is {
          |  user ImprovingApp.Owner wants "to establish an organization" so that
          |  "they can conduct business as that organization"
          |  term 'conduct business' briefly
          |  "Any legal business activity supported by the terms of use."
          |
          |  case primary is {
          |    step from user ImprovingApp.Owner "creates an Organization" to
          |      input ImprovingApp.Improving_app.OrganizationPage.accept
          |      briefly "create org",
          |    step for user ImprovingApp.Owner is "contemplates his navel"
          |      briefly "self-processing",
          |    step from output ImprovingApp.Improving_app.OrganizationPage.show
          |      "presented" to user ImprovingApp.Owner
          |      briefly "organization added"
          |  }
          |} briefly "A story about establishing an organization in Improving.app"
          |  described as "TBD"
          |} briefly "A placeholder" described by "Not important"
          |""".stripMargin
      )
      parseAndValidateDomain(rpi, shouldFailOnErrors = false) {
        case (domain: Domain, _: RiddlParserInput, msgs: Messages.Messages) =>
          domain mustNot be(empty)
          domain.epics mustNot be(empty)
          if msgs.nonEmpty then { info(msgs.format) }
          msgs.hasErrors mustBe false
          succeed
      }
    }
  }
}
