package io.hydrosphere.mist.worker

import java.util.concurrent.Executors._

import akka.actor.Actor
import akka.cluster.Cluster
import akka.cluster.ClusterEvent._
import io.hydrosphere.mist.Messages.{ListWorkers, RemoveContext, StopAllContexts, StringMessage}
import io.hydrosphere.mist.{Constants, MistConfig}
import io.hydrosphere.mist.jobs.FullJobConfiguration

import scala.concurrent.ExecutionContext
import scala.util.Random


class CLINode extends Actor {

  val executionContext = ExecutionContext.fromExecutorService(newFixedThreadPool(MistConfig.Settings.threadNumber))

  private val cluster = Cluster(context.system)

  private val serverAddress = Random.shuffle[String, List](MistConfig.Akka.Worker.serverList).head + "/user/" + Constants.Actors.workerManagerName
  private val serverActor = cluster.system.actorSelection(serverAddress)

  val nodeAddress = cluster.selfAddress

  override def preStart(): Unit = {
    cluster.subscribe(self, InitialStateAsEvents, classOf[MemberEvent], classOf[UnreachableMember])
  }

  override def postStop(): Unit = {
    cluster.unsubscribe(self)
  }

  override def receive: Receive = {
    case StringMessage(message) =>
      println(message)
      val killmsg = "kill"
      if(message.contains(killmsg)) {
        serverActor ! new RemoveContext(message.substring(killmsg.length + 1))
      }

    case StopAllContexts =>
      serverActor ! StopAllContexts

    case ListWorkers =>
      serverActor ! ListWorkers

    case MemberUp(member) =>
      if (member.address == cluster.selfAddress) {
      }

    case MemberExited(member) =>
      if (member.address == cluster.selfAddress) {
        cluster.system.shutdown()
      }

    case MemberRemoved(member, prevStatus) =>
      if (member.address == cluster.selfAddress) {
        sys.exit(0)
      }
  }
}
