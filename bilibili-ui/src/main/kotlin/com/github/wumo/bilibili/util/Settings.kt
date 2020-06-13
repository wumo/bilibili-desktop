package com.github.wumo.bilibili.util

import com.github.wumo.bilibili.service.download.DownloadConfig
import com.github.wumo.bilibili.service.download.DownloadService
import com.github.wumo.bilibili.service.download.History
import kotlinx.serialization.Serializable
import java.nio.file.Paths

object Settings {
  val configPath = Paths.get(System.getProperty("user.home"), ".bilibili")!!
  
  @Serializable
  data class PlayerConfig(var volume: Double = 0.5)
  
  @Serializable
  class Setting(var downloadConfig: DownloadConfig = DownloadConfig(),
                val playerConfig: PlayerConfig = PlayerConfig())
  
  val settingFile = configPath.resolve("setting.json").toFile().apply {
    if(!exists()) {
      parentFile?.mkdirs()
      createNewFile()
      writeText(jsonPretty.stringify(Setting.serializer(), Setting()))
    }
  }
  
  val downloadConfigFile = configPath.resolve("download.json").toFile().apply {
    if(!exists()) {
      parentFile?.mkdirs()
      createNewFile()
      writeText(jsonPretty.stringify(History.serializer(), History(mutableMapOf())))
    }
  }
  
  lateinit var setting: Setting
  
  fun load() {
    val data = settingFile.readText()
    setting = jsonPretty.parse(Setting.serializer(), data)
    DownloadService.downloadConfig = setting.downloadConfig
  }
  
  fun save() {
    setting.downloadConfig = DownloadService.downloadConfig
    val data = jsonPretty.stringify(Setting.serializer(), setting)
    settingFile.writeText(data)
  }
}