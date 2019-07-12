package com.hastybox.pigeoneye.core

import cats.effect.{ContextShift, Effect, IO, Timer}
import cats.instances.list._
import cats.syntax.all._
import com.hastybox.pigeoneye.core.QueryState.{Failed, Successful, Undefined}
import com.typesafe.scalalogging.Logger

import scala.concurrent.ExecutionContext
import scala.concurrent.duration.FiniteDuration
import scala.language.higherKinds

object ParallelQueryExecutor {
  private val log: Logger = Logger[ParallelQueryExecutor[Any]]
}

trait ParallelQueryExecutor[F[_]] extends QueryExecutor[F] {

  import ParallelQueryExecutor._

  implicit def F: Effect[F]

  implicit def ec: ExecutionContext

  implicit lazy val timer: Timer[IO] = IO.timer(ec)
  implicit lazy val cs: ContextShift[IO] = IO.contextShift(ec)

  override def execute(queries: List[F[QueryResult]],
                       successExecute: F[ExecuteResult],
                       failureExecute: F[ExecuteResult]): F[ExecuteResult] = {

    def doExecute(previousState: QueryState): F[ExecuteResult] = {

      val queryResult = for {
        r <- F.liftIO(queries.map(F.toIO).parSequence)
      } yield {
        r.reduceLeft((a, b) => (a, b) match {
          case (Left(e), _) => Left(e)
          case (_, Left(e)) => Left(e)
          case (Right(x), Right(y)) => if (x == Successful || y == Successful) Right(Successful) else Right(Failed)
        })
      }

      for {
        r <- queryResult
        ee <- r match {
          case Left(e) => F.pure(Left(e).asInstanceOf[ExecuteResult]) <* F.delay(log.error("Aborting due to error", e))
          case Right(state) => {
            val d = delay(state)
            ((state match {
                case _ if previousState == state => F.pure(Right(()).asInstanceOf[ExecuteResult]) <* F.delay(log.debug(s"No state change from $previousState"))
                case Successful => F.delay(log.info(s"State changed to $state")) *> successExecute
                case Failed => F.delay(log.info(s"State changed to $state")) *> failureExecute
              })
                <* d) *> doExecute(state)
          }
        }
      } yield ee
    }

    doExecute(Undefined)
  }

  private def delay(state: QueryState) = {
    val duration = if (state == Successful) {
      successDelay
    } else {
      failureDelay
    }

    F.delay(log.debug(s"Sleeping for $duration")) *> F.liftIO(IO.sleep(duration))
  }
}

class SimpleParallelQueryExecutor[F[_]](
                                         val successDelay: FiniteDuration,
                                         val failureDelay: FiniteDuration
                                       )(implicit val F: Effect[F],
                                         implicit val ec: ExecutionContext) extends ParallelQueryExecutor[F]
