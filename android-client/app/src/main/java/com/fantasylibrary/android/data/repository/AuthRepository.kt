package com.fantasylibrary.android.data.repository

import com.fantasylibrary.android.data.auth.PersistentCookieJar
import com.fantasylibrary.android.data.auth.SecureStore
import com.fantasylibrary.android.data.network.ApiClient
import com.fantasylibrary.android.data.network.CredentialsDto
import com.fantasylibrary.android.data.network.CsrfInterceptor
import com.fantasylibrary.android.data.network.CsrfProofDto
import com.fantasylibrary.android.data.network.Outcome
import com.fantasylibrary.android.data.network.ProfileDto
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * 登录会话状态。
 *
 * 说明：现有后端不使用 Bearer Token，而是「数据库会话令牌 + HttpOnly Cookie + CSRF 头」：
 *   GET  /api/auth/csrf   → 拿 CSRF token（同时下发 XSRF-TOKEN cookie）
 *   POST /api/auth/login  → 校验 CSRF 后下发 fantasy_library_session cookie
 * 因此客户端只保存 Cookie 与 CSRF token（加密存储），**不保存密码**。
 */
data class SessionInfo(
    val signedIn: Boolean = false,
    val displayName: String = "",
    val username: String = "",
    val profileVerified: Boolean = false,
    val storageEncrypted: Boolean = true,
    val storageLabel: String = "",
)

class AuthRepository(
    private val client: ApiClient,
    private val secureStore: SecureStore,
    private val cookieJar: PersistentCookieJar,
) {

    private val _session = MutableStateFlow(loadStoredSession())
    val session: StateFlow<SessionInfo> = _session.asStateFlow()

    /** 先取 CSRF，再执行写操作；登录 / 退出前都必须调用一次。 */
    suspend fun refreshCsrf(baseUrl: String): Outcome<CsrfProofDto> {
        val result = callApi("获取 CSRF Token") { client.api(baseUrl).csrf() }
        if (result is Outcome.Ok) {
            secureStore.putString(CsrfInterceptor.KEY_HEADER, result.value.headerName)
            secureStore.putString(CsrfInterceptor.KEY_TOKEN, result.value.token)
        }
        return result
    }

    suspend fun login(baseUrl: String, username: String, password: String): Outcome<SessionInfo> {
        val account = username.trim()
        if (account.isEmpty() || password.isEmpty()) {
            return Outcome.Err(
                com.fantasylibrary.android.data.network.Failure(
                    com.fantasylibrary.android.data.network.FailureKind.INVALID_ARGUMENT,
                    "请填写用户名和密码",
                ),
            )
        }

        when (val csrf = refreshCsrf(baseUrl)) {
            is Outcome.Err -> return csrf
            is Outcome.Ok -> Unit
        }

        val loginResult = callApiUnit("登录") {
            client.api(baseUrl).login(CredentialsDto(username = account, password = password))
        }
        if (loginResult is Outcome.Err) return loginResult

        // 用受保护的 /api/me/profile 验证会话确实生效（而不是只看登录接口 200）
        val profileResult = callApi("读取个人资料") { client.api(baseUrl).profile() }
        val profile = (profileResult as? Outcome.Ok)?.value

        val displayName = profile?.displayName?.takeIf { it.isNotBlank() } ?: account
        secureStore.putString(KEY_DISPLAY_NAME, displayName)
        secureStore.putString(KEY_USERNAME, profile?.username?.takeIf { it.isNotBlank() } ?: account)
        secureStore.putString(KEY_SIGNED_IN, "1")
        // 登录成功后服务端旋转 CSRF token，本地旧值必须作废
        secureStore.putString(CsrfInterceptor.KEY_TOKEN, null)

        val info = SessionInfo(
            signedIn = true,
            displayName = displayName,
            username = profile?.username ?: account,
            profileVerified = profile != null,
            storageEncrypted = secureStore.encrypted,
            storageLabel = secureStore.storageLabel,
        )
        _session.value = info
        return Outcome.Ok(info)
    }

    suspend fun logout(baseUrl: String): Outcome<Unit> {
        refreshCsrf(baseUrl)
        val result = callApiUnit("退出登录") { client.api(baseUrl).logout() }
        clearLocal()
        return when (result) {
            is Outcome.Ok -> Outcome.Ok(Unit)
            // 网络失败也已在本地清理，但仍如实回报服务端没有确认退出
            is Outcome.Err -> result
        }
    }

    suspend fun loadProfile(baseUrl: String): Outcome<ProfileDto> {
        val result = callApi("读取个人资料") { client.api(baseUrl).profile() }
        if (result is Outcome.Ok) {
            val profile = result.value
            secureStore.putString(KEY_DISPLAY_NAME, profile.displayName)
            secureStore.putString(KEY_USERNAME, profile.username)
            secureStore.putString(KEY_SIGNED_IN, "1")
            _session.value = _session.value.copy(
                signedIn = true,
                displayName = profile.displayName,
                username = profile.username,
                profileVerified = true,
            )
        }
        return result
    }

    fun clearLocal() {
        cookieJar.clear()
        secureStore.putString(KEY_DISPLAY_NAME, null)
        secureStore.putString(KEY_USERNAME, null)
        secureStore.putString(KEY_SIGNED_IN, null)
        secureStore.putString(CsrfInterceptor.KEY_TOKEN, null)
        _session.value = SessionInfo(
            signedIn = false,
            storageEncrypted = secureStore.encrypted,
            storageLabel = secureStore.storageLabel,
        )
    }

    private fun loadStoredSession(): SessionInfo {
        val signedIn = secureStore.getString(KEY_SIGNED_IN) == "1"
        return SessionInfo(
            signedIn = signedIn,
            displayName = secureStore.getString(KEY_DISPLAY_NAME).orEmpty(),
            username = secureStore.getString(KEY_USERNAME).orEmpty(),
            profileVerified = false,
            storageEncrypted = secureStore.encrypted,
            storageLabel = secureStore.storageLabel,
        )
    }

    private companion object {
        const val KEY_DISPLAY_NAME = "session.displayName"
        const val KEY_USERNAME = "session.username"
        const val KEY_SIGNED_IN = "session.signedIn"
    }
}
