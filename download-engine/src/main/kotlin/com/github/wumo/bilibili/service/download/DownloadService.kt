package com.github.wumo.bilibili.service.download

import com.github.wumo.bilibili.model.Folder
import com.github.wumo.bilibili.model.MediaPage
import com.github.wumo.bilibili.model.MediaResource
import com.github.wumo.bilibili.util.apiScope
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.serialization.Serializable

enum class State {
  New, Scheduled, Downloading, Paused, Finished, Cancelled
}

data class DownloadFolderTask(val folder: Folder, val tid: Int, val order: Int) {
  override fun equals(other: Any?): Boolean {
    if (this === other) return true
    if (javaClass != other?.javaClass) return false
    
    other as DownloadFolderTask
    
    if (folder != other.folder) return false
    if (tid != other.tid) return false
    
    return true
  }
  
  override fun hashCode(): Int {
    var result = folder.hashCode()
    result = 31 * result + tid
    return result
  }
}

@Serializable
data class Policy(var concurrent: Int = 3,
                  var threads: Int = 3,
                  var blockSize: Int = 1024 * 1024,
                  var blockNum: Int = 10)

@Serializable
data class DownloadConfig(
    var dir: String = "./download",
    val normal: Policy = Policy(),
    val high: Policy = Policy())

object DownloadService : CoroutineScope by apiScope {
  var downloadConfig = DownloadConfig()
  
  private val folderDownloader = FolderStreamer()
  private val mediaStreamer = MediaStreamer()
  private val pageStreamer = MediaPageStreamer()
  private val danmakuDownloader = DanmakuDownloader()
  private val legacyPageStreamer = LegacyPageDownloader()
  private val modernPageStreamer = ModernPageDownloader()
  private val fileTerminator = FileTerminator()
  
  init {
    folderDownloader.mediaStreamer = mediaStreamer
    mediaStreamer.pageStreamer = pageStreamer
    pageStreamer.legacyPageStreamer = legacyPageStreamer
    pageStreamer.modernPageStreamer = modernPageStreamer
    pageStreamer.danmakuDownloader = danmakuDownloader
    danmakuDownloader.fileOpTerminator = fileTerminator
    legacyPageStreamer.fileOpTerminator = fileTerminator
    modernPageStreamer.fileOpTerminator = fileTerminator
  }
  
  fun downloadMediaPage(mediaPage: MediaPage): Job {
    println("download media page${mediaPage.id}")
    return pageStreamer.offer(mediaPage)
  }
  
  fun downloadMedia(media: MediaResource) {
    println("download media ${media.avid}")
    mediaStreamer.offer(media)
  }
  
  fun downloadFolder(folder: Folder, tid: Int, order: Int) {
    folderDownloader.offer(DownloadFolderTask(folder, tid, order))
  }
  
  fun pause(page: MediaPage) {
    pageStreamer.pause(page)
  }
  
  fun resume(page: MediaPage) {
    pageStreamer.resume(page)
  }
  
  fun removeDisk(res: MediaResource) {
    mediaStreamer.remove(res)
  }
  
  fun remove(res: MediaResource) {
    mediaStreamer.remove(res)
  }
}