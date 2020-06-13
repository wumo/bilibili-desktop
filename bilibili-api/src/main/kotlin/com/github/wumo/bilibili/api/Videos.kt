package com.github.wumo.bilibili.api

import com.github.wumo.bilibili.api.API.client
import com.github.wumo.bilibili.model.MediaPage
import com.github.wumo.bilibili.model.MediaType
import com.github.wumo.bilibili.model.PageLink
import com.github.wumo.bilibili.util.*
import kotlinx.serialization.json.content
import kotlinx.serialization.json.int
import java.time.Instant

object Videos {
  fun Int.qualityDesc() =
      when (this) {
        116 -> "1080P60"
        112 -> "1080P+"
        80 -> "1080P"
        74 -> "720P60"
        64 -> "720P"
        32 -> "480P"
        16 -> "360P"
        else -> ""
      }
  
  private val baseURL = url("https", "api.bilibili.com", "x/player")
  
  suspend fun getPageList(aid: String, title: String): List<MediaPage> {
    ensureSuccess() {
      val result = client.get(
          baseURL.url("pagelist", "aid", aid, "jsonp", "jsonp")
      ).json().check()
      return result["data"].jsonArray.map { page ->
        MediaPage(MediaType.Video, page["page"].int, aid, page["cid"].content, page["part"].content, title)
      }
    }
  }
  
  suspend fun extract(avid: String, cid: String): PageLink =
//    ensureSuccess() {
      run {
        assert(Login.buvid3.isNotEmpty()) { "buvid3 shouldn't be empty, please login!" }
        val session = "${Login.buvid3}${Instant.now().toEpochMilli()}".md5()
        val result = client.get(
            baseURL.url("playurl",
                "avid", avid, "cid", cid, "session", session,
                "qn", "120", "fourk", "1", "otype", "json", "fnver", "0", "fnval", "16"),
            headers("Referer", "https://www.bilibili.com/video/av$avid")
        ).json().check()
        result["data"].let { data ->
          val currentQuality = data["quality"].int
          val bestQuality = data["accept_quality"].jsonArray.map { it.int }.max()!!
          if (data.contains("dash"))
            data["dash"].let { streams ->
              val video = streams["video"].jsonArray[0]
              val videoUrl = video["baseUrl"].content
              val audio = streams["audio"].jsonArray[0]
              val audioUrl = audio["baseUrl"].content
              val videoHeaders = ensureSuccess {
                client.getHeaders(
                    videoUrl, mapOf("Range" to "bytes=0-",
                    "Referer" to "https://www.bilibili.com/video/av$avid"))
              }
              val videoSize = videoHeaders["Content-Length"]!!.toLong()
              val audioHeaders = ensureSuccess {
                client.getHeaders(
                    audioUrl, mapOf("Range" to "bytes=0-",
                    "Referer" to "https://www.bilibili.com/video/av$avid"))
              }
              val audioSize = audioHeaders["Content-Length"]!!.toLong()
              return PageLink(currentQuality, bestQuality,
                  videoSize, videoUrl, audioSize, audioUrl, false
              )
            }
          else {
            val url = data["durl"].jsonArray[0]["url"].content
            val videoHeaders = ensureSuccess {
              client.getHeaders(
                  url, mapOf("Range" to "bytes=0-",
                  "Referer" to "https://www.bilibili.com/video/av$avid"))
            }
            val size = videoHeaders["Content-Length"]!!.toLong()
            return PageLink(currentQuality, bestQuality,
                size, url, 0L, null, true)
          }
        }
      }
}