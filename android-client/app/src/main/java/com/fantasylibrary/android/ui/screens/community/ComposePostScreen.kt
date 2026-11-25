package com.fantasylibrary.android.ui.screens.community

import android.content.ContentResolver
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.fantasylibrary.android.data.network.*
import com.fantasylibrary.android.di.AppContainer
import com.fantasylibrary.android.ui.components.*
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.MultipartBody
import okhttp3.RequestBody.Companion.toRequestBody

@Composable
fun ComposePostScreen(container: AppContainer, onBack: () -> Unit, onLogin: () -> Unit, onPublished: (Long) -> Unit) {
    val session by container.authRepository.session.collectAsStateWithLifecycle()
    val settings by container.settingsRepository.settings.collectAsStateWithLifecycle(initialValue = null)
    val resolver = LocalContext.current.contentResolver
    val scope = rememberCoroutineScope()
    var title by rememberSaveable { mutableStateOf("") }
    var body by rememberSaveable { mutableStateOf("") }
    var topic by rememberSaveable { mutableStateOf(Topic.GENERAL) }
    var images by remember { mutableStateOf<List<Uri>>(emptyList()) }
    var error by remember { mutableStateOf<String?>(null) }
    var busy by remember { mutableStateOf(false) }
    val picker = rememberLauncherForActivityResult(ActivityResultContracts.GetMultipleContents()) { selected ->
        if (selected.size > 4) error = "最多可附上 4 张图片。"
        images = selected.take(4)
    }
    ScrollRoom("撰写手稿", onBack, backLabel = "返回") {
        if (!session.signedIn) {
            PageStatus("登录后即可发表手稿。", actionLabel = "登录图书馆", action = onLogin)
            return@ScrollRoom
        }
        Column(Modifier.fillMaxSize().imePadding().verticalScroll(rememberScrollState()).padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)) {
            ScrollHeading("新手稿")
            LazyRow(horizontalArrangement = Arrangement.spacedBy(7.dp)) {
                items(Topic.entries, key = { it.name }) { option -> PageChip(option.label, topic == option) { topic = option } }
            }
            PageField(title, { title = it.take(100) }, "标题", Modifier.fillMaxWidth())
            PageField(body, { body = it.take(6000) }, "正文", Modifier.fillMaxWidth().heightIn(min = 150.dp), singleLine = false)
            Row {
                PageButton(if (images.isEmpty()) "添加图片" else "已选 ${images.size} 张图片", { picker.launch("image/*") }, enabled = !busy)
                if (images.isNotEmpty()) PageButton("移除配图", { images = emptyList() }, enabled = !busy)
            }
            error?.let { Text(it, color = PageMuted, style = MaterialTheme.typography.bodyMedium) }
            PageButton(if (busy) "正在发表…" else "发表手稿", enabled = !busy && title.isNotBlank() && body.isNotBlank(),
                emphasized = true, onClick = {
                    val base = settings?.baseUrl.orEmpty()
                    if (base.isBlank()) error = "请先填写服务器地址。"
                    else {
                        busy = true
                        error = null
                        scope.launch {
                            try {
                                val attachments = withContext(Dispatchers.IO) { images.mapIndexed { i, uri -> imagePart(resolver, uri, i) } }
                                when (val result = container.readerRepository.publish(base, PostDraft(title.trim(), body.trim(), topic.name), attachments)) {
                                    is Outcome.Ok -> onPublished(result.value.id)
                                    is Outcome.Err -> error = result.failure.readerMessage()
                                }
                            } catch (cancelled: CancellationException) {
                                throw cancelled
                            } catch (problem: Exception) {
                                error = (problem as? IllegalArgumentException)?.message ?: "无法读取所选图片。"
                            } finally { busy = false }
                        }
                    }
                })
        }
    }
}

private fun imagePart(resolver: ContentResolver, uri: Uri, index: Int): MultipartBody.Part {
    val mime = resolver.getType(uri)
    require(mime == "image/jpeg" || mime == "image/png") { "请选择 JPG 或 PNG 图片。" }
    val limit = 5 * 1024 * 1024
    val bytes = resolver.openInputStream(uri)?.use { input ->
        val output = java.io.ByteArrayOutputStream()
        val buffer = ByteArray(8192)
        while (output.size() <= limit) {
            val count = input.read(buffer)
            if (count < 0) break
            output.write(buffer, 0, count)
        }
        output.toByteArray()
    }
    require(bytes != null && bytes.isNotEmpty()) { "无法读取所选图片。" }
    require(bytes.size <= limit) { "单张图片不能超过 5 MB。" }
    val extension = if (mime == "image/png") "png" else "jpg"
    return MultipartBody.Part.createFormData("images", "image-${index + 1}.$extension", bytes.toRequestBody(mime.toMediaType()))
}
