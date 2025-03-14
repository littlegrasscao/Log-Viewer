name         := "LogViewerTool"
organization := "databricks"
version      := "0.1-SNAPSHOT"

scalaVersion := "2.13.12"

libraryDependencies ++= Seq(
  "org.scalafx"   %% "scalafx"   % "21.0.0-R32",
  "org.scalatest" %% "scalatest" % "3.2.17" % "test"
)

// Fork a new JVM for 'run' and 'test:run' to avoid JavaFX double initialization problems
fork := true

// set the main class for the main 'run' task
// change Compile to Test to set it for 'test:run'
Compile / run / mainClass := Some("sun.scalafx.LogViewerApp")