package com.github.wumo.bilibili.ui.download

import com.github.wumo.bilibili.service.download.DownloadHistory
import com.github.wumo.bilibili.service.download.DownloadService
import com.github.wumo.bilibili.service.download.State
import com.github.wumo.bilibili.service.download.State.*
import com.github.wumo.bilibili.ui.MainController.uiScope
import com.github.wumo.bilibili.ui.MainUI
import com.github.wumo.bilibili.ui.css.Style
import com.github.wumo.bilibili.ui.css.Style.Companion.NormalTextCss
import com.github.wumo.bilibili.ui.download.DownloadStatusRefresher.finishedTasks
import com.github.wumo.bilibili.ui.download.DownloadStatusRefresher.ongoingTasks
import com.github.wumo.bilibili.ui.download.DownloadStatusRefresher.traceMediaStateUpdate
import com.github.wumo.bilibili.ui.download.DownloadStatusRefresher.traceMediaPageState
import com.github.wumo.bilibili.util.*
import com.github.wumo.bilibili.util.Settings.setting
import javafx.beans.property.StringProperty
import javafx.geometry.Insets
import javafx.geometry.Orientation
import javafx.geometry.Pos
import javafx.scene.layout.Priority
import kotlinx.coroutines.CoroutineScope
import org.apache.commons.lang3.SystemUtils
import tornadofx.*
import java.awt.Desktop
import java.nio.file.Paths

class TabDownload : Fragment(), CoroutineScope by uiScope {
  val msgProp: StringProperty by param()
  
  override val root =
      vbox {
        spacing = 5.0
        hbox {
          vboxConstraints {
            margin = Insets(0.0, 5.0, 0.0, 5.0)
          }
          button("继续未完成下载") {
            action {
              DownloadHistory.resume()
            }
          }
        }
        hbox {
          alignment = Pos.CENTER
          vboxConstraints {
            margin = Insets(0.0, 5.0, 0.0, 5.0)
          }
          spacing = 5.0
          label("下载路径：") {
            addClass(NormalTextCss)
          }
          val dirProp = textfield(setting.downloadConfig.dir) {
            prefHeight = 36.0
            addClass(NormalTextCss)
            hboxConstraints { hGrow = Priority.ALWAYS }
            isEditable = false
          }.textProperty()
          button("选择") {
            addClass(NormalTextCss)
            action {
              chooseDirectory("选择下载文件夹")?.also { dir ->
                setting.downloadConfig.dir = dir.absolutePath
                dirProp.value = setting.downloadConfig.dir
              }
            }
          }
        }
        splitpane(Orientation.VERTICAL) {
          
          vboxConstraints {
            vGrow = Priority.ALWAYS
            margin = Insets(0.0, 5.0, 0.0, 5.0)
          }
          hbox {
            minHeight = 400.0
            squeezebox {
              hboxConstraints { hGrow = Priority.ALWAYS }
              fold("进行中的下载", expanded = true) {
                listview(ongoingTasks) {
                  hboxConstraints { hGrow = Priority.ALWAYS }
                  cellFormat { monitor ->
                    graphic = cache(monitor) {
                      vbox {
                        hbox {
                          vboxConstraints {
                            margin = Insets(5.0, 0.0, 5.0, 0.0)
                          }
                          prefWidthProperty().bind(this@listview.widthProperty().subtract(40))
                          spacing = 4.0
                          hbox {
                            hboxConstraints { hGrow = Priority.ALWAYS }
                            alignment = Pos.CENTER_LEFT
                            label("${monitor.monitor.page.mediaTitle}-${monitor.monitor.page.title}") {
                              minHeight = 38.0
                              addClass(Style.NormalTextCss)
                              isWrapText = true
                            }
                          }
                          vbox {
                            spacing = 4.0
                            progressbar {
                              minWidth = 200.0
                              progressProperty().bind(monitor.uiProgress)
                            }
                            label {
                              addClass(Style.SmallTextCss)
                              textProperty().bind(monitor.uiSpeed)
                            }
                          }
                          vbox {
                            minWidth = 150.0
                            alignment = Pos.CENTER
                            label {
                              addClass(Style.SmallTextCss)
                              textProperty().bind(monitor.uiProgressSize)
                            }
                            label {
                              addClass(Style.SmallTextCss)
                              textProperty().bind(monitor.uiETA)
                            }
                          }
                          onDoubleClick {
                            when (monitor.monitor.state.get()) {
                              New,
                              Scheduled,
                              Downloading -> DownloadService.pause(monitor.monitor.page)
                              Paused -> DownloadService.resume(monitor.monitor.page)
                            }
                          }
                        }
                        separator { }
                      }
                    }
                  }
                }
              }
              fold("已完成的下载", expanded = true) {
                listview(finishedTasks) {
                  hboxConstraints { hGrow = Priority.ALWAYS }
                  cellFormat { monitor ->
                    graphic = cache(monitor) {
                      vbox {
                        hbox {
                          vboxConstraints {
                            margin = Insets(5.0, 0.0, 5.0, 0.0)
                          }
                          prefWidthProperty().bind(this@listview.widthProperty().subtract(40))
                          val info = monitor.monitor.info!!
                          val time = dateTimeOfEpochSecond(info.ts)
                          val progress = info.progress
                          val filePath = progress!!.filePath()
                          val file = filePath.toFile()
                          vbox {
                            minWidth = 110.0
                            alignment = Pos.CENTER
                            label(time.format()) {
                              addClass(Style.SmallTextCss)
                            }
                            label((monitor.monitor.info?.info?.totalSize ?: 0).prettyMem()) {
                              addClass(Style.SmallTextCss)
                            }
                          }
                          hbox {
                            hboxConstraints { hGrow = Priority.ALWAYS }
                            alignment = Pos.CENTER_LEFT
                            label("${monitor.monitor.page.mediaTitle}-${monitor.monitor.page.title} " +
                                file.toString()) {
                              minHeight = 38.0
                              addClass(Style.NormalTextCss)
                              isWrapText = true
                            }
                          }
                          
                          onDoubleClick {
                            if (file.exists()) {
                              Desktop.getDesktop().open(file)
                              return@onDoubleClick
                            } else
                              MainUI.app.hostServices.showDocument(
                                  Paths.get(progress.downloadDir).toUri().toString())
                          }
                        }
                        separator { }
                      }
                    }
                  }
                }
              }
            }
            traceMediaPageState()
            traceMediaStateUpdate()
          }
        }
      }
}

