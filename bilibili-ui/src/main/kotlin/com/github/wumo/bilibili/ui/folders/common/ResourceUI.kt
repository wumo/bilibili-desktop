package com.github.wumo.bilibili.ui.folders.common

import com.github.wumo.bilibili.model.MediaResource
import com.github.wumo.bilibili.service.ImageStore

class ResourceUI(val res: MediaResource,
                 val coverImg: ImageStore.AsyncImage) {
  override fun equals(other: Any?): Boolean {
    if(this === other) return true
    if(javaClass != other?.javaClass) return false
    
    other as ResourceUI
    
    if(res != other.res) return false
    
    return true
  }
  
  override fun hashCode(): Int {
    return res.hashCode()
  }
}