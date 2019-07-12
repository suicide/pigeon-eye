package com.hastybox.pigeoneye.core

import scala.concurrent.duration.FiniteDuration
import scala.language.higherKinds

trait QueryExecutor[F[_]] {

  type QueryResult = Either[Throwable, QueryState]
  type ExecuteResult = Either[Throwable, Unit]

  def successDelay: FiniteDuration

  def failureDelay: FiniteDuration

  def execute(queries: List[F[QueryResult]],
              successExecute: F[ExecuteResult],
              failureExecute: F[ExecuteResult]): F[ExecuteResult]

}
