package com.hastybox.pigeoneye.app

case class Config(
                   queryHosts: List[String],
                   successUrl: String,
                   failureUrl: String,
                   pingTimeout: Int,
                   successDelay: Int,
                   failureDelay: Int
                 )
