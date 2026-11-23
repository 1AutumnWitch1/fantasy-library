package com.fantasylibrary.android.data.network

import kotlinx.serialization.SerializationException
import kotlinx.serialization.json.contentOrNull
import retrofit2.HttpException
import java.io.EOFException
import java.io.IOException
import java.io.InterruptedIOException
import java.net.ConnectException
import java.net.SocketException
import java.net.SocketTimeoutException
import java.net.UnknownHostException
import java.net.UnknownServiceException
import javax.net.ssl.SSLException
import javax.net.ssl.SSLHandshakeException

/** 失败分类：需求要求区分「无法连接 / 超时 / 接口不存在 / 认证失败」等情况。 */
enum class FailureKind(val label: String, val hint: String) {
    NO_ADDRESS("未配置地址", "请先在「连接」页保存电脑上的服务器地址"),
    INVALID_ADDRESS("地址无效", "地址需要形如 http://192.168.1.20:8080"),
    CLEARTEXT_BLOCKED("明文 HTTP 被禁止", "Release 版本只允许 HTTPS，请改用 https:// 地址"),
    DNS("域名无法解析", "请检查 IP / 域名拼写，以及手机与电脑是否在同一局域网"),
    CONNECT("无法连接服务器", "端口没有监听、被防火墙拦截，或 Docker 只绑定了 127.0.0.1"),
    TIMEOUT("请求超时", "服务器无响应或网络很慢，可重试并检查局域网质量"),
    TLS("HTTPS 握手失败", "证书不被信任或不是 HTTPS 端口"),
    NETWORK("网络中断", "手机网络断开或与电脑不在同一网络"),
    BAD_RESPONSE("响应无法解析", "接口返回的不是预期 JSON，可能地址指向了别的服务"),
    NOT_FOUND("接口不存在", "服务器在运行，但该接口路径不存在（HTTP 404）"),
    UNAUTHORIZED("认证失败", "需要登录，或登录状态已过期"),
    FORBIDDEN("请求被拒绝", "CSRF 校验失败或权限不足（HTTP 403）"),
    INVALID_ARGUMENT("参数被拒绝", "后端校验未通过（HTTP 400）"),
    SERVER_ERROR("服务器内部错误", "后端返回 5xx，请查看 Spring Boot 日志"),
    DATABASE("数据库不可用", "Oracle 未就绪或馆藏表未初始化"),
    MEDIA_STORAGE("媒体存储不可用", "MinIO 未就绪或对象不存在"),
    UNKNOWN("未知错误", "请查看详细信息"),
}

data class Failure(
    val kind: FailureKind,
    val message: String,
    val httpStatus: Int? = null,
    val code: String? = null,
    val detail: String? = null,
    val retryable: Boolean = true,
) {
    val title: String get() = kind.label
}

/** 业务结果：不抛异常，失败携带可展示的分类信息。 */
sealed interface Outcome<out T> {
    data class Ok<T>(val value: T) : Outcome<T>
    data class Err(val failure: Failure) : Outcome<Nothing>
}

inline fun <T> Outcome<T>.onOk(block: (T) -> Unit): Outcome<T> {
    if (this is Outcome.Ok) block(value)
    return this
}

object Failures {

    fun fromThrowable(t: Throwable): Failure = when (t) {
        is HttpException -> fromHttp(t.code(), safeErrorBody(t))
        is UnknownHostException -> Failure(
            FailureKind.DNS, "无法解析主机：${t.message ?: ""}".trim(), detail = t.message,
        )
        is SocketTimeoutException, is InterruptedIOException -> Failure(
            FailureKind.TIMEOUT, "请求超时（连接或读取超时）", detail = t.message,
        )
        is SSLHandshakeException -> Failure(
            FailureKind.TLS, "TLS 握手失败，证书或协议不匹配", detail = t.message,
        )
        is SSLException -> Failure(FailureKind.TLS, "HTTPS 连接异常", detail = t.message)
        is ConnectException -> Failure(
            FailureKind.CONNECT, "连接被拒绝：目标端口没有服务在监听", detail = t.message,
        )
        is UnknownServiceException -> {
            val msg = t.message.orEmpty()
            if (msg.contains("cleartext", ignoreCase = true) || msg.contains("CLEARTEXT")) {
                Failure(FailureKind.CLEARTEXT_BLOCKED, "系统禁止明文 HTTP 请求", detail = msg)
            } else {
                Failure(FailureKind.NETWORK, "网络请求失败", detail = msg)
            }
        }
        is SocketException -> Failure(FailureKind.NETWORK, "套接字错误，可能网络已断开", detail = t.message)
        is EOFException -> Failure(FailureKind.NETWORK, "连接被对端关闭", detail = t.message)
        is SerializationException -> Failure(
            FailureKind.BAD_RESPONSE, "JSON 结构与接口不一致，无法解析", detail = t.message,
        )
        is IllegalArgumentException -> Failure(
            FailureKind.INVALID_ADDRESS, "地址不合法：${t.message ?: ""}".trim(), detail = t.message,
        )
        is IOException -> Failure(FailureKind.NETWORK, "网络异常：${t.message ?: t.javaClass.simpleName}", detail = t.message)
        else -> Failure(
            FailureKind.UNKNOWN,
            "未知错误：${t.javaClass.simpleName}",
            detail = t.message,
        )
    }

