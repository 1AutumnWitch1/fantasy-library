package com.fantasylibrary.android.data.network

import com.fantasylibrary.android.data.auth.SecureStore
import com.jakewharton.retrofit2.converter.kotlinx.serialization.asConverterFactory
import kotlinx.serialization.json.Json
import okhttp3.Interceptor
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Response
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import java.util.concurrent.TimeUnit

/**
 * 自动为写操作附加 CSRF 头。
 *
 * 现有后端使用 Spring Security 的 CookieCsrfTokenRepository：
 *   GET  /api/auth/csrf        → 下发 XSRF-TOKEN Cookie，返回 { headerName, token }
 *   POST /api/auth/login       → 必须带上 headerName: token
 * 因此登录/退出前先取一次 CSRF，并把它缓存到加密存储里。
 */
class CsrfInterceptor(private val store: SecureStore) : Interceptor {

    override fun intercept(chain: Interceptor.Chain): Response {
        val request = chain.request()
        if (request.method == "GET" || request.method == "HEAD" || request.method == "OPTIONS") {
            return chain.proceed(request)
        }
        val header = store.getString(KEY_HEADER)?.takeIf { it.isNotBlank() } ?: DEFAULT_HEADER
        val token = store.getString(KEY_TOKEN)
        if (token.isNullOrBlank()) return chain.proceed(request)
        return chain.proceed(request.newBuilder().header(header, token).build())
    }

    companion object {
        const val KEY_HEADER = "session.csrf.header"
        const val KEY_TOKEN = "session.csrf.token"
        const val DEFAULT_HEADER = "X-XSRF-TOKEN"
    }
}

/**
 * 统一的网络栈：一个 OkHttpClient（带 CookieJar + CSRF + 超时），
 * 按当前服务器地址缓存 Retrofit 实例，地址变化时自动重建。
 */
class ApiClient(
    cookieJar: okhttp3.CookieJar,
    private val csrfStore: SecureStore,
    val allowCleartext: Boolean,
    debugLogging: Boolean,
) {

    val json: Json = Json {
        ignoreUnknownKeys = true
        explicitNulls = false
        coerceInputValues = true
        isLenient = true
        encodeDefaults = true
    }

    val http: OkHttpClient = OkHttpClient.Builder()
        .cookieJar(cookieJar)
        .addInterceptor(CsrfInterceptor(csrfStore))
        .connectTimeout(12, TimeUnit.SECONDS)
        .readTimeout(30, TimeUnit.SECONDS)
        .writeTimeout(30, TimeUnit.SECONDS)
        .callTimeout(45, TimeUnit.SECONDS)
        .retryOnConnectionFailure(true)
        .apply {
            if (debugLogging) {
                addInterceptor(
                    HttpLoggingInterceptor().apply {
                        level = HttpLoggingInterceptor.Level.BASIC
                    },
                )
            }
        }
        .build()

    private val retrofitLock = Any()
    private var cachedBaseUrl: String? = null
    private var cachedApi: FantasyApi? = null

    /** baseUrl 形如 http://10.0.2.2:8080（不带末尾斜杠）。 */
    fun api(baseUrl: String): FantasyApi = synchronized(retrofitLock) {
        val normalized = baseUrl.trim().trimEnd('/') + "/"
        cachedApi?.let { if (cachedBaseUrl == normalized) return it }
        val retrofit = Retrofit.Builder()
            .baseUrl(normalized)
            .client(http)
            .addConverterFactory(json.asConverterFactory("application/json".toMediaType()))
            .build()
        val api = retrofit.create(FantasyApi::class.java)
        cachedBaseUrl = normalized
        cachedApi = api
        api
    }
}
