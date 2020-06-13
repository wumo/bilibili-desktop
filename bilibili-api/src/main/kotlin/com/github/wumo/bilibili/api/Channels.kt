package com.github.wumo.bilibili.api

import com.github.wumo.bilibili.api.API.client
import com.github.wumo.bilibili.api.Cache.cacheFavMedias
import com.github.wumo.bilibili.api.Cache.cachedMedias
import com.github.wumo.bilibili.model.Folder
import com.github.wumo.bilibili.model.FolderType.ChannelFolder
import com.github.wumo.bilibili.model.MediaResource
import com.github.wumo.bilibili.model.MediaType
import com.github.wumo.bilibili.util.*
import kotlinx.serialization.json.content
import kotlinx.serialization.json.int
import kotlinx.serialization.json.long

object Channels {
  
  private val channelURL = url("https", "api.bilibili.com", "x/space/channel")
  suspend fun getChannelFolders(mid: String): List<Folder> =
    ensureSuccess() {
      val result = client.get(Channels.channelURL.url("list", "mid", mid, "guest", "false", "jsonp", "jsonp"),
                              headers("Referer", "https://space.bilibili.com/$mid/channel/index")
      ).json().check()
      val data = result["data"]
      if(data.isNull || data["count"].int == 0) return emptyList()
      return data["list"].jsonArray.map { ch ->
        Folder(ChannelFolder, ch["mid"].content, ch["cid"].content,
               ch["name"].content, ch["count"].int, false)
      }
    }
  
  private val orders = listOf("0", "1")
  suspend fun fetchMedias(folder: Folder, page: Int, tid: Int, order: Int): List<MediaResource> =
    cacheFavMedias.getOrPut(CachedMediasKey(folder.fid, page, tid, order)) {
      ensureSuccess() {
        val result = client.get(
            Channels.channelURL.url(
                "video", "mid", folder.mid, "cid", folder.fid,
                "pn", "${page + 1}", "ps", MaxFavResourcePerPage.toString(),
                "order", Channels.orders[order], "jsonp", "jsonp"),
            headers("Referer",
                    "https://space.bilibili.com/${folder.mid}/channel/detail?cid=${folder.fid}")
        ).json().check()
        val list = result["data", "list"]
        val count = list["count"].int
        val archives = list["archives"].jsonArray
        if(count == 0 || archives.isEmpty()) return emptyList()
        return list["archives"].jsonArray.map { video ->
          val upper = video["owner"]
          val id = video["aid"].content
          cachedMedias.getOrPut(id) {
            MediaResource(id, MediaType.Video,
                          video["title"].content,
                          video["pic"].content,
                          video["ctime"].long,
                          video["desc"].content,
                          upper["mid"].content,
                          upper["name"].content,
                          video["stat", "view"].int,
                          true
            )
          }
        }
      }
    }
}