package com.fantasylibrary.android.ui.screens.catalog

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
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.fantasylibrary.android.ui.components.EmptyPanel
import com.fantasylibrary.android.ui.components.FailurePanel
import com.fantasylibrary.android.ui.components.LibraryTopBar
import com.fantasylibrary.android.ui.components.LinkAction
import com.fantasylibrary.android.ui.components.LoadingPanel
import com.fantasylibrary.android.ui.components.RemoteImage
import com.fantasylibrary.android.ui.components.SecondaryButton
import com.fantasylibrary.android.ui.components.TagChips
import com.fantasylibrary.android.ui.components.libraryBackground
import com.fantasylibrary.android.ui.theme.Brass
import com.fantasylibrary.android.ui.theme.BrassDim
import com.fantasylibrary.android.ui.theme.Ink
import com.fantasylibrary.android.ui.theme.InkGreen
import com.fantasylibrary.android.ui.theme.Parchment
import com.fantasylibrary.android.ui.theme.ParchmentDeep
import com.fantasylibrary.android.ui.theme.ParchmentDim

@Composable
fun CatalogScreen(
    viewModel: CatalogViewModel,
    onOpenDetail: (Long) -> Unit,
) {
    val state by viewModel.state.collectAsStateWithLifecycle()

    Column(
        Modifier
            .fillMaxSize()
            .libraryBackground(),
    ) {
        LibraryTopBar(
            title = viewModel.kind.title,
            subtitle = viewModel.kind.subtitle,
            actions = {
                LinkAction(
                    text = if (state.loading) "刷新中…" else "刷新",
                    onClick = viewModel::refresh,
                )
            },
        )

        when {
            state.loading && state.items.isEmpty() -> LoadingPanel("正在从 Oracle 读取馆藏…")
            state.items.isEmpty() && state.error != null -> Column(Modifier.padding(16.dp)) {
                FailurePanel(failure = state.error!!, onRetry = viewModel::refresh)
            }
            state.items.isEmpty() -> Column(Modifier.padding(16.dp)) {
                EmptyPanel("馆藏里暂时没有这类作品")
                SecondaryButton(
                    text = "重新读取",
                    onClick = viewModel::refresh,
                    modifier = Modifier.fillMaxWidth(),
                )
            }
            else -> LazyVerticalGrid(
                columns = GridCells.Fixed(2),
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(horizontal = 14.dp, vertical = 12.dp),
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                item(span = { GridItemSpan(maxLineSpan) }) {
                    Column {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                text = "共 ${state.total} 部",
                                style = MaterialTheme.typography.labelMedium,
                                color = Brass,
                            )
                            Spacer(Modifier.padding(start = 8.dp))
                            Text(
                                text = "第 ${state.page}${if (state.totalPages > 0) " / ${state.totalPages}" else ""} 页",
                                style = MaterialTheme.typography.labelSmall,
                                color = ParchmentDeep,
                            )
                        }
                        Spacer(Modifier.height(2.dp))
                        Box(
                            Modifier
                                .fillMaxWidth()
                                .height(1.dp)
                                .background(BrassDim.copy(alpha = 0.4f)),
                        )
                    }
                }

                items(state.items, key = { it.id }) { card ->
                    CatalogCardView(card = card, onClick = { onOpenDetail(card.id) })
                }

                if (state.error != null) {
                    item(span = { GridItemSpan(maxLineSpan) }) {
                        FailurePanel(
                            failure = state.error!!,
                            onRetry = if (state.page > 1) viewModel::loadMore else viewModel::refresh,
                        )
                    }
                }

                if (state.loadingMore) {
                    item(span = { GridItemSpan(maxLineSpan) }) {
                        LoadingPanel("正在加载下一页…")
                    }
                } else if (!state.endReached && state.items.isNotEmpty()) {
                    item(span = { GridItemSpan(maxLineSpan) }) {
                        SecondaryButton(
                            text = "加载更多",
                            onClick = viewModel::loadMore,
                            modifier = Modifier.fillMaxWidth(),
                        )
                    }
                } else {
                    item(span = { GridItemSpan(maxLineSpan) }) {
                        Text(
                            text = "— 已到馆藏末页 —",
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 14.dp),
                            style = MaterialTheme.typography.labelSmall,
                            color = ParchmentDim,
                            textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun CatalogCardView(card: CatalogCard, onClick: () -> Unit) {
    Surface(
        onClick = onClick,
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(9.dp),
        color = Parchment,
        shadowElevation = 2.dp,
        border = BorderStroke(1.dp, BrassDim.copy(alpha = 0.9f)),
    ) {
        Column {
            Box(
                Modifier
                    .fillMaxWidth()
                    .aspectRatio(3f / 4f),
            ) {
                RemoteImage(
                    check = card.cover,
                    contentDescription = card.title,
                    modifier = Modifier.fillMaxSize(),
                    contentScale = ContentScale.Crop,
                )
                if (card.badge.isNotBlank()) {
                    Surface(
                        modifier = Modifier
                            .align(Alignment.TopEnd)
                            .padding(6.dp),
                        shape = RoundedCornerShape(3.dp),
                        color = InkGreen.copy(alpha = 0.94f),
                    ) {
                        Text(
                            text = card.badge,
                            modifier = Modifier.padding(horizontal = 5.dp, vertical = 2.dp),
                            style = MaterialTheme.typography.labelSmall,
                            color = Parchment,
                        )
                    }
                }
            }
            Box(
                Modifier
                    .fillMaxWidth()
                    .height(1.dp)
                    .background(BrassDim.copy(alpha = 0.8f)),
            )
            Column(Modifier.padding(horizontal = 9.dp, vertical = 8.dp)) {
                Text(
                    text = card.title,
                    style = MaterialTheme.typography.titleSmall,
                    color = Ink,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                )
                if (card.category.isNotBlank()) {
                    Text(
                        text = card.category,
                        style = MaterialTheme.typography.labelSmall,
                        color = ParchmentDeep,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
                if (card.metaLine.isNotBlank()) {
                    Text(
                        text = card.metaLine,
                        style = MaterialTheme.typography.bodySmall,
                        color = ParchmentDeep,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
                if (card.tags.isNotEmpty()) {
                    Spacer(Modifier.height(5.dp))
                    TagChips(tags = card.tags, maxShown = 3)
                }
            }
        }
    }
}
