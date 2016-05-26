package com.ngs.xad.sim.agents

package object Common {

  val SUCCESS  = 0
  val CONTINUE = 1
  val TIMEOUT  = 2
  val FAILURE  = 3

}

import scala.collection.immutable.HashMap

import akka.actor.Actor

import com.ngs.xad.sim.cfg.AllConfigs

import java.io.{FileWriter, File}
import javax.security.auth.login.Configuration

import akka.actor.SupervisorStrategy._
import akka.actor.{OneForOneStrategy, Actor, Props, ActorRef}

import akka.actor.Props
import akka.actor._
import akka.routing.{RoundRobinRoutingLogic, SmallestMailboxRoutingLogic, Router, ActorRefRoutee}
import akka.actor.ActorSystem._


import com.typesafe.config.ConfigFactory
import scala.collection.immutable.{Nil, List, Map}
import scala.collection.mutable
import scala.io.Source
import scala.util.control.NonFatal
import scala.collection.mutable._
import scala.annotation._
import scala.concurrent.duration._

//import scala.concurrent.duration.Duration
//import scala.concurrent.duration.TimeUnit
import akka.actor.Cancellable

import java.io.{ File, FileWriter }
//import org.joda.time.{ Duration, DateTime }
import akka.actor._
import akka.actor.ActorLogging
import akka.actor.SupervisorStrategy._
import akka.actor.SupervisorStrategy.restart
import akka.routing.{ ActorRefRoutee, Router, RoundRobinRoutingLogic }

import scala.util.control.NonFatal
import scala.collection.mutable.{ ListBuffer, Queue }
import scala.util.parsing.json._
import scala.util.{ Try, Success, Failure }

case class Tweak(id:Long, name:String, text:String, timestamp_ms:Long)

sealed trait XAdMsg
case object StartSimulationMsg extends XAdMsg
case object StopSimulationMsg extends XAdMsg
case class InitConsumerMsg(id: Int) extends XAdMsg
case object StopConsumerMsg extends XAdMsg
case object ScheduleClockMsg extends XAdMsg
case class StartJobMsg(data: Tweak) extends XAdMsg
case class EndJobMsg(id: Int, elapsedTime: Long, bytesRead: Long, status: Int) extends XAdMsg


class Stats {
  var id:Long = 0L
  var elapsedTime: Long = 0L
  var bytesRead: Long = 0L
  var status = Common.CONTINUE
}

object XAdProducer {
  def props = Props(classOf[XAdProducer])
}//--------------

class XAdProducer extends Actor with AllConfigs {

  import context._

  val Rnd = new scala.util.Random

  var schClock: Option[Cancellable] = None
  var activeConsumers = 0
  var startTime:Long = 0L
  val charStor = ('a' to 'z') ++ ('A' to 'Z')

  private var nextTweakId:Long = 0L
  private var nextOccurrence:Int = 0

  val jobQue = new ListBuffer[Tweak]
  val consumers = new ArrayBuffer[Stats]

  def randomWord(rnd: scala.util.Random): String= {
    (1 to appMinWord +rnd.nextInt(appMaxWord)).foldLeft(new StringBuilder) { (ac, j) => {
      ac += charStor(rnd.nextInt(charStor.length))
      ac += ' '
      ac
    }}.toString
  }

  def randomText(rnd: scala.util.Random): String = {
    val minWords = rnd.nextInt(appMinText)
    val maxWords = minWords +rnd.nextInt(appMaxText)
    (minWords to maxWords).foldLeft(new StringBuilder) { (ac, i) => {
      ac ++= randomWord(rnd)
      if (nextTweakId == nextOccurrence && !appTokenNever) {
        ac ++= "xAd "
        incrementNextOccurrence
      }
      ac
    }}.toString
  }

  def getNextTweakId: Long = {
    nextTweakId += 1
    nextTweakId
  }

  def incrementNextOccurrence: Int = {
    nextOccurrence += Rnd.nextInt(appTokOcurrence)
    nextOccurrence
  }


  def shutDownSystemResources = {
    //println("shutDownSystem")
    schClock.map( _.cancel)
    router.routees.foreach( ar => {
      ar.send(StopConsumerMsg, context.self)
    })
  }

  def printStatus(stats: Stats): Unit = stats.status match {
    case Common.SUCCESS =>
      print(s"time elapsed: ${stats.elapsedTime} ms, \t\t")
      print(s"bytecount: ${stats.elapsedTime}, \t\t")
      println(s"status : SUCCESS")
    case Common.TIMEOUT =>
      print(s"time elapsed: ${0} ms, \t\t")
      print(s"bytecount: ${0}, \t\t")
      println(s"status : TIMEOUT")
    case Common.FAILURE =>
      print(s"time elapsed: ${0} ms, \t\t")
      print(s"bytecount: ${0}, \t\t")
      println(s"status : FAILURE")
    case Common.CONTINUE =>
      print(s"time elapsed: ${0} ms, \t\t")
      print(s"bytecount: ${0}, \t\t")
      println(s"status : TIMEOUT")
  }

