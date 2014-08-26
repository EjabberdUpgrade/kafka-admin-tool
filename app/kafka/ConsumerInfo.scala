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
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package kafka

import org.I0Itec.zkclient.ZkClient
import kafka.utils.{Json, ZkUtils, ZKStringSerializer}
import kafka.consumer.SimpleConsumer
import kafka.api.OffsetRequest
import kafka.common.BrokerNotAvailableException
import scala.collection._
import kafka.common.TopicAndPartition
import kafka.api.PartitionOffsetRequestInfo
import play.api.Logger


object ConsumerInfo {

  def collectOffsets(zkClient: ZkClient, group: String): Map[String, List[ConsumerGroupDetails]] = {
      val topicList = ZkUtils.getChildren(zkClient, "/consumers/%s/offsets".format(group)).toList
      val consumerOffsetData: mutable.Map[String, List[ConsumerGroupDetails]] = mutable.Map()
      topicList.sorted.foreach {
        topic => {
          consumerOffsetData.put(topic, collectTopics(zkClient, group, topic))
        }
      }
      consumerOffsetData
  }

  private def collectTopics(zkClient: ZkClient, group: String, topic: String): List[ConsumerGroupDetails] = {
    val pidMap = ZkUtils.getPartitionsForTopics(zkClient, Seq(topic))
    val topicList: List[ConsumerGroupDetails] = pidMap.get(topic) match {
      case Some(partitionId) => {
        var parts: List[ConsumerGroupDetails] = List()
        partitionId.sorted.foreach {
          partitionId => {
            parts :+= collectPartitions(zkClient, group, topic, partitionId)
          }
        }
        parts
      }
      case None => List()
    }
    topicList
  }

  private def collectPartitions(zkClient: ZkClient, group: String, topic: String, pid: Int): ConsumerGroupDetails = {
    val consumerMap: mutable.Map[Int, Option[SimpleConsumer]] = mutable.Map()
    try {
      val offset = ZkUtils.readData(zkClient, "/consumers/%s/offsets/%s/%s".format(group, topic, pid))._1.toLong
      val owner = ZkUtils.readDataMaybeNull(zkClient, "/consumers/%s/owners/%s/%s".format(group, topic, pid))._1

      var partValues: ConsumerGroupDetails = null
      ZkUtils.getLeaderForPartition(zkClient, topic, pid) match {
        case Some(brokerId) =>
          val consumerOpt = consumerMap.getOrElseUpdate(brokerId, createConsumer(zkClient, brokerId))
          consumerOpt match {
            case Some(consumer) =>
              val partitionLatestOffset = fetchPartitionLatestOffset(consumer, topic, pid)
              val lag = partitionLatestOffset - offset
              partValues = new ConsumerGroupDetails(pid, offset, partitionLatestOffset, lag, owner.getOrElse("none"))
              consumer.close()
            case _ => // Nothing
          }
        case None =>
          Logger.error("No broker for partition %s - %s".format(topic, pid))
      }
      partValues
    } finally {
      for (simpleConsumer <- consumerMap.values) {
        simpleConsumer match {
          case Some(consumer) => consumer.close()
          case _ => // Nothing
        }
      }
    }
  }

  private def createConsumer(zkClient: ZkClient, brokerId: Int): Option[SimpleConsumer] = {
    try {
      val (brokerInfo, _) = ZkUtils.readDataMaybeNull(zkClient, ZkUtils.BrokerIdsPath + "/" + brokerId)
      brokerInfo match {
        case Some(brokerInfoString) =>
          parseBrokerJsonToConsumer(brokerInfoString, brokerId)
        case None =>
          throw new BrokerNotAvailableException("Broker id %d does not exist".format(brokerId))
      }
    } catch {
      case t: Throwable =>
        Logger.error("Could not parse broker info", t)
        None
    }
  }

  def parseBrokerJsonToConsumer(brokerInfoString: String, brokerId: Int) = {
    Json.parseFull(brokerInfoString) match {
      case Some(m) =>
        val brokerInfo = m.asInstanceOf[Map[String, Any]]
        val host = brokerInfo.get("host").get.asInstanceOf[String]
        val port = brokerInfo.get("port").get.asInstanceOf[Int]
        Some(new SimpleConsumer(host, port, 10000, 100000, "KwcConsumerInfo"))
      case None =>
        throw new BrokerNotAvailableException("Broker id %d does not exist".format(brokerId))
    }
  }

  def fetchPartitionLatestOffset(consumer: SimpleConsumer, topic: String, pid: Int) = {
    val topicAndPartition = TopicAndPartition(topic, pid)
    val request = OffsetRequest(immutable.Map(topicAndPartition -> PartitionOffsetRequestInfo(OffsetRequest.LatestTime, 1)))
    consumer.getOffsetsBefore(request).partitionErrorAndOffsets(topicAndPartition).offsets.head
  }

}
