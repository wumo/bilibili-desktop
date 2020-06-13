package com.github.wumo.bilibili.ui.folders.tabs

import com.github.wumo.bilibili.api.Contributes
import com.github.wumo.bilibili.api.OrderOption
import com.github.wumo.bilibili.ui.MainController
import com.github.wumo.bilibili.ui.folders.common.ControllerTemplate
import com.github.wumo.bilibili.ui.folders.common.ListFoldersFunc
import com.github.wumo.bilibili.ui.folders.common.FolderTemplate

object ContributesController: ControllerTemplate() {
  override val coverWidth = 160
  override val coverHeight = 100
  override val tabIndex = MainController.contributesTabIdx
  override val folderFuncs =
    listOf(ListFoldersFunc("", Contributes::getContributeFolders))
  override val folderPartionFunc =
    Contributes::partitionOptions
  override val fetchMediaFunc =
    Contributes::fetchMedias
  override val fetchOrderOptions =
    listOf(OrderOption("最新发布", 0),
        OrderOption("最多播放", 1),
        OrderOption("最多收藏", 2))
}

class TabContributes: FolderTemplate() {
  
  override val controller = ContributesController
}

