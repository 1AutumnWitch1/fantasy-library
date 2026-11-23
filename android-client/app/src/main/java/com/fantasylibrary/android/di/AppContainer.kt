package com.fantasylibrary.android.di

import android.content.Context
import coil.ImageLoader
import com.fantasylibrary.android.BuildConfig
import com.fantasylibrary.android.data.auth.PersistentCookieJar
import com.fantasylibrary.android.data.auth.SecureStore
import com.fantasylibrary.android.data.network.ApiClient
import com.fantasylibrary.android.data.network.Diagnostics
import com.fantasylibrary.android.data.repository.AuthRepository
import com.fantasylibrary.android.data.repository.CatalogRepository
import com.fantasylibrary.android.data.repository.DiagnosticsRepository
import com.fantasylibrary.android.data.settings.SettingsRepository

/**
 * 手写依赖容器（测试客户端体量下没必要引入 Hilt）。
 * 一个 App 进程内只有一个实例，由 Application 持有。
 */
class AppContainer(context: Context) {

    val settingsRepository = SettingsRepository(context.applicationContext)

    val secureStore: SecureStore = SecureStore.create(context.applicationContext)

    private val cookieJar = PersistentCookieJar(secureStore)

    val apiClient = ApiClient(
        cookieJar = cookieJar,
        csrfStore = secureStore,
        allowCleartext = BuildConfig.ALLOW_CLEARTEXT,
        debugLogging = BuildConfig.DEBUG,
    )

    private val diagnostics = Diagnostics(apiClient.http)

    val catalogRepository = CatalogRepository(apiClient)
    val diagnosticsRepository = DiagnosticsRepository(diagnostics)
    val authRepository = AuthRepository(apiClient, secureStore, cookieJar)

    /** 图片统一走同一个 OkHttpClient（共享 Cookie 与连接池）。 */
    val imageLoader: ImageLoader = ImageLoader.Builder(context.applicationContext)
        .callFactory(apiClient.http)
        .crossfade(180)
        .build()
}
