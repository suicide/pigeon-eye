package com.hastybox.pigeoneye.app

import cats.effect.{ExitCode, IO, IOApp}
import com.hastybox.pigeoneye.core.{SimpleHostPingQueryCreator, SimpleParallelQueryExecutor, SimplePostRequestCreator}
import org.http4s.client.blaze.BlazeClientBuilder
import pureconfig.ConfigReader
import pureconfig.generic.auto._

import scala.concurrent.duration._
import scala.concurrent.{ExecutionContext, ExecutionContextExecutor}

object Main extends IOApp {
  override def run(args: List[String]): IO[ExitCode] = {

    implicit val ec: ExecutionContextExecutor = ExecutionContext.global

    implicit val stringListReader: ConfigReader[List[String]] =
      ConfigReader[String].map(_.split(",").toList)

    for {
      config <- IO(pureconfig.loadConfig[Config]).flatMap {
        case Left(e) => IO.raiseError(new RuntimeException(e.toList.mkString))
        case Right(c) => IO.pure(c)
      }
      ping <- IO(new SimpleHostPingQueryCreator[IO](config.pingTimeout.seconds))
      executor <- IO(new SimpleParallelQueryExecutor[IO](config.successDelay.seconds, config.failureDelay.seconds))
      queries <- IO(config.queryHosts.map(ping.ping))
      res <- BlazeClientBuilder[IO](ec).resource.use(client =>
        for {
          request <- IO(new SimplePostRequestCreator[IO](client))
          _ <- executor.execute(queries,
            request.post(config.successUrl),
            request.post(config.failureUrl))
        } yield ExitCode.Success
      )
    } yield res

  }
}
