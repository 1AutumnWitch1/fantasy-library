package com.fantasylibrary.android.data.repository

import com.fantasylibrary.android.data.network.ApiEnvelope
import com.fantasylibrary.android.data.network.Failure
import com.fantasylibrary.android.data.network.FailureKind
import com.fantasylibrary.android.data.network.Outcome
import kotlinx.coroutines.CancellationException

/** 统一把业务码翻译成失败分类，保证 UI 上的错误提示可读。 */
internal fun kindForBusinessCode(code: String): FailureKind = when (code) {
    "LOGIN_REQUIRED", "INVALID_CREDENTIALS" -> FailureKind.UNAUTHORIZED
    "FORBIDDEN" -> FailureKind.FORBIDDEN
    "WORK_NOT_FOUND", "PLAYBACK_NOT_FOUND", "MEDIA_NOT_FOUND", "COMMUNITY_NOT_FOUND" -> FailureKind.MEDIA_STORAGE
    "ROUTE_NOT_FOUND", "METHOD_NOT_ALLOWED" -> FailureKind.NOT_FOUND
    "INVALID_ARGUMENT" -> FailureKind.INVALID_ARGUMENT
    "DATABASE_UNAVAILABLE", "CATALOG_NOT_INITIALIZED" -> FailureKind.DATABASE
    "MEDIA_STORAGE_UNAVAILABLE", "COMMUNITY_STORAGE_UNAVAILABLE" -> FailureKind.MEDIA_STORAGE
    "INTERNAL_ERROR", "REQUEST_ERROR" -> FailureKind.SERVER_ERROR
    else -> FailureKind.SERVER_ERROR
}

/** 业务码不是 OK 时返回 Failure。 */
internal fun ApiEnvelope<*>.failureOrNull(): Failure? {
    if (isOk) return null
    return Failure(
        kind = kindForBusinessCode(code),
        message = message.ifBlank { "后端返回失败：${code.ifBlank { "UNKNOWN" }}" },
        code = code.ifBlank { null },
        retryable = false,
    )
}

/** 包住一次挂起调用，把异常转成可展示的 Failure（不吞掉协程取消）。 */
internal suspend fun <T> guarded(block: suspend () -> T): Outcome<T> = try {
    Outcome.Ok(block())
} catch (cancelled: CancellationException) {
    throw cancelled
} catch (error: Throwable) {
    Outcome.Err(com.fantasylibrary.android.data.network.Failures.fromThrowable(error))
}
