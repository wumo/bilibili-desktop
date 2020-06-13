package com.github.wumo.bilibili.api

import com.github.wumo.bilibili.api.API.client
import com.github.wumo.bilibili.api.Bangumis.BangumiType.Comic
import com.github.wumo.bilibili.api.Bangumis.BangumiType.TV
import com.github.wumo.bilibili.api.Cache.cacheFavMedias
import com.github.wumo.bilibili.api.Cache.cachedMedias
import com.github.wumo.bilibili.model.Folder
import com.github.wumo.bilibili.model.FolderType.SubscriptionFolder
import com.github.wumo.bilibili.model.MediaPage
import com.github.wumo.bilibili.model.MediaResource
import com.github.wumo.bilibili.model.MediaType.Bangumi
import com.github.wumo.bilibili.model.PageLink
import com.github.wumo.bilibili.util.*
import kotlinx.serialization.json.content
import kotlinx.serialization.json.int
import java.time.Instant

object Bangumis {
  enum class BangumiType(val id: String, val title: String) {
    Comic("comic", "追番"), TV("tv", "追剧"),
  }
  
  private val bangumURL = url("https", "api.bilibili.com", "x/space/bangumi")
  private val webURL = url("https", "api.bilibili.com", "pgc/web")
  private val playerURL = url("https", "api.bilibili.com", "pgc/player/web")
  suspend fun getBangumiFolders(mid: String): List<Folder> =
    ensureSuccess() {
      val comicSize = Bangumis.getMediasCount(mid, 1)
      val tvSize = Bangumis.getMediasCount(mid, 2)
      return listOf(Folder(SubscriptionFolder, mid, Comic.id, Comic.title, comicSize, false),
                    Folder(SubscriptionFolder, mid, TV.id, TV.title, tvSize, false))
    }
  
  /**
   * page++获取知道获取到空列表为止
   */
  suspend fun fetchMedias(folder: Folder, page: Int, tid: Int, order: Int) =
    cacheFavMedias.getOrPut(CachedMediasKey(folder.id, page, tid, order)) {
      when(folder.fid) {
        Comic.id -> Bangumis.getMedias(folder.mid, 1, page, order)
        TV.id -> Bangumis.getMedias(folder.mid, 2, page, order)
        else -> emptyList()
      }
    }!!
  
  private val orders = listOf("0", "1", "2", "3")
  private suspend fun getMedias(mid: String, type: Int, page: Int, order: Int): List<MediaResource> =
    ensureSuccess() {
      val result = client.get(
          Bangumis.bangumURL.url("follow/list", "type", type,
                        "vmid", mid,
                        "pn", page + 1,
                        "ps", MaxFavResourcePerPage,
                        "follow_status", Bangumis.orders[order],
                        "ts", Instant.now().toEpochMilli()),
          headers("Referer", "https://space.bilibili.com/$mid/bangumi")
      )
        .json()
      if(result["code"].int == 53013) return emptyList()
      val data = result["data"]
      if(data.isNull || data["total"].int == 0) return emptyList()
      data["list"].jsonArray.map {
        val media_id = it["media_id"].content
        val id = it["season_id"].content
        cachedMedias.getOrPut(id) {
          MediaResource(id,
                        Bangumi,
                        it["title"].content,
                        it["cover"].content,
                        0,
                        it["evaluate"].content, "", "",
                        it["stat", "view"].int)
        }
      }
    }
  
  private suspend fun getMediasCount(mid: String, type: Int): Int {
    ensureSuccess() {
      val result = client.get(
          Bangumis.bangumURL.url("follow/list", "type", type,
                        "vmid", mid, "pn", 1, "ps", 1, "follow_status", 0,
                        "ts", Instant.now().toEpochMilli()),
          headers("Referer", "https://space.bilibili.com/$mid/bangumi")
      ).json()
      if(result["code"].int == 53013) return 0
      val data = result["data"]
      if(data.isNull) return 0
      return data["total"].int
    }
  }
  
  suspend fun getEpisodes(sid: String, title: String): List<MediaPage> {
    ensureSuccess() {
      val result = client.get(
          Bangumis.webURL.url("season/section", "season_id", sid),
          headers("Referer", "https://www.bilibili.com/bangumi/play/ss$sid/")
      ).json().check()
      var pIdx = 0
      return result["result", "main_section", "episodes"].jsonArray.map { ep ->
        MediaPage(Bangumi, pIdx++, sid,
                  ep["cid"].content,
                  "第${ep["title"].content}话 ${ep["long_title"].content}",
                  title,
                  ep["aid"].content,
                  ep["id"].content)
      }
    }
  }
  
  suspend fun extract(avid: String, cid: String, eid: String): PageLink {
    ensureSuccess() {
      assert(Login.buvid3.isNotEmpty()) { "buvid3 shouldn't be empty, please login!" }
      val session = "${Login.buvid3}${Instant.now().toEpochMilli()}".md5()
      val result = client.get(
          Bangumis.playerURL.url("playurl",
                        "avid", avid, "cid", cid, "session", session,
                        "qn", 120, "otype", "json",
                        "fourk", 1, "fnver", 0, "fnval", 16),
          headers("Referer", "https://www.bilibili.com/bangumi/play/ep$eid/")
      ).json().check()
      result["result"].let { data ->
        val currentQuality = data["quality"].int
        val bestQuality = data["accept_quality"].jsonArray.map { it.int }.max()!!
        if(data.contains("dash"))
          data["dash"].let { streams ->
            val video = streams["video"].jsonArray[0]
            val videoUrl = video["baseUrl"].content
            val audio = streams["audio"].jsonArray[0]
            val audioUrl = audio["baseUrl"].content
            val videoHeaders = ensureSuccess {
              client.getHeaders(
                  videoUrl, mapOf("Range" to "bytes=0-",
                                  "Referer" to "https://www.bilibili.com/bangumi/play/ep$avid"))
            }
            val videoSize = videoHeaders["Content-Length"]!!.toLong()
            val audioHeaders = ensureSuccess {
              client.getHeaders(
                  audioUrl, mapOf("Range" to "bytes=0-",
                                  "Referer" to "https://www.bilibili.com/bangumi/play/ep$avid/"))
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
                           "Referer" to "https://www.bilibili.com/bangumi/play/ep$avid/"))
          }
          val size = videoHeaders["Content-Length"]!!.toLong()
          return PageLink(currentQuality, bestQuality,
                          size, url, 0L, null, true)
        }
      }
    }
  }
  
  suspend fun getAllSeasons(media_id: String): List<MediaResource> =
    ensureSuccess() {
      val result = client.get(
          url("https", "api.bilibili.com", "pgc/view/web/media",
              "media_id", media_id),
          headers("Referer", "https://www.bilibili.com/bangumi/media/md$media_id")
      ).json().check()
      val lists = mutableListOf<MediaResource>()
      val data = result["result"]
      val coverUrl = data["cover"].content
      val seasons = data["seasons"].jsonArray
      if(seasons.isEmpty())
        lists += MediaResource(data["season_id"].content,
                               Bangumi,
                               data["title"].content,
                               coverUrl)
      else
        seasons.forEach { season ->
          lists += MediaResource(season["season_id"].content,
                                 Bangumi,
                                 season["title"].content,
                                 coverUrl)
        }
      lists
    }
}