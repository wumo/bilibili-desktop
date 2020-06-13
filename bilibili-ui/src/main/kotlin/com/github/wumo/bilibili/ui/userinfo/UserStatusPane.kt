package com.github.wumo.bilibili.ui.userinfo

import com.github.wumo.bilibili.ui.css.Style.Companion.NormalTextCss
import com.github.wumo.bilibili.ui.login.UserLogin.currentUser
import com.github.wumo.bilibili.util.pretty
import javafx.event.EventTarget
import javafx.geometry.Pos
import tornadofx.*

fun EventTarget.userStatusPane() = hbox {
  alignment = Pos.CENTER_RIGHT
  spacing = 20.0
  vbox {
    spacing = 4.0
    alignment = Pos.CENTER
    label("关注") {
      addClass(NormalTextCss)
    }
    label {
      addClass(NormalTextCss)
      currentUser.addListener { _, _, new ->
        textProperty().bind(new.following.stringBinding {
          it?.toString() ?: "0"
        })
      }
    }
  }
  vbox {
    spacing = 4.0
    alignment = Pos.CENTER
    label("粉丝") {
      addClass(NormalTextCss)
    }
    label {
      addClass(NormalTextCss)
      currentUser.addListener { _, _, new ->
        textProperty().bind(new.followers.stringBinding {
          it?.pretty() ?: "0"
        })
      }
    }
  }
}