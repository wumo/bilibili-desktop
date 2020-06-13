package com.github.wumo.bilibili.service

import com.github.wumo.bilibili.util.ioLaunch
import com.github.wumo.bilibili.util.ioLaunchHere
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.cancelAndJoin
import kotlinx.coroutines.channels.Channel

abstract class OnceService<Task>(scope: CoroutineScope)
  : CoroutineScope by scope {
  
  fun restart(task: Task) {
    control.offer(task)
  }
  
  private val control = Channel<Task>(Channel.CONFLATED)
  
  init {
    ioLaunch {
      var job: Job? = null
      for(task in control) {
        job?.cancelAndJoin()
        job = ioLaunch {
          onTask(task)
        }
        job.invokeOnCompletion {
          onFinish()
        }
      }
    }
  }
  
  protected abstract suspend fun onTask(task: Task)
  
  protected open fun onFinish() {}
}
