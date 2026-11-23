package com.fantasylibrary.android.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.fantasylibrary.android.data.network.Failure
import com.fantasylibrary.android.ui.theme.Brass
import com.fantasylibrary.android.ui.theme.BrassBright
import com.fantasylibrary.android.ui.theme.BrassDim
import com.fantasylibrary.android.ui.theme.DangerRose
import com.fantasylibrary.android.ui.theme.Ink
import com.fantasylibrary.android.ui.theme.InkGreen
import com.fantasylibrary.android.ui.theme.Parchment
import com.fantasylibrary.android.ui.theme.ParchmentDeep
import com.fantasylibrary.android.ui.theme.ParchmentDim
import com.fantasylibrary.android.ui.theme.ParchmentSoft
import com.fantasylibrary.android.ui.theme.SuccessGreen
import com.fantasylibrary.android.ui.theme.Walnut
import com.fantasylibrary.android.ui.theme.WalnutDeep
import com.fantasylibrary.android.ui.theme.WalnutEdge
import com.fantasylibrary.android.ui.theme.WarnAmber

/** 羊皮纸卡片：浅色底 + 黄铜边框，古书页的感觉。 */
@Composable
fun PaperCard(
    modifier: Modifier = Modifier,
    onClick: (() -> Unit)? = null,
    contentPadding: Int = 14,
    content: @Composable ColumnScope.() -> Unit,
) {
    val shape = RoundedCornerShape(9.dp)
    val border = BorderStroke(1.dp, BrassDim.copy(alpha = 0.85f))
    if (onClick != null) {
        Surface(
            onClick = onClick,
            modifier = modifier,
            shape = shape,
            color = Parchment,
            contentColor = Ink,
            border = border,
        ) {
            Column(Modifier.padding(contentPadding.dp), content = content)
        }
    } else {
        Surface(
            modifier = modifier,
            shape = shape,
            color = Parchment,
            contentColor = Ink,
            border = border,
        ) {
            Column(Modifier.padding(contentPadding.dp), content = content)
        }
    }
}

/** 深色皮质面板，用于技术信息 / 播放器容器。 */
@Composable
fun LeatherPanel(
    modifier: Modifier = Modifier,
    content: @Composable ColumnScope.() -> Unit,
) {
    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(9.dp),
        color = Walnut,
        contentColor = Parchment,
        border = BorderStroke(1.dp, WalnutEdge),
    ) {
        Column(Modifier.padding(14.dp), content = content)
    }
}

enum class Tone { Neutral, Ok, Warn, Bad, Brass }

@Composable
fun toneColor(tone: Tone): Color = when (tone) {
    Tone.Ok -> SuccessGreen
    Tone.Warn -> WarnAmber
    Tone.Bad -> DangerRose
    Tone.Brass -> BrassBright
    Tone.Neutral -> ParchmentDim
}

/** 状态小标签，例如「应用 UP」「数据库 UP」。 */
@Composable
fun StatusPill(
    text: String,
    tone: Tone = Tone.Neutral,
    modifier: Modifier = Modifier,
) {
    val color = toneColor(tone)
    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(50),
        color = color.copy(alpha = 0.18f),
        border = BorderStroke(1.dp, color.copy(alpha = 0.7f)),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(
                Modifier
                    .padding(start = 8.dp)
                    .size(5.dp)
                    .background(color, RoundedCornerShape(50)),
            )
            Text(
                text = text,
                modifier = Modifier.padding(start = 5.dp, end = 9.dp, top = 3.dp, bottom = 3.dp),
                style = androidx.compose.material3.MaterialTheme.typography.labelMedium,
                color = color,
            )
        }
    }
}

/** 区块标题：左侧书籍脊背 + 标题 + 可选右侧说明。 */
@Composable
fun SectionTitle(
    title: String,
    modifier: Modifier = Modifier,
    trailing: String? = null,
) {
    Row(
        modifier = modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        SpineMark()
        Spacer(Modifier.width(8.dp))
        Text(
            text = title,
            style = androidx.compose.material3.MaterialTheme.typography.titleMedium,
            color = Parchment,
        )
        if (trailing != null) {
            Spacer(Modifier.weight(1f))
            Text(
                text = trailing,
                style = androidx.compose.material3.MaterialTheme.typography.labelMedium,
                color = ParchmentDeep,
            )
        }
    }
}

