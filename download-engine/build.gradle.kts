dependencies {
  api(project(":bilibili-api"))
  implementation("org.bytedeco:ffmpeg:4.2.1-1.5.2")
  implementation("org.bytedeco:ffmpeg:4.2.1-1.5.2:${osdetector.classifier}")
}
