dependencies {
  val ktor_version = "1.3.1"
  implementation("org.jetbrains.kotlinx:kotlinx-io-jvm:0.1.16")
  implementation("io.ktor:ktor-client-websockets:$ktor_version")
//  implementation("io.ktor:ktor-client-cio:$ktor_version")
  implementation("io.ktor:ktor-client-logging-jvm:$ktor_version")
  implementation("io.ktor:ktor-client-okhttp:$ktor_version")
  testImplementation("org.junit.jupiter:junit-jupiter-api:5.4.2")
}