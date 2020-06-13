package com.github.wumo.bilibili.model

import kotlinx.serialization.Serializable

@Serializable
class MediaPage(
    val type: MediaType,
    val pIdx: Int,
    val avid: String,
    val cid: String,
    val title: String,
    val mediaTitle: String = "",
    val avid2: String = "",//bangumi拥有额外的avid，这里用sid代替
    val epid: String = "",
    var folder: String = "",
    var url: String = ""
) {
  val id = "$avid-$cid"
  override fun equals(other: Any?): Boolean {
    if (this === other) return true
    if (javaClass != other?.javaClass) return false
    
    other as MediaPage
    
    if (id != other.id) return false
    
    return true
  }
  
  override fun hashCode(): Int {
    return id.hashCode()
  }
}