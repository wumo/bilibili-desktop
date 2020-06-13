package com.github.wumo.bilibili.util

import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.delay
import java.io.ByteArrayOutputStream
import java.io.PrintStream
import java.nio.charset.Charset
import java.util.concurrent.RejectedExecutionException

suspend inline fun <T> ensureSuccess(onError: (Exception) -> Unit = {}, block: () -> T): T {
  var i = 1000L
  var j = 0
  while (true)
    try {
      return block()
    } catch (e: Exception) {
      when (e) {
        is CancellationException -> throw e
        else -> {
          onError(e)
          e.printStackTrace()
          i *= 2
          println("error reschedule in $i s")
          delay(i)
          if (j++ >= 5)
            throw  e
        }
      }
    }
  
}

inline fun Exception.print(title: () -> String = { "" }): String {
  printStackTrace()
  val bout = ByteArrayOutputStream()
  bout.use {
    printStackTrace(PrintStream(bout, true, Charset.defaultCharset()))
  }
  return "[${title()}]\n\terror: $message ${bout.toString(Charset.defaultCharset())}"
}