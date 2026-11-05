package com.ysfclc.mosaic.core.design

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

object MosaicPalette {
    val Canvas = Color(0xFF080B14)
    val Surface = Color(0xFF111725)
    val SurfaceRaised = Color(0xFF182235)
    val SurfaceGlass = Color(0xB51B2940)
    val TextPrimary = Color(0xFFF3F5FF)
    val TextSecondary = Color(0xFF9BA7BD)
    val TextMuted = Color(0xFF64728B)
    val Violet = Color(0xFF9B8CFF)
    val VioletDeep = Color(0xFF7164E8)
    val Cyan = Color(0xFF47DED2)
    val Blue = Color(0xFF6B9CFF)
    val Lime = Color(0xFFC5E89A)
    val Divider = Color(0xFF263147)
}

private val MosaicDarkScheme = darkColorScheme(
    primary = MosaicPalette.Violet,
    onPrimary = Color(0xFF100D26),
    secondary = MosaicPalette.Cyan,
    onSecondary = Color(0xFF06201E),
    tertiary = MosaicPalette.Blue,
    background = MosaicPalette.Canvas,
    onBackground = MosaicPalette.TextPrimary,
    surface = MosaicPalette.Surface,
    onSurface = MosaicPalette.TextPrimary,
    surfaceVariant = MosaicPalette.SurfaceRaised,
    onSurfaceVariant = MosaicPalette.TextSecondary,
    outline = MosaicPalette.Divider,
)

@Composable
fun MosaicTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = MosaicDarkScheme,
        content = content,
    )
}
