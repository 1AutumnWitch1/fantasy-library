package com.fantasylibrary.android.ui.screens.recommendation

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.fantasylibrary.android.data.network.*
import com.fantasylibrary.android.di.AppContainer
import com.fantasylibrary.android.ui.components.*
import com.fantasylibrary.android.ui.screens.community.PostPreview
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

class RecommendationViewModel(private val container: AppContainer) : ViewModel() {
    data class State(val base: String = "", val feed: Outcome<FeedDto>? = null)
    private val _state = MutableStateFlow(State())
    val state = _state.asStateFlow()
    private val retry = MutableStateFlow(0)

    init {
        viewModelScope.launch {
            combine(
                container.settingsRepository.settings.map { it.baseUrl }.distinctUntilChanged(),
                container.authRepository.session.map { it.signedIn }.distinctUntilChanged(),
                retry,
            ) { base, signedIn, _ -> base to signedIn }.collectLatest { (base, signedIn) ->
                _state.value = State(base)
                if (base.isNotBlank()) {
                    val repo = container.readerRepository
                    val result = repo.recommendations(base, signedIn)
                    val feed = if (signedIn && result is Outcome.Err && result.failure.kind == FailureKind.UNAUTHORIZED) {
                        repo.recommendations(base)
                    } else result
                    _state.value = State(base, feed)
                }
            }
        }
    }

    fun reload() { retry.update { it + 1 } }

    companion object {
        fun factory(container: AppContainer) = viewModelFactory { initializer { RecommendationViewModel(container) } }
    }
}

@Composable
fun RecommendationScreen(viewModel: RecommendationViewModel, cleartext: Boolean, onBack: () -> Unit,
    onWork: (Long) -> Unit, onPost: (Long) -> Unit, onSettings: () -> Unit,
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    var channel by rememberSaveable { mutableStateOf(0) }
    val lists = rememberLazyListState()
    val channels = listOf("为你推荐", "热门榜", "新入馆藏", "活跃讨论")
    ScrollRoom("推荐", onBack) {
        LazyRow(contentPadding = PaddingValues(horizontal = 16.dp), horizontalArrangement = Arrangement.spacedBy(7.dp)) {
            items(channels.size) { index -> PageChip(channels[index], channel == index) { channel = index } }
        }
        when (val result = state.feed) {
            null -> if (state.base.isBlank()) PageStatus("连接服务器后即可浏览推荐。", actionLabel = "连接设置", action = onSettings)
                else PageStatus("正在整理精选…", busy = true)
            is Outcome.Err -> PageStatus(result.failure.readerMessage(), action = viewModel::reload)
            is Outcome.Ok -> key(channel) {
                val feed = result.value
                LazyColumn(Modifier.fillMaxSize(), state = if (channel == 0) lists else rememberLazyListState(),
                    contentPadding = PaddingValues(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    item {
                        ScrollHeading(channels[channel], when (channel) {
                            0 -> when (feed.mode) {
                                "PERSONALIZED" -> "按你的私人藏书偏好挑选"
                                "POPULAR" -> "来自馆内真实热度"
                                "EDITORIAL" -> "馆员整理的作品"
                                else -> null
                            }
                            1 -> "按馆内收藏、浏览和讨论排序"
                            else -> null
                        })
                    }
                    when (channel) {
                        0 -> {
                            items(feed.forYou.distinctBy { it.entry.work.id }, key = { it.entry.work.id }) {
                                RecommendationTile(it.entry, state.base, cleartext, it.reason, onWork)
                            }
                            if (feed.forYou.isEmpty()) item { PageStatus("暂时没有推荐作品。") }
                        }
                        1 -> {
                            items(feed.popular.distinctBy { it.entry.work.id }, key = { it.entry.work.id }) {
                                RecommendationTile(it.entry, state.base, cleartext,
                                    "${it.heat.favoriteCount} 收藏 · ${it.heat.viewCount} 浏览 · ${it.heat.commentCount} 评论", onWork)
                            }
                            if (feed.popular.isEmpty()) item { PageStatus("暂时没有热门作品。") }
                        }
                        2 -> {
                            items(feed.newest.distinctBy { it.work.id }, key = { it.work.id }) {
                                RecommendationTile(it, state.base, cleartext, it.addedAt?.take(10)?.let { date -> "$date 入馆" }, onWork)
                            }
                            if (feed.newest.isEmpty()) item { PageStatus("暂时没有新增作品。") }
                        }
                        else -> {
                            items(feed.discussions.distinctBy { it.id }, key = { it.id }) {
                                PostPreview(it, state.base, cleartext) { onPost(it.id) }
                            }
                            if (feed.discussions.isEmpty()) item { PageStatus("暂时没有活跃讨论。") }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun RecommendationTile(entry: WorkEntryDto, base: String, cleartext: Boolean, reason: String?, onWork: (Long) -> Unit) {
    val work = entry.work
    PaperTile(Modifier.fillMaxWidth(), onClick = { onWork(work.id) }) {
        Row(Modifier.fillMaxWidth()) {
            CoverArt(MediaUrls.resolve(work.cover?.url, base, cleartext), null, Modifier.width(104.dp).height(130.dp))
            Column(Modifier.weight(1f).padding(11.dp), verticalArrangement = Arrangement.spacedBy(5.dp)) {
                Text((listOf(if (work.type == "GAME") "游戏" else "追番") + entry.tags.take(2).map { it.name }).joinToString(" · "),
                    color = PageMuted, style = MaterialTheme.typography.labelSmall, maxLines = 1, overflow = TextOverflow.Ellipsis)
                Text(work.title, color = PageInk, style = MaterialTheme.typography.titleMedium, maxLines = 2, overflow = TextOverflow.Ellipsis)
                work.summary?.takeIf { it.isNotBlank() }?.let { Text(it, color = PageInk, style = MaterialTheme.typography.bodySmall,
                    maxLines = 2, overflow = TextOverflow.Ellipsis) }
                reason?.takeIf { it.isNotBlank() }?.let { Text(it, color = PageMuted, style = MaterialTheme.typography.labelMedium) }
            }
        }
    }
}
