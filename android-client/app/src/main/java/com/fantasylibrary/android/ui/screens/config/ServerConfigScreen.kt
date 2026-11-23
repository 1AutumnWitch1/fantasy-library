package com.fantasylibrary.android.ui.screens.config

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
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
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.fantasylibrary.android.data.network.HealthReport
import com.fantasylibrary.android.ui.components.BrassDivider
import com.fantasylibrary.android.ui.components.FailurePanel
import com.fantasylibrary.android.ui.components.KeyValueRow
import com.fantasylibrary.android.ui.components.LeatherPanel
import com.fantasylibrary.android.ui.components.LibraryTopBar
import com.fantasylibrary.android.ui.components.LinkAction
import com.fantasylibrary.android.ui.components.NotePanel
import com.fantasylibrary.android.ui.components.PaperCard
import com.fantasylibrary.android.ui.components.PrimaryButton
import com.fantasylibrary.android.ui.components.SecondaryButton
import com.fantasylibrary.android.ui.components.SectionTitle
import com.fantasylibrary.android.ui.components.StatusPill
import com.fantasylibrary.android.ui.components.Tone
import com.fantasylibrary.android.ui.components.libraryBackground
import com.fantasylibrary.android.ui.theme.Brass
import com.fantasylibrary.android.ui.theme.BrassDim
import com.fantasylibrary.android.ui.theme.DangerRose
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
fun ServerConfigScreen(
    viewModel: ServerConfigViewModel,
    onBack: (() -> Unit)? = null,
    onSaved: () -> Unit = {},
) {
    val state by viewModel.state.collectAsStateWithLifecycle()

    Column(
        Modifier
            .fillMaxSize()
            .libraryBackground(),
    ) {
        LibraryTopBar(
            title = "馆门钥匙",
            subtitle = "服务器连接配置 · 支持任意 IP / 端口",
            onBack = onBack,
            actions = {
                LinkAction(text = "清空", onClick = viewModel::clearAddress, color = ParchmentDim)
            },
        )
        Column(
            Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 16.dp, vertical = 14.dp),
        ) {
            SectionTitle("连接目标", trailing = if (state.loaded && state.savedBaseUrl.isNotBlank()) "已保存" else null)
            Spacer(Modifier.height(8.dp))

            PaperCard(Modifier.fillMaxWidth()) {
                SchemeSelector(
                    scheme = state.scheme,
                    onSelect = viewModel::onSchemeChange,
                )
                Spacer(Modifier.height(10.dp))

                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    ConfigField(
                        value = state.host,
                        onValueChange = viewModel::onHostChange,
                        label = "服务器地址（IP 或域名）",
                        placeholder = "192.168.1.20",
                        modifier = Modifier.weight(1f),
                    )
                    ConfigField(
                        value = state.port,
                        onValueChange = viewModel::onPortChange,
                        label = "端口",
                        placeholder = "8080",
                        modifier = Modifier.width(96.dp),
                        numeric = true,
                    )
                }

                Spacer(Modifier.height(10.dp))
                Text(
                    text = "将访问",
                    style = MaterialTheme.typography.labelSmall,
                    color = ParchmentDeep,
                )
                Spacer(Modifier.height(3.dp))
                Box(
                    Modifier
                        .fillMaxWidth()
                        .background(InkGreen.copy(alpha = 0.85f), RoundedCornerShape(5.dp))
                        .padding(horizontal = 10.dp, vertical = 8.dp),
                ) {
                    Text(
                        text = state.normalizedBaseUrl.ifBlank { "—" },
                        style = MaterialTheme.typography.bodySmall,
                        color = Parchment,
                        fontFamily = FontFamily.Monospace,
                    )
                }

                val inputError = state.inputError
                if (inputError != null) {
                    Spacer(Modifier.height(8.dp))
                    Text(
                        text = inputError,
                        style = MaterialTheme.typography.bodySmall,
                        color = DangerRose,
                    )
                }

                state.warnings.forEach { warning ->
                    Spacer(Modifier.height(6.dp))
                    NotePanel(text = warning, accent = BrassDim)
                }

                Spacer(Modifier.height(12.dp))
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    PrimaryButton(
                        text = "保存地址",
                        onClick = {
                            viewModel.save()
                            onSaved()
                        },
                        enabled = state.canSubmit,
                        loading = state.saving,
                        modifier = Modifier.weight(1f),
                    )
                    SecondaryButton(
                        text = if (state.probing) "测试中…" else "测试连接",
                        onClick = viewModel::testConnection,
                        enabled = state.canSubmit,
                        modifier = Modifier.weight(1f),
                    )
                }
                if (state.statusMessage != null) {
                    Spacer(Modifier.height(8.dp))
                    Text(
                        text = state.statusMessage.orEmpty(),
                        style = MaterialTheme.typography.bodySmall,
                        color = Brass,
                    )
                }
            }

            Spacer(Modifier.height(14.dp))
            PaperCard(Modifier.fillMaxWidth(), contentPadding = 12) {
                Text(
                    text = "快速填入",
                    style = MaterialTheme.typography.labelSmall,
                    color = ParchmentDeep,
                )
                Spacer(Modifier.height(6.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    PresetChip("模拟器 10.0.2.2", "10.0.2.2") {
                        viewModel.applyPreset("10.0.2.2", "8080", "http")
                    }
                    PresetChip("本机 127.0.0.1", "127.0.0.1") {
                        viewModel.applyPreset("127.0.0.1", "8080", "http")
                    }
                }
                Spacer(Modifier.height(8.dp))
                NotePanel(
                    text = "10.0.2.2 只对 Android 模拟器有效（映射到电脑本机）。真机请填电脑的局域网 IP，" +
                        "例如 192.168.1.20；127.0.0.1 在手机上指向手机自己。",
                    onLight = true,
                )
            }

            Spacer(Modifier.height(16.dp))
            SectionTitle("连接结果", trailing = if (state.report != null) "来自真实接口" else "尚未测试")
            Spacer(Modifier.height(8.dp))

            val report = state.report
            when {
                state.probing -> LeatherPanel(Modifier.fillMaxWidth()) {
                    KeyValueRow("请求中", "正在访问 /api/health …")
                }
                report == null -> LeatherPanel(Modifier.fillMaxWidth()) {
                    Text(
                        text = "点击「测试连接」后，这里会显示 HTTP 状态码、响应时间、数据库状态与失败原因。",
                        style = MaterialTheme.typography.bodySmall,
                        color = ParchmentDim,
                    )
                }
                else -> HealthReportPanel(report, onRetry = viewModel::testConnection)
            }

            Spacer(Modifier.height(16.dp))
            NotePanel(
                text = "本页只保存服务器地址，不会保存任何数据库或 MinIO 凭据；" +
                    "所有业务数据都通过 Spring Boot API 获取。",
                accent = BrassDim,
            )
            Spacer(Modifier.height(28.dp))
        }
    }
}

