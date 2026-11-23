package com.fantasylibrary.android.ui.screens.login

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.fantasylibrary.android.ui.components.FailurePanel
import com.fantasylibrary.android.ui.components.KeyValueRow
import com.fantasylibrary.android.ui.components.LeatherPanel
import com.fantasylibrary.android.ui.components.LibraryTopBar
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
import com.fantasylibrary.android.ui.theme.Ink
import com.fantasylibrary.android.ui.theme.Parchment
import com.fantasylibrary.android.ui.theme.ParchmentDeep

@Composable
fun LoginScreen(
    viewModel: LoginViewModel,
    onBack: () -> Unit,
) {
    val state by viewModel.state.collectAsStateWithLifecycle()

    Column(
        Modifier
            .fillMaxSize()
            .libraryBackground(),
    ) {
        LibraryTopBar(
            title = "读者登录",
            subtitle = "现有接口：Cookie + CSRF 会话（无 Bearer Token）",
            onBack = onBack,
        )

        Column(
            Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .navigationBarsPadding()
                .padding(horizontal = 16.dp, vertical = 14.dp),
        ) {
            if (state.session.signedIn) {
                SectionTitle("当前会话", trailing = "已登录")
                Spacer(Modifier.height(8.dp))
                PaperCard(Modifier.fillMaxWidth()) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        StatusPill(text = "已登录", tone = Tone.Ok)
                        Spacer(Modifier.weight(1f))
                        Text(
                            text = state.session.displayName.ifBlank { state.session.username },
                            style = MaterialTheme.typography.titleMedium,
                            color = Ink,
                        )
                    }
                    Spacer(Modifier.height(8.dp))
                    Text(
                        text = "会话凭据保存在：${state.session.storageLabel.ifBlank { "—" }}",
                        style = MaterialTheme.typography.bodySmall,
                        color = ParchmentDeep,
                    )
                    Spacer(Modifier.height(10.dp))
                    SecondaryButton(
                        text = "退出登录",
                        onClick = viewModel::logout,
                        enabled = !state.busy,
                        modifier = Modifier.fillMaxWidth(),
                    )
                }
            } else {
                SectionTitle("输入账号", trailing = "不会保存密码")
                Spacer(Modifier.height(8.dp))
                PaperCard(Modifier.fillMaxWidth()) {
                    LoginField(
                        value = state.username,
                        onValueChange = viewModel::onUsernameChange,
                        label = "用户名",
                    )
                    Spacer(Modifier.height(8.dp))
                    LoginField(
                        value = state.password,
                        onValueChange = viewModel::onPasswordChange,
                        label = "密码",
                        password = true,
                    )
                    Spacer(Modifier.height(12.dp))
                    PrimaryButton(
                        text = if (state.busy) "登录中…" else "登录",
                        onClick = viewModel::login,
                        enabled = state.canSubmit,
                        loading = state.busy,
                        modifier = Modifier.fillMaxWidth(),
                    )
                    Spacer(Modifier.height(6.dp))
                    Text(
                        text = "登录会依次调用 GET /api/auth/csrf 与 POST /api/auth/login，" +
                            "成功后用 /api/me/profile 验证会话确实生效。",
                        style = MaterialTheme.typography.labelSmall,
                        color = ParchmentDeep,
                    )
                }
            }

            if (state.error != null) {
                Spacer(Modifier.height(12.dp))
                FailurePanel(failure = state.error!!, onRetry = viewModel::login)
            }
            if (state.notice != null) {
                Spacer(Modifier.height(12.dp))
                NotePanel(text = state.notice.orEmpty(), accent = Brass)
            }

            Spacer(Modifier.height(16.dp))
            SectionTitle("会话信息")
            Spacer(Modifier.height(8.dp))
            LeatherPanel(Modifier.fillMaxWidth()) {
                KeyValueRow("服务器", state.baseUrl.ifBlank { "未配置" }, mono = true)
                KeyValueRow("登录状态", if (state.session.signedIn) "已登录" else "未登录")
                KeyValueRow("账号", state.session.username.ifBlank { "—" })
                KeyValueRow(
                    "资料校验",
                    when {
                        !state.session.signedIn -> "—"
                        state.session.profileVerified -> "已通过 /api/me/profile"
                        else -> "尚未校验"
                    },
                )
            }

            Spacer(Modifier.height(16.dp))
            NotePanel(
                text = "现有后端没有面向移动端的 Token 接口：登录态是数据库会话令牌 + HttpOnly Cookie。" +
                    "客户端只保存该 Cookie 与 CSRF token（加密存储），不保存密码，" +
                    "因此不会出现「本地假登录成功」的情况。",
                accent = BrassDim,
            )
            Spacer(Modifier.height(10.dp))
            NotePanel(
                text = "若还没有账号，请先在网站注册后再回到这里登录；" +
                    "测试客户端不会自动创建账号，也不会伪造登录成功。",
                accent = BrassDim,
            )
            Spacer(Modifier.height(28.dp))
        }
    }
}

@Composable
private fun LoginField(
    value: String,
    onValueChange: (String) -> Unit,
    label: String,
    password: Boolean = false,
) {
    OutlinedTextField(
        value = value,
        onValueChange = onValueChange,
        modifier = Modifier.fillMaxWidth(),
        label = {
            Text(text = label, style = MaterialTheme.typography.labelSmall)
        },
        singleLine = true,
        visualTransformation = if (password) PasswordVisualTransformation() else androidx.compose.ui.text.input.VisualTransformation.None,
        keyboardOptions = KeyboardOptions(
            keyboardType = if (password) KeyboardType.Password else KeyboardType.Text,
        ),
        textStyle = MaterialTheme.typography.bodyMedium.copy(color = Ink),
        shape = RoundedCornerShape(5.dp),
        colors = OutlinedTextFieldDefaults.colors(
            focusedContainerColor = Parchment,
            unfocusedContainerColor = Parchment,
            cursorColor = Ink,
            focusedBorderColor = Brass,
            unfocusedBorderColor = BrassDim.copy(alpha = 0.7f),
            focusedLabelColor = Ink,
            unfocusedLabelColor = ParchmentDeep,
        ),
    )
}
