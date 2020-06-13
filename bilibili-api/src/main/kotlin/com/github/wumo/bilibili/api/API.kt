package com.github.wumo.bilibili.api

import com.github.wumo.bilibili.util.close
import okhttp3.JavaNetCookieJar
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import java.net.CookieHandler
import java.net.CookieManager
import java.net.CookiePolicy
import java.net.CookieStore

object API {
  private const val userAgent = "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/80.0.3987.149 Safari/537.36"
  internal lateinit var cookieStore: CookieStore
  lateinit var client: OkHttpClient
  
  fun init(cookieStore: CookieStore = CookieManager().cookieStore) {
    API.cookieStore = cookieStore
    val cookieMgr = CookieManager(cookieStore, CookiePolicy.ACCEPT_ALL)
    CookieHandler.setDefault(cookieMgr)
    
    API.client = OkHttpClient.Builder()
        .cookieJar(JavaNetCookieJar(cookieMgr))
        .addNetworkInterceptor { chain ->
          val req = chain.request()
          chain.proceed(req.newBuilder()
              .header("User-Agent", API.userAgent)
              .build())
        }
//      .addNetworkInterceptor(HttpLoggingInterceptor().setLevel(HttpLoggingInterceptor.Level.BODY))
        .build()
  }
  
  fun close() {
    API.client.close()
  }
}