package com.github.wumo.bilibili.api

import com.github.wumo.bilibili.api.API.client
import com.github.wumo.bilibili.api.Cache.cacheFavMedias
import com.github.wumo.bilibili.api.Cache.cachedMedias
import com.github.wumo.bilibili.api.Contributes.ContributeType.*
import com.github.wumo.bilibili.model.Folder
import com.github.wumo.bilibili.model.FolderType.ContributeFolder
import com.github.wumo.bilibili.model.MediaResource
import com.github.wumo.bilibili.model.MediaType
import com.github.wumo.bilibili.util.*
import kotlinx.serialization.json.content
import kotlinx.serialization.json.int
import kotlinx.serialization.json.intOrNull
import kotlinx.serialization.json.long

object Contributes {
  enum class ContributeType(val id: String, val title: String) {
    Video("video", "视频"), Audio("audio", "音频"),
    Article("article", "专栏"), Album("album", "相簿")
  }
  
  private val spaceURL = url("https", "api.bilibili.com", "x/space")
  suspend fun getContributeFolders(mid: String): List<Folder> =
    ensureSuccess() {
      val result = client.get(Contributes.spaceURL.url("navnum", "mid", mid, "jsonp", "jsonp"),
                              headers("Referer", "https://space.bilibili.com/$mid/video")
      ).json().check()
      result["data"].let {
        val video = it["video"].int
        val audio = it["audio"].int
        val article = it["article"].int
        val album = it["album"].int
        return listOf(Folder(ContributeFolder, mid, Video.id, Video.title, video, false),
                      Folder(ContributeFolder, mid, Audio.id, Audio.title, audio, false),
                      Folder(ContributeFolder, mid, Article.id, Article.title, article, false),
                      Folder(ContributeFolder, mid, Album.id, Album.title, album, false))
      }
    }
  
  suspend fun partitionOptions(folder: Folder): List<TidOption> {
    return when(folder.fid) {
      Video.id -> ensureSuccess() {
        val result = client.get(
            Contributes.spaceURL.url("arc/search",
                         "mid", folder.mid, "pn", "1", "ps", "1",
                         "tid", "0", "order", Contributes.videoOrders[0],
                         "jsonp", "jsonp"),
            headers("Referer", "https://space.bilibili.com/${folder.mid}/video")
        ).json().check()
        val data = result["data"]
        val list = mutableListOf<TidOption>()
        list += TidOption("全部", 0, data["page", "count"].int)
        data["list", "tlist"].jsonObject.content.forEach { tid, entry ->
          list += TidOption(entry["name"].content, tid.toInt(), entry["count"].int)
        }
        return list
      }
      else -> emptyList()
    }
  }
  
  suspend fun fetchMedias(folder: Folder, page: Int, tid: Int, order: Int) =
    cacheFavMedias.getOrPut(CachedMediasKey(folder.id, page, tid, order)) {
      when(folder.fid) {
        Video.id -> Contributes.getVideos(folder, page, tid, order)
        Audio.id -> Contributes.getAudios(folder, page, order)
        else -> emptyList()
      }
    }
  
  private val videoOrders = listOf("pubdate", "click", "stow")
  private suspend fun getVideos(folder: Folder, page: Int, tid: Int, order: Int): List<MediaResource> =
    ensureSuccess() {
      val result = client.get(
          Contributes.spaceURL.url("arc/search",
                       "mid", folder.mid,
                       "pn", "${page + 1}",
                       "ps", MaxFavResourcePerPage.toString(),
                       "tid", tid.toString(),
                       "order", Contributes.videoOrders[order], "jsonp", "jsonp"),
          headers("Referer", "https://space.bilibili.com/${folder.mid}/video")
      ).json().check()
      val data = result["data"]
      if(data.isNull || data["page", "count"].int == 0) return emptyList()
      return data["list", "vlist"].jsonArray.map {
        val id = it["aid"].content
        cachedMedias.getOrPut(id) {
          MediaResource(id,
                        MediaType.Video,
                        it["title"].content,
                        "https:${it["pic"].content}",
                        it["created"].long,
                        it["description"].content,
                        it["mid"].content,
                        it["author"].content,
                        it["play"].intOrNull ?: 0, true)
        }
      }
    }
  
  private val musicURL = url("https", "api.bilibili.com", "audio/music-service/web/song")
  val audioOrders = listOf("1", "2", "3")
  private suspend fun getAudios(folder: Folder, page: Int, order: Int): List<MediaResource> =
    ensureSuccess() {
      val result = client.get(
          Contributes.musicURL.url("upper",
                       "uid", folder.mid,
                       "pn", "${page + 1}",
                       "ps", MaxFavResourcePerPage.toString(),
                       "order", Contributes.audioOrders[order], "jsonp", "jsonp"),
          headers("Referer", "https://space.bilibili.com/${folder.mid}/audio")
      ).json().check()
      val data = result["data"]
      if(data.isNull || data["totalSize"].int == 0) return emptyList()
      return data["data"].jsonArray.map { audio ->
        val id = audio["id"].content
        cachedMedias.getOrPut(id) {
          MediaResource(id, MediaType.Audio,
                        audio["title"].content,
                        audio["cover"].content,
                        audio["ctime"].long / 1000,
                        audio["intro"].let { if(it.isNull) "" else it.content },
                        audio["uid"].content,
                        audio["uname"].content,
                        audio["statistic", "play"].int, true)
        }
      }
    }
}