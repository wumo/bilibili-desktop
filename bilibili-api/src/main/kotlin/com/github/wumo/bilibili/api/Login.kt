package com.github.wumo.bilibili.api

import com.github.wumo.bilibili.api.API.client
import com.github.wumo.bilibili.api.API.cookieStore
import com.github.wumo.bilibili.util.get
import com.github.wumo.bilibili.util.json
import com.github.wumo.bilibili.util.post
import kotlinx.serialization.json.*
import java.net.CookieStore
import java.net.HttpCookie
import java.net.URI
import java.util.concurrent.ConcurrentHashMap

object Login {
  class UserInfo {
    var isLogin = false
    var mid = ""
    var name = ""
    var faceUrl = ""
    var sign = ""
    var sex = ""
    var level = 0
    var currentExp = 0
    var nextExp = 0
    var topPhotoUrl = ""
    var isVip = false
    var coin = 0
    var bcoin = 0f
    var isEmailVerified = false
    var isMobileVerified = false
    var following = 0
    var followers = 0
  }
  
  const val loginURL = "https://passport.bilibili.com/qrcode/getLoginUrl"
  const val checkURL = "https://passport.bilibili.com/qrcode/getLoginInfo"
  
  lateinit var buvid3: String
  var master = Login.UserInfo()
  
  fun fetchCookie(cookieStore: CookieStore) {
    val cookie = cookieStore.get(URI.create("http://passport.bilibili.com"))
    cookie.forEach {
      when(it.name) {
        "DedeUserID" -> {
          Login.master.mid = it.value
          return@forEach
        }
        "buvid3" -> {
          Login.buvid3 = it.value
          return@forEach
        }
      }
    }
  }
  
  suspend fun getLoginURL(): Pair<String, String> {
    val result = client.get(
        Login.loginURL, mapOf(
        "Accept" to "application/json, text/javascript, */*; q=0.01",
        "Referer" to "https://passport.bilibili.com/login",
        "X-Requested-With" to "XMLHttpRequest"
    )
    ).json().check()
    return result["data"].let { it["url"].content to it["oauthKey"].content }
  }
  
  suspend fun checkLogin(oauthKey: String): Boolean {
    val result = client.post(
        Login.checkURL, mapOf(
        "Accept" to "application/json, text/javascript, */*; q=0.01",
        "Referer" to "https://passport.bilibili.com/login",
        "X-Requested-With" to "XMLHttpRequest"
    ), mapOf("oauthKey" to oauthKey, "gourl" to "https://www.bilibili.com/")
    ).json()
    return result["status"].boolean
  }
  
  suspend fun addAdditionalCookies() {
    client.get(
        "https://data.bilibili.com/v/web/web_page_view?mid=${Login.master.mid}",
        mapOf("Referer" to "https://www.bilibili.com/")
    )
    cookieStore.add(
        URI.create("https://www.bilibili.com/"),
        HttpCookie("CURRENT_QUALITY", "120").also { it.path = "/" }
    )
  }
  
  val cachedUserName = ConcurrentHashMap<String, String>()
  suspend fun getUserNameCached(mid: String): String {
    return Login.cachedUserName.getOrPut(mid) {
      val result = client.get(
          "https://api.bilibili.com/x/space/acc/info?mid=$mid&jsonp=jsonp",
          mapOf("Referer" to "https://space.bilibili.com/$mid")
      ).json().check()
      result["data", "name"].content
    }
  }
  
  suspend fun fetchUserInfo(mid: String): Login.UserInfo {
    val userInfo = Login.UserInfo()
    userInfo.mid = mid
    val info = client.get(
        "https://api.bilibili.com/x/space/acc/info?mid=$mid&jsonp=jsonp",
        mapOf("Referer" to "https://space.bilibili.com/$mid")
    ).json().check()
    val stat = client.get(
        "https://api.bilibili.com/x/relation/stat?vmid=$mid&jsonp=jsonp",
        mapOf("Referer" to "https://space.bilibili.com/$mid")
    ).json()
    userInfo.apply {
      info["data"].also { data ->
        name = data["name"].content
        faceUrl = data["face"].content
        sign = data["sign"].content.replace(Regex("\\s"), " ")
        sex = data["sex"].content
        level = data["level"].int
        topPhotoUrl = data["top_photo"].content
        isVip = data["vip", "type"].int > 0
        coin = data["coins"].int
      }
      stat["data"].also { data ->
        following = data["following"].int
        followers = data["follower"].int
      }
    }
    return userInfo
  }
  
  suspend fun fetchMasterInfo(): Login.UserInfo {
    val userInfo = Login.UserInfo()
    val result = client.get(
        "https://api.bilibili.com/x/web-interface/nav",
        mapOf("Referer" to "https://www.bilibili.com/")
    ).json().check()
    val data = result["data"]
    val isLogin = data["isLogin"].boolean
    if(!isLogin) throw Exception()
    userInfo.apply {
      mid = data["mid"].content
      this.isLogin = data["isLogin"].boolean
      isEmailVerified = data["email_verified"].int != 0
      isMobileVerified = data["mobile_verified"].int != 0
      data["level_info"].also { levelInfo ->
        currentExp = levelInfo["current_exp"].int
        nextExp = levelInfo["next_exp"].int
      }
      bcoin = data["wallet", "bcoin_balance"].float
    }
    return userInfo
  }
}