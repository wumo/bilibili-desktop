import org.gradle.internal.os.OperatingSystem

plugins {
  application
  id("com.github.johnrengelman.shadow")
  id("org.beryx.runtime")
}

repositories {
  jcenter()
  mavenLocal()
  maven("https://oss.sonatype.org/content/repositories/snapshots")
  maven(url = "https://jitpack.io")
}

val os = "${osdetector.os}-${osdetector.arch}"
dependencies {
  api(project(":download-engine"))
  val coroutineVer = "1.3.5" //"1.3.5-$kotlinVer"
  implementation("org.jetbrains.kotlinx:kotlinx-coroutines-javafx:$coroutineVer")
  implementation("io.nayuki:qrcodegen:1.6.0")
  implementation("no.tornado:tornadofx:2.0.0-SNAPSHOT")
  implementation("org.openjfx:javafx-swing:13:win")
  implementation("org.openjfx:javafx-controls:13:win")
  implementation("org.openjfx:javafx-graphics:13:win")
  
  implementation("com.github.wumo:video-player:0.0.2")
  implementation("com.github.wumo:common-utils:1.0.8")
  
}

application {
  mainClassName = "com.github.wumo.bilibili.MainKt"
}

tasks {
  shadowDistZip {
    val fileName = "${project.name}-${project.version}-$os.${archiveExtension.get()}"
    doLast {
      file(archiveFile).renameTo(file(destinationDirectory.file(fileName)))
    }
  }
}

runtime {
  addOptions("--strip-debug", "--compress", "2", "--no-header-files", "--no-man-pages")
  addModules("jdk.crypto.ec")
  additive.set(true)
  
  jpackage {
    imageName = "Bilibili Desktop"
    appVersion = project.version.toString()
    val icon = sourceSets.main.get().resources.first { it.name == "bilibili.ico" }
    imageOptions = listOf("--icon", icon.path)
    installerOptions = listOf("--app-version", version.toString())
    if (OperatingSystem.current().isWindows) {
      skipInstaller = true
      // install wixtoolset first
      installerType = "exe"
      // imageOptions = ['--win-console']
      installerOptions = installerOptions + listOf("--win-per-user-install", "--win-dir-chooser", "--win-menu", "--win-shortcut")
    }
  }
}