package com.github.wumo.bilibili.api

data class TidOption(val name: String, val tid: Int, val count: Int)
data class OrderOption(val name: String, val value: Int) {
  override fun toString() = name
}

data class SortOption(val tid: TidOption?, val order: OrderOption?)

data class CachedMediasKey(val fid: String,
                           val page: Int,
                           val tid: Int,
                           val order: Int)