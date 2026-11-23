package com.fantasylibrary.android.ui.screens.catalog

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.fantasylibrary.android.data.network.Failure
import com.fantasylibrary.android.data.network.FailureKind
import com.fantasylibrary.android.data.network.Outcome
import com.fantasylibrary.android.data.repository.CatalogRepository
import com.fantasylibrary.android.data.settings.SettingsRepository
import com.fantasylibrary.android.di.AppContainer
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

/** 动漫 / 游戏列表共用的 ViewModel（分页、刷新、加载态、错误态）。 */
class CatalogViewModel(
    val kind: CatalogKind,
    private val catalogRepository: CatalogRepository,
    private val settingsRepository: SettingsRepository,
    private val allowCleartext: Boolean,
) : ViewModel() {

    data class UiState(
        val baseUrl: String = "",
        val items: List<CatalogCard> = emptyList(),
        val page: Int = 0,
        val totalPages: Int = 0,
        val total: Long = 0,
        val loading: Boolean = true,
        val loadingMore: Boolean = false,
        val error: Failure? = null,
        val loadedOnce: Boolean = false,
        val endReached: Boolean = false,
    )

    private data class PageMeta(val page: Int, val total: Long, val totalPages: Int)

    private val _state = MutableStateFlow(UiState())
    val state: StateFlow<UiState> = _state.asStateFlow()

    private var started = false

    init {
        viewModelScope.launch {
            settingsRepository.settings.collect { stored ->
                val previous = _state.value.baseUrl
                _state.update { it.copy(baseUrl = stored.baseUrl) }
                if (stored.baseUrl != previous || !started) {
                    started = true
                    reload()
                }
            }
        }
    }

    fun refresh() = reload()

    private fun reload() {
        val baseUrl = _state.value.baseUrl
        if (baseUrl.isBlank()) {
            _state.update {
                it.copy(
                    loading = false,
                    error = Failure(
                        FailureKind.NO_ADDRESS,
                        "尚未配置服务器地址，请先到「连接」页填写并测试",
                    ),
                )
            }
            return
        }
        _state.update { it.copy(loading = true, loadingMore = false, error = null, endReached = false) }
        viewModelScope.launch {
            when (val result = fetch(baseUrl, page = 1, size = PAGE_SIZE)) {
                is Outcome.Ok -> {
                    val (cards, meta) = result.value
                    _state.update {
                        it.copy(
                            items = cards,
                            page = meta.page,
                            total = meta.total,
                            totalPages = meta.totalPages,
                            loading = false,
                            loadedOnce = true,
                            error = null,
                            endReached = meta.page >= meta.totalPages,
                        )
                    }
                }
                is Outcome.Err -> _state.update {
                    it.copy(loading = false, loadedOnce = true, error = result.failure)
                }
            }
        }
    }

    fun loadMore() {
        val current = _state.value
        if (current.loadingMore || current.loading || current.endReached || current.baseUrl.isBlank()) return
        _state.update { it.copy(loadingMore = true, error = null) }
        viewModelScope.launch {
            when (val result = fetch(current.baseUrl, page = current.page + 1, size = PAGE_SIZE)) {
                is Outcome.Ok -> {
                    val (cards, meta) = result.value
                    _state.update {
                        it.copy(
                            items = it.items + cards,
                            page = meta.page,
                            total = meta.total,
                            totalPages = meta.totalPages,
                            loadingMore = false,
                            endReached = meta.page >= meta.totalPages,
                        )
                    }
                }
                is Outcome.Err -> _state.update {
                    it.copy(loadingMore = false, error = result.failure)
                }
            }
        }
    }

    private suspend fun fetch(
        baseUrl: String,
        page: Int,
        size: Int,
    ): Outcome<Pair<List<CatalogCard>, PageMeta>> = when (kind) {
        CatalogKind.ANIME -> when (val result = catalogRepository.animePage(baseUrl, page, size)) {
            is Outcome.Ok -> Outcome.Ok(
                result.value.items.map { CatalogMapper.animeCard(it, baseUrl, allowCleartext) } to
                    PageMeta(result.value.page, result.value.total, result.value.totalPages.toInt()),
            )
            is Outcome.Err -> result
        }
        CatalogKind.GAME -> when (val result = catalogRepository.gamesPage(baseUrl, page, size)) {
            is Outcome.Ok -> Outcome.Ok(
                result.value.items.map { CatalogMapper.gameCard(it, baseUrl, allowCleartext) } to
                    PageMeta(result.value.page, result.value.total, result.value.totalPages.toInt()),
            )
            is Outcome.Err -> result
        }
    }

    companion object {
        private const val PAGE_SIZE = 20

        fun factory(container: AppContainer, kind: CatalogKind): ViewModelProvider.Factory = viewModelFactory {
            initializer {
                CatalogViewModel(
                    kind = kind,
                    catalogRepository = container.catalogRepository,
                    settingsRepository = container.settingsRepository,
                    allowCleartext = container.apiClient.allowCleartext,
                )
            }
        }
    }
}
