# Kafka Web Console

The Kafka Web Console is an admin tool for managing your [Kafka](http://kafka.apache.org/) cluster. It provides a quick
and easy way to administer your Kafka cluster. The console allows you to create/delete topics, and find
information about your cluster, including detailed information about topics, brokers, and consumers.

The goal of the console is to simplify Kafka administration, and make it easy for those just getting started with
Kafka to get up and running quickly. The console organizes and consolidates information about the
Kafka cluster and presents it as a set of easy to understand web pages.


The Kafka Web Console was built for Kafka v0.8, as a stand alone web application written in Scala using the
[Play Framework](http://www.playframework.com/).


## Quick Start


1. Install [sbt](http://www.scala-sbt.org/)

2. Clone the project
```
	$ git clone git@github.com:netossd/KafkaWebConsole.git
	$ cd KafkaWebConsole/
```
3. Run project, override zookeeper location (if not localhost:2181)
```
	$KafkaWebConsole> sbt run -Dzk.host=<zookeeper.host>:<zookeeper.port>
```
4. Navigate browser to Console
 [localhost:9000](http://localhost:9000)


## Development
The 'master' branch is considered the latest stable code, all development is done on the 'develop' branch. Pull
requests or feature branches should be made off of 'develop' branch. Releases will be tagged with the version
number, we will be using [semantic versioning](http://semver.org/) of the form MAJOR.MINOR.PATCH

1. Install the [Play Framework v2.0.4](http://www.playframework.com/download)
2. Clone the repo:$ git clone git@github.com:netossd/KafkaWebConsole.git KafkaWebConsole
3. Change directories into the project home: $ cd KafkaWebConsole
4. Switch to development branch:$ git checkout develop
5. Start play console:$ play
6. Run the application (it will start on localhost:9000 by default):$ run



### Build

To build a binary version of the application:

```
$ git clone git@github.com:netossd/KafkaWebConsole.git
$ cd KafkaWebConsole/
$ play dist
```


Bugs and Feedback
----------------
For bugs, suggestions, or feedback please use [Github Issues](https://github.com/netossd/KafkaWebConsole/issues).

License
-------
Copyright 2014 Comcast Cable Communications Management, LLC

Licensed under the Apache License, Version 2.0 (the "License");
you may not use this file except in compliance with the License.
You may obtain a copy of the License at

<http://www.apache.org/licenses/LICENSE-2.0>

Unless required by applicable law or agreed to in writing, software
distributed under the License is distributed on an "AS IS" BASIS,
WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
See the License for the specific language governing permissions and
limitations under the License.
