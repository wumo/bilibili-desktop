@file:Suppress("NOTHING_TO_INLINE")

package com.github.wumo.bilibili.ui.css

import com.github.wumo.bilibili.service.ImageStore
import com.github.wumo.bilibili.service.ImageStore.defaultBackgroundImage
import com.github.wumo.bilibili.util.FontUtils.biliFont
import javafx.css.Styleable
import javafx.scene.layout.*
import javafx.scene.text.FontWeight
import tornadofx.*

inline fun Region.BackgroundCss(url: String?): Background {
  val url = url ?: defaultBackgroundImage
  return Background(
      BackgroundImage(
          ImageStore.getOrCache(url),
          BackgroundRepeat.REPEAT, BackgroundRepeat.REPEAT,
          BackgroundPosition.DEFAULT,
          BackgroundSize(1.0, 1.0, true, true, false, true)
      )
  )
}

inline val Styleable.VIPIconCss
  get() = style {
    font = biliFont
    fontSize = 36.px
    textFill = c("#FF6699")
    backgroundRadius = multi(box(36.px))
    backgroundColor = multi(c("#fff"))
  }

inline val Styleable.NameCss
  get() = style {
    fontSize = 36.px
    fontWeight = FontWeight.BOLD
    textFill = c("#fff")
  }

inline val Styleable.SignCss
  get() = style {
    fontSize = 18.px
    fontWeight = FontWeight.NORMAL
    textFill = c("#d6dee4")
  }

inline fun Styleable.LevelCss(level: Int) =
  style {
    fontSize = 20.px
    fontWeight = FontWeight.BOLD
    textFill = c("#fff")
    
    backgroundRadius = multi(box(5.px))
    backgroundColor = multi(
        arrayOf(
            c("#BEBEBE"), c("#BEBEBE"),
            c("#95DDB2"), c("#92D1E5"),
            c("#FFB37C"), c("#FF6C00"),
            c("#FF0000"), c("#E52FEC"),
            c("#841CF9"), c("#151515")
        )[level]
    )
  }

inline val Styleable.HeaderStatusTextCss
  get() =
    style {
      fontFamily = "Microsoft YaHei"
      fontSize = 16.px
      textFill = c("#d6dee4")
    }

inline fun Styleable.StatusIconCss(color: String) =
  style {
    font = biliFont
    fontSize = 16.px
    textFill = c(color)
  }

inline fun Styleable.NavigatorIconCss(color: String) =
  style {
    font = biliFont
    fontSize = 25.px
    textFill = c(color)
  }

inline val Styleable.NavigatorTextCss
  get() =
    style {
      fontFamily = "Microsoft YaHei"
      fontSize = 20.px
    }

