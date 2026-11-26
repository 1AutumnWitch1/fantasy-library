package com.fantasylibrary.android.ui.screens.video

import android.content.Context
import android.media.AudioManager
import android.provider.Settings
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.media3.common.Player
import androidx.media3.ui.AspectRatioFrameLayout
import androidx.media3.ui.PlayerView
import com.fantasylibrary.android.ui.theme.*
import kotlinx.coroutines.delay
import kotlin.math.abs
import kotlin.math.roundToInt

private enum class VideoDrag { SEEK, BRIGHTNESS, VOLUME }

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun PlayerStage(
    player: Player, hasMedia: Boolean, isPlaying: Boolean, activelyPlaying: Boolean,
    positionMs: Long, durationMs: Long, fullscreen: Boolean, errorText: String?, buffering: Boolean,
    title: String, qualities: List<Pair<Int, String>>, currentSource: Int,
    onBack: () -> Unit, onToggleFullscreen: () -> Unit, onSeek: (Long) -> Unit, onRetry: () -> Unit,
    onQuality: (Int) -> Unit, modifier: Modifier = Modifier,
) {
    val context = LocalContext.current
    val window = remember(context) { context.findActivity()?.window }
    val audio = remember(context) { context.getSystemService(Context.AUDIO_SERVICE) as AudioManager }
    val originalBrightness = remember(window) { window?.attributes?.screenBrightness ?: -1f }
    var brightness by remember(window) {
        val system = Settings.System.getInt(context.contentResolver, Settings.System.SCREEN_BRIGHTNESS, 128)
        mutableFloatStateOf(if (originalBrightness >= 0f) originalBrightness else (system / 255f).coerceIn(.03f, 1f))
    }
    DisposableEffect(window) {
        onDispose { window?.let { it.attributes = it.attributes.apply { screenBrightness = originalBrightness } } }
    }

    var show by remember { mutableStateOf(true) }
    var interaction by remember { mutableIntStateOf(0) }
    var menu by remember { mutableStateOf<String?>(null) }
    var sliding by remember { mutableStateOf(false) }
    var dragging by remember { mutableStateOf(false) }
    var preview by remember { mutableLongStateOf(0) }
    var hud by remember { mutableStateOf<String?>(null) }
    var hudRevision by remember { mutableIntStateOf(0) }
    var speed by remember(player) { mutableFloatStateOf(player.playbackParameters.speed) }
    val actualDuration by rememberUpdatedState(durationMs)
    val latestSeek by rememberUpdatedState(onSeek)
    val controlVisible = show || !activelyPlaying || sliding || dragging || menu != null

    fun touch() { show = true; interaction++ }
    fun feedback(value: String) { hud = value; hudRevision++ }
    fun transport() {
        if (player.playWhenReady && player.playbackState != Player.STATE_ENDED) player.pause()
        else {
            if (player.playbackState == Player.STATE_ENDED) latestSeek(0)
            player.play()
        }
        touch()
    }

    LaunchedEffect(activelyPlaying, interaction, sliding, dragging, menu, fullscreen, hasMedia) {
        if (!activelyPlaying || sliding || dragging || menu != null) show = true
        else {
            delay(3_000)
            show = false
        }
    }
    LaunchedEffect(hudRevision, sliding, dragging) {
        if (!dragging && !sliding && hud != null) { delay(900); hud = null }
    }

    Box(modifier.background(Color.Black)) {
        if (hasMedia) {
            AndroidView(
                factory = { ctx -> PlayerView(ctx).apply {
                    useController = false
                    resizeMode = AspectRatioFrameLayout.RESIZE_MODE_FIT
                    setShowBuffering(PlayerView.SHOW_BUFFERING_NEVER)
                } },
                update = { if (it.player !== player) it.player = player },
                onRelease = { it.player = null },
                modifier = Modifier.matchParentSize(),
            )
            Box(Modifier.matchParentSize()
                .pointerInput(player) {
                    detectTapGestures(onTap = { touch() }, onDoubleTap = { point ->
                        val distance = when {
                            point.x < size.width / 3f -> -10_000L
                            point.x > size.width * 2 / 3f -> 10_000L
                            else -> 0
                        }
                        if (distance == 0L) {
                            transport()
                            feedback(if (player.playWhenReady) "继续播放" else "已暂停")
                        } else if (actualDuration > 0) {
                            latestSeek((player.currentPosition + distance).coerceIn(0, actualDuration))
                            feedback(if (distance < 0) "快退 10 秒" else "快进 10 秒")
                        }
                    })
                }
                .pointerInput(player, window, audio) {
                    awaitEachGesture {
                        val first = awaitFirstDown(requireUnconsumed = false)
                        val position = player.currentPosition.coerceAtLeast(0)
                        val initialBrightness = brightness
                        val max = audio.getStreamMaxVolume(AudioManager.STREAM_MUSIC).coerceAtLeast(1)
                        val initialVolume = audio.getStreamVolume(AudioManager.STREAM_MUSIC)
                        var volume = initialVolume
                        var delta = Offset.Zero
                        var axis: VideoDrag? = null
                        try {
                            while (true) {
                                val pointer = awaitPointerEvent().changes.firstOrNull { it.id == first.id } ?: break
                                if (pointer.isConsumed) break
                                if (!pointer.pressed) {
                                    if (axis == VideoDrag.SEEK && actualDuration > 0) latestSeek(preview)
                                    if (axis != null) pointer.consume()
                                    break
                                }
                                delta = pointer.position - first.position
                                if (axis == null && delta.getDistance() > viewConfiguration.touchSlop) {
                                    axis = when {
                                        abs(delta.x) >= abs(delta.y) -> VideoDrag.SEEK
                                        first.position.x < size.width / 2f -> VideoDrag.BRIGHTNESS
                                        else -> VideoDrag.VOLUME
                                    }
                                    dragging = true
                                    touch()
                                }
                                when (axis) {
                                    VideoDrag.SEEK -> if (actualDuration > 0) {
                                        val range = minOf(actualDuration, 120_000)
                                        preview = (position + delta.x / size.width * range).toLong().coerceIn(0, actualDuration)
                                        val difference = (preview - position) / 1000
                                        feedback("${if (difference < 0) "快退" else "快进"} ${abs(difference)} 秒 · ${formatClock(preview)}")
                                    }
                                    VideoDrag.BRIGHTNESS -> if (window != null) {
                                        brightness = (initialBrightness - delta.y / size.height).coerceIn(.03f, 1f)
                                        window.attributes = window.attributes.apply { screenBrightness = brightness }
                                        feedback("☀ 亮度 ${(brightness * 100).roundToInt()}%")
                                    }
                                    VideoDrag.VOLUME -> {
                                        val next = (initialVolume - delta.y / size.height * max).roundToInt().coerceIn(0, max)
                                        if (volume != next) {
                                            audio.setStreamVolume(AudioManager.STREAM_MUSIC, next, 0)
                                            volume = audio.getStreamVolume(AudioManager.STREAM_MUSIC)
                                        }
                                        feedback("♪ 音量 ${volume * 100 / max}%")
                                    }
                                    null -> Unit
                                }
                                if (axis != null) pointer.consume()
                            }
                        } finally {
                            if (axis != null) { dragging = false; interaction++ }
                        }
                    }
                })
        }

        if ((buffering || !hasMedia) && errorText == null && hud == null) {
            Column(Modifier.align(Alignment.Center).background(WalnutDeep.copy(alpha = .74f), RoundedCornerShape(7.dp))
                .padding(9.dp), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(4.dp)) {
                if (buffering) CircularProgressIndicator(Modifier.size(22.dp), color = Brass, strokeWidth = 2.dp)
                Text(if (!buffering) "暂无可播放的分集" else if (hasMedia) "正在缓冲…" else "正在加载…",
                    color = Parchment, style = MaterialTheme.typography.labelSmall)
            }
        }
        if (errorText != null) {
            Column(Modifier.align(Alignment.Center).padding(horizontal = 30.dp)
                .background(WalnutDeep.copy(alpha = .9f), RoundedCornerShape(7.dp)).padding(9.dp),
                horizontalAlignment = Alignment.CenterHorizontally) {
                Text(errorText, color = Parchment, style = MaterialTheme.typography.bodySmall)
                VideoControl("重试", onRetry)
            }
        }
        if (controlVisible) {
            Row(Modifier.align(Alignment.TopCenter).fillMaxWidth().background(Brush.verticalGradient(
                listOf(Color.Black.copy(alpha = .83f), Color.Transparent))).padding(horizontal = 7.dp),
                verticalAlignment = Alignment.CenterVertically) {
                VideoControl(if (fullscreen) "‹ 退出全屏" else "‹ 返回", onBack)
                Text(title, color = Parchment, style = MaterialTheme.typography.labelLarge,
                    maxLines = 1, overflow = TextOverflow.Ellipsis, modifier = Modifier.weight(1f).padding(start = 4.dp))
            }
            if (hasMedia) Column(Modifier.align(Alignment.BottomCenter).fillMaxWidth()
                .background(Brush.verticalGradient(listOf(Color.Transparent, WalnutDeep.copy(alpha = .94f))))
                .padding(horizontal = if (fullscreen) 26.dp else 8.dp)) {
                Slider(
                    value = (if (sliding) preview else positionMs).coerceIn(0, durationMs.coerceAtLeast(1)).toFloat(),
                    onValueChange = {
                        sliding = true
                        preview = it.toLong()
                        feedback(formatClock(preview))
                    },
                    onValueChangeFinished = { latestSeek(preview); sliding = false; touch() },
                    valueRange = 0f..durationMs.coerceAtLeast(1).toFloat(),
                    enabled = durationMs > 0 && errorText == null,
                    modifier = Modifier.fillMaxWidth().height(29.dp).semantics { contentDescription = "播放进度" },
                    thumb = {
                        Box(Modifier.size(11.dp).background(if (durationMs > 0) BrassBright else BrassDim, CircleShape))
                    },
                    track = { slider ->
                        val total = durationMs.toFloat().coerceAtLeast(1f)
                        val buffered = (player.bufferedPosition / total).coerceIn(0f, 1f)
                        val elapsed = (slider.value / total).coerceIn(0f, 1f)
                        Box(Modifier.fillMaxWidth().height(4.dp).background(Parchment.copy(alpha = .25f), CircleShape)) {
                            Box(Modifier.fillMaxHeight().fillMaxWidth(buffered).background(Parchment.copy(alpha = .36f), CircleShape))
                            Box(Modifier.fillMaxHeight().fillMaxWidth(elapsed).background(Brass, CircleShape))
                        }
                    },
                )
                Row(verticalAlignment = Alignment.CenterVertically) {
                    VideoControl(if (isPlaying) "暂停" else "播放", ::transport)
                    Text("${formatClock(if (sliding) preview else positionMs)} / ${formatClock(durationMs)}",
                        Modifier.weight(1f), color = Parchment, style = MaterialTheme.typography.labelSmall,
                        maxLines = 1, overflow = TextOverflow.Ellipsis)
                    Box {
                        VideoControl(if (speed == 1f) "倍速" else "${speed}×", { touch(); menu = "speed" })
                        DropdownMenu(expanded = menu == "speed", onDismissRequest = { menu = null; touch() },
                            containerColor = InkGreenDeep) {
                            listOf(.5f, .75f, 1f, 1.25f, 1.5f, 2f).forEach { rate ->
                                DropdownMenuItem(text = { Text("${if (rate == speed) "✓ " else ""}${rate}×", color = Parchment) },
                                    onClick = { speed = rate; player.setPlaybackSpeed(rate); menu = null; touch() })
                            }
                        }
                    }
                    if (qualities.size > 1) Box {
                        VideoControl("清晰度", { touch(); menu = "quality" })
                        DropdownMenu(expanded = menu == "quality", onDismissRequest = { menu = null; touch() },
                            containerColor = InkGreenDeep) {
                            qualities.forEach { (index, label) ->
                                DropdownMenuItem(text = { Text("${if (index == currentSource) "✓ " else ""}$label", color = Parchment) },
                                    onClick = { menu = null; onQuality(index); touch() })
                            }
                        }
                    }
                    VideoControl(if (fullscreen) "退出全屏" else "全屏", { onToggleFullscreen(); touch() })
                }
            }
        }
        hud?.let { text ->
            Text(text, Modifier.align(Alignment.Center).background(WalnutDeep.copy(alpha = .82f), RoundedCornerShape(8.dp))
                .padding(horizontal = 16.dp, vertical = 10.dp), color = Parchment, style = MaterialTheme.typography.labelLarge)
        }
    }
}

@Composable
private fun VideoControl(label: String, action: () -> Unit) {
    TextButton(onClick = action, contentPadding = PaddingValues(horizontal = 6.dp),
        modifier = Modifier.heightIn(min = 43.dp), colors = ButtonDefaults.textButtonColors(contentColor = BrassBright)) {
        Text(label, style = MaterialTheme.typography.labelMedium, maxLines = 1)
    }
}
