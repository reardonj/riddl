import com.jsuereth.sbtpgp.PgpKeys.pgpSigner
import org.scoverage.coveralls.Imports.CoverallsKeys.*
import sbtbuildinfo.BuildInfoOption.ToJson
import sbtbuildinfo.BuildInfoOption.ToMap
import sbtbuildinfo.BuildInfoOption.BuildTime
import java.util.Calendar

Global / onChangedBuildSource := ReloadOnSourceChanges
(Global / excludeLintKeys) ++= Set(
  buildInfoPackage,
  buildInfoKeys,
  buildInfoOptions,
  dynverVTagPrefix,
  mainClass,
  maintainer,
  headerLicense
)

// NEVER  SET  THIS: version := "0.1"
// IT IS HANDLED BY: sbt-dynver
ThisBuild / dynverSeparator := "-"

lazy val riddl = (project in file("."))
  .enablePlugins(ScoverageSbtPlugin)
  .enablePlugins(AutomateHeaderPlugin)
  .configure(C.withInfo)
  .settings(
    publish := {},
    publishLocal := {},
    pgpSigner / skip := true,
    publishTo := Some(Resolver.defaultLocal)
  )
  .aggregate(
    utils,
    language,
    passes,
    commands,
    testkit,
    prettify,
    hugo,
    doc,
    riddlc,
    plugin
  )

lazy val Utils = config("utils")
lazy val utils = project
  .in(file("utils"))
  .configure(C.mavenPublish)
  .configure(C.withScala3)
  .configure(C.withCoverage(0))
  .enablePlugins(BuildInfoPlugin)
  .settings(
    name := "riddl-utils",
    libraryDependencies ++= Seq(Dep.compress, Dep.lang3) ++ Dep.testing,
    buildInfoObject := "RiddlBuildInfo",
    buildInfoPackage := "com.reactific.riddl.utils",
    buildInfoOptions := Seq(ToMap, ToJson, BuildTime),
    buildInfoUsePackageAsPath := true,
    buildInfoKeys ++= Seq[BuildInfoKey](
      name,
      version,
      description,
      organization,
      organizationName,
      BuildInfoKey.map(organizationHomepage) { case (k, v) =>
        k -> v.get.toString
      },
      BuildInfoKey.map(homepage) { case (k, v) =>
        "projectHomepage" -> v.map(_.toString).getOrElse("https://riddl.tech")
      },
      BuildInfoKey.map(startYear) { case (k, v) =>
        k -> v.map(_.toString).getOrElse("2019")
      },
      BuildInfoKey.map(startYear) { case (k, v) =>
        "copyright" -> s"© ${v.map(_.toString).getOrElse("2019")}-${Calendar
            .getInstance()
            .get(Calendar.YEAR)} Ossum Inc."
      },
      scalaVersion,
      sbtVersion,
      BuildInfoKey.map(scalaVersion) { case (k, v) =>
        val version = if (v.head == '2') { v.substring(0, v.lastIndexOf('.')) }
        else v
        "scalaCompatVersion" -> version
      },
      BuildInfoKey.map(licenses) { case (k, v) =>
        k -> v.map(_._1).mkString(", ")
      }
    )
  )

val Language: Configuration = config("language")
lazy val language: Project = project
  .in(file("language"))
  .configure(C.withCoverage(0))
  .configure(C.mavenPublish)
  .configure(C.withScala3)
  .settings(
    name := "riddl-language",
    // FIXME: Don't defeat doc generation
    //     See https://github.com/lampepfl/dotty/issues/17577 for details
    Compile / sbt.Keys.doc / sources := Seq(),
    coverageExcludedPackages := "<empty>;.*BuildInfo;.*Terminals",
    libraryDependencies ++= Seq(Dep.fastparse, Dep.lang3, Dep.commons_io) ++
      Dep.testing
  )
  .dependsOn(utils)

val Passes = config("passes")
lazy val passes = project
  .in(file("passes"))
  .configure(C.withCoverage(0))
  .configure(C.mavenPublish)
  .configure(C.withScala3)
  .settings(
    name := "riddl-passes",
    coverageExcludedPackages := "<empty>;.*BuildInfo;.*Terminals",
    libraryDependencies ++= Dep.testing
  )
  .dependsOn(language % "compile->compile;test->test")

val Commands = config("commands")

lazy val commands: Project = project
  .in(file("commands"))
  .configure(C.withScala3)
  .configure(C.withCoverage(0))
  .configure(C.mavenPublish)
  .settings(
    name := "riddl-commands",
    libraryDependencies ++= Seq(Dep.scopt, Dep.pureconfig) ++ Dep.testing
  )
  .dependsOn(
    utils % "compile->compile;test->test",
    passes % "compile->compile;test->test"
  )

val TestKit = config("testkit")

lazy val testkit: Project = project
  .in(file("testkit"))
  .configure(C.withScala3)
  .configure(C.mavenPublish)
  .settings(name := "riddl-testkit", libraryDependencies ++= Dep.testKitDeps)
  .dependsOn(commands % "compile->compile;test->test")

