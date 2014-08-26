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

package service

import play.api.{Logger, Play}
import org.I0Itec.zkclient.ZkClient
import kafka.utils.ZKStringSerializer

trait Zookeeper {
  val zkClient : ZkClient
  def close
}

trait I0ItecZk extends Zookeeper {
  //Zookeeper Configs from application.conf
  val zkHosts = Play.current.configuration.getString("zk.host").get
  val sessionTimeout = Play.current.configuration.getInt("zk.sessionTimeout").get
  val connectionTimeout = Play.current.configuration.getInt("zk.connectionTimeout").get
  val zkClient = {
    Logger.info("creating zk client")
    new ZkClient(zkHosts, sessionTimeout, connectionTimeout, ZKStringSerializer)
  }
  def close = {
    Logger.info("closing zk client")
    zkClient.close()
  }

}

object Zookeeper extends I0ItecZk

