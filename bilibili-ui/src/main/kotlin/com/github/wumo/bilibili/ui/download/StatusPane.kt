package com.github.wumo.bilibili.ui.download

import com.github.wumo.bilibili.ui.download.DownloadStatusRefresher.downloadMedias
import com.github.wumo.bilibili.ui.download.DownloadStatusRefresher.downloadPages
import com.github.wumo.bilibili.ui.download.DownloadStatusRefresher.downloadSpeed
import com.github.wumo.bilibili.ui.download.DownloadStatusRefresher.downloadThreads
import com.github.wumo.bilibili.ui.download.DownloadStatusRefresher.eta
import com.github.wumo.bilibili.ui.download.DownloadStatusRefresher.traceStatusPane
import javafx.event.EventTarget
import javafx.geometry.Orientation
import javafx.geometry.Pos
import tornadofx.hbox
import tornadofx.label
import tornadofx.separator

fun EventTarget.statusPane() = hbox {
  alignment = Pos.CENTER_RIGHT
  spacing = 5.0
  eta = label("") {
    minWidth = 70.0
    alignment = Pos.CENTER
    isWrapText = true
  }.textProperty()
  downloadSpeed = label("") {
    minWidth = 70.0
    alignment = Pos.CENTER
    isWrapText = true
  }.textProperty()
  separator(Orientation.VERTICAL)
  downloadMedias = label {
    minWidth = 70.0
    alignment = Pos.CENTER
    isWrapText = true
  }.textProperty()
  separator(Orientation.VERTICAL)
  downloadPages = label {
    minWidth = 70.0
    alignment = Pos.CENTER
    isWrapText = true
  }.textProperty()
  separator(Orientation.VERTICAL)
  downloadThreads = label {
    minWidth = 70.0
    alignment = Pos.CENTER
    isWrapText = true
  }.textProperty()
  traceStatusPane()
}