/** 键值行，例如「HTTP 状态 / 200」。 */
@Composable
fun KeyValueRow(
    label: String,
    value: String,
    modifier: Modifier = Modifier,
    valueColor: Color = ParchmentSoft,
    mono: Boolean = false,
) {
    Row(
        modifier = modifier.fillMaxWidth().padding(vertical = 3.dp),
        verticalAlignment = Alignment.Top,
    ) {
        Text(
            text = label,
            modifier = Modifier.width(92.dp),
            style = androidx.compose.material3.MaterialTheme.typography.bodySmall,
            color = ParchmentDeep,
        )
        Text(
            text = value,
            modifier = Modifier.weight(1f),
            style = androidx.compose.material3.MaterialTheme.typography.bodySmall,
            color = valueColor,
            fontFamily = if (mono) FontFamily.Monospace else null,
        )
    }
}

/** 提示条：黄铜左竖线 + 说明文字。放在羊皮纸卡片里时用 onLight = true 换深色文字。 */
@Composable
fun NotePanel(
    text: String,
    modifier: Modifier = Modifier,
    accent: Color = BrassDim,
    onLight: Boolean = false,
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .background(
                color = if (onLight) accent.copy(alpha = 0.16f) else accent.copy(alpha = 0.10f),
                shape = RoundedCornerShape(6.dp),
            )
            .padding(horizontal = 10.dp, vertical = 8.dp),
    ) {
        Box(
            Modifier
                .width(2.dp)
                .height(14.dp)
                .background(accent, RoundedCornerShape(2.dp)),
        )
        Spacer(Modifier.width(8.dp))
        Text(
            text = text,
            style = androidx.compose.material3.MaterialTheme.typography.bodySmall,
            color = if (onLight) Ink else ParchmentSoft,
        )
    }
}

@Composable
fun LoadingPanel(
    text: String = "正在检索馆藏…",
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier.fillMaxWidth().padding(vertical = 32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        CircularProgressIndicator(
            modifier = Modifier.size(26.dp),
            color = Brass,
            strokeWidth = 2.dp,
        )
        Spacer(Modifier.height(12.dp))
        Text(
            text = text,
            style = androidx.compose.material3.MaterialTheme.typography.bodySmall,
            color = ParchmentDim,
        )
    }
}

/** 错误面板：分类标题 + 具体原因 + 后端 code / HTTP 状态 + 重试。 */
@Composable
fun FailurePanel(
    failure: Failure,
    modifier: Modifier = Modifier,
    onRetry: (() -> Unit)? = null,
) {
    Surface(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(9.dp),
        color = DangerRose.copy(alpha = 0.08f),
        border = BorderStroke(1.dp, DangerRose.copy(alpha = 0.45f)),
    ) {
        Column(Modifier.padding(14.dp)) {
            Text(
                text = failure.title,
                style = androidx.compose.material3.MaterialTheme.typography.titleSmall,
                color = DangerRose,
            )
            Spacer(Modifier.height(4.dp))
            Text(
                text = failure.message,
                style = androidx.compose.material3.MaterialTheme.typography.bodyMedium,
                color = Parchment,
            )
            val meta = buildList {
                failure.httpStatus?.let { add("HTTP $it") }
                failure.code?.let { add(it) }
            }.joinToString(" · ")
            if (meta.isNotEmpty()) {
                Spacer(Modifier.height(6.dp))
                Text(
                    text = meta,
                    style = androidx.compose.material3.MaterialTheme.typography.labelSmall,
                    color = ParchmentDeep,
                )
            }
            if (!failure.kind.hint.isNullOrBlank()) {
                Spacer(Modifier.height(6.dp))
                Text(
                    text = "可能原因：${failure.kind.hint}",
                    style = androidx.compose.material3.MaterialTheme.typography.bodySmall,
                    color = ParchmentDim,
                )
            }
            if (!failure.detail.isNullOrBlank()) {
                Spacer(Modifier.height(6.dp))
                Text(
                    text = failure.detail,
                    style = androidx.compose.material3.MaterialTheme.typography.bodySmall,
                    color = ParchmentDeep,
                    fontFamily = FontFamily.Monospace,
                    fontSize = 10.5.sp,
                )
            }
            if (onRetry != null) {
                Spacer(Modifier.height(10.dp))
                SecondaryButton(text = "重新尝试", onClick = onRetry)
            }
        }
    }
}

@Composable
fun EmptyPanel(
    text: String,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier.fillMaxWidth().padding(vertical = 36.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text(
            text = "◈",
            color = BrassDim,
            style = androidx.compose.material3.MaterialTheme.typography.headlineSmall,
        )
        Spacer(Modifier.height(8.dp))
        Text(
            text = text,
            style = androidx.compose.material3.MaterialTheme.typography.bodySmall,
            color = ParchmentDim,
            textAlign = TextAlign.Center,
        )
    }
}

