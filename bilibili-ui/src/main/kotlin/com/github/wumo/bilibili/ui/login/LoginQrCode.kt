package com.github.wumo.bilibili.ui.login

import com.github.wumo.bilibili.api.Login
import com.github.wumo.bilibili.ui.MainController.uiScope
import com.github.wumo.bilibili.util.PersistentCookieStore
import com.github.wumo.bilibili.util.uiLaunch
import io.nayuki.qrcodegen.QrCode
import javafx.embed.swing.SwingFXUtils
import javafx.geometry.Insets
import javafx.geometry.Pos
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import tornadofx.*
import java.lang.System.currentTimeMillis as time

class LoginQrCode: Fragment(), CoroutineScope by uiScope {
  init {
    title = "登录或者访问"
  }
  
  val cdTime = 180_000L
  val loopTime = 3_000L
  private lateinit var job: Job
  
  override fun onUndock() {
    job.cancel()
  }
  
  override val root =
    vbox {
      alignment = Pos.CENTER
      spacing = 5.0
      hbox {
        alignment = Pos.CENTER
        vboxConstraints {
          margin = Insets(20.0, 5.0, 0.0, 5.0)
        }
        spacing = 5.0
        label("UP：")
        val upID = textfield().textProperty()
        button("访问") {
          action {
            val mid = upID.value
            UserLogin.visitUser(mid)
            this@LoginQrCode.close()
          }
        }
      }
      canvas {
        width = 200.0
        height = 200.0
        val g = graphicsContext2D
        job = uiLaunch {
          outer@ while(isActive) {
            val (url, oauthKey) = Login.getLoginURL()
            val img = QrCode.encodeText(url, QrCode.Ecc.MEDIUM)
              .toImage(4, 10)
            g.drawImage(SwingFXUtils.toFXImage(img, null), 0.0, 0.0, 200.0, 200.0)
            val last = time()
            while(time() - last < cdTime) {
              delay(loopTime)
              if(Login.checkLogin(oauthKey)) {
                Login.addAdditionalCookies()
                Login.fetchCookie(PersistentCookieStore)
                PersistentCookieStore.save()
                UserLogin.visitMasterLogin()
                this@LoginQrCode.close()
                break@outer
              }
            }
          }
        }
      }
      
    }
}