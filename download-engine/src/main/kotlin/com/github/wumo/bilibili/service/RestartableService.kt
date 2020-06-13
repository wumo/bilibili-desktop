package com.github.wumo.bilibili.service

import com.github.wumo.bilibili.util.ioLaunch
import com.github.wumo.bilibili.util.ioLaunchHere
import kotlinx.coroutines.CompletableJob
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.cancelAndJoin
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.channels.Channel.Factory.UNLIMITED

abstract class RestartableService<Task: Any>(scope: CoroutineScope)
  : ConsumerService<Task>, CoroutineScope by scope {
  
  fun restart() {
    queuingTasks = Channel(UNLIMITED)
    control.offer(Unit)
  }
  
  override fun offer(task: Task, parentJob: Job?): Job {
    val job = Job(parentJob)
    queuingTasks.offer(Req(task, job))
    return job
  }
  
  private val control = Channel<Unit>(Channel.CONFLATED)
  private var queuingTasks = Channel<Req<Task>>(UNLIMITED)
  
  init {
    ioLaunch {
      var job: Job? = null
      for(param in control) {
        job?.cancelAndJoin()
        job = ioLaunchHere {
          for((task, job) in queuingTasks) {
            try {
              onTask(task, job)
              job.complete()
            } catch(e: Exception) {
              job.completeExceptionally(e)
            }
          }
        }
      }
    }
  }
  
  protected abstract suspend fun onTask(task: Task, job: CompletableJob)
}
