import com.typesafe.sbt.packager.Keys.maintainer
import de.heikoseeberger.sbtheader.HeaderPlugin.autoImport.HeaderLicense
import de.heikoseeberger.sbtheader.HeaderPlugin.autoImport.HeaderLicenseStyle
import de.heikoseeberger.sbtheader.HeaderPlugin.autoImport.headerLicense
import sbt.Keys.organizationName
import sbt.Keys._
import sbt._
import sbt.io.Path.allSubpaths
import scoverage.ScoverageKeys._
import sbtdynver.DynVerPlugin.autoImport.dynverSeparator
import sbtdynver.DynVerPlugin.autoImport.dynverSonatypeSnapshots
import sbtdynver.DynVerPlugin.autoImport.dynverVTagPrefix

/** V - Dependency Versions object */
object V {
  val commons_io = "2.13.0"
  val compress = "1.23.0"
  val config = "1.4.2"
  val fastparse = "3.0.1"
  val jgit = "6.5.0"
  val lang3 = "3.12.0"
  val pureconfig = "0.17.4"
  val scalacheck = "1.17.0"
  val scalatest = "3.2.16"
  val scopt = "4.1.0"
  val slf4j = "2.0.4"
}

object Dep {
  val compress = "org.apache.commons" % "commons-compress" % V.compress
  val commons_io = "commons-io" % "commons-io" % V.commons_io
  val fastparse = "com.lihaoyi" %% "fastparse" % V.fastparse
  val jgit = "org.eclipse.jgit" % "org.eclipse.jgit" % V.jgit
  val lang3 = "org.apache.commons" % "commons-lang3" % V.lang3
  val pureconfig = "com.github.pureconfig" %% "pureconfig-core" % V.pureconfig
  val scalactic = "org.scalactic" %% "scalactic" % V.scalatest
  val scalatest = "org.scalatest" %% "scalatest" % V.scalatest
  val scalacheck = "org.scalacheck" %% "scalacheck" % V.scalacheck
  val scopt = "com.github.scopt" %% "scopt" % V.scopt
  val slf4j = "org.slf4j" % "slf4j-nop" % V.slf4j

  val testing: Seq[ModuleID] =
    Seq(scalactic % "test", scalatest % "test", scalacheck % "test")
  val testKitDeps: Seq[ModuleID] = Seq(scalactic, scalatest, scalacheck)

}

object C {
  def withInfo(p: Project): Project = {
    p.settings(
      ThisBuild / maintainer := "reid@ossum.biz",
      ThisBuild / maintainer := "reid@ossum.biz",
      ThisBuild / organization := "com.reactific",
      ThisBuild / organizationHomepage :=
        Some(new URL("https://reactific.com/")),
      ThisBuild / organizationName := "Ossum Inc.",
      ThisBuild / startYear := Some(2019),
      ThisBuild / licenses +=
        (
          "Apache-2.0",
          new URL("https://www.apache.org/licenses/LICENSE-2.0.txt")
        ),
      ThisBuild / versionScheme := Option("early-semver"),
      ThisBuild / dynverVTagPrefix := false,
      // NEVER  SET  THIS: version := "0.1"
      // IT IS HANDLED BY: sbt-dynver
      ThisBuild / dynverSeparator := "-",
      headerLicense := Some(
        HeaderLicense.ALv2(
          startYear.value.get.toString,
          "Ossum, Inc.",
          HeaderLicenseStyle.SpdxSyntax
        )
      )
    )
  }

  lazy val scala_3_options: Seq[String] =
    Seq(
      "-deprecation",
      "-feature",
      "-new-syntax",
      "-explain",
      "-explain-types",
      "-Werror",
      "-pagewidth",
      "120"
    )

  def scala_3_doc_options(version: String): Seq[String] = {
    Seq(
      "-deprecation",
      "-feature",
      "-groups",
      "-project:RIDDL",
      "-comment-syntax:wiki",
      s"-project-version:$version",
      "-siteroot:doc/src/hugo/static/apidoc",
      "-author",
      "-doc-canonical-base-url:https://riddl.tech/apidoc"
    )
  }

  def withScala3(p: Project): Project = {
    p.configure(withInfo)
      .settings(
        scalaVersion := "3.3.1-RC1",
        scalacOptions := scala_3_options,
        Compile / doc / scalacOptions := scala_3_doc_options((compile / scalaVersion).value),
        apiURL := Some(url("https://riddl.tech/apidoc/")),
        autoAPIMappings := true
      )
  }

  final val defaultPercentage: Int = 50

  def withCoverage(percent: Int = defaultPercentage)(p: Project): Project = {
    p.settings(
      coverageFailOnMinimum := true,
      coverageMinimumStmtTotal := percent,
      coverageMinimumBranchTotal := percent,
      coverageMinimumStmtPerPackage := percent,
      coverageMinimumBranchPerPackage := percent,
      coverageMinimumStmtPerFile := percent,
      coverageMinimumBranchPerFile := percent,
      coverageExcludedPackages := "<empty>"
    )
  }

  private def makeThemeResource(
    name: String,
    from: File,
    targetDir: File
  ): Seq[File] = {
    val zip = name + ".zip"
    val distTarget = targetDir / zip
    IO.copyDirectory(from, targetDir)
    val dirToZip = targetDir / name
    IO.zip(allSubpaths(dirToZip), distTarget, None)
    Seq(distTarget)
  }

  def zipResource(srcDir: String)(p: Project): Project = {
    p.settings(
      Compile / resourceGenerators += Def.task {
        val projectName = name.value
        val from = sourceDirectory.value / srcDir
        val targetDir = target.value / "dist"
        makeThemeResource(projectName, from, targetDir)
      }.taskValue,
      Compile / packageDoc / publishArtifact := false
    )
  }

  def mavenPublish(p: Project): Project = {
    p.settings(
      ThisBuild / dynverSonatypeSnapshots := true,
      ThisBuild / dynverSeparator := "-",
      maintainer := "reid@ossum.biz",
      organization := "com.reactific",
      organizationName := "Ossum Inc.",
      organizationHomepage := Some(url("https://riddl.tech")),
      scmInfo := Some(
        ScmInfo(
          url("https://github.com/reactific/riddl"),
          "scm:git:git://github.com/reactific/riddl.git"
        )
      ),
      developers := List(
        Developer(
          id = "reid-spencer",
          name = "Reid Spencer",
          email = "reid@reactific.com",
          url = url("https://riddl.tech")
        )
      ),
      description :=
        """RIDDL is a language and toolset for specifying a system design using ideas from
          |DDD, reactive architecture, distributed systems patterns, and other software
          |architecture practices.""".stripMargin,
      licenses := List(
        "Apache License, Version 2.0" ->
          new URL("https://www.apache.org/licenses/LICENSE-2.0")
      ),
      homepage := Some(url("https://riddl.tech")),

      // Remove all additional repository other than Maven Central from POM
      pomIncludeRepository := { _ => false },
      publishTo := {
        val nexus = "https://oss.sonatype.org/"
        if (isSnapshot.value) {
          Some("snapshots" at nexus + "content/repositories/snapshots")
        } else {
          Some("releases" at nexus + "service/local/staging/deploy/maven2")
        }
      },
      publishMavenStyle := true
    )
  }
}
