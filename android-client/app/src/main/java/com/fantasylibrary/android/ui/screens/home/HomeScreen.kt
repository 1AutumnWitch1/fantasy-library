package com.fantasylibrary.android.ui.screens.home

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.fantasylibrary.android.data.network.HealthReport
import com.fantasylibrary.android.data.repository.SessionInfo
import com.fantasylibrary.android.ui.components.BrassDivider
import com.fantasylibrary.android.ui.components.FailurePanel
import com.fantasylibrary.android.ui.components.KeyValueRow
import com.fantasylibrary.android.ui.components.LeatherPanel
import com.fantasylibrary.android.ui.components.LibraryTopBar
import com.fantasylibrary.android.ui.components.LinkAction
import com.fantasylibrary.android.ui.components.NotePanel
import com.fantasylibrary.android.ui.components.PaperCard
import com.fantasylibrary.android.ui.components.PrimaryButton
import com.fantasylibrary.android.ui.components.RuneIcon
import com.fantasylibrary.android.ui.components.RuneKind
import com.fantasylibrary.android.ui.components.SecondaryButton
import com.fantasylibrary.android.ui.components.SectionTitle
import com.fantasylibrary.android.ui.components.StatusPill
import com.fantasylibrary.android.ui.components.Tone
import com.fantasylibrary.android.ui.components.libraryBackground
import com.fantasylibrary.android.ui.theme.Brass
import com.fantasylibrary.android.ui.theme.BrassDim
import com.fantasylibrary.android.ui.theme.Ink
import com.fantasylibrary.android.ui.theme.InkGreen
import com.fantasylibrary.android.ui.theme.Parchment
import com.fantasylibrary.android.ui.theme.ParchmentDeep
import com.fantasylibrary.android.ui.theme.ParchmentDim
import com.fantasylibrary.android.ui.theme.Walnut
import com.fantasylibrary.android.ui.theme.WalnutEdge
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun HomeScreen(
    viewModel: HomeViewModel,
    onOpenAnime: () -> Unit,
    onOpenGames: () -> Unit,
    onOpenVideo: () -> Unit,
    onOpenConfig: () -> Unit,
    onOpenLogin: () -> Unit,
) {
    val state by viewModel.state.collectAsStateWithLifecycle()

    Column(
        Modifier
            .fillMaxSize()
            .libraryBackground(),
    ) {
        LibraryTopBar(
            title = "馆内状态",
            subtitle = "服务连通性与登录状态",
            actions = {
                LinkAction(text = if (state.checking) "检测中…" else "重新检测", onClick = viewModel::refresh)
            },
        )

        Column(
            Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 16.dp, vertical = 14.dp),
        ) {
            // —— 服务状态 ——
            SectionTitle("Spring Boot 服务", trailing = if (state.checking) "连接中" else null)
            Spacer(Modifier.height(8.dp))
            LeatherPanel(Modifier.fillMaxWidth()) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    StatusPill(
                        text = "地址",
                        tone = Tone.Brass,
                    )
                    Spacer(Modifier.weight(1f))
                    Text(
                        text = state.baseUrl.ifBlank { "未配置" },
                        style = MaterialTheme.typography.bodySmall,
                        color = Parchment,
                        fontFamily = FontFamily.Monospace,
                    )
                }
                Spacer(Modifier.height(8.dp))
                val report = state.health
                if (report == null && !state.checking) {
                    Text(
                        text = "尚未检测。点击右上角「重新检测」访问 /api/health。",
                        style = MaterialTheme.typography.bodySmall,
                        color = ParchmentDim,
                    )
                }
                if (state.checking) {
                    Text(
                        text = "正在请求 /api/health …",
                        style = MaterialTheme.typography.bodySmall,
                        color = ParchmentDim,
                    )
                }
                if (report != null) {
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        StatusPill(
                            text = if (report.ok) "服务正常" else "服务异常",
                            tone = if (report.ok) Tone.Ok else Tone.Bad,
                        )
                        report.application?.let {
                            StatusPill("应用 $it", if (it == "UP") Tone.Ok else Tone.Warn)
                        }
                        report.database?.let {
                            StatusPill(
                                "数据库 $it",
                                when (it) {
                                    "UP" -> Tone.Ok
                                    "NOT_CONFIGURED" -> Tone.Warn
                                    else -> Tone.Bad
                                },
                            )
                        }
                    }
                    Spacer(Modifier.height(10.dp))
                    KeyValueRow("HTTP 状态", report.httpStatus?.toString() ?: "无响应")
                    KeyValueRow("响应时间", "${report.elapsedMs} ms")
                    KeyValueRow("接口", report.endpoint, mono = true)
                    if (state.checkedAt > 0) {
                        KeyValueRow(
                            "检测时间",
                            SimpleDateFormat("HH:mm:ss", Locale.getDefault()).format(Date(state.checkedAt)),
                        )
                    }
                }
            }
            if (state.health?.failure != null) {
                Spacer(Modifier.height(8.dp))
                FailurePanel(
                    failure = state.health!!.failure!!,
                    onRetry = viewModel::refresh,
                )
            }
            if (state.notice != null) {
                Spacer(Modifier.height(8.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    NotePanel(text = state.notice.orEmpty())
                    Spacer(Modifier.height(0.dp))
                }
            }

            Spacer(Modifier.height(16.dp))
            // —— 馆藏统计 ——
            SectionTitle("Oracle 馆藏统计", trailing = if (state.statsChecking) "读取中" else "真实数据")
            Spacer(Modifier.height(8.dp))
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                StatCard(
                    label = "动漫",
                    value = state.animeTotal?.toString() ?: "—",
                    unit = "部",
                    modifier = Modifier.weight(1f),
                    onClick = onOpenAnime,
                )
                StatCard(
                    label = "游戏",
                    value = state.gameTotal?.toString() ?: "—",
                    unit = "部",
                    modifier = Modifier.weight(1f),
                    onClick = onOpenGames,
                )
            }

            Spacer(Modifier.height(16.dp))
            // —— 登录状态 ——
            SectionTitle("登录状态", trailing = "Cookie + CSRF 会话")
            Spacer(Modifier.height(8.dp))
            SessionCard(
                session = state.session,
                onOpenLogin = onOpenLogin,
                onLogout = viewModel::logout,
            )

            Spacer(Modifier.height(16.dp))
            SectionTitle("测试入口")
            Spacer(Modifier.height(8.dp))
            NavTile(
                rune = RuneKind.Anime,
                title = "动漫资料库",
                description = "读取 /api/anime 的真实数据与封面图",
                onClick = onOpenAnime,
            )
            Spacer(Modifier.height(8.dp))
            NavTile(
                rune = RuneKind.Games,
                title = "游戏资料库",
                description = "读取 /api/games，含平台与开发商信息",
                onClick = onOpenGames,
            )
            Spacer(Modifier.height(8.dp))
            NavTile(
                rune = RuneKind.Play,
                title = "视频播放测试",
                description = "ExoPlayer 播放 MinIO 中的真实视频，并检测 Range",
                onClick = onOpenVideo,
            )
            Spacer(Modifier.height(8.dp))
            NavTile(
                rune = RuneKind.Link,
                title = "服务器连接",
                description = "修改 IP / 端口，保存并测试连接",
                onClick = onOpenConfig,
            )

            Spacer(Modifier.height(24.dp))
            NotePanel(
                text = "客户端不直接连接 Oracle，也不访问 MinIO 控制台；" +
                    "图片与视频地址全部由 Spring Boot 返回。",
                accent = BrassDim,
            )
            Spacer(Modifier.height(28.dp))
        }
    }
}