@Composable
private fun HealthReportPanel(report: HealthReport, onRetry: () -> Unit) {
    Column(Modifier.fillMaxWidth()) {
        if (report.failure != null) {
            FailurePanel(failure = report.failure, onRetry = onRetry)
            Spacer(Modifier.height(10.dp))
        }
        LeatherPanel(Modifier.fillMaxWidth()) {
            Row(
                Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                StatusPill(
                    text = if (report.ok) "连接成功" else "连接失败",
                    tone = if (report.ok) Tone.Ok else Tone.Bad,
                )
                if (report.ok) {
                    StatusPill(
                        text = "应用 ${report.application ?: "?"}",
                        tone = if (report.application == "UP") Tone.Ok else Tone.Warn,
                    )
                    StatusPill(
                        text = "数据库 ${report.database ?: "?"}",
                        tone = when (report.database) {
                            "UP" -> Tone.Ok
                            "NOT_CONFIGURED" -> Tone.Warn
                            else -> Tone.Bad
                        },
                    )
                }
            }
            Spacer(Modifier.height(10.dp))
            KeyValueRow("请求地址", report.endpoint, mono = true)
            KeyValueRow("HTTP 状态", report.httpStatus?.toString() ?: "无响应")
            KeyValueRow("响应时间", "${report.elapsedMs} ms")
            if (report.contentType != null) {
                KeyValueRow("Content-Type", report.contentType, mono = true)
            }
            KeyValueRow(
                "检测时间",
                SimpleDateFormat("HH:mm:ss", Locale.getDefault()).format(Date(report.checkedAt)),
            )
            if (!report.rawBody.isNullOrBlank()) {
                Spacer(Modifier.height(8.dp))
                BrassDivider(Modifier.fillMaxWidth())
                Spacer(Modifier.height(6.dp))
                Text(
                    text = "响应正文",
                    style = MaterialTheme.typography.labelSmall,
                    color = ParchmentDeep,
                )
                Text(
                    text = report.rawBody,
                    style = MaterialTheme.typography.bodySmall,
                    color = Parchment,
                    fontFamily = FontFamily.Monospace,
                )
            }
        }
    }
}

