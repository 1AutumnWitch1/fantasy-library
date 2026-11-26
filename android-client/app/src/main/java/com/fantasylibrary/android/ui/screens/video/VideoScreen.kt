package com.fantasylibrary.android.ui.screens.video

import android.app.Activity
import android.content.Context
import android.content.ContextWrapper
import android.content.pm.ActivityInfo
import android.view.WindowManager
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LifecycleEventEffect
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.media3.common.MediaItem
import androidx.media3.common.PlaybackException
import androidx.media3.common.Player
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.ui.PlayerView
import com.fantasylibrary.android.data.network.MediaUrls
import com.fantasylibrary.android.di.AppContainer
import com.fantasylibrary.android.ui.components.*
import com.fantasylibrary.android.ui.theme.*
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive

@Composable
fun VideoScreen(viewModel: VideoViewModel, discussion: VideoDiscussionViewModel, container: AppContainer,
    onOpenConfig: () -> Unit, onBack: () -> Unit, onLogin: () -> Unit,
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val comments by discussion.state.collectAsStateWithLifecycle()
    val session by container.authRepository.session.collectAsStateWithLifecycle()
    val context = LocalContext.current
    val activity = remember(context) { context.findActivity() }
    val appContext = context.applicationContext
    val player = remember(appContext) { ExoPlayer.Builder(appContext).build() }
    var playRequested by remember { mutableStateOf(false) }
    var isPlaying by remember { mutableStateOf(false) }
    var playbackState by remember { mutableIntStateOf(Player.STATE_IDLE) }
    var durationMs by remember { mutableLongStateOf(0L) }
    var positionMs by remember { mutableLongStateOf(0L) }
    var playbackError by remember { mutableStateOf<String?>(null) }
    var fullscreen by rememberSaveable { mutableStateOf(false) }
    val cleartext = container.apiClient.allowCleartext
    val source = state.selectedSource
    val media = remember(source, state.baseUrl, cleartext) { MediaUrls.resolve(source?.url, state.baseUrl, cleartext) }
    val url = media.resolved
    val episodeId = state.selectedEpisode?.id
    val workId = state.workId
    var tab by rememberSaveable(workId) { mutableIntStateOf(0) }
    val (recommendations, discussionList) = key(workId) { rememberLazyListState() to rememberLazyListState() }
    val holder = key(workId) { androidx.compose.runtime.saveable.rememberSaveableStateHolder() }

    val recorder = remember(player) { MediaRecording(player) }
    val selectedEpisode = state.selectedEpisode
    val qualities = selectedEpisode?.sources.orEmpty().mapIndexedNotNull { index, option ->
        option.quality?.takeIf { it.isNotBlank() && MediaUrls.resolve(option.url, state.baseUrl, cleartext).ok }
            ?.let { index to it }
    }.distinctBy { it.second }

    fun prepare(link: String, offset: Long) {
        playbackError = null
        durationMs = 0L
        positionMs = offset
        player.setMediaItem(MediaItem.fromUri(link), offset)
        player.prepare()
        player.playWhenReady = true
    }

    DisposableEffect(player) {
        val listener = object : Player.Listener {
            override fun onPlayWhenReadyChanged(playWhenReady: Boolean, reason: Int) {
                playRequested = playWhenReady
            }
            override fun onIsPlayingChanged(playing: Boolean) {
                isPlaying = playing
                recorder.playing(playing)
            }

            override fun onPositionDiscontinuity(oldPosition: Player.PositionInfo, newPosition: Player.PositionInfo, reason: Int) {
                if (reason == Player.DISCONTINUITY_REASON_SEEK) recorder.discontinuity(newPosition.positionMs)
            }

            override fun onPlaybackStateChanged(next: Int) {
                playbackState = next
                if (next == Player.STATE_ENDED) recorder.finish()
                if (next == Player.STATE_READY) {
                    durationMs = player.duration.coerceAtLeast(0L)
                    playbackError = null
                }
            }
            override fun onPlayerError(error: PlaybackException) { playbackError = describePlaybackError(error) }
        }
        player.addListener(listener)
        onDispose {
            recorder.close()
            player.removeListener(listener)
            player.release()
        }
    }
    LifecycleEventEffect(Lifecycle.Event.ON_STOP) { player.pause() }
    DisposableEffect(activity, isPlaying) {
        val window = activity?.window
        val keepOn = WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON
        val wasOn = window != null && window.attributes.flags and keepOn != 0
        if (isPlaying) window?.addFlags(keepOn)
        onDispose { if (isPlaying && !wasOn) window?.clearFlags(keepOn) }
    }

    // The media selection is the only input to preparation; tabs and the view layout are independent.
    LaunchedEffect(player, url, workId, episodeId, source?.id) {
        val offset = recorder.closeForSwitch(workId, selectedEpisode?.edition, selectedEpisode?.number) ?: state.resumePosition
        if (url == null || source == null || selectedEpisode == null) {
            player.stop()
            player.clearMediaItems()
            playbackError = null
            positionMs = 0
            durationMs = 0
        } else {
            prepare(url, offset)
            recorder.attach(workId, selectedEpisode.edition ?: "EPISODE", selectedEpisode.number,
                container.viewingSync.session(state.baseUrl, workId, source.id,
                    selectedEpisode.number.takeIf { selectedEpisode.edition == "EPISODE" }, viewModel::refreshStats))
        }
    }
    LaunchedEffect(player) {
        while (isActive) {
            positionMs = player.currentPosition.coerceAtLeast(0L)
            if (player.playbackState == Player.STATE_READY && player.duration > 0) durationMs = player.duration
            recorder.heartbeat()
            delay(300)
        }
    }

    DisposableEffect(activity, fullscreen) {
        if (activity != null && fullscreen) {
            val window = activity.window
            val controller = WindowCompat.getInsetsController(window, window.decorView)
            controller.systemBarsBehavior = WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
            controller.hide(WindowInsetsCompat.Type.systemBars())
            val previous = activity.requestedOrientation
            activity.requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_SENSOR_LANDSCAPE
            onDispose {
                controller.show(WindowInsetsCompat.Type.systemBars())
                activity.requestedOrientation = previous
            }
        } else onDispose {}
    }
    BackHandler(enabled = fullscreen) { fullscreen = false }

    val frame = if (fullscreen) Modifier.fillMaxSize() else Modifier.fillMaxSize().windowInsetsPadding(WindowInsets.safeDrawing)
    Column(frame.background(if (fullscreen) Color.Black else InkGreenDeep)) {
        PlayerStage(player = player, hasMedia = url != null,
            isPlaying = playRequested && playbackState != Player.STATE_ENDED && playbackError == null,
            positionMs = positionMs, durationMs = durationMs, fullscreen = fullscreen, errorText = playbackError,
            activelyPlaying = isPlaying, title = state.detail?.work?.title ?: "放映室",
            qualities = qualities, currentSource = state.source, onQuality = viewModel::selectSource,
            onBack = { if (fullscreen) fullscreen = false else onBack() },
            buffering = playbackState == Player.STATE_BUFFERING || (url == null && state.loading),
            onToggleFullscreen = { fullscreen = !fullscreen }, onSeek = {
                recorder.beforeSeek()
                player.seekTo(it)
                positionMs = it
            },
            onRetry = { playbackError = null; player.prepare(); player.play() },
            modifier = if (fullscreen) Modifier.fillMaxSize() else Modifier.fillMaxWidth().aspectRatio(16f / 9f),
        )
        if (!fullscreen) {
            VideoTabs(tab) { tab = it }
            Box(Modifier.weight(1f).fillMaxWidth().background(PagePaper)) {
                key(workId) {
                    if (tab == 0) holder.SaveableStateProvider("recommendations") {
                        VideoSuggestions(viewModel, state, container, recommendations,
                            sourceMessage = if (state.selectedEpisode != null && url == null) media.reason else null,
                            onSettings = onOpenConfig, onLogin = onLogin,
                        )
                    } else holder.SaveableStateProvider("comments") {
                        if (state.baseUrl.isBlank()) {
                            PageStatus("连接图书馆后即可阅读评论。", actionLabel = "连接设置", action = onOpenConfig)
                        } else VideoDiscussion(discussion, comments.takeIf { it.workId == workId } ?: VideoDiscussionViewModel.State(), workId, state.baseUrl, cleartext,
                            session.signedIn, discussionList, onLogin)
                    }
                }
            }
        }
    }
}

@Composable
private fun VideoTabs(selected: Int, onSelect: (Int) -> Unit) {
    Row(Modifier.fillMaxWidth().background(InkGreenDeep)) {
        listOf("推荐", "评论").forEachIndexed { index, title ->
            val active = selected == index
            Box(Modifier.weight(1f).height(47.dp).selectable(active, role = Role.Tab, onClick = { onSelect(index) }),
                contentAlignment = Alignment.Center) {
                Text(title, color = if (active) BrassBright else ParchmentSoft, style = MaterialTheme.typography.titleSmall)
                if (active) Box(Modifier.align(Alignment.BottomCenter).height(2.dp).width(32.dp).background(BrassBright))
            }
        }
    }
}

internal fun formatClock(milliseconds: Long): String {
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

internal tailrec fun Context.findActivity(): Activity? = when (this) {
    is Activity -> this
    is ContextWrapper -> baseContext.findActivity()
    else -> null
}
