package com.github.wumo.bilibili.ui.folders.tabs

import com.github.wumo.bilibili.api.Favorites
import com.github.wumo.bilibili.api.OrderOption
import com.github.wumo.bilibili.ui.MainController
import com.github.wumo.bilibili.ui.folders.common.ControllerTemplate
import com.github.wumo.bilibili.ui.folders.common.ListFoldersFunc
import com.github.wumo.bilibili.ui.folders.common.FolderTemplate

object FavoriteController: ControllerTemplate() {
  override val coverWidth = 160
  override val coverHeight = 100
  override val tabIndex = MainController.favoritesTabIdx
  override val folderFuncs =
    listOf(ListFoldersFunc("我的创建", Favorites::getCreatedFolders),
           ListFoldersFunc("我的收藏", Favorites::getCollectedFolders))
  override val folderPartionFunc =
    Favorites::partitionOptions
  override val fetchMediaFunc =
    Favorites::fetchMedias
  override val fetchOrderOptions =
    listOf(OrderOption("最近收藏", 0),
        OrderOption("最多播放", 1),
        OrderOption("最新投稿", 2))
}

class TabFavorites: FolderTemplate() {
  override val controller = FavoriteController
}