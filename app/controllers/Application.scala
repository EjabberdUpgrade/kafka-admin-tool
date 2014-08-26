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

package controllers

import play.api._
import play.api.mvc._
import kafka.ConsumerInfo
import service.{Zookeeper, Admin, Kafka}
import play.api.data._
import play.api.data.Forms._

object Application extends Controller {
  val admin = new Admin(Zookeeper.zkClient)
  val kafka = new Kafka(Zookeeper.zkClient)

  /**
   * Main console page, displays cluster overview
   */
  def index = Action { implicit request =>
    val brokers = kafka.listBrokers
    val zookeepers = kafka.listZookeepers
    Ok(views.html.index(brokers, zookeepers))
  }

  /**
   * List of consumers with overview
   */
  def consumers = Action { implicit request =>
    val consumers = kafka.listConsumerGroups

    Ok(views.html.consumerGroups("cg", "Consumer Groups", consumers))
  }

  /**
   * List topics
   */
  def topics = Action { implicit request =>
    val topics = kafka.listTopics
    Ok(views.html.topics(topics))
  }

  /**
   * Details about a specific consumer group
   */
  def consumerDetails(groupName: String) = Action { implicit request =>
    val zkHosts = Play.current.configuration.getString("zk.host").get
    val cOffsets = ConsumerInfo.collectOffsets(Zookeeper.zkClient, groupName)
    val allConsumers = kafka.listConsumersInGroups(groupName)
    Ok(views.html.consumerDetails(groupName, allConsumers, cOffsets))
  }

  /**
   * Redistribute partition leaders to preferred broker location
   */
  def rebalance() = Action { implicit request =>

    admin.rebalancePartitions()
    Redirect("/")
  }

  /**
   * Admin page
   */
  def adminCommands() = Action { implicit request =>
    Ok(views.html.admin())
  }

  /**
   * Create a new topic on the cluster
   */
  def createTopic = Action { implicit request =>
    val (name, partitions, replica) = createTopicForm.bindFromRequest.get
    try {
      admin.createTopic(name, partitions, replica)
      val success= "Created topic: '" + name + "'"
      Redirect("/topics").flashing(
        "success" -> success
      )
    }
    catch {
      case e =>
        Logger.error("could not create topic", e)
        Redirect("/topics").flashing(
          "error" -> ("Error: " + e.getMessage)
        )
    }

  }

  /**
   * Delete a topic from the cluster
   */
  def deleteTopic = Action { implicit request =>
    val name  = deleteTopicForm.bindFromRequest.get
    try {
      admin.deleteTopic(name)
      val success= "Deleted topic: '" + name + "'"
      Redirect("/topics").flashing(
        "success" -> success
      )
    }
    catch {
      case e =>
        Logger.error("could not delete topic", e)
        Redirect("/topics").flashing(
          "error" -> ("Error: " + e.getMessage)
        )
    }
  }

  def resetOffset = Action { implicit request =>
    val(group, topic, pid, offset) = resetOffsetForm.bindFromRequest.get
    try {
      Logger.info("resetting " + group)
      kafka.resetConsumerGroupOffset(group, topic, pid, offset)

      Redirect("/consumer/details/" + group).flashing(
        "success" -> "Reset Successful"
      )
    }
    catch {
      case e =>
        Logger.error("could not reset consumer", e)
        Redirect("/consumers").flashing(
          "error" -> ("Error: " + e.getMessage)
        )
    }
  }
  /**
   * Display about screen
   */
  def about() = Action { implicit request =>
    Ok(views.html.about())
  }

  /**
   * Form for create topic
   */
  val createTopicForm = Form(
    tuple(
      "name" -> text,
      "partitions" -> number,
      "replica" -> number
    )
  )

  /**
   * Form for delete topic
   */
  val deleteTopicForm = Form(
    single(
      "name" -> text
    )
  )

  val resetOffsetForm = Form(
    tuple(
      "group" -> text,
      "topic" -> text,
      "pid" -> number,
      "newOffset" -> number(min = 0)

    )
  )

}

