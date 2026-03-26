package com.animalgame.ui.navigation

import androidx.compose.runtime.Composable
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.navArgument
import com.animalgame.ui.screens.GameListScreen
import com.animalgame.ui.screens.GameScreen
import com.animalgame.ui.screens.SettingsScreen

/**
 * 导航路由定义
 */
object NavRoutes {
    const val GAME_LIST = "game_list"
    const val GAME = "game/{gameType}"
    const val SETTINGS = "settings"

    fun gameRoute(gameType: String) = "game/$gameType"
}

/**
 * 游戏导航图
 */
@Composable
fun GameNavHost(
    navController: NavHostController,
    startDestination: String = NavRoutes.GAME_LIST
) {
    NavHost(
        navController = navController,
        startDestination = startDestination
    ) {
        // 游戏列表页
        composable(NavRoutes.GAME_LIST) {
            GameListScreen(
                onGameClick = { gameType ->
                    // 跳转到游戏页，传递 gameType 参数
                    navController.navigate(NavRoutes.gameRoute(gameType))
                },
                onSettingsClick = {
                    // 跳转到设置页
                    navController.navigate(NavRoutes.SETTINGS)
                }
            )
        }

        // 游戏页面 - 接收 gameType 参数
        composable(
            route = NavRoutes.GAME,
            arguments = listOf(
                navArgument("gameType") { type = NavType.StringType }
            )
        ) { backStackEntry ->
            val gameType = backStackEntry.arguments?.getString("gameType") ?: "memory"
            GameScreen(
                gameType = gameType,
                onBack = {
                    // 返回游戏列表
                    navController.popBackStack()
                }
            )
        }

        // 设置页面
        composable(NavRoutes.SETTINGS) {
            SettingsScreen(
                onBack = {
                    navController.popBackStack()
                }
            )
        }
    }
}
