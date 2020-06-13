package com.github.wumo.bilibili.util

inline fun errorIf(condition: Boolean, msg: () -> String = { "error" }) {
  if(condition)
    error(msg())
}