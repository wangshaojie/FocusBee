package com.animalgame.ui

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.platform.LocalContext
import androidx.navigation.compose.rememberNavController
import com.animalgame.core.manager.SettingsManager
import com.animalgame.ui.navigation.GameNavHost
import com.animalgame.ui.theme.AppTheme

/**
 * 主屏幕 - 包含导航控制器和主题
 */
@Composable
fun MainScreen() {
    val context = LocalContext.current
    val settings by SettingsManager.getSettingsFlow(context).collectAsState(initial = null)
    val systemDark = isSystemInDarkTheme()

    // 根据设置决定是否使用深色主题
    // 0=跟随系统, 1=浅色, 2=深色
    val isDarkTheme = when (settings?.darkModeEnabled) {
        true -> true   // 强制深色
        false -> false // 强制浅色
        null -> systemDark // 跟随系统
    }

    AppTheme(darkTheme = isDarkTheme) {
        // 创建 NavController
        val navController = rememberNavController()

        // 使用 NavHost 管理页面
        GameNavHost(navController = navController)
    }
}
