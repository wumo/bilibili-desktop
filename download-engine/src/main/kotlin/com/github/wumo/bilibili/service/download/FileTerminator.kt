package com.github.wumo.bilibili.service.download

import com.github.wumo.bilibili.service.SimpleService
import com.github.wumo.bilibili.util.VideoProcessor
import com.github.wumo.bilibili.util.apiScope
import com.github.wumo.bilibili.util.ensureSuccess
import kotlinx.coroutines.CompletableJob
import java.io.File
import java.io.RandomAccessFile
import java.nio.file.Files
import java.nio.file.Paths
import java.nio.file.StandardCopyOption

sealed class FileOp

class WriteFileOp(val filePath: String, val totalSize: Long,
                  val bytes: ByteArray, val offset: Long, val size: Int): FileOp()

class CloseFileOp(val filePath: String): FileOp()

class MoveFileOp(val from: String, val to: String): FileOp()

class DeleteFileOp(val files: List<String>): FileOp()

data class MergeVideoAudioOp(val videoPath: String,
                             val audioPath: String,
                             val targetPath: String): FileOp()

data class ConvertOp(val fromPath: String, val toPath: String): FileOp()

class FileTerminator: SimpleService<FileOp>(apiScope) {
  private val files =
    mutableMapOf<String, RandomAccessFile>()
  
  override suspend fun onTask(task: FileOp, job: CompletableJob) {
    when(task) {
      is WriteFileOp -> {
        val file = files.getOrPut(task.filePath) {
          ensureSuccess {
            File(task.filePath).parentFile?.mkdirs()
            RandomAccessFile(task.filePath, "rw").apply {
              setLength(task.totalSize)
            }
          }
        }
        ensureSuccess {
          file.seek(task.offset)
          file.write(task.bytes, 0, task.size)
        }
      }
      is CloseFileOp -> ensureSuccess {
        val file = files.remove(task.filePath)!!
        file.close()
      }
      is MoveFileOp -> ensureSuccess {
        Files.move(Paths.get(task.from), Paths.get(task.to),
                   StandardCopyOption.REPLACE_EXISTING)
      }
      is MergeVideoAudioOp -> ensureSuccess {
        val (video, audio, target) = task
        VideoProcessor.merge(video, audio, target)
      }
      is ConvertOp -> ensureSuccess {
        val (from, to) = task
        VideoProcessor.convert(from, to)
      }
      is DeleteFileOp -> ensureSuccess {
        task.files.forEach {
          Files.delete(Paths.get(it))
        }
      }
    }
  }
}
