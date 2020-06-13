package com.github.wumo.bilibili.ui

import com.github.wumo.bilibili.api.API
import com.github.wumo.bilibili.service.download.DownloadHistory
import com.github.wumo.bilibili.ui.MainController.uiScope
import com.github.wumo.bilibili.ui.login.UserLogin.onChangeTab
import com.github.wumo.bilibili.ui.control.BiliTabPane.Companion.bilitabpane
import com.github.wumo.bilibili.ui.css.BackgroundCss
import com.github.wumo.bilibili.ui.css.Style
import com.github.wumo.bilibili.ui.download.TabDownload
import com.github.wumo.bilibili.ui.download.statusPane
import com.github.wumo.bilibili.ui.folders.tabs.TabChannels
import com.github.wumo.bilibili.ui.folders.tabs.TabContributes
import com.github.wumo.bilibili.ui.folders.tabs.TabFavorites
import com.github.wumo.bilibili.ui.folders.tabs.TabSubscriptions
import com.github.wumo.bilibili.ui.login.UserLogin
import com.github.wumo.bilibili.ui.login.UserLogin.currentUser
import com.github.wumo.bilibili.ui.userinfo.userInfoPane
import com.github.wumo.bilibili.ui.userinfo.userStatusPane
import com.github.wumo.bilibili.util.*
import com.github.wumo.bilibili.util.FontUtils.bili_icon_download
import com.github.wumo.bilibili.util.FontUtils.icon_ic_channel
import com.github.wumo.bilibili.util.FontUtils.icon_ic_collect
import com.github.wumo.bilibili.util.FontUtils.icon_ic_home
import com.github.wumo.bilibili.util.FontUtils.icon_ic_sub
import com.github.wumo.bilibili.util.FontUtils.icon_ic_video
import javafx.application.Platform
import javafx.geometry.Insets
import javafx.scene.Scene
import javafx.scene.image.Image
import javafx.scene.input.MouseButton.BACK
import javafx.scene.input.MouseButton.FORWARD
import javafx.scene.input.MouseEvent.MOUSE_CLICKED
import javafx.scene.layout.Priority
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.cancel
import tornadofx.*

const val appName = "BiliBili Desktop"

class MainUI : App(MainView::class, Style::class), CoroutineScope by uiScope {
  companion object {
    lateinit var app: App
  }
  
  init {
    app = this
    addStageIcon(Image("/img/bilibili.png"))
    Settings.load()
    API.init(PersistentCookieStore)
    DownloadHistory.sync(Settings.downloadConfigFile)
  }
  
  override fun createPrimaryScene(view: UIComponent): Scene {
    return super.createPrimaryScene(view).apply {
      addEventFilter(MOUSE_CLICKED) {
        if (it.button == FORWARD) UserLogin.visitNextUser()
        else if (it.button == BACK) UserLogin.visitPreviousUser()
      }
    }
  }
}

class MainView : View() {
  init {
    title = appName
  }
  
  override fun onDock() {
    uiScope.uiLaunch {
      UserLogin.visitMasterLogin()
    }
  }
  
  override fun onUndock() {
    Settings.save()
    DownloadHistory.save(Settings.downloadConfigFile)
    apiScope.cancel()
    uiScope.cancel()
    API.close()
    Platform.exit()
    System.exit(0)
    println("close  main ")
  }
  
  override val root =
      vbox {
        prefWidth = 1080.0
        prefHeight = 1000.0
        style {
          backgroundColor += c("#F4F5F7")
        }
        
        //header
        hbox {
          hboxConstraints {
            marginLeft = 5.0
            marginTop = 5.0
            marginRight = 5.0
          }
          minWidth = 500.0
          minHeight = 190.0
          
          currentUser.addListener { _, _, new ->
            backgroundProperty().bind(new.topPhotoUrl.objectBinding {
              BackgroundCss(it)
            })
          }
          
          //header
          userInfoPane().apply {
            hboxConstraints {
              hGrow = Priority.ALWAYS
            }
          }
        }
        
        bilitabpane {
          vboxConstraints {
            marginLeft = 5.0
            marginTop = 5.0
            marginRight = 5.0
            marginBottom = 5.0
            vGrow = Priority.ALWAYS
          }
          biliButton(icon_ic_home, "#00c091", "主页") {
            UserLogin.visitMasterLogin()
          }
          bilitab(icon_ic_video, "#02b5da", "投稿") {
            find<TabContributes>().finishBuild()
          }
          bilitab(icon_ic_channel, "#00a1d6", "频道") {
            find<TabChannels>().finishBuild()
          }
          bilitab(icon_ic_collect, "#f3a034", "收藏") {
            find<TabFavorites>().finishBuild()
          }
          bilitab(icon_ic_sub, "#ff5d47", "订阅") {
            find<TabSubscriptions>().finishBuild()
          }
          bilitab(bili_icon_download, "#23c9ed", "下载") {
            find<TabDownload>(TabDownload::msgProp to it).root
          }
          biliCornor { userStatusPane() }
          MainController.selectedTab = selectedTab
          MainController.selectedTab.addListener { _, old, new ->
            onChangeTab(old.toInt(), new.toInt())
          }
        }
        
        statusPane().apply {
          vboxConstraints {
            margin = Insets(5.0, 5.0, 5.0, 5.0)
          }
        }
      }
}

