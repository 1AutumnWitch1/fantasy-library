package com.fantasylibrary.android.ui.screens.login

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.fantasylibrary.android.ui.components.*
import com.fantasylibrary.android.ui.theme.InkGreen

@Composable
fun LoginScreen(viewModel: LoginViewModel, onBack: () -> Unit, onSettings: () -> Unit, onSignedIn: () -> Unit = onBack) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    LaunchedEffect(state.session.signedIn, state.busy) {
        if (state.session.signedIn && !state.busy) onSignedIn()
    }
    ScrollRoom("读者登录", onBack, backLabel = "返回") {
        Column(Modifier.fillMaxSize().imePadding().verticalScroll(rememberScrollState()).padding(18.dp),
            verticalArrangement = Arrangement.spacedBy(13.dp)) {
            ScrollHeading("登录幻想图书馆")
            PaperTile(Modifier.fillMaxWidth()) {
                Column(Modifier.padding(15.dp), verticalArrangement = Arrangement.spacedBy(11.dp)) {
                    PageField(state.username, viewModel::onUsernameChange, "账号名", Modifier.fillMaxWidth())
                    OutlinedTextField(state.password, viewModel::onPasswordChange, Modifier.fillMaxWidth(),
                        label = { Text("密码") }, singleLine = true, visualTransformation = PasswordVisualTransformation(),
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                        colors = OutlinedTextFieldDefaults.colors(focusedTextColor = PageInk, unfocusedTextColor = PageInk,
                            focusedBorderColor = InkGreen, unfocusedBorderColor = PageLine,
                            focusedLabelColor = InkGreen, unfocusedLabelColor = PageMuted, cursorColor = InkGreen))
                    state.error?.let { Text(it.readerMessage(), color = PageMuted, style = MaterialTheme.typography.bodyMedium) }
                    PageButton(if (state.busy) "登录中…" else "登录", viewModel::login, state.canSubmit, emphasized = true)
                }
            }
            if (state.baseUrl.isBlank()) PageStatus("登录前需要连接图书馆服务器。", actionLabel = "连接设置", action = onSettings)
        }
    }
}
