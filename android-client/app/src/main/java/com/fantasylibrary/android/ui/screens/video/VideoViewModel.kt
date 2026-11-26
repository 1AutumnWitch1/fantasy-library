package com.fantasylibrary.android.ui.screens.video

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.fantasylibrary.android.data.network.*
import com.fantasylibrary.android.di.AppContainer
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

data class PlaybackNode(val label: String, val episodes: List<EpisodeDto>)
data class VideoRecommendation(val entry: WorkEntryDto, val episode: EpisodeDto, val heat: HeatDto?, val studio: String?, val reason: String?)

class VideoViewModel(private val container: AppContainer, initialWorkId: Long) : ViewModel() {
    data class State(
        val workId: Long = 0,
        val baseUrl: String = "",
        val detail: WorkDetailDto? = null,
        val nodes: List<PlaybackNode> = emptyList(),
        val node: Int = 0,
        val episode: Int = 0,
        val source: Int = 0,
        val loading: Boolean = true,
        val error: Failure? = null,
        val recommendations: List<VideoRecommendation> = emptyList(),
        val recommendationsLoading: Boolean = true,
        val recommendationsError: Failure? = null,
        val stats: PlaybackStatsDto? = null,
        val resumePosition: Long = 0,
    ) {
        val selectedEpisode get() = nodes.getOrNull(node)?.episodes?.getOrNull(episode)
        val selectedSource get() = selectedEpisode?.sources?.getOrNull(source)
    }

    private data class Target(val workId: Long, val episodeId: Long? = null)
    private val target = MutableStateFlow(Target(initialWorkId))
    private val _state = MutableStateFlow(State(workId = initialWorkId))
    val state = _state.asStateFlow()
    private val retry = MutableStateFlow(0)
    private val retryRecommendations = MutableStateFlow(0)
    private val statsRevision = MutableStateFlow(0)
    private val baseUrl = container.settingsRepository.settings.map { it.baseUrl }.distinctUntilChanged()
    private var feedCache: Pair<String, FeedDto>? = null
    private var studios: Pair<String, Map<Long, String>>? = null
    private val available = mutableMapOf<Pair<String, Long>, EpisodeDto?>()

    init {
        viewModelScope.launch {
            combine(baseUrl, target, retry) { base, current, _ -> base to current }.collectLatest { (base, current) ->
                _state.update { before ->
                    val next = if (before.workId == current.workId && before.baseUrl == base) before
                    else State(workId = current.workId, baseUrl = base)
                    next.copy(loading = base.isNotBlank(), error = null)
                }
                if (base.isBlank()) return@collectLatest
                launch {
                    val detail = container.catalogRepository.workDetail(base, current.workId)
                    if (detail is Outcome.Ok) _state.update { it.copy(detail = detail.value) }
                }
                val saved = async {
                    if (!container.authRepository.session.value.signedIn) container.viewingSync.localProgress(base, current.workId)
                    else (container.viewingSync.progress(base, current.workId) as? Outcome.Ok)?.value?.latest
                }
                when (val result = container.catalogRepository.episodes(base, current.workId)) {
                    is Outcome.Ok -> {
                        val nodes = result.value.groupBy { it.nodeId to it.nodeLabel }.map { (key, items) ->
                            PlaybackNode(key.second?.takeIf { it.isNotBlank() } ?: "馆内线路", items.sortedBy { it.number })
                        }
                        val previous = saved.await()
                        val availableEpisodes = nodes.flatMap { it.episodes }.filter { playable(it, base) }
                        val resume = previous?.takeIf { !it.completed }
                        val targetEpisode = availableEpisodes.firstOrNull { it.id == current.episodeId }
                            ?: resume?.let { progress -> availableEpisodes.firstOrNull { it.edition == "EPISODE" && it.number == progress.episodeNo } }
                            ?: previous?.takeIf { it.completed }?.let { progress ->
                                availableEpisodes.filter { it.edition == "EPISODE" && it.number > progress.episodeNo }.minByOrNull { it.number }
                            }
                            ?: availableEpisodes.firstOrNull { it.edition == "EPISODE" } ?: availableEpisodes.firstOrNull()
                        val targetId = targetEpisode?.id
                        val offset = resume?.takeIf { targetEpisode?.edition == "EPISODE" && targetEpisode.number == it.episodeNo }
                            ?.positionMs ?: 0
                        val node = nodes.indexOfFirst { group -> group.episodes.any { it.id == targetId } }
                            .takeIf { it >= 0 } ?: nodes.indexOfFirst { group -> group.episodes.any { playable(it, base) } }.coerceAtLeast(0)
                        val episodes = nodes.getOrNull(node)?.episodes.orEmpty()
                        val episode = episodes.indexOfFirst { it.id == targetId && playable(it, base) }
                            .takeIf { it >= 0 } ?: episodes.indexOfFirst { playable(it, base) }.coerceAtLeast(0)
                        val source = episodes.getOrNull(episode)?.sources?.indexOfFirst { usable(it, base) }?.coerceAtLeast(0) ?: 0
                        _state.update { it.copy(nodes = nodes, node = node, episode = episode, source = source, resumePosition = offset, loading = false) }
                    }
                    is Outcome.Err -> _state.update { it.copy(error = result.failure, loading = false) }
                }
            }
        }
        viewModelScope.launch {
            combine(state.map { it.baseUrl to it.workId }.distinctUntilChanged(), statsRevision) { context, _ -> context }
                .collectLatest { (base, work) ->
                    if (base.isBlank() || work <= 0) return@collectLatest
                    val result = container.readerRepository.playbackStats(base, work)
                    if (result is Outcome.Ok) _state.update { if (it.workId == work) it.copy(stats = result.value) else it }
                }
        }
        viewModelScope.launch {
            combine(state.map { it.baseUrl to it.workId }.distinctUntilChanged(),
                retryRecommendations) { key, attempts -> key to attempts }.collectLatest { (key, attempt) ->
                val (base, currentId) = key
                if (base.isBlank() || currentId <= 0) return@collectLatest
                loadRecommendations(base, currentId, attempt)
            }
        }
    }