@Composable
private fun SchemeSelector(scheme: String, onSelect: (String) -> Unit) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Text(
            text = "协议",
            style = MaterialTheme.typography.labelSmall,
            color = ParchmentDeep,
            modifier = Modifier.width(40.dp),
        )
        SchemeChip("http", scheme == "http") { onSelect("http") }
        Spacer(Modifier.width(8.dp))
        SchemeChip("https", scheme == "https") { onSelect("https") }
        Spacer(Modifier.width(10.dp))
        Text(
            text = if (scheme == "http") "测试环境" else "需要证书",
            style = MaterialTheme.typography.labelSmall,
            color = ParchmentDim,
        )
    }
}

@Composable
private fun SchemeChip(text: String, selected: Boolean, onClick: () -> Unit) {
    Surface(
        modifier = Modifier.clickable(onClick = onClick),
        shape = RoundedCornerShape(4.dp),
        color = if (selected) InkGreen else Color.Transparent,
        border = BorderStroke(1.dp, if (selected) BrassDim else WalnutEdge),
    ) {
        Text(
            text = text,
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
            style = MaterialTheme.typography.labelMedium,
            color = if (selected) Parchment else ParchmentDeep,
        )
    }
}

@Composable
private fun PresetChip(label: String, host: String, onClick: () -> Unit) {
    Surface(
        modifier = Modifier.clickable(onClick = onClick),
        shape = RoundedCornerShape(4.dp),
        color = Walnut,
        border = BorderStroke(1.dp, BrassDim.copy(alpha = 0.6f)),
    ) {
        Text(
            text = label,
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp),
            style = MaterialTheme.typography.labelSmall,
            color = Brass,
        )
    }
}

@Composable
private fun ConfigField(
    value: String,
    onValueChange: (String) -> Unit,
    label: String,
    placeholder: String,
    modifier: Modifier = Modifier,
    numeric: Boolean = false,
) {
    OutlinedTextField(
        value = value,
        onValueChange = onValueChange,
        modifier = modifier,
        label = {
            Text(
                text = label,
                style = MaterialTheme.typography.labelSmall,
            )
        },
        placeholder = {
            Text(
                text = placeholder,
                style = MaterialTheme.typography.bodySmall,
                color = ParchmentDeep.copy(alpha = 0.6f),
            )
        },
        singleLine = true,
        textStyle = MaterialTheme.typography.bodyMedium.copy(color = Ink),
        keyboardOptions = if (numeric) {
            KeyboardOptions(keyboardType = KeyboardType.Number)
        } else {
            KeyboardOptions(keyboardType = KeyboardType.Uri)
        },
        shape = RoundedCornerShape(5.dp),
        colors = OutlinedTextFieldDefaults.colors(
            focusedContainerColor = Parchment,
            unfocusedContainerColor = Parchment,
            errorContainerColor = Parchment,
            cursorColor = Ink,
            focusedBorderColor = Brass,
            unfocusedBorderColor = BrassDim.copy(alpha = 0.7f),
            focusedLabelColor = Ink,
            unfocusedLabelColor = ParchmentDeep,
        ),
    )
}
