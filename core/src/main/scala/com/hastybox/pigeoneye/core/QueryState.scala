package com.hastybox.pigeoneye.core

sealed trait QueryState

object QueryState {
  case object Undefined extends QueryState
  case object Successful extends QueryState
  case object Failed extends QueryState
}
