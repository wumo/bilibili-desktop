package com.github.wumo.bilibili.ui.download

import com.github.wumo.bilibili.model.MediaPage
import com.github.wumo.bilibili.service.download.State.*
import com.github.wumo.bilibili.service.download.StateMonitor.concurrentThreads
import com.github.wumo.bilibili.service.download.StateMonitor.downloadedBytes
import com.github.wumo.bilibili.service.download.StateMonitor.mediaPagesToDownload
import com.github.wumo.bilibili.service.download.StateMonitor.mediasToDownload
import com.github.wumo.bilibili.service.download.StateMonitor.stateUpdatedMediaMonitors
import com.github.wumo.bilibili.service.download.StateMonitor.stateUpdatedMediaPageMonitors
import com.github.wumo.bilibili.ui.MainController.uiScope
import com.github.wumo.bilibili.util.ioLaunch
import com.github.wumo.bilibili.util.uiLaunch
import com.github.wumo.bilibili.util.prettyMem
import com.github.wumo.bilibili.util.prettyTime
import javafx.beans.property.StringProperty
import javafx.collections.FXCollections.observableList
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import java.util.*
import kotlin.math.max

object DownloadStatusRefresher : CoroutineScope by uiScope {
  
  lateinit var downloadThreads: StringProperty
  lateinit var downloadPages: StringProperty
  lateinit var downloadMedias: StringProperty
  lateinit var downloadSpeed: StringProperty
  lateinit var eta: StringProperty
  
  val ongoingTasks = observableList<MediaPageMonitorUI>(LinkedList())!!
  val finishedTasks = observableList<MediaPageMonitorUI>(LinkedList())!!
  private val addedFinishedTasks = mutableSetOf<MediaPage>()
  
  fun traceStatusPane() {
    uiLaunch {
      var last = 0L
      var lastTime = System.currentTimeMillis()
      while (isActive) {
        delay(2000)
        val current = downloadedBytes.get()
        val currentTime = System.currentTimeMillis()
        val elapsed = max(currentTime - lastTime, 1L)
        val speed = 1.0 * (current - last) / elapsed * 1000
        downloadSpeed.value = "Speed: ${speed.prettyMem()}/s"
        downloadMedias.value = "Medias: ${mediasToDownload.get()}"
        downloadPages.value = "Pages: ${mediaPagesToDownload.get()}"
        downloadThreads.value = "Threads: ${concurrentThreads.get()}"
        last = current
        lastTime = currentTime
      }
    }
  }
  
  private val lastProgress = mutableMapOf<String, Long>()
  private var lastTime = 0L
  fun traceMediaPageState() {
    ioLaunch {
      val toAdd = mutableListOf<MediaPageMonitorUI>()
      while (true) {
        stateUpdatedMediaPageMonitors.iterator().also {
          while (it.hasNext()) {
            val newPage = it.next()
            it.remove()
            if (addedFinishedTasks.add(newPage.page))
              toAdd.add(DownloadMonitor.monitorUI(newPage))
          }
        }
        uiLaunch {
          ongoingTasks.addAll(toAdd)
          val now = System.currentTimeMillis()
          val elapsed = max(1, now - lastTime)
          lastTime = now
          val iter = ongoingTasks.listIterator()
          val walking = mutableListOf<MediaPageMonitorUI>()
          while (iter.hasNext()) {
            val m = iter.next()
            when (m.monitor.state.get()) {
              Finished -> {
                m.uiProgress.value = 100.0
                iter.remove()
                finishedTasks.add(0, m)
              }
              Cancelled -> {
                addedFinishedTasks.remove(m.monitor.page)
                iter.remove()
              }
              Paused -> {
                m.uiMessage.value = "Paused " + m.monitor.progress.get().toDouble().prettyMem() +
                    "/${m.monitor.totalSize.toDouble().prettyMem()}"
              }
              else -> {
                val last = lastProgress.getOrPut(m.monitor.page.id) { m.monitor.progress.get() }
                m.uiProgress.value = if (m.monitor.totalSize > 0)
                  m.monitor.progress.get() / m.monitor.totalSize.toDouble()
                else 0.0
                val p = m.monitor.progress.get() - last
                if (p > 0) walking += m
                val speed = 1.0 * p / elapsed * 1000
                lastProgress[m.monitor.page.id] = m.monitor.progress.get()
                val remain = m.monitor.totalSize - m.monitor.progress.get()
                val eta = if (p > 0) (remain / speed).prettyTime() else ""
                m.uiETA.value = eta
                m.uiSpeed.value = "${speed.prettyMem()}/s"
                m.uiProgressSize.value = m.monitor.progress.get().toDouble().prettyMem() +
                    "/${m.monitor.totalSize.toDouble().prettyMem()}"
              }
            }
          }
          ongoingTasks.removeAll(walking)
          walking.asReversed().forEach {
            ongoingTasks.add(0, it)
          }
        }.join()
        toAdd.clear()
        delay(1000)
      }
    }
  }
  
  fun traceMediaStateUpdate() {
    ioLaunch {
      val toUpdates = mutableSetOf<MediaMonitorUI>()
      while (true) {
        toUpdates.clear()
        stateUpdatedMediaMonitors.iterator().also {
          while (it.hasNext()) {
            val monitor = it.next()
            it.remove()
            val monitorUI = DownloadMonitor.monitorUI(monitor)
            toUpdates += monitorUI
          }
        }
        if (toUpdates.isNotEmpty())
          uiLaunch {
            toUpdates.forEach {
              it.uiState.value = it.monitor.state.get()
            }
          }
        delay(1000)
      }
    }
  }
}