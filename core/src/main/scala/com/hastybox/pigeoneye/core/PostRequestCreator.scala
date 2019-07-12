package com.hastybox.pigeoneye.core

import cats.effect.Sync
import cats.syntax.all._
import com.typesafe.scalalogging.Logger
import org.http4s._
import org.http4s.client.Client
import org.http4s.client.dsl.Http4sClientDsl
import org.http4s.dsl.io._

import scala.language.higherKinds

object PostRequestCreator {
  private val log = Logger[PostRequestCreator[Any]]
}

trait PostRequestCreator[F[_]] extends Http4sClientDsl[F] {

  import PostRequestCreator._

  implicit def F: Sync[F]

  def httpClient: Client[F]

  def post(url: String): F[Either[Throwable, Unit]] = {

    for {
      uri <- F.delay(Uri.fromString(url))
      _ <- F.delay(log.debug(s"Posting to $uri"))
      result <- uri match {
        case Left(e) => F.pure[Either[Throwable, Unit]](Left(e))
        case Right(u) =>
          val req = POST(UrlForm(), u)
          httpClient.fetch[Either[Throwable, Unit]](req)(res => if (res.status.isSuccess) {
            F.delay(log.debug(s"Post request successful")) *>
              F.pure(Right(()))
          } else {
            F.delay(log.error(s"Post request failed $res")) *>
              F.pure(Left(new RuntimeException("failed")))
          })
      }

    } yield result
  }

}

class SimplePostRequestCreator[F[_]](
                                      val httpClient: Client[F]
                                    )(
                                      implicit val F: Sync[F]
                                    ) extends PostRequestCreator[F]
