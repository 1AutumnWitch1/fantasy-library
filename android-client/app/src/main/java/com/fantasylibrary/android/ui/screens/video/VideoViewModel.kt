package com.fantasylibrary.android.ui.screens.video

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.fantasylibrary.android.data.network.EpisodeDto
import com.fantasylibrary.android.data.network.Failure
import com.fantasylibrary.android.data.network.MediaProbeReport
import com.fantasylibrary.android.data.network.MediaUrls
import com.fantasylibrary.android.data.network.Outcome
import com.fantasylibrary.android.data.network.SourceDto
import com.fantasylibrary.android.data.repository.CatalogRepository
import com.fantasylibrary.android.data.repository.DiagnosticsRepository
import com.fantasylibrary.android.data.settings.SettingsRepository
import com.fantasylibrary.android.di.AppContainer
import com.fantasylibrary.android.ui.screens.catalog.CatalogCard
import com.fantasylibrary.android.ui.screens.catalog.CatalogMapper
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

/**
 * 视频播放测试：选动漫 → 选分集 → 选播放源，播放地址全部来自后端 /api/works/{id}/episodes。
 * 这里不硬编码任何视频地址。
 */
class VideoViewModel(
    private val catalogRepository: CatalogRepository,
    private val diagnosticsRepository: DiagnosticsRepository,
    private val settingsRepository: SettingsRepository,
    private val allowCleartext: Boolean,
    private val initialWorkId: Long? = null,
) : ViewModel() {

    data class UiState(
        val baseUrl: String = "",
        val allowCleartext: Boolean = true,
        val anime: List<CatalogCard> = emptyList(),
        val animeLoading: Boolean = true,
        val animeError: Failure? = null,
        val selectedWorkId: Long? = null,
        val selectedTitle: String = "",
        val episodes: List<EpisodeDto> = emptyList(),
        val episodesLoading: Boolean = false,
        val episodesError: Failure? = null,
        val episodeIndex: Int = 0,
        val sourceIndex: Int = 0,
        val probing: Boolean = false,
        val probe: MediaProbeReport? = null,
        val notice: String? = null,
    ) {
        val episode: EpisodeDto? get() = episodes.getOrNull(episodeIndex)
        val source: SourceDto? get() = episode?.sources?.getOrNull(sourceIndex)
        val mediaCheck: MediaUrls.Check
            get() = MediaUrls.resolve(source?.url, baseUrl, allowCleartext)
    }

    private val _state = MutableStateFlow(UiState(allowCleartext = allowCleartext))
    val state: StateFlow<UiState> = _state.asStateFlow()

    init {
        viewModelScope.launch {
            val stored = settingsRepository.settings.first()
            _state.update { it.copy(baseUrl = stored.baseUrl) }
            loadAnime()
        }
    }

    fun loadAnime() {
        val baseUrl = _state.value.baseUrl
        if (baseUrl.isBlank()) {
            _state.update {
                it.copy(
                    animeLoading = false,
                    animeError = Failure(
                        com.fantasylibrary.android.data.network.FailureKind.NO_ADDRESS,
                        "尚未配置服务器地址，请先到「连接」页填写",
                    ),
                )
            }
            return
        }
        _state.update { it.copy(animeLoading = true, animeError = null) }
        viewModelScope.launch {
            when (val result = catalogRepository.animePage(baseUrl, page = 1, size = 50)) {
                is Outcome.Ok -> {
                    val cards = result.value.items.map {
                        CatalogMapper.animeCard(it, baseUrl, allowCleartext)
                    }
                    _state.update { it.copy(animeLoading = false, anime = cards) }
                    if (cards.isNotEmpty() && _state.value.selectedWorkId == null) {
                        val preferred = initialWorkId?.let { id -> cards.firstOrNull { it.id == id } }
                        selectWork(preferred ?: cards.first())
                    }
                }
                is Outcome.Err -> _state.update {
                    it.copy(animeLoading = false, animeError = result.failure)
                }
            }
        }
    }

    fun selectWork(card: CatalogCard) {
        if (_state.value.selectedWorkId == card.id && _state.value.episodes.isNotEmpty()) return
        _state.update {
            it.copy(
                selectedWorkId = card.id,
                selectedTitle = card.title,
                episodes = emptyList(),
                episodeIndex = 0,
                sourceIndex = 0,
                probe = null,
                notice = null,
                episodesError = null,
            )
        }
        loadEpisodes(card.id)
    }

    private fun loadEpisodes(workId: Long) {
        val baseUrl = _state.value.baseUrl
        _state.update { it.copy(episodesLoading = true, episodesError = null) }
        viewModelScope.launch {
            when (val result = catalogRepository.episodes(baseUrl, workId)) {
                is Outcome.Ok -> {
                    val withSources = result.value
                    _state.update {
                        it.copy(
                            episodes = withSources,
                            episodesLoading = false,
                            episodeIndex = withSources.indexOfFirst { ep -> ep.sources.isNotEmpty() }
                                .takeIf { index -> index >= 0 } ?: 0,
                            sourceIndex = 0,
                            probe = null,
                        )
                    }
                }
                is Outcome.Err -> _state.update {
                    it.copy(episodesLoading = false, episodesError = result.failure)
                }
            }
        }
    }

    fun selectEpisode(index: Int) {
        _state.update {
            if (index !in it.episodes.indices) it
            else it.copy(episodeIndex = index, sourceIndex = 0, probe = null, notice = null)
        }
    }

    fun selectSource(index: Int) {
        _state.update {
            val episodes = it.episode?.sources.orEmpty()
            if (index !in episodes.indices) it
            else it.copy(sourceIndex = index, probe = null, notice = null)
        }
    }

    fun probeRange() {
        val check = _state.value.mediaCheck
        if (!check.ok) {
            _state.update {
                it.copy(
                    notice = check.reason ?: "当前没有可用的播放地址",
                    probe = null,
                )
            }
            return
        }
        val url = check.resolved ?: return
        _state.update { it.copy(probing = true, probe = null, notice = null) }
        viewModelScope.launch {
            val report = diagnosticsRepository.probeMedia(url)
            _state.update { it.copy(probing = false, probe = report) }
        }
    }

    fun clearNotice() = _state.update { it.copy(notice = null) }

    companion object {
        fun factory(
            container: AppContainer,
            initialWorkId: Long? = null,
        ): ViewModelProvider.Factory = viewModelFactory {
            initializer {
                VideoViewModel(
                    catalogRepository = container.catalogRepository,
                    diagnosticsRepository = container.diagnosticsRepository,
                    settingsRepository = container.settingsRepository,
                    allowCleartext = container.apiClient.allowCleartext,
                    initialWorkId = initialWorkId,
                )
            }
        }
    }
}
