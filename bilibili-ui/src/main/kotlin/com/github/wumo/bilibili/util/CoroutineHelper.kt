package com.github.wumo.bilibili.util

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers.Main
import kotlinx.coroutines.launch

fun CoroutineScope.uiLaunch(block: suspend CoroutineScope.() -> Unit) =
  launch(Main, block = block)