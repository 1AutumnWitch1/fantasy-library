package com.fantasylibrary.android.ui.theme

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.unit.dp

private val LibraryColorScheme = darkColorScheme(
    primary = Brass,
    onPrimary = WalnutDeep,
    primaryContainer = BrassDim,
    onPrimaryContainer = Parchment,
    secondary = InkGreenSoft,
    onSecondary = Parchment,
    secondaryContainer = InkGreen,
    onSecondaryContainer = Parchment,
    tertiary = ParchmentDim,
    onTertiary = WalnutDeep,
    background = WalnutDeep,
    onBackground = Parchment,
    surface = Walnut,
    onSurface = Parchment,
    surfaceVariant = WalnutLight,
    onSurfaceVariant = ParchmentSoft,
    surfaceContainer = Walnut,
    surfaceContainerHigh = WalnutLight,
    surfaceContainerHighest = WalnutLight,
    surfaceContainerLow = WalnutDeep,
    surfaceContainerLowest = WalnutDeep,
    outline = BrassDim,
    outlineVariant = WalnutEdge,
    error = DangerRose,
    onError = WalnutDeep,
    scrim = WalnutDeep,
)

private val LibraryShapes = Shapes(
    extraSmall = RoundedCornerShape(3.dp),
    small = RoundedCornerShape(5.dp),
    medium = RoundedCornerShape(8.dp),
    large = RoundedCornerShape(12.dp),
    extraLarge = RoundedCornerShape(18.dp),
)

@Composable
fun LibraryTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = LibraryColorScheme,
        typography = LibraryTypography,
        shapes = LibraryShapes,
        content = content,
    )
}
