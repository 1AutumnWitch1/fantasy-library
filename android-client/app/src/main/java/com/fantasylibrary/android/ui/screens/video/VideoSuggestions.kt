package com.fantasylibrary.android.ui.screens.video

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.fantasylibrary.android.data.network.*
import com.fantasylibrary.android.di.AppContainer
import com.fantasylibrary.android.ui.components.*
import com.fantasylibrary.android.ui.theme.Parchment
import com.fantasylibrary.android.ui.theme.WalnutDeep
import kotlinx.coroutines.launch

@Composable
fun VideoSuggestions(viewModel: VideoViewModel, state: VideoViewModel.State, container: AppContainer,
    list: LazyListState, sourceMessage: String?, onSettings: () -> Unit, onLogin: () -> Unit,
) {
    val cleartext = container.apiClient.allowCleartext
    LazyColumn(Modifier.fillMaxSize(), state = list,
        contentPadding = PaddingValues(horizontal = 14.dp, vertical = 11.dp),
        verticalArrangement = Arrangement.spacedBy(11.dp)) {
        item {
            when {
                state.baseUrl.isBlank() && !state.loading ->
                    PageStatus("连接图书馆后即可播放。", actionLabel = "连接设置", action = onSettings)
                state.error != null -> PageStatus(state.error.readerMessage(), action = viewModel::reload)
                else -> VideoInfo(container, state, onLogin)
            }
        }
        if (state.nodes.isNotEmpty()) item {
            PlaybackChoices(viewModel, state, sourceMessage)
        } else if (state.baseUrl.isNotBlank() && !state.loading && state.error == null) {
            item { Text("该作品暂未提供可播放的分集。", color = PageMuted, style = MaterialTheme.typography.bodySmall) }
        }
        item { ScrollHeading("相关推荐") }
        when {
            state.recommendationsError != null -> item {
                PageStatus(state.recommendationsError.readerMessage(), action = viewModel::reloadRecommendations)
            }
            state.recommendationsLoading && state.baseUrl.isNotBlank() -> item { PageStatus("正在查找馆藏视频…", busy = true) }
            state.recommendations.isEmpty() -> item { PageStatus("暂时没有可播放的相关视频。") }
            else -> items(state.recommendations, key = { it.entry.work.id }) { recommendation ->
                RecommendationRow(recommendation, state.baseUrl, cleartext) {
                    viewModel.openRecommendation(recommendation)
                }
            }
        }
    }
}

@Composable
private fun VideoInfo(container: AppContainer, state: VideoViewModel.State, onLogin: () -> Unit) {
    val detail = state.detail
    val work = detail?.work
    val episode = state.selectedEpisode
    if (work == null) {
        if (state.loading) PageStatus("正在准备放映…", busy = true)
        else if (episode != null) ScrollHeading(episode.videoLabel())
        return
    }
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        val studio = detail.anime?.studio?.takeIf { it.isNotBlank() && it != "未知" }
        if (studio != null) Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            // 馆藏只记录制作方名称，使用统一纹章作为制作方标记。
            ReaderAvatar(null, state.baseUrl, container.apiClient.allowCleartext, "", Modifier.size(34.dp))
            Text(studio, color = PageInk, style = MaterialTheme.typography.titleSmall)
            Text("制作方", color = PageMuted, style = MaterialTheme.typography.labelSmall)
        }
        Text(work.title, color = PageInk, style = MaterialTheme.typography.headlineSmall)
        val meta = listOfNotNull(episode?.videoLabel(), work.releaseDate?.takeIf { it.isNotBlank() }?.let { "首播 $it" },
            state.heat?.favoriteCount?.let { "$it 收藏" })
        if (meta.isNotEmpty()) Text(meta.joinToString(" · "), color = PageMuted, style = MaterialTheme.typography.labelMedium)
        work.summary?.takeIf { it.isNotBlank() }?.let { summary ->
            var expanded by rememberSaveable(work.id) { mutableStateOf(false) }
            Text(summary, color = PageInk, style = MaterialTheme.typography.bodyMedium,
                maxLines = if (expanded) Int.MAX_VALUE else 3, overflow = TextOverflow.Ellipsis)
            if (summary.length > 80) PageButton(if (expanded) "收起简介" else "展开简介", { expanded = !expanded })
        }
        VideoFavorite(container, state.baseUrl, work.id, onLogin)
        HorizontalDivider(color = PageLine.copy(alpha = .45f))
    }
}

