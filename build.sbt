//lazy val scala211 = "2.11.12"
//lazy val scala212 = "2.12.13"
lazy val scala213 = "2.13.5"
lazy val scala3 = "3.0.0"

// val versionsBase   = Seq(scala212, scala211, scala213)
val versionsBase   = Seq(scala213)
val versionsJVM    = versionsBase //:+ scala300
val versionsJS     = versionsJVM
val versionsNative = versionsBase

ThisBuild / scalaVersion := scala213

inThisBuild(
  List(
    description := "A port of the Java API for XML processing (JAXP), targeting Scala.js and Scala Native",
    organization := "com.github.david-bouyssie",
    homepage := Some(url("https://github.com/david-bouyssie/xml4s/")),
    licenses := List(
      "Apache2" -> url("https://github.com/david-bouyssie/xml4s/blob/master/LICENSE.txt")
    ),
    developers := List(
      Developer(
        id = "david-bouyssie",
        name = "David Bouyssie",
        email = "",
        url = url("http://github.com/david-bouyssie/")
      )
    )
  )
)

lazy val root = (project in file("."))
  .settings(
    name := "xml4s-root",
    crossScalaVersions := Nil,
    doc / aggregate := false
    // FIXME
    /*,
    doc := (xml4sSaxJS / Compile / doc).value,
    packageDoc / aggregate := false,
    packageDoc := (xml4sSaxJS / Compile / packageDoc).value*/
  )
  .settings(skipPublish: _*)
  .aggregate(
    xml4sCommonJS,
    xml4sCommonNative,
    //xml4sDomJS,
    //xml4sDomNative,
    xml4sSaxJS,
    xml4sSaxNative,
    xml4sXmlPullJS,
    xml4sXmlPullJVM,
    xml4sXmlPullNative,
    // TODO: stax, trax, validation, xml4sXmlPull
    testSuiteJVM,
    testSuiteJS,
    testSuiteNative
  )


val commonSettings: Seq[Setting[_]] = Seq(
  version := "0.1.0",
  // scalacOptions ++= Seq("-unchecked", "-deprecation", "-feature", "-Xsource:3")
  scalacOptions ++= Seq("-deprecation", "-feature")
)

val modulesCommonSettings: Seq[Setting[_]] = commonSettings ++ Seq(
  Test / test := {},
  Compile / packageBin / mappings ~= {
    _.filter(!_._2.endsWith(".class"))
  },
  // FIXME: enable me when we need to split scala 2/3 source code
  //sharedScala2or3Source
)

val modulesNativeSettings: Seq[Setting[_]] = Seq(
  Compile / run := {},
  crossScalaVersions := versionsNative,
  logLevel := Level.Info // Info or Debug
)

lazy val xml4sCommonAPI = crossProject(JSPlatform, JVMPlatform, NativePlatform)
  .crossType(CrossType.Full)
  .in(file("common"))
  .settings(modulesCommonSettings)
  .settings(name := "xml4s-common-api")
  .jsSettings(crossScalaVersions := versionsJS)
  .nativeSettings(modulesNativeSettings)

lazy val xml4sCommonJS     = xml4sCommonAPI.js
lazy val xml4sCommonNative = xml4sCommonAPI.native

/*lazy val xml4sDom = crossProject(JSPlatform, NativePlatform)
  .crossType(CrossType.Full)
  .in(file("dom"))
  .settings(modulesCommonSettings)
  .jsSettings(crossScalaVersions := versionsJS)
  .jsConfigure(_.dependsOn(xml4sSaxJS))
  .nativeSettings(modulesNativeSettings)
  .nativeConfigure(_.dependsOn(xml4sSaxNative))

lazy val xml4sDomJS     = xml4sDom.js
lazy val xml4sDomNative = xml4sDom.native*/

lazy val xml4sSax = crossProject(JSPlatform, NativePlatform)
  .crossType(CrossType.Full)
  .in(file("sax"))
  .settings(modulesCommonSettings)
  .settings(name := "xml4s-sax-api")
  .dependsOn(xml4sCommonAPI)
  .jsSettings(crossScalaVersions := versionsJS)
  .nativeSettings(modulesNativeSettings)

lazy val xml4sSaxJS     = xml4sSax.js
lazy val xml4sSaxNative = xml4sSax.native

