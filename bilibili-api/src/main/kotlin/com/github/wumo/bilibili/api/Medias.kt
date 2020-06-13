package com.github.wumo.bilibili.api

import com.github.wumo.bilibili.model.MediaPage
import com.github.wumo.bilibili.model.MediaType.*

object Medias {
  suspend fun extractInfo(page: MediaPage) =
    when(page.type) {
      Video -> Videos.extract(page.avid, page.cid)
      Audio -> Audios.extract(page.cid)
      Bangumi -> Bangumis.extract(page.avid2, page.cid, page.epid)
      Other -> throw Exception("cannot download this type")
    }
}