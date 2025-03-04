/*
 * Copyright 2019 Ossum, Inc.
 *
 * SPDX-License-Identifier: Apache-2.0
 */

package com.reactific.riddl.utils

/** The interface that plugins must implement */

trait PluginInterface {
  def interfaceVersion: Int = Plugin.interfaceVersion
  def riddlVersion: String = RiddlBuildInfo.version
  def pluginName: String
  def pluginVersion: String
}