@Composable
private fun VideoFavorite(container: AppContainer, base: String, work: Long, onLogin: () -> Unit) {
    val session by container.authRepository.session.collectAsStateWithLifecycle()
    val repo = container.readerRepository
    val scope = rememberCoroutineScope()
    var retry by remember { mutableIntStateOf(0) }
    var entry by remember(base, work, session.signedIn) { mutableStateOf<SavedWorkDto?>(null) }
    var ready by remember(base, work, session.signedIn) { mutableStateOf(false) }
    var error by remember(base, work, session.signedIn) { mutableStateOf<String?>(null) }
    var busy by remember(base, work, session.signedIn) { mutableStateOf(false) }
    LaunchedEffect(base, work, session.signedIn, retry) {
        if (base.isNotBlank() && session.signedIn) {
            error = null
            when (val result = repo.saved(base, work)) {
                is Outcome.Ok -> { entry = result.value.item; ready = true }
                is Outcome.Err -> error = result.failure.readerMessage()
            }
        }
    }
    val saved = entry
    val favorite = saved?.favorite == 1
    PageButton(if (favorite) "★ 已收藏番剧" else "☆ 收藏番剧", enabled = !busy && (ready || !session.signedIn),
        onClick = {
            if (!session.signedIn) onLogin()
            else {
                busy = true
                error = null
                scope.launch {
                    try {
                        val retain = saved != null && (saved.watchedEpisodes > 0 || saved.watchStatus != null || saved.wishlist != 0)
                        if (favorite && !retain) when (val result = repo.remove(base, work)) {
                            is Outcome.Ok -> entry = null
                            is Outcome.Err -> error = result.failure.readerMessage()
                        } else when (val result = repo.save(base, work, SaveWorkDto(!favorite, saved?.wishlist == 1,
                            saved?.watchStatus, saved?.watchedEpisodes ?: 0))) {
                            is Outcome.Ok -> entry = result.value
                            is Outcome.Err -> error = result.failure.readerMessage()
                        }
                    } finally { busy = false }
                }
            }
        })
    error?.let { text ->
        Text(text, color = PageMuted, style = MaterialTheme.typography.bodySmall)
        if (!ready) PageButton("重试读取收藏", { retry++ })
    }
}

@Composable
private fun PlaybackChoices(viewModel: VideoViewModel, state: VideoViewModel.State, sourceMessage: String?) {
    val node = state.nodes.getOrNull(state.node) ?: return
    var expanded by rememberSaveable(state.workId) { mutableStateOf(false) }
    PaperTile(Modifier.fillMaxWidth()) {
        Column(Modifier.padding(horizontal = 10.dp, vertical = 4.dp)) {
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f).padding(start = 3.dp)) {
                    Text(node.label, color = PageInk, style = MaterialTheme.typography.titleSmall)
                    val active = listOfNotNull(state.selectedEpisode?.videoLabel(), state.selectedSource?.quality)
                    Text(active.joinToString(" · "), color = PageMuted, style = MaterialTheme.typography.labelMedium)
                }
                PageButton(if (expanded) "收起" else "选集与线路", { expanded = !expanded })
            }
            if (expanded) {
                Text("播放节点", color = PageInk, style = MaterialTheme.typography.titleSmall)
                LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    itemsIndexed(state.nodes) { index, item -> PageChip(item.label, index == state.node) { viewModel.selectNode(index) } }
                }
                Text("选集", color = PageInk, style = MaterialTheme.typography.titleSmall)
                LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    itemsIndexed(node.episodes) { index, ep -> PageChip(ep.videoLabel(), index == state.episode) { viewModel.selectEpisode(index) } }
                }
                val sources = state.selectedEpisode?.sources.orEmpty()
                if (sources.isNotEmpty()) {
                    Text("播放来源", color = PageInk, style = MaterialTheme.typography.titleSmall)
                    LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        itemsIndexed(sources) { index, source ->
                            val label = listOfNotNull(source.label ?: "来源 ${index + 1}", source.quality).joinToString(" · ")
                            PageChip(label, index == state.source) { viewModel.selectSource(index) }
                        }
                    }
                }
            }
            if (sourceMessage != null) Text(sourceMessage, Modifier.padding(vertical = 6.dp), color = PageMuted,
                style = MaterialTheme.typography.bodySmall)
        }
    }
}

@Composable
private fun RecommendationRow(item: VideoRecommendation, base: String, cleartext: Boolean, onOpen: () -> Unit) {
    val work = item.entry.work
    PaperTile(Modifier.fillMaxWidth(), onOpen) {
        Row(Modifier.fillMaxWidth().padding(8.dp), verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(9.dp)) {
            Box(Modifier.width(122.dp).aspectRatio(16f / 9).clip(RoundedCornerShape(6.dp))) {
                CoverArt(MediaUrls.resolve(work.cover?.url, base, cleartext), null, Modifier.matchParentSize())
                item.episode.durationSeconds?.takeIf { it > 0 }?.let {
                    Text(formatClock(it * 1000L), color = Parchment, style = MaterialTheme.typography.labelSmall,
                        modifier = Modifier.align(Alignment.BottomEnd).background(WalnutDeep.copy(alpha = .85f)).padding(horizontal = 4.dp))
                }
            }
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text(work.title, color = PageInk, style = MaterialTheme.typography.titleSmall,
                    maxLines = 2, overflow = TextOverflow.Ellipsis)
                val episode = item.episode.videoLabel()
                val text = listOfNotNull(item.studio, episode, item.heat?.favoriteCount?.let { "$it 收藏" }).joinToString(" · ")
                Text(text, color = PageMuted, style = MaterialTheme.typography.labelSmall,
                    maxLines = 1, overflow = TextOverflow.Ellipsis)
                item.reason?.takeIf { it.isNotBlank() }?.let { Text(it, color = PageMuted, maxLines = 1,
                    style = MaterialTheme.typography.bodySmall, overflow = TextOverflow.Ellipsis) }
            }
        }
    }
}

private fun EpisodeDto.videoLabel(): String {
    val prefix = if (edition == "PREVIEW") "预告" else "第 $number 集"
    val name = title?.takeIf { it.isNotBlank() && it != number.toString() && it != prefix }
    return if (name != null) "$prefix · $name" else prefix
}
