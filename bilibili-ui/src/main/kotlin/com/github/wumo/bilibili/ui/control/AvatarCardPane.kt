package com.github.wumo.bilibili.ui.control

import com.github.wumo.bilibili.ui.login.UserLogin
import javafx.event.EventTarget
import javafx.scene.layout.Pane
import tornadofx.opcr
import kotlin.math.min

fun EventTarget.avatarpane(block: AvatarCardPane.() -> Unit = {}) {
  val pane = AvatarCardPane()
  opcr(this, pane, block)
}

class AvatarCardPane: Pane() {
  
  override fun layoutChildren() {
    super.layoutChildren()
    val x = snappedLeftInset()
    val y = snappedTopInset()
    // Java 9 - snapSize is deprecated, use snapSizeX() and snapSizeY() accordingly
    val w = snapSizeX(width) - x - snappedRightInset()
    val h = snapSizeY(height) - y - snappedBottomInset()
    val total = children.size
    val nodeW = children.firstOrNull()?.layoutBounds?.width ?: 0.0
    val leftTotal = w - nodeW
    val leftNode = min(w - nodeW, (total - 1) * nodeW)
    val startX = leftTotal - leftNode
    val unit = if(total <= 1) 0.0 else leftNode / (total - 1)
    val list = children.toMutableList()
    list.sortBy {
      val pair = it.userData as UserLogin.VisitUser
      pair.idx
    }
    var uiIdx = 0
    list.forEach { node ->
      val half = node.layoutBounds.height / 2
      node.relocate(startX + (uiIdx++) * unit, h / 2 - half)
    }
    list.sortBy {
      val pair = it.userData as UserLogin.VisitUser
      if(pair.hover) Int.MAX_VALUE else pair.idx
    }
    children.setAll(list)
  }
}