/*lazy val xml4sStax = crossProject(JSPlatform, NativePlatform)
  .crossType(CrossType.Full)
  .in(file("stax"))
  .settings(modulesCommonSettings)
  .jsSettings(crossScalaVersions := versionsJS)
  .jsConfigure(_.dependsOn(xml4sCommonJS))
  .nativeSettings(modulesNativeSettings)
  .nativeConfigure(_.dependsOn(xml4sCommonNative))

lazy val xml4sStaxJS     = xml4sStax.js
lazy val xml4sStaxNative = xml4sStax.native*/

/*lazy val xml4sTrax = crossProject(JSPlatform, NativePlatform)
  .crossType(CrossType.Full)
  .in(file("trax"))
  .settings(modulesCommonSettings)
  .jsSettings(crossScalaVersions := versionsJS)
  .jsConfigure(_.dependsOn(xml4sCommonJS))
  .nativeSettings(modulesNativeSettings)
  .nativeConfigure(_.dependsOn(xml4sCommonNative))

lazy val xml4sTraxJS     = xml4sTrax.js
lazy val xml4sTraxNative = xml4sTrax.native*/

/*lazy val xml4sValidation = crossProject(JSPlatform, NativePlatform)
  .crossType(CrossType.Full)
  .in(file("validation"))
  .settings(modulesCommonSettings)
  .jsSettings(crossScalaVersions := versionsJS)
  .jsConfigure(_.dependsOn(xml4sCommonJS))
  .nativeSettings(modulesNativeSettings)
  .nativeConfigure(_.dependsOn(xml4sCommonNative))

lazy val xml4sValidationJS     = xml4sValidation.js
lazy val xml4sValidationNative = xml4sValidation.native*/

lazy val xml4sXmlPull = crossProject(JSPlatform, JVMPlatform, NativePlatform)
  .crossType(CrossType.Full)
  .in(file("xmlpull"))
  .settings(modulesCommonSettings)
  .settings(name := "xml4s-xmlpull-api")
  .dependsOn(xml4sCommonAPI)
  .jsSettings(crossScalaVersions := versionsJS)
  .jsConfigure(_.dependsOn(xml4sSaxJS))
  .nativeSettings(modulesNativeSettings)
  .nativeConfigure(_.dependsOn(xml4sSaxNative))

lazy val xml4sXmlPullJS     = xml4sXmlPull.js
lazy val xml4sXmlPullJVM    = xml4sXmlPull.jvm
lazy val xml4sXmlPullNative = xml4sXmlPull.native

lazy val testSuite = crossProject(JSPlatform, JVMPlatform, NativePlatform)
  .in(file("tests"))
  .settings(commonSettings: _*)
  .settings(skipPublish: _*)
  .settings(
    testOptions += Tests.Argument(TestFrameworks.JUnit, "-a", "-s", "-v"),
    scalacOptions += "-target:jvm-1.8",
    libraryDependencies += "org.scala-lang.modules" %%% "scala-xml" % "2.0.0"
  )
  .jvmSettings(
    name := "xml4s testSuite on JVM",
    crossScalaVersions := versionsJVM,
    libraryDependencies +=
      "com.novocode" % "junit-interface" % "0.11" % Test
  )
  .jvmConfigure(_.dependsOn(xml4sXmlPullJVM))
  .jsSettings(
    name := "xml4s testSuite on JS",
    crossScalaVersions := versionsJS,
    scalaJSLinkerConfig ~= (_.withModuleKind(ModuleKind.CommonJSModule))
  )
  .jsConfigure(_.enablePlugins(ScalaJSJUnitPlugin))
  .jsConfigure(_.dependsOn(xml4sSaxJS))
  .nativeSettings(
    name := "xml4s testSuite on Native",
    crossScalaVersions := versionsNative,
    // Set to false or remove if you want to show stubs as linking errors
    nativeLinkStubs := true,
    nativeMode := "debug", // "debug","release-fast","release"
    nativeLTO := "thin",   // "none","thin"
    nativeGC := "immix",   // "none","boehm","commix","immix"
    addCompilerPlugin(
      "org.scala-native" % "junit-plugin" % nativeVersion cross CrossVersion.full
    ),
    libraryDependencies += "org.scala-native" %%% "junit-runtime" % nativeVersion
  )
  .nativeConfigure(_.dependsOn(xml4sXmlPullNative))

lazy val testSuiteJS     = testSuite.js
lazy val testSuiteNative = testSuite.native
lazy val testSuiteJVM    = testSuite.jvm

val skipPublish = Seq(
  // no artifacts in this project
  publishArtifact := false,
  // make-pom has a more specific publishArtifact setting already
  // so needs specific override
  makePom / publishArtifact := false,
  // no docs to publish
  packageDoc / publishArtifact := false,
  publish / skip := true
)
