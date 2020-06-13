@file:Suppress("NOTHING_TO_INLINE")

package com.github.wumo.bilibili.util

import kotlinx.coroutines.suspendCancellableCoroutine
import okhttp3.*
import okhttp3.Headers.Companion.toHeaders
import java.io.File
import java.io.FileOutputStream
import java.io.IOException
import java.io.OutputStream
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException

data class Range(val start: Long, val end: Long, val size: Long) {
  companion object {
    fun Range(start: Long, size: Long) = Range(start, size + start - 1, size)
    val rangePattern = Regex("""(?<unit>\w+)?\s*(?<start>\d+)-(?<end>\d*)?(?:/(?<size>\d+))?""")
    fun parse(range: String): Range? {
      rangePattern.matchEntire(range)?.also { result ->
        result.groups["unit"]?.also { unit ->
          assert(unit.value == "bytes")
        }
        val start = result.groups["start"]!!.value.toLong()
        val end = result.groups["end"]?.value?.toLong() ?: Long.MAX_VALUE
        val size = result.groups["size"]?.value?.toLong() ?: -1L
        return Range(start, end, size)
      }
      return null
    }
  }
}

inline fun url(scheme: String, host: String, path: String,
               vararg queryParams: String
) = HttpUrl.Builder()
  .scheme(scheme)
  .host(host)
  .addPathSegments(path)
  .apply {
    assert(queryParams.size % 2 == 0)
    repeat(queryParams.size / 2) {
      addQueryParameter(queryParams[it * 2], queryParams[it * 2 + 1])
    }
  }.build()

inline fun HttpUrl.url(path: String,
                       vararg queryParams: Any) =
  newBuilder().addPathSegments(path).apply {
    assert(queryParams.size % 2 == 0)
    repeat(queryParams.size / 2) {
      addQueryParameter(queryParams[it * 2].toString(), queryParams[it * 2 + 1].toString())
    }
  }.build()

inline fun headers(vararg headers: String) = Headers.headersOf(*headers)

val emptyHeaders = Headers.Builder().build()

fun OkHttpClient.close() {
  dispatcher.executorService.shutdown()
  connectionPool.evictAll()
  cache?.close()
}

suspend fun OkHttpClient.readBytes(url: String): ByteArray {
  getResp(url).use {
    it.body?.byteStream()?.also { inStream ->
      return inStream.readAllBytes()
    }
  }
  return ByteArray(0)
}

suspend fun OkHttpClient.get(url: String, headers: Map<String, String> = emptyMap()): String {
  getResp(url, headers).use {
    return it.body?.string() ?: ""
  }
}

suspend fun OkHttpClient.get(url: HttpUrl, headers: Headers = emptyHeaders): String {
  getResp(url, headers).use {
    return it.body?.string() ?: ""
  }
}

suspend fun OkHttpClient.getHeaders(
  url: String, headers: Map<String, String> = emptyMap()): Headers {
  headResp(url, headers).use {
    return it.headers
  }
}

suspend fun OkHttpClient.getResp(url: HttpUrl, headers: Headers = emptyHeaders): Response {
  val request = Request.Builder().url(url).headers(headers).build()
  val response = newCall(request).await()
  if(!response.isSuccessful) throw IOException()
  return response
}

suspend fun OkHttpClient.getResp(url: String, headers: Map<String, String> = emptyMap()): Response {
  val headerBuilder = headers.toHeaders()
  val request = Request.Builder().url(url).headers(headerBuilder).build()
  val response = newCall(request).await()
  if(!response.isSuccessful) {
    response.close()
    throw IOException()
  }
  return response
}

suspend fun OkHttpClient.headResp(url: String, headers: Map<String, String> = emptyMap()): Response {
  val headerBuilder = headers.toHeaders()
  val request = Request.Builder().get().url(url).headers(headerBuilder).build()
  val response = newCall(request).await()
  if(!response.isSuccessful) {
    response.close()
    throw IOException()
  }
  return response
}

