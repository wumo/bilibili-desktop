@file:Suppress("NOTHING_TO_INLINE")

package com.github.wumo.bilibili.util

import java.time.Instant
import java.time.LocalDateTime
import java.time.ZoneId
import java.time.format.DateTimeFormatter

inline fun now() = System.currentTimeMillis()
inline fun Long.elapsedS() = (now() - this) / 1000
inline fun dateTimeOfEpochSecond(epoch: Long) = LocalDateTime.ofInstant(Instant.ofEpochSecond(epoch), ZoneId.systemDefault())
inline fun nowEpochSecond() = Instant.now().epochSecond
var formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm")
inline fun LocalDateTime.format() = format(formatter)
inline fun printNow() = dateTimeOfEpochSecond(nowEpochSecond()).format()
