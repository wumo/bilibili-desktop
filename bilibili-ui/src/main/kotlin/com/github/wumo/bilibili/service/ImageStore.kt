@file:UseSerializers(AtomicIntSerializer::class)
@file:Suppress("NOTHING_TO_INLINE")

package com.github.wumo.bilibili.service

import com.github.wumo.bilibili.ui.MainController.uiScope
import com.github.wumo.bilibili.util.AtomicIntSerializer
import com.github.wumo.bilibili.util.Settings.configPath
import com.github.wumo.bilibili.util.uiLaunch
import javafx.beans.property.SimpleObjectProperty
import javafx.scene.image.Image
import kotlinx.coroutines.CompletableJob
import kotlinx.coroutines.channels.Channel.Factory.UNLIMITED
import kotlinx.serialization.Serializable
import kotlinx.serialization.UseSerializers
import java.nio.file.Paths
import java.util.concurrent.atomic.AtomicInteger
import kotlin.collections.set

object ImageStore {
  @Serializable
  class CacheImages(
      val value: MutableMap<String, String>,
      var counter: AtomicInteger = AtomicInteger(0)
  )
  
  data class LodaImageTask(val url: String, val prop: SimpleObjectProperty<Image>,
                           val width: Int, val height: Int)
  
  class AsyncImage(val url: String, width: Int = 0, height: Int = 0,
                   imageHolder: String? = null)
    : SimpleObjectProperty<Image>() {
    init {
      val cachedImg = ImageStore[url, width, height]
      if (cachedImg != null)
        value = cachedImg
      else {
        if (imageHolder != null)
          value = getOrCache(imageHolder, width, height)
        loadImageService.offer(LodaImageTask(url, this, width, height))
      }
    }
  }
  
  const val defaultBackgroundImage = "/img/background.png"
  const val defaultAvatarImage = "/img/akari_130x130.png"
  const val defaultVideoImage = "/img/video-placeholder.png"
  
  private val destDir = configPath.resolve("cache").toString()
  
  private val cacheImages = CacheImages(mutableMapOf(), AtomicInteger(0))
  private val cachedBufferedImage = mutableMapOf<String, Image>()
  
  val loadImageService =
      object : RestartableService<LodaImageTask>(uiScope) {
        override suspend fun onTask(task: LodaImageTask, job: CompletableJob) {
          val (url, prop, width, height) = task
          val img = getOrCache(url, width, height)
          uiLaunch {
            prop.value = img
          }.join()
        }
      }
  
  fun getOrCache(url: String, width: Int = 0, height: Int = 0): Image {
    var img = get(url, width, height)
    if (img != null) return img
    val newUrl = if ((url.startsWith("http://")
            || url.startsWith("https://"))
        && !url.endsWith(".gif") && width != 0 && height != 0)
      "${url}_${width}x${height}.jpg"
    else url
    img = Image(newUrl, width.toDouble(), height.toDouble(), true, false, true)
    return cache(url, width, height, img)
  }
  
  private fun imgID(url: String, width: Int, height: Int) = "${url}_${width}x$height"
  
  operator fun get(url: String, width: Int, height: Int) =
      cachedBufferedImage[imgID(url, width, height)]
  
  private fun cache(url: String, width: Int, height: Int, img: Image): Image {
    val cachedImage = get(url, width, height)
    if (cachedImage != null) return cachedImage
    val id = cacheImages.counter.incrementAndGet()
    val path = Paths.get(destDir, "$id.png")
    val imgId = imgID(url, width, height)
    cacheImages.value[imgId] = path.toString()
    cachedBufferedImage[imgId] = img
    return img
  }
}