package com.fantasylibrary.android.ui.screens.community

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.fantasylibrary.android.data.network.*
import com.fantasylibrary.android.di.AppContainer
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

class CommunityViewModel(private val container: AppContainer, private val workId: Long?) : ViewModel() {
    data class State(
        val base: String = "",
        val topic: Topic? = null,
        val hot: Boolean = false,
        val items: List<PostDto> = emptyList(),
        val page: Int = 0,
        val more: Boolean = false,
        val busy: Boolean = true,
        val error: Failure? = null,
    )
    private val _state = MutableStateFlow(State())
    val state = _state.asStateFlow()
    private var request: Job? = null

    init {
        viewModelScope.launch {
            container.settingsRepository.settings.map { it.baseUrl }.distinctUntilChanged().collect { base ->
                _state.update { it.copy(base = base) }
                load(true)
            }
        }
    }

    fun select(topic: Topic?, hot: Boolean) {
        _state.update { it.copy(topic = topic, hot = hot) }
        load(true)
    }

    fun load(reset: Boolean) {
        val current = _state.value
        if (!reset && (current.busy || !current.more)) return
        request?.cancel()
        if (current.base.isBlank()) {
            _state.update { it.copy(busy = false, error = Failure(FailureKind.NO_ADDRESS, "请先填写服务器地址。")) }
            return
        }
        _state.update {
            if (reset) it.copy(items = emptyList(), page = 0, more = false, busy = true, error = null)
            else it.copy(busy = true, error = null)
        }
        request = viewModelScope.launch {
            val result = container.readerRepository.posts(current.base, if (reset) 1 else current.page + 1,
                current.topic, current.hot, workId)
            _state.update { state ->
                when (result) {
                    is Outcome.Ok -> state.copy(items = (state.items + result.value.items).distinctBy { it.id },
                        page = result.value.page, more = result.value.page < result.value.totalPages, busy = false)
                    is Outcome.Err -> state.copy(busy = false, error = result.failure)
                }
            }
        }
    }

    companion object {
        fun factory(container: AppContainer, workId: Long?) = viewModelFactory { initializer { CommunityViewModel(container, workId) } }
    }
}
