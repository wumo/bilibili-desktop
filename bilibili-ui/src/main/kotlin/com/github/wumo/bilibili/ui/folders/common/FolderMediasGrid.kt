package com.github.wumo.bilibili.ui.folders.common

import com.github.wumo.bilibili.service.download.DownloadHistory
import com.github.wumo.bilibili.service.download.DownloadService
import com.github.wumo.bilibili.service.download.State.*
import com.github.wumo.bilibili.service.download.StateMonitor.monitorMedia
import com.github.wumo.bilibili.ui.MainUI
import com.github.wumo.bilibili.ui.css.Style
import com.github.wumo.bilibili.ui.css.Style.Companion.NormalTextCss
import com.github.wumo.bilibili.ui.download.DownloadMonitor
import com.github.wumo.bilibili.ui.login.UserLogin
import com.github.wumo.bilibili.ui.player.MediaPlayerController
import com.github.wumo.bilibili.util.FontUtils.biliFont
import com.github.wumo.bilibili.util.FontUtils.bili_icon_xinxi_UPzhu
import com.github.wumo.bilibili.util.FontUtils.icon_ic_play
import com.github.wumo.bilibili.util.Settings.setting
import com.github.wumo.bilibili.util.pretty
import javafx.event.EventTarget
import javafx.geometry.Pos
import javafx.scene.layout.Priority
import javafx.scene.paint.Color
import javafx.scene.shape.ArcType
import javafx.stage.Modality
import javafx.util.Duration
import org.apache.commons.lang3.SystemUtils
import tornadofx.*
import java.nio.file.Paths

