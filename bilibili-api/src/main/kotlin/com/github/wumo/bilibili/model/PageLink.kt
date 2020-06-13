package com.github.wumo.bilibili.model

import kotlinx.serialization.Serializable
import okhttp3.HttpUrl.Companion.toHttpUrl
import java.time.Instant

@Serializable
data class PageLink(val quality: Int,
                    val bestQuality: Int,
                    val videoSize: Long,
                    @Transient
                    var videoURL: String? = null,
                    val audioSize: Long,
                    @Transient
                    var audioURL: String? = null,
                    val legacy: Boolean) {
  fun needToFetchUrl(): Boolean {
    val _url = videoURL ?: audioURL ?: return true
    val url = _url.toHttpUrl()
    try {
      val deadline = (url.queryParameter("expires") ?: url.queryParameter("deadline")!!).toLong()
      
      return Instant.now().epochSecond >= deadline
    } catch(e: Exception) {
      throw Exception("url deadline invalid: $url")
    }
  }
  
  val totalSize: Long
    get() = videoSize + audioSize
}