package com.github.wumo.bilibili.util

import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.channels.Channel.Factory.UNLIMITED

object LogService {
  var isLogged = true
  val logQueue = Channel<String>(UNLIMITED)
  inline fun log(msg: () -> Any) {
    if(isLogged)
      logQueue.offer("[${printNow()}] ${msg()}")
  }
}