package com.fantasylibrary.android.ui.screens.catalog

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyGridState
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.grid.rememberLazyGridState
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.fantasylibrary.android.data.network.MediaUrls
import com.fantasylibrary.android.ui.components.*

@Composable
fun AnimeScreen(viewModel: CatalogViewModel, onBack: () -> Unit, onOpenDetail: (Long) -> Unit, onSettings: () -> Unit) {
    CatalogScreen(viewModel, onBack, onOpenDetail, onSettings, games = false)
}

@Composable
fun GameScreen(viewModel: CatalogViewModel, onBack: () -> Unit, onOpenDetail: (Long) -> Unit, onSettings: () -> Unit) {
    CatalogScreen(viewModel, onBack, onOpenDetail, onSettings, games = true)
}

@Composable
private fun CatalogScreen(viewModel: CatalogViewModel, onBack: () -> Unit, onOpenDetail: (Long) -> Unit, onSettings: () -> Unit, games: Boolean) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val list = rememberLazyGridState()
    var query by rememberSaveable { mutableStateOf(state.query) }
    LaunchedEffect(viewModel) { if (query != state.query) viewModel.search(query) }
    val filtered = state.query.isNotBlank() || state.selected.isNotEmpty()
    ScrollRoom(if (games) "游戏" else "追番", onBack) {
        PageField(query, { input -> query = input.take(100); viewModel.search(query) }, if (games) "搜索游戏" else "搜索番剧",
            Modifier.fillMaxWidth().padding(start = 16.dp, end = 16.dp, top = 10.dp))
        if (state.tags.isNotEmpty()) {
            LazyRow(contentPadding = PaddingValues(horizontal = 16.dp), horizontalArrangement = Arrangement.spacedBy(7.dp)) {
                item { PageChip("全部", state.selected.isEmpty()) { viewModel.filter(null) } }
                items(state.tags, key = { it.slug }) { tag ->
                    PageChip(tag.name, tag.slug in state.selected) { viewModel.filter(tag.slug) }
                }
            }
        }
        CatalogFeed(state, games, filtered, list, viewModel, onOpenDetail, onSettings)
    }
}

@Composable
private fun CatalogFeed(state: CatalogViewModel.UiState, games: Boolean, filtered: Boolean, list: LazyGridState,
    viewModel: CatalogViewModel, onOpenDetail: (Long) -> Unit, onSettings: () -> Unit,
) {
    val feature = if (!games && !filtered && state.items.size > 1) state.items.first() else null
    val visible = if (feature == null) state.items else state.items.drop(1)
    LazyVerticalGrid(GridCells.Fixed(if (games) 1 else 2), state = list, modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp),
        horizontalArrangement = Arrangement.spacedBy(11.dp)) {
        if (state.loading) {
            item(span = { GridItemSpan(maxLineSpan) }) { PageStatus("正在查阅馆藏…", busy = true) }
        } else if (state.items.isEmpty()) {
            item(span = { GridItemSpan(maxLineSpan) }) {
                when {
                    state.baseUrl.isBlank() -> PageStatus("填写服务器地址后即可查看馆藏。", actionLabel = "连接设置", action = onSettings)
                    state.error != null -> PageStatus(state.error.readerMessage(), action = viewModel::refresh)
                    else -> PageStatus(if (filtered) "没有符合条件的作品。" else "馆藏暂时没有作品。")
                }
            }
        } else {
            if (feature != null) item(span = { GridItemSpan(maxLineSpan) }) {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    ScrollHeading("馆员精选")
                    CatalogTile(feature, onOpenDetail, wide = true)
                }
            }
            if (games && !filtered && state.popular.isNotEmpty()) item {
                Column(verticalArrangement = Arrangement.spacedBy(9.dp)) {
                    ScrollHeading("热门游戏", "按馆内热度排序")
                    LazyRow(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        items(state.popular.take(8), key = { it.entry.work.id }) { ranked ->
                            val work = ranked.entry.work
                            PaperTile(Modifier.width(140.dp), onClick = { onOpenDetail(work.id) }) {
                                CoverArt(MediaUrls.resolve(work.cover?.url, state.baseUrl, com.fantasylibrary.android.BuildConfig.ALLOW_CLEARTEXT),
                                    null, Modifier.fillMaxWidth().aspectRatio(1.38f))
                                Text(work.title, Modifier.padding(8.dp), color = PageInk, maxLines = 1, overflow = TextOverflow.Ellipsis,
                                    style = MaterialTheme.typography.titleSmall)
                            }
                        }
                    }
                }
            }
            item(span = { GridItemSpan(maxLineSpan) }) {
                ScrollHeading(if (filtered) "检索结果" else if (games) "游戏图鉴" else "番剧目录")
            }
            items(visible, key = { it.id }) { entry -> CatalogTile(entry, onOpenDetail, wide = games) }
            if (state.hasMore || state.loadingMore || state.error != null) item(span = { GridItemSpan(maxLineSpan) }) {
                if (state.loadingMore) PageStatus("正在读取下一页…", busy = true)
                else Box(Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
                    PageButton(if (state.error != null) "重试读取下一页" else "加载更多", viewModel::loadMore)
                }
            }
        }
    }
}

@Composable
private fun CatalogTile(item: CatalogCard, onOpen: (Long) -> Unit, wide: Boolean) {
    PaperTile(Modifier.fillMaxWidth(), onClick = { onOpen(item.id) }) {
        if (wide) Row(verticalAlignment = Alignment.CenterVertically) {
            CoverArt(item.cover, null, Modifier.width(120.dp).height(128.dp), if (item.kind == CatalogKind.GAME) "⚔" else "☾")
            CatalogTileText(item, Modifier.weight(1f), summaryLines = 2)
        } else {
            CoverArt(item.cover, null, Modifier.fillMaxWidth().aspectRatio(.73f), "☾")
            CatalogTileText(item, Modifier.fillMaxWidth(), summaryLines = 2)
        }
    }
}

@Composable
private fun CatalogTileText(item: CatalogCard, modifier: Modifier, summaryLines: Int) {
    Column(modifier.padding(horizontal = 10.dp, vertical = 9.dp), verticalArrangement = Arrangement.spacedBy(5.dp)) {
        Text(item.title, color = PageInk, style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.SemiBold),
            maxLines = 2, overflow = TextOverflow.Ellipsis)
        val labels = (listOf(item.category, item.badge) + item.tags.take(2)).filter { it.isNotBlank() }.distinct()
        if (labels.isNotEmpty()) Text(labels.joinToString(" · "), color = PageMuted, style = MaterialTheme.typography.labelSmall,
            maxLines = 1, overflow = TextOverflow.Ellipsis)
        if (item.summary.isNotBlank()) Text(item.summary, color = PageInk, style = MaterialTheme.typography.bodySmall,
            maxLines = summaryLines, overflow = TextOverflow.Ellipsis)
        if (item.metaLine.isNotBlank()) Text(item.metaLine, color = PageMuted, style = MaterialTheme.typography.labelSmall,
            maxLines = 1, overflow = TextOverflow.Ellipsis)
    }
}
