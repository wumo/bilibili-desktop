package com.github.wumo.bilibili.ui.folders.common

import com.github.wumo.bilibili.ui.css.Style
import javafx.scene.layout.Priority
import tornadofx.*

abstract class FolderTemplate: Fragment() {
  abstract val controller: ControllerTemplate
  
  override val root = hbox {}
  
  fun finishBuild() = root.apply {
    if(controller.folderFuncs.size == 1) {
      val folder = controller.folderFuncs.first()
      listview(folder.folderProp) {
        controller.selectedFolder.onChange { selected ->
          if(selected != null)
            selectionModel.select(selected)
        }
        prefWidth = 190.0
        cellFormat {
          favFolderCell(it, controller)
        }
      }
    } else {
      squeezebox {
        var isFirst = true
        prefWidth = 190.0
        controller.folderFuncs.forEach {
          fold(it.name, expanded = isFirst) {
            addClass(Style.NormalLightTextCss)
            listview(it.folderProp) {
              controller.selectedFolder.onChange { selected ->
                if(selected != null)
                  selectionModel.select(selected)
              }
              cellFormat {
                favFolderCell(it, controller)
              }
            }
          }
          if(isFirst) isFirst = false
        }
      }
    }
    
    vbox {
      visibleProperty().bind(controller.selectedFolder.booleanBinding {
        it != null && it.mediaCount > 0
      })
      hboxConstraints {
        hGrow = Priority.ALWAYS
        marginLeft = 5.0
      }
      spacing = 5.0
      
      folderMediasBar(controller).apply {
        vboxConstraints {
        }
      }
      
      folderMediasGrid(controller).apply {
        vboxConstraints {
          vgrow = Priority.ALWAYS
        }
      }
    }
    
  }
}