package com.had.downloader.ui.theme

import android.content.Context
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp

private const val THEME_PREFS = "had_appearance_prefs"
private const val KEY_THEME = "selected_theme"

data class HadPalette(
    val spaceBlack: Color,
    val surfaceDark: Color,
    val elevatedSurf: Color,
    val borderColor: Color,
    val cyanPrimary: Color,
    val cyanGlow: Color,
    val cyanDim: Color,
    val greenSuccess: Color,
    val redError: Color,
    val orangeWarn: Color,
    val purpleAccent: Color,
    val textPrimary: Color,
    val textSecondary: Color,
    val textMuted: Color,
    
    val useMonoAccent: Boolean = false
)

enum class AppThemeType(
    val storageKey: String,
    val nameEn: String,
    val nameFa: String,
    val descriptionEn: String,
    val descriptionFa: String,
    val palette: HadPalette
) {
    DEFAULT_CYBER(
        storageKey = "default_cyber",
        nameEn = "Cyber Cyan",
        nameFa = "سایبر آبی",
        descriptionEn = "The original HAD look — deep space black with electric cyan.",
        descriptionFa = "ظاهر اصلی HAD — مشکی فضایی با آبی سایبری درخشان.",
        palette = HadPalette(
            spaceBlack = Color(0xFF080B14), surfaceDark = Color(0xFF0F1420),
            elevatedSurf = Color(0xFF161C2E), borderColor = Color(0xFF1E2640),
            cyanPrimary = Color(0xFF00D4FF), cyanGlow = Color(0x3300D4FF), cyanDim = Color(0xFF007A94),
            greenSuccess = Color(0xFF00FF88), redError = Color(0xFFFF4057), orangeWarn = Color(0xFFFF8C42),
            purpleAccent = Color(0xFF9B59FF),
            textPrimary = Color(0xFFE8EDF5), textSecondary = Color(0xFF6B7A99), textMuted = Color(0xFF3A4460)
        )
    ),
    ROSE_QUARTZ(
        storageKey = "rose_quartz",
        nameEn = "Rose Quartz",
        nameFa = "کوارتز صورتی",
        descriptionEn = "Chic and modern — plum-black surfaces with rose pink and lilac accents.",
        descriptionFa = "شیک و مدرن — زمینه‌ی مشکی‌بنفش با لهجه‌های صورتی رز و یاسی.",
        palette = HadPalette(
            spaceBlack = Color(0xFF1A0E14), surfaceDark = Color(0xFF241521),
            elevatedSurf = Color(0xFF321D2C), borderColor = Color(0xFF44293C),
            cyanPrimary = Color(0xFFFF6FA5), cyanGlow = Color(0x33FF6FA5), cyanDim = Color(0xFFB94C79),
            greenSuccess = Color(0xFF5EDCA6), redError = Color(0xFFFF5470), orangeWarn = Color(0xFFFFB86C),
            purpleAccent = Color(0xFFC78FFF),
            textPrimary = Color(0xFFFCEEF3), textSecondary = Color(0xFFCB9FB2), textMuted = Color(0xFF7A5468)
        )
    ),
    LAVENDER_BLOOM(
        storageKey = "lavender_bloom",
        nameEn = "Lavender Bloom",
        nameFa = "شکوفه‌ی یاسی",
        descriptionEn = "Soft and modern — dusky violet surfaces with lavender and pink glow.",
        descriptionFa = "لطیف و مدرن — زمینه‌ی بنفش گرد‌آلود با درخشش یاسی و صورتی.",
        palette = HadPalette(
            spaceBlack = Color(0xFF14101F), surfaceDark = Color(0xFF1C1730),
            elevatedSurf = Color(0xFF261F3F), borderColor = Color(0xFF362B54),
            cyanPrimary = Color(0xFFB794F6), cyanGlow = Color(0x33B794F6), cyanDim = Color(0xFF8464C4),
            greenSuccess = Color(0xFF7FE0C8), redError = Color(0xFFFF7597), orangeWarn = Color(0xFFFFC98B),
            purpleAccent = Color(0xFFFF9AD6),
            textPrimary = Color(0xFFF3EEFC), textSecondary = Color(0xFFB8A9D9), textMuted = Color(0xFF6B5F8A)
        )
    ),
    MATRIX_CLASSIC(
        storageKey = "matrix_classic",
        nameEn = "Matrix Classic",
        nameFa = "ماتریکس کلاسیک",
        descriptionEn = "Pure black terminal with classic phosphor-green glow.",
        descriptionFa = "ترمینال مشکی خالص با درخشش سبز فسفری کلاسیک.",
        palette = HadPalette(
            spaceBlack = Color(0xFF000400), surfaceDark = Color(0xFF020A03),
            elevatedSurf = Color(0xFF061206), borderColor = Color(0xFF0C2410),
            cyanPrimary = Color(0xFF00FF41), cyanGlow = Color(0x3300FF41), cyanDim = Color(0xFF00A82B),
            greenSuccess = Color(0xFF39FF6A), redError = Color(0xFFFF3131), orangeWarn = Color(0xFFCFFF3D),
            purpleAccent = Color(0xFF00FFAA),
            textPrimary = Color(0xFFC8FFD4), textSecondary = Color(0xFF5FA96E), textMuted = Color(0xFF276B37),
            useMonoAccent = true
        )
    ),
    NEON_MATRIX(
        storageKey = "neon_matrix",
        nameEn = "Neon Matrix",
        nameFa = "ماتریکس نئونی",
        descriptionEn = "Modern cyberpunk take on the matrix — neon green with glitchy magenta/cyan pops.",
        descriptionFa = "نسخه‌ی سایبرپانکی و مدرن ماتریکس — سبز نئونی با جرقه‌های صورتی و آبی.",
        palette = HadPalette(
            spaceBlack = Color(0xFF060608), surfaceDark = Color(0xFF0A0E0C),
            elevatedSurf = Color(0xFF101612), borderColor = Color(0xFF1B2A1E),
            cyanPrimary = Color(0xFF39FF14), cyanGlow = Color(0x3339FF14), cyanDim = Color(0xFF1FAE0C),
            greenSuccess = Color(0xFF00E5A0), redError = Color(0xFFFF2D6C), orangeWarn = Color(0xFFE8FF3B),
            purpleAccent = Color(0xFF00F0FF),
            textPrimary = Color(0xFFDDFFE4), textSecondary = Color(0xFF6FCF8E), textMuted = Color(0xFF335540),
            useMonoAccent = true
        )
    ),
    MIDNIGHT_ONYX(
        storageKey = "midnight_onyx",
        nameEn = "Midnight Onyx",
        nameFa = "عقیق نیمه‌شب",
        descriptionEn = "Elegant and understated — graphite black with warm champagne gold.",
        descriptionFa = "شیک و آرام — مشکی گرافیتی با طلایی گرم شامپاینی.",
        palette = HadPalette(
            spaceBlack = Color(0xFF0A0A0C), surfaceDark = Color(0xFF121214),
            elevatedSurf = Color(0xFF1A1A1E), borderColor = Color(0xFF28282E),
            cyanPrimary = Color(0xFFD4AF6A), cyanGlow = Color(0x33D4AF6A), cyanDim = Color(0xFF8F753F),
            greenSuccess = Color(0xFF4CD98A), redError = Color(0xFFE55B5B), orangeWarn = Color(0xFFE8A64C),
            purpleAccent = Color(0xFFB08BD0),
            textPrimary = Color(0xFFF0EDE6), textSecondary = Color(0xFF9A9590), textMuted = Color(0xFF56524C)
        )
    ),
    OBSIDIAN_ROYALE(
        storageKey = "obsidian_royale",
        nameEn = "Obsidian Royale",
        nameFa = "اوبسیدین سلطنتی",
        descriptionEn = "Rich and elegant — deep navy-black with royal indigo and violet.",
        descriptionFa = "غنی و شیک — سرمه‌ای تیره با آبی سلطنتی و بنفش.",
        palette = HadPalette(
            spaceBlack = Color(0xFF07070F), surfaceDark = Color(0xFF0E0E1C),
            elevatedSurf = Color(0xFF161628), borderColor = Color(0xFF232340),
            cyanPrimary = Color(0xFF6C7BFF), cyanGlow = Color(0x336C7BFF), cyanDim = Color(0xFF4750A8),
            greenSuccess = Color(0xFF3DDC97), redError = Color(0xFFFF5C7A), orangeWarn = Color(0xFFFFB157),
            purpleAccent = Color(0xFFA855F7),
            textPrimary = Color(0xFFEDEDF7), textSecondary = Color(0xFF9B9BC0), textMuted = Color(0xFF525270)
        )
    );

    companion object {
        fun fromStorageKey(key: String?): AppThemeType =
            entries.firstOrNull { it.storageKey == key } ?: DEFAULT_CYBER
    }
}

