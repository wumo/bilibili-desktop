package com.github.wumo.bilibili.ui.folders.common

import com.github.wumo.bilibili.ui.player.MediaPlayerController
import javafx.event.EventTarget
import javafx.geometry.Orientation
import javafx.geometry.Pos
import javafx.scene.layout.Priority
import tornadofx.*

fun EventTarget.folderMediasBar(controller: ControllerTemplate) =
    vbox {
      spacing = 4.0
      hbox {
        alignment = Pos.CENTER_LEFT
        spacing = 4.0
        button("下载所选") {
          action {
            controller.downloadFolder(controller.selectedFolder.value!!,
                controller.chosenTidOption.value?.tid ?: 0,
                controller.chosenOrderOption.value?.value ?: 0)
          }
        }
        button("播放所选") {
          action {
            val medias = controller.resourcesToShow.map {
              it.res
            }
            MediaPlayerController.addToPlayList(medias);
          }
        }
        hbox {
          hboxConstraints { hGrow = Priority.ALWAYS }
          alignment = Pos.CENTER_RIGHT
          combobox(values = controller.fetchOrderOptions) {
            selectionModel.selectFirst()
            controller.chosenOrderOption.onChange {
              selectionModel.select(it)
            }
            valueProperty().onChange {
              if (controller.chosenOrderOption.value != it)
                controller.chosenOrderOption.value = it
              controller.openFolder(controller.selectedFolder.value,
                  controller.chosenTidOption.value,
                  it)
            }
          }
        }
      }
      
      listview(controller.tidOptions) {
        visibleProperty().bind(controller.tidOptions.sizeProperty.greaterThan(0))
        managedProperty().bind(visibleProperty())
        hboxConstraints { hGrow = Priority.ALWAYS }
        selectionModel.selectFirst()
        minHeight = 36.0
        maxHeight = 36.0
        orientation = Orientation.HORIZONTAL
        controller.chosenTidOption.onChange {
          selectionModel.select(it)
        }
        cellFormat {
          onLeftClick {
            controller.chosenTidOption.value = it
            controller.openFolder(controller.selectedFolder.value!!,
                it,
                controller.chosenOrderOption.value)
          }
          graphic = cache("${it.tid}-${it.count}") {
            hbox {
              alignment = Pos.CENTER_LEFT
              label(it.name)
              
              label(it.count.toString()) { }
            }
          }
        }
      }
    }