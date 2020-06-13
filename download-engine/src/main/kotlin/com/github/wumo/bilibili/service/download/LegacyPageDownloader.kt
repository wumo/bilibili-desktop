package com.github.wumo.bilibili.service.download

import com.github.wumo.bilibili.api.Medias
import com.github.wumo.bilibili.model.MediaType.Audio
import com.github.wumo.bilibili.service.StatefulService
import com.github.wumo.bilibili.service.ConsumerService
import com.github.wumo.bilibili.service.download.DownloadService.downloadConfig
import com.github.wumo.bilibili.service.download.State.Downloading
import com.github.wumo.bilibili.service.download.StateMonitor.concurrentThreads
import com.github.wumo.bilibili.service.download.StateMonitor.downloadedBytes
import com.github.wumo.bilibili.service.download.StateMonitor.mediaPagesToDownload
import com.github.wumo.bilibili.service.download.StateMonitor.monitor
import com.github.wumo.bilibili.service.download.StateMonitor.stateUpdatedMediaPageMonitors
import com.github.wumo.bilibili.util.*
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CompletableJob
import kotlinx.coroutines.isActive
import kotlinx.coroutines.sync.Semaphore
import java.io.File
import java.nio.file.Paths

class LegacyPageDownloader : StatefulService<DownloadPage>(apiScope) {
  lateinit var fileOpTerminator: ConsumerService<FileOp>
  
  private val config = downloadConfig.normal
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
    val pageMonitor = monitor(task.page)
    ioLaunch(job) {
      pageMonitor.state.set(Downloading)
      stateUpdatedMediaPageMonitors += pageMonitor
      val numThreads = config.threads
      val blockSize = config.blockSize
      val blockNum = config.blockNum
      val bytes = ByteArray(blockSize * blockNum)
      val tempMedia = Paths.get(task.progress!!.downloadDir,
          "${task.progress!!.fileName}.temp").toString()
      
      var downloaded = if (File(tempMedia).exists())
        task.progress!!.downloaded
      else 0L
      val totalSize = task.info!!.totalSize
      pageMonitor.totalSize = totalSize
      pageMonitor.progress.set(downloaded)
      val alreadyDownloaded = downloaded == totalSize
      while (downloaded < totalSize) {
        if (task.info!!.needToFetchUrl())
          task.info = Medias.extractInfo(task.page)
        val url = task.info!!.videoURL ?: task.info!!.audioURL!!
        try {
          val size = multiDownloadFile(url, task.page.avid, bytes, downloaded, totalSize,
              numThreads, blockSize,
              { concurrentThreads.incrementAndGet() },
              { concurrentThreads.decrementAndGet() }) {
            downloadedBytes.addAndGet(it)
            pageMonitor.progress.addAndGet(it)
          }
          fileOpTerminator.offer(WriteFileOp(tempMedia, totalSize, bytes, downloaded, size))
              .join()
          if (task.page.type == Audio) task.progress!!.audioDownloaded += size
          else task.progress!!.videoDownloaded += size
          downloaded += size
        } catch (e: CancellationException) {
          throw e
        } catch (e: Exception) {
          if (!isActive) throw  e
          LogService.log { e.print { "fail to download legacy url=$url,download=$downloaded,totalSize=$totalSize" } }
        }
      }
      if (!alreadyDownloaded)
        fileOpTerminator.offer(CloseFileOp(tempMedia))
      val filePath = task.progress!!.filePath().toString()
      val ext = task.info?.videoURL?.ext() ?: task.info?.audioURL?.ext()!!
      if (ext != defaultMediaFormat)
        fileOpTerminator.offer(ConvertOp(tempMedia, filePath)).join()
      else
        fileOpTerminator.offer(MoveFileOp(tempMedia, filePath)).join()
      fileOpTerminator.offer(DeleteFileOp(listOf(tempMedia))).join()
    }
  }
  
  override fun onRemove(task: DownloadPage) {
    TODO("Not yet implemented")
  }
}