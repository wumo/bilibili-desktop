package com.github.wumo.bilibili.ui

import com.github.wumo.bilibili.ui.folders.tabs.ChannelController
import com.github.wumo.bilibili.ui.folders.tabs.ContributesController
import com.github.wumo.bilibili.ui.folders.tabs.FavoriteController
import com.github.wumo.bilibili.ui.folders.tabs.SubscriptionsController
import javafx.beans.property.SimpleIntegerProperty
import kotlinx.coroutines.MainScope

object MainController {
  val uiScope= MainScope()
  val contributesTabIdx = 0
  val channelTabIdx = 1
  val favoritesTabIdx = 2
  val subscriptionsTabIdx = 3
  val downloadTabIdx = 4
  lateinit var selectedTab: SimpleIntegerProperty
  val tabControllers = listOf(
      ContributesController,
      ChannelController,
      FavoriteController,
      SubscriptionsController)
}