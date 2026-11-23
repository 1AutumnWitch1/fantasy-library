package com.fantasylibrary.android.ui.screens.config

import android.os.SystemClock
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.fantasylibrary.android.data.network.FailureKind
import com.fantasylibrary.android.data.network.HealthReport
import com.fantasylibrary.android.data.network.MediaUrls
import com.fantasylibrary.android.data.network.ServerAddress
import com.fantasylibrary.android.data.repository.DiagnosticsRepository
import com.fantasylibrary.android.data.settings.SettingsRepository
import com.fantasylibrary.android.di.AppContainer
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

/**
 * 服务器连接配置页的状态与动作。
 *
 * 设计要点：
 *  - 输入即时组合成完整地址并显示，避免“保存的地址和我以为的不一样”；
 *  - 「测试连接」会把地址一并保存，保证后续页面用的就是刚测通的地址；
 *  - 结果里保留真实 HTTP 状态码、耗时、后端 application/database 字段。
 */
class ServerConfigViewModel(
    private val settingsRepository: SettingsRepository,
    private val diagnosticsRepository: DiagnosticsRepository,
    private val allowCleartext: Boolean,
) : ViewModel() {

    data class UiState(
        val scheme: String = "http",
        val host: String = "",
        val port: String = "8080",
        val loaded: Boolean = false,
        val savedBaseUrl: String = "",
        val normalizedBaseUrl: String = "",
        val inputError: String? = null,
        val warnings: List<String> = emptyList(),
        val saving: Boolean = false,
        val probing: Boolean = false,
        val report: HealthReport? = null,
        val statusMessage: String? = null,
        val lastSavedAt: Long = 0L,
    ) {
        val canSubmit: Boolean get() = host.isNotBlank() && inputError == null && !saving && !probing
    }

    private val _state = MutableStateFlow(
        UiState(scheme = if (allowCleartext) "http" else "https", port = "8080"),
    )
    val state: StateFlow<UiState> = _state.asStateFlow()

    init {
        viewModelScope.launch {
            val stored = settingsRepository.settings.first()
            val saved = stored.baseUrl.trim()
            if (saved.isEmpty()) {
                _state.update { it.copy(loaded = true) }
            } else {
                _state.update {
                    it.copy(
                        scheme = MediaUrls.schemeOf(saved) ?: "http",
                        host = MediaUrls.hostOf(saved).orEmpty(),
                        port = MediaUrls.portOf(saved).toString(),
                        savedBaseUrl = saved,
                        normalizedBaseUrl = saved,
                        loaded = true,
                        inputError = null,
                        warnings = emptyList(),
                    )
                }
            }
        }
    }

    fun onSchemeChange(scheme: String) = edit { it.copy(scheme = scheme) }

    fun onHostChange(host: String) = edit { it.copy(host = host.trim()) }

    fun onPortChange(port: String) = edit { it.copy(port = port.filter { ch -> ch.isDigit() }.take(5)) }

    fun applyPreset(host: String, port: String, scheme: String = "http") = edit {
        it.copy(host = host, port = port, scheme = scheme)
    }

    private fun edit(transform: (UiState) -> UiState) {
        _state.update { current -> recompute(transform(current).copy(statusMessage = null)) }
    }

    private fun recompute(state: UiState): UiState {
        if (state.host.isBlank()) {
            return state.copy(normalizedBaseUrl = "", warnings = emptyList(), inputError = null)
        }
        return when (val parsed = ServerAddress.parse(state.scheme, state.host, state.port, allowCleartext)) {
            is ServerAddress.ParseResult.Ok -> state.copy(
                normalizedBaseUrl = parsed.address.baseUrl,
                warnings = parsed.warnings,
                inputError = null,
            )
            is ServerAddress.ParseResult.Invalid -> state.copy(
                normalizedBaseUrl = "",
                warnings = emptyList(),
                inputError = parsed.message,
            )
        }
    }

    fun save() {
        val parsed = parseOrReport() ?: return
        viewModelScope.launch {
            _state.update { it.copy(saving = true, statusMessage = null) }
            settingsRepository.saveBaseUrl(parsed.baseUrl)
            _state.update {
                it.copy(
                    saving = false,
                    savedBaseUrl = parsed.baseUrl,
                    normalizedBaseUrl = parsed.baseUrl,
                    statusMessage = "地址已保存，下次启动会自动读取",
                    lastSavedAt = System.currentTimeMillis(),
                )
            }
        }
    }

    fun testConnection() {
        val parsed = parseOrReport() ?: return
        viewModelScope.launch {
            _state.update { it.copy(probing = true, report = null, statusMessage = null) }
            // 测试即保存：避免测的是 A 地址、后续页面用的是 B 地址
            settingsRepository.saveBaseUrl(parsed.baseUrl)
            val started = SystemClock.elapsedRealtime()
            val report = diagnosticsRepository.checkHealth(parsed.baseUrl)
            val elapsedTotal = SystemClock.elapsedRealtime() - started
            _state.update {
                it.copy(
                    probing = false,
                    report = report,
                    savedBaseUrl = parsed.baseUrl,
                    normalizedBaseUrl = parsed.baseUrl,
                    lastSavedAt = System.currentTimeMillis(),
                    statusMessage = if (report.ok) "连接成功（总耗时 ${elapsedTotal}ms）" else null,
                )
            }
            settingsRepository.recordProbe(report.ok, summarize(report))
        }
    }

    fun clearAddress() {
        viewModelScope.launch {
            settingsRepository.clearBaseUrl()
            _state.update {
                it.copy(
                    host = "",
                    port = "8080",
                    savedBaseUrl = "",
                    normalizedBaseUrl = "",
                    report = null,
                    warnings = emptyList(),
                    inputError = null,
                    statusMessage = "已清除保存的服务器地址",
                )
            }
        }
    }

    private fun parseOrReport(): ServerAddress? {
        val current = _state.value
        return when (val parsed = ServerAddress.parse(current.scheme, current.host, current.port, allowCleartext)) {
            is ServerAddress.ParseResult.Ok -> parsed.address
            is ServerAddress.ParseResult.Invalid -> {
                _state.update { it.copy(inputError = parsed.message, normalizedBaseUrl = "") }
                null
            }
        }
    }

    private fun summarize(report: HealthReport): String = if (report.ok) {
        "HTTP ${report.httpStatus} · ${report.elapsedMs}ms · 数据库 ${report.database ?: "?"}"
    } else {
        report.failure?.let { "${it.kind.label}：${it.message}" }
            ?: "HTTP ${report.httpStatus ?: FailureKind.NETWORK.label}"
    }

    companion object {
        fun factory(container: AppContainer): ViewModelProvider.Factory = viewModelFactory {
            initializer {
                ServerConfigViewModel(
                    settingsRepository = container.settingsRepository,
                    diagnosticsRepository = container.diagnosticsRepository,
                    allowCleartext = container.apiClient.allowCleartext,
                )
            }
        }
    }
}
