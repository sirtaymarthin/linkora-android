package com.linkora.ui

import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

val Accent = Color(0xFF5E5CE6)
val AccentDark = Color(0xFF7B79FF)
val Ok = Color(0xFF30A46C)
val Danger = Color(0xFFE5484D)
val Heart = Color(0xFFE5484D)
val WhatsApp = Color(0xFF25D366)

private val LightScheme = lightColorScheme(
    primary = Accent,
    onPrimary = Color.White,
    secondary = Accent,
    background = Color(0xFFF6F7F9),
    onBackground = Color(0xFF15171C),
    surface = Color.White,
    onSurface = Color(0xFF15171C),
    surfaceVariant = Color(0xFFEFF1F5),
    onSurfaceVariant = Color(0xFF7A8089),
    outlineVariant = Color(0xFFE8EAEF),
    error = Danger
)

private val DarkScheme = darkColorScheme(
    primary = AccentDark,
    onPrimary = Color.White,
    secondary = AccentDark,
    background = Color(0xFF0F1116),
    onBackground = Color(0xFFF1F2F4),
    surface = Color(0xFF191C23),
    onSurface = Color(0xFFF1F2F4),
    surfaceVariant = Color(0xFF222630),
    onSurfaceVariant = Color(0xFF8B919C),
    outlineVariant = Color(0xFF262A33),
    error = Danger
)

private val LinkoraShapes = Shapes(
    extraSmall = androidx.compose.foundation.shape.RoundedCornerShape(8.dp),
    small = androidx.compose.foundation.shape.RoundedCornerShape(12.dp),
    medium = androidx.compose.foundation.shape.RoundedCornerShape(16.dp),
    large = androidx.compose.foundation.shape.RoundedCornerShape(22.dp),
    extraLarge = androidx.compose.foundation.shape.RoundedCornerShape(28.dp)
)

private val LinkoraType = Typography().let { t ->
    t.copy(
        headlineSmall = t.headlineSmall.copy(fontWeight = FontWeight.ExtraBold, letterSpacing = (-0.8).sp),
        titleLarge = t.titleLarge.copy(fontWeight = FontWeight.ExtraBold, letterSpacing = (-0.6).sp),
        titleMedium = t.titleMedium.copy(fontWeight = FontWeight.Bold),
        bodyMedium = t.bodyMedium.copy(lineHeight = 20.sp),
        labelSmall = t.labelSmall.copy(fontWeight = FontWeight.Bold, letterSpacing = 0.6.sp)
    )
}

@Composable
fun LinkoraTheme(
    dark: Boolean = isSystemInDarkTheme(),
    dynamic: Boolean = true,
    content: @Composable () -> Unit
) {
    val ctx = LocalContext.current
    val scheme = when {
        dynamic && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S ->
            if (dark) dynamicDarkColorScheme(ctx) else dynamicLightColorScheme(ctx)
        dark -> DarkScheme
        else -> LightScheme
    }
    MaterialTheme(colorScheme = scheme, shapes = LinkoraShapes, typography = LinkoraType, content = content)
}
