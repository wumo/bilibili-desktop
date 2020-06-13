@file:Suppress("EXPERIMENTAL_API_USAGE")

package com.github.wumo.bilibili.util

import kotlinx.coroutines.*
import kotlinx.coroutines.Dispatchers.IO

val apiScope = CoroutineScope(SupervisorJob())

suspend fun contextWith(parent: CompletableJob, block: suspend CoroutineScope.() -> Unit) {
  withContext(parent) {
    parent.complete()
    block()
  }
}

fun CoroutineScope.ioLaunch(parent: CompletableJob? = null, block: suspend CoroutineScope.() -> Unit) =
  if(parent != null)
    launch(IO + parent) {
      parent.complete()
      block()
    }
  else
    launch(IO, block = block)

fun CoroutineScope.ioLaunchHere(parent: CompletableJob? = null, block: suspend CoroutineScope.() -> Unit) =
  if(parent != null)
    launch(IO + parent, CoroutineStart.UNDISPATCHED) {
      parent.complete()
      block()
    }
  else
    launch(IO, CoroutineStart.UNDISPATCHED, block = block)
