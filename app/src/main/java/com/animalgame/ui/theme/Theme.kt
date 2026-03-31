package com.animalgame.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

/**
 * 主题颜色定义
 */

// 浅色主题颜色
private val LightColorScheme = lightColorScheme(
    primary = Color(0xFFFF7043),           // 橙色主色
    onPrimary = Color.White,
    primaryContainer = Color(0xFFFFECB3),
    onPrimaryContainer = Color(0xFF5D4037),
    secondary = Color(0xFF81C784),          // 绿色
    onSecondary = Color.White,
    secondaryContainer = Color(0xFFC8E6C9),
    onSecondaryContainer = Color(0xFF2E7D32),
    tertiary = Color(0xFF64B5F6),             // 蓝色
    onTertiary = Color.White,
    tertiaryContainer = Color(0xFFBBDEFB),
    onTertiaryContainer = Color(0xFF1565C0),
    error = Color(0xFFE57373),
    onError = Color.White,
    errorContainer = Color(0xFFFFCDD2),
    onErrorContainer = Color(0xFFC62828),
    background = Color(0xFFFFF8E1),           // 暖黄背景
    onBackground = Color(0xFF5D4037),
    surface = Color.White,
    onSurface = Color(0xFF5D4037),
    surfaceVariant = Color(0xFFF5F0E6),
    onSurfaceVariant = Color(0xFF8D6E63),
    outline = Color(0xFFBDBDBD)
)

// 深色主题颜色
private val DarkColorScheme = darkColorScheme(
    primary = Color(0xFFFFAB91),              // 浅橙主色
    onPrimary = Color(0xFF3E2723),
    primaryContainer = Color(0xFFBF5F32),
    onPrimaryContainer = Color(0xFFFFECB3),
    secondary = Color(0xFFA5D6A7),           // 浅绿
    onSecondary = Color(0xFF1B5E20),
    secondaryContainer = Color(0xFF388E3C),
    onSecondaryContainer = Color(0xFFC8E6C9),
    tertiary = Color(0xFF90CAF9),           // 浅蓝
    onTertiary = Color(0xFF0D47A1),
    tertiaryContainer = Color(0xFF1976D2),
    onTertiaryContainer = Color(0xFFBBDEFB),
    error = Color(0xFFEF9A9A),
    onError = Color(0xFF4A0000),
    errorContainer = Color(0xFFD32F2F),
    onErrorContainer = Color(0xFFFFCDD2),
    background = Color(0xFF1A1A1A),          // 深灰背景
    onBackground = Color(0xFFE0E0E0),
    surface = Color(0xFF2D2D2D),             // 深色卡片
    onSurface = Color(0xFFE0E0E0),
    surfaceVariant = Color(0xFF3D3D3D),
    onSurfaceVariant = Color(0xFFBDBDBD),
    outline = Color(0xFF757575)
)

/**
 * 应用主题
 * @param darkTheme 是否使用深色主题（false=跟随系统，true=强制深色）
 * @param content 内容
 */
@Composable
fun AppTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit
) {
    val colorScheme = if (darkTheme) DarkColorScheme else LightColorScheme

    MaterialTheme(
        colorScheme = colorScheme,
        content = content
    )
}

/**
 * 深色模式检测辅助对象
 */
object ThemeUtils {
    /**
     * 根据设置模式判断是否使用深色主题
     * @param darkModeSetting 0=跟随系统, 1=浅色, 2=深色
     */
    fun shouldUseDarkTheme(darkModeSetting: Int): Boolean? {
        return when (darkModeSetting) {
            1 -> false   // 强制浅色
            2 -> true   // 强制深色
            else -> null // 跟随系统（返回null，让系统决定）
        }
    }
}
