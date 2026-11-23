package com.fantasylibrary.android.data.auth

import kotlinx.serialization.Serializable
import kotlinx.serialization.builtins.ListSerializer
import kotlinx.serialization.json.Json
import okhttp3.Cookie
import okhttp3.CookieJar
import okhttp3.HttpUrl

/** 序列化后的 Cookie 记录（不含密码，只含服务端下发的会话/CSRF Cookie）。 */
@Serializable
data class CookieRecord(
    val name: String,
    val value: String,
    val domain: String,
    val path: String,
    val secure: Boolean,
    val hostOnly: Boolean,
    val expiresAt: Long,
)

/**
 * 把 OkHttp 收到的 Cookie 持久化下来，使 App 重启后仍保持登录状态。
 * 存储介质由 [SecureStore] 决定（优先加密）。
 */
class PersistentCookieJar(private val store: SecureStore) : CookieJar {

    private val json = Json { ignoreUnknownKeys = true }
    private val lock = Any()
    private var records: MutableList<CookieRecord>? = null

    private fun loaded(): MutableList<CookieRecord> = synchronized(lock) {
        records ?: run {
            val raw = store.getString(KEY)
            val parsed = if (raw.isNullOrBlank()) {
                mutableListOf()
            } else {
                try {
                    json.decodeFromString(ListSerializer(CookieRecord.serializer()), raw).toMutableList()
                } catch (_: Exception) {
                    mutableListOf()
                }
            }
            records = parsed
            parsed
        }
    }

    private fun persist() = synchronized(lock) {
        val list = records.orEmpty()
        store.putString(KEY, json.encodeToString(ListSerializer(CookieRecord.serializer()), list))
    }

    override fun saveFromResponse(url: HttpUrl, cookies: List<Cookie>) {
        if (cookies.isEmpty()) return
        synchronized(lock) {
            val list = loaded()
            val now = System.currentTimeMillis()
            list.removeAll { it.expiresAt in 1 until now }
            for (cookie in cookies) {
                // 服务端清空 Cookie 时（logout）Max-Age=0，此时应删除本地记录
                if (cookie.expiresAt in 1 until now) {
                    list.removeAll { it.name == cookie.name && it.domain == cookie.domain }
                    continue
                }
                list.removeAll { it.name == cookie.name && it.domain == cookie.domain && it.path == cookie.path }
                list.add(
                    CookieRecord(
                        name = cookie.name,
                        value = cookie.value,
                        domain = cookie.domain,
                        path = cookie.path,
                        secure = cookie.secure,
                        hostOnly = cookie.hostOnly,
                        expiresAt = cookie.expiresAt,
                    ),
                )
            }
            records = list
            persist()
        }
    }

    override fun loadForRequest(url: HttpUrl): List<Cookie> {
        val now = System.currentTimeMillis()
        return synchronized(lock) {
            val list = loaded()
            val alive = list.filter { it.expiresAt == Long.MIN_VALUE || it.expiresAt > now }
            if (alive.size != list.size) {
                records = alive.toMutableList()
                persist()
            }
            alive.filter { it.matches(url) }.mapNotNull { it.toCookie() }
        }
    }

    fun clear() {
        synchronized(lock) {
            records = mutableListOf()
            store.putString(KEY, null)
        }
    }

    fun snapshot(): List<CookieRecord> = synchronized(lock) { loaded().toList() }

    private fun CookieRecord.matches(url: HttpUrl): Boolean {
        if (secure && !url.isHttps) return false
        val host = url.host.lowercase()
        val domainMatches = if (hostOnly) {
            host == domain.lowercase()
        } else {
            host == domain.lowercase() || host.endsWith("." + domain.lowercase())
        }
        if (!domainMatches) return false
        val requestPath = url.encodedPath.ifEmpty { "/" }
        return requestPath == path || requestPath.startsWith(if (path.endsWith("/")) path else "$path/")
    }

    private fun CookieRecord.toCookie(): Cookie? = try {
        Cookie.Builder()
            .name(name)
            .value(value)
            .apply {
                if (hostOnly) hostOnlyDomain(domain) else domain(domain)
                path(path)
                if (secure) secure()
                if (expiresAt > 0) expiresAt(expiresAt)
            }
            .build()
    } catch (_: IllegalArgumentException) {
        null
    }

    private companion object {
        const val KEY = "cookies.v1"
    }
}
