package com.fantasylibrary.android.ui.screens.video

import android.app.Activity
import android.content.Context
import android.content.ContextWrapper
import android.content.pm.ActivityInfo
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.media3.common.MediaItem
import androidx.media3.common.PlaybackException
import androidx.media3.common.Player
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.ui.PlayerView
import com.fantasylibrary.android.data.network.MediaUrls
import com.fantasylibrary.android.ui.components.FailurePanel
import com.fantasylibrary.android.ui.components.KeyValueRow
import com.fantasylibrary.android.ui.components.LeatherPanel
import com.fantasylibrary.android.ui.components.LibraryTopBar
import com.fantasylibrary.android.ui.components.LinkAction
import com.fantasylibrary.android.ui.components.NotePanel
import com.fantasylibrary.android.ui.components.PrimaryButton
import com.fantasylibrary.android.ui.components.SectionTitle
import com.fantasylibrary.android.ui.components.SecondaryButton
import com.fantasylibrary.android.ui.components.StatusPill
import com.fantasylibrary.android.ui.components.Tone
import com.fantasylibrary.android.ui.components.libraryBackground
import com.fantasylibrary.android.ui.theme.Brass
import com.fantasylibrary.android.ui.theme.BrassDim
import com.fantasylibrary.android.ui.theme.DangerRose
import com.fantasylibrary.android.ui.theme.InkGreen
import com.fantasylibrary.android.ui.theme.Parchment
import com.fantasylibrary.android.ui.theme.ParchmentDeep
import com.fantasylibrary.android.ui.theme.ParchmentDim
import com.fantasylibrary.android.ui.theme.SuccessGreen
import com.fantasylibrary.android.ui.theme.Walnut
import com.fantasylibrary.android.ui.theme.WalnutDeep
import com.fantasylibrary.android.ui.theme.WalnutEdge
import com.fantasylibrary.android.ui.theme.WarnAmber
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive

