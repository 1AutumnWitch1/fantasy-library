package com.fantasylibrary.android.ui.screens.personal

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.fantasylibrary.android.data.network.*
import com.fantasylibrary.android.di.AppContainer
import com.fantasylibrary.android.ui.components.*
import com.fantasylibrary.android.ui.theme.BrassBright
import kotlinx.coroutines.launch

private enum class Section(val title: String) {
    Anime("我的追番"), Games("游戏收藏"), Wishlist("游戏愿望单"), History("观看记录"), Inbox("消息")
}

fun watchStatusLabel(status: String?) = when (status) {
    "PLAN_TO_WATCH" -> "想看"
    "WATCHING" -> "在看"
    "COMPLETED" -> "已看"
    "ON_HOLD" -> "搁置"
    else -> null
}

@Composable
fun ProfileScreen(container: AppContainer, onBack: () -> Unit, onLogin: () -> Unit,
    onSettings: () -> Unit, onWork: (Long) -> Unit, onPost: (Long) -> Unit, openHistory: Boolean = false,
) {
    val settings by container.settingsRepository.settings.collectAsStateWithLifecycle(initialValue = null)
    val session by container.authRepository.session.collectAsStateWithLifecycle()
    val base = settings?.baseUrl.orEmpty()
    val cleartext = container.apiClient.allowCleartext
    val repo = container.readerRepository
    val scope = rememberCoroutineScope()
    var section by rememberSaveable { mutableStateOf(if (openHistory) Section.History else Section.Anime) }
    var retry by remember { mutableIntStateOf(0) }
    var busy by remember { mutableStateOf(false) }
    var message by remember { mutableStateOf<String?>(null) }
    val profile by produceState<Outcome<ProfileDto>?>(null, base, session.signedIn, retry) {
        value = if (base.isNotBlank() && session.signedIn) container.authRepository.loadProfile(base) else null
        val failure = (value as? Outcome.Err)?.failure
        if (failure?.kind == FailureKind.UNAUTHORIZED) container.authRepository.clearLocal()
    }
    val collection by produceState<Outcome<List<SavedWorkDto>>?>(null, base, session.signedIn, retry) {
        value = if (base.isNotBlank() && session.signedIn) repo.collection(base) else null
    }

    ScrollRoom("个人书架", onBack, actions = {
        TextButton(onClick = onSettings, colors = ButtonDefaults.textButtonColors(contentColor = BrassBright)) { Text("设置") }
    }) {
        if (!session.signedIn) {
            Column(Modifier.fillMaxWidth().padding(16.dp)) {
                ScrollHeading("私人藏书室")
                PageStatus(message ?: "登录后即可同步追番进度、收藏、愿望单和馆内消息。",
                    actionLabel = "登录图书馆", action = onLogin)
            }
            return@ScrollRoom
        }

        val person = (profile as? Outcome.Ok)?.value
        val saved = (collection as? Outcome.Ok)?.value
        val name = person?.displayName?.ifBlank { session.username } ?: session.displayName.ifBlank { session.username }
        PaperTile(Modifier.fillMaxWidth().padding(start = 16.dp, end = 16.dp, top = 15.dp)) {
            Row(Modifier.fillMaxWidth().padding(13.dp), verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(11.dp)) {
                ReaderAvatar(person?.avatarKey, base, cleartext, name, Modifier.size(54.dp))
                Column(Modifier.weight(1f)) {
                    Text(name, color = PageInk, style = MaterialTheme.typography.titleLarge, maxLines = 1, overflow = TextOverflow.Ellipsis)
                    Text(person?.bio?.takeIf { it.isNotBlank() } ?: "@${person?.username ?: session.username}", color = PageMuted,
                        style = MaterialTheme.typography.bodyMedium, maxLines = 2, overflow = TextOverflow.Ellipsis)
                }
                PageButton("退出", enabled = !busy, onClick = {
                    busy = true
                    scope.launch {
                        try {
                            val result = container.authRepository.logout(base)
                            if (result is Outcome.Err) message = "本机已退出，服务器暂未确认。"
                        } finally { busy = false }
                    }
                })
            }
            if (saved != null) Row(Modifier.fillMaxWidth().padding(start = 14.dp, end = 14.dp, bottom = 12.dp),
                horizontalArrangement = Arrangement.spacedBy(13.dp)) {
                Text("${saved.count { it.workType == "ANIME" }} 部追番", color = PageMuted, style = MaterialTheme.typography.labelMedium)
                Text("${saved.count { it.workType == "GAME" && it.favorite == 1 }} 游戏收藏", color = PageMuted, style = MaterialTheme.typography.labelMedium)
                Text("${saved.count { it.workType == "GAME" && it.wishlist == 1 }} 愿望单", color = PageMuted, style = MaterialTheme.typography.labelMedium)
            }
        }
        if (profile is Outcome.Err) PageButton("重试读取个人资料", { retry++ }, enabled = !busy)
        LazyRow(contentPadding = PaddingValues(horizontal = 16.dp), horizontalArrangement = Arrangement.spacedBy(7.dp)) {
            items(Section.entries, key = { it.name }) { choice -> PageChip(choice.title, section == choice) { section = choice } }
        }
        key(section) {
            when (section) {
                Section.History -> HistoryList(container, base, cleartext, onWork)
                Section.Inbox -> InboxList(container, base, onWork, onPost)
                else -> when (val result = collection) {
                    null -> PageStatus("正在取出私人藏书…", busy = true)
                    is Outcome.Err -> PageStatus(result.failure.readerMessage(), action = { retry++ })
                    is Outcome.Ok -> {
                        val entries = result.value.filter {
                            when (section) {
                                Section.Anime -> it.workType == "ANIME"
                                Section.Games -> it.workType == "GAME" && it.favorite == 1
                                Section.Wishlist -> it.workType == "GAME" && it.wishlist == 1
                                else -> false
                            }
                        }
                        if (entries.isEmpty()) PageStatus("这一卷暂时没有收藏。")
                        else LazyColumn(Modifier.fillMaxSize(), state = rememberLazyListState(), contentPadding = PaddingValues(16.dp),
                            verticalArrangement = Arrangement.spacedBy(9.dp)) {
                            items(entries, key = { it.workId }) { item ->
                                val meta = if (item.workType == "ANIME")
                                    listOfNotNull(watchStatusLabel(item.watchStatus), progressLabel(item.watchedEpisodes, item.episodeCount)).joinToString(" · ")
                                else if (section == Section.Wishlist) "愿望单" else "已收藏"
                                PersonalEntry(item.title, item.coverUrl, meta, base, cleartext) { onWork(item.workId) }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun HistoryList(container: AppContainer, base: String, cleartext: Boolean, onWork: (Long) -> Unit) {
    var page by remember(base) { mutableIntStateOf(1) }
    var retry by remember(base) { mutableIntStateOf(0) }
    var records by remember(base) { mutableStateOf<List<WatchEventDto>>(emptyList()) }
    var more by remember(base) { mutableStateOf(false) }
    var error by remember(base) { mutableStateOf<String?>(null) }
    var busy by remember(base) { mutableStateOf(true) }
    LaunchedEffect(base, page, retry) {
        if (base.isBlank()) return@LaunchedEffect
        busy = true
        error = null
        when (val result = container.readerRepository.history(base, page)) {
            is Outcome.Ok -> {
                records = (records + result.value.items).distinctBy { it.id }
                more = page < result.value.totalPages
            }
            is Outcome.Err -> error = result.failure.readerMessage()
        }
        busy = false
    }
    LazyColumn(Modifier.fillMaxSize(), state = rememberLazyListState(), contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(9.dp)) {
        items(records, key = { it.id }) { record ->
            val meta = listOfNotNull(watchStatusLabel(record.watchStatus), progressLabel(record.watchedEpisodes, record.episodeCount),
                record.recordedAt.take(10)).joinToString(" · ")
            PersonalEntry(record.title, record.coverUrl, meta, base, cleartext) { onWork(record.workId) }
        }
        when {
            error != null -> item { PageStatus(error.orEmpty(), action = { retry++ }) }
            busy -> item { PageStatus("正在读取…", busy = true) }
            records.isEmpty() -> item { PageStatus("暂时没有观看记录。") }
            more -> item { PageButton("加载更多", { page++ }) }
        }
    }
}

@Composable
private fun InboxList(container: AppContainer, base: String, onWork: (Long) -> Unit, onPost: (Long) -> Unit) {
    var page by remember(base) { mutableIntStateOf(1) }
    var retry by remember(base) { mutableIntStateOf(0) }
    var notices by remember(base) { mutableStateOf<List<NoticeDto>>(emptyList()) }
    var more by remember(base) { mutableStateOf(false) }
    var unread by remember(base) { mutableLongStateOf(0L) }
    var error by remember(base) { mutableStateOf<String?>(null) }
    var busy by remember(base) { mutableStateOf(true) }
    val scope = rememberCoroutineScope()
    LaunchedEffect(base, page, retry) {
        if (base.isBlank()) return@LaunchedEffect
        busy = true
        error = null
        when (val result = container.readerRepository.inbox(base, page)) {
            is Outcome.Ok -> {
                val incoming = result.value.page.items
                val updatedIds = incoming.map { it.id }.toSet()
                notices = if (page == 1) incoming else notices.filter { it.id !in updatedIds } + incoming
                more = page < result.value.page.totalPages
                unread = result.value.unread
            }
            is Outcome.Err -> error = result.failure.readerMessage()
        }
        busy = false
    }
    LazyColumn(Modifier.fillMaxSize(), state = rememberLazyListState(), contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(9.dp)) {
        if (unread > 0) item { ScrollHeading("$unread 条未读消息") }
        items(notices, key = { it.id }) { notice ->
            PaperTile(Modifier.fillMaxWidth()) {
                Column(Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(5.dp)) {
                    Text(notice.actorName, color = PageMuted, style = MaterialTheme.typography.labelMedium)
                    Text(notice.postTitle, color = PageInk, style = MaterialTheme.typography.titleSmall)
                    notice.excerpt?.let { Text(it, color = PageInk, maxLines = 3, overflow = TextOverflow.Ellipsis) }
                    Text(notice.createdAt.take(10), color = PageMuted, style = MaterialTheme.typography.labelSmall)
                    Row {
                        if (notice.screeningWorkId != null || notice.postId > 0) PageButton("查看原文", {
                            if (notice.screeningWorkId != null) onWork(notice.screeningWorkId) else onPost(notice.postId)
                        })
                        if (!notice.read) PageButton("标记已读", enabled = !busy, onClick = {
                            busy = true
                            scope.launch {
                                try {
                                    when (val result = container.readerRepository.markRead(base, notice.id)) {
                                        is Outcome.Ok -> {
                                            notices = notices.map { if (it.id == notice.id) it.copy(read = true) else it }
                                            unread = (unread - 1).coerceAtLeast(0)
                                        }
                                        is Outcome.Err -> error = result.failure.readerMessage()
                                    }
                                } finally { busy = false }
                            }
                        })
                    }
                }
            }
        }
        when {
            error != null -> item { PageStatus(error.orEmpty(), action = { retry++ }) }
            busy -> item { PageStatus("正在读取…", busy = true) }
            notices.isEmpty() -> item { PageStatus("暂时没有馆内消息。") }
            more -> item { PageButton("加载更多", { page++ }) }
        }
    }
}

@Composable
private fun PersonalEntry(title: String, cover: String?, meta: String, base: String, cleartext: Boolean, open: () -> Unit) {
    PaperTile(Modifier.fillMaxWidth(), open) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            CoverArt(MediaUrls.resolve(cover, base, cleartext), null, Modifier.width(62.dp).height(76.dp))
            Column(Modifier.weight(1f).padding(11.dp), verticalArrangement = Arrangement.spacedBy(5.dp)) {
                Text(title, color = PageInk, style = MaterialTheme.typography.titleSmall, maxLines = 2, overflow = TextOverflow.Ellipsis)
                Text(meta, color = PageMuted, style = MaterialTheme.typography.bodySmall)
            }
        }
    }
}

private fun progressLabel(watched: Int, total: Int?) = if (total != null) "$watched / $total 集" else "已记录 $watched 集"