@Composable
fun PrimaryButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    loading: Boolean = false,
) {
    Button(
        onClick = onClick,
        modifier = modifier.height(44.dp),
        enabled = enabled && !loading,
        shape = RoundedCornerShape(6.dp),
        colors = ButtonDefaults.buttonColors(
            containerColor = Brass,
            contentColor = WalnutDeep,
            disabledContainerColor = BrassDim.copy(alpha = 0.35f),
            disabledContentColor = ParchmentDeep,
        ),
        border = BorderStroke(1.dp, BrassBright.copy(alpha = 0.6f)),
    ) {
        if (loading) {
            CircularProgressIndicator(
                modifier = Modifier.size(15.dp),
                color = WalnutDeep,
                strokeWidth = 2.dp,
            )
            Spacer(Modifier.width(8.dp))
        }
        Text(text = text, style = androidx.compose.material3.MaterialTheme.typography.labelLarge)
    }
}

@Composable
fun SecondaryButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
) {
    OutlinedButton(
        onClick = onClick,
        modifier = modifier.height(40.dp),
        enabled = enabled,
        shape = RoundedCornerShape(6.dp),
        border = BorderStroke(1.dp, BrassDim),
        colors = ButtonDefaults.outlinedButtonColors(
            contentColor = Brass,
            disabledContentColor = ParchmentDeep,
        ),
    ) {
        Text(text = text, style = androidx.compose.material3.MaterialTheme.typography.labelMedium)
    }
}

/** 小号可点击文字按钮（放在卡片右上角）。 */
@Composable
fun LinkAction(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    color: Color = Brass,
) {
    Text(
        text = text,
        modifier = modifier
            .clickable(onClick = onClick)
            .padding(horizontal = 6.dp, vertical = 4.dp),
        style = androidx.compose.material3.MaterialTheme.typography.labelMedium,
        color = color,
    )
}

/** 页面标题栏。 */
@Composable
fun LibraryTopBar(
    title: String,
    subtitle: String? = null,
    onBack: (() -> Unit)? = null,
    actions: @Composable () -> Unit = {},
) {
    Column(
        Modifier
            .fillMaxWidth()
            .statusBarsPadding(),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            if (onBack != null) {
                Box(
                    Modifier
                        .clickable(onClick = onBack)
                        .padding(end = 12.dp, top = 4.dp, bottom = 4.dp),
                ) {
                    Text(
                        text = "‹ 返回",
                        style = androidx.compose.material3.MaterialTheme.typography.labelLarge,
                        color = Brass,
                    )
                }
            }
            Column(Modifier.weight(1f)) {
                Text(
                    text = title,
                    style = androidx.compose.material3.MaterialTheme.typography.headlineSmall,
                    color = Parchment,
                )
                if (!subtitle.isNullOrBlank()) {
                    Text(
                        text = subtitle,
                        style = androidx.compose.material3.MaterialTheme.typography.labelSmall,
                        color = ParchmentDim,
                    )
                }
            }
            actions()
        }
        BrassDivider(Modifier.fillMaxWidth().padding(horizontal = 16.dp))
    }
}

/** 全屏底色容器：深胡桃木 + 墨绿晕染。 */
@Composable
fun LibrarySurface(
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit,
) {
    Box(
        modifier = modifier
            .fillMaxSize()
            .libraryBackground(),
    ) {
        content()
    }
}

@Composable
fun VerticalGap(height: Int) = Spacer(Modifier.height(height.dp))

@Composable
fun HorizontalGap(width: Int) = Spacer(Modifier.width(width.dp))

/** 卡片里的标签行（墨绿小圆角）。 */
@Composable
fun TagChips(
    tags: List<String>,
    modifier: Modifier = Modifier,
    maxShown: Int = 6,
) {
    if (tags.isEmpty()) return
    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(5.dp),
    ) {
        tags.take(maxShown).forEach { tag ->
            Surface(
                shape = RoundedCornerShape(3.dp),
                color = InkGreen.copy(alpha = 0.9f),
                contentColor = ParchmentSoft,
            ) {
                Text(
                    text = tag,
                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                    style = androidx.compose.material3.MaterialTheme.typography.labelSmall,
                    fontWeight = FontWeight.Normal,
                )
            }
        }
    }
}
