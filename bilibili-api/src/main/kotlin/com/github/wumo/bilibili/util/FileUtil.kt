package com.github.wumo.bilibili.util

import okhttp3.HttpUrl.Companion.toHttpUrl
import java.nio.file.InvalidPathException
import java.nio.file.Paths

//val escapeRegex = Regex("""[^\w\p{sc=Han} !！【】\-\[\].～#“”()｜･？?の]""")
val escapeRegex = Regex("""[\\/]""")

//val escapeRegex = Regex("""[^\w]""")
val whiteRegex = Regex("""\s+""")
fun String.escapePath(): String {
  return this.replace(escapeRegex, "")
}

fun String.toValidFileName(): String {
  var processed = this.escapePath()
  
  while(true) {
    try {
      Paths.get(processed)
      break
    } catch(e: InvalidPathException) {
      processed = processed.replace(e.input[e.index].toString(), "")
        .replace(whiteRegex, " ")
      if(processed.isBlank())
        processed = "( )"
    }
  }
  return processed
}

fun String.ext(): String {
  val url = toHttpUrl()
  val dotExt = url.pathSegments.lastOrNull() ?: ".mp4"
  val idx = dotExt.lastIndexOf('.')
  val ext = if(idx == -1) "mp4"
  else dotExt.substring(idx + 1)
  return when(ext) {
    "flv" -> "flv"
    else -> "mp4"
  }
}