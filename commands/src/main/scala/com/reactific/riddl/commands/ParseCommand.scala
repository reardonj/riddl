/*
 * Copyright 2019 Ossum, Inc.
 *
 * SPDX-License-Identifier: Apache-2.0
 */

package com.reactific.riddl.commands

import com.reactific.riddl.language.Messages.Messages
import com.reactific.riddl.language.{CommonOptions, Parser}
import com.reactific.riddl.passes.PassesResult
import com.reactific.riddl.utils.Logger

import java.nio.file.Path

object ParseCommand {
  val cmdName = "parse"
}

/** A Command for Parsing RIDDL input
  */
class ParseCommand extends InputFileCommandPlugin(ParseCommand.cmdName) {
  import InputFileCommandPlugin.Options

  override def run(
    options: Options,
    commonOptions: CommonOptions,
    log: Logger,
    outputDirOverride: Option[Path]
  ): Either[Messages, PassesResult] = {
    options.withInputFile { (inputFile: Path) =>
      Parser.parse(inputFile, commonOptions)
        .map(_ => PassesResult()).map(_ => PassesResult())
    }
  }

  override def loadOptionsFrom(
    configFile: Path,
    commonOptions: CommonOptions
  ): Either[Messages, Options] = {
    super.loadOptionsFrom(configFile, commonOptions).map { options =>
      resolveInputFileToConfigFile(options, commonOptions, configFile)
    }
  }

  override def replaceInputFile(
    opts: Options,
    inputFile: Path
  ): Options = { opts.copy(inputFile = Some(inputFile)) }

}
