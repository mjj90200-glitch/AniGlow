package com.aniglow.app.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.Typography
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.unit.dp

val FireflyGreen = Color(0xFF00C853)
val FireflyBright = Color(0xFF00E676)
val SkyBlue = Color(0xFF4098C3)
val SakuraPink = Color(0xFFFF9AAE)
val WarmBackground = Color(0xFFFDFBF7)

private val AniGlowColorScheme = lightColorScheme(
    primary = FireflyGreen,
    onPrimary = Color.White,
    primaryContainer = Color(0xFFDDFBE8),
    onPrimaryContainer = Color(0xFF00541C),
    secondary = SkyBlue,
    onSecondary = Color.White,
    secondaryContainer = Color(0xFFE5F4FA),
    onSecondaryContainer = Color(0xFF164E68),
    tertiary = SakuraPink,
    background = WarmBackground,
    onBackground = Color(0xFF20241F),
    surface = Color.White,
    onSurface = Color(0xFF20241F),
    surfaceVariant = Color(0xFFF1EFEA),
    onSurfaceVariant = Color(0xFF696E68),
    outline = Color(0xFFD4D2CC),
    error = Color(0xFFBA1A1A),
)

private val AniGlowShapes = Shapes(
    extraSmall = RoundedCornerShape(12.dp),
    small = RoundedCornerShape(16.dp),
    medium = RoundedCornerShape(20.dp),
    large = RoundedCornerShape(28.dp),
    extraLarge = RoundedCornerShape(32.dp),
)

@Composable
fun AniGlowTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = AniGlowColorScheme,
        typography = Typography(),
        shapes = AniGlowShapes,
        content = content,
    )
}
