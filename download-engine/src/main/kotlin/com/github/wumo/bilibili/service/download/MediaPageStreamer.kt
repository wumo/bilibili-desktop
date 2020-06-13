package com.github.wumo.bilibili.service.download

import com.github.wumo.bilibili.api.Medias
import com.github.wumo.bilibili.api.Videos.qualityDesc
import com.github.wumo.bilibili.model.MediaPage
import com.github.wumo.bilibili.model.MediaType
import com.github.wumo.bilibili.service.StatefulService
import com.github.wumo.bilibili.service.ConsumerService
import com.github.wumo.bilibili.service.download.DownloadHistory.record
import com.github.wumo.bilibili.service.download.DownloadService.downloadConfig
import com.github.wumo.bilibili.service.download.State.*
import com.github.wumo.bilibili.service.download.StateMonitor.mediaPagesToDownload
import com.github.wumo.bilibili.service.download.StateMonitor.monitor
import com.github.wumo.bilibili.service.download.StateMonitor.stateUpdatedMediaPageMonitors
import com.github.wumo.bilibili.util.apiScope
import com.github.wumo.bilibili.util.escapePath
import com.github.wumo.bilibili.util.nowEpochSecond
import com.github.wumo.bilibili.util.toValidFileName
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CompletableJob
import java.nio.file.Paths

const val defaultMediaFormat = "mkv"

class MediaPageStreamer: StatefulService<MediaPage>(apiScope) {
  lateinit var legacyPageStreamer: ConsumerService<DownloadPage>
  lateinit var modernPageStreamer: ConsumerService<DownloadPage>
  lateinit var danmakuDownloader: ConsumerService<DownloadPage>
  
  override fun onOffer(task: MediaPage, job: CompletableJob) {
    val pageMonitor = monitor(task)
    val downloadPage = record(task)
    job.invokeOnCompletion {
      mediaPagesToDownload.decrementAndGet()
      when(it) {
        null -> {
          pageMonitor.state.set(Finished)
          stateUpdatedMediaPageMonitors += pageMonitor
          downloadPage.done = true
          downloadPage.ts = nowEpochSecond()
        }
        is CancellationException -> {
          pageMonitor.state.set(Paused)
          stateUpdatedMediaPageMonitors += pageMonitor
          downloadPage.done = false
        }
      }
    }
    mediaPagesToDownload.incrementAndGet()
  }
  
  override suspend fun onTask(task: MediaPage, job: CompletableJob) {
    val pageMonitor = monitor(task)
    val downloadPage = record(task)
    
    pageMonitor.state.set(Scheduled)
    stateUpdatedMediaPageMonitors += pageMonitor
    val pageInfo = Medias.extractInfo(task)
    val bestQuality = if(pageInfo.quality == pageInfo.bestQuality) "[best]" else ""
    val qualityDesc = pageInfo.quality.qualityDesc()
    val name = "${task.mediaTitle.escapePath()}-${task.title.escapePath()} [p${task.pIdx}][$qualityDesc]$bestQuality"
    val fileName = name.toValidFileName().trim()
    val folder = Paths.get(downloadConfig.dir, task.folder).toString()
    downloadPage.info = pageInfo
    downloadPage.progress = downloadPage.progress
                            ?: PageProgress(folder, fileName, defaultMediaFormat, 0, 0)
    pageMonitor.info = downloadPage
    val filePath = downloadPage.progress!!.filePath()
    
    if(filePath.toFile().exists()) return
    downloadPage.done = false
    
    (if(downloadPage.info!!.legacy) legacyPageStreamer else modernPageStreamer)
      .offer(downloadPage, job)
    if(task.type != MediaType.Audio)
      danmakuDownloader.offer(downloadPage)
  }
  
  override fun onRemove(task: MediaPage) {
    val pageMonitor = monitor(task)
    pageMonitor.state.set(Cancelled)
    stateUpdatedMediaPageMonitors += pageMonitor
    DownloadHistory.remove(task)
  }
}