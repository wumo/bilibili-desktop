import com.github.wumo.bilibili.api.API
import com.github.wumo.bilibili.api.API.client
import com.github.wumo.bilibili.api.Login
import com.github.wumo.bilibili.api.Videos
import com.github.wumo.bilibili.util.PersistentCookieStore
import com.github.wumo.bilibili.util.ensureSuccess
import com.github.wumo.bilibili.util.getResp
import com.github.wumo.videoplayer.InputCallback
import com.github.wumo.videoplayer.NativePlayer
import com.github.wumo.videoplayer.NativeVideoPlayer.FrameFetcher.*
import kotlinx.coroutines.runBlocking
import okhttp3.Response
import org.bytedeco.javacpp.BytePointer
import java.io.InputStream

class HttpCallback(val url: String, val aid: String, val size: Long) : InputCallback() {
  lateinit var resp: Response
  lateinit var inputStream: InputStream
  var cur = 0L
  
  init {
    runBlocking {
      ensureSuccess {
        resp = client.getResp(url,
            mapOf("Range" to "bytes=0-",
                "Referer" to "https://www.bilibili.com/video/av$aid"))
        inputStream = resp.body!!.byteStream()
      }
    }
  }
  
  override fun read(buf: BytePointer, buf_size: Int): Int {
    buf.position(0).capacity(buf_size.toLong())
    val bytes = runBlocking {
      ensureSuccess {
        val bytes = inputStream.readNBytes(buf_size)
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
        resp.close()
        runBlocking {
          ensureSuccess {
            resp = client.getResp(
                url,
                mapOf("Range" to "bytes=$offset-",
                    "Referer" to "https://www.bilibili.com/video/av$aid"))
            inputStream = resp.body!!.byteStream()
          }
        }
      }
      seek_cur -> {
        resp.close()
        runBlocking {
          ensureSuccess {
            resp = client.getResp(
                url,
                mapOf("Range" to "bytes=${cur + offset}-",
                    "Referer" to "https://www.bilibili.com/video/av$aid"))
          }
          inputStream = resp.body!!.byteStream()
        }
      }
      seek_end -> {
        resp.close()
        runBlocking {
          ensureSuccess {
            resp = client.getResp(url,
                mapOf("Range" to "bytes=${size - 1}-",
                    "Referer" to "https://www.bilibili.com/video/av$aid"))
            inputStream = resp.body!!.byteStream()
          }
        }
      }
      seek_size -> return size
    }
    return 0
  }
  
  override fun stop() {
    resp.close()
  }
}

fun main() {
  runBlocking {
    API.init(PersistentCookieStore)
    Login.fetchCookie(PersistentCookieStore)
    val aid = "64781860"
    val cid = "112506649"
    val link = Videos.extract(aid, cid)
    val player = NativePlayer(this)
    player.start()
    player.play(HttpCallback(link.videoURL!!, aid, link.videoSize),
        HttpCallback(link.audioURL!!, aid, link.audioSize))
  }
}