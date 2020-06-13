package com.github.wumo.bilibili.util

import com.github.wumo.bilibili.util.Settings.configPath
import kotlinx.serialization.Serializable
import kotlinx.serialization.builtins.list
import java.net.CookieManager
import java.net.CookieStore
import java.net.HttpCookie
import java.net.URI

object PersistentCookieStore: CookieStore by CookieManager().cookieStore {
  @Serializable
  data class SerializableHttpCookie(
    val name: String?,
    val value: String?,
    val comment: String?,
    val commentURL: String?,
    val discard: Boolean,
    val domain: String?,
    val maxAge: Long,
    val path: String?,
    val portlist: String?,
    val secure: Boolean,
    val httpOnly: Boolean,
    val version: Int = 1
  ) {
    fun toHttpCookie() = HttpCookie(name, value).let {
      it.comment = comment
      it.commentURL = commentURL
      it.discard = discard
      it.domain = domain
      it.maxAge = maxAge
      it.path = path
      it.portlist = portlist
      it.secure = secure
      it.isHttpOnly = httpOnly
      it.version = version
      it
    }
  }
  
  @Serializable
  data class Cookie(val uri: String, val entries: List<SerializableHttpCookie>)
  
  val cacheFile = configPath.resolve("cookie.json").toFile().apply {
    if(!exists()) {
      parentFile?.mkdirs()
      createNewFile()
      writeText(
          jsonPretty.stringify(
              Cookie.serializer().list,
              emptyList()
          )
      )
    }
  }
  
  init {
    val data = cacheFile.readText()
    val cookies = jsonPretty.parse(Cookie.serializer().list, data)
    
    cookies.forEach { (uriS, entries) ->
      val uri = URI.create(uriS)
      entries.forEach {
        add(uri, it.toHttpCookie())
      }
    }
  }
  
  fun save() {
    val cookies = urIs.map { uri ->
      val cookieEntries = get(uri).map {
        SerializableHttpCookie(
            it.name, it.value, it.comment, it.commentURL, it.discard, it.domain
            , it.maxAge, it.path, it.portlist, it.secure, it.isHttpOnly, it.version
        )
      }.toList()
      Cookie(uri.toString(), cookieEntries)
    }.toList()
    
    val data = jsonPretty.stringify(Cookie.serializer().list, cookies)
    cacheFile.writeText(data)
    println("flush cookies to disk!")
  }
}