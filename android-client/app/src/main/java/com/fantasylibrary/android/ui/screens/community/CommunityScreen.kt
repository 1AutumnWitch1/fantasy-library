package com.fantasylibrary.android.ui.screens.community

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.staggeredgrid.LazyVerticalStaggeredGrid
import androidx.compose.foundation.lazy.staggeredgrid.StaggeredGridCells
import androidx.compose.foundation.lazy.staggeredgrid.StaggeredGridItemSpan
import androidx.compose.foundation.lazy.staggeredgrid.items
import androidx.compose.foundation.lazy.staggeredgrid.rememberLazyStaggeredGridState
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.Alignment
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.fantasylibrary.android.data.network.MediaUrls
import com.fantasylibrary.android.data.network.PostDto
import com.fantasylibrary.android.data.network.Topic
import com.fantasylibrary.android.ui.components.*
import com.fantasylibrary.android.ui.theme.BrassBright

@Composable
fun CommunityScreen(viewModel: CommunityViewModel, cleartext: Boolean, onBack: () -> Unit, onPost: (Long) -> Unit,
    onCompose: () -> Unit, onSettings: () -> Unit,
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val grid = rememberLazyStaggeredGridState()
    val failure = state.error
    ScrollRoom("社区", onBack, actions = {
        TextButton(onClick = onCompose, colors = ButtonDefaults.textButtonColors(contentColor = BrassBright)) { Text("撰写手稿") }
    }) {
        LazyRow(contentPadding = PaddingValues(horizontal = 16.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            item { PageChip("最新", !state.hot) { viewModel.select(state.topic, false) } }
            item { PageChip("热门", state.hot) { viewModel.select(state.topic, true) } }
        }
        LazyRow(contentPadding = PaddingValues(horizontal = 16.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            item { PageChip("全部话题", state.topic == null) { viewModel.select(null, state.hot) } }
            items(Topic.entries, key = { it.name }) { topic ->
                PageChip(topic.label, state.topic == topic) { viewModel.select(topic, state.hot) }
            }
        }
        LazyVerticalStaggeredGrid(StaggeredGridCells.Fixed(2), Modifier.fillMaxSize(),
            state = grid, contentPadding = PaddingValues(horizontal = 16.dp, vertical = 12.dp),
            verticalItemSpacing = 11.dp, horizontalArrangement = Arrangement.spacedBy(11.dp)) {
            item(span = StaggeredGridItemSpan.FullLine) {
                ScrollHeading("讨论档案")
            }
            items(state.items, key = { it.id }) { post ->
                PostPreview(post, state.base, cleartext, onClick = { onPost(post.id) })
            }
            if (state.items.isEmpty() || state.more || state.error != null) item(span = StaggeredGridItemSpan.FullLine) {
                when {
                    state.busy -> PageStatus("正在翻阅手稿…", busy = true)
                    state.base.isBlank() -> PageStatus("填写服务器地址即可浏览社区。", actionLabel = "连接设置", action = onSettings)
                    failure != null -> PageStatus(failure.readerMessage(), action = { viewModel.load(state.items.isEmpty()) })
                    state.items.isEmpty() -> PageStatus("这个话题还没有手稿。")
                    else -> Box(Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
                        PageButton("加载更多手稿", { viewModel.load(false) })
                    }
                }
            }
        }
    }
}

@Composable
fun PostPreview(post: PostDto, base: String, cleartext: Boolean, onClick: () -> Unit) {
    PaperTile(Modifier.fillMaxWidth(), onClick) {
        if (!post.previewImage.isNullOrBlank()) {
            CoverArt(communityImage(post.previewImage, base, cleartext), null,
                Modifier.fillMaxWidth().aspectRatio(if (post.id % 2L == 0L) 1.05f else .86f), "❖")
        }
        Column(Modifier.padding(11.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Text(Topic.entries.find { it.name == post.topic }?.label.orEmpty(), color = PageMuted, style = MaterialTheme.typography.labelSmall)
            Text(post.title, color = PageInk, maxLines = 3, overflow = TextOverflow.Ellipsis, style = MaterialTheme.typography.titleSmall)
            if (post.previewImage.isNullOrBlank()) Text(post.body, color = PageMuted, maxLines = 3, overflow = TextOverflow.Ellipsis,
                style = MaterialTheme.typography.bodySmall)
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(5.dp)) {
                ReaderAvatar(post.avatarKey, base, cleartext, post.authorName, Modifier.size(23.dp))
                Text(post.authorName, Modifier.weight(1f), color = PageInk, maxLines = 1, overflow = TextOverflow.Ellipsis,
                    style = MaterialTheme.typography.labelSmall)
                Text("♡ ${post.likeCount}", color = PageMuted, style = MaterialTheme.typography.labelSmall)
            }
            Text("${post.commentCount} 评论 · ${post.viewCount} 浏览", color = PageMuted, style = MaterialTheme.typography.labelSmall)
        }
    }
}

fun communityImage(key: String, base: String, cleartext: Boolean) =
    MediaUrls.resolve("/api/community/media/" + android.net.Uri.encode(key), base, cleartext)
