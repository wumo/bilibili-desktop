package com.github.wumo.bilibili.api

import kotlinx.coroutines.delay
import kotlinx.coroutines.runBlocking
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import java.net.CookieHandler
import java.net.CookieStore

fun main() {
  API.init()
  runBlocking {
   val wss= Danmaku.broadcast("87584483", "149642097")
    delay(100000000)
    "0, 0, 0, 92, 0, 18, 0, 1, 0, 0, 0, 7, 0, 0, 0, 4, 0, 0, 123, 34, 114, 111, 111, 109, 95, 105, 100, 34, 58, 34, 118, 105, 100, 101, 111, 58, 47, 47, 56, 55, 53, 56, 52, 52, 56, 51, 47, 49, 52, 57, 54, 52, 50, 48, 57, 55, 34, 44, 34, 112, 108, 97, 116, 102, 111, 114, 109, 34, 58, 34, 119, 101, 98, 34, 44, 34, 97, 99, 99, 101, 112, 116, 115, 34, 58, 91, 49, 48, 48, 48, 93, 125"
  }
//  API.close()
}
