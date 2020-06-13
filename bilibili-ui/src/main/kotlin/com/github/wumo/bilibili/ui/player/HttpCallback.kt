package com.github.wumo.bilibili.ui.player

import com.github.wumo.bilibili.api.API.client
import com.github.wumo.bilibili.util.ensureSuccess
import com.github.wumo.bilibili.util.getResp
import com.github.wumo.videoplayer.InputCallback
import com.github.wumo.videoplayer.NativeVideoPlayer.FrameFetcher.*
import kotlinx.coroutines.runBlocking
import okhttp3.Response
import org.bytedeco.javacpp.BytePointer
import java.io.InputStream

class HttpCallback(val url: String, val aid: String, val size: Long) : InputCallback() {
  var resp: Response? = null
  var inputStream: InputStream? = null
  var cur = 0L
  
  private suspend fun reopen(offset: Long) {
    resp?.close()
    ensureSuccess {
      resp = client.getResp(url,
          mapOf("Range" to "bytes=$offset-",
              "Referer" to "https://www.bilibili.com/video/av$aid"))
      inputStream = resp!!.body!!.byteStream()
    }
  }
  
  override fun read(buf: BytePointer, buf_size: Int): Int {
    buf.position(0).capacity(buf_size.toLong())
    val bytes = runBlocking {
      ensureSuccess({
        reopen(cur)
      }) {
        if (inputStream == null) reopen(0)
        val bytes = inputStream!!.readNBytes(buf_size)
        buf.put(*bytes)
        cur += bytes.size
        bytes
      }
    }
    return bytes.size
  }
  
  override fun seek(offset: Long, whence: Int): Long {
    when (whence) {
      seek_set -> {
        cur = offset
        runBlocking {
          reopen(offset)
        }
      }
      seek_cur -> {
        runBlocking {
          reopen(cur + offset)
        }
      }
      seek_end -> {
        runBlocking {
          reopen(size - 1)
        }
      }
      seek_size -> return size
    }
    return 0
  }
  
  override fun stop() {
    resp?.close()
  }
}