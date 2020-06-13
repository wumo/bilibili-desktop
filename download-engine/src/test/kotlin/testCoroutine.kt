import com.github.wumo.bilibili.util.ioLaunch
import kotlinx.coroutines.*
import kotlinx.coroutines.channels.produce
import kotlinx.coroutines.selects.select
import kotlin.Exception

fun main() {
  runBlocking {
    ioLaunch {
      val job = Job()
      job.invokeOnCompletion {
        println("${Thread.currentThread()} outer complete $it")
      }
      ioLaunch(job) {
        var i = 0
        while(true) {
          i = 0
        }
      }
      job.cancel()
      println("${Thread.currentThread()} after cancel")
      job.join()
    }
  }
}