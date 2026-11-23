package com.fantasylibrary.android.data.network

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request

/** 健康检查结果：包含真实 HTTP 状态码、耗时与后端返回的数据库状态。 */
data class HealthReport(
    val endpoint: String,
    val httpStatus: Int? = null,
    val elapsedMs: Long = 0,
    val application: String? = null,
    val database: String? = null,
    val contentType: String? = null,
    val rawBody: String? = null,
    val failure: Failure? = null,
    val checkedAt: Long = System.currentTimeMillis(),
) {
    val ok: Boolean get() = failure == null && httpStatus in 200..299
}

/** 视频地址探测结果：验证服务器是否真的支持 HTTP Range（拖动进度条的前提）。 */
data class MediaProbeReport(
    val url: String,
    val httpStatus: Int? = null,
    val acceptRanges: String? = null,
    val contentRange: String? = null,
    val contentType: String? = null,
    val contentLength: Long? = null,
    val elapsedMs: Long = 0,
    val failure: Failure? = null,
) {
    val ok: Boolean get() = failure == null && httpStatus in 200..299

    /** 206 Partial Content 才能确认支持断点/拖动；200 说明服务端忽略了 Range。 */
    val rangeSupported: Boolean get() = httpStatus == 206 && !contentRange.isNullOrBlank()
}

/**
 * 直接基于 OkHttp 的诊断探针。
 *
 * 不使用 Retrofit，因为需求要求显示「真实 HTTP 状态码 + 响应时间」，
 * 并区分超时 / 拒绝连接 / 404 / 401 等情况，直接读 Response 最可靠。
 * 所有结果都来自真实服务器，不做任何模拟。
 */
class Diagnostics(private val client: OkHttpClient) {

    suspend fun checkHealth(baseUrl: String): HealthReport = withContext(Dispatchers.IO) {
        val endpoint = baseUrl.trim().trimEnd('/') + "/api/health"
        val request = try {
            Request.Builder()
                .url(endpoint)
                .header("Accept", "application/json")
                .header("Cache-Control", "no-cache")
                .get()
                .build()
        } catch (error: IllegalArgumentException) {
            return@withContext HealthReport(
                endpoint = endpoint,
                failure = Failure(
                    FailureKind.INVALID_ADDRESS,
                    "地址不合法：${error.message ?: endpoint}",
                    detail = error.message,
                ),
            )
        }

        val started = System.nanoTime()
        try {
            client.newCall(request).execute().use { response ->
                val elapsed = (System.nanoTime() - started) / 1_000_000
                val body = response.body?.string()
                val contentType = response.header("Content-Type")
                if (!response.isSuccessful) {
                    return@withContext HealthReport(
                        endpoint = endpoint,
                        httpStatus = response.code,
                        elapsedMs = elapsed,
                        contentType = contentType,
                        rawBody = body?.take(800),
                        failure = Failures.fromHttp(response.code, body),
                    )
                }
                val health = try {
                    kotlinx.serialization.json.Json { ignoreUnknownKeys = true }
                        .decodeFromString(HealthDto.serializer(), body.orEmpty())
                } catch (_: Exception) {
                    null
                }
                if (health == null) {
                    return@withContext HealthReport(
                        endpoint = endpoint,
                        httpStatus = response.code,
                        elapsedMs = elapsed,
                        contentType = contentType,
                        rawBody = body?.take(800),
                        failure = Failure(
                            FailureKind.BAD_RESPONSE,
                            "返回 200，但内容不是 /api/health 的 JSON",
                            httpStatus = response.code,
                            detail = body?.take(300),
                        ),
                    )
                }
                HealthReport(
                    endpoint = endpoint,
                    httpStatus = response.code,
                    elapsedMs = elapsed,
                    application = health.application,
                    database = health.database,
                    contentType = contentType,
                    rawBody = body?.take(800),
                )
            }
        } catch (error: Exception) {
            val elapsed = (System.nanoTime() - started) / 1_000_000
            HealthReport(
                endpoint = endpoint,
                elapsedMs = elapsed,
                failure = Failures.fromThrowable(error),
            )
        }
    }

    /** 用 Range: bytes=0-1 探测媒体地址；立刻关闭响应体，不会下载整个文件。 */
    suspend fun probeMedia(mediaUrl: String): MediaProbeReport = withContext(Dispatchers.IO) {
        val request = try {
            Request.Builder()
                .url(mediaUrl)
                .header("Range", "bytes=0-1")
                .header("Accept", "*/*")
                .get()
                .build()
        } catch (error: IllegalArgumentException) {
            return@withContext MediaProbeReport(
                url = mediaUrl,
                failure = Failure(FailureKind.INVALID_ADDRESS, "媒体地址不合法", detail = error.message),
            )
        }

        val started = System.nanoTime()
        try {
            client.newCall(request).execute().use { response ->
                val elapsed = (System.nanoTime() - started) / 1_000_000
                val contentType = response.header("Content-Type")
                val length = response.header("Content-Length")?.toLongOrNull()
                if (!response.isSuccessful) {
                    val body = try {
                        response.body?.string()?.take(400)
                    } catch (_: Exception) {
                        null
                    }
                    return@withContext MediaProbeReport(
                        url = mediaUrl,
                        httpStatus = response.code,
                        contentType = contentType,
                        elapsedMs = elapsed,
                        failure = Failures.fromHttp(response.code, body),
                    )
                }
                MediaProbeReport(
                    url = mediaUrl,
                    httpStatus = response.code,
                    acceptRanges = response.header("Accept-Ranges"),
                    contentRange = response.header("Content-Range"),
                    contentType = contentType,
                    contentLength = length,
                    elapsedMs = elapsed,
                )
            }
        } catch (error: Exception) {
            val elapsed = (System.nanoTime() - started) / 1_000_000
            MediaProbeReport(
                url = mediaUrl,
                elapsedMs = elapsed,
                failure = Failures.fromThrowable(error),
            )
        }
    }
}
