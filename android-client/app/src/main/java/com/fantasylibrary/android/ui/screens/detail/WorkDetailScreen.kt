package com.fantasylibrary.android.ui.screens.detail

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
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
    ScrollRoom(if (detail?.work?.type == "GAME") "游戏档案" else "作品档案", onBack, backLabel = "返回") {
        when {
            state.loading -> PageStatus("正在取出作品档案…", busy = true)
            failure != null -> PageStatus(failure.readerMessage(), action = viewModel::retry)
            detail == null -> PageStatus("没有找到这部作品。")
            else -> {
                val work = detail.work
                LazyColumn(Modifier.fillMaxSize(), verticalArrangement = Arrangement.spacedBy(14.dp),
                    contentPadding = PaddingValues(16.dp)) {
                    item {
                        PaperTile(Modifier.fillMaxWidth()) {
                            Row(Modifier.padding(12.dp), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                                CoverArt(state.cover, work.cover?.altText, Modifier.width(109.dp).aspectRatio(.73f))
                                Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                                    work.category?.name?.let { Text(it, color = PageMuted, style = MaterialTheme.typography.labelMedium) }
                                    Text(work.title, color = PageInk, style = MaterialTheme.typography.headlineSmall)
                                    val metadata = listOfNotNull(
                                        work.releaseDate,
                                        detail.anime?.studio,
                                        detail.anime?.episodeCount?.let { "$it 集" },
                                        CatalogMapper.airingLabel(detail.anime?.airingStatus).takeIf { it.isNotBlank() },
                                        detail.game?.developer,
                                        detail.game?.ageRating,
                                        detail.game?.platforms?.joinToString(" / ") { it.name }?.takeIf { it.isNotEmpty() },
                                    )
                                    metadata.forEach { Text(it, color = PageMuted, style = MaterialTheme.typography.bodySmall) }
                                }
                            }
                            if (state.isAnime) {
                                Row(Modifier.padding(start = 12.dp, end = 12.dp, bottom = 7.dp)) {
                                    PageButton("开始观看", { onPlayAnime(work.id) }, emphasized = true)
                                }
                            }
                        }
                    }
                    if (detail.tags.isNotEmpty()) item {
                        Text(detail.tags.joinToString("  ·  ") { it.name }, color = PageMuted, style = MaterialTheme.typography.labelMedium)
                    }
                    work.summary?.takeIf { it.isNotBlank() }?.let { summary ->
                        item {
                            ScrollHeading("作品简介")
                            Text(summary, Modifier.padding(top = 9.dp), color = PageInk, style = MaterialTheme.typography.bodyLarge)
                        }
                    }
                    item { CollectionActions(container, state.baseUrl, detail, onLogin) }
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
