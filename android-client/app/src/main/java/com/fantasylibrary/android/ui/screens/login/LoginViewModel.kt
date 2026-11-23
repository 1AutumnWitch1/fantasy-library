package com.fantasylibrary.android.ui.screens.login

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.fantasylibrary.android.data.network.Failure
import com.fantasylibrary.android.data.network.Outcome
import com.fantasylibrary.android.data.repository.AuthRepository
import com.fantasylibrary.android.data.repository.SessionInfo
import com.fantasylibrary.android.data.settings.SettingsRepository
import com.fantasylibrary.android.di.AppContainer
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

/**
 * 登录测试。
 *
 * 现有后端已有可用的登录接口（Cookie + CSRF 会话），因此这里实现真实登录，
 * 不存在「假登录」分支：失败就如实显示后端返回的 code / HTTP 状态。
 */
class LoginViewModel(
    private val authRepository: AuthRepository,
    private val settingsRepository: SettingsRepository,
) : ViewModel() {

    data class UiState(
        val baseUrl: String = "",
        val username: String = "",
        val password: String = "",
        val busy: Boolean = false,
        val error: Failure? = null,
        val notice: String? = null,
        val session: SessionInfo = SessionInfo(),
    ) {
        val canSubmit: Boolean get() = username.isNotBlank() && password.isNotEmpty() && !busy
    }

    private val _state = MutableStateFlow(UiState())
    val state: StateFlow<UiState> = _state.asStateFlow()

    init {
        viewModelScope.launch {
            val stored = settingsRepository.settings.first()
            _state.update { it.copy(baseUrl = stored.baseUrl) }
        }
        viewModelScope.launch {
            authRepository.session.collect { info -> _state.update { it.copy(session = info) } }
        }
    }

    fun onUsernameChange(value: String) = _state.update {
        it.copy(username = value.trim(), error = null)
    }

    fun onPasswordChange(value: String) = _state.update {
        it.copy(password = value, error = null)
    }

    fun login() {
        val current = _state.value
        if (!current.canSubmit) return
        if (current.baseUrl.isBlank()) {
            _state.update {
                it.copy(
                    error = Failure(
                        com.fantasylibrary.android.data.network.FailureKind.NO_ADDRESS,
                        "尚未配置服务器地址",
                    ),
                )
            }
            return
        }
        viewModelScope.launch {
            _state.update { it.copy(busy = true, error = null, notice = null) }
            when (val result = authRepository.login(current.baseUrl, current.username, current.password)) {
                is Outcome.Ok -> _state.update {
                    it.copy(
                        busy = false,
                        password = "",
                        notice = buildString {
                            append("登录成功：")
                            append(result.value.displayName.ifBlank { result.value.username })
                            if (!result.value.profileVerified) {
                                append("（注意：/api/me/profile 校验未通过）")
                            }
                        },
                    )
                }
                is Outcome.Err -> _state.update {
                    it.copy(busy = false, error = result.failure)
                }
            }
        }
    }

    fun logout() {
        val baseUrl = _state.value.baseUrl
        viewModelScope.launch {
            _state.update { it.copy(busy = true, error = null, notice = null) }
            val result = authRepository.logout(baseUrl)
            _state.update {
                it.copy(
                    busy = false,
                    notice = when (result) {
                        is Outcome.Ok -> "已退出登录"
                        is Outcome.Err -> "本地会话已清除；服务器未确认退出（${result.failure.message}）"
                    },
                )
            }
        }
    }

    companion object {
        fun factory(container: AppContainer): ViewModelProvider.Factory = viewModelFactory {
            initializer {
                LoginViewModel(
                    authRepository = container.authRepository,
                    settingsRepository = container.settingsRepository,
                )
            }
        }
    }
}