  def receive = {

    //INITIAL START SYSTEM
    case StartSimulationMsg => {
      val orgSender = sender
      //println(s"XAdProducer: StartSimulationMsg ${ts}")

      startTime = System.currentTimeMillis
      incrementNextOccurrence

      schClock = Some(context.system.scheduler.schedule(
        Duration(0, MILLISECONDS), Duration(500, MILLISECONDS), context.self, ScheduleClockMsg))

      router.routees.foreach( ar => {
        ar.send(InitConsumerMsg(activeConsumers), context.self)
        val stat = new Stats
        stat.id = activeConsumers
        consumers.append(stat)

        val tweak = Tweak(getNextTweakId, randomWord(Rnd), randomText(Rnd), System.currentTimeMillis)
        ar.send(StartJobMsg(tweak), context.self)
        activeConsumers += 1
      })
    }//..........................

    //HANDLE CLOCK TICK MESSAGES
    case ScheduleClockMsg => {
      //println("XAdProducer: ScheduleClockMsg")
      if (appTimeout <= System.currentTimeMillis -startTime) {
        //println(s"XAdProducer: ScheduleClockMsg ${} ${}")
        shutDownSystemResources
      }
      else
        jobQue += Tweak(getNextTweakId, randomWord(Rnd), randomText(Rnd), System.currentTimeMillis)
    }//.....................

    case EndJobMsg(id: Int, elapsed: Long, bytes: Long, status: Int) => {
      val orgSender = sender
      //println(s"XAdProducer: EndJobMsg ${id} ${elapsed} ${bytes} ${status}")

      if (status == Common.SUCCESS) {
        //println("SUCCESS")
        consumers(id).bytesRead += bytes
        consumers(id).elapsedTime += elapsed
        consumers(id).status = Common.SUCCESS

        sender ! StopConsumerMsg
      }
      else if (status == Common.TIMEOUT) {
        //println("TIMEOUT")
        consumers(id).bytesRead = 0
        consumers(id).elapsedTime = 0
        consumers(id).status = Common.TIMEOUT

        sender ! StopConsumerMsg
      }
      else if (status == Common.FAILURE) {
        //println("FAILURE")
        consumers(id).bytesRead = 0
        consumers(id).elapsedTime = 0
        consumers(id).status = Common.FAILURE

        sender ! StopConsumerMsg
      }
      else {
        //println(s"CONTINUE ${jobQue.length} activeConsumers ${activeConsumers}")
        consumers(id).bytesRead += bytes
        consumers(id).elapsedTime += elapsed

        if (0 < activeConsumers) {
          if (jobQue.length <= 0)
            orgSender ! StartJobMsg(Tweak(getNextTweakId, randomWord(Rnd), randomText(Rnd), System.currentTimeMillis))
          else
            orgSender ! StartJobMsg(jobQue.remove(0))
        }
      }
    }

    case StopSimulationMsg => {
      //println("XAdProducer StopSimulationMsg: " +(System.currentTimeMillis -startTime))
      consumers.map( e =>
        e.elapsedTime = if (e.status != Common.SUCCESS) 0 else e.elapsedTime
      )
      consumers.sortBy( _.elapsedTime).reverse.foreach( e => printStatus(e) )
      val sc = consumers.filter( _.status == Common.SUCCESS)
      val avg_bytes_ms =
        sc.foldLeft(0.0){ (ac, stat) => ac +(stat.bytesRead.toDouble/stat.elapsedTime.toDouble) }
      println(s"avg bytes/ms: ${avg_bytes_ms}")

      System.exit(0)
    }

    case StopConsumerMsg => {
      val orgSender = sender
      //println("XAdProducer: StopConsumerMsg")
      //sender ! Kill
      activeConsumers -= 1
      if (activeConsumers <= 0)
        context.self ! StopSimulationMsg
    }

  }//.............

  override def supervisorStrategy = {
    OneForOneStrategy() { case _ => Escalate }
  }//....................

  val router = {
    //println("routees maxActors " +maxActors)
    val consumers = Vector.fill(maxActors) {
      val r = context.actorOf(XAdConsumer.props)
      context.watch(r)
      ActorRefRoutee(r)
    }
    Router(RoundRobinRoutingLogic(), consumers)
  }//........................

}//------------------



object XAdConsumer {
  def props = Props(classOf[XAdConsumer])
}//-----------------------

class XAdConsumer extends Actor with AllConfigs {

  import context._
  var id = -1

  def receive = {

    case InitConsumerMsg(pid: Int) => {
      //println(s"XAdConsumer: InitConsumerMsg ${pid} ${timeSpan}")
      val orgSender = sender
      id = pid
    }

    case StartJobMsg(data: Tweak) => {
      //println(s"XAdConsumer: StartJobMsg ${id}")
      val orgSender = sender

      val start_time = System.currentTimeMillis
      val res = data.text.indexOf("xAd")
      val elapsed_time = System.currentTimeMillis -start_time
      if (res < 0)
        orgSender ! EndJobMsg(id, elapsed_time, data.text.length, Common.CONTINUE)
      else
        orgSender ! EndJobMsg(id, elapsed_time, res +1, Common.SUCCESS)
    }

    case StopConsumerMsg => {
      //println(s"XAdConsumer: StopConsumerMsg ${id}")
      val orgSender = sender
      sender ! StopConsumerMsg
    }

  }
}//-------------------------------



