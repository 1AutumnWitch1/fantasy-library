package com.fantasylibrary.android.ui.screens.detail

import androidx.compose.foundation.layout.*
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.scale
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.fantasylibrary.android.data.network.*
import com.fantasylibrary.android.di.AppContainer
import com.fantasylibrary.android.ui.components.*
import com.fantasylibrary.android.ui.theme.*
import kotlinx.coroutines.launch

@Composable
fun CollectionActions(
    container: AppContainer, base: String, work: Long, isAnime: Boolean,
    onLogin: () -> Unit, onChanged: () -> Unit = {},
) {
    val session by container.authRepository.session.collectAsStateWithLifecycle()
    key(base, work, session.username) {
        val repo = container.readerRepository
        val scope = rememberCoroutineScope()
        var saved by remember { mutableStateOf<SavedWorkDto?>(null) }
        var loading by remember { mutableStateOf(session.signedIn) }
        var ready by remember { mutableStateOf(!session.signedIn) }
        var busy by remember { mutableStateOf(false) }
        var message by remember { mutableStateOf<Failure?>(null) }
        var retry by remember { mutableIntStateOf(0) }

        LaunchedEffect(session.signedIn, retry) {
            if (!session.signedIn || base.isBlank()) return@LaunchedEffect
            loading = true
            message = null
            when (val result = repo.saved(base, work)) {
                is Outcome.Ok -> { saved = result.value.item; ready = true }
                is Outcome.Err -> {
                    message = result.failure
                    if (result.failure.kind == FailureKind.UNAUTHORIZED) container.authRepository.clearLocal()
                }
            }
            loading = false
        }

        fun select(kind: CollectionSelection, selected: Boolean) {
            if (!session.signedIn) { onLogin(); return }
            if (busy || !ready) return
            busy = true
            message = null
            scope.launch {
                try {
                    when (val result = repo.selection(base, work, kind, selected)) {
                        is Outcome.Ok -> { saved = result.value.item; onChanged() }
                        is Outcome.Err -> {
                            message = result.failure
                            if (result.failure.kind == FailureKind.UNAUTHORIZED) {
                                container.authRepository.clearLocal()
                                onLogin()
                            }
                        }
                    }
                } finally { busy = false }
            }
        }

        val favorite = saved?.favorite == 1
        val wishlist = saved?.wishlist == 1
        val enabled = !loading && !busy && ready
        Column(verticalArrangement = Arrangement.spacedBy(3.dp)) {
            val row = if (isAnime) Modifier.fillMaxWidth() else Modifier.fillMaxWidth().height(IntrinsicSize.Min)
            Row(row, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                if (isAnime) {
                    RelationshipButton(if (favorite) "✓ 已追番" else "＋ 追番", favorite, enabled) {
                        select(CollectionSelection.FOLLOW, !favorite)
                    }
                } else {
                    RelationshipButton(if (favorite) "★ 已收藏" else "☆ 收藏", favorite, enabled, Modifier.weight(1f).fillMaxHeight()) {
                        select(CollectionSelection.FAVORITE, !favorite)
                    }
                    RelationshipButton(if (wishlist) "✓ 已加入愿望单" else "＋ 加入愿望单", wishlist, enabled, Modifier.weight(1f).fillMaxHeight()) {
                        select(CollectionSelection.WISHLIST, !wishlist)
                    }
                }
            }
            if (loading) Text("正在读取状态…", color = PageMuted, style = MaterialTheme.typography.labelSmall)
            message?.let {
                Text(it.readerMessage(), color = PageMuted, style = MaterialTheme.typography.bodySmall)
                if (!ready) PageButton("重试", { retry++ })
            }
        }
    }
}

@Composable
private fun RelationshipButton(
    label: String, active: Boolean, enabled: Boolean, modifier: Modifier = Modifier, onClick: () -> Unit,
) {
    val interaction = remember { MutableInteractionSource() }
    val pressed by interaction.collectIsPressedAsState()
    val scale by animateFloatAsState(if (pressed) .97f else 1f, label = "collection press")
    Button(
        onClick = onClick, enabled = enabled, interactionSource = interaction,
        modifier = modifier.heightIn(min = 48.dp).scale(scale).semantics { selected = active },
        contentPadding = PaddingValues(horizontal = 7.dp, vertical = 8.dp),
        shape = RoundedCornerShape(7.dp),
        colors = ButtonDefaults.buttonColors(
            containerColor = if (active) InkGreen else Brass,
            contentColor = if (active) Parchment else WalnutDeep,
        ),
    ) {
        Text(label, textAlign = TextAlign.Center, style = MaterialTheme.typography.labelMedium)
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