@Composable
private fun StatCard(
    label: String,
    value: String,
    unit: String,
    modifier: Modifier = Modifier,
    onClick: () -> Unit,
) {
    PaperCard(modifier = modifier, onClick = onClick, contentPadding = 12) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            color = ParchmentDeep,
        )
        Spacer(Modifier.height(2.dp))
        Row(verticalAlignment = Alignment.Bottom) {
            Text(
                text = value,
                style = MaterialTheme.typography.headlineMedium,
                color = Ink,
            )
            Text(
                text = unit,
                modifier = Modifier.padding(bottom = 5.dp, start = 3.dp),
                style = MaterialTheme.typography.labelSmall,
                color = ParchmentDeep,
            )
        }
    }
}

@Composable
private fun SessionCard(
    session: SessionInfo,
    onOpenLogin: () -> Unit,
    onLogout: () -> Unit,
) {
    PaperCard(Modifier.fillMaxWidth(), contentPadding = 12) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            StatusPill(
                text = if (session.signedIn) "已登录" else "未登录",
                tone = if (session.signedIn) Tone.Ok else Tone.Neutral,
            )
            Spacer(Modifier.weight(1f))
            if (session.signedIn) {
                LinkAction(text = "退出登录", onClick = onLogout, color = InkGreen)
            } else {
                LinkAction(text = "去登录", onClick = onOpenLogin, color = InkGreen)
            }
        }
        Spacer(Modifier.height(6.dp))
        if (session.signedIn) {
            Text(
                text = session.displayName.ifBlank { session.username },
                style = MaterialTheme.typography.titleMedium,
                color = Ink,
            )
            if (session.username.isNotBlank()) {
                Text(
                    text = "@${session.username}",
                    style = MaterialTheme.typography.bodySmall,
                    color = ParchmentDeep,
                )
            }
        } else {
            Text(
                text = "尚未登录。登录后可访问 /api/me/** 受保护接口。",
                style = MaterialTheme.typography.bodySmall,
                color = Ink,
            )
        }
        Spacer(Modifier.height(4.dp))
        Text(
            text = "凭据存储：${session.storageLabel.ifBlank { "—" }}",
            style = MaterialTheme.typography.labelSmall,
            color = ParchmentDeep,
        )
    }
}

@Composable
private fun NavTile(
    rune: RuneKind,
    title: String,
    description: String,
    onClick: () -> Unit,
) {
    Surface(
        onClick = onClick,
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(8.dp),
        color = Walnut,
        border = BorderStroke(1.dp, WalnutEdge),
    ) {
        Row(
            Modifier.padding(horizontal = 12.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            RuneIcon(kind = rune, tint = Brass, size = 22.dp)
            Spacer(Modifier.width(10.dp))
            Column(Modifier.weight(1f)) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleMedium,
                    color = Parchment,
                )
                Text(
                    text = description,
                    style = MaterialTheme.typography.bodySmall,
                    color = ParchmentDim,
                )
            }
            Text(
                text = "›",
                style = MaterialTheme.typography.headlineSmall,
                color = BrassDim,
            )
        }
    }
}
