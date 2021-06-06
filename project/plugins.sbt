// versions
val crossProjectVersion = "1.0.0"
val scalaJSVersion      = "1.5.1"
val scalaNativeVersion  = "0.4.0"

// Scala.js support
addSbtPlugin("org.portable-scala" % "sbt-scalajs-crossproject"      % crossProjectVersion)
addSbtPlugin("org.scala-js"       % "sbt-scalajs"                   % scalaJSVersion)

// Scala Native support
addSbtPlugin("org.portable-scala" % "sbt-scala-native-crossproject" % crossProjectVersion)
addSbtPlugin("org.scala-native"   % "sbt-scala-native"              % scalaNativeVersion)

// SBT CI release (includes sbt-dynver, sbt-pgp, sbt-sonatype and sbt-git)
addSbtPlugin("com.geirsson" % "sbt-ci-release" % "1.5.7")