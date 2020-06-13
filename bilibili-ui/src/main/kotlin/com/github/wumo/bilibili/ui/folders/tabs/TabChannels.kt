package com.github.wumo.bilibili.ui.folders.tabs

import com.github.wumo.bilibili.api.Channels
import com.github.wumo.bilibili.api.OrderOption
import com.github.wumo.bilibili.api.TidOption
import com.github.wumo.bilibili.model.Folder
import com.github.wumo.bilibili.ui.MainController
import com.github.wumo.bilibili.ui.folders.common.ControllerTemplate
import com.github.wumo.bilibili.ui.folders.common.ListFoldersFunc
import com.github.wumo.bilibili.ui.folders.common.FolderTemplate

object ChannelController : ControllerTemplate() {
  override val coverWidth = 160
  override val coverHeight = 100
  override val tabIndex = MainController.channelTabIdx
  override val folderFuncs =
      listOf(ListFoldersFunc("", Channels::getChannelFolders))
  override val folderPartionFunc: suspend (Folder) -> List<TidOption> =
      { emptyList() }
  override val fetchMediaFunc =
      Channels::fetchMedias
  override val fetchOrderOptions =
      listOf(OrderOption("默认排序", 0),
          OrderOption("倒序排序", 1))
}

class TabChannels : FolderTemplate() {
  override val controller = ChannelController
}