package com.github.wumo.bilibili.service.download

import com.github.wumo.bilibili.api.Bangumis
import com.github.wumo.bilibili.api.Videos
import com.github.wumo.bilibili.model.MediaPage
import com.github.wumo.bilibili.model.MediaResource
import com.github.wumo.bilibili.model.MediaType.*
import com.github.wumo.bilibili.service.StatefulService
import com.github.wumo.bilibili.service.ConsumerService
import com.github.wumo.bilibili.service.download.StateMonitor.mediasToDownload
import com.github.wumo.bilibili.service.download.StateMonitor.monitorMedia
import com.github.wumo.bilibili.service.download.StateMonitor.stateUpdatedMediaMonitors
import com.github.wumo.bilibili.util.apiScope
import com.github.wumo.bilibili.util.toValidFileName
import kotlinx.coroutines.CompletableJob
import kotlinx.coroutines.isActive

class MediaStreamer: StatefulService<MediaResource>(apiScope) {
  lateinit var pageStreamer: StatefulService<MediaPage>
  
  override fun onOffer(task: MediaResource, job: CompletableJob) {
    val monitor = monitorMedia(task.avid)
    monitor.state.set(State.Scheduled)
    stateUpdatedMediaMonitors += monitor
    mediasToDownload.incrementAndGet()
    job.invokeOnCompletion {
      mediasToDownload.decrementAndGet()
      if(it == null) {
        monitor.state.set(State.Finished)
        stateUpdatedMediaMonitors += monitor
      }
    }
  }
  
  override suspend fun onTask(task: MediaResource, job: CompletableJob) {
    if(!task.valid) return
    val pages = when(task.type) {
      Video -> Videos.getPageList(task.avid, task.title).also {
        for(page in it) {
          if(!isActive) break
          page.folder = task.upperName.toValidFileName()
        }
      }
      Audio -> listOf(MediaPage(task.type, 1, task.avid, task.avid, task.title,
                                folder = task.upperName.toValidFileName()))
      Bangumi -> Bangumis.getEpisodes(task.avid, task.title).also {
        for(page in it) {
          if(!isActive) break
          page.folder = task.title.toValidFileName()
        }
      }
      Other -> return
    }
    for(page in pages) {
      if(!isActive) break
      pageStreamer.offer(page, job)
    }
  }
  
  override fun onRemove(task: MediaResource) {
    DownloadHistory.record(task.avid).pages.values.forEach {
      pageStreamer.remove(it.page)
    }
    DownloadHistory.remove(task.avid)
    val monitor = monitorMedia(task.avid)
    monitor.state.set(State.New)
    stateUpdatedMediaMonitors += monitor
  }
}