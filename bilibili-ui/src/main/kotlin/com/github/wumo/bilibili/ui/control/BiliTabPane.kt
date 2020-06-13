package com.github.wumo.bilibili.ui.control

import com.github.wumo.bilibili.ui.css.NavigatorIconCss
import com.github.wumo.bilibili.ui.css.NavigatorTextCss
import javafx.animation.Interpolator
import javafx.beans.property.SimpleIntegerProperty
import javafx.beans.property.SimpleStringProperty
import javafx.beans.property.StringProperty
import javafx.event.EventTarget
import javafx.geometry.Pos
import javafx.scene.Node
import javafx.scene.layout.*
import tornadofx.*

class BiliTabPane: VBox() {
  companion object {
    fun EventTarget.bilitabpane(op: BiliTabPane.() -> Unit = {}) {
      val pane = BiliTabPane().apply {
        style {
          backgroundColor = multi(c("#fff"))
          borderColor = multi(box(c("#eee")))
          borderRadius = multi(box(4.px))
        }
        hbox {
          minHeight = USE_PREF_SIZE
          vboxConstraints {
            marginLeft = 20.0
            marginTop = 5.0
            marginRight = 20.0
            marginBottom = 0.0
          }
          alignment = Pos.CENTER_LEFT
          
          hbox {
            alignment = Pos.CENTER_LEFT
            tabHolder = this
            hboxConstraints {
              hGrow = Priority.ALWAYS
            }
            spacing = 50.0
          }
          pane {
            gridpaneConstraints {
              marginTop = 5.0
              marginRight = 20.0
              marginBottom = 5.0
            }
            cornerHolder = this
          }
        }
        pane {
          minHeight = 3.0
          vboxConstraints {
            marginLeft = 20.0
            marginTop = 5.0
            marginRight = 20.0
            marginBottom = 5.0
          }
          rectangle {
            fill = c("#00a1d6")
            height = 3.0
            layoutY = -1.0
            width = 73.0
            layoutX = 123.0
            moveCursorTo = { x, width ->
              layoutXProperty().animate(x, 0.2.seconds, Interpolator.EASE_BOTH)
              widthProperty().animate(width, 0.2.seconds, Interpolator.EASE_BOTH)
            }
          }
        }
        stackpane {
          contentHolder = this
          vboxConstraints {
            vGrow = Priority.ALWAYS
          }
        }
      }
      opcr(this, pane, op)
    }
  }
  
  private lateinit var contentHolder: StackPane
  private lateinit var tabHolder: Pane
  private lateinit var cornerHolder: Pane
  private lateinit var moveCursorTo: (Double, Double) -> Unit
  private val tabContentNodes = mutableListOf<Node>()
  private val tabHeaderNodes = mutableListOf<Region>()
  
  public val selectedTab = SimpleIntegerProperty(0)
  
  init {
    selectedTab.onChange { idx ->
      assert(idx < counter)
      tabContentNodes.forEachIndexed { i, node ->
        node.isVisible = i == idx
      }
      val headerNode = tabHeaderNodes[idx]
      moveCursorTo(headerNode.layoutX, headerNode.width)
    }
  }
  
  var counter = 0
  fun bilitab(
    icon: String, iconColor: String, title: String,
    tabContent: EventTarget.(StringProperty) -> Node
  ): Int {
    val idx = counter++
    var isFirst = true
    val msgProp = SimpleStringProperty("")
    tabHeaderNodes += tabHolder.hbox {
      alignment = Pos.CENTER_LEFT
      minWidth = USE_PREF_SIZE
      spacing = 4.0
      label(icon) {
        NavigatorIconCss(iconColor)
      }
      val labelTextFill = label(title) {
        NavigatorTextCss
      }.textFillProperty()
      label {
        NavigatorTextCss
        textProperty().bind(msgProp)
      }
      
      isFirst = idx == 0
      onHover {
        if(it) labelTextFill.value = c("#00a1d6")
        else labelTextFill.value = c("#222")
      }
      onLeftClick {
        assert(idx < counter)
        selectedTab.value = idx
        tabContentNodes.forEachIndexed { i, node ->
          node.isVisible = i == idx
        }
        moveCursorTo(layoutX, width)
      }
    }
    val tabNode = contentHolder.tabContent(msgProp)
    if(tabNode.parent == null)
      contentHolder.addChildIfPossible(tabNode)
    tabContentNodes += tabNode
    if(!isFirst) tabNode.isVisible = false
    return idx
  }
  
  fun biliButton(
    icon: String, iconColor: String, title: String,
    action: () -> Unit
  ) {
    val msgProp = SimpleStringProperty("")
    tabHolder.hbox {
      alignment = Pos.CENTER_LEFT
      minWidth = USE_PREF_SIZE
      spacing = 4.0
      label(icon) {
        NavigatorIconCss(iconColor)
      }
      val labelTextFill = label(title) {
        NavigatorTextCss
      }.textFillProperty()
      label {
        NavigatorTextCss
        textProperty().bind(msgProp)
      }
      
      onHover {
        if(it) labelTextFill.value = c("#00a1d6")
        else labelTextFill.value = c("#222")
      }
      onLeftClick(action = action)
    }
  }
  
  fun biliCornor(cornor: EventTarget.() -> Node) {
    cornerHolder.clear()
    cornerHolder.cornor()
  }
}
