package com.github.wumo.bilibili.api

import com.github.wumo.bilibili.util.errorIf
import com.github.wumo.bilibili.util.get
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.content
import kotlinx.serialization.json.int
import kotlin.math.ceil

const val MaxFavResourcePerPage = 20
val Int.pagesNeeded
  get() = ceil(this.toDouble() / MaxFavResourcePerPage).toInt()

fun JsonElement.check(): JsonElement {
  errorIf(this["code"].int != 0) { this["message"].content }
  return this
}