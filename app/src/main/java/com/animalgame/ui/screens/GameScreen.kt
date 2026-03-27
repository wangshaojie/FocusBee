package com.animalgame.ui.screens

import android.app.Activity
import android.content.Intent
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.sp
import com.animalgame.core.manager.ScoreManager
import com.animalgame.core.model.GameResult
import com.animalgame.games.colormind.ColorMindGameModule
import com.animalgame.games.colormind.ColorMindGameScreen
import com.animalgame.games.animal.AnimalGameActivity
import com.animalgame.games.memory.MemoryGameModule
import com.animalgame.games.memory.MemoryGameUI
import com.animalgame.games.schulte.SchulteGameScreen
import com.animalgame.games.gravity.GravityGameModule
import com.animalgame.games.gravity.GravityGameScreen
import com.animalgame.games.slide.SlideGameModule
import com.animalgame.games.slide.SlideGameScreen
import kotlinx.coroutines.flow.collectLatest

// 游戏页面配色
private object GameScreenColors {
    val BackgroundStart = Color(0xFFE8F4FD)
    val BackgroundEnd = Color(0xFFF3E8FD)
}

/**
 * 游戏页面
 * 根据 gameType 加载不同的游戏
 * 使用统一的 GameModule 接口
 */
@Composable
fun GameScreen(
    gameType: String,
    onBack: () -> Unit
) {
    val context = LocalContext.current

    // 根据 gameType 加载对应的游戏
    when (gameType) {
        "memory" -> {
            // 记忆翻牌游戏 - 使用 GameModule 接口
            val scoreManager = remember { ScoreManager.getInstance(context) }
            val module = remember { MemoryGameModule() }

            // 收集结果并保存
            LaunchedEffect(Unit) {
                module.result.collectLatest { result ->
                    result?.let {
                        // 转换为模型使用的 GameResult
                        val modelResult = GameResult(
                            gameId = it.gameId,
                            level = it.level,
                            score = it.score,
                            stars = it.stars,
                            isCompleted = it.isSuccess,
                            timeMillis = it.timeMillis,
                            mistakes = it.mistakes
                        )
                        scoreManager.reportResult(modelResult)
                    }
                }
            }

            // 使用通用 UI
            MemoryGameUI(
                module = module,
                onBack = onBack
            )
        }
        "schulte" -> {
            // 舒尔特训练
            val module = remember { com.animalgame.games.schulte.SchulteGameModule() }
            SchulteGameScreen(
                module = module,
                onBack = onBack
            )
        }
        "animal" -> {
            // 萌音大挑战 - 启动 Activity 后立即返回，让 Activity 覆盖在下方
            androidx.compose.runtime.LaunchedEffect(Unit) {
                val activity = context as? Activity
                activity?.let {
                    val intent = Intent(it, AnimalGameActivity::class.java)
                    it.startActivity(intent)
                    onBack()
                }
            }
        }
        "color_mind" -> {
            // 颜色识别训练
            val scoreManager = remember { ScoreManager.getInstance(context) }
            val module = remember { ColorMindGameModule() }

            // 收集结果并保存
            LaunchedEffect(Unit) {
                module.result.collectLatest { result ->
                    result?.let {
                        // 转换为模型使用的 GameResult
                        val modelResult = GameResult(
                            gameId = it.gameId,
                            level = it.level,
                            score = it.score,
                            stars = it.stars,
                            isCompleted = it.isSuccess,
                            timeMillis = it.timeMillis,
                            mistakes = it.mistakes
                        )
                        scoreManager.reportResult(modelResult)
                    }
                }
            }

            // 使用 Color Mind UI
            ColorMindGameScreen(
                module = module,
                onBack = onBack
            )
        }
        "gravity" -> {
            // 平衡小球游戏
            val scoreManager = remember { ScoreManager.getInstance(context) }
            val module = remember { GravityGameModule() }

            // 收集结果并保存
            LaunchedEffect(Unit) {
                module.result.collectLatest { result ->
                    result?.let {
                        val modelResult = GameResult(
                            gameId = it.gameId,
                            level = it.level,
                            score = it.score,
                            stars = it.stars,
                            isCompleted = it.isSuccess,
                            timeMillis = it.timeMillis,
                            mistakes = it.mistakes
                        )
                        scoreManager.reportResult(modelResult)
                    }
                }
            }

            // 使用 Gravity UI
            GravityGameScreen(
                module = module,
                onBack = onBack
            )
        }
        "slide" -> {
            // 方块推推乐游戏
            val scoreManager = remember { ScoreManager.getInstance(context) }
            val module = remember { SlideGameModule() }

            // 收集结果并保存
            LaunchedEffect(Unit) {
                module.result.collectLatest { result ->
                    result?.let {
                        val modelResult = GameResult(
                            gameId = it.gameId,
                            level = it.level,
                            score = it.score,
                            stars = it.stars,
                            isCompleted = it.isSuccess,
                            timeMillis = it.timeMillis,
                            mistakes = it.mistakes
                        )
                        scoreManager.reportResult(modelResult)
                    }
                }
            }

            // 使用 Slide UI
            SlideGameScreen(
                module = module,
                onBack = onBack
            )
        }
        else -> {
            // 未知游戏
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(
                        Brush.verticalGradient(
                            colors = listOf(GameScreenColors.BackgroundStart, GameScreenColors.BackgroundEnd)
                        )
                    ),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "未知游戏类型: $gameType",
                    fontSize = 18.sp
                )
            }
        }
    }
}

/**
 * 游戏占位符（暂时用于未实现完整集成的游戏）
 */
@Composable
private fun GamePlaceholder(
    gameName: String,
    onBack: () -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    colors = listOf(GameScreenColors.BackgroundStart, GameScreenColors.BackgroundEnd)
                )
            ),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = "🎮 $gameName 即将上线",
            fontSize = 20.sp
        )
    }
}