    private var lastRetry = 0
    private suspend fun loadRecommendations(base: String, currentId: Long, attempt: Int) {
        _state.update { it.copy(recommendationsLoading = true, recommendationsError = null) }
        if (attempt != lastRetry) {
            feedCache = null
            studios = null
            available.clear()
            lastRetry = attempt
        }
        val cached = feedCache?.takeIf { it.first == base }?.second
        val feed = cached ?: when (val response = container.readerRepository.recommendations(base, type = "ANIME")) {
            is Outcome.Ok -> response.value.also { feedCache = base to it }
            is Outcome.Err -> {
                _state.update { it.copy(recommendationsLoading = false, recommendationsError = response.failure) }
                return
            }
        }
        val production = studios?.takeIf { it.first == base }?.second ?: when (val page = container.catalogRepository.animePage(base, 1, 50)) {
            is Outcome.Ok -> page.value.items.mapNotNull { record ->
                record.anime?.studio?.takeIf { it.isNotBlank() && it != "未知" }?.let { record.work.id to it }
            }.toMap().also { studios = base to it }
            is Outcome.Err -> emptyMap()
        }
        val heat = feed.popular.associate { it.entry.work.id to it.heat }
        val reasons = feed.forYou.associate { it.entry.work.id to it.reason }
        val candidates = (feed.forYou.map { it.entry } + feed.popular.map { it.entry } + feed.newest)
            .distinctBy { it.work.id }.filter { it.work.id != currentId && it.work.type.equals("ANIME", true) }.take(12)
        var lookupFailure: Failure? = null
        val entries = mutableListOf<VideoRecommendation>()
        for (batch in candidates.chunked(4)) {
            val resolved = kotlinx.coroutines.coroutineScope {
                batch.map { entry ->
                    async {
                        val id = entry.work.id
                        val cacheKey = base to id
                        val known = available.containsKey(cacheKey)
                        var episode = available[cacheKey]
                        var failure: Failure? = null
                        if (!known) when (val response = container.catalogRepository.episodes(base, id)) {
                            is Outcome.Ok -> {
                                val playableEpisodes = response.value.filter { playable(it, base) }
                                episode = playableEpisodes.firstOrNull { it.edition == "EPISODE" } ?: playableEpisodes.firstOrNull()
                                available[cacheKey] = episode
                            }
                            is Outcome.Err -> failure = response.failure
                        }
                        episode?.let { VideoRecommendation(entry, it, heat[id], production[id], reasons[id]) } to failure
                    }
                }.awaitAll()
            }
            resolved.forEach { (entry, failure) ->
                if (entry != null) entries.add(entry)
                if (failure != null) lookupFailure = failure
            }
            if (entries.size >= 6) break
        }
        _state.update {
            it.copy(recommendations = entries.take(6), recommendationsLoading = false,
                recommendationsError = if (entries.isEmpty()) lookupFailure else null)
        }
    }

    fun openRecommendation(item: VideoRecommendation) {
        target.value = Target(item.entry.work.id, item.episode.id)
    }

    fun selectNode(index: Int) {
        _state.update { state ->
            val node = state.nodes.getOrNull(index)
            if (node == null || index == state.node) state
            else {
                val previous = state.selectedEpisode
                val match = node.episodes.indexOfFirst {
                    it.edition == previous?.edition && it.number == previous?.number && playable(it, state.baseUrl)
                }
                val episode = if (match >= 0) match else node.episodes.indexOfFirst { playable(it, state.baseUrl) }.coerceAtLeast(0)
                state.copy(node = index, episode = episode, resumePosition = 0,
                    source = node.episodes.getOrNull(episode)?.sources?.indexOfFirst { usable(it, state.baseUrl) }?.coerceAtLeast(0) ?: 0)
            }
        }
    }

    fun selectEpisode(index: Int) {
        _state.update { state ->
            if (state.episode == index) state else state.copy(episode = index, resumePosition = 0,
                source = state.nodes.getOrNull(state.node)?.episodes?.getOrNull(index)?.sources
                    ?.indexOfFirst { usable(it, state.baseUrl) }?.coerceAtLeast(0) ?: 0)
        }
    }

    fun selectSource(index: Int) { _state.update { it.copy(source = index) } }
    fun refreshStats() { statsRevision.update { it + 1 } }
    fun reload() { retry.update { it + 1 } }
    fun reloadRecommendations() { retryRecommendations.update { it + 1 } }

    private fun playable(episode: EpisodeDto, base: String): Boolean = episode.sources.any { usable(it, base) }
    private fun usable(source: SourceDto, base: String): Boolean =
        MediaUrls.resolve(source.url, base, container.apiClient.allowCleartext).ok

    companion object {
        fun factory(container: AppContainer, initialWorkId: Long) = viewModelFactory {
            initializer { VideoViewModel(container, initialWorkId) }
        }
    }
}
