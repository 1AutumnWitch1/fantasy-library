package com.fantasylibrary.android.ui.screens.detail

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.fantasylibrary.android.data.network.EpisodeDto
import com.fantasylibrary.android.data.network.Failure
import com.fantasylibrary.android.data.network.MediaUrls
import com.fantasylibrary.android.data.network.Outcome
import com.fantasylibrary.android.data.network.WorkDetailDto
import com.fantasylibrary.android.data.repository.CatalogRepository
import com.fantasylibrary.android.data.settings.SettingsRepository
import com.fantasylibrary.android.di.AppContainer
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

/** 作品详情：封面、简介、标签、图片、动漫分集（真实数据）。 */
class WorkDetailViewModel(
    private val workId: Long,
    private val catalogRepository: CatalogRepository,
    private val settingsRepository: SettingsRepository,
    private val allowCleartext: Boolean,
) : ViewModel() {

    data class UiState(
        val baseUrl: String = "",
        val loading: Boolean = true,
        val error: Failure? = null,
        val detail: WorkDetailDto? = null,
        val cover: MediaUrls.Check? = null,
        val gallery: List<MediaUrls.Check> = emptyList(),
        val episodes: List<EpisodeDto> = emptyList(),
        val episodesLoading: Boolean = false,
        val episodesError: Failure? = null,
    ) {
        val isAnime: Boolean get() = detail?.work?.type.equals("ANIME", ignoreCase = true)
    }

    private val _state = MutableStateFlow(UiState())
    val state: StateFlow<UiState> = _state.asStateFlow()

    init {
        viewModelScope.launch {
            val stored = settingsRepository.settings.first()
            _state.update { it.copy(baseUrl = stored.baseUrl) }
            load()
        }
    }

    fun retry() {
        _state.update { it.copy(loading = true, error = null) }
        viewModelScope.launch { load() }
    }

    private suspend fun load() {
        val baseUrl = _state.value.baseUrl
        if (baseUrl.isBlank()) {
            _state.update {
                it.copy(
                    loading = false,
                    error = Failure(
                        com.fantasylibrary.android.data.network.FailureKind.NO_ADDRESS,
                        "尚未配置服务器地址",
                    ),
                )
            }
            return
        }
        when (val result = catalogRepository.workDetail(baseUrl, workId)) {
            is Outcome.Ok -> {
                val detail = result.value
                _state.update {
                    it.copy(
                        loading = false,
                        error = null,
                        detail = detail,
                        cover = MediaUrls.resolve(detail.work.cover?.url, baseUrl, allowCleartext),
                        gallery = detail.images.map { image ->
                            MediaUrls.resolve(image.url, baseUrl, allowCleartext)
                        },
                    )
                }
                if (detail.work.type.equals("ANIME", ignoreCase = true)) {
                    loadEpisodes(baseUrl)
                }
            }
            is Outcome.Err -> _state.update {
                it.copy(loading = false, error = result.failure)
            }
        }
    }

    private suspend fun loadEpisodes(baseUrl: String) {
        _state.update { it.copy(episodesLoading = true, episodesError = null) }
        when (val result = catalogRepository.episodes(baseUrl, workId)) {
            is Outcome.Ok -> _state.update {
                it.copy(episodes = result.value, episodesLoading = false)
            }
            is Outcome.Err -> _state.update {
                it.copy(episodesLoading = false, episodesError = result.failure)
            }
        }
    }

    companion object {
        fun factory(container: AppContainer, workId: Long): ViewModelProvider.Factory = viewModelFactory {
            initializer {
                WorkDetailViewModel(
                    workId = workId,
                    catalogRepository = container.catalogRepository,
                    settingsRepository = container.settingsRepository,
                    allowCleartext = container.apiClient.allowCleartext,
                )
            }
        }
    }
}
