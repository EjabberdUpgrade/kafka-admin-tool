/**
 * Copyright 2014 Comcast Cable Communications Management, LLC
 *
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */


import sbt._
import Keys._
import PlayProject._
import sbt.ExclusionRule

object ApplicationBuild extends Build {

  val appName         = "KafkaWebConsole"
  val appVersion      = "0.1-SNAPSHOT"

  val appDependencies = Seq(
    "org.apache.kafka" % "kafka_2.9.1" % "0.8.0" exclude ("jline", "jline") exclude("junit", "junit") exclude("org.slf4j", "slf4j-simple"),
    "log4j" % "log4j" % "1.2.17"
  )

  val main = PlayProject(appName, appVersion, appDependencies, mainLang = SCALA).settings(
    // add apache repo, to get 'good' kafka 0.8.0 beta release
    resolvers += "apache repository" at "https://repository.apache.org/content/repositories/releases"
  )

}
