package com.fantasylibrary.android.ui.screens.config

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.fantasylibrary.android.BuildConfig
import com.fantasylibrary.android.ui.components.*

@Composable
fun ServerConfigScreen(viewModel: ServerConfigViewModel, onBack: () -> Unit) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    ScrollRoom("连接设置", onBack, backLabel = "返回") {
        Column(Modifier.fillMaxSize().imePadding().verticalScroll(rememberScrollState()).padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)) {
            ScrollHeading("图书馆服务器", "填写你平时访问图书馆时使用的服务器地址。")
            PaperTile(Modifier.fillMaxWidth()) {
                Column(Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(9.dp)) {
                    if (BuildConfig.ALLOW_CLEARTEXT) Row(horizontalArrangement = Arrangement.spacedBy(7.dp)) {
                        PageChip("HTTP", state.scheme == "http") { viewModel.onSchemeChange("http") }
                        PageChip("HTTPS", state.scheme == "https") { viewModel.onSchemeChange("https") }
                    }
                    PageField(state.host, viewModel::onHostChange, "服务器地址", Modifier.fillMaxWidth())
                    PageField(state.port, viewModel::onPortChange, "端口", Modifier.fillMaxWidth())
                    if (state.normalizedBaseUrl.isNotBlank()) Text(state.normalizedBaseUrl, color = PageMuted,
                        style = MaterialTheme.typography.bodySmall)
                    state.inputError?.let { Text(it, color = PageInk, style = MaterialTheme.typography.bodyMedium) }
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        PageButton(if (state.saving) "正在保存…" else "保存地址", viewModel::save, state.canSubmit, emphasized = true)
                        PageButton(if (state.probing) "正在连接…" else "检查连接", viewModel::testConnection, state.canSubmit)
                    }
                }
            }
            val report = state.report
            if (report != null) {
                val text = if (report.ok) "已连接到图书馆。" else report.failure?.readerMessage() ?: "暂时无法连接图书馆。"
                Text(text, color = PageInk, style = MaterialTheme.typography.bodyMedium)
            } else if (state.statusMessage != null) Text(state.statusMessage.orEmpty(), color = PageInk,
                style = MaterialTheme.typography.bodyMedium)
        }
    }
}
