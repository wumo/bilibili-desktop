package com.github.wumo.bilibili.service

import com.github.wumo.bilibili.util.ioLaunch
import kotlinx.coroutines.CompletableJob
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.channels.Channel.Factory.UNLIMITED

abstract class SimpleService<Task: Any>(scope: CoroutineScope)
  : ConsumerService<Task>, CoroutineScope by scope {
  override fun offer(task: Task, parentJob: Job?): Job {
    val job = Job(parentJob)
    queuingTasks.offer(Req(task, job))
    return job
  }
  
  private val queuingTasks = Channel<Req<Task>>(UNLIMITED)
  
  init {
    ioLaunch {
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
  
  protected abstract suspend fun onTask(task: Task, job: CompletableJob)
}