val StatsTrans = config("stats")
lazy val stats: Project = project
  .in(file("stats"))
  .configure(C.withCoverage(0))
  .configure(C.withScala3)
  .configure(C.mavenPublish)
  .settings(name := "riddl-stats", libraryDependencies ++= Seq(Dep.pureconfig) ++ Dep.testing)
  .dependsOn(commands % "compile->compile;test->test", testkit % "test->compile")

val Prettify = config("prettify")
lazy val prettify = project
  .in(file("prettify"))
  .configure(C.withCoverage(0))
  .configure(C.withScala3)
  .configure(C.mavenPublish)
  .settings(name := "riddl-prettify", libraryDependencies ++= Dep.testing)
  .dependsOn(commands, testkit % "test->compile")
  .dependsOn(utils)

val HugoTrans = config("hugo")
lazy val hugo: Project = project
  .in(file("hugo"))
  .configure(C.withCoverage(0))
  .configure(C.withScala3)
  .configure(C.mavenPublish)
  .settings(
    name := "riddl-hugo",
    Compile / unmanagedResourceDirectories += {
      baseDirectory.value / "resources"
    },
    Test / parallelExecution := false,
    libraryDependencies ++= Seq(Dep.pureconfig) ++ Dep.testing
  )
  .dependsOn(passes % "compile->compile;test->test", commands, testkit % "test->compile", stats)

lazy val scaladocSiteProjects = List(
  (utils, Utils),
  (language, Language),
  (passes, Passes),
  (commands, Commands),
  (testkit, TestKit),
  (prettify, Prettify),
  (hugo, HugoTrans),
  (riddlc, Riddlc)
)

lazy val scaladocSiteSettings = scaladocSiteProjects
  .flatMap { case (project, conf) =>
    SiteScaladocPlugin.scaladocSettings(
      conf,
      project / Compile / packageDoc / mappings,
      scaladocDir = s"api/${project.id}"
    )
  }

lazy val doc = project
  .in(file("doc"))
  .enablePlugins(ScalaUnidocPlugin, SitePlugin, SiteScaladocPlugin, HugoPlugin)
  .disablePlugins(ScoverageSbtPlugin)
  .configure(C.withInfo)
  .configure(C.withScala3)
  .settings(scaladocSiteSettings)
  .settings(
    name := "riddl-doc",
    publishTo := Option(Resolver.defaultLocal),
    // Hugo / baseURL := uri("https://riddl.tech"),
    SiteScaladoc / siteSubdirName := "api",
    ScalaUnidoc / unidoc / unidocProjectFilter :=
      inAnyProject -- inProjects(plugin),
    ScalaUnidoc / scalaVersion := (compile / scalaVersion).value,

    /* TODO: Someday, auto-download and unpack to themes/hugo-geekdoc like this:
    mkdir -p themes/hugo-geekdoc/
    curl -L https://github.com/thegeeklab/hugo-geekdoc/releases/latest/download/hugo-geekdoc.tar.gz | tar -xz -C  themes/hugo-geekdoc/ --strip-components=1
     */
    // Hugo / sourceDirectory := sourceDirectory.value / "hugo",
    // siteSubdirName / ScalaUnidoc := "api",
    // (mappings / (
    //   ScalaUnidoc, packageDoc), siteSubdirName in ScalaUnidoc
    // ),
    publishSite
  )
  .dependsOn(hugo % "test->test", riddlc)

val Riddlc = config("riddlc")
lazy val riddlc: Project = project
  .in(file("riddlc"))
  .enablePlugins(JavaAppPackaging, UniversalDeployPlugin)
  .enablePlugins(MiniDependencyTreePlugin, GraalVMNativeImagePlugin)
  .configure(C.withScala3)
  .configure(C.mavenPublish)
  .configure(C.withCoverage(0))
  .dependsOn(
    utils % "compile->compile;test->test",
    commands,
    passes,
    hugo,
    testkit % "test->compile"
  )
  .settings(
    name := "riddlc",
    coverallsTokenFile := Some("/home/reid/.coveralls.yml"),
    mainClass := Option("com.reactific.riddl.RIDDLC"),
    graalVMNativeImageOptions ++= Seq(
      "--verbose",
      "--no-fallback",
      "--native-image-info",
      "--enable-url-protocols=https,http",
      "-H:ResourceConfigurationFiles=../../src/native-image.resources"
    ),
    libraryDependencies ++= Seq(Dep.pureconfig) ++ Dep.testing
  )

lazy val plugin = (project in file("sbt-riddl"))
  .enablePlugins(SbtPlugin, BuildInfoPlugin)
  .disablePlugins(ScoverageSbtPlugin)
  .configure(C.mavenPublish)
  .settings(
    name := "sbt-riddl",
    sbtPlugin := true,
    scalaVersion := "2.12.18",
    Compile / sbt.Keys.doc / sources := Seq(),
    Compile / packageDoc / publishArtifact := false,
    buildInfoObject := "SbtRiddlPluginBuildInfo",
    buildInfoPackage := "com.reactific.riddl.sbt",
    buildInfoOptions := Seq(BuildTime),
    buildInfoUsePackageAsPath := true,
    scriptedLaunchOpts := {
      scriptedLaunchOpts.value ++
        Seq("-Xmx1024M", "-Dplugin.version=" + version.value)
    },
    scriptedBufferLog := false
  )
