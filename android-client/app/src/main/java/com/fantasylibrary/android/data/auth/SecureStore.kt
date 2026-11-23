package com.fantasylibrary.android.data.auth

import android.content.Context
import android.content.SharedPreferences
import android.util.Log
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey

/**
 * 敏感信息存储。
 *
 * 优先使用 androidx.security 的 EncryptedSharedPreferences（AES256-GCM，密钥在 Android Keystore 中），
 * 若设备不支持（极少数模拟器 / Keystore 异常），自动降级为普通 SharedPreferences，
 * 并在界面上明确提示当前存储方式——绝不静默假装“已加密”。
 *
 * 这里只保存：服务端下发的会话 Cookie、CSRF Token、昵称与账号名。
 * **永远不会保存密码**，也不会保存 Oracle / MinIO 的任何凭据（客户端根本没有这些凭据）。
 */
class SecureStore private constructor(
    private val prefs: SharedPreferences,
    val encrypted: Boolean,
) {

    fun getString(key: String): String? = prefs.getString(key, null)

    fun putString(key: String, value: String?) {
        prefs.edit().apply {
            if (value == null) remove(key) else putString(key, value)
        }.apply()
    }

    fun clear() {
        prefs.edit().clear().apply()
    }

    val storageLabel: String
        get() = if (encrypted) "EncryptedSharedPreferences（Keystore 加密）" else "普通 SharedPreferences（设备不支持加密，已降级）"

    companion object {
        private const val TAG = "SecureStore"
        private const val FILE_ENCRYPTED = "fantasy_library_secure"
        private const val FILE_PLAIN = "fantasy_library_secure_plain"

        fun create(context: Context): SecureStore {
            val app = context.applicationContext
            return try {
                val masterKey = MasterKey.Builder(app)
                    .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
                    .build()
                @Suppress("DEPRECATION")
                val prefs = EncryptedSharedPreferences.create(
                    app,
                    FILE_ENCRYPTED,
                    masterKey,
                    EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
                    EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM,
                )
                SecureStore(prefs, encrypted = true)
            } catch (error: Exception) {
                Log.w(TAG, "加密存储不可用，降级为普通存储：${error.javaClass.simpleName}", error)
                val prefs = app.getSharedPreferences(FILE_PLAIN, Context.MODE_PRIVATE)
                SecureStore(prefs, encrypted = false)
            }
        }
    }
}
