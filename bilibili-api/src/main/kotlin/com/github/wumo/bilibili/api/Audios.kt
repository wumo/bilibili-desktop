package com.github.wumo.bilibili.api

import com.github.wumo.bilibili.api.API.client
import com.github.wumo.bilibili.model.PageLink
import com.github.wumo.bilibili.util.*
import kotlinx.serialization.json.content
import kotlinx.serialization.json.long

object Audios {
  private val baseURL = url("https", "www.bilibili.com", "audio/music-service-c/web")
  
  suspend fun extract(sid: String): PageLink =
    ensureSuccess() {
      val result = client.get(
          Audios.baseURL.url("url", "sid", sid, "privilege", "2", "quality", "2"),
          headers("Referer", "https://www.bilibili.com/audio/au$sid?type=3")
      ).json().check()
      val data = result["data"]
      val audioSize = data["size"].long
      val url = data["cdns"].jsonArray[0].content
      return PageLink(2, 2, 0L, null, audioSize, url, true)
    }
}