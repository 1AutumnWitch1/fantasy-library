package com.fantasylibrary.android.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import coil.compose.AsyncImagePainter
import coil.request.ImageRequest
import com.fantasylibrary.android.data.network.MediaUrls
import com.fantasylibrary.android.ui.theme.Brass
import com.fantasylibrary.android.ui.theme.ParchmentDeep
import com.fantasylibrary.android.ui.theme.ParchmentDim
import com.fantasylibrary.android.ui.theme.WalnutLight

/**
 * 真实图片加载：URL 来自后端，加载失败可重试。
 *
 * 如果地址被 [MediaUrls] 判定为不可达（回环地址 / Docker 内部名 / 明文被禁），
 * 直接显示原因，而不是给用户一个空白框。
 */
@Composable
fun RemoteImage(
    check: MediaUrls.Check,
    contentDescription: String?,
    modifier: Modifier = Modifier,
    contentScale: ContentScale = ContentScale.Crop,
) {
    if (!check.ok) {
        Box(
            modifier = modifier.background(WalnutLight),
            contentAlignment = Alignment.Center,
        ) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier.padding(10.dp),
            ) {
                Text(
                    text = "◈",
                    color = Brass.copy(alpha = 0.5f),
                    style = MaterialTheme.typography.titleLarge,
                )
                Spacer(Modifier.height(4.dp))
                Text(
                    text = "地址不可用",
                    style = MaterialTheme.typography.labelSmall,
                    color = ParchmentDim,
                )
                Text(
                    text = check.reason.orEmpty(),
                    style = MaterialTheme.typography.bodySmall,
                    color = ParchmentDeep,
                    textAlign = TextAlign.Center,
                )
            }
        }
        return
    }

    var attempt by remember(check.resolved) { mutableIntStateOf(0) }
    var failed by remember(check.resolved, attempt) { mutableStateOf(false) }
    val context = LocalContext.current

    val request = remember(check.resolved, attempt) {
        ImageRequest.Builder(context)
            .data(check.resolved)
            .crossfade(true)
            // 用不同的缓存 key 触发真正的重新请求，而不是直接读缓存里的失败结果
            .memoryCacheKey("${check.resolved}#$attempt")
            .diskCacheKey("${check.resolved}#$attempt")
            .build()
    }

    Box(modifier = modifier.background(WalnutLight)) {
        AsyncImage(
            model = request,
            contentDescription = contentDescription,
            modifier = Modifier.fillMaxSize(),
            contentScale = contentScale,
            onState = { state ->
                failed = state is AsyncImagePainter.State.Error
            },
        )
        if (failed) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .background(WalnutLight.copy(alpha = 0.86f))
                    .padding(8.dp),
                verticalArrangement = androidx.compose.foundation.layout.Arrangement.Center,
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                Text(
                    text = "图片加载失败",
                    style = MaterialTheme.typography.labelSmall,
                    color = ParchmentDim,
                    textAlign = TextAlign.Center,
                )
                Spacer(Modifier.height(6.dp))
                SecondaryButton(text = "重试", onClick = { attempt += 1 })
            }
        }
    }
}
