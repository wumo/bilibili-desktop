package com.github.wumo.bilibili.service.download

import com.github.wumo.bilibili.model.MediaPage
import com.github.wumo.bilibili.model.MediaResource
import com.github.wumo.bilibili.service.download.State.*
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.channels.Channel.Factory.UNLIMITED
import java.util.*
import java.util.Collections.newSetFromMap
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.atomic.AtomicInteger
import java.util.concurrent.atomic.AtomicLong
import java.util.concurrent.atomic.AtomicReference

open class DownloadStateful {
  val state = AtomicReference(New)
}

data class MediaMonitor(val avid: String): DownloadStateful()

data class MediaPageMonitor(val page: MediaPage): DownloadStateful() {
  var info: DownloadPage? = null
  
  @Volatile
  var totalSize = 0L
  val progress = AtomicLong(0)
  
  fun syncHistory(page: DownloadPage) {
    state.set(if(page.done) Finished else Paused)
    info = page
    totalSize = page.info?.totalSize ?: 0
    progress.set(page.progress?.downloaded ?: 0)
  }
}

object StateMonitor {
  val stateUpdatedMediaPageMonitors: MutableSet<MediaPageMonitor> = newSetFromMap(ConcurrentHashMap())
  val stateUpdatedMediaMonitors: MutableSet<MediaMonitor> = newSetFromMap(ConcurrentHashMap())
  val mediasToDownload = AtomicInteger(0)
  val downloadedBytes = AtomicLong(0)
  val mediaPagesToDownload = AtomicInteger(0)
  val concurrentThreads = AtomicInteger(0)
  
  private val pageMonitors = ConcurrentHashMap<MediaPage, MediaPageMonitor>()
  private val mediaMonitors = ConcurrentHashMap<String, MediaMonitor>()
  
  fun monitorMedia(avid: String): MediaMonitor {
    return mediaMonitors.getOrPut(avid) { MediaMonitor(avid) }
  }
  
  fun monitor(page: MediaPage): MediaPageMonitor {
    return pageMonitors.getOrPut(page) { MediaPageMonitor(page) }
  }
}