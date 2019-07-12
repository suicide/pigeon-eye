package com.hastybox.pigeoneye.core

import java.net.InetAddress

import cats.effect.Sync
import com.hastybox.pigeoneye.core.QueryState.{Failed, Successful}
import com.typesafe.scalalogging.Logger

import scala.concurrent.duration.Duration
import scala.language.higherKinds
import scala.util.Try

object HostPingQueryCreator {
  private val log = Logger[HostPingQueryCreator[Any]]
}

trait HostPingQueryCreator[F[_]] {

  import HostPingQueryCreator._

  implicit def F: Sync[F]

  def timeout: Duration

  def ping(host: String): F[Either[Throwable, QueryState]] = {

    F.delay {
      Try {
        log.debug(s"Prepare to query $host")
        val addr = InetAddress.getByName(host)
        log.debug(s"Pinging $host")
        addr.isReachable(timeout.toMillis.toInt)
      }.toEither
        .map(if (_) {
          log.debug(s"Ping to $host successful")
          Successful
        } else {
          log.debug(s"Ping to $host failed")
          Failed
        })
    }
  }

}

class SimpleHostPingQueryCreator[F[_]](
                                        val timeout: Duration
                                      )(implicit val F: Sync[F]) extends HostPingQueryCreator[F]
