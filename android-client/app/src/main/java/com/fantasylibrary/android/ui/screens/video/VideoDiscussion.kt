package com.fantasylibrary.android.ui.screens.video

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.fantasylibrary.android.data.network.ScreeningCommentDto
import com.fantasylibrary.android.ui.components.*

@Composable
fun VideoDiscussion(viewModel: VideoDiscussionViewModel, state: VideoDiscussionViewModel.State,
    work: Long, base: String, cleartext: Boolean, signedIn: Boolean, list: LazyListState, onLogin: () -> Unit,
) {
    var editor by rememberSaveable(work) { mutableStateOf(false) }
    var parent by rememberSaveable(work) { mutableStateOf<Long?>(null) }
    var replyName by rememberSaveable(work) { mutableStateOf("") }
    var draft by rememberSaveable(work) { mutableStateOf("") }
    LaunchedEffect(base, work, signedIn) { viewModel.load(base, work, signedIn) }

    val reply: (ScreeningCommentDto?) -> Unit = { target ->
        if (!signedIn) onLogin()
        else {
            viewModel.clearError()
            parent = target?.id
            replyName = target?.authorName.orEmpty()
            editor = true
        }
    }
    val like: (ScreeningCommentDto) -> Unit = { if (signedIn) viewModel.like(it) else onLogin() }

    Column(Modifier.fillMaxSize()) {
        LazyColumn(Modifier.weight(1f).fillMaxWidth(), state = list, contentPadding = PaddingValues(14.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)) {
            item {
                ScrollHeading("番剧评论" + (state.total?.let { " · $it" } ?: ""),
                    "按最新排序 · 同一番剧的各集共享讨论")
            }
            items(state.threads, key = { it.comment.id }) { thread ->
                val root = thread.comment.id
                val loaded = state.replies[root]
                val replies = loaded?.items?.takeIf { it.isNotEmpty() } ?: thread.replies
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    CommentLine(thread.comment, base, cleartext, thread.comment.id in state.likesPending, reply, like)
                    if (replies.isNotEmpty() || thread.comment.replyCount > 0) {
                        Column(Modifier.padding(start = 39.dp).fillMaxWidth()
                            .background(PageCard, RoundedCornerShape(8.dp)).padding(8.dp)) {
                            replies.forEach { comment ->
                                CommentLine(comment, base, cleartext, comment.id in state.likesPending, reply, like)
                            }
                            if (loaded?.loading == true) Text("正在加载回复…", color = PageMuted)
                            else if (loaded?.error != null) PageButton("重试读取回复", { viewModel.moreReplies(root) })
                            else if (loaded?.more == true || (loaded == null && thread.comment.replyCount > replies.size)) {
                                PageButton("查看回复 · ${thread.comment.replyCount}", { viewModel.moreReplies(root) })
                            }
                        }
                    }
                    HorizontalDivider(color = PageLine.copy(alpha = .34f))
                }
            }
            if (state.error != null) item { PageStatus(state.error.readerMessage(), action = viewModel::loadPage) }
            else if (state.loading) item { PageStatus("正在翻阅评论…", busy = true) }
            else if (state.total == 0L) item { PageStatus("暂时没有评论。") }
            else if (state.page > 0 && state.more) item { PageButton("加载更多评论", viewModel::loadPage) }
            if (!editor && state.writeError != null) item { PageStatus(state.writeError.readerMessage()) }
        }
        Row(Modifier.fillMaxWidth().background(PageCard).padding(horizontal = 12.dp, vertical = 2.dp),
            verticalAlignment = Alignment.CenterVertically) {
            Surface(onClick = { reply(null) }, modifier = Modifier.fillMaxWidth().padding(vertical = 5.dp),
                color = PagePaper, border = BorderStroke(1.dp, PageLine), shape = RoundedCornerShape(24.dp)) {
                Box(Modifier.heightIn(min = 46.dp).padding(horizontal = 14.dp), contentAlignment = Alignment.CenterStart) {
                    Text(if (signedIn) "写下你的评论" else "登录后发表评论", color = PageMuted,
                        style = MaterialTheme.typography.bodyMedium)
                }
            }
        }
    }

    if (editor && signedIn) {
        AlertDialog(
            onDismissRequest = { if (!state.sending) editor = false },
            containerColor = PagePaper,
            titleContentColor = PageInk,
            title = { Text(if (replyName.isBlank()) "发表评论" else "回复 $replyName") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(7.dp)) {
                    PageField(draft, { draft = it.take(900) }, "评论内容", Modifier.fillMaxWidth(), singleLine = false)
                    state.writeError?.let { Text(it.readerMessage(), color = PageMuted) }
                }
            },
            confirmButton = {
                PageButton(if (state.sending) "发送中…" else "发送", enabled = draft.isNotBlank() && !state.sending, emphasized = true,
                    onClick = { viewModel.post(draft, parent) { draft = ""; editor = false } })
            },
            dismissButton = { PageButton("取消", enabled = !state.sending, onClick = { editor = false }) },
        )
    }
}

@Composable
private fun CommentLine(comment: ScreeningCommentDto, base: String, cleartext: Boolean, pending: Boolean,
    onReply: (ScreeningCommentDto) -> Unit, onLike: (ScreeningCommentDto) -> Unit,
) {
    Row(Modifier.fillMaxWidth().padding(vertical = 5.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        val name = comment.authorName.orEmpty()
        ReaderAvatar(comment.avatarKey, base, cleartext, name, Modifier.size(31.dp))
        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(3.dp)) {
            Text(name.ifBlank { if (comment.removed) "已移除的评论" else "馆内读者" }, color = PageMuted,
                style = MaterialTheme.typography.titleSmall)
            comment.replyTo?.takeIf { it.isNotBlank() }?.let {
                Text("回复 $it", color = PageMuted, style = MaterialTheme.typography.labelSmall)
            }
            Text(comment.body ?: "该评论已移除。", color = PageInk, style = MaterialTheme.typography.bodyMedium)
            Text(comment.createdAt.take(16).replace('T', ' '), color = PageMuted, style = MaterialTheme.typography.labelSmall)
            if (!comment.removed) Row(verticalAlignment = Alignment.CenterVertically) {
                PageButton("回复", { onReply(comment) })
                Spacer(Modifier.weight(1f))
                PageButton((if (comment.liked) "已赞 · " else "点赞 · ") + comment.likeCount, { onLike(comment) }, enabled = !pending)
            }
        }
    }
}
