package com.github.wumo.bilibili.util

import org.bytedeco.ffmpeg.ffmpeg
import org.bytedeco.javacpp.Loader

object VideoProcessor {
  private val ffmpeg = Loader.load(ffmpeg::class.java)
  
  fun merge(video: String, audio: String, output: String) {
    val process =
      ProcessBuilder(
          ffmpeg,
          "-hide_banner", "-loglevel", "panic", "-y",
          "-i", video,
          "-i", audio,
          "-codec", "copy",
          output
      )
    process.inheritIO().start().waitFor()
  }
  
  fun convert(from: String, to: String) {
    val process =
      ProcessBuilder(
          ffmpeg,
          "-hide_banner", "-loglevel", "panic", "-y",
          "-i", from,
          "-codec", "copy",
          to
      )
    process.inheritIO().start().waitFor()
  }
}