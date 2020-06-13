import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

plugins {
  application
  `maven-publish`
  kotlin("jvm") version "1.3.71" apply false
  kotlin("plugin.serialization") version "1.3.71" apply false
  id("com.github.johnrengelman.shadow") version "5.2.0" apply false
  id("org.beryx.runtime") version "1.8.0" apply false
  id("com.google.osdetector") version "1.6.2" apply false
}

subprojects {
  apply(plugin = "maven-publish")
  apply(plugin = "org.jetbrains.kotlin.jvm")
  apply(plugin = "org.jetbrains.kotlin.plugin.serialization")
  apply(plugin = "com.google.osdetector")
  
  group = "com.github.waahoo"
  version = "0.0.1"
  
  val kotlinVer = "1.3.71"
  val coroutineVer = "1.3.5" //"1.3.5-$kotlinVer"
  val serializationVer = "0.20.0" //"0.20.0-$kotlinVer"
  
  repositories {
    jcenter()
    mavenLocal()
    maven("https://oss.sonatype.org/content/repositories/snapshots")
    maven(url = "https://jitpack.io")
  }
  
  tasks.withType<KotlinCompile> {
    kotlinOptions.jvmTarget = "9"
  }
  
  dependencies {
    implementation(kotlin("stdlib-jdk8"))
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core:$coroutineVer")
    implementation("org.jetbrains.kotlinx:kotlinx-serialization-runtime:$serializationVer")
    implementation("org.apache.commons:commons-lang3:3.9")
    implementation("org.apache.commons:commons-collections4:4.4")
    implementation("com.squareup.okhttp3:okhttp:4.4.1")
    implementation("com.squareup.okhttp3:logging-interceptor:4.4.1")
    implementation("com.squareup.okhttp3:okhttp-urlconnection:4.4.1")
  }
  
  val kotlinSourcesJar by tasks
  
  publishing {
    publications {
      create<MavenPublication>("maven") {
        from(components["kotlin"])
        artifact(kotlinSourcesJar)
      }
    }
  }
}