/*
 * Copyright 2019 Ossum, Inc.
 *
 * SPDX-License-Identifier: Apache-2.0
 */

package com.reactific.riddl.hugo

import com.reactific.riddl.commands.CommandOptions.optional
import com.reactific.riddl.commands.{CommandOptions, PassCommand, TranslatingOptions}
import com.reactific.riddl.language.CommonOptions
import com.reactific.riddl.language.Messages.Messages
import com.reactific.riddl.passes.Pass.{PassesCreator, standardPasses}
import com.reactific.riddl.passes.{PassInput, PassesResult}
import com.reactific.riddl.utils.Logger
import com.reactific.riddl.stats.StatsPass
import pureconfig.ConfigCursor
import pureconfig.ConfigReader
import scopt.OParser

import java.net.URL
import java.nio.file.Path
import scala.annotation.unused

/** Unit Tests For HugoCommand */
object HugoCommand {
  case class Options(
    inputFile: Option[Path] = None,
    outputDir: Option[Path] = None,
    projectName: Option[String] = None,
    enterpriseName: Option[String] = None,
    eraseOutput: Boolean = false,
    siteTitle: Option[String] = None,
    siteDescription: Option[String] = None,
    siteLogoPath: Option[String] = Some("images/logo.png"),
    siteLogoURL: Option[URL] = None,
    baseUrl: Option[URL] = Option(new URL("https://example.com/")),
    themes: Seq[(String, Option[URL])] =
    Seq("hugo-geekdoc" -> Option(HugoPass.geekDoc_url)),
    sourceURL: Option[URL] = None,
    editPath: Option[String] = Some("edit/main/src/main/riddl"),
    viewPath: Option[String] = Some("blob/main/src/main/riddl"),
    withGlossary: Boolean = true,
    withTODOList: Boolean = true,
    withGraphicalTOC: Boolean = false,
    withStatistics: Boolean = true)
    extends CommandOptions with TranslatingOptions {
    def command: String = "hugo"

    def outputRoot: Path = outputDir.getOrElse(Path.of("")).toAbsolutePath

    def contentRoot: Path = outputRoot.resolve("content")

    def staticRoot: Path = outputRoot.resolve("static")

    def themesRoot: Path = outputRoot.resolve("themes")

    def configFile: Path = outputRoot.resolve("config.toml")
  }
}

class HugoCommand extends PassCommand[HugoCommand.Options]("hugo") {

  import HugoCommand.Options

  override def getOptions: (OParser[Unit, Options], Options) = {
    import builder.*
    cmd("hugo").text(
      """Parse and validate the input-file and then translate it into the input
        |needed for hugo to translate it to a functioning web site."""
        .stripMargin
    ).children(
      inputFile((v, c) => c.copy(inputFile = Option(v.toPath))),
      outputDir((v, c) => c.copy(outputDir = Option(v.toPath))),
      opt[String]('p', "project-name").optional()
        .action((v, c) => c.copy(projectName = Option(v)))
        .text("optional project name to associate with the generated output")
        .validate(n =>
          if n.isBlank then {
            Left("option project-name cannot be blank or empty")
          } else { Right(()) }
        ),
      opt[String]('E', "enterprise-name").optional()
        .action((v, c) => c.copy(projectName = Option(v)))
        .text("optional enterprise name for C4 diagram output").validate(n =>
          if n.isBlank then {
            Left("option enterprise-name cannot be blank or empty")
          } else { Right(()) }
        ),
      opt[Boolean]('e', name = "erase-output")
        .text("Erase entire output directory before putting out files")
        .optional().action((v, c) => c.copy(eraseOutput = v)),
      opt[Boolean]('e', name = "with-statistics")
        .text("Generate a statistics page accessible from index page")
        .optional().action((v, c) => c.copy(withStatistics = v)),
      opt[Boolean]('e', name = "with-glossary")
        .text("Generate a glossary of terms and definitions ").optional()
        .action((v, c) => c.copy(withGlossary = v)),
      opt[Boolean]('e', name = "with-todo-list").text("Generate a To Do list")
        .optional().action((v, c) => c.copy(withTODOList = v)),
      opt[Boolean]('e', name = "with-graphical-toc")
        .text("Generate a graphically navigable table of contents").optional()
        .action((v, c) => c.copy(withGraphicalTOC = v)),
      opt[URL]('b', "base-url").optional()
        .action((v, c) => c.copy(baseUrl = Some(v)))
        .text("Optional base URL for root of generated http URLs"),
      opt[Map[String, String]]('t', name = "themes").action((t, c) =>
        c.copy(themes = t.toSeq.map(x => x._1 -> Some(new URL(x._2))))
      ).text("Add theme name/url pairs to use alternative Hugo themes"),
      opt[URL]('s', name = "source-url")
        .action((u, c) => c.copy(baseUrl = Option(u)))
        .text("URL to the input file's Git Repository"),
      opt[String]('h', name = "edit-path")
        .action((h, c) => c.copy(editPath = Option(h)))
        .text("Path to add to source-url to allow editing"),
      opt[String]('m', "site-logo-path")
        .action((s, c) => c.copy(siteLogoPath = Option(s)))
        .text("""Path, in 'static' directory to placement and use
                |of the site logo.""".stripMargin),
      opt[String]('n', "site-logo-url")
        .action((s, c) => c.copy(siteLogoURL = Option(new URL(s))))
        .text("URL from which to copy the site logo.")
    ) -> HugoCommand.Options()
  }

