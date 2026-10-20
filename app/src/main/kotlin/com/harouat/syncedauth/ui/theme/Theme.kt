package com.harouat.syncedauth.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp

/**
 * Design tokens for SyncedAuth, following the Proton Authenticator system:
 * warm stone-gray pages, white rounded cards, big monospaced codes, and a single
 * rich blue accent reserved for interactive elements — matching the flower keyhole logo.
 */
@Immutable
data class SaColors(
    val bgTop: Color,
    val bgBottom: Color,
    val card: Color,
    val cardBorder: Color,
    val textNormal: Color,
    val textWeak: Color,
    val textPlaceholder: Color,
    val accent: Color,
    val focus: Color,
    val gradTop: Color,
    val gradBottom: Color,
    val inputFill: Color,
    val inputBorder: Color,
    val pillFill: Color,
    val iconTileBg: Color,
    val success: Color,
    val warning: Color,
    val danger: Color,
    val error: Color,
    val snackbarBg: Color,
    val snackbarText: Color,
) {
    val pageGradient: Brush get() = Brush.verticalGradient(listOf(bgTop, bgBottom))
    val buttonGradient: Brush get() = Brush.verticalGradient(listOf(gradTop, gradBottom))
}

val LightColors = SaColors(
    bgTop = Color(0xFFF5F5F4),
    bgBottom = Color(0xFFFAFAF9),
    card = Color(0xFFFFFFFF),
    cardBorder = Color(0xFFECECEC),
    textNormal = Color(0xFF44403C),
    textWeak = Color(0xFF78716C),
    textPlaceholder = Color(0xFF9E9894),
    accent = Color(0xFF2272E6),
    focus = Color(0xFF6FA8FF),
    gradTop = Color(0xFF3E86F2),
    gradBottom = Color(0xFF1748A9),
    inputFill = Color(0x0A000000),
    inputBorder = Color(0x0F000000),
    pillFill = Color(0x15000000),
    iconTileBg = Color(0xFFE9F1FC),
    success = Color(0xFF4AB89A),
    warning = Color(0xFFFFB879),
    danger = Color(0xFFFF7979),
    error = Color(0xFFCC2D4F),
    snackbarBg = Color(0xF03A3835),
    snackbarText = Color(0xFFFFFFFF),
)

val DarkColors = SaColors(
    bgTop = Color(0xFF2D2A28),
    bgBottom = Color(0xFF161514),
    card = Color(0xFF373535),
    cardBorder = Color(0x1FFFFFFF),
    textNormal = Color(0xFFFFFFFF),
    textWeak = Color(0xFFDFDFDF),
    textPlaceholder = Color(0x66FFFFFF),
    accent = Color(0xFF8FBBFF),
    focus = Color(0xFF8FBBFF),
    gradTop = Color(0xFF3E86F2),
    gradBottom = Color(0xFF1748A9),
    inputFill = Color(0x7D000000),
    inputBorder = Color(0x1FFFFFFF),
    pillFill = Color(0xFF3A3836),
    iconTileBg = Color(0xFF242A33),
    success = Color(0xFF4AB89A),
    warning = Color(0xFFFFB879),
    danger = Color(0xFFFF7979),
    error = Color(0xFFF08FA4),
    snackbarBg = Color(0xFFF5F5F4),
    snackbarText = Color(0xFF2D2A28),
)

val LocalSaColors = staticCompositionLocalOf { LightColors }

/** Typography tokens (sp sizes / line heights from the reference style guide). */
object SaType {
    val title = TextStyle(fontSize = 32.sp, lineHeight = 38.sp, fontWeight = FontWeight.Bold)
    val subtitle = TextStyle(fontSize = 26.sp, lineHeight = 38.sp, fontWeight = FontWeight.Bold)
    val headline = TextStyle(fontSize = 20.sp, lineHeight = 26.sp, fontWeight = FontWeight.Bold)
    val body18 = TextStyle(fontSize = 18.sp, lineHeight = 22.sp, fontWeight = FontWeight.Normal)
    val body1 = TextStyle(fontSize = 15.sp, lineHeight = 22.sp, fontWeight = FontWeight.Normal)
    val body1Medium = TextStyle(fontSize = 15.sp, lineHeight = 22.sp, fontWeight = FontWeight.SemiBold)
    val body1Bold = TextStyle(fontSize = 16.sp, lineHeight = 22.sp, fontWeight = FontWeight.Bold)
    val body2 = TextStyle(fontSize = 14.sp, lineHeight = 18.sp, fontWeight = FontWeight.Normal)
    val body2Medium = TextStyle(fontSize = 13.sp, lineHeight = 18.sp, fontWeight = FontWeight.SemiBold)
    val body3 = TextStyle(fontSize = 13.sp, lineHeight = 16.sp, fontWeight = FontWeight.Normal)
    val caption = TextStyle(fontSize = 12.sp, lineHeight = 14.sp, fontWeight = FontWeight.Normal)
    val compact = TextStyle(fontSize = 12.sp, lineHeight = 18.sp, fontWeight = FontWeight.SemiBold)
    val code = TextStyle(fontFamily = FontFamily.Monospace, fontSize = 28.sp, fontWeight = FontWeight.SemiBold)
    val monoNext = TextStyle(fontFamily = FontFamily.Monospace, fontSize = 16.sp, lineHeight = 18.sp, fontWeight = FontWeight.Normal)
}

@Composable
fun SyncedAuthTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit,
) {
    val sa = if (darkTheme) DarkColors else LightColors
    val scheme = if (darkTheme) {
        darkColorScheme(
            primary = sa.accent,
            onPrimary = Color.White,
            surface = sa.card,
            onSurface = sa.textNormal,
            background = sa.bgBottom,
            onBackground = sa.textNormal,
            error = sa.error,
            outlineVariant = sa.cardBorder,
        )
    } else {
        lightColorScheme(
            primary = sa.accent,
            onPrimary = Color.White,
            surface = sa.card,
            onSurface = sa.textNormal,
            background = sa.bgBottom,
            onBackground = sa.textNormal,
            error = sa.error,
            outlineVariant = sa.cardBorder,
        )
    }
    MaterialTheme(colorScheme = scheme) {
        CompositionLocalProvider(LocalSaColors provides sa) {
            content()
        }
    }
}
