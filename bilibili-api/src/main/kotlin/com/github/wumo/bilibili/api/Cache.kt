package com.github.wumo.bilibili.api

import com.github.wumo.bilibili.model.Folder
import com.github.wumo.bilibili.model.MediaResource
import java.util.concurrent.ConcurrentHashMap

typealias fetchMediasFuncType = suspend (folder: Folder, page: Int, tid: Int, order: Int) -> List<MediaResource>

object Cache {
  val cacheFavMedias = ConcurrentHashMap<CachedMediasKey, List<MediaResource>>()
  val cachedMedias = ConcurrentHashMap<String, MediaResource>()
}