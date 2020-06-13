package com.github.wumo.bilibili.service

import kotlinx.coroutines.CompletableJob

data class Req<Task>(val task: Task, val job: CompletableJob)