package com.fantasylibrary.android.ui.screens.detail

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.fantasylibrary.android.ui.components.EmptyPanel
import com.fantasylibrary.android.ui.components.FailurePanel
import com.fantasylibrary.android.ui.components.KeyValueRow
import com.fantasylibrary.android.ui.components.LeatherPanel
import com.fantasylibrary.android.ui.components.LibraryTopBar
import com.fantasylibrary.android.ui.components.LoadingPanel
import com.fantasylibrary.android.ui.components.PaperCard
import com.fantasylibrary.android.ui.components.PrimaryButton
import com.fantasylibrary.android.ui.components.RemoteImage
import com.fantasylibrary.android.ui.components.SectionTitle
import com.fantasylibrary.android.ui.components.StatusPill
import com.fantasylibrary.android.ui.components.TagChips
import com.fantasylibrary.android.ui.components.Tone
import com.fantasylibrary.android.ui.components.libraryBackground
import com.fantasylibrary.android.ui.theme.Brass
import com.fantasylibrary.android.ui.theme.BrassDim
import com.fantasylibrary.android.ui.theme.Ink
import com.fantasylibrary.android.ui.theme.Parchment
import com.fantasylibrary.android.ui.theme.ParchmentDeep
import com.fantasylibrary.android.ui.theme.ParchmentDim
import com.fantasylibrary.android.ui.theme.WalnutDeep
import com.fantasylibrary.android.ui.screens.catalog.CatalogMapper

