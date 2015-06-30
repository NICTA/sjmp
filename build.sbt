
name := "sjmp"
organization := "com.n1analytics"
version := "0.0.0"
libraryDependencies ++= Seq(
  "com.novocode" % "junit-interface" % "0.11" % Test
)

// Enable assertions
fork in run := true
//javaOptions in run += "-ea"
//javaOptions in run += "-XX:+UnlockDiagnosticVMOptions"
//javaOptions in run += "-XX:+PrintAssembly"

//javaOptions ++= Seq("-XX:+UnlockDiagnosticVMOptions", "-XX:+LogCompilation", "-XX:LogFile='.'", "-XX:+PrintAssembly")
javacOptions ++= Seq("-source", "1.8", "-target", "1.8")

EclipseKeys.executionEnvironment := Some(EclipseExecutionEnvironment.JavaSE18)
EclipseKeys.projectFlavor := EclipseProjectFlavor.Java
