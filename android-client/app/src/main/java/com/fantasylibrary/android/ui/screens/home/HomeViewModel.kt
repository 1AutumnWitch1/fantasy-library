package com.fantasylibrary.android.ui.screens.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.fantasylibrary.android.data.network.HealthReport
import com.fantasylibrary.android.data.network.Outcome
import com.fantasylibrary.android.data.repository.AuthRepository
import com.fantasylibrary.android.data.repository.CatalogRepository
import com.fantasylibrary.android.data.repository.DiagnosticsRepository
import com.fantasylibrary.android.data.repository.SessionInfo
import com.fantasylibrary.android.data.settings.SettingsRepository
import com.fantasylibrary.android.di.AppContainer
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

/** 首页 / 服务状态：健康检查、馆藏总量、登录状态。 */
class HomeViewModel(
    private val settingsRepository: SettingsRepository,
    private val diagnosticsRepository: DiagnosticsRepository,
    private val catalogRepository: CatalogRepository,
    private val authRepository: AuthRepository,
) : ViewModel() {

    data class UiState(
        val baseUrl: String = "",
        val settingsLoaded: Boolean = false,
        val health: HealthReport? = null,
        val checking: Boolean = false,
        val checkedAt: Long = 0L,
        val animeTotal: Long? = null,
        val gameTotal: Long? = null,
        val statsChecking: Boolean = false,
        val session: SessionInfo = SessionInfo(),
        val notice: String? = null,
    )

    private val _state = MutableStateFlow(UiState())
    val state: StateFlow<UiState> = _state.asStateFlow()

    private var autoChecked = false

    init {
        viewModelScope.launch {
            settingsRepository.settings.collect { stored ->
                val changed = _state.value.baseUrl != stored.baseUrl
                _state.update { it.copy(baseUrl = stored.baseUrl, settingsLoaded = true) }
                if (changed) {
                    _state.update { it.copy(health = null, animeTotal = null, gameTotal = null) }
                    if (stored.baseUrl.isNotBlank() && autoChecked) refresh()
                }
            }
        }
        viewModelScope.launch {
            authRepository.session.collect { info -> _state.update { it.copy(session = info) } }
        }
        viewModelScope.launch {
            // 首次进入自动检测一次（只在有地址时）
            settingsRepository.settings.collect { stored ->
                if (!autoChecked && stored.baseUrl.isNotBlank()) {
                    autoChecked = true
                    refresh()
                }
            }
        }
    }

    fun refresh() {
        val baseUrl = _state.value.baseUrl
        if (baseUrl.isBlank()) {
            _state.update { it.copy(notice = "还没有配置服务器地址，请先到「连接」页填写") }
            return
        }
        viewModelScope.launch {
            _state.update { it.copy(checking = true, notice = null) }
            val report = diagnosticsRepository.checkHealth(baseUrl)
            _state.update {
                it.copy(
                    checking = false,
                    health = report,
                    checkedAt = System.currentTimeMillis(),
                )
            }
            settingsRepository.recordProbe(
                report.ok,
                if (report.ok) {
                    "HTTP ${report.httpStatus} · ${report.elapsedMs}ms · 数据库 ${report.database ?: "?"}"
                } else {
                    report.failure?.let { f -> "${f.kind.label}：${f.message}" } ?: "检查失败"
                },
            )
            loadTotals(baseUrl)
        }
    }

    private suspend fun loadTotals(baseUrl: String) {
        _state.update { it.copy(statsChecking = true) }
        val anime = catalogRepository.animePage(baseUrl, page = 1, size = 1)
        val games = catalogRepository.gamesPage(baseUrl, page = 1, size = 1)
        _state.update {
            it.copy(
                statsChecking = false,
                animeTotal = (anime as? Outcome.Ok)?.value?.total,
                gameTotal = (games as? Outcome.Ok)?.value?.total,
            )
        }
    }

    fun logout() {
        val baseUrl = _state.value.baseUrl
        viewModelScope.launch {
            val result = authRepository.logout(baseUrl)
            val message = when (result) {
                is Outcome.Ok -> "已退出登录，本地会话已清除"
                is Outcome.Err -> "本地会话已清除；但服务器未确认退出（${result.failure.message}）"
            }
            _state.update { it.copy(notice = message) }
        }
    }

    fun clearNotice() = _state.update { it.copy(notice = null) }

    companion object {
        fun factory(container: AppContainer): ViewModelProvider.Factory = viewModelFactory {
            initializer {
                HomeViewModel(
                    settingsRepository = container.settingsRepository,
                    diagnosticsRepository = container.diagnosticsRepository,
                    catalogRepository = container.catalogRepository,
                    authRepository = container.authRepository,
                )
            }
        }
    }
}
