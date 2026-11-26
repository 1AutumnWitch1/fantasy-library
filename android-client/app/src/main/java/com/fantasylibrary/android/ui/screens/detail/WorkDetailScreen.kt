package com.fantasylibrary.android.ui.screens.detail

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.HorizontalDivider
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LifecycleEventEffect
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.fantasylibrary.android.data.network.*
import com.fantasylibrary.android.ui.screens.video.formatClock
import java.time.OffsetDateTime
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import com.fantasylibrary.android.di.AppContainer
import com.fantasylibrary.android.ui.components.*
import com.fantasylibrary.android.ui.screens.catalog.CatalogMapper

@Composable
fun WorkDetailScreen(viewModel: WorkDetailViewModel, container: AppContainer, onBack: () -> Unit, onLogin: () -> Unit,
    onPlayAnime: (Long) -> Unit, onDiscussion: (Long) -> Unit,
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val detail = state.detail
    val failure = state.error
    val session by container.authRepository.session.collectAsStateWithLifecycle()
    ScrollRoom(if (detail?.work?.type == "GAME") "游戏档案" else "作品档案", onBack, backLabel = "返回") {
        when {
            state.loading -> PageStatus("正在取出作品档案…", busy = true)
            failure != null -> PageStatus(failure.readerMessage(), action = viewModel::retry)
            detail == null -> PageStatus("没有找到这部作品。")
            else -> {
                val work = detail.work
                var revision by remember(work.id) { mutableIntStateOf(0) }
                var showAll by rememberSaveable(work.id) { mutableStateOf(false) }
                var progress by remember(work.id, session.username) { mutableStateOf<Outcome<ViewingProgressDto>?>(null) }
                var stats by remember(work.id) { mutableStateOf<PlaybackStatsDto?>(null) }
                LifecycleEventEffect(Lifecycle.Event.ON_RESUME) { revision++ }
                LaunchedEffect(work.id, state.baseUrl, revision) {
                    if (state.isAnime) {
                        stats = (container.readerRepository.playbackStats(state.baseUrl, work.id) as? Outcome.Ok)?.value
                    }
                }
                LaunchedEffect(work.id, state.baseUrl, session.username, revision) {
                    if (state.isAnime && session.signedIn) progress = container.viewingSync.progress(state.baseUrl, work.id)
                }
                val viewing = (progress as? Outcome.Ok)?.value
                val latest = if (session.signedIn) viewing?.latest else container.viewingSync.localProgress(state.baseUrl, work.id)
                val resume = latest != null && !latest.completed && state.episodes.any {
                    it.edition == "EPISODE" && it.number == latest.episodeNo && it.sources.isNotEmpty()
                }
                val playLabel = if (resume) "继续观看" else if (latest == null) "开始观看" else "观看分集"
                LazyColumn(Modifier.fillMaxSize(), verticalArrangement = Arrangement.spacedBy(10.dp),
                    contentPadding = PaddingValues(16.dp)) {
                    item {
                        PaperTile(Modifier.fillMaxWidth()) {
                            Row(Modifier.padding(12.dp), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                                CoverArt(state.cover, work.cover?.altText, Modifier.width(93.dp).aspectRatio(.73f))
                                Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                                    work.category?.name?.let { Text(it, color = PageMuted, style = MaterialTheme.typography.labelMedium) }
                                    Text(work.title, color = PageInk, style = MaterialTheme.typography.titleLarge)
                                    val metadata = listOfNotNull(
                                        work.releaseDate,
                                        detail.anime?.studio?.takeIf { it.isNotBlank() && it != "未知" },
                                        detail.anime?.episodeCount?.let { "$it 集" },
                                        CatalogMapper.airingLabel(detail.anime?.airingStatus).takeIf { it.isNotBlank() },
                                        detail.game?.developer,
                                        detail.game?.ageRating,
                                        detail.game?.platforms?.joinToString(" / ") { it.name }?.takeIf { it.isNotEmpty() },
                                    )
                                    metadata.forEach { Text(it, color = PageMuted, style = MaterialTheme.typography.bodySmall) }
                                    stats?.let {
                                        Text("${it.followers} 人追番 · ${it.plays} 次播放", color = PageMuted,
                                            style = MaterialTheme.typography.labelMedium)
                                    }
                                }
                            }
                            Column(Modifier.padding(start = 12.dp, end = 12.dp, bottom = 9.dp),
                                verticalArrangement = Arrangement.spacedBy(3.dp)) {
                                CollectionActions(container, state.baseUrl, work.id, state.isAnime, onLogin) { revision++ }
                                if (state.isAnime) PageButton(playLabel, { onPlayAnime(work.id) }, emphasized = true)
                            }
                        }
                    }
                    if (detail.tags.isNotEmpty()) item {
                        Text(detail.tags.joinToString("  ·  ") { it.name }, color = PageMuted, style = MaterialTheme.typography.labelMedium)
                    }
                    work.summary?.takeIf { it.isNotBlank() }?.let { summary ->
                        item {
                            var overflow by remember(summary) { mutableStateOf(false) }
                            Text("作品简介", color = PageInk, style = MaterialTheme.typography.titleMedium)
                            Text(summary, Modifier.padding(top = 4.dp), color = PageInk,
                                style = MaterialTheme.typography.bodyMedium, maxLines = if (showAll) Int.MAX_VALUE else 3,
                                overflow = TextOverflow.Ellipsis, onTextLayout = { if (!showAll) overflow = it.hasVisualOverflow })
                            if (overflow || showAll) PageButton(if (showAll) "收起简介" else "展开简介", { showAll = !showAll })
                        }
                    }
                    if (state.isAnime) item {
                        HorizontalDivider(color = PageLine.copy(alpha = .5f))
                        Column(Modifier.padding(top = 9.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                            Text("我的观看记录", color = PageInk, style = MaterialTheme.typography.titleMedium)
                            val recorded = when {
                                !session.signedIn -> "登录后可在设备间同步观看记录"
                                progress is Outcome.Err -> "暂时无法读取观看进度"
                                viewing == null -> "正在读取观看记录…"
                                latest == null -> "尚未观看"
                                else -> {
                                    val complete = detail.anime?.episodeCount?.let { viewing.completedEpisodes >= it } == true
                                    val active = if (complete) "已看完" else if (!latest.completed) "正在观看第 ${latest.episodeNo} 集" else null
                                    listOfNotNull("已看 ${viewing.completedEpisodes} 集", active).joinToString(" · ")
                                }
                            }
                            Text(recorded, color = PageMuted, style = MaterialTheme.typography.bodyMedium)
                            if (latest != null && session.signedIn) {
                                val date = runCatching {
                                    OffsetDateTime.parse(latest.updatedAt).atZoneSameInstant(ZoneId.systemDefault())
                                        .format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm"))
                                }.getOrDefault(latest.updatedAt)
                                Text("最近观看 · 第 ${latest.episodeNo} 集 · ${formatClock(latest.positionMs)}",
                                    color = PageMuted, style = MaterialTheme.typography.bodySmall)
                                Text(date, color = PageMuted, style = MaterialTheme.typography.labelSmall)
                            }
                            if (progress is Outcome.Err) PageButton("重试", { revision++ })
                        }
                    }
                    if (!state.isAnime) item { DownloadActions(container, state.baseUrl, work.id) }
                    item { PageButton("查看作品讨论", { onDiscussion(work.id) }) }
                    val gallery = state.gallery.filter { it.ok }.distinctBy { it.resolved }
                    if (gallery.isNotEmpty()) item {
                        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                            ScrollHeading("馆藏插图")
                            LazyRow(horizontalArrangement = Arrangement.spacedBy(9.dp)) {
                                items(gallery, key = { it.resolved.orEmpty() }) { image ->
                                    PaperTile(Modifier.width(200.dp)) { CoverArt(image, "馆藏插图", Modifier.fillMaxWidth().aspectRatio(1.5f)) }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}
