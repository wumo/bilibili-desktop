package com.github.wumo.bilibili.api

import kotlinx.coroutines.runBlocking
import org.junit.jupiter.api.Test

internal class ContributesTest {
  
  @Test
  fun getContributeFolders() {
    runBlocking {
      Contributes.getContributeFolders("1939319")
    }
  }
}