package com.github.wumo.bilibili.model

import kotlinx.serialization.Serializable

enum class MediaType(val type: Int) {
  Video(2), Audio(12), Bangumi(3), Other(0);
  
  companion object {
    fun parse(type: Int) = when(type) {
      2 -> Video
      12 -> Audio
      3 -> Bangumi
      else -> Other
    }
  }
  
  override fun toString(): String {
    return type.toString()
  }
}

@Serializable
class MediaResource(
  val avid: String,
  val type: MediaType,
  val title: String,
  val coverURL: String,
  val time: Long = 0L,
  val intro: String = "",
  val upperMid: String = "",
  val upperName: String = "",
  val play: Int = 0,
  val valid: Boolean = true) {
  
  override fun equals(other: Any?): Boolean {
    if(this === other) return true
    if(javaClass != other?.javaClass) return false
    
    other as MediaResource
    
    if(avid != other.avid) return false
    if(type != other.type) return false
    
    return true
  }
  
  override fun hashCode(): Int {
    var result = avid.hashCode()
    result = 31 * result + type.hashCode()
    return result
  }
}
