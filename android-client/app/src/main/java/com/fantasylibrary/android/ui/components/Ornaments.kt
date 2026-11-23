package com.fantasylibrary.android.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.fantasylibrary.android.ui.theme.Brass
import com.fantasylibrary.android.ui.theme.BrassDim
import com.fantasylibrary.android.ui.theme.InkGreen
import com.fantasylibrary.android.ui.theme.Parchment
import com.fantasylibrary.android.ui.theme.Walnut
import com.fantasylibrary.android.ui.theme.WalnutDeep
import kotlin.math.max

/**
 * 页面的「深胡桃木 + 墨绿晕染」底纹。
 * 用 Canvas 画，而不是图片资源，保证任意分辨率下都清晰且不增加包体。
 */
fun Modifier.libraryBackground(): Modifier = this.drawBehind {
    drawRect(Brush.verticalGradient(listOf(WalnutDeep, Walnut, WalnutDeep)))
    // 顶部墨绿晕染，模拟旧图书馆的阴影
    drawRect(
        Brush.verticalGradient(
            colors = listOf(InkGreen.copy(alpha = 0.55f), Color.Transparent),
            startY = 0f,
            endY = size.height * 0.38f,
        ),
    )
    // 木纹细线
    val step = 23.dp.toPx()
    var y = step
    while (y < size.height) {
        drawLine(
            color = Parchment.copy(alpha = 0.035f),
            start = Offset(0f, y),
            end = Offset(size.width, y + 1.5f),
            strokeWidth = 1f,
        )
        y += step
    }
}

/** 黄铜细线 + 中央菱形，作为段落分隔符。 */
@Composable
fun BrassDivider(
    modifier: Modifier = Modifier,
    color: Color = BrassDim,
    diamond: Boolean = true,
) {
    Canvas(modifier = modifier.height(10.dp)) {
        val midY = size.height / 2f
        val gap = size.width * 0.06f
        val lineEnd = size.width / 2f - gap
        drawLine(color.copy(alpha = 0.7f), Offset(0f, midY), Offset(lineEnd, midY), strokeWidth = 1f)
        drawLine(color.copy(alpha = 0.7f), Offset(size.width - lineEnd, midY), Offset(size.width, midY), strokeWidth = 1f)
        if (diamond) {
            val r = size.height * 0.34f
            val center = Offset(size.width / 2f, midY)
            val path = Path().apply {
                moveTo(center.x, center.y - r)
                lineTo(center.x + r, center.y)
                lineTo(center.x, center.y + r)
                lineTo(center.x - r, center.y)
                close()
            }
            drawPath(path, color)
        }
    }
}

/** 在任意内容之上叠加四角黄铜角饰（用 drawWithContent，保证画在子内容之后）。 */
fun Modifier.brassCornersOverlay(
    color: Color = Brass,
    inset: Dp = 5.dp,
    length: Dp = 11.dp,
): Modifier = this.drawWithContent {
    drawContent()
    drawCornerMarks(color, inset.toPx(), length.toPx())
}

private fun androidx.compose.ui.graphics.drawscope.DrawScope.drawCornerMarks(
    color: Color,
    insetPx: Float,
    lengthPx: Float,
) {
    val stroke = 1.1.dp.toPx()
    val w = size.width
    val h = size.height
    val i = insetPx
    val l = lengthPx
    val corners = listOf(
        Triple(Offset(i, i), Offset(i + l, i), Offset(i, i + l)),
        Triple(Offset(w - i, i), Offset(w - i - l, i), Offset(w - i, i + l)),
        Triple(Offset(i, h - i), Offset(i + l, h - i), Offset(i, h - i - l)),
        Triple(Offset(w - i, h - i), Offset(w - i - l, h - i), Offset(w - i, h - i - l)),
    )
    corners.forEach { (origin, horizontal, vertical) ->
        drawLine(color.copy(alpha = 0.8f), origin, horizontal, strokeWidth = stroke, cap = StrokeCap.Round)
        drawLine(color.copy(alpha = 0.8f), origin, vertical, strokeWidth = stroke, cap = StrokeCap.Round)
    }
}

