package com.github.wumo.bilibili.ui.login

import com.github.wumo.bilibili.api.Login.fetchCookie
import com.github.wumo.bilibili.api.Login.fetchMasterInfo
import com.github.wumo.bilibili.api.Login.fetchUserInfo
import com.github.wumo.bilibili.api.OrderOption
import com.github.wumo.bilibili.api.TidOption
import com.github.wumo.bilibili.model.Folder
import com.github.wumo.bilibili.service.ImageStore
import com.github.wumo.bilibili.ui.MainController
import com.github.wumo.bilibili.ui.MainController.uiScope
import com.github.wumo.bilibili.util.PersistentCookieStore
import com.github.wumo.bilibili.util.ioLaunch
import com.github.wumo.bilibili.util.uiLaunch
import javafx.beans.property.*
import javafx.scene.Node
import javafx.scene.layout.Pane
import javafx.scene.shape.Circle
import javafx.stage.StageStyle
import kotlinx.coroutines.CoroutineScope
import tornadofx.find
import tornadofx.imageview
import tornadofx.onHover
import tornadofx.onLeftClick

class UserInfoUI {
  val isLogin = SimpleBooleanProperty(false)
  val mid = SimpleStringProperty()
  val name = SimpleStringProperty()
  val faceUrl = SimpleStringProperty()
  val sign = SimpleStringProperty()
  val sex = SimpleStringProperty()
  val level = SimpleIntegerProperty(0)
  val currentExp = SimpleIntegerProperty(0)
  val nextExp = SimpleIntegerProperty(0)
  val topPhotoUrl = SimpleStringProperty()
  val isVip = SimpleBooleanProperty(false)
  val coin = SimpleIntegerProperty(0)
  val bcoin = SimpleFloatProperty(0f)
  val isEmailVerified = SimpleBooleanProperty(false)
  val isMobileVerified = SimpleBooleanProperty(false)
  val following = SimpleIntegerProperty(0)
  val followers = SimpleIntegerProperty(0)
}

object UserLogin : CoroutineScope by uiScope {
  class WorkingState(var chosenTabIdx: Int = 0,
                     var selectedFolder: Folder? = null,
                     var tidOption: TidOption? = null,
                     var orderOption: OrderOption? = null)
  
  class VisitUser(mid: String, var idx: Int) {
    val info = UserInfoUI().also { it.mid.value = mid }
    var hover: Boolean = false
    val workingState = WorkingState()
    lateinit var node: Node
  }
  
  val currentUser = SimpleObjectProperty<UserInfoUI>(null)
  var lastUser: UserInfoUI? = null
  
  val master = VisitUser("", 0)
  
  private const val maxHistory = 5
  private val userQueue = mutableListOf<VisitUser>()
  private var idx = 0
  lateinit var holder: Pane
  
  fun visitPreviousUser() {
    val idx = userQueue.indexOfFirst { it.info.mid == currentUser.value?.mid }
    if (idx <= 0) return
    val user = userQueue[idx - 1]
    visitUser(user.info.mid.value, updateHistory = false)
  }
  
  fun visitNextUser() {
    val idx = userQueue.indexOfFirst { it.info.mid == currentUser.value?.mid }
    if (idx == -1 || idx == userQueue.lastIndex) return
    val user = userQueue[idx + 1]
    visitUser(user.info.mid.value, updateHistory = false)
  }
  
  fun visitUser(mid: String, visitor: VisitUser? = null, updateHistory: Boolean = true) {
    uiLaunch {
      val lastHistory = userQueue.find { it.info.mid == currentUser.value?.mid }
      val currentHistory = userQueue.find { it.info.mid.value == mid }
      val current = if (currentHistory == null) {
        if (userQueue.size >= maxHistory) {
          val v = userQueue.removeAt(0)
          holder.children.remove(v.node)
        }
        val v = visitor ?: VisitUser(mid, 0)
        v.idx = idx++
        ioLaunch {
          val userInfo = fetchUserInfo(mid)
          v.info.apply {
            name.value = userInfo.name
            faceUrl.value = userInfo.faceUrl
            sign.value = userInfo.sign
            sex.value = userInfo.sex
            level.value = userInfo.level
            topPhotoUrl.value = userInfo.topPhotoUrl
            isVip.value = userInfo.isVip
            coin.value = userInfo.coin
            following.value = userInfo.following
            followers.value = userInfo.followers
          }
          ImageStore.getOrCache(v.info.faceUrl.value, 130, 130)
        }.join()
        userQueue.add(v)
        v.node = holder.imageview(ImageStore.getOrCache(
            v.info.faceUrl.value, 130, 130)) {
          userData = v
          clip = Circle(65.0, 65.0, 65.0)
          onHover {
            v.hover = it
            holder.requestLayout()
          }
          onLeftClick {
            visitUser(mid, updateHistory = false)
          }
        }
        holder.requestLayout()
        v
      } else {
        if (updateHistory) {
          userQueue.remove(currentHistory)
          userQueue.add(currentHistory)
          currentHistory.idx = idx++
        }
        currentHistory
      }
      lastUser = currentUser.value
      currentUser.value = current.info
      if (lastUser?.mid?.value != currentUser.value.mid.value)
        changeUser(lastHistory, current)
    }
  }
  
  fun visitMasterLogin() {
    uiLaunch {
      try {
        val userInfo = fetchMasterInfo()
        userInfo.apply {
          master.info.mid.value = mid
          master.info.isLogin.value = isLogin
          master.info.isEmailVerified.value = isEmailVerified
          master.info.isMobileVerified.value = isMobileVerified
          master.info.currentExp.value = currentExp
          master.info.nextExp.value = nextExp
          master.info.bcoin.value = bcoin
        }
        fetchCookie(PersistentCookieStore)
        visitUser(master.info.mid.value, master)
      } catch (e: Exception) {
        e.printStackTrace()
        find<LoginQrCode>().openModal(stageStyle = StageStyle.UTILITY, block = true, resizable = false)
      }
    }
  }
  
  fun onChangeTab(old: Int, new: Int) {
    if (old in 0..MainController.tabControllers.lastIndex)
      MainController.tabControllers[old].onLeave()
    if (new in 0..MainController.tabControllers.lastIndex)
      MainController.tabControllers[new].onChosen()
  }
  
  private fun changeUser(old: VisitUser?, new: VisitUser) {
    old?.also {
      val state = old.workingState
      if (MainController.selectedTab.value in 0..MainController.tabControllers.lastIndex) {
        state.chosenTabIdx = MainController.selectedTab.value
        val controller = MainController.tabControllers[MainController.selectedTab.value]
        state.selectedFolder = controller.selectedFolder.value
        state.tidOption = controller.chosenTidOption.value
        state.orderOption = controller.chosenOrderOption.value
      }
    }
    MainController.tabControllers.forEach {
      it.selectedFolder.value = null
      it.chosenTidOption.value = null
      it.chosenOrderOption.value = null
    }
    val state = new.workingState
    if (state.chosenTabIdx in 0..MainController.tabControllers.lastIndex) {
      val controller = MainController.tabControllers[state.chosenTabIdx]
      controller.selectedFolder.value = state.selectedFolder
      controller.chosenTidOption.value = state.tidOption
      controller.chosenOrderOption.value = state.orderOption
    }
    if (MainController.selectedTab.value == state.chosenTabIdx) //新旧一致时不会执行，这里手动调用
      onChangeTab(MainController.selectedTab.value, state.chosenTabIdx)
    else
      MainController.selectedTab.value = state.chosenTabIdx
  }
}