suspend fun OkHttpClient.post(
  url: String,
  headers: Map<String, String> = emptyMap(),
  form: Map<String, String> = emptyMap()
): String {
  val header = headers.toHeaders()
  val data = FormBody.Builder().apply {
    form.forEach { (k, v) ->
      add(k, v)
    }
  }.build()
  val request = Request.Builder().url(url).headers(header).post(data).build()
  val response = newCall(request).await()
  if(!response.isSuccessful) throw IOException()
  response.use {
    return it.body?.string() ?: ""
  }
}

suspend fun OkHttpClient.download(
  out: OutputStream, url: String,
  headers: Map<String, String> = emptyMap(),
  block: (Long, Long) -> Unit = { _, _ -> }
) {
  getResp(url, headers).use {
    it.body?.apply {
      byteStream().also { input ->
        val total = contentLength()
        val buffer = ByteArray(DEFAULT_BUFFER_SIZE)
        var bytes = input.read(buffer)
        while(bytes >= 0) {
          out.write(buffer, 0, bytes)
          block(bytes.toLong(), total)
          bytes = input.read(buffer)
        }
        block(0, total)
      }
    }
  }
}

suspend fun OkHttpClient.download(
  dest: File, url: String,
  headers: Map<String, String> = emptyMap(),
  block: (Long, Long) -> Unit = { _, _ -> }
) {
  FileOutputStream(dest).use { out ->
    download(out, url, headers, block)
  }
}

suspend fun OkHttpClient.wss(url: String,
                             headers: Headers = emptyHeaders,
                             listener: WebSocketListener): WebSocket {
  val req = Request.Builder().url(url).headers(headers).build()
  return newWebSocket(req, listener)
}

/**
 * Suspend extension that allows suspend [Call] inside coroutine.
 *
 * [recordStack] enables track recording, so in case of exception stacktrace will contain call stacktrace, may be useful for debugging
 *      Not free! Creates exception on each request so disabled by default, but may be enabled using system properties:
 *
 *      ```
 *      System.setProperty(OKHTTP_STACK_RECORDER_PROPERTY, OKHTTP_STACK_RECORDER_ON)
 *      ```
 *      see [README.md](https://github.com/gildor/kotlin-coroutines-okhttp/blob/master/README.md#Debugging) with details about debugging using this feature
 *
 * @return Result of request or throw exception
 */
suspend fun Call.await(recordStack: Boolean = isRecordStack): Response {
  val callStack = if(recordStack) {
    IOException().apply {
      // Remove unnecessary lines from stacktrace
      // This doesn't remove await$default, but better than nothing
      stackTrace = stackTrace.copyOfRange(1, stackTrace.size)
    }
  } else {
    null
  }
  return suspendCancellableCoroutine { continuation ->
    enqueue(object: Callback {
      override fun onResponse(call: Call, response: Response) {
        continuation.resume(response)
      }
      
      override fun onFailure(call: Call, e: IOException) {
        // Don't bother with resuming the continuation if it is already cancelled.
        if(continuation.isCancelled) return
        callStack?.initCause(e)
        continuation.resumeWithException(callStack ?: e)
      }
    })
    
    continuation.invokeOnCancellation {
      try {
        cancel()
      } catch(ex: Throwable) {
        //Ignore cancel exception
      }
    }
  }
}

const val OKHTTP_STACK_RECORDER_PROPERTY = "ru.gildor.coroutines.okhttp.stackrecorder"

/**
 * Debug turned on value for [DEBUG_PROPERTY_NAME]. See [newCoroutineContext][CoroutineScope.newCoroutineContext].
 */
const val OKHTTP_STACK_RECORDER_ON = "on"

/**
 * Debug turned on value for [DEBUG_PROPERTY_NAME]. See [newCoroutineContext][CoroutineScope.newCoroutineContext].
 */
const val OKHTTP_STACK_RECORDER_OFF = "off"

@JvmField
val isRecordStack = when(System.getProperty(OKHTTP_STACK_RECORDER_PROPERTY)) {
  OKHTTP_STACK_RECORDER_ON -> true
  OKHTTP_STACK_RECORDER_OFF, null, "" -> false
  else -> error(
      "System property '$OKHTTP_STACK_RECORDER_PROPERTY' has unrecognized value '${System.getProperty(
          OKHTTP_STACK_RECORDER_PROPERTY
      )}'"
  )
}