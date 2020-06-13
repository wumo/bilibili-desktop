package com.github.wumo.bilibili.api

import com.github.wumo.bilibili.api.API.client
import com.github.wumo.bilibili.api.BroadCast.UserAuthendication
import com.github.wumo.bilibili.util.*
import io.ktor.client.HttpClient
import io.ktor.client.features.logging.DEFAULT
import io.ktor.client.features.logging.LogLevel
import io.ktor.client.features.logging.Logger
import io.ktor.client.features.logging.Logging
import io.ktor.client.features.websocket.WebSockets
import io.ktor.client.features.websocket.ws
import io.ktor.http.cio.websocket.Frame
import io.ktor.http.cio.websocket.readBytes
import io.ktor.http.cio.websocket.readText
import io.ktor.http.cio.websocket.send
import kotlinx.serialization.json.content
import kotlinx.serialization.json.float
import kotlinx.serialization.json.int
import java.util.zip.GZIPInputStream
import java.util.zip.Inflater
import java.util.zip.InflaterInputStream

object Danmaku {
  suspend fun fetch(cid: String): ByteArray =
    ensureSuccess {
      val result = client.getResp(
          "https://api.bilibili.com/x/v1/dm/list.so?oid=$cid"
      )
      val encoding = result.headers["Content-Encoding"]
      
      result.body?.use {
        return when(encoding) {
          "deflate" -> InflaterInputStream(it.byteStream(), Inflater(true))
            .readAllBytes()
          "gzip" -> GZIPInputStream(it.byteStream()).readAllBytes()
          else -> it.bytes()
        }
      }
      return ByteArray(0)
    }
  
  val ktorClient = HttpClient {
    install(WebSockets)
    install(Logging){
      logger= Logger.DEFAULT
      level=LogLevel.ALL
    }
  }
  
  suspend fun broadcast(avid: String, cid: String) =
    ensureSuccess {
      
      val result = client.get("https://api.bilibili.com/x/web-interface/broadcast/servers?platform=web")
        .json().check()
      val data = result["data"]
      val domain = data["domain"].content
      val tcp_port = data["tcp_port"].int
      val ws_port = data["ws_port"].int
      val wss_port = data["wss_port"].int
      val heartbeat = data["heartbeat"].int
      val nodes = data["nodes"].jsonArray.map { it.content }
      val backoff = data["backoff"]
      val max_delay = backoff["max_delay"].int
      val base_delay = backoff["base_delay"].int
      val factor = backoff["factor"].float
      val jitter = backoff["jitter"].float
      val heartbeat_max = data["heartbeat_max"].int
      
      val handler = BroadCast(avid, cid)
      Danmaku.ktorClient.ws("wss://$domain:$wss_port/sub") {
        val msg = json.stringify(UserAuthendication.serializer(), UserAuthendication("video://$avid/$cid"))
        val bytes = Msg(Ops.WS_OP_USER_AUTHENTICATION, 1, msg).toByteArray()
        send(bytes)
        val frame = incoming.receive()
        when(frame) {
          is Frame.Binary -> println(frame.readBytes())
          is Frame.Text -> println(frame.readText())
          is Frame.Close -> println("close")
          is Frame.Ping -> println("ping")
          is Frame.Pong -> println("pong")
        }
      }
//      client.wss("wss://$domain:$wss_port/sub", listener = BroadCast(avid, cid))
    }
}