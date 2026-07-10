package com.had.downloader.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp

val SpaceBlack   = Color(0xFF080B14)   
val SurfaceDark  = Color(0xFF0F1420)   
val ElevatedSurf = Color(0xFF161C2E)   
val BorderColor  = Color(0xFF1E2640)   

val CyanPrimary  = Color(0xFF00D4FF)   
val CyanGlow     = Color(0x3300D4FF)   
val CyanDim      = Color(0xFF007A94)   

val GreenSuccess = Color(0xFF00FF88)   
val RedError     = Color(0xFFFF4057)   
val OrangeWarn   = Color(0xFFFF8C42)   
val PurpleAccent = Color(0xFF9B59FF)   

val TextPrimary  = Color(0xFFE8EDF5)
val TextSecondary= Color(0xFF6B7A99)
val TextMuted    = Color(0xFF3A4460)

val HadTypography = Typography(
    displayLarge = TextStyle(
        fontFamily = FontFamily.Default,
        fontWeight = FontWeight.Black,
        fontSize = 48.sp,
        letterSpacing = (-1.5).sp,
        color = TextPrimary
    ),
    displayMedium = TextStyle(
        fontFamily = FontFamily.Default,
        fontWeight = FontWeight.Bold,
        fontSize = 28.sp,
        letterSpacing = (-0.5).sp,
        color = TextPrimary
    ),
    headlineLarge = TextStyle(
        fontFamily = FontFamily.Default,
        fontWeight = FontWeight.Bold,
        fontSize = 22.sp,
        color = TextPrimary
    ),
    headlineMedium = TextStyle(
        fontFamily = FontFamily.Default,
        fontWeight = FontWeight.SemiBold,
        fontSize = 18.sp,
        color = TextPrimary
    ),
    titleLarge = TextStyle(
        fontFamily = FontFamily.Default,
        fontWeight = FontWeight.SemiBold,
        fontSize = 16.sp,
        color = TextPrimary
    ),
    titleMedium = TextStyle(
        fontFamily = FontFamily.Default,
        fontWeight = FontWeight.Medium,
        fontSize = 14.sp,
        color = TextPrimary
    ),
    bodyLarge = TextStyle(
        fontFamily = FontFamily.Default,
        fontWeight = FontWeight.Normal,
        fontSize = 16.sp,
        color = TextPrimary
    ),
    bodyMedium = TextStyle(
        fontFamily = FontFamily.Default,
        fontWeight = FontWeight.Normal,
        fontSize = 14.sp,
        color = TextSecondary
    ),
    bodySmall = TextStyle(
        fontFamily = FontFamily.Monospace,
        fontWeight = FontWeight.Normal,
        fontSize = 12.sp,
        color = TextSecondary
    ),
    labelLarge = TextStyle(
        fontFamily = FontFamily.Default,
        fontWeight = FontWeight.Medium,
        fontSize = 13.sp,
        letterSpacing = 0.5.sp,
        color = TextPrimary
    ),
    labelSmall = TextStyle(
        fontFamily = FontFamily.Monospace,
        fontWeight = FontWeight.Medium,
        fontSize = 10.sp,
        letterSpacing = 1.sp,
        color = TextSecondary
    )
)

private val DarkColorScheme = darkColorScheme(
    primary          = CyanPrimary,
    onPrimary        = SpaceBlack,
    primaryContainer = Color(0xFF003545),
    onPrimaryContainer = CyanPrimary,
    secondary        = PurpleAccent,
    onSecondary      = SpaceBlack,
    tertiary         = GreenSuccess,
    background       = SpaceBlack,
    surface          = SurfaceDark,
    surfaceVariant   = ElevatedSurf,
    onBackground     = TextPrimary,
    onSurface        = TextPrimary,
    onSurfaceVariant = TextSecondary,
    outline          = BorderColor,
    error            = RedError,
)

@Composable
fun HADTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = DarkColorScheme,
        typography  = HadTypography,
        content     = content
    )
}