  override def getConfigReader: ConfigReader[Options] = { (cur: ConfigCursor) =>
    for
      topCur <- cur.asObjectCursor
      topRes <- topCur.atKey(pluginName)
      objCur <- topRes.asObjectCursor
      inputPathRes <- objCur.atKey("input-file")
      inputPath <- inputPathRes.asString
      outputPathRes <- objCur.atKey("output-dir")
      outputPath <- outputPathRes.asString
      eraseOutput <- optional(objCur, "erase-output", true) { cc =>
        cc.asBoolean
      }
      projectName <- optional(objCur, "project-name", "No Project Name") {
        cur => cur.asString
      }
      enterpriseName <-
        optional(objCur, "enterprise-name", "No Enterprise Name") { cur =>
          cur.asString
        }
      siteTitle <- optional(objCur, "site-title", "No Site Title") { cur =>
        cur.asString
      }
      siteDescription <-
        optional(objCur, "site-description", "No Site Description") { cur =>
          cur.asString
        }
      siteLogoPath <- optional(objCur, "site-logo-path", "static/somewhere") {
        cc => cc.asString
      }
      siteLogoURL <- optional(objCur, "site-logo-url", Option.empty[String]) {
        cc => cc.asString.map(Option[String])
      }
      baseURL <- optional(objCur, "base-url", Option.empty[String]) { cc =>
        cc.asString.map(Option[String])
      }
      themesMap <- optional(objCur, "themes", Map.empty[String, ConfigCursor]) {
        cc => cc.asMap
      }
      sourceURL <- optional(objCur, "source-url", Option.empty[String]) { cc =>
        cc.asString.map(Option[String])
      }
      viewPath <- optional(objCur, "view-path", "blob/main/src/main/riddl") {
        cc => cc.asString
      }
      editPath <- optional(objCur, "edit-path", "edit/main/src/main/riddl") {
        cc => cc.asString
      }
      withGlossary <- optional(objCur, "with-glossary", true) { cc =>
        cc.asBoolean
      }
      withToDoList <- optional(objCur, "with-todo-list", true) { cc =>
        cc.asBoolean
      }
      withStatistics <- optional(objCur, "with-statistics", true) { cc =>
        cc.asBoolean
      }
      withGraphicalTOC <- optional(objCur, "with-graphical-toc", false) { cc =>
        cc.asBoolean
      }
    yield {
      def handleURL(url: Option[String]): Option[URL] = {
        if url.isEmpty || url.get.isEmpty then None
        else Option(new java.net.URL(url.get))
      }

      val themes =
        if themesMap.isEmpty then {
          Seq("hugo-geekdoc" -> Option(HugoPass.geekDoc_url))
        } else {
          val themesEither = themesMap.toSeq.map(x => x._1 -> x._2.asString)
          themesEither.map { case (name, maybeUrl) =>
            name -> {
              maybeUrl match {
                case Right(s) => handleURL(Option(s))
                case Left(x) =>
                  val errs = x.prettyPrint(1)
                  throw new IllegalArgumentException(errs)
              }
            }
          }
        }
      HugoCommand.Options(
        Option(Path.of(inputPath)),
        Option(Path.of(outputPath)),
        Option(projectName),
        Option(enterpriseName),
        eraseOutput,
        Option(siteTitle),
        Option(siteDescription),
        Option(siteLogoPath),
        handleURL(siteLogoURL),
        handleURL(baseURL),
        themes,
        handleURL(sourceURL),
        Option(editPath),
        Option(viewPath),
        withGlossary,
        withToDoList,
        withGraphicalTOC,
        withStatistics
      )
    }
  }

  def overrideOptions(options: Options, newOutputDir: Path): Options = {
    options.copy(outputDir = Some(newOutputDir))
  }


  def getPasses(
    log: Logger,
    commonOptions: CommonOptions,
    options: Options
  ): PassesCreator = {
    standardPasses ++ Seq(
      { (input: PassInput) => StatsPass(input) },
      { (input: PassInput) =>
        val result = PassesResult(input)
        val state = HugoTranslatorState(result, options, commonOptions, log)
        HugoPass(input, state)
      }
    )
  }

  override def replaceInputFile(
    opts: Options,
    @unused inputFile: Path
  ): Options = { opts.copy(inputFile = Some(inputFile)) }

  override def loadOptionsFrom(
    configFile: Path,
    commonOptions: CommonOptions
  ): Either[Messages, HugoCommand.Options] = {
    super.loadOptionsFrom(configFile, commonOptions).map { options =>
      resolveInputFileToConfigFile(options, commonOptions, configFile)
    }
  }

}
