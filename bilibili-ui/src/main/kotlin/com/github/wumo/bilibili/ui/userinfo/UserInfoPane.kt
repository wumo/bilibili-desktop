package com.github.wumo.bilibili.ui.userinfo

import com.github.wumo.bilibili.service.ImageStore
import com.github.wumo.bilibili.service.ImageStore.defaultAvatarImage
import com.github.wumo.bilibili.ui.login.LoginQrCode
import com.github.wumo.bilibili.ui.login.UserLogin
import com.github.wumo.bilibili.ui.control.avatarpane
import com.github.wumo.bilibili.ui.css.*
import com.github.wumo.bilibili.ui.login.UserLogin.currentUser
import com.github.wumo.bilibili.util.FontUtils.bili_icon_dingdao_Bbi
import com.github.wumo.bilibili.util.FontUtils.bili_icon_dingdao_bangdingshouji
import com.github.wumo.bilibili.util.FontUtils.bili_icon_dingdao_dahuiyuan
import com.github.wumo.bilibili.util.FontUtils.bili_icon_dingdao_yingbi
import com.github.wumo.bilibili.util.FontUtils.bili_icon_dingdao_youxiang
import javafx.event.EventTarget
import javafx.geometry.Pos
import javafx.scene.image.Image
import javafx.scene.layout.*
import javafx.scene.layout.Region.USE_PREF_SIZE
import javafx.scene.shape.Circle
import javafx.stage.StageStyle
import tornadofx.*

fun EventTarget.userInfoPane() = hbox {//info
  background = Background(
      BackgroundImage(
          Image("/img/gradient.png", true),
          BackgroundRepeat.REPEAT, BackgroundRepeat.REPEAT,
          BackgroundPosition.DEFAULT,
          BackgroundSize(1.0, 1.0, true, true, true, true)
      )
  )
  
  stackpane {//avatar
    minWidth = 130.0
    imageview(Image(defaultAvatarImage)) {
      clip = Circle(65.0, 65.0, 65.0)
      currentUser.addListener { _, _, new ->
        imageProperty().bind(new.faceUrl.objectBinding {
          ImageStore.getOrCache(it ?: defaultAvatarImage, 130, 130)
        })
      }
      onLeftClick {
        find<LoginQrCode>().openModal(stageStyle = StageStyle.UTILITY,
                                      block = true, resizable = false)
      }
    }
    
    label(bili_icon_dingdao_dahuiyuan) {
      VIPIconCss
      stackpaneConstraints {
        marginLeft = 130 - 36.0
        marginTop = 80.0
      }
      currentUser.addListener { _, _, new ->
        visibleProperty().bind(new.isVip)
      }
    }
    hboxConstraints {
      marginLeft = 20.0
      marginTop = 20.0
    }
  }
  vbox {//info
    spacing = 8.0
    hbox {
      alignment = Pos.CENTER_LEFT
      spacing = 10.0
      label {//name
        minWidth = USE_PREF_SIZE
        NameCss
        currentUser.addListener { _, _, new ->
          textProperty().bind(new.name.stringBinding {
            NameCss
            it
          })
        }
      }
      val imgGender = imageview()
      val lblLevel = label {
        minWidth = USE_PREF_SIZE
      }
      currentUser.addListener { _, _, new ->
        imgGender.imageProperty().bind(new.sex.objectBinding {
          when(it) {
            "男" -> "/img/male.png"
            "女" -> "/img/female.png"
            else -> null
          }?.let { img ->
            Image(img, 30.0, 30.0, false, false)
          }
        })
        lblLevel.textProperty().bind(new.level.stringBinding {
          lblLevel.LevelCss(it!!.toInt())
          " LV${it} "
        })
      }
      
    }
    
    label {//sign
      SignCss
      currentUser.addListener { _, _, new ->
        textProperty().bind(new.sign.stringBinding {
          SignCss
          it
        })
      }
    }
    
    hbox {
      alignment = Pos.CENTER_LEFT
      spacing = 4.0
      hbox {
        spacing = 4.0
        alignment = Pos.CENTER_LEFT
        label(bili_icon_dingdao_yingbi) {
          StatusIconCss("#00A1D6")
        }
        label {
          HeaderStatusTextCss
          currentUser.addListener { _, _, new ->
            textProperty().bind(stringBinding(new.coin) {
              value.toString()
            })
          }
          
        }
      }
      hbox {
        spacing = 4.0
        alignment = Pos.CENTER_LEFT
        label(bili_icon_dingdao_Bbi) {
          StatusIconCss("#FFAE00")
        }
        label {
          HeaderStatusTextCss
          currentUser.addListener { _, _, new ->
            textProperty().bind(stringBinding(new.bcoin) {
              value.toString()
            })
          }
        }
      }
      label(bili_icon_dingdao_youxiang) {
        StatusIconCss("#00A1D6")
        currentUser.addListener { _, _, new ->
          visibleProperty().bind(new.isEmailVerified)
        }
      }
      label(bili_icon_dingdao_bangdingshouji) {
        StatusIconCss("#00A1D6")
        currentUser.addListener { _, _, new ->
          visibleProperty().bind(new.isMobileVerified)
        }
      }
    }
    hboxConstraints {
      marginLeft = 20.0
      marginTop = 40.0
    }
  }
  hbox {
    alignment = Pos.CENTER_RIGHT
    hboxConstraints {
      marginLeft = 50.0
      marginRight = 10.0
      hGrow = Priority.ALWAYS
    }
    avatarpane {
      minWidth = 300.0
      maxWidth = 300.0
      UserLogin.holder = this
    }
  }
}