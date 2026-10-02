package com.example.ui.theme

import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext

val LocalAnimeSkin = staticCompositionLocalOf { AnimeSkinTheme.SAKURA_STARLIGHT }

fun getAnimeColorScheme(skin: AnimeSkinTheme, darkTheme: Boolean) = if (darkTheme) {
    // 🌙 夜间模式：借鉴新世纪福音战士 EVA / 赛博机甲 IP (深色炫彩背景 + 高反差亮字)
    darkColorScheme(
        primary = Color(0xFF00F5D4), // 觉醒荧光绿 (初号机觉醒)
        onPrimary = Color.Black,
        primaryContainer = Color(0xFF7B2CBF), // EVA 幻紫
        onPrimaryContainer = Color.White,
        secondary = Color(0xFFFF0055), // 暴走赤红
        onSecondary = Color.White,
        secondaryContainer = Color(0xFF240046),
        onSecondaryContainer = Color(0xFF00F5D4),
        tertiary = Color(0xFFFFB703),
        background = Color(0xFF0B0914), // 极夜机甲深色背景
        surface = Color(0xFF140D22),
        onBackground = Color(0xFFF8FAFC), // 极亮夜间白字
        onSurface = Color(0xFFFFFFFF),
        surfaceVariant = Color(0xFF1F1535),
        onSurfaceVariant = Color(0xFFCBD5E1),
        outline = Color(0xFF00F5D4).copy(alpha = 0.4f),
        outlineVariant = Color(0xFF7B2CBF).copy(alpha = 0.3f)
    )
} else {
    // ☀️ 日间模式：白底黑字，颜色借鉴百变小樱 (Sakura) 与 初音未来 (Miku) 热门 IP
    lightColorScheme(
        primary = Color(0xFFFF5D8F), // 魔法樱粉 (百变小樱)
        onPrimary = Color.White,
        primaryContainer = Color(0xFFFFEEF3),
        onPrimaryContainer = Color(0xFF9D174D),
        secondary = Color(0xFF00B4D8), // 初音未来青葱蓝
        onSecondary = Color.White,
        secondaryContainer = Color(0xFFE0F7FA),
        onSecondaryContainer = Color(0xFF0E7490),
        tertiary = Color(0xFFFFB703), // 魔法星光金
        onTertiary = Color.Black,
        tertiaryContainer = Color(0xFFFEF3C7),
        onTertiaryContainer = Color(0xFF92400E),
        background = Color(0xFFFFFFFF), // 纯白底色 (White background)
        surface = Color(0xFFFFFFFF),
        onBackground = Color(0xFF0F172A), // 深黑字 (Deep Black text)
        onSurface = Color(0xFF0F172A),     // 深黑字 (Deep Black text)
        surfaceVariant = Color(0xFFF8FAFC),
        onSurfaceVariant = Color(0xFF334155),
        outline = Color(0xFFFF5D8F).copy(alpha = 0.3f),
        outlineVariant = Color(0xFF00B4D8).copy(alpha = 0.2f)
    )
}

@Composable
fun MyApplicationTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    animeSkin: AnimeSkinTheme = AnimeSkinTheme.SAKURA_STARLIGHT,
    dynamicColor: Boolean = false,
    content: @Composable () -> Unit,
) {
    val colorScheme = when {
        dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> {
            val context = LocalContext.current
            if (darkTheme) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
        }
        else -> getAnimeColorScheme(animeSkin, darkTheme)
    }

    CompositionLocalProvider(LocalAnimeSkin provides animeSkin) {
        MaterialTheme(
            colorScheme = colorScheme,
            typography = Typography,
            content = content
        )
    }
}