@Composable
fun VideoScreen(
    viewModel: VideoViewModel,
    onOpenConfig: () -> Unit,
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val context = LocalContext.current
    val activity = remember(context) { context.findActivity() }

    val player = remember { ExoPlayer.Builder(context).build() }
    var isPlaying by remember { mutableStateOf(false) }
    var durationMs by remember { mutableLongStateOf(0L) }
    var positionMs by remember { mutableLongStateOf(0L) }
    var playbackError by remember { mutableStateOf<String?>(null) }
    var fullscreen by remember { mutableStateOf(false) }

    val mediaCheck = state.mediaCheck
    val mediaUrl = mediaCheck.resolved

    DisposableEffect(Unit) {
        onDispose { player.release() }
    }

    LaunchedEffect(mediaUrl) {
        playbackError = null
        if (mediaUrl == null) {
            player.stop()
            player.clearMediaItems()
        } else {
            player.setMediaItem(MediaItem.fromUri(mediaUrl))
            player.prepare()
            player.playWhenReady = true
        }
    }

    DisposableEffect(player) {
        val listener = object : Player.Listener {
            override fun onIsPlayingChanged(playing: Boolean) {
                isPlaying = playing
            }

            override fun onPlaybackStateChanged(playbackState: Int) {
                if (playbackState == Player.STATE_READY) {
                    durationMs = player.duration.coerceAtLeast(0L)
                    // 重新加载成功后自动消除上一次的错误遮罩
                    playbackError = null
                }
            }

            override fun onPlayerError(error: PlaybackException) {
                playbackError = describePlaybackError(error)
                isPlaying = false
            }
        }
        player.addListener(listener)
        onDispose { player.removeListener(listener) }
    }

    // 重新加载：清空错误状态并从头准备同一地址（用于解码/网络错误的恢复）
    val reloadMedia: () -> Unit = {
        playbackError = null
        durationMs = 0L
        positionMs = 0L
        val url = mediaUrl
        if (url != null) {
            player.stop()
            player.clearMediaItems()
            player.setMediaItem(MediaItem.fromUri(url))
            player.prepare()
            player.playWhenReady = true
        }
    }

    LaunchedEffect(player) {
        while (isActive) {
            positionMs = player.currentPosition.coerceAtLeast(0L)
            if (player.playbackState == Player.STATE_READY && player.duration > 0) {
                durationMs = player.duration
            }
            delay(350)
        }
    }

    DisposableEffect(fullscreen) {
        if (activity != null) {
            val window = activity.window
            val controller = WindowCompat.getInsetsController(window, window.decorView)
            if (fullscreen) {
                controller.systemBarsBehavior =
                    WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
                controller.hide(WindowInsetsCompat.Type.systemBars())
                activity.requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_SENSOR_LANDSCAPE
            } else {
                controller.show(WindowInsetsCompat.Type.systemBars())
                activity.requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_UNSPECIFIED
            }
        }
        onDispose {
            if (activity != null) {
                val window = activity.window
                WindowCompat.getInsetsController(window, window.decorView)
                    .show(WindowInsetsCompat.Type.systemBars())
                activity.requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_UNSPECIFIED
            }
        }
    }

    BackHandler(enabled = fullscreen) { fullscreen = false }

    if (fullscreen) {
        Box(Modifier.fillMaxSize().background(Color.Black)) {
            PlayerStage(
                player = player,
                hasMedia = mediaUrl != null,
                isPlaying = isPlaying,
                positionMs = positionMs,
                durationMs = durationMs,
                fullscreen = true,
                errorText = playbackError,
                onToggleFullscreen = { fullscreen = false },
                onSeek = { positionMs = it },
                onRetry = reloadMedia,
                modifier = Modifier.fillMaxSize(),
            )
        }
        return
    }

    Column(
        Modifier
            .fillMaxSize()
            .libraryBackground(),
    ) {
        LibraryTopBar(
            title = "放映室",
            subtitle = "ExoPlayer 播放 MinIO 视频 · 自动 Range 流式",
            actions = {
                LinkAction(
                    text = if (state.animeLoading) "读取中…" else "刷新片单",
                    onClick = viewModel::loadAnime,
                )
            },
        )

        Column(
            Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 14.dp, vertical = 12.dp),
        ) {
            PlayerStage(
                player = player,
                hasMedia = mediaUrl != null,
                isPlaying = isPlaying,
                positionMs = positionMs,
                durationMs = durationMs,
                fullscreen = false,
                errorText = playbackError,
                onToggleFullscreen = { fullscreen = true },
                onSeek = { positionMs = it },
                onRetry = reloadMedia,
                modifier = Modifier
                    .fillMaxWidth()
                    .aspectRatio(16f / 9f)
                    .clip(RoundedCornerShape(8.dp)),
            )

            if (!state.allowCleartext && mediaUrl != null && mediaUrl.startsWith("http://")) {
                Spacer(Modifier.height(6.dp))
                NotePanel(
                    text = "当前为 Release 构建，明文 HTTP 地址会被拒绝使用。",
                    accent = DangerRose,
                )
            }

            if (mediaUrl == null) {
                Spacer(Modifier.height(8.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    NotePanel(
                        text = mediaCheck.reason
                            ?: "请选择一部动漫、一集与一个播放源；地址来自后端 /api/works/{id}/episodes。",
                        accent = BrassDim,
                    )
                }
            }

            if (state.notice != null) {
                Spacer(Modifier.height(8.dp))
                NotePanel(text = state.notice.orEmpty(), accent = WarnAmber)
            }

            Spacer(Modifier.height(14.dp))
            SectionTitle("1 · 选择动漫", trailing = if (state.animeLoading) "读取中" else "${state.anime.size} 部")
            Spacer(Modifier.height(6.dp))
            when {
                state.animeError != null -> FailurePanel(
                    failure = state.animeError!!,
                    onRetry = { viewModel.loadAnime() },
                )
                state.anime.isEmpty() && !state.animeLoading -> NotePanel(
                    text = "没有读取到动漫列表，请先在「连接」页确认服务器地址。",
                    accent = BrassDim,
                )
                else -> LazyRow(
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                ) {
                    itemsIndexed(state.anime) { _, card ->
                        SelectChip(
                            text = card.title,
                            selected = card.id == state.selectedWorkId,
                            onClick = { viewModel.selectWork(card) },
                        )
                    }
                }
            }

            Spacer(Modifier.height(12.dp))
            SectionTitle(
                "2 · 选择分集",
                trailing = when {
                    state.episodesLoading -> "读取中"
                    state.selectedWorkId == null -> null
                    else -> "${state.episodes.size} 集"
                },
            )
            Spacer(Modifier.height(6.dp))
            when {
                state.episodesError != null -> FailurePanel(
                    failure = state.episodesError!!,
                    onRetry = { viewModel.loadAnime() },
                )
                state.episodes.isEmpty() -> NotePanel(
                    text = "该作品暂无分集数据（/api/works/{id}/episodes 返回空）。",
                    accent = BrassDim,
                )
                else -> LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    itemsIndexed(state.episodes) { index, episode ->
                        SelectChip(
                            text = buildString {
                                append("第 ${episode.number} 集")
                                episode.durationSeconds?.let { append(" · ${it}s") }
                                if (episode.sources.isEmpty()) append(" · 无源")
                            },
                            selected = index == state.episodeIndex,
                            onClick = { viewModel.selectEpisode(index) },
                        )
                    }
                }
            }

            Spacer(Modifier.height(12.dp))
            SectionTitle(
                "3 · 选择播放源",
                trailing = state.source?.let { "${it.quality ?: "?"} · ${it.format ?: "?"}" },
            )
            Spacer(Modifier.height(6.dp))
            val sources = state.episode?.sources.orEmpty()
            if (sources.isEmpty()) {
                NotePanel(text = "该分集没有可用的播放源。", accent = BrassDim)
            } else {
                LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    itemsIndexed(sources) { index, source ->
                        SelectChip(
                            text = (source.label ?: "源 ${index + 1}") +
                                (source.quality?.let { " · $it" } ?: ""),
                            selected = index == state.sourceIndex,
                            onClick = { viewModel.selectSource(index) },
                        )
                    }
                }
            }

            Spacer(Modifier.height(14.dp))
            SectionTitle("地址检查", trailing = "防止 localhost / Docker 内部名")
            Spacer(Modifier.height(6.dp))
            LeatherPanel(Modifier.fillMaxWidth()) {
                KeyValueRow("原始地址", state.source?.url ?: "—", mono = true)
                KeyValueRow("解析后地址", mediaCheck.resolved ?: "—", mono = true)
                KeyValueRow(
                    label = "判定",
                    value = verdictLabel(mediaCheck.verdict),
                    valueColor = when (mediaCheck.verdict) {
                        MediaUrls.Verdict.REJECTED -> DangerRose
                        MediaUrls.Verdict.PRESIGNED -> WarnAmber
                        else -> SuccessGreen
                    },
                )
                if (mediaCheck.reason != null) {
                    KeyValueRow("原因", mediaCheck.reason, valueColor = DangerRose)
                }
                KeyValueRow("预签名", if (mediaCheck.isPresigned) "是（保持原样，不重写 query）" else "否")
                state.episode?.let { episode ->
                    KeyValueRow(
                        "分集",
                        "第 ${episode.number} 集 · ${episode.title ?: "无标题"} · 节点 ${episode.nodeLabel ?: "?"}",
                    )
                }
            }

            Spacer(Modifier.height(14.dp))
            SectionTitle("HTTP Range 检测", trailing = "拖动进度条的前提")
            Spacer(Modifier.height(6.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                PrimaryButton(
                    text = if (state.probing) "检测中…" else "检测服务器是否支持 Range",
                    onClick = viewModel::probeRange,
                    enabled = mediaCheck.ok && !state.probing,
                    loading = state.probing,
                    modifier = Modifier.weight(1f),
                )
                SecondaryButton(
                    text = "重新加载",
                    onClick = reloadMedia,
                    enabled = mediaUrl != null,
                )
            }
            Spacer(Modifier.height(8.dp))
            val probe = state.probe
            if (probe == null) {
                NotePanel(
                    text = "点击检测：客户端会发送 Range: bytes=0-1，若返回 206 Partial Content 与 " +
                        "Content-Range，则说明支持拖动与断点续播。",
                    accent = BrassDim,
                )
            } else {
                LeatherPanel(Modifier.fillMaxWidth()) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        StatusPill(
                            text = when {
                                probe.rangeSupported -> "支持 Range（HTTP 206）"
                                probe.ok -> "忽略 Range（HTTP 200）"
                                else -> "检测失败"
                            },
                            tone = when {
                                probe.rangeSupported -> Tone.Ok
                                probe.ok -> Tone.Warn
                                else -> Tone.Bad
                            },
                        )
                    }
                    Spacer(Modifier.height(8.dp))
                    KeyValueRow("HTTP 状态", probe.httpStatus?.toString() ?: "无响应")
                    KeyValueRow("Accept-Ranges", probe.acceptRanges ?: "—", mono = true)
                    KeyValueRow("Content-Range", probe.contentRange ?: "—", mono = true)
                    KeyValueRow("Content-Type", probe.contentType ?: "—", mono = true)
                    KeyValueRow("Content-Length", probe.contentLength?.toString() ?: "—")
                    KeyValueRow("耗时", "${probe.elapsedMs} ms")
                    if (probe.failure != null) {
                        Spacer(Modifier.height(8.dp))
                        FailurePanel(failure = probe.failure, onRetry = viewModel::probeRange)
                    }
                }
            }

            Spacer(Modifier.height(20.dp))
            NotePanel(
                text = "播放地址只来自后端 /api/works/{id}/episodes 返回的 source.url，客户端不内置任何视频地址；" +
                    "也不持有 MinIO 账号或密钥。",
                accent = BrassDim,
            )
            Spacer(Modifier.height(12.dp))
            SecondaryButton(
                text = "前往连接设置",
                onClick = onOpenConfig,
                modifier = Modifier.fillMaxWidth(),
            )
            Spacer(Modifier.height(28.dp))
        }
    }
}

