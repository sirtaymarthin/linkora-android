package com.linkora.ui

import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

val Accent = Color(0xFF6C63FF)
val AccentLight = Color(0xFF9D97FF)
val AccentDark = Color(0xFF8B83FF)
val Ok = Color(0xFF22C55E)
val Danger = Color(0xFFEF4444)
val Heart = Color(0xFFEF4444)
val WhatsApp = Color(0xFF25D366)

val RingGradient = Brush.sweepGradient(
    listOf(Color(0xFFFF6B6B), Color(0xFFFFB938), Color(0xFF6C63FF), Color(0xFFFF6B6B))
)
val RingInactive = Brush.sweepGradient(listOf(Color(0xFFD1D5DB), Color(0xFFD1D5DB)))

private val LightScheme = lightColorScheme(
    primary = Accent,
    onPrimary = Color.White,
    secondary = AccentLight,
    background = Color(0xFFF8F9FC),
    onBackground = Color(0xFF1A1C22),
    surface = Color.White,
    onSurface = Color(0xFF1A1C22),
    surfaceVariant = Color(0xFFF0F1F5),
    onSurfaceVariant = Color(0xFF6E7280),
    outlineVariant = Color(0xFFE5E7EB),
    error = Danger
)

private val DarkScheme = darkColorScheme(
    primary = AccentDark,
    onPrimary = Color.White,
    secondary = AccentLight,
    background = Color(0xFF0C0D12),
    onBackground = Color(0xFFF1F2F5),
    surface = Color(0xFF161820),
    onSurface = Color(0xFFF1F2F5),
    surfaceVariant = Color(0xFF1F222C),
    onSurfaceVariant = Color(0xFF9499A5),
    outlineVariant = Color(0xFF282C36),
    error = Danger
)

private val LinkoraShapes = Shapes(
    extraSmall = androidx.compose.foundation.shape.RoundedCornerShape(10.dp),
    small = androidx.compose.foundation.shape.RoundedCornerShape(14.dp),
    medium = androidx.compose.foundation.shape.RoundedCornerShape(18.dp),
    large = androidx.compose.foundation.shape.RoundedCornerShape(24.dp),
    extraLarge = androidx.compose.foundation.shape.RoundedCornerShape(30.dp)
)

private val LinkoraType = Typography().let { t ->
    t.copy(
        headlineSmall = t.headlineSmall.copy(fontWeight = FontWeight.Black, letterSpacing = (-1).sp),
        titleLarge = t.titleLarge.copy(fontWeight = FontWeight.ExtraBold, letterSpacing = (-0.7).sp),
        titleMedium = t.titleMedium.copy(fontWeight = FontWeight.Bold, letterSpacing = (-0.3).sp),
        bodyLarge = t.bodyLarge.copy(fontWeight = FontWeight.Medium),
        bodyMedium = t.bodyMedium.copy(lineHeight = 21.sp),
        labelSmall = t.labelSmall.copy(fontWeight = FontWeight.Bold, letterSpacing = 0.5.sp)
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
