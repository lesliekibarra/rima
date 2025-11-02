
name := "rima"
version := "0.1"
organization := "edu.uarizona.ece"

scalaVersion := "2.13.10"

libraryDependencies ++= Seq(
  "edu.berkeley.cs" %% "chisel3" % "3.6.1",
  "edu.berkeley.cs" %% "chiseltest" % "0.6.0" % "test",
  "org.scalatest" %% "scalatest" % "3.2.15" % "test"
)

addCompilerPlugin("edu.berkeley.cs" % "chisel3-plugin" % "3.6.1" cross CrossVersion.full)

resolvers ++= Seq(
  Resolver.sonatypeRepo("releases"),
  Resolver.sonatypeRepo("snapshots")
)