fun EventTarget.folderMediasGrid(controller: ControllerTemplate) =
    datagrid<ResourceUI> {
      cellHeight = controller.coverHeight + 66.0
      cellWidth = controller.coverWidth.toDouble()
      horizontalCellSpacing = 20.0
      verticalCellSpacing = 10.0
      
      itemsProperty.bind(controller.resourcesToShow)
      
      style {
        backgroundColor += c("#F4F5F7")
        borderColor = multi(box(c("#eee")))
        borderRadius = multi(box(4.px))
      }
      cellFormat {
        controller.resourceShown(item)
      }
      cellCache { mediaUI ->
        vbox {
          stackpane {
            val cover = imageview {
              imageProperty().bind(mediaUI.coverImg)
              isPreserveRatio = true
              onLeftClick {
                MediaPlayerController.addToPlayList(listOf(mediaUI.res));
              }
              contextmenu {
                item("删除") {
                  addClass(NormalTextCss)
                  action {
                    DownloadService.remove(mediaUI.res)
                  }
                }
                item("彻底删除") {
                  addClass(NormalTextCss)
                  action {
                    DownloadService.removeDisk(mediaUI.res)
                  }
                }
              }
              tooltip(mediaUI.res.intro) {
                prefWidth = 300.0
                isWrapText = true
                addClass(NormalTextCss)
                showDuration = Duration.INDEFINITE
              }
            }
            if (mediaUI.res.valid) {
              pane {
                stackpaneConstraints {
                  marginLeft = controller.coverWidth - 25.0
                  marginTop = controller.coverHeight - 25.0
                }
                val mediaMonitor = DownloadMonitor.monitorUI(
                    monitorMedia(mediaUI.res.avid))
                pane {
                  visibleProperty().bind(mediaMonitor.uiState.isNotEqualTo(Finished))
                  circle {
                    centerX = 13.0
                    centerY = 13.0
                    radius = 18.0
                    fill = c("#fff")
                    opacity = 0.0
                  }
                  arc {
                    centerX = 13.0
                    centerY = 13.0
                    radiusX = 17.0
                    radiusY = 17.0
                    startAngle = 90.0
                    length = 0.0
                    type = ArcType.ROUND
                    fill = c("#06B025")
                  }
                  val qualityCircle = circle {
                    centerX = 13.0
                    centerY = 13.0
                    radius = 14.0
                    fill = c("#fff")
                  }
                  svgicon("M0 320C0 496.73 143.27 640 320 640C496.73 640 640 496.73 640 320C640 143.27 496.73 0 320 0C143.27 0 0 143.27 0 320ZM212.24 285.71C215.61 285.71 232.49 285.71 262.86 285.71C262.86 230.86 262.86 200.38 262.86 194.29C262.86 187.97 267.97 182.86 274.29 182.86C283.43 182.86 356.57 182.86 365.71 182.86C372.03 182.86 377.14 187.97 377.14 194.29C377.14 200.38 377.14 230.86 377.14 285.71C407.51 285.71 424.39 285.71 427.76 285.71C434.07 285.71 439.19 290.83 439.19 297.14C439.19 300.34 437.85 303.4 435.49 305.57C424.71 315.44 338.5 394.47 327.73 404.34C323.36 408.34 316.65 408.34 312.29 404.34C301.51 394.47 215.3 315.44 204.53 305.57C199.87 301.31 199.55 294.09 203.8 289.44C203.81 289.43 203.81 289.42 203.82 289.42C205.98 287.06 209.04 285.71 212.24 285.71ZM434.29 468.57C434.29 468.57 434.29 468.57 434.29 468.57C411.43 468.57 228.57 468.57 205.71 468.57C199.4 468.57 194.29 463.45 194.29 457.14C194.29 457.14 194.29 457.14 194.29 457.14C194.29 454.86 194.29 436.57 194.29 434.29C194.29 427.97 199.4 422.86 205.71 422.86C228.57 422.86 411.43 422.86 434.29 422.86C440.6 422.86 445.71 427.97 445.71 434.29C445.71 435.81 445.71 443.43 445.71 457.14C442.3 464.76 438.49 468.57 434.29 468.57Z",
                      26.0, c("#00a1d6")
                  ) {
                    val tooltipTxt = tooltip {
                      showDuration = Duration.INDEFINITE
                    }.textProperty()
                    qualityCircle.fillProperty().bind(mediaMonitor.uiState.objectBinding {
                      when (it) {
                        New -> Color.WHITE
                        Scheduled -> Color.RED
                        else -> Color.WHITE
                      }
                    })
                    var scheduled = false
                    onLeftClick {
                      if (!scheduled) {
                        scheduled = true
                        DownloadService.downloadMedia(mediaUI.res)
                      }
                    }
                  }
                }
                stackpane {
                  visibleProperty().bind(mediaMonitor.uiState.isEqualTo(Finished))
                  svgicon("M464 128H272l-64-64H48C21.49 64 0 85.49 0 112v288c0 26.51 21.49 48 48 48h416c26.51 0 48-21.49 48-48V176c0-26.51-21.49-48-48-48z",
                      25.0, c("#EFCD69")
                  ) {
                    stackpaneConstraints {
                      marginTop = 5.0
                    }
                    style(true) {
                      minWidth = 25.px
                      minHeight = 20.px
                      maxWidth = 25.px
                      maxHeight = 20.px
                      borderWidth += box(2.px)
                      borderColor += box(c("#222"))
                    }
                  }
                  onLeftClick {
                    val firstPage = DownloadHistory.record(mediaUI.res.avid).pages.values.firstOrNull()?.progress?.filePath()
                    if (SystemUtils.IS_OS_WINDOWS) {
                      Runtime.getRuntime().exec("""explorer.exe /select,"$firstPage"""")
                      return@onLeftClick
                    }
                    MainUI.app.hostServices.showDocument(
                        Paths.get(setting.downloadConfig.dir).toUri().toString())
                  }
                }
              }
            }
          }
          label(mediaUI.res.title) {
            minHeight = 38.0
            addClass(Style.FavoriteVideoTitleCss)
            isWrapText = true
            vboxConstraints {
              marginTop = 6.0
            }
          }
          hbox {
            alignment = Pos.CENTER_LEFT
            spacing = 5.0
            label(bili_icon_xinxi_UPzhu) {
              minWidth = 18.0
              style {
                font = biliFont
                fontSize = 18.px
              }
            }
            label(mediaUI.res.upperName) {
              addClass(Style.FavoriteVideoAuthorNameCss)
              onLeftClick {
                UserLogin.visitUser(mediaUI.res.upperMid)
              }
            }
            hbox {
              minWidth = 60.0
              alignment = Pos.CENTER_RIGHT
              hboxConstraints { hGrow = Priority.ALWAYS }
              label(icon_ic_play) {
                style {
                  font = biliFont
                  fontSize = 16.px
                  this.textFill = c("#999")
                }
              }
              label(mediaUI.res.play.pretty()) {
                addClass(Style.FavoriteVideoAuthorNameCss)
              }
            }
          }
        }
      }
    }