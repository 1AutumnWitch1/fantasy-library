package com.fantasylibrary.android.ui.screens.community

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.fantasylibrary.android.data.network.*
import com.fantasylibrary.android.di.AppContainer
import com.fantasylibrary.android.ui.components.*
import kotlinx.coroutines.launch

@Composable
fun PostScreen(id: Long, container: AppContainer, onBack: () -> Unit, onLogin: () -> Unit, onWork: (Long) -> Unit) {
    val settings by container.settingsRepository.settings.collectAsStateWithLifecycle(initialValue = null)
    val session by container.authRepository.session.collectAsStateWithLifecycle()
    val base = settings?.baseUrl.orEmpty()
    val cleartext = container.apiClient.allowCleartext
    val repo = container.readerRepository
    val scope = rememberCoroutineScope()
    var reload by remember { mutableIntStateOf(0) }
    var viewed by rememberSaveable(id) { mutableStateOf(false) }
    var detail by remember(base, id) { mutableStateOf<Outcome<PostDetailDto>?>(null) }
    var comments by remember(base, id) { mutableStateOf<List<CommentDto>>(emptyList()) }
    var nextPage by remember(base, id) { mutableIntStateOf(1) }
    var more by remember(base, id) { mutableStateOf(false) }
    var commentError by remember(base, id) { mutableStateOf<String?>(null) }
    var reaction by remember(base, id, session.signedIn) { mutableStateOf<ReactionDto?>(null) }
    var text by rememberSaveable(id) { mutableStateOf("") }
    var replyTo by remember { mutableStateOf<CommentDto?>(null) }
    var writing by remember { mutableStateOf(false) }
    var paging by remember { mutableStateOf(false) }

    suspend fun loadComments(page: Int) {
        commentError = null
        when (val result = repo.comments(base, id, page)) {
            is Outcome.Ok -> {
                comments = if (page == 1) result.value.items else (comments + result.value.items).distinctBy { it.id }
                nextPage = page + 1
                more = page < result.value.totalPages
            }
            is Outcome.Err -> commentError = result.failure.readerMessage()
        }
    }

    LaunchedEffect(base, id, reload) {
        if (base.isBlank()) return@LaunchedEffect
        detail = repo.post(base, id, !viewed)
        if (detail is Outcome.Ok) {
            viewed = true
            loadComments(1)
        }
    }
    LaunchedEffect(base, id, session.signedIn, reload) {
        if (base.isNotBlank() && session.signedIn) {
            reaction = (repo.postReaction(base, id) as? Outcome.Ok)?.value
        }
    }

    ScrollRoom("讨论手稿", onBack, backLabel = "返回") {
        val post = (detail as? Outcome.Ok)?.value
        when {
            post == null && detail is Outcome.Err -> PageStatus((detail as Outcome.Err).failure.readerMessage(), action = { reload++ })
            post == null && settings != null && base.isBlank() -> PageStatus("请先填写服务器地址。")
            post == null -> PageStatus("正在打开手稿…", busy = true)
            else -> LazyColumn(Modifier.fillMaxSize().imePadding(), contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(13.dp)) {
                item {
                    ScrollHeading(post.post.title)
                    Row(Modifier.padding(top = 10.dp), horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically) {
                        ReaderAvatar(post.post.avatarKey, base, cleartext, post.post.authorName)
                        Column {
                            Text(post.post.authorName, color = PageInk, style = MaterialTheme.typography.titleSmall)
                            Text(post.post.createdAt.take(10), color = PageMuted, style = MaterialTheme.typography.labelSmall)
                        }
                    }
                }
                item { Text(post.post.body, color = PageInk, style = MaterialTheme.typography.bodyLarge) }
                items(post.images.distinct(), key = { it }) { image ->
                    CoverArt(communityImage(image, base, cleartext), "帖子配图", Modifier.fillMaxWidth().aspectRatio(1.3f))
                }
                post.post.workId?.let { work -> item { PageButton("关联作品 · ${post.post.workTitle.orEmpty()}", { onWork(work) }) } }
                item {
                    PageButton(if (reaction?.liked == true) "已点赞 · ${reaction?.count}" else "点赞 · ${reaction?.count ?: post.post.likeCount}",
                        enabled = !writing, onClick = {
                            if (!session.signedIn) onLogin()
                            else {
                                writing = true
                                scope.launch {
                                    try {
                                        when (val result = repo.react(base, id, reaction?.liked != true)) {
                                            is Outcome.Ok -> reaction = result.value
                                            is Outcome.Err -> commentError = result.failure.readerMessage()
                                        }
                                    } finally { writing = false }
                                }
                            }
                        })
                    ScrollHeading("评论")
                    if (!session.signedIn) PageButton("登录后发表评论", onLogin)
                    else {
                        replyTo?.let { PageButton("正在回复 ${it.authorName} · 取消", { replyTo = null }) }
                        PageField(text, { text = it.take(900) }, "写下你的评论", Modifier.fillMaxWidth(), singleLine = false)
                        PageButton("发送评论", enabled = !writing && text.isNotBlank(), emphasized = true, onClick = {
                            writing = true
                            scope.launch {
                                try {
                                    when (val result = repo.comment(base, id, CommentDraft(text.trim(), replyTo?.id))) {
                                        is Outcome.Ok -> { text = ""; replyTo = null; loadComments(1) }
                                        is Outcome.Err -> commentError = result.failure.readerMessage()
                                    }
                                } finally { writing = false }
                            }
                        })
                    }
                }
                if (comments.isEmpty() && commentError == null) item { PageStatus("暂无评论。") }
                items(comments, key = { it.id }) { comment ->
                    PaperTile(Modifier.fillMaxWidth()) {
                        Column(Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                            Row(horizontalArrangement = Arrangement.spacedBy(7.dp), verticalAlignment = Alignment.CenterVertically) {
                                ReaderAvatar(comment.avatarKey, base, cleartext, comment.authorName, Modifier.size(26.dp))
                                Text(comment.authorName, color = PageInk, style = MaterialTheme.typography.titleSmall)
                            }
                            comment.replyTo?.let { Text("回复 $it", color = PageMuted, style = MaterialTheme.typography.labelSmall) }
                            Text(comment.body, color = PageInk)
                            PageButton("回复", { if (session.signedIn) replyTo = comment else onLogin() })
                        }
                    }
                }
                if (commentError != null) item { PageStatus(commentError.orEmpty()) }
                if (more || commentError != null) item {
                    PageButton(if (commentError == null) "更多评论" else "重新读取评论", enabled = !paging, onClick = {
                        paging = true
                        scope.launch { try { loadComments(if (comments.isEmpty()) 1 else nextPage) } finally { paging = false } }
                    })
                }
            }
        }
    }
}
