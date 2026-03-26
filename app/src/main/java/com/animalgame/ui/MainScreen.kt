package com.animalgame.ui

import androidx.compose.runtime.Composable
import androidx.navigation.compose.rememberNavController
import com.animalgame.ui.navigation.GameNavHost

/**
 * 主屏幕 - 包含导航控制器
 */
@Composable
fun MainScreen() {
    // 创建 NavController
    val navController = rememberNavController()

    // 使用 NavHost 管理页面
    GameNavHost(navController = navController)
}
