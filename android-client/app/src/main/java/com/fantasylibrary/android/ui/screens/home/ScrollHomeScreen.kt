package com.fantasylibrary.android.ui.screens.home

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.GenericShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.*
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.layout.boundsInRoot
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.LocalWindowInfo
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.fantasylibrary.android.R
import com.fantasylibrary.android.ui.components.BrassDivider
import com.fantasylibrary.android.ui.components.ReaderAvatar
import com.fantasylibrary.android.ui.screens.personal.ReaderIdentityViewModel
import com.fantasylibrary.android.ui.theme.*

enum class LibraryScroll(val route: String, val title: String, val seal: String, val ink: Color) {
    Anime("anime", "追番", "☾", Color(0xFF703345)),
    Games("games", "游戏", "⚔", Color(0xFF315747)),
    Community("community", "社区", "❖", Color(0xFF3B516C)),
    Recommendations("recommendations", "推荐", "✧", Color(0xFF79582C)),
    Personal("personal", "个人书架", "♙", Color(0xFF65435B)),
}

@Composable
fun ScrollHomeScreen(active: Boolean, identity: ReaderIdentityViewModel.State, cleartext: Boolean,
    onOpen: (LibraryScroll, Float) -> Unit, onProfile: () -> Unit,
) {
    val window = LocalWindowInfo.current.containerSize
    var selected by remember { mutableStateOf<LibraryScroll?>(null) }
    LaunchedEffect(active) { if (active) selected = null }
    Box(Modifier.fillMaxSize().background(WalnutDeep)) {
        Image(painterResource(R.drawable.scroll_hall), null, Modifier.fillMaxSize(), contentScale = ContentScale.Crop)
        Box(Modifier.fillMaxSize().background(Brush.verticalGradient(
            listOf(Color(0xA515100C), Color(0x5422180F), Color(0xD7110D0A)),
        )))
        BoxWithConstraints(Modifier.fillMaxSize().windowInsetsPadding(WindowInsets.safeDrawing)) {
            val compact = maxHeight < 670.dp
            val spacing = if (compact) 9.dp else 14.dp
            val height = ((maxHeight - (if (compact) 109.dp else 157.dp) - spacing * 4) / 5).coerceIn(61.dp, 103.dp)
            Column(Modifier.align(Alignment.TopCenter).widthIn(max = 560.dp).fillMaxWidth()
                .verticalScroll(rememberScrollState()).padding(bottom = 13.dp),
                horizontalAlignment = Alignment.CenterHorizontally) {
                Box(Modifier.fillMaxWidth().height(if (compact) 64.dp else 78.dp).padding(horizontal = 21.dp)) {
                    Text("馆藏书架", color = Parchment, modifier = Modifier.align(Alignment.CenterStart),
                        style = MaterialTheme.typography.headlineMedium.copy(letterSpacing = 3.sp))
                    val person = identity.profile
                    val avatarModifier = Modifier.align(Alignment.CenterEnd).size(48.dp).clip(CircleShape)
                        .clickable(role = Role.Button, onClick = onProfile)
                        .semantics { contentDescription = "个人资料" }
                        .border(1.dp, Brass, CircleShape)
                    if (person?.avatarKey.isNullOrBlank()) {
                        Canvas(avatarModifier.background(InkGreenDeep)) {
                            val center = Offset(size.width * .5f, size.height * .35f)
                            drawCircle(BrassBright, size.width * .118f, center, style = Stroke(1.35.dp.toPx()))
                            val inset = size.width * .235f
                            drawArc(BrassBright, 187f, 166f, false, Offset(inset, size.height * .52f),
                                Size(size.width - 2 * inset, size.height * .38f), style = Stroke(1.35.dp.toPx(), cap = StrokeCap.Round))
                        }
                    } else ReaderAvatar(person?.avatarKey, identity.base, cleartext, "", avatarModifier)
                }
                BrassDivider(Modifier.fillMaxWidth(.87f))
                Spacer(Modifier.height(if (compact) 13.dp else 25.dp))
                Column(Modifier.fillMaxWidth(.87f), verticalArrangement = Arrangement.spacedBy(spacing)) {
                    LibraryScroll.entries.forEach { scroll ->
                        var centerY by remember { mutableFloatStateOf(0f) }
                        HorizontalScroll(scroll, selected == scroll, Modifier.fillMaxWidth().height(height)
                            .onGloballyPositioned { centerY = it.boundsInRoot().center.y }) {
                            if (active && selected == null) {
                                selected = scroll
                                onOpen(scroll, (2f * centerY / window.height.coerceAtLeast(1) - 1f).coerceIn(-1f, 1f))
                            }
                        }
                    }
                }
                Spacer(Modifier.height(if (compact) 12.dp else 23.dp))
                BrassDivider(Modifier.width(94.dp))
            }
        }
    }
}

