package com.example.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.example.model.ThemeMode

@Immutable
data class GlassPalette(
    val isDark: Boolean,
    val glassFillTop: Color,
    val glassFillMid: Color,
    val glassFillBottom: Color,
    val glassCardFill: Color,
    val glassBorderHighlight: Color,
    val glassBorderEdge: Color,
    val glassSpecular: Color,
    val glassRefractionTint: Color,
    val tapGlowPrimary: Color,
    val tapGlowSecondary: Color,
    val ambientAuroraOne: Color,
    val ambientAuroraTwo: Color,
    val ambientAuroraThree: Color,
    val islandSurface: Color,
    val islandBorder: Color,
    val shimmerHighlight: Color
)

private val DarkGlassPalette = GlassPalette(
    isDark = true,
    glassFillTop = Color(0x38FFFFFF),
    glassFillMid = Color(0x18D8F3FF),
    glassFillBottom = Color(0x10FFFFFF),
    glassCardFill = Color(0x22FFFFFF),
    glassBorderHighlight = Color(0x88FFFFFF),
    glassBorderEdge = Color(0x2E22D3EE),
    glassSpecular = Color(0x48FFFFFF),
    glassRefractionTint = Color(0x3822D3EE),
    tapGlowPrimary = Color(0xFF22D3EE),
    tapGlowSecondary = Color(0xFFFF2D55),
    ambientAuroraOne = Color(0x4DFF2D55),
    ambientAuroraTwo = Color(0x4522D3EE),
    ambientAuroraThree = Color(0x3A8B5CF6),
    islandSurface = Color(0xDE090E1B),
    islandBorder = Color(0x7022D3EE),
    shimmerHighlight = Color(0x52FFFFFF)
)

private val LightGlassPalette = GlassPalette(
    isDark = false,
    glassFillTop = Color(0xBEFFFFFF),
    glassFillMid = Color(0x7CEAF6FF),
    glassFillBottom = Color(0x5CFFFFFF),
    glassCardFill = Color(0x9EFFFFFF),
    glassBorderHighlight = Color(0xFFFFFFFF),
    glassBorderEdge = Color(0x450284C7),
    glassSpecular = Color(0xAAFFFFFF),
    glassRefractionTint = Color(0x4438BDF8),
    tapGlowPrimary = Color(0xFF0284C7),
    tapGlowSecondary = Color(0xFFE11D48),
    ambientAuroraOne = Color(0x3AE11D48),
    ambientAuroraTwo = Color(0x3D0284C7),
    ambientAuroraThree = Color(0x307C3AED),
    islandSurface = Color(0xE60B1222),
    islandBorder = Color(0x7838BDF8),
    shimmerHighlight = Color(0xB8FFFFFF)
)

val LocalGlassPalette = staticCompositionLocalOf { DarkGlassPalette }

private val DarkColorScheme = darkColorScheme(
    primary = GlassCrimson,
    onPrimary = Color.White,
    primaryContainer = Color(0xFF4A0D20),
    onPrimaryContainer = Color(0xFFFFD9DF),
    secondary = GlassCyan,
    onSecondary = Color(0xFF002026),
    secondaryContainer = Color(0xFF0A3340),
    onSecondaryContainer = Color(0xFFB5F5FF),
    tertiary = GlassViolet,
    onTertiary = Color.White,
    background = GlassObsidianBg,
    onBackground = TextOnDark,
    surface = GlassObsidianSurface,
    onSurface = TextOnDark,
    surfaceVariant = GlassObsidianSurfaceVariant,
    onSurfaceVariant = TextSecondaryDark,
    outline = Color(0x33FFFFFF),
    outlineVariant = Color(0x1AFFFFFF)
)

private val LightColorScheme = lightColorScheme(
    primary = GlassCrimsonLight,
    onPrimary = Color.White,
    primaryContainer = Color(0xFFFFE4E8),
    onPrimaryContainer = Color(0xFF4C0519),
    secondary = GlassCyanLight,
    onSecondary = Color.White,
    secondaryContainer = Color(0xFFE0F2FE),
    onSecondaryContainer = Color(0xFF0C4A6E),
    tertiary = GlassVioletLight,
    onTertiary = Color.White,
    background = GlassIceBg,
    onBackground = TextOnLight,
    surface = GlassIceSurface,
    onSurface = TextOnLight,
    surfaceVariant = GlassIceSurfaceVariant,
    onSurfaceVariant = TextSecondaryLight,
    outline = Color(0x260F172A),
    outlineVariant = Color(0x140F172A)
)

private val GlassShapes = Shapes(
    small = RoundedCornerShape(12.dp),
    medium = RoundedCornerShape(20.dp),
    large = RoundedCornerShape(26.dp),
    extraLarge = RoundedCornerShape(32.dp)
)

@Composable
fun GlassTubeTheme(
    themeMode: ThemeMode = ThemeMode.SYSTEM,
    content: @Composable () -> Unit
) {
    val systemDark = isSystemInDarkTheme()
    val useDarkTheme = when (themeMode) {
        ThemeMode.DARK -> true
        ThemeMode.LIGHT -> false
        ThemeMode.SYSTEM -> systemDark
    }

    val colorScheme = if (useDarkTheme) DarkColorScheme else LightColorScheme
    val glassPalette = if (useDarkTheme) DarkGlassPalette else LightGlassPalette

    CompositionLocalProvider(LocalGlassPalette provides glassPalette) {
        MaterialTheme(
            colorScheme = colorScheme,
            typography = GlassTypography,
            shapes = GlassShapes,
            content = content
        )
    }
}