    fun fromHttp(status: Int, body: String?): Failure {
        val envelope = parseEnvelope(body)
        val code = envelope?.first
        val message = envelope?.second
        val backend = listOfNotNull(message, code).joinToString(" / ").ifBlank { null }

        val kind = when {
            status == 401 && code == "INVALID_CREDENTIALS" -> FailureKind.UNAUTHORIZED
            status == 401 -> FailureKind.UNAUTHORIZED
            status == 403 -> FailureKind.FORBIDDEN
            status == 404 && code in setOf("WORK_NOT_FOUND", "PLAYBACK_NOT_FOUND", "MEDIA_NOT_FOUND") ->
                FailureKind.MEDIA_STORAGE
            status == 404 -> FailureKind.NOT_FOUND
            status == 400 || status == 413 || status == 422 -> FailureKind.INVALID_ARGUMENT
            status == 503 && code in setOf("DATABASE_UNAVAILABLE", "CATALOG_NOT_INITIALIZED", "COMMUNITY_STORAGE_UNAVAILABLE") ->
                FailureKind.DATABASE
            status == 503 && code in setOf("MEDIA_STORAGE_UNAVAILABLE", "MEDIA_NOT_FOUND") -> FailureKind.MEDIA_STORAGE
            status == 503 -> FailureKind.SERVER_ERROR
            status >= 500 -> FailureKind.SERVER_ERROR
            else -> FailureKind.SERVER_ERROR
        }

        val readable = when (code) {
            "LOGIN_REQUIRED" -> "需要登录后才能访问该接口"
            "INVALID_CREDENTIALS" -> "用户名或密码错误"
            "FORBIDDEN" -> "请求未通过安全校验（CSRF）或权限不足"
            "ROUTE_NOT_FOUND" -> "接口路径不存在，请确认后端版本与地址前缀"
            "DATABASE_UNAVAILABLE" -> "馆藏数据库暂时不可用"
            "CATALOG_NOT_INITIALIZED" -> "馆藏表尚未初始化"
            "MEDIA_STORAGE_UNAVAILABLE" -> "媒体存储（MinIO）暂时不可用"
            "MEDIA_NOT_FOUND" -> "该媒体对象在 MinIO 中不存在"
            "PLAYBACK_NOT_FOUND" -> "该播放资源已不可用"
            "WORK_NOT_FOUND" -> "未找到该作品"
            "INTERNAL_ERROR" -> "服务暂时无法完成请求"
            else -> null
        } ?: backend ?: defaultMessage(status)

        return Failure(
            kind = kind,
            message = readable,
            httpStatus = status,
            code = code,
            detail = backend,
            retryable = status >= 500 || status == 408 || status == 429,
        )
    }

    private fun defaultMessage(status: Int): String = when (status) {
        400 -> "请求参数不正确（HTTP 400）"
        401 -> "认证失败（HTTP 401）"
        403 -> "请求被拒绝（HTTP 403）"
        404 -> "接口不存在（HTTP 404）"
        405 -> "请求方法不被支持（HTTP 405）"
        in 500..599 -> "服务器错误（HTTP $status）"
        else -> "请求失败（HTTP $status）"
    }

    /** 尽力解析后端统一错误结构 { code, message }，失败则返回 null。 */
    fun parseEnvelope(body: String?): Pair<String?, String?>? {
        if (body.isNullOrBlank()) return null
        return try {
            val obj = kotlinx.serialization.json.Json.parseToJsonElement(body)
                as? kotlinx.serialization.json.JsonObject ?: return null
            val code = (obj["code"] as? kotlinx.serialization.json.JsonPrimitive)?.contentOrNull
            val message = (obj["message"] as? kotlinx.serialization.json.JsonPrimitive)?.contentOrNull
            if (code == null && message == null) null else code to message
        } catch (_: Exception) {
            null
        }
    }

    private fun safeErrorBody(t: HttpException): String? = try {
        t.response()?.errorBody()?.string()
    } catch (_: Exception) {
        null
    }
}
