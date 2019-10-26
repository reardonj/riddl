resolvers in ThisBuild ++= Seq(
  "Artima Maven Repository" at "https://repo.artima.com/releases"
)

addSbtPlugin("org.scalameta" % "sbt-scalafmt" % "2.0.4")
addSbtPlugin("de.heikoseeberger" % "sbt-header" % "5.2.0")
addSbtPlugin("com.eed3si9n" % "sbt-unidoc" % "0.4.2")
addSbtPlugin("com.dwijnand" % "sbt-dynver" % "4.0.0")
addSbtPlugin("com.typesafe.sbt" % "sbt-site" % "1.4.0")
addSbtPlugin("com.lightbend.paradox" % "sbt-paradox" % "0.6.5")
addSbtPlugin("io.github.jonas" % "sbt-paradox-material-theme" % "0.6.0")
addSbtPlugin("com.wanari" % "sbt-paradox-diagrams" % "0.1.3")
addSbtPlugin("com.lightbend.paradox" % "sbt-paradox-apidoc" % "0.4")
addSbtPlugin("com.eed3si9n" % "sbt-buildinfo" % "0.9.0")
