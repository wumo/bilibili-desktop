package com.github.wumo.bilibili.ui.folders.common

import com.github.wumo.bilibili.model.Folder
import com.github.wumo.bilibili.ui.css.Style
import com.github.wumo.bilibili.util.FontUtils
import javafx.geometry.Pos
import javafx.scene.control.ListCell
import javafx.scene.layout.Priority
import javafx.util.Duration
import tornadofx.*

fun ListCell<Folder>.favFolderCell(folder: Folder, controller: ControllerTemplate) {
  onLeftClick {
    controller.openFolder(folder, controller.chosenTidOption.value,
        controller.chosenOrderOption.value)
  }
  graphic = cache(folder) {
    gridpane {
      alignment = Pos.CENTER_LEFT
      row {
        label(if (folder.isPrivate) FontUtils.icon_huaban else FontUtils.icon_bodan) {
          minWidth = 22.0
          style {
            font = FontUtils.biliFont
            fontSize = 22.px
          }
          gridpaneConstraints { marginRight = 10.0 }
        }
        hbox {
          maxWidth = 86.0
          label(folder.title) {
            addClass(Style.NormalLightTextCss)
            tooltip(folder.title) {
              addClass(Style.NormalLightTextCss)
              showDuration = Duration.INDEFINITE
            }
            onHover {
            
            }
          }
          gridpaneConstraints {
            hGrow = Priority.ALWAYS
          }
        }
        
        label(folder.mediaCount.toString()) {
          addClass(Style.NormalLightTextCss)
        }
      }
    }
  }
}