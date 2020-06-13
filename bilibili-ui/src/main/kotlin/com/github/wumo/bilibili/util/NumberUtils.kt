package com.github.wumo.bilibili.util

import kotlin.math.floor

fun Number.pretty() = when(this) {
  in 0..9999 -> this.toString()
  else -> String.format("%.1f万", this.toDouble() / 10000.0)
}

private const val _1KB = 1024
private const val _1MB = 1024 * _1KB
private const val _1GB = 1024 * _1MB

fun Number.prettyMem(): String {
  val value = this.toDouble()
  return when {
    0 <= value && value < _1KB -> String.format("%.1fB", value)
    _1KB <= value && value < _1MB -> String.format("%.1fKB", value / _1KB)
    _1MB <= value && value < _1GB -> String.format("%.1fMB", value / _1MB)
    else -> String.format("%.1fGB", value / _1GB)
  }
}

private const val _1min = 60
private const val _1h = 60 * _1min
private const val _1d = 24 * _1h
fun Number.prettyTime(): String {
  val value = this.toDouble()
  return when {
    0 <= value && value < _1min -> "${floor(value)}s"
    _1min <= value && value < _1h -> "${(value / _1min).toInt()}min${(value % _1min).toInt()}s"
    else -> "${(value / _1h).toInt()}h${(value % _1h).toInt()}min"
  }
}