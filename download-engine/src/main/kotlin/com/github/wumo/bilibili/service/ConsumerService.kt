package com.github.wumo.bilibili.service

import kotlinx.coroutines.Job

interface ConsumerService<in Task> {
  fun offer(task: Task, parentJob: Job? = null): Job
}