package com.github.wumo.bilibili.api

import com.github.wumo.bilibili.model.Folder
import com.github.wumo.bilibili.model.FolderType.SubscriptionFolder
import kotlinx.coroutines.runBlocking
import org.junit.jupiter.api.Test

class BangumisTest {
  
  @Test
  fun fetchMedias() {
    runBlocking {
      Bangumis.fetchMedias(Folder(SubscriptionFolder, "226388", "comic", "", 80, false)
                           , 0, 0, 0)
    }
  }
  
  @Test
  fun fetchPages() {
    runBlocking {
      Bangumis.getAllSeasons("28223066")
    }
  }
}