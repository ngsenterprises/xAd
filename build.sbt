name := "xAd"

version := "1.0.0"

scalaVersion := "2.11.8"

resolvers ++= Seq(
  "Maven Releases" at "http://repo.typesafe.com/typesafe/maven-releases",
  "Typesafe Repository" at "http://repo.typesafe.com/typesafe/releases/"
)

libraryDependencies ++= {
  val akkaVersion       = "2.4.6"
  val akkaStreamVersion = "2.0.1"
  val scalaTestVersion  = "2.2.4"
  Seq(
    "joda-time"                    % "joda-time"                         % "2.9.1",
    "ch.qos.logback"               % "logback-classic"                   % "1.1.3",
    "com.typesafe.scala-logging"  %% "scala-logging"                     % "3.1.0",
    "com.typesafe.akka"           %% "akka-actor"                        % akkaVersion,
    "com.typesafe.akka"           %% "akka-slf4j"                        % akkaVersion,
    "com.typesafe.akka"           %% "akka-testkit"                      % akkaVersion % "test",
    "com.typesafe.akka"           %% "akka-stream-experimental"          % akkaStreamVersion,
    "com.typesafe.akka"           %% "akka-http-core-experimental"       % akkaStreamVersion,
    "com.typesafe.akka"           %% "akka-http-experimental"            % akkaStreamVersion,
    "com.typesafe.akka"           %% "akka-http-spray-json-experimental" % akkaStreamVersion,
    "com.typesafe.akka"           %% "akka-http-testkit-experimental"    % akkaStreamVersion,
    "org.scalatest"               %% "scalatest"                         % scalaTestVersion  % "test",
    "com.typesafe"                % "config"                             % "1.3.0"
  )
}


fork in run := true