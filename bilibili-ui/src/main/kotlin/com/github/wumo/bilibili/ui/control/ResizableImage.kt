package com.github.wumo.bilibili.ui.control

import javafx.scene.image.ImageView

internal class WrappedImageView: ImageView() {
  init {
    isPreserveRatio = true
  }
  
  override fun minWidth(height: Double): Double {
    return 40.0
  }
  
  override fun maxWidth(height: Double): Double {
    return 16384.0
  }
  
//  override fun prefWidth(height: Double): Double {
//    return image?.width ?: return minWidth(height)
//  }
  
  override fun minHeight(width: Double): Double {
    return 40.0
  }
  
  override fun maxHeight(width: Double): Double {
    return 16384.0
  }
  
//  override fun prefHeight(width: Double): Double {
//    return image?.height ?: return minHeight(width)
//  }
  
  override fun isResizable(): Boolean {
    return true
  }
  
  override fun resize(width: Double, height: Double) {
    fitWidth = width
    fitHeight = height
  }
}