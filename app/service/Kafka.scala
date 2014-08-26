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

import org.I0Itec.zkclient.ZkClient
import kafka.utils.ZkUtils
import scala.collection.mutable.ListBuffer
import kafka.{BrokerNode, Topic}
import play.api.{Play, Logger}

class Kafka(zkClient: ZkClient) {

  def listTopics : Seq[Topic] = {

    var topicSeq = new ListBuffer[Topic]()
    try {
      var topicList: Seq[String] = Nil

      topicList = ZkUtils.getChildrenParentMayNotExist(zkClient, ZkUtils.BrokerTopicsPath).sorted

      if (topicList.size <= 0)
        Logger.warn("no topics exist")
      else
        Logger.info("Found Topics:" + topicList.size)

      val brokerMap: Map[Int, String] = ZkUtils.getAllBrokersInCluster(zkClient).map(a => a.id -> (a.host + ":" + a.port) ).toMap

      for (topic <- topicList) {
        topicSeq ++= showTopic(zkClient, topic, brokerMap)
      }
    }
    catch {
      case e : Throwable =>
        Logger.error("list topic failed", e)
    }
    topicSeq
  }

  def showTopic(zkClient: ZkClient, topic: String, brokerMap: Map[Int, String]) : ListBuffer[Topic] = {
    var topicObj = new ListBuffer[Topic]()
    ZkUtils.getPartitionAssignmentForTopics(zkClient, List(topic)).get(topic) match {
      case Some(topicPartitionAssignment) =>
        val sortedPartitions = topicPartitionAssignment.toList.sortWith((m1, m2) => m1._1 < m2._1)
        for ((partitionId, assignedReplicas) <- sortedPartitions) {
          val inSyncReplicas = ZkUtils.getInSyncReplicasForPartition(zkClient, topic, partitionId)
          val leader = ZkUtils.getLeaderForPartition(zkClient, topic, partitionId)
          val leaderNode =  brokerIdToHost(leader.getOrElse(-99), brokerMap)
          val replicas =  assignedReplicas.map( a => brokerIdToHost(a, brokerMap)).mkString(",")
          val isr = inSyncReplicas.map( a => brokerIdToHost(a, brokerMap)).mkString(",")
          topicObj += new Topic(topic, partitionId.toString, leaderNode, replicas, isr)
        }
      case None =>
        Logger.warn("topic " + topic + " doesn't exist!")
    }
    topicObj
  }

  def brokerIdToHost(brokerId: Int, brokerMap: Map[Int, String]): String = {
    brokerMap.getOrElse(brokerId, "None")
  }

  def listZookeepers : Seq[String] =  {
    Play.current.configuration.getString("zk.host").get.split(",")
  }

  def listBrokers : Seq[BrokerNode] =  {
    var brokers: Seq[BrokerNode] = Seq.empty[BrokerNode]
    try {
      brokers = ZkUtils.getAllBrokersInCluster(zkClient).map( s => BrokerNode(s.id, s.host, s.port)).toSeq
    }
    catch {
      case e : Throwable =>
        Logger.error("list brokers failed", e)
    }
    brokers
  }

  def listConsumerGroups : Seq[String] = {
    var consumers: Seq[String] = Seq.empty[String]
    try {
      consumers = ZkUtils.getChildrenParentMayNotExist(zkClient, ZkUtils.ConsumersPath)
    }
    catch {
      case e : Throwable =>
        Logger.error("list consumers", e)
    }
    consumers
  }

  def listConsumersInGroups(groupName: String) : Seq[String] = {
    var consumers: Seq[String] = Seq.empty[String]
    try {
      consumers = ZkUtils.getConsumersInGroup(zkClient,groupName)
    }
    catch {
      case e : Throwable =>
        Logger.error("list consumers in group failed", e)
    }
    consumers
  }

  def resetConsumerGroupOffset(group: String, topic: String, pid: Int, newOffset: Int) = {
    ZkUtils.updatePersistentPath(zkClient, "/consumers/%s/offsets/%s/%s".format(group, topic, pid), newOffset.toString)
  }
}

