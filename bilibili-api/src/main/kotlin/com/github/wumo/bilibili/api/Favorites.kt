package com.github.wumo.bilibili.api

import com.github.wumo.bilibili.api.API.client
import com.github.wumo.bilibili.api.Cache.cacheFavMedias
import com.github.wumo.bilibili.api.Cache.cachedMedias
import com.github.wumo.bilibili.model.Folder
import com.github.wumo.bilibili.model.FolderType.FavoriteFolder
import com.github.wumo.bilibili.model.MediaResource
import com.github.wumo.bilibili.model.MediaType
import com.github.wumo.bilibili.util.*
import kotlinx.serialization.json.content
import kotlinx.serialization.json.int
import kotlinx.serialization.json.long

object Favorites {
  private val favURL = url("https", "api.bilibili.com", "x/v3/fav")
  
  suspend fun getCreatedFolders(mid: String): List<Folder> =
    ensureSuccess() {
      val result = client.get(
          Favorites.favURL.url("folder/created/list-all",
                     "up_mid", mid, "jsonp", "jsonp"),
          headers("Referer", "https://space.bilibili.com/$mid/favlist")
      ).json().check()
      val data = result["data"]
      if(data.isNull || data["count"].int == 0) return emptyList()
      return data["list"].jsonArray.map {
        Folder(FavoriteFolder, mid, it["id"].content, it["title"].content, it["media_count"].int,
               it["attr"].int != 2)
      }
    }
  
  suspend fun getCollectedFolders(mid: String): List<Folder> {
    val folders = mutableListOf<Folder>()
    var page = 1
    var totalPages = Int.MAX_VALUE
    while(page < totalPages) {
      ensureSuccess() {
        val result = client.get(
            Favorites.favURL.url("folder/collected/list",
                       "pn", page.toString(),
                       "ps", MaxFavResourcePerPage.toString(),
                       "up_mid", mid, "jsonp", "jsonp"),
            headers("Referer", "https://space.bilibili.com/$mid/favlist")
        ).json().check()
        val data = result["data"]
        if(data.isNull || data["count"].int == 0) return folders
        totalPages = data["count"].int.pagesNeeded
        data["list"].jsonArray.forEach {
          val valid = it["state"].int == 0
          if(!valid) return@forEach
          folders += Folder(FavoriteFolder, mid,
                            it["id"].content,
                            it["title"].content,
                            it["media_count"].int,
                            it["attr"].int != 2)
        }
        page++
      }
    }
    return folders
  }
  
  suspend fun partitionOptions(folder: Folder): List<TidOption> =
    ensureSuccess() {
      val result = client.get(
          Favorites.favURL.url("resource/partition", "up_mid", folder.mid,
                     "media_id", folder.fid, "jsonp", "jsonp"),
          headers("Referer", "https://space.bilibili.com/${folder.mid}/favlist")
      ).json().check()
      val list = mutableListOf<TidOption>()
      list += TidOption("全部", 0, 0)
      var total = 0
      result["data"].jsonArray.forEach { entry ->
        val count = entry["count"].int
        total += count
        list += TidOption(entry["name"].content, entry["tid"].int, count)
      }
      list[0] = TidOption("全部", 0, total)
      return list
    }
  
  private val orders = listOf("mtime", "view", "pubtime")
  
  suspend fun fetchMedias(folder: Folder, page: Int, tid: Int, order: Int): List<MediaResource> =
    cacheFavMedias.getOrPut(CachedMediasKey(folder.fid, page, tid, order)) {
      ensureSuccess() {
        val result = client.get(
            Favorites.favURL.url("resource/list", "media_id", folder.fid,
                       "pn", "${page + 1}", "ps", MaxFavResourcePerPage.toString(),
                       "tid", tid.toString(),
                       "order", Favorites.orders[order]),
            headers("Referer", "https://www.bilibili.com/medialist/detail/ml${folder.fid}?type=1")
        ).json().check()
        val data = result["data"]
        val list = data["medias"]
        if(list.isNull || data["info", "media_count"].int.pagesNeeded < page + 1)
          return emptyList()
        return list.jsonArray.map { entry ->
          val id = entry["id"].content
          cachedMedias.getOrPut(id) {
            val upper = entry["upper"]
            MediaResource(id,
                          MediaType.parse(entry["type"].int),
                          entry["title"].content,
                          entry["cover"].content,
                          entry["fav_time"].long,
                          entry["intro"].content,
                          upper["mid"].content,
                          upper["name"].content,
                          entry["cnt_info", "play"].int,
                          entry["attr"].int == 0)
          }
        }
      }
    }
}