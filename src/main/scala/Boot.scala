package com.ngs.xad.sim

import java.io.{FileWriter, File}

//import scala.concurrent.duration._
import com.typesafe.config.Config
import com.typesafe.config.ConfigFactory
import akka.actor.ActorSystem

import akka.actor.SupervisorStrategy.{Escalate, Resume, Restart}
import akka.actor.{OneForOneStrategy, Actor, Props, ActorRef}
import akka.routing.{RoundRobinRoutingLogic, SmallestMailboxRoutingLogic, Router, ActorRefRoutee}
import com.typesafe.config.ConfigFactory
import scala.collection.immutable.{Nil, List}
import scala.collection.mutable
import scala.io.Source
import scala.util.control.NonFatal
import scala.collection.mutable._
import scala.annotation._

import com.ngs.xad.sim.cfg.AppConfig
import com.ngs.xad.sim.agents._
import com.ngs.xad.sim.agents.XAdProducer
import com.ngs.xad.sim.agents.StartSimulationMsg

import akka.actor.ActorSystem
import com.typesafe.config.ConfigFactory
import scala.collection.mutable.{HashMap, ListBuffer}
import scala.collection.immutable.Map


object XAdApp extends AppConfig {

  def main(args: Array[String]): Unit = {

    val system = ActorSystem("xAdSystem")
    val supervisor = system.actorOf(XAdProducer.props, name = "xAdProducer")

    supervisor ! StartSimulationMsg
  }
}
