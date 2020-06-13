package com.github.wumo.bilibili.ui.folders.tabs

import com.github.wumo.bilibili.api.Bangumis
import com.github.wumo.bilibili.api.OrderOption
import com.github.wumo.bilibili.api.TidOption
import com.github.wumo.bilibili.model.Folder
import com.github.wumo.bilibili.ui.MainController
import com.github.wumo.bilibili.ui.folders.common.ControllerTemplate
import com.github.wumo.bilibili.ui.folders.common.ListFoldersFunc
import com.github.wumo.bilibili.ui.folders.common.FolderTemplate

object SubscriptionsController : ControllerTemplate() {
  override val coverWidth = 160
  override val coverHeight = 203
  override val tabIndex = MainController.subscriptionsTabIdx
  override val folderFuncs =
      listOf(ListFoldersFunc("", Bangumis::getBangumiFolders))
  override val folderPartionFunc: suspend (Folder) -> List<TidOption> =
      { emptyList() }
  override val fetchMediaFunc =
      Bangumis::fetchMedias
  override val fetchOrderOptions =
      listOf(OrderOption("全部", 0),
          OrderOption("想看", 1),
          OrderOption("在看", 2),
          OrderOption("看过", 3))
}

class TabSubscriptions : FolderTemplate() {
  
  override val controller = SubscriptionsController
}

