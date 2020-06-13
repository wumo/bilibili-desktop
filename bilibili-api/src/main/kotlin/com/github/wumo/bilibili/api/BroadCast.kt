package com.github.wumo.bilibili.api

import com.github.wumo.bilibili.util.json
import com.github.wumo.bilibili.util.jsonPretty
import kotlinx.io.streams.asInput
import kotlinx.io.streams.asOutput
import kotlinx.serialization.Serializable
import kotlinx.serialization.stringFromUtf8Bytes
import kotlinx.serialization.toUtf8Bytes
import okhttp3.Response
import okhttp3.WebSocket
import okhttp3.WebSocketListener
import okio.ByteString
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream

object Ops {
  val WS_OP_HEARTBEAT = 2
  val WS_OP_HEARTBEAT_REPLY = 3
  val WS_OP_DATA = 1000
  val WS_OP_BATCH_DATA = 9
  val WS_OP_DISCONNECT_REPLY = 6
  val WS_OP_USER_AUTHENTICATION = 7
  val WS_OP_CONNECT_SUCCESS = 8
  val WS_OP_CHANGEROOM = 12
  val WS_OP_CHANGEROOM_REPLY = 13
  val WS_OP_REGISTER = 14
  val WS_OP_REGISTER_REPLY = 15
  val WS_OP_UNREGISTER = 16
  val WS_OP_UNREGISTER_REPLY = 17
  val WS_PACKAGE_HEADER_TOTAL_LENGTH = 18
  val WS_PACKAGE_OFFSET = 0
  val WS_HEADER_OFFSET = 4
  val WS_VERSION_OFFSET = 6
  val WS_OPERATION_OFFSET = 8
  val WS_SEQUENCE_OFFSET = 12
  val WS_COMPRESS_OFFSET = 16
  val WS_CONTENTTYPE_OFFSET = 17
  val WS_BODY_PROTOCOL_VERSION = 1
  val WS_HEADER_DEFAULT_VERSION = 1
  val WS_HEADER_DEFAULT_OPERATION = 1
  val ws_header_default_sequence = 1
  val WS_HEADER_DEFAULT_COMPRESS = 0
  val WS_HEADER_DEFAULT_CONTENTTYPE = 0
}

class Msg(val op: Int = 1, val seq: Int = 1, val msg: String) {
  val headerlen: Short = 18
  val ver: Short = 1
  val compress: Byte = 0
  val contenttype: Byte = 0
  
  fun toByteArray(): ByteArray {
    val content = msg.toUtf8Bytes()
    val size = Int.SIZE_BYTES + headerlen + content.size
    val out = ByteArrayOutputStream(size).also {
      it.asOutput().apply {
        writeInt(size)
        writeShort(headerlen)
        writeShort(ver)
        writeInt(op)
        writeInt(seq)
        writeByte(compress)
        writeByte(contenttype)
        writeFully(content, 0, content.size)
      }.flush()
    }
    out.close()
    return out.toByteArray()
  }
  
  companion object {
//    fun fromByteArray(buf: ByteArray): Msg {
//      ByteArrayInputStream(buf).use {
//        return it.asInput().let { input ->
//          val _headerlen = input.readShort()
//          val _ver = input.readShort()
//          val _op = input.readInt()
//          val _seq = input.readInt()
//          val _compress = input.readByte()
//          val _contenttype = input.readByte()
//          input.readFully(msg, 0, msg.size)
//          Msg(_op, _seq, stringFromUtf8Bytes(msg)
//        }
//      }
//    }
  }
}

class BroadCast(val avid: String, val cid: String): WebSocketListener() {
  private var seq = 1
  
  @Serializable
  data class UserAuthendication(val room_id: String) {
    val platform = "web"
    val accepts = intArrayOf(1000)
  }
  
  override fun onOpen(webSocket: WebSocket, response: Response) {
    println("[Websocket]: On Open.")
    val msg = json.stringify(BroadCast.UserAuthendication.serializer(), BroadCast.UserAuthendication("video://$avid/$cid"))
    val bytes = Msg(Ops.WS_OP_USER_AUTHENTICATION, seq++, msg).toByteArray()
    for(byte in bytes) {
      println(byte)
    }
    val ret = webSocket.send(ByteString.of(*bytes))
    println("send ${if(ret) "successful" else "fail"}")
  }
  
  override fun onClosed(webSocket: WebSocket, code: Int, reason: String) {
    println("closed: code $code, reason:$reason")
  }
  
  override fun onClosing(webSocket: WebSocket, code: Int, reason: String) {
    println("closing: code $code, reason:$reason")
  }
  
  override fun onFailure(webSocket: WebSocket, t: Throwable, response: Response?) {
    t.printStackTrace()
  }
  
  override fun onMessage(webSocket: WebSocket, text: String) {
    println("message: $text")
  }
  
  override fun onMessage(webSocket: WebSocket, bytes: ByteString) {
    println("message: ${bytes.hex()}")
  }
}