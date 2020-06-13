package com.github.wumo.bilibili.service.download

import com.github.wumo.bilibili.api.Medias
import com.github.wumo.bilibili.service.StatefulService
import com.github.wumo.bilibili.service.ConsumerService
import com.github.wumo.bilibili.service.download.DownloadService.downloadConfig
import com.github.wumo.bilibili.service.download.State.Downloading
import com.github.wumo.bilibili.service.download.StateMonitor.concurrentThreads
import com.github.wumo.bilibili.service.download.StateMonitor.downloadedBytes
import com.github.wumo.bilibili.service.download.StateMonitor.mediaPagesToDownload
import com.github.wumo.bilibili.service.download.StateMonitor.monitor
import com.github.wumo.bilibili.service.download.StateMonitor.stateUpdatedMediaPageMonitors
import com.github.wumo.bilibili.util.LogService
import com.github.wumo.bilibili.util.apiScope
import com.github.wumo.bilibili.util.ioLaunch
import com.github.wumo.bilibili.util.print
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CompletableJob
import kotlinx.coroutines.isActive
import kotlinx.coroutines.sync.Semaphore
import java.io.File
import java.nio.file.Paths

class ModernPageDownloader : StatefulService<DownloadPage>(apiScope) {
  lateinit var fileOpTerminator: ConsumerService<FileOp>
  
  private val config = downloadConfig.high
  private val semaphore = Semaphore(config.concurrent)
  
  override fun onOffer(task: DownloadPage, job: CompletableJob) {
    mediaPagesToDownload.incrementAndGet()
    job.invokeOnCompletion {
      mediaPagesToDownload.decrementAndGet()
    }
  }
  
  override suspend fun onTask(task: DownloadPage, job: CompletableJob) {
    semaphore.acquire()
    job.invokeOnCompletion { semaphore.release() }
    ioLaunch(job) {
      val pageMonitor = monitor(task.page)
      pageMonitor.state.set(Downloading)
      stateUpdatedMediaPageMonitors += pageMonitor
      val numThreads = config.threads
      val blockSize = config.blockSize
      val blockNum = config.blockNum
      val bytes = ByteArray(blockSize * blockNum)
      
      val info = task.info
      val progress = task.progress
      pageMonitor.totalSize = info!!.totalSize
      val tempVideo = Paths.get(progress!!.downloadDir,
          "${progress.fileName}.video").toString()
      val tempAudio = Paths.get(progress.downloadDir,
          "${progress.fileName}.audio").toString()
      download(task, tempAudio, false, task.page.avid, bytes, info.audioSize,
          numThreads, blockSize, pageMonitor)
      download(task, tempVideo, true, task.page.avid, bytes, info.videoSize,
          numThreads, blockSize, pageMonitor)
      val filePath = task.progress!!.filePath().toString()
      fileOpTerminator.offer(MergeVideoAudioOp(tempVideo, tempAudio, filePath)).join()
      fileOpTerminator.offer(DeleteFileOp(listOf(tempVideo, tempAudio))).join()
    }
  }
  
  private suspend fun download(task: DownloadPage, tempFile: String,
                               isVideo: Boolean, mediaID: String,
                               bytes: ByteArray, totalSize: Long,
                               numThread: Int, blockSize: Int,
                               pageMonitor: MediaPageMonitor) {
    var downloaded = if (File(tempFile).exists()) {
      if (isVideo) task.progress!!.videoDownloaded
      else task.progress!!.audioDownloaded
    } else 0L
    pageMonitor.progress.addAndGet(downloaded)
    if (downloaded == totalSize) return
    while (downloaded < totalSize) {
      if (task.info!!.needToFetchUrl())
        task.info = Medias.extractInfo(task.page)
      val url = if (isVideo) task.info!!.videoURL!! else task.info!!.audioURL!!
      try {
        val size = multiDownloadFile(url, mediaID, bytes, downloaded, totalSize,
            numThread, blockSize,
            { concurrentThreads.incrementAndGet() },
            { concurrentThreads.decrementAndGet() }) {
          downloadedBytes.addAndGet(it)
          pageMonitor.progress.addAndGet(it)
        }
        fileOpTerminator.offer(WriteFileOp(tempFile, totalSize, bytes, downloaded, size))
            .join()
        if (isVideo) task.progress!!.videoDownloaded += size
        else task.progress!!.audioDownloaded += size
        downloaded += size
      } catch (e: CancellationException) {
        throw e
      } catch (e: Exception) {
        if (!isActive) throw  e
        LogService.log { e.print { "fail to download modern url=$url,download=$downloaded,totalSize=$totalSize" } }
      }
    }
    fileOpTerminator.offer(CloseFileOp(tempFile)).join()
  }
  
  override fun onRemove(task: DownloadPage) {
    TODO("Not yet implemented")
  }
}