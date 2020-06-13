package com.github.wumo.bilibili.service.download

import com.github.wumo.bilibili.api.*
import com.github.wumo.bilibili.model.FolderType.*
import com.github.wumo.bilibili.model.MediaResource
import com.github.wumo.bilibili.service.StatefulService
import com.github.wumo.bilibili.service.ConsumerService
import com.github.wumo.bilibili.util.apiScope
import kotlinx.coroutines.CompletableJob
import kotlinx.coroutines.isActive

class FolderStreamer: StatefulService<DownloadFolderTask>(apiScope) {
  lateinit var mediaStreamer: ConsumerService<MediaResource>
  
  override suspend fun onTask(task: DownloadFolderTask, job: CompletableJob) {
    when(task.folder.type) {
      FavoriteFolder -> fetchFolderMedias(task, job, Favorites::fetchMedias)
      ContributeFolder -> fetchFolderMedias(task, job, Contributes::fetchMedias)
      ChannelFolder -> fetchFolderMedias(task, job, Channels::fetchMedias)
      SubscriptionFolder -> fetchFolderMedias(task, job, Bangumis::fetchMedias)
    }
  }
  
  private suspend fun fetchFolderMedias(
    task: DownloadFolderTask,
    job: CompletableJob,
    fetchMediaFunc: fetchMediasFuncType) {
    var page = 0
    do {
      val pageRes = fetchMediaFunc(task.folder, page++, task.tid, task.order)
      if(pageRes.isEmpty()) return
      for(res in pageRes)
        if(isActive) mediaStreamer.offer(res, job)
        else return
    } while(isActive)
  }
  
  override fun onRemove(task: DownloadFolderTask) {
    TODO("Not yet implemented")
  }
}