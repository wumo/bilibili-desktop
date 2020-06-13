package com.github.wumo.bilibili.service.download

import com.github.wumo.bilibili.model.MediaPage
import com.github.wumo.bilibili.model.MediaResource
import com.github.wumo.bilibili.model.PageLink
import com.github.wumo.bilibili.service.download.StateMonitor.monitor
import com.github.wumo.bilibili.service.download.StateMonitor.monitorMedia
import com.github.wumo.bilibili.util.apiScope
import com.github.wumo.bilibili.util.elapsedS
import com.github.wumo.bilibili.util.ioLaunch
import com.github.wumo.bilibili.util.now
import kotlinx.coroutines.CoroutineScope
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonConfiguration
import java.io.File
import java.nio.file.Path
import java.nio.file.Paths
import java.time.Instant
import java.util.*
import java.util.concurrent.ConcurrentHashMap
import kotlin.Comparator

@Serializable
data class History(var medias: MutableMap<String, DownloadMedia> = ConcurrentHashMap())

@Serializable
data class DownloadMedia(val avid: String,
                         var pages: MutableMap<String, DownloadPage> = ConcurrentHashMap())

@Serializable
data class DownloadPage(val page: MediaPage,
                        var done: Boolean = false,
                        var ts: Long = Instant.now().epochSecond,
                        var info: PageLink? = null,
                        var progress: PageProgress? = null)

@Serializable
data class WorkRange(val startOffset: Long, val endOffset: Long)

@Serializable
data class PageProgress(val downloadDir: String,
                        val fileName: String,
                        val ext: String,
                        var videoDownloaded: Long,
                        var audioDownloaded: Long) {
  fun filePath(): Path = Paths.get(downloadDir, "$fileName.$ext")
  val downloaded
    get() = videoDownloaded + audioDownloaded
}

object DownloadHistory: CoroutineScope by apiScope {
  private var history = History()
  private val json = Json(JsonConfiguration.Stable)
  
  fun remove(avid: String) {
    history.medias.remove(avid)
  }
  
  fun record(avid: String): DownloadMedia {
    return history.medias.getOrPut(avid) { DownloadMedia(avid) }
  }
  
  fun record(page: MediaPage): DownloadPage {
    return record(page.avid).pages.getOrPut(page.cid) { DownloadPage(page) }
  }
  
  fun remove(page: MediaPage) {
    history.medias[page.avid]?.pages?.remove(page.cid)
  }
  
  fun sync(configFile: File) {
    ioLaunch {
      load(configFile)
      syncHistory()
    }
  }
  
  private fun syncHistory() {
    val pages = PriorityQueue<DownloadPage>(Comparator { a, b ->
      (a.ts - b.ts).toInt()
    })
    history.medias.values.forEach {
      pages += it.pages.values
    }
    pages.forEach { task ->
      val monitor = monitor(task.page)
      monitor.syncHistory(task)
      StateMonitor.stateUpdatedMediaPageMonitors += monitor
    }
  }
  
  fun resume() {
    val pages = PriorityQueue<DownloadPage>(Comparator { a, b ->
      (a.ts - b.ts).toInt()
    })
    history.medias.values.forEach {
      it.pages.values.forEach { page ->
        if(!page.done)
          pages += page
      }
    }
    
    pages.forEach { task ->
      DownloadService.downloadMediaPage(task.page)
    }
  }
  
  private fun load(cacheFile: File) {
    val start = now()
    val data = cacheFile.readText()
    history = json.parse(History.serializer(), data)
    history.medias = ConcurrentHashMap(history.medias)
    println("load download history take: ${start.elapsedS()}s")
  }
  
  fun save(cacheFile: File) {
    val data = json.stringify(History.serializer(), history)
    cacheFile.writeText(data)
    println("successfully save download history")
  }
}