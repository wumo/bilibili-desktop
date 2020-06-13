package com.github.wumo.bilibili.ui.download

import com.github.wumo.bilibili.service.download.MediaMonitor
import com.github.wumo.bilibili.service.download.MediaPageMonitor
import com.github.wumo.bilibili.service.download.State.New
import javafx.beans.property.SimpleDoubleProperty
import javafx.beans.property.SimpleObjectProperty
import javafx.beans.property.SimpleStringProperty
import java.util.concurrent.ConcurrentHashMap

class MediaPageMonitorUI(val monitor: MediaPageMonitor) {
  val uiState = SimpleObjectProperty(monitor.state.get())
  val uiProgress = SimpleDoubleProperty(0.0)
  val uiMessage = SimpleStringProperty("")
  val uiSpeed = SimpleStringProperty("")
  val uiProgressSize = SimpleStringProperty("")
  val uiETA = SimpleStringProperty("")
}

class MediaMonitorUI(val monitor: MediaMonitor) {
  val uiState = SimpleObjectProperty(monitor.state.get())
}

object DownloadMonitor {
  private val pageMonitors = ConcurrentHashMap<MediaPageMonitor, MediaPageMonitorUI>()
  fun monitorUI(monitor: MediaPageMonitor): MediaPageMonitorUI {
    return pageMonitors.getOrPut(monitor) { MediaPageMonitorUI(monitor) }
  }
  
  private val mediaMonitors = ConcurrentHashMap<MediaMonitor, MediaMonitorUI>()
  fun monitorUI(monitor: MediaMonitor): MediaMonitorUI {
    return mediaMonitors.getOrPut(monitor) { MediaMonitorUI(monitor) }
  }
}