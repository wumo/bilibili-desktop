package com.github.wumo.bilibili.service.download

import com.github.wumo.bilibili.api.API.client
import com.github.wumo.bilibili.util.getResp
import com.github.wumo.bilibili.util.ioLaunch
import kotlinx.coroutines.Job
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.isActive
import kotlinx.coroutines.joinAll
import kotlin.math.ceil
import kotlin.math.min

suspend fun downloadFile(url: String, mediaID: String,
                         buffer: ByteArray,
                         offset: Long, totalSize: Long,
                         threadBegin: () -> Unit = {},
                         threadEnd: () -> Unit = {},
                         progress: (Long) -> Unit): Int = coroutineScope {
  val maxSize = min(buffer.size.toLong(), totalSize - offset)
  val endOffset = min(offset + buffer.size, totalSize) - 1
  var workOffset = offset
  try {
    threadBegin()
    val resp = client.getResp(
        url,
        mapOf("Range" to "bytes=$offset-$endOffset",
              "Referer" to "https://www.bilibili.com/video/av$mediaID"))
    resp.body!!.use { body ->
      val input = body.byteStream()
      do {
        val bytes = input.read(buffer, (workOffset - offset).toInt(),
                               (endOffset - workOffset + 1).toInt())
        if(bytes >= 0) {
          progress(bytes.toLong())
          workOffset += bytes
        }
      } while(isActive && bytes > 0)
    }
  } finally {
    threadEnd()
  }
  assert(workOffset - offset == maxSize)
  maxSize.toInt()
}

suspend fun multiDownloadFile(url: String, mediaID: String,
                              buffer: ByteArray,
                              offset: Long, totalSize: Long,
                              numThread: Int, blockSize: Int,
                              threadBegin: () -> Unit = {},
                              threadEnd: () -> Unit = {},
                              progress: (Long) -> Unit): Int = coroutineScope {
  val maxSize = min(buffer.size.toLong(), totalSize - offset)
  val numWorks = ceil(maxSize * 1.0 / blockSize).toInt()
  val numWorkers = min(numWorks, numThread)
  val works = Channel<WorkRange>(numWorks)
  var globalOffset = offset
  val endOffsetExclusive = min(offset + buffer.size, totalSize)
  while(globalOffset < endOffsetExclusive) {
    val workSize = min(blockSize.toLong(), totalSize - globalOffset)
    works.offer(WorkRange(globalOffset, globalOffset + workSize - 1))
    globalOffset += workSize
  }
  val jobs = mutableListOf<Job>()
  repeat(numWorkers) {
    jobs += ioLaunch {
      try {
        threadBegin()
        while(true) {
          val work = works.poll() ?: break
          var workOffset = work.startOffset
          val resp = client.getResp(
              url,
              mapOf("Range" to "bytes=${workOffset}-${work.endOffset}",
                    "Referer" to "https://www.bilibili.com/video/av$mediaID"))
          resp.body!!.use { body ->
            val input = body.byteStream()
            do {
              val bytes = input.read(buffer, (workOffset - offset).toInt(),
                                     (work.endOffset - workOffset + 1).toInt())
              if(bytes >= 0) {
                progress(bytes.toLong())
                workOffset += bytes
              }
            } while(isActive && bytes > 0)
          }
          assert(workOffset - 1 == work.endOffset)
        }
      } finally {
        threadEnd()
      }
    }
  }
  jobs.joinAll()
  maxSize.toInt()
}