@Composable
private fun PlayerStage(
    player: Player,
    hasMedia: Boolean,
    isPlaying: Boolean,
    positionMs: Long,
    durationMs: Long,
    fullscreen: Boolean,
    errorText: String?,
    onToggleFullscreen: () -> Unit,
    onSeek: (Long) -> Unit,
    onRetry: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Box(modifier.background(Color.Black)) {
        if (hasMedia) {
            AndroidView(
                factory = { ctx ->
                    PlayerView(ctx).apply {
                        useController = false
                        setShowBuffering(PlayerView.SHOW_BUFFERING_WHEN_PLAYING)
                    }
                },
                update = { view -> view.player = player },
                modifier = Modifier.fillMaxSize(),
            )
        } else {
            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        text = "◈",
                        style = MaterialTheme.typography.displaySmall,
                        color = BrassDim,
                    )
                    Text(
                        text = "尚未选择可播放的分集",
                        style = MaterialTheme.typography.bodySmall,
                        color = ParchmentDim,
                    )
                }
            }
        }

        if (errorText != null) {
            Box(
                Modifier
                    .fillMaxSize()
                    .background(WalnutDeep.copy(alpha = 0.82f))
                    .padding(18.dp),
                contentAlignment = Alignment.Center,
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        text = "播放失败",
                        style = MaterialTheme.typography.titleMedium,
                        color = DangerRose,
                    )
                    Spacer(Modifier.height(4.dp))
                    Text(
                        text = errorText,
                        style = MaterialTheme.typography.bodySmall,
                        color = Parchment,
                        textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                    )
                    Spacer(Modifier.height(10.dp))
                    SecondaryButton(text = "重试播放", onClick = onRetry)
                }
            }
        }

        Column(
            Modifier
                .align(Alignment.BottomCenter)
                .fillMaxWidth()
                .background(
                    Brush.verticalGradient(
                        listOf(Color.Transparent, WalnutDeep.copy(alpha = 0.94f)),
                    ),
                )
                .padding(horizontal = 12.dp, vertical = 8.dp),
        ) {
            var scrubbing by remember { mutableStateOf(false) }
            var scrubValue by remember { mutableFloatStateOf(0f) }
            val duration = durationMs.coerceAtLeast(1L)

            Slider(
                value = if (scrubbing) {
                    scrubValue
                } else {
                    positionMs.toFloat().coerceIn(0f, duration.toFloat())
                },
                onValueChange = { value ->
                    scrubbing = true
                    scrubValue = value
                },
                onValueChangeFinished = {
                    player.seekTo(scrubValue.toLong())
                    onSeek(scrubValue.toLong())
                    scrubbing = false
                },
                valueRange = 0f..duration.toFloat(),
                enabled = hasMedia && durationMs > 0,
                colors = SliderDefaults.colors(
                    thumbColor = Brass,
                    activeTrackColor = Brass,
                    inactiveTrackColor = WalnutEdge,
                ),
            )

            Row(verticalAlignment = Alignment.CenterVertically) {
                TransportButton(
                    isPlaying = isPlaying,
                    enabled = hasMedia,
                    onClick = { if (player.isPlaying) player.pause() else player.play() },
                )
                Spacer(Modifier.width(12.dp))
                Text(
                    text = "${formatClock(if (scrubbing) scrubValue.toLong() else positionMs)} / ${formatClock(durationMs)}",
                    style = MaterialTheme.typography.bodySmall,
                    color = Parchment,
                    fontFamily = FontFamily.Monospace,
                )
                Spacer(Modifier.weight(1f))
                Surface(
                    onClick = onToggleFullscreen,
                    shape = RoundedCornerShape(4.dp),
                    color = Walnut.copy(alpha = 0.7f),
                    border = BorderStroke(1.dp, BrassDim),
                ) {
                    Text(
                        text = if (fullscreen) "退出全屏" else "全屏",
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp),
                        style = MaterialTheme.typography.labelSmall,
                        color = Brass,
                    )
                }
            }
        }
    }
}