private val RolledPaper = GenericShape { size, _ ->
    val edge = size.height * .085f
    moveTo(0f, edge)
    quadraticTo(size.width * .5f, -edge * .3f, size.width, edge)
    lineTo(size.width, size.height - edge)
    quadraticTo(size.width * .5f, size.height + edge * .3f, 0f, size.height - edge)
    close()
}

@Composable
private fun HorizontalScroll(scroll: LibraryScroll, selected: Boolean, modifier: Modifier, onClick: () -> Unit) {
    val press by animateFloatAsState(if (selected) 1f else 0f, tween(180), label = "卷轴选中")
    Box(modifier.graphicsLayer {
        scaleX = 1f + .035f * press
        scaleY = 1f + .08f * press
    }.clickable(role = Role.Button, onClick = onClick), contentAlignment = Alignment.Center) {
        val shaftWidth = 24.dp
        Box(Modifier.fillMaxSize().padding(horizontal = 17.dp, vertical = 5.dp)
            .shadow(8.dp, RolledPaper).clip(RolledPaper).background(Color(0xFFEAD0A0))) {
            Image(painterResource(R.drawable.scroll_paper), null, Modifier.matchParentSize(), contentScale = ContentScale.Crop)
            Box(Modifier.matchParentSize().background(
                Brush.verticalGradient(0f to Color(0xA0653C1D), .20f to Color(0x00FFFFFF),
                    .46f to Color(0x38FFF5CA), .75f to Color(0x08FFFFFF), 1f to Color(0xB1734925)),
            ))
            Box(Modifier.matchParentSize().background(Brush.horizontalGradient(
                0f to Color(0xAD543119), .13f to Color.Transparent, .87f to Color.Transparent, 1f to Color(0xA64A2A19),
            )))
            Canvas(Modifier.matchParentSize().padding(horizontal = 12.dp, vertical = 11.dp)) {
                val stroke = .65.dp.toPx()
                val edge = 6.dp.toPx()
                drawRoundRect(scroll.ink.copy(alpha = .55f), cornerRadius = CornerRadius(edge), style = Stroke(stroke))
                val mark = 3.dp.toPx()
                for (x in listOf(edge * 2f, size.width - edge * 2f)) {
                    val y = size.height / 2
                    val gem = Path().apply { moveTo(x, y - mark); lineTo(x + mark, y); lineTo(x, y + mark); lineTo(x - mark, y); close() }
                    drawPath(gem, scroll.ink.copy(alpha = .65f))
                }
            }
            Row(Modifier.align(Alignment.Center), verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(11.dp)) {
                Text(scroll.seal, color = scroll.ink, fontSize = 23.sp)
                Text(scroll.title, color = Color(0xFF3E2718), style = MaterialTheme.typography.titleLarge.copy(
                    fontWeight = FontWeight.SemiBold, letterSpacing = 1.3.sp))
            }
        }
        RollEnd(Modifier.align(Alignment.CenterStart).width(shaftWidth).fillMaxHeight())
        RollEnd(Modifier.align(Alignment.CenterEnd).width(shaftWidth).fillMaxHeight())
    }
}

@Composable
private fun RollEnd(modifier: Modifier) {
    Canvas(modifier) {
        val w = size.width
        val h = size.height
        val cap = h * .165f
        drawRoundRect(Color(0xA6140B06), Offset(w * .10f, cap * .53f), Size(w * .87f, h - cap * .83f), CornerRadius(w * .28f))
        val wood = Brush.horizontalGradient(listOf(Color(0xFF241309), Color(0xFF73482C), Color(0xFF4A2A19), Color(0xFF231309)))
        drawRoundRect(wood, Offset(w * .13f, cap * .50f), Size(w * .74f, h - cap), CornerRadius(w * .20f))
        for (ratio in listOf(.34f, .52f, .70f)) {
            drawLine(Color(0x38E4B87F), Offset(w * ratio, cap), Offset(w * ratio - 1.dp.toPx(), h - cap), .75.dp.toPx())
        }
        val brass = Brush.horizontalGradient(listOf(Color(0xFF523818), Color(0xFFF0CF88), Color(0xFFA47738), Color(0xFF543516)))
        for (y in listOf(0f, h - cap)) {
            drawRoundRect(brass, Offset(0f, y + cap * .15f), Size(w, cap * .76f), CornerRadius(cap * .24f))
            drawOval(Color(0xFFD3AC61), Offset(w * .02f, y), Size(w * .96f, cap * .53f))
            drawOval(Color(0xFF59401D), Offset(w * .20f, y + cap * .10f), Size(w * .60f, cap * .30f))
            drawOval(Color(0xFF8C6733), Offset(w * .30f, y + cap * .15f), Size(w * .40f, cap * .19f))
        }
    }
}
