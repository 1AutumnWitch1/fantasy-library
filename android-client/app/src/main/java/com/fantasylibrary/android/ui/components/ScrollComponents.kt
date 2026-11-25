package com.fantasylibrary.android.ui.components

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.fantasylibrary.android.R
import com.fantasylibrary.android.data.network.Failure
import com.fantasylibrary.android.data.network.MediaUrls
import com.fantasylibrary.android.ui.theme.*

val PagePaper = Color(0xFFF0DEB6)
val PageInk = Color(0xFF30231A)
val PageMuted = Color(0xFF654B35)
val PageLine = Color(0xFFAB8859)
val PageCard = Color(0xEAF9EAC9)

@Composable
fun ScrollRoom(title: String, onBack: () -> Unit, backLabel: String = "收卷", actions: @Composable RowScope.() -> Unit = {}, content: @Composable ColumnScope.() -> Unit) {
    Column(Modifier.fillMaxSize().background(InkGreenDeep).windowInsetsPadding(WindowInsets.safeDrawing)) {
        Row(Modifier.fillMaxWidth().padding(horizontal = 12.dp).heightIn(min = 57.dp),
            verticalAlignment = Alignment.CenterVertically) {
            TextButton(onClick = onBack, colors = ButtonDefaults.textButtonColors(contentColor = BrassBright)) { Text("‹ $backLabel") }
            Text(title, color = Parchment, style = MaterialTheme.typography.titleLarge, modifier = Modifier.weight(1f).padding(start = 5.dp))
            actions()
        }
        Box(Modifier.fillMaxWidth().height(2.dp).background(BrassDim))
        Box(Modifier.weight(1f).fillMaxWidth().background(PagePaper)) {
            Image(painterResource(R.drawable.scroll_paper), null, Modifier.matchParentSize(), contentScale = ContentScale.Crop, alpha = .20f)
            Column(Modifier.fillMaxSize(), content = content)
        }
    }
}

@Composable
fun ScrollHeading(title: String, description: String? = null) {
    Column(verticalArrangement = Arrangement.spacedBy(5.dp)) {
        Text(title, color = PageInk, style = MaterialTheme.typography.headlineSmall)
        if (!description.isNullOrBlank()) Text(description, color = PageMuted, style = MaterialTheme.typography.bodyMedium)
        HorizontalDivider(color = PageLine.copy(alpha = .55f), modifier = Modifier.padding(top = 3.dp))
    }
}

@Composable
fun PageButton(label: String, onClick: () -> Unit, enabled: Boolean = true, emphasized: Boolean = false) {
    if (emphasized) {
        Button(onClick = onClick, enabled = enabled, shape = RoundedCornerShape(7.dp),
            colors = ButtonDefaults.buttonColors(containerColor = InkGreen, contentColor = Parchment)) { Text(label) }
    } else {
        TextButton(onClick = onClick, enabled = enabled,
            colors = ButtonDefaults.textButtonColors(contentColor = InkGreen)) { Text(label) }
    }
}

@Composable
fun PageChip(label: String, selected: Boolean, onClick: () -> Unit) {
    FilterChip(selected = selected, onClick = onClick, label = { Text(label) },
        modifier = Modifier.heightIn(min = 48.dp),
        colors = FilterChipDefaults.filterChipColors(containerColor = PageCard, labelColor = PageInk,
            selectedContainerColor = InkGreen, selectedLabelColor = Parchment))
}

@Composable
fun PageField(value: String, onChange: (String) -> Unit, label: String, modifier: Modifier = Modifier, singleLine: Boolean = true) {
    OutlinedTextField(value, onChange, modifier, label = { Text(label) }, singleLine = singleLine,
        shape = RoundedCornerShape(8.dp), colors = OutlinedTextFieldDefaults.colors(
            focusedTextColor = PageInk, unfocusedTextColor = PageInk, focusedBorderColor = InkGreen,
            unfocusedBorderColor = PageLine, focusedLabelColor = InkGreen, unfocusedLabelColor = PageMuted,
            cursorColor = InkGreen, focusedContainerColor = PageCard, unfocusedContainerColor = PageCard,
        ))
}

@Composable
fun PageStatus(text: String, busy: Boolean = false, actionLabel: String = "重试", action: (() -> Unit)? = null) {
    Column(Modifier.fillMaxWidth().padding(horizontal = 20.dp, vertical = 26.dp),
        horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(8.dp)) {
        if (busy) CircularProgressIndicator(Modifier.size(25.dp), color = InkGreen, strokeWidth = 2.dp)
        Text(text, color = PageMuted, textAlign = TextAlign.Center, style = MaterialTheme.typography.bodyMedium)
        if (action != null) PageButton(actionLabel, action, emphasized = true)
    }
}

fun Failure.readerMessage(): String = message.ifBlank { title }

@Composable
fun CoverArt(check: MediaUrls.Check?, label: String?, modifier: Modifier = Modifier, mark: String = "✧") {
    Box(modifier.background(InkGreenDeep), contentAlignment = Alignment.Center) {
        Text(mark, color = BrassDim, style = MaterialTheme.typography.headlineSmall)
        if (check?.ok == true) AsyncImage(check.resolved, label, Modifier.matchParentSize(), contentScale = ContentScale.Crop)
    }
}

@Composable
fun ReaderAvatar(key: String?, baseUrl: String, cleartext: Boolean, name: String, modifier: Modifier = Modifier.size(36.dp)) {
    val image = key?.takeIf { it.isNotBlank() }?.let { MediaUrls.resolve("/api/avatars/" + android.net.Uri.encode(it), baseUrl, cleartext) }
    CoverArt(image, null, modifier.clip(CircleShape), name.firstOrNull()?.toString() ?: "✧")
}

@Composable
fun PaperTile(modifier: Modifier = Modifier, onClick: (() -> Unit)? = null, content: @Composable ColumnScope.() -> Unit) {
    val shape = RoundedCornerShape(9.dp)
    val border = androidx.compose.foundation.BorderStroke(1.dp, PageLine.copy(alpha = .55f))
    if (onClick == null) Surface(modifier, shape, PageCard, PageInk, border = border) { Column(content = content) }
    else Surface(onClick, modifier, shape = shape, color = PageCard, contentColor = PageInk, border = border) { Column(content = content) }
}
