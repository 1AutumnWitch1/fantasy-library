package com.fantasylibrary.android.ui.screens.detail

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.fantasylibrary.android.data.network.*
import com.fantasylibrary.android.di.AppContainer
import com.fantasylibrary.android.ui.components.*
import com.fantasylibrary.android.ui.screens.personal.watchStatusLabel
import kotlinx.coroutines.launch

@Composable
fun CollectionActions(container: AppContainer, base: String, detail: WorkDetailDto, onLogin: () -> Unit) {
    val session by container.authRepository.session.collectAsStateWithLifecycle()
    val repo = container.readerRepository
    val scope = rememberCoroutineScope()
    val work = detail.work
    var entry by remember(work.id, session.signedIn, base) { mutableStateOf<SavedWorkDto?>(null) }
    var favorite by remember(work.id, session.signedIn, base) { mutableStateOf(false) }
    var wishlist by remember(work.id, session.signedIn, base) { mutableStateOf(false) }
    var status by remember(work.id, session.signedIn, base) { mutableStateOf<String?>(null) }
    var watched by remember(work.id, session.signedIn, base) { mutableStateOf("0") }
    var busy by remember { mutableStateOf(false) }
    var ready by remember(work.id, session.signedIn, base) { mutableStateOf(false) }
    var message by remember { mutableStateOf<String?>(null) }
    var retry by remember { mutableIntStateOf(0) }
    val isAnime = work.type == "ANIME"

    fun show(saved: SavedWorkDto?) {
        entry = saved
        favorite = saved?.favorite == 1
        wishlist = saved?.wishlist == 1
        status = saved?.watchStatus
        watched = (saved?.watchedEpisodes ?: 0).toString()
    }

    LaunchedEffect(work.id, base, session.signedIn, retry) {
        if (session.signedIn && base.isNotBlank()) {
            busy = true
            message = null
            try {
                when (val result = repo.saved(base, work.id)) {
                    is Outcome.Ok -> { show(result.value.item); ready = true }
                    is Outcome.Err -> {
                        message = result.failure.readerMessage()
                        if (result.failure.kind == FailureKind.UNAUTHORIZED) container.authRepository.clearLocal()
                    }
                }
            } finally { busy = false }
        }
    }
    PaperTile(Modifier.fillMaxWidth()) {
        Column(Modifier.padding(13.dp), verticalArrangement = Arrangement.spacedBy(5.dp)) {
            ScrollHeading(if (isAnime) "追番记录" else "游戏收藏")
            if (!session.signedIn) {
                PageButton("登录后加入私人书架", onLogin)
                return@Column
            }
            if (!ready) {
                if (busy) PageStatus("正在查找收藏…", busy = true)
                else PageStatus(message ?: "暂时无法读取收藏。", action = { retry++ })
                return@Column
            }
            Row(horizontalArrangement = Arrangement.spacedBy(7.dp)) {
                PageChip("收藏", favorite) { if (!busy) favorite = !favorite }
                if (!isAnime) PageChip("愿望单", wishlist) { if (!busy) wishlist = !wishlist }
            }
            if (isAnime) {
                val options = listOf("PLAN_TO_WATCH", "WATCHING", "COMPLETED", "ON_HOLD")
                LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    items(options) { option -> PageChip(watchStatusLabel(option).orEmpty(), status == option) {
                        if (!busy) status = if (status == option) null else option
                    } }
                }
                PageField(watched, { if (!busy) watched = it.filter { char -> char in '0'..'9' }.take(5) },
                    "已看集数", Modifier.fillMaxWidth())
            }
            Row(horizontalArrangement = Arrangement.spacedBy(5.dp)) {
                val hasSelection = favorite || wishlist || status != null || (watched.toIntOrNull() ?: 0) > 0
                PageButton("保存", enabled = !busy && hasSelection, emphasized = true, onClick = {
                    val count = watched.toIntOrNull() ?: 0
                    val max = detail.anime?.episodeCount
                    if (isAnime && max != null && count > max) message = "集数不能超过 $max 集。"
                    else {
                        busy = true
                        scope.launch {
                            try {
                                val form = SaveWorkDto(favorite, wishlist, status ?: if (isAnime && count > 0) "WATCHING" else null, count)
                                when (val result = repo.save(base, work.id, form)) {
                                    is Outcome.Ok -> { show(result.value); message = "已保存到私人书架。" }
                                    is Outcome.Err -> message = result.failure.readerMessage()
                                }
                            } finally { busy = false }
                        }
                    }
                })
                if (entry != null) PageButton("移除记录", enabled = !busy, onClick = {
                    busy = true
                    scope.launch {
                        try {
                            when (val result = repo.remove(base, work.id)) {
                                is Outcome.Ok -> { show(null); message = "已移除记录。" }
                                is Outcome.Err -> message = result.failure.readerMessage()
                            }
                        } finally { busy = false }
                    }
                })
            }
            message?.let { Text(it, color = PageMuted, style = MaterialTheme.typography.bodySmall) }
        }
    }
}

@Composable
fun DownloadActions(container: AppContainer, base: String, workId: Long) {
    var retry by remember { mutableIntStateOf(0) }
    val nodes by produceState<Outcome<List<DownloadNodeDto>>?>(null, base, workId, retry) {
        if (base.isNotBlank()) value = container.readerRepository.downloads(base, workId)
    }
    val uri = LocalUriHandler.current
    var error by remember { mutableStateOf<String?>(null) }
    PaperTile(Modifier.fillMaxWidth()) {
        Column(Modifier.padding(13.dp), verticalArrangement = Arrangement.spacedBy(5.dp)) {
            ScrollHeading("下载来源")
            when (val result = nodes) {
                null -> PageStatus("正在读取节点…", busy = true)
                is Outcome.Err -> PageStatus(result.failure.readerMessage(), action = { retry++ })
                is Outcome.Ok -> {
                    if (result.value.isEmpty()) Text("该作品暂未提供下载。", color = PageMuted)
                    result.value.forEach { node ->
                        val link = MediaUrls.resolve(node.url, base, container.apiClient.allowCleartext).resolved
                        if (link != null) PageButton(listOfNotNull(node.label, node.platform?.takeIf { it.isNotBlank() }).joinToString(" · "), {
                            try { uri.openUri(link) } catch (_: IllegalArgumentException) { error = "当前设备无法打开这个下载来源。" }
                        })
                    }
                }
            }
            error?.let { Text(it, color = PageMuted) }
        }
    }
}
