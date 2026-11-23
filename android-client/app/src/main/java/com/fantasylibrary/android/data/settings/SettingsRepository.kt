package com.fantasylibrary.android.data.settings

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.libraryDataStore: DataStore<Preferences> by preferencesDataStore(name = "library_settings")

/** 客户端设置：目前只有服务器地址与上一次连接检测的摘要（不含任何凭据）。 */
data class AppSettings(
    val baseUrl: String = "",
    val lastProbeOk: Boolean = false,
    val lastProbeSummary: String = "",
    val lastProbeAt: Long = 0L,
) {
    val configured: Boolean get() = baseUrl.isNotBlank()
}

class SettingsRepository(private val context: Context) {

    private object Keys {
        val baseUrl = stringPreferencesKey("server.base_url")
        val probeOk = booleanPreferencesKey("server.last_probe_ok")
        val probeSummary = stringPreferencesKey("server.last_probe_summary")
        val probeAt = longPreferencesKey("server.last_probe_at")
    }

    val settings: Flow<AppSettings> = context.libraryDataStore.data.map { prefs ->
        AppSettings(
            baseUrl = prefs[Keys.baseUrl].orEmpty(),
            lastProbeOk = prefs[Keys.probeOk] ?: false,
            lastProbeSummary = prefs[Keys.probeSummary].orEmpty(),
            lastProbeAt = prefs[Keys.probeAt] ?: 0L,
        )
    }

    suspend fun saveBaseUrl(baseUrl: String) {
        context.libraryDataStore.edit { prefs ->
            prefs[Keys.baseUrl] = baseUrl.trim().trimEnd('/')
        }
    }

    suspend fun recordProbe(ok: Boolean, summary: String) {
        context.libraryDataStore.edit { prefs ->
            prefs[Keys.probeOk] = ok
            prefs[Keys.probeSummary] = summary
            prefs[Keys.probeAt] = System.currentTimeMillis()
        }
    }

    suspend fun clearBaseUrl() {
        context.libraryDataStore.edit { prefs ->
            prefs.remove(Keys.baseUrl)
            prefs.remove(Keys.probeOk)
            prefs.remove(Keys.probeSummary)
            prefs.remove(Keys.probeAt)
        }
    }
}
