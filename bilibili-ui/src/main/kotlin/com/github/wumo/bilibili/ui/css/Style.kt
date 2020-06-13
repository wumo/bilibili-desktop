package com.github.wumo.bilibili.ui.css

import com.github.wumo.bilibili.util.FontUtils
import javafx.scene.text.FontWeight
import tornadofx.*

class Style : Stylesheet() {
  companion object {
    val SmallTextCss by cssclass()
    val NormalTextCss by cssclass()
    val NormalLightTextCss by cssclass()
    val FavoriteVideoTitleCss by cssclass()
    val FavoriteVideoAuthorNameCss by cssclass()
    val videoDownloadIconCss by cssclass()
  }
  
  init {
    SmallTextCss {
      fontFamily = "Microsoft YaHei"
      fontSize = 12.px
    }
    NormalTextCss {
      fontFamily = "Microsoft YaHei"
      fontSize = 16.px
    }
    NormalLightTextCss {
      fontFamily = "Microsoft YaHei"
      fontWeight = FontWeight.LIGHT
      fontSize = 16.px
    }
    FavoriteVideoTitleCss {
      ellipsisString = ""
      fontFamily = "Microsoft YaHei"
      fontWeight = FontWeight.NORMAL
      textFill = c(33, 37, 39)
      fontSize = 12.px
    }
    FavoriteVideoAuthorNameCss {
      fontFamily = "Microsoft YaHei"
      fontWeight = FontWeight.NORMAL
      fontSize = 12.px
      textFill = c("#999")
    }
    videoDownloadIconCss {
      font = FontUtils.biliFont
      fontSize = 25.px
      textFill = c("#00a1d6")
      backgroundRadius = multi(box(25.px))
      backgroundColor = multi(c("#fff"))
    }
  }
}