object ThemeManager {
    var current by mutableStateOf(AppThemeType.DEFAULT_CYBER)
        private set

    private var initialized = false

    fun init(context: Context) {
        if (initialized) return
        initialized = true
        val prefs = context.applicationContext.getSharedPreferences(THEME_PREFS, Context.MODE_PRIVATE)
        current = AppThemeType.fromStorageKey(prefs.getString(KEY_THEME, null))
    }

    fun setTheme(context: Context, theme: AppThemeType) {
        current = theme
        context.applicationContext
            .getSharedPreferences(THEME_PREFS, Context.MODE_PRIVATE)
            .edit().putString(KEY_THEME, theme.storageKey).apply()
    }
}

val SpaceBlack: Color get() = ThemeManager.current.palette.spaceBlack
val SurfaceDark: Color get() = ThemeManager.current.palette.surfaceDark
val ElevatedSurf: Color get() = ThemeManager.current.palette.elevatedSurf
val BorderColor: Color get() = ThemeManager.current.palette.borderColor

val CyanPrimary: Color get() = ThemeManager.current.palette.cyanPrimary
val CyanGlow: Color get() = ThemeManager.current.palette.cyanGlow
val CyanDim: Color get() = ThemeManager.current.palette.cyanDim

val GreenSuccess: Color get() = ThemeManager.current.palette.greenSuccess
val RedError: Color get() = ThemeManager.current.palette.redError
val OrangeWarn: Color get() = ThemeManager.current.palette.orangeWarn
val PurpleAccent: Color get() = ThemeManager.current.palette.purpleAccent

