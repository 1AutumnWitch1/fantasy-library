package com.fantasylibrary.android.ui.screens.catalog

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.fantasylibrary.android.data.network.*
import com.fantasylibrary.android.data.repository.CatalogRepository
import com.fantasylibrary.android.data.repository.ReaderRepository
import com.fantasylibrary.android.data.settings.SettingsRepository
import com.fantasylibrary.android.di.AppContainer
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class CatalogViewModel(
    val kind: CatalogKind,
    private val catalog: CatalogRepository,
    private val reader: ReaderRepository,
    settings: SettingsRepository,
    private val allowCleartext: Boolean,
) : ViewModel() {
    data class UiState(
        val baseUrl: String = "",
        val query: String = "",
        val tags: List<TagDto> = emptyList(),
        val selected: Set<String> = emptySet(),
        val items: List<CatalogCard> = emptyList(),
        val popular: List<RankedDto> = emptyList(),
        val page: Int = 0,
        val hasMore: Boolean = false,
        val loading: Boolean = true,
        val loadingMore: Boolean = false,
        val error: Failure? = null,
    )

    private val _state = MutableStateFlow(UiState())
    val state = _state.asStateFlow()
    private var request: Job? = null
    private var metadata: Job? = null
    private val type = if (kind == CatalogKind.ANIME) "ANIME" else "GAME"

    init {
        viewModelScope.launch {
            settings.settings.map { it.baseUrl }.distinctUntilChanged().collect { base ->
                metadata?.cancel()
                _state.update { UiState(baseUrl = base, query = it.query) }
                refresh()
                if (base.isNotBlank()) {
                    metadata = viewModelScope.launch {
                        launch {
                            val tags = catalog.tags(base, type)
                            if (tags is Outcome.Ok) _state.update { it.copy(tags = tags.value) }
                        }
                        if (kind == CatalogKind.GAME) {
                            val feed = reader.recommendations(base, type = type)
                            if (feed is Outcome.Ok) _state.update { it.copy(popular = feed.value.popular) }
                        }
                    }
                }
            }
        }
    }

    fun search(value: String) {
        val query = value.take(100)
        if (query == _state.value.query) return
        _state.update { it.copy(query = query) }
        load(reset = true, delayMillis = 340)
    }

    fun filter(slug: String?) {
        _state.update {
            val selected = if (slug == null) emptySet() else if (slug in it.selected) it.selected - slug else it.selected + slug
            it.copy(selected = selected)
        }
        refresh()
    }

    fun refresh() = load(reset = true)
    fun loadMore() = load(reset = false)

    private fun load(reset: Boolean, delayMillis: Long = 0) {
        val state = _state.value
        if (!reset && (!state.hasMore || state.loading || state.loadingMore)) return
        request?.cancel()
        if (state.baseUrl.isBlank()) {
            _state.update { it.copy(loading = false, error = Failure(FailureKind.NO_ADDRESS, "请先在连接设置中填写服务器地址。")) }
            return
        }
        _state.update {
            if (reset) it.copy(items = emptyList(), page = 0, hasMore = false, loading = true, loadingMore = false, error = null)
            else it.copy(loadingMore = true, error = null)
        }
        request = viewModelScope.launch {
            delay(delayMillis)
            val page = if (reset) 1 else state.page + 1
            val query = state.query.trim().takeIf { it.isNotBlank() }
            val tags = state.selected.toList()
            when (kind) {
                CatalogKind.ANIME -> consume(catalog.animePage(state.baseUrl, page, 20, query, tags = tags)) {
                    CatalogMapper.animeCard(it, state.baseUrl, allowCleartext)
                }
                CatalogKind.GAME -> consume(catalog.gamesPage(state.baseUrl, page, 20, query, tags = tags)) {
                    CatalogMapper.gameCard(it, state.baseUrl, allowCleartext)
                }
            }
        }
    }

    private fun <T> consume(result: Outcome<PageDto<T>>, map: (T) -> CatalogCard) {
        _state.update { current ->
            when (result) {
                is Outcome.Ok -> current.copy(items = (current.items + result.value.items.map(map)).distinctBy { it.id },
                    page = result.value.page, hasMore = result.value.page < result.value.totalPages,
                    loading = false, loadingMore = false)
                is Outcome.Err -> current.copy(loading = false, loadingMore = false, error = result.failure)
            }
        }
    }

    companion object {
        fun factory(container: AppContainer, kind: CatalogKind): ViewModelProvider.Factory = viewModelFactory {
            initializer { CatalogViewModel(kind, container.catalogRepository, container.readerRepository,
                container.settingsRepository, container.apiClient.allowCleartext) }
        }
    }
}