@Composable
private fun TransportButton(
    isPlaying: Boolean,
    enabled: Boolean,
    onClick: () -> Unit,
) {
    Box(
        modifier = Modifier
            .size(44.dp)
            .clip(CircleShape)
            .background(if (enabled) Brass.copy(alpha = 0.95f) else BrassDim.copy(alpha = 0.35f))
            .clickable(enabled = enabled, onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        Canvas(Modifier.size(16.dp)) {
            if (isPlaying) {
                val barWidth = size.width * 0.3f
                drawRect(color = WalnutDeep, topLeft = Offset(0f, 0f), size = Size(barWidth, size.height))
                drawRect(
                    color = WalnutDeep,
                    topLeft = Offset(size.width - barWidth, 0f),
                    size = Size(barWidth, size.height),
                )
            } else {
                val path = Path().apply {
                    moveTo(size.width * 0.16f, 0f)
                    lineTo(size.width, size.height / 2f)
                    lineTo(size.width * 0.16f, size.height)
                    close()
                }
                drawPath(path, WalnutDeep)
            }
        }
    }
}

@Composable
private fun SelectChip(
    text: String,
    selected: Boolean,
    onClick: () -> Unit,
) {
    Surface(
        modifier = Modifier.clickable(onClick = onClick),
        shape = RoundedCornerShape(5.dp),
        color = if (selected) InkGreen else Walnut,
        border = BorderStroke(1.dp, if (selected) Brass else WalnutEdge),
    ) {
        Text(
            text = text,
            modifier = Modifier
                .padding(horizontal = 10.dp, vertical = 6.dp)
                .widthIn(max = 170.dp),
            style = MaterialTheme.typography.labelMedium,
            color = if (selected) Parchment else ParchmentDeep,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
    }
}

private fun verdictLabel(verdict: MediaUrls.Verdict): String = when (verdict) {
    MediaUrls.Verdict.RESOLVED_RELATIVE -> "站内相对路径，已按服务器地址补全"
    MediaUrls.Verdict.ABSOLUTE -> "绝对地址，原样使用"
    MediaUrls.Verdict.PRESIGNED -> "预签名地址，原样使用（保留签名参数）"
    MediaUrls.Verdict.REJECTED -> "地址被拒绝"
}

private fun formatClock(milliseconds: Long): String {
    val totalSeconds = (milliseconds / 1000).coerceAtLeast(0)
    val hours = totalSeconds / 3600
    val minutes = (totalSeconds % 3600) / 60
    val seconds = totalSeconds % 60
    return if (hours > 0) {
        String.format(java.util.Locale.US, "%d:%02d:%02d", hours, minutes, seconds)
    } else {
        String.format(java.util.Locale.US, "%02d:%02d", minutes, seconds)
    }
}

private fun describePlaybackError(error: PlaybackException): String = when (error.errorCode) {
    PlaybackException.ERROR_CODE_IO_NETWORK_CONNECTION_FAILED,
    PlaybackException.ERROR_CODE_IO_NETWORK_CONNECTION_TIMEOUT,
    -> "网络中断或连接超时（${error.errorCodeName}）"

    PlaybackException.ERROR_CODE_IO_FILE_NOT_FOUND -> "视频不存在（404）"
    PlaybackException.ERROR_CODE_IO_BAD_HTTP_STATUS -> "服务器返回错误状态，可能是 403/404（${error.errorCodeName}）"
    PlaybackException.ERROR_CODE_IO_CLEARTEXT_NOT_PERMITTED -> "系统禁止明文 HTTP，Release 请使用 HTTPS"
    PlaybackException.ERROR_CODE_IO_READ_POSITION_OUT_OF_RANGE -> "服务器没有正确响应 Range 请求（读取越界）"
    PlaybackException.ERROR_CODE_DECODING_FAILED,
    PlaybackException.ERROR_CODE_DECODER_INIT_FAILED,
    -> "解码失败：视频编码不受支持"

    PlaybackException.ERROR_CODE_PARSING_CONTAINER_MALFORMED,
    PlaybackException.ERROR_CODE_PARSING_MANIFEST_MALFORMED,
    -> "媒体格式无法解析（可能不是有效视频）"

    else -> "${error.errorCodeName}：${error.message ?: "未知播放错误"}"
}

private tailrec fun Context.findActivity(): Activity? = when (this) {
    is Activity -> this
    is ContextWrapper -> baseContext.findActivity()
    else -> null
}
