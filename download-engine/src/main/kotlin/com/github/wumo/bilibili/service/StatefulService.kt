package com.github.wumo.bilibili.service

import com.github.wumo.bilibili.util.ioLaunch
import kotlinx.coroutines.CompletableJob
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.cancelAndJoin
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.channels.Channel.Factory.UNLIMITED
import kotlinx.coroutines.selects.select

sealed class ControlMessage<Input>
class PauseAll<Input> : ControlMessage<Input>()
class Pause<Input>(val task: Input) : ControlMessage<Input>()
class ResumeAll<Input> : ControlMessage<Input>()
class Resume<Input>(val task: Input) : ControlMessage<Input>()
class RemoveAll<Input> : ControlMessage<Input>()
class Remove<Input>(val task: Input) : ControlMessage<Input>()

abstract class StatefulService<Task : Any>(scope: CoroutineScope)
  : ConsumerService<Task>, CoroutineScope by scope {
  
  fun pause(task: Task) {
    control.offer(Pause(task))
  }
  
  fun pauseAll() {
    control.offer(PauseAll())
  }
  
  fun resume(task: Task) {
    control.offer(Resume(task))
  }
  
  fun resumeAll() {
    control.offer(ResumeAll())
  }
  
  fun remove(task: Task) {
    control.offer(Remove(task))
  }
  
  fun removeAll() {
    control.offer(RemoveAll())
  }
  
  open fun onOffer(task: Task, job: CompletableJob) {}
  
  override fun offer(task: Task, parentJob: Job?): Job {
    val job = Job(parentJob)
    onOffer(task, job)
    queuingTasks.offer(Req(task, job))
    return job
  }
  
  private val control = Channel<ControlMessage<Task>>(Channel.CONFLATED)
  private val queuingTasks = Channel<Req<Task>>(UNLIMITED)
  private val tasks = mutableMapOf<Task, Job>()
  
  init {
    ioLaunch {
      while (true)
        select<Unit> {
          control.onReceive {
            when (it) {
              is PauseAll -> _pauseAll()
              is ResumeAll -> _resumeAll()
              is RemoveAll -> _removeAll()
              is Pause -> _pause(it.task)
              is Resume -> _resume(it.task)
              is Remove -> _remove(it.task)
            }
          }
          queuingTasks.onReceive { (task, job) ->
            val jobBefore = tasks.getOrPut(task) { job }
            if (job !== jobBefore && !jobBefore.isCancelled) {
              jobBefore.invokeOnCompletion {
                if (it != null) job.completeExceptionally(it)
                else job.complete()
              }
              return@onReceive
            }
            tasks[task] = job
            try {
              onTask(task, job)
              job.complete()
            } catch (e: Exception) {
              job.completeExceptionally(e)
            }
          }
        }
    }
  }
  
  protected abstract suspend fun onTask(task: Task, job: CompletableJob)
  
  private suspend fun _pauseAll() {
    for ((task) in tasks)
      _pause(task)
  }
  
  private suspend fun _pause(task: Task) {
    tasks[task]?.cancelAndJoin()
  }
  
  private fun _resumeAll() {
    for ((task) in tasks)
      _resume(task)
  }
  
  private fun _resume(task: Task) {
    offer(task)
  }
  
  private suspend fun _removeAll() {
    for ((task) in tasks)
      _remove(task)
  }
  
  private suspend fun _remove(task: Task) {
    tasks[task]?.cancelAndJoin()
    tasks.remove(task)
    onRemove(task)
  }
  
  protected abstract fun onRemove(task: Task)
}