@Composable
fun WorkDetailScreen(
    viewModel: WorkDetailViewModel,
    onBack: () -> Unit,
    onPlayAnime: (Long) -> Unit,
) {
    val state by viewModel.state.collectAsStateWithLifecycle()

    Column(
        Modifier
            .fillMaxSize()
            .libraryBackground(),
    ) {
        LibraryTopBar(title = "作品详情", subtitle = "真实数据库记录", onBack = onBack)

        when {
            state.loading -> LoadingPanel("正在读取作品详情…")
            state.error != null -> Column(Modifier.padding(16.dp)) {
                FailurePanel(failure = state.error!!, onRetry = viewModel::retry)
            }
            state.detail == null -> Column(Modifier.padding(16.dp)) {
                EmptyPanel("没有找到这部作品")
            }
            else -> {
                val detail = state.detail!!
                val work = detail.work
                Column(
                    Modifier
                        .fillMaxSize()
                        .verticalScroll(rememberScrollState())
                        .navigationBarsPadding(),
                ) {
                    // 封面横幅
                    Box(
                        Modifier
                            .fillMaxWidth()
                            .aspectRatio(16f / 9f),
                    ) {
                        state.cover?.let { cover ->
                            RemoteImage(
                                check = cover,
                                contentDescription = work.title,
                                modifier = Modifier.fillMaxSize(),
                                contentScale = ContentScale.Crop,
                            )
                        }
                        Box(
                            Modifier
                                .fillMaxSize()
                                .background(
                                    Brush.verticalGradient(
                                        listOf(Color.Transparent, WalnutDeep.copy(alpha = 0.92f)),
                                    ),
                                ),
                        )
                        Column(
                            Modifier
                                .align(Alignment.BottomStart)
                                .padding(horizontal = 16.dp, vertical = 12.dp),
                        ) {
                            Text(
                                text = work.title,
                                style = MaterialTheme.typography.headlineSmall,
                                color = Parchment,
                            )
                            Row(
                                horizontalArrangement = Arrangement.spacedBy(6.dp),
                                verticalAlignment = Alignment.CenterVertically,
                            ) {
                                if (work.category?.name != null) {
                                    StatusPill(text = work.category.name, tone = Tone.Brass)
                                }
                                val airing = CatalogMapper.airingLabel(detail.anime?.airingStatus)
                                if (airing.isNotBlank()) {
                                    StatusPill(text = airing, tone = Tone.Ok)
                                }
                                detail.game?.ageRating?.takeIf { it.isNotBlank() }?.let {
                                    StatusPill(text = it, tone = Tone.Warn)
                                }
                            }
                        }
                    }

                    Column(Modifier.padding(horizontal = 16.dp, vertical = 12.dp)) {
                        // 元信息
                        LeatherPanel(Modifier.fillMaxWidth()) {
                            KeyValueRow("作品 ID", work.id.toString())
                            KeyValueRow("类型", if (work.type == "GAME") "游戏" else "动漫")
                            work.slug.takeIf { it.isNotBlank() }?.let {
                                KeyValueRow("标识", it, mono = true)
                            }
                            work.releaseDate?.takeIf { it.isNotBlank() }?.let {
                                KeyValueRow("发布日期", it)
                            }
                            detail.anime?.let { anime ->
                                anime.studio?.takeIf { it.isNotBlank() }?.let {
                                    KeyValueRow("制作公司", it)
                                }
                                anime.episodeCount?.let {
                                    KeyValueRow("集数", "$it 集")
                                }
                            }
                            detail.game?.let { game ->
                                game.developer?.takeIf { it.isNotBlank() }?.let {
                                    KeyValueRow("开发商", it)
                                }
                                game.ageRating?.takeIf { it.isNotBlank() }?.let {
                                    KeyValueRow("分级", it)
                                }
                                if (game.platforms.isNotEmpty()) {
                                    KeyValueRow("平台", game.platforms.joinToString(" / ") { it.name })
                                }
                            }
                        }

                        Spacer(Modifier.height(14.dp))
                        SectionTitle("简介")
                        Spacer(Modifier.height(6.dp))
                        PaperCard(Modifier.fillMaxWidth()) {
                            Text(
                                text = work.summary?.takeIf { it.isNotBlank() } ?: "后端没有提供简介",
                                style = MaterialTheme.typography.bodyMedium,
                                color = Ink,
                            )
                        }

                        if (detail.tags.isNotEmpty()) {
                            Spacer(Modifier.height(14.dp))
                            SectionTitle("标签", trailing = "${detail.tags.size} 个")
                            Spacer(Modifier.height(6.dp))
                            TagChips(
                                tags = detail.tags.map { it.name },
                                maxShown = 20,
                            )
                        }

                        if (state.gallery.size > 1) {
                            Spacer(Modifier.height(14.dp))
                            SectionTitle("图片资料", trailing = "MinIO")
                            Spacer(Modifier.height(6.dp))
                            LazyRow(
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                                contentPadding = PaddingValues(end = 8.dp),
                            ) {
                                itemsIndexed(state.gallery) { index, check ->
                                    Surface(
                                        shape = RoundedCornerShape(7.dp),
                                        border = BorderStroke(1.dp, BrassDim.copy(alpha = 0.7f)),
                                        modifier = Modifier
                                            .width(150.dp)
                                            .height(100.dp),
                                    ) {
                                        RemoteImage(
                                            check = check,
                                            contentDescription = "图片 ${index + 1}",
                                            modifier = Modifier.fillMaxSize(),
                                        )
                                    }
                                }
                            }
                        }

                        if (state.isAnime) {
                            Spacer(Modifier.height(16.dp))
                            SectionTitle(
                                "分集与播放源",
                                trailing = if (state.episodesLoading) "读取中" else "${state.episodes.size} 集",
                            )
                            Spacer(Modifier.height(6.dp))
                            when {
                                state.episodesError != null -> FailurePanel(
                                    failure = state.episodesError!!,
                                    onRetry = viewModel::retry,
                                )
                                state.episodes.isEmpty() -> PaperCard(Modifier.fillMaxWidth()) {
                                    Text(
                                        text = "该作品在数据库中还没有已发布的播放分集。",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = ParchmentDeep,
                                    )
                                }
                                else -> Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                                    state.episodes.forEach { episode ->
                                        PaperCard(Modifier.fillMaxWidth(), contentPadding = 10) {
                                            Row(verticalAlignment = Alignment.CenterVertically) {
                                                Text(
                                                    text = "第 ${episode.number} 集",
                                                    style = MaterialTheme.typography.titleSmall,
                                                    color = Ink,
                                                )
                                                Spacer(Modifier.width(8.dp))
                                                Text(
                                                    text = (episode.title ?: "").take(28),
                                                    style = MaterialTheme.typography.bodySmall,
                                                    color = ParchmentDeep,
                                                    modifier = Modifier.weight(1f),
                                                )
                                                StatusPill(
                                                    text = "${episode.sources.size} 个源",
                                                    tone = if (episode.sources.isEmpty()) Tone.Bad else Tone.Ok,
                                                )
                                            }
                                        }
                                    }
                                }
                            }

                            Spacer(Modifier.height(12.dp))
                            PrimaryButton(
                                text = "进入视频播放测试",
                                onClick = { onPlayAnime(work.id) },
                                enabled = state.episodes.any { it.sources.isNotEmpty() },
                                modifier = Modifier.fillMaxWidth(),
                            )
                            if (state.episodes.none { it.sources.isNotEmpty() }) {
                                Spacer(Modifier.height(6.dp))
                                Text(
                                    text = "没有可播放的分集源，无法启动播放测试。",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = ParchmentDim,
                                )
                            }
                        }

                        Spacer(Modifier.height(20.dp))
                        Text(
                            text = "数据来源：Spring Boot /api/works/${work.id} · 图片来自 MinIO",
                            style = MaterialTheme.typography.labelSmall,
                            color = ParchmentDim,
                            fontFamily = FontFamily.Monospace,
                        )
                        Spacer(Modifier.height(28.dp))
                    }
                }
            }
        }
    }
}