enum class RuneKind { Link, Status, Anime, Games, Play }

/**
 * 底部导航 / 列表用的极简符号。
 * 手绘 Path，避免引入 material-icons-extended（几十 MB 的依赖）。
 */
@Composable
fun RuneIcon(
    kind: RuneKind,
    modifier: Modifier = Modifier,
    tint: Color = Brass,
    size: Dp = 18.dp,
) {
    Canvas(modifier = modifier.size(size)) {
        val w = this.size.width
        val h = this.size.height
        val stroke = Stroke(
            width = max(1.3f, w * 0.085f),
            cap = StrokeCap.Round,
            join = StrokeJoin.Round,
        )
        when (kind) {
            RuneKind.Link -> {
                drawCircle(tint, radius = w * 0.24f, center = Offset(w * 0.36f, h * 0.5f), style = stroke)
                drawCircle(tint, radius = w * 0.24f, center = Offset(w * 0.64f, h * 0.5f), style = stroke)
                drawLine(tint, Offset(w * 0.5f, h * 0.34f), Offset(w * 0.5f, h * 0.66f), strokeWidth = stroke.width)
            }
            RuneKind.Status -> {
                drawCircle(tint.copy(alpha = 0.55f), radius = w * 0.36f, center = Offset(w * 0.5f, h * 0.5f), style = stroke)
                drawCircle(tint, radius = w * 0.14f, center = Offset(w * 0.5f, h * 0.5f))
            }
            RuneKind.Anime -> {
                val path = Path().apply {
                    moveTo(w * 0.16f, h * 0.28f)
                    quadraticTo(w * 0.34f, h * 0.2f, w * 0.5f, h * 0.32f)
                    quadraticTo(w * 0.66f, h * 0.2f, w * 0.84f, h * 0.28f)
                    lineTo(w * 0.84f, h * 0.76f)
                    quadraticTo(w * 0.66f, h * 0.68f, w * 0.5f, h * 0.8f)
                    quadraticTo(w * 0.34f, h * 0.68f, w * 0.16f, h * 0.76f)
                    close()
                }
                drawPath(path, tint, style = stroke)
                drawLine(tint, Offset(w * 0.5f, h * 0.32f), Offset(w * 0.5f, h * 0.79f), strokeWidth = stroke.width)
            }
            RuneKind.Games -> {
                drawRoundRect(
                    color = tint,
                    topLeft = Offset(w * 0.14f, h * 0.24f),
                    size = androidx.compose.ui.geometry.Size(w * 0.72f, h * 0.52f),
                    cornerRadius = androidx.compose.ui.geometry.CornerRadius(w * 0.14f),
                    style = stroke,
                )
                drawLine(tint, Offset(w * 0.5f, h * 0.36f), Offset(w * 0.5f, h * 0.64f), strokeWidth = stroke.width)
                drawLine(tint, Offset(w * 0.36f, h * 0.5f), Offset(w * 0.64f, h * 0.5f), strokeWidth = stroke.width)
            }
            RuneKind.Play -> {
                drawCircle(tint.copy(alpha = 0.6f), radius = w * 0.38f, center = Offset(w * 0.5f, h * 0.5f), style = stroke)
                val path = Path().apply {
                    moveTo(w * 0.42f, h * 0.34f)
                    lineTo(w * 0.68f, h * 0.5f)
                    lineTo(w * 0.42f, h * 0.66f)
                    close()
                }
                drawPath(path, tint)
            }
        }
    }
}

/** 极小的“书籍脊背”装饰条，用在区块标题左侧。 */
@Composable
fun SpineMark(modifier: Modifier = Modifier, color: Color = Brass) {
    Box(modifier = modifier.size(width = 4.dp, height = 18.dp)) {
        Canvas(Modifier.size(width = 4.dp, height = 18.dp)) {
            drawRect(color.copy(alpha = 0.9f))
            drawRect(
                color = InkGreen,
                topLeft = Offset(0f, size.height * 0.3f),
                size = androidx.compose.ui.geometry.Size(size.width, size.height * 0.4f),
            )
        }
    }
}