val TextPrimary: Color get() = ThemeManager.current.palette.textPrimary
val TextSecondary: Color get() = ThemeManager.current.palette.textSecondary
val TextMuted: Color get() = ThemeManager.current.palette.textMuted

val HadTypography: Typography
    get() {
        val mono = if (ThemeManager.current.palette.useMonoAccent) FontFamily.Monospace else FontFamily.Default
        return Typography(
            displayLarge = TextStyle(
                fontFamily = FontFamily.Default, fontWeight = FontWeight.Black,
                fontSize = 48.sp, letterSpacing = (-1.5).sp, color = TextPrimary
            ),
            displayMedium = TextStyle(
                fontFamily = FontFamily.Default, fontWeight = FontWeight.Bold,
                fontSize = 28.sp, letterSpacing = (-0.5).sp, color = TextPrimary
            ),
            headlineLarge = TextStyle(
                fontFamily = FontFamily.Default, fontWeight = FontWeight.Bold,
                fontSize = 22.sp, color = TextPrimary
            ),
            headlineMedium = TextStyle(
                fontFamily = FontFamily.Default, fontWeight = FontWeight.SemiBold,
                fontSize = 18.sp, color = TextPrimary
            ),
            titleLarge = TextStyle(
                fontFamily = FontFamily.Default, fontWeight = FontWeight.SemiBold,
                fontSize = 16.sp, color = TextPrimary
            ),
            titleMedium = TextStyle(
                fontFamily = FontFamily.Default, fontWeight = FontWeight.Medium,
                fontSize = 14.sp, color = TextPrimary
            ),
            bodyLarge = TextStyle(
                fontFamily = FontFamily.Default, fontWeight = FontWeight.Normal,
                fontSize = 16.sp, color = TextPrimary
            ),
            bodyMedium = TextStyle(
                fontFamily = FontFamily.Default, fontWeight = FontWeight.Normal,
                fontSize = 14.sp, color = TextSecondary
            ),
            bodySmall = TextStyle(
                fontFamily = mono, fontWeight = FontWeight.Normal,
                fontSize = 12.sp, color = TextSecondary
            ),
            labelLarge = TextStyle(
                fontFamily = FontFamily.Default, fontWeight = FontWeight.Medium,
                fontSize = 13.sp, letterSpacing = 0.5.sp, color = TextPrimary
            ),
            labelSmall = TextStyle(
                fontFamily = mono, fontWeight = FontWeight.Medium,
                fontSize = 10.sp, letterSpacing = 1.sp, color = TextSecondary
            )
        )
    }

private val currentColorScheme: ColorScheme
    @Composable get() {
        val p = ThemeManager.current.palette
        return darkColorScheme(
            primary = p.cyanPrimary,
            onPrimary = p.spaceBlack,
            primaryContainer = p.elevatedSurf,
            onPrimaryContainer = p.cyanPrimary,
            secondary = p.purpleAccent,
            onSecondary = p.spaceBlack,
            tertiary = p.greenSuccess,
            background = p.spaceBlack,
            surface = p.surfaceDark,
            surfaceVariant = p.elevatedSurf,
            onBackground = p.textPrimary,
            onSurface = p.textPrimary,
            onSurfaceVariant = p.textSecondary,
            outline = p.borderColor,
            error = p.redError,
        )
    }

@Composable
fun HADTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = currentColorScheme,
        typography  = HadTypography,
        content     = content
    )
}
