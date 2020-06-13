package com.github.wumo.bilibili.ui.folders.common

import com.github.wumo.bilibili.api.OrderOption
import com.github.wumo.bilibili.api.SortOption
import com.github.wumo.bilibili.api.TidOption
import com.github.wumo.bilibili.model.Folder
import com.github.wumo.bilibili.model.MediaResource
import com.github.wumo.bilibili.service.ImageStore.AsyncImage
import com.github.wumo.bilibili.service.ImageStore.defaultVideoImage
import com.github.wumo.bilibili.service.ImageStore.loadImageService
import com.github.wumo.bilibili.service.OnceService
import com.github.wumo.bilibili.service.download.DownloadService
import com.github.wumo.bilibili.ui.MainController.uiScope
import com.github.wumo.bilibili.ui.login.UserLogin.currentUser
import com.github.wumo.bilibili.util.uiLaunch
import javafx.beans.property.SimpleListProperty
import javafx.beans.property.SimpleObjectProperty
import javafx.collections.FXCollections.observableArrayList
import javafx.collections.FXCollections.observableSet
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.isActive

data class ListFoldersFunc(val name: String,
                           val fetchFunc: suspend (String) -> List<Folder>) {
  val folderProp = observableArrayList<Folder>()!!
}

abstract class ControllerTemplate : CoroutineScope by uiScope {
  
  abstract val tabIndex: Int
  abstract val folderFuncs: List<ListFoldersFunc>
  abstract val folderPartionFunc: suspend (Folder) -> List<TidOption>
  abstract val fetchMediaFunc: suspend (Folder, Int, Int, Int) -> List<MediaResource>
  abstract val fetchOrderOptions: List<OrderOption>
  
  val tidOptions = observableArrayList<TidOption>()!!
  val resourcesToShow = SimpleListProperty<ResourceUI>(observableArrayList())
  private var shownResources = observableSet<ResourceUI>()!!
  val selectedFolder = SimpleObjectProperty<Folder>()
  val chosenTidOption = SimpleObjectProperty<TidOption>()
  var chosenOrderOption = SimpleObjectProperty<OrderOption>()
  
  var folderClosed = true
  private val fetchFoldersService =
      object : OnceService<String?>(uiScope) {
        override suspend fun onTask(task: String?) {
          if (task == null) {
            folderClosed = true
            return
          }
          val folderProps = folderFuncs.map { it.folderProp }
          val folders = folderFuncs.map { it.fetchFunc(task) }
          uiLaunch {
            folderProps.asSequence()
                .zip(folders.asSequence())
                .forEach { (folderProp, folder) ->
                  folderProp.setAll(folder)
                }
            var toSelect = selectedFolder.value
            if (toSelect == null)
              for (folder in folders)
                if (folder.isNotEmpty()) {
                  toSelect = folder.first()
                  break
                }
            folderClosed = false
            if (toSelect != null) {
              openFolder(toSelect, chosenTidOption.value, chosenOrderOption.value)
              selectedFolder.value = null
              selectedFolder.value = toSelect
            }
          }
        }
      }
  
  private val openFolderService =
      object : OnceService<Pair<Folder, SortOption>?>(uiScope) {
        override suspend fun onTask(task: Pair<Folder, SortOption>?) {
          if (folderClosed) return
          uiLaunch {
            resourcesToShow.value = observableArrayList() //clear graphic cache
            shownResources = observableSet()!!
          }.join()
          loadImageService.restart()
          task ?: return
          val (folder, option) = task
          uiLaunch {
            val options = folderPartionFunc(folder)
            tidOptions.retainAll(options)
            if (tidOptions.size != options.size)
              tidOptions.setAll(options)
            if (chosenTidOption.value == null)
              chosenTidOption.value = tidOptions.firstOrNull()
            if (chosenOrderOption.value == null)
              chosenOrderOption.value = fetchOrderOptions.firstOrNull()
          }.join()
          var page = 0
          while (true) {
            val resources = fetchMediaFunc(folder, page++, option.tid?.tid ?: 0,
                option.order?.value ?: fetchOrderOptions[0].value)
            if (resources.isEmpty()) return
            val uiResources = resources.map { res ->
              if (!isActive) return
              ResourceUI(res, AsyncImage(res.coverURL, coverWidth, coverHeight,
                  defaultVideoImage))
            }
            uiLaunch {
              resourcesToShow.addAll(uiResources)
            }.join()
          }
        }
      }
  
  fun onChosen() {
    if (currentUser.value?.mid?.value?.isNotEmpty() == true)
      fetchFoldersService.restart(currentUser.value.mid.value)
    else onLeave()
  }
  
  fun onLeave() {
    openFolderService.restart(null)
    fetchFoldersService.restart(null)
  }
  
  fun openFolder(folder: Folder?, tid: TidOption?, order: OrderOption?) {
    if (folder == null || folder.mediaCount == 0) return
    selectedFolder.value = folder
    chosenTidOption.value = tid
    chosenOrderOption.value = order
    openFolderService.restart(folder to SortOption(tid, order))
  }
  
  fun resourceShown(item: ResourceUI) {
//    shownResources.add(item)
//    openFolderService.offer(Unit)
  }
  
  fun downloadFolder(folder: Folder, tid: Int, order: Int) {
    DownloadService.downloadFolder(folder, tid, order)
  }
  
  abstract val coverWidth: Int
  abstract val coverHeight: Int
}