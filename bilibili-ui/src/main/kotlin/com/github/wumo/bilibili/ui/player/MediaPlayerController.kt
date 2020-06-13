package com.github.wumo.bilibili.ui.player

import com.github.wumo.bilibili.api.Bangumis
import com.github.wumo.bilibili.api.Medias
import com.github.wumo.bilibili.api.Videos
import com.github.wumo.bilibili.model.MediaPage
import com.github.wumo.bilibili.model.MediaResource
import com.github.wumo.bilibili.model.MediaType.*
import com.github.wumo.bilibili.service.download.DownloadService
import com.github.wumo.bilibili.service.download.StateMonitor.monitor
import com.github.wumo.bilibili.ui.MainController.uiScope
import com.github.wumo.bilibili.ui.download.DownloadMonitor
import com.github.wumo.bilibili.util.Settings.setting
import com.github.wumo.bilibili.util.ioLaunch
import com.github.wumo.bilibili.util.toValidFileName
import com.github.wumo.resource.ResourceHelper
import com.github.wumo.videoplayer.EntryState
import com.github.wumo.videoplayer.FileCallback
import com.github.wumo.videoplayer.NativePlayTask
import com.github.wumo.videoplayer.NativePlayer
import kotlinx.coroutines.*
import tornadofx.Controller


object MediaPlayerController : Controller(), CoroutineScope by uiScope {
  
  private var player: NativePlayer? = null
  private var refreshPagesJob: Job? = null
  
  fun addToPlayList(medias: List<MediaResource>) {
    player = player ?: NativePlayer(uiScope,
        onWindowOpen = {
          ResourceHelper.getInputStream("img/bilibili.png").use {
            player!!.setIcon(it.readAllBytes())
          }
        },
        onVolumeChange = { setting.playerConfig.volume = it },
        closeCallback = { stop() }).also { player ->
      player.start()
      player.volume = setting.playerConfig.volume
    }
    startRefreshPages(medias)
  }
  
  private fun startRefreshPages(medias: List<MediaResource>) {
    refreshPagesJob?.cancel()
    refreshPagesJob = ioLaunch {
      for (media in medias) {
        if (!isActive) break
        if (!media.valid) continue
        val pages = when (media.type) {
          Video -> Videos.getPageList(media.avid, media.title).also {
            it.forEach { page ->
              page.folder = media.upperName.toValidFileName()
              page.url = "https://www.bilibili.com/video/av${page.avid}?p=${page.pIdx}"
            }
          }
          Audio -> listOf(MediaPage(media.type, 1, media.avid, media.avid, media.title,
              folder = media.upperName.toValidFileName(), url = "https://www.bilibili.com/audio/au${media.avid}"))
          Bangumi -> Bangumis.getEpisodes(media.avid, media.title).also {
            it.forEach { page ->
              page.folder = media.title.toValidFileName()
              page.url = "https://www.bilibili.com/bangumi/play/ep${page.epid}"
            }
          }
          Other -> return@ioLaunch
        }
        for (page in pages) {
          if (!isActive) break
          val monitor = monitor(page)
          val file = monitor.info?.progress?.filePath()?.toFile()
          if (file == null || !file.exists()) {
            player?.addToPlayList("${media.title}-${page.title}", EntryState(remotePath = page.url) {
              val link = Medias.extractInfo(page)
              NativePlayTask(
                  link.videoURL?.let { HttpCallback(it, page.avid, link.videoSize) },
                  link.audioURL?.let { HttpCallback(it, page.avid, link.audioSize) })
            }.also { state ->
              state.downloadFunc = {
                DownloadService.downloadMediaPage(page).invokeOnCompletion {
                  if (it == null) {
                    state.localPath = monitor.info?.progress?.filePath()?.toFile()?.toString()
                    player?.updateCurrentState()
                  }
                }
              }
            })
          } else {
            player?.addToPlayList("${media.title}-${page.title}",
                EntryState(file.toString(), page.url) {
                  NativePlayTask(FileCallback(file.toString()))
                })
          }
        }
      }
    }
  }
  
  private fun stop() {
    player = null
    refreshPagesJob?.cancel()
    refreshPagesJob = null
  }
}