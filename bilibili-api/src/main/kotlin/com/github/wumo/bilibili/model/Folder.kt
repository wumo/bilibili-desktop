package com.github.wumo.bilibili.model

enum class FolderType {
  FavoriteFolder, ContributeFolder, ChannelFolder, SubscriptionFolder
}

class Folder(
  val type: FolderType,
  val mid: String,
  val fid: String,
  val title: String,
  val mediaCount: Int,
  val isPrivate: Boolean
) {
  val id = "$mid-$fid"
  
  data class Detail(val coverURL: String)
  
  override fun equals(other: Any?): Boolean {
    if(this === other) return true
    if(other !is Folder) return false
    return id == other.id
  }
  
  override fun hashCode(): Int {
    return id.hashCode()
  }
}

