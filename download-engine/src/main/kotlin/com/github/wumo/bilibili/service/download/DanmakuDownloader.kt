package com.github.wumo.bilibili.service.download

import com.github.wumo.bilibili.api.Danmaku
import com.github.wumo.bilibili.model.MediaPage
import com.github.wumo.bilibili.service.ConsumerService
import com.github.wumo.bilibili.service.StatefulService
import com.github.wumo.bilibili.util.apiScope
import kotlinx.coroutines.CompletableJob
import java.nio.file.Paths

class DanmakuDownloader: StatefulService<DownloadPage>(apiScope) {
  lateinit var fileOpTerminator: ConsumerService<FileOp>
  
  override suspend fun onTask(task: DownloadPage, job: CompletableJob) {
    val danmaku = Danmaku.fetch(task.page.cid)
    val danmakuFile = Paths.get(task.progress!!.downloadDir,
                                "${task.progress!!.fileName}.xml").toString()
    fileOpTerminator.offer(WriteFileOp(danmakuFile, danmaku.size.toLong(), danmaku, 0, danmaku.size))
      .join()
    fileOpTerminator.offer(CloseFileOp(danmakuFile))
  }
  
  override fun onRemove(task: DownloadPage) {
  }
}