import java.util.UUID

import akka.actor.{Actor, ActorSelection, ActorSystem, Props}
import akka.actor.Actor.Receive
import com.typesafe.config.ConfigFactory
import akka.actor.{Actor, ActorRef, ActorSelection, ActorSystem, Props}
import com.typesafe.config.ConfigFactory

import scala.concurrent.duration._
import scala.io.Source

/**
  * Created by zhang on 2017/12/14.
  */
class Worker(val masterHost: String, val masterPort: Int, val memory: Int, cores: Int) extends Actor {
  var master: ActorSelection = _
  val WORKER_ID = "My name is 涂兆国"

  override def preStart(): Unit = {

    master = context.actorSelection(s"akka.tcp://MasterSystem@$masterHost:$masterPort/user/Master")
    master ! RegisterWorker(WORKER_ID, memory, cores)
  }

  override def receive: Receive = {
    case "reply" => {
      println("链接开始工作")
    }

    case RegisteredWorker => {
      //Worker启动一个定时器，定期向Master发送心跳
      //利用akka启动一个定时器,自己给自己发消息
      import context.dispatcher
      context.system.scheduler.schedule(0 millis, 10000 millis, self, SendHeartbeat)

    }

    case SendHeartbeat => {
      //执行判断逻辑
      //向Master发送心跳
      master ! Heartbeat(WORKER_ID)

    }

    case Task(file) => {

      //读取文件，然后进行单词计数
      val wordAndCounts: Map[String, Int] = Source.fromFile(file).getLines()
        .toList
        .flatMap(_.split(" "))
        .map((_, 1))
        .groupBy(_._1)
        .mapValues(_.length)

      //将结果返回给发送者
      sender() ! Result(wordAndCounts.toSeq)
    }
  }
}

  object Worker {

    def main(args: Array[String]): Unit = {
      val host = args(0)
      val port = args(1).toInt
      val masterHost = args(2)
      val masterPort = args(3).toInt
      val memory = args(4).toInt
      val cores = args(5).toInt
      val configStr =
        s"""
           |akka.actor.provider = "akka.remote.RemoteActorRefProvider"
           |akka.remote.netty.tcp.hostname = "$host"
           |akka.remote.netty.tcp.port = "$port"
         """.stripMargin

      val config = ConfigFactory.parseString(configStr)
      //ActorSystem老大，辅助创建和监控下面的Actor，他是单例的
      val actorSystem = ActorSystem("WorkerSystem", config)
      //创建Actor
      val workerRef = actorSystem.actorOf(Props(new Worker(masterHost, masterPort, memory, cores)), "Worker")
      actorSystem.awaitTermination()
    }

}