package com.animalgame.games.gravity

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import com.animalgame.core.game.GameAction
import com.animalgame.core.game.GameModule
import com.animalgame.core.game.GameState
import com.animalgame.ui.components.GameTopBar
import com.animalgame.ui.components.DifficultyCard
import com.animalgame.ui.components.DifficultyColors
import com.animalgame.core.game.AbstractGameModule

/**
 * 平衡小球游戏 UI
 */
@Composable
fun GravityGameScreen(
    module: GameModule,
    onBack: () -> Unit
) {
    val gravityModule = module as? GravityGameModule
    val gameState by module.state.collectAsState()

    // 处理返回逻辑
    val handleBack: () -> Unit = {
        when (gameState) {
            is GameState.Idle -> onBack()
            else -> gravityModule?.resetToIdle() ?: module.onUserAction(GameAction.Quit)
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFFF0F4FF))
    ) {
        when (val state = gameState) {
            is GameState.Idle -> {
                LevelSelectScreen(
                    module = gravityModule!!,
                    onBack = handleBack
                )
            }

            is GameState.Ready -> {
                ReadyScreen(countdown = state.countdown)
            }

            is GameState.Playing -> {
                val difficultyName = gravityModule?.getCurrentDifficultyName()
                val levelInDifficulty = gravityModule?.getCurrentLevelIndex() ?: state.level

                PlayingScreen(
                    module = gravityModule!!,
                    state = state,
                    difficultyName = difficultyName,
                    levelInDifficulty = levelInDifficulty,
                    onBack = handleBack,
                    onRestart = {
                        gravityModule.restartCurrentLevel()
                    }
                )
            }

            is GameState.Completed -> {
                val difficultyName = gravityModule?.getCurrentDifficultyName()
                val levelInDifficulty = gravityModule?.getCurrentLevelIndex() ?: state.level
                val isLastInDifficulty = gravityModule?.isDifficultyCompleted() ?: false

                CompletedScreen(
                    state = state,
                    difficultyName = difficultyName,
                    levelInDifficulty = levelInDifficulty,
                    isLastInDifficulty = isLastInDifficulty,
                    onNextLevel = {
                        gravityModule?.nextLevel()
                    },
                    onReplay = {
                        gravityModule?.restartCurrentLevel()
                    },
                    onBack = handleBack
                )
            }

            is GameState.Paused -> {
                // 暂停状态，显示继续按钮
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text("游戏暂停", fontSize = 24.sp, fontWeight = FontWeight.Bold)
                        Spacer(modifier = Modifier.height(16.dp))
                        Button(onClick = { module.onUserAction(GameAction.Resume) }) {
                            Text("继续")
                        }
                    }
                }
            }

            is GameState.AllCompleted -> {
                // 全部通关，显示结算
                CompletedScreen(
                    state = GameState.Completed(
                        level = state.levelResults.size,
                        isSuccess = true,
                        timeMillis = state.totalTime,
                        score = state.totalScore,
                        stars = state.levelResults.maxOfOrNull { it.stars } ?: 0
                    ),
                    difficultyName = gravityModule?.getCurrentDifficultyName(),
                    levelInDifficulty = state.levelResults.size,
                    isLastInDifficulty = true,
                    onNextLevel = {},
                    onReplay = { gravityModule?.restartCurrentLevel() },
                    onBack = handleBack
                )
            }
        }
    }
}

/**
 * 关卡选择屏幕 - 卡通风格
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun LevelSelectScreen(
    module: GravityGameModule,
    onBack: () -> Unit
) {
    Column(
        modifier = Modifier.fillMaxSize()
    ) {
        GameTopBar(
            title = "🎯 平衡小球",
            level = 0,
            score = 0,
            stars = 0,
            onBack = onBack
        )

        // 说明文字
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                "倾斜手机，控制小球到达终点！",
                fontSize = 16.sp,
                color = Color(0xFF5D4037),
                textAlign = TextAlign.Center
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                "小心不要碰到墙壁哦",
                fontSize = 14.sp,
                color = Color(0xFF8D6E63),
                textAlign = TextAlign.Center
            )
        }

        // 难度选择卡片
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 24.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            DifficultyCard(
                emoji = "🌟",
                title = "简单",
                subtitle = "3次容错 · 25秒 · 10关",
                color = DifficultyColors.EasyColor,
                onClick = {
                    module.setDifficulty(Difficulty.EASY)
                    module.start(1)
                }
            )

            DifficultyCard(
                emoji = "⭐",
                title = "中等",
                subtitle = "2次容错 · 20秒 · 10关",
                color = DifficultyColors.MediumColor,
                onClick = {
                    module.setDifficulty(Difficulty.MEDIUM)
                    module.start(1)
                }
            )

            DifficultyCard(
                emoji = "🏆",
                title = "困难",
                subtitle = "1次容错 · 15秒 · 10关",
                color = DifficultyColors.HardColor,
                onClick = {
                    module.setDifficulty(Difficulty.HARD)
                    module.start(1)
                }
            )

            DifficultyCard(
                emoji = "💎",
                title = "挑战",
                subtitle = "0次容错 · 12秒 · 10关",
                color = DifficultyColors.ExpertColor,
                onClick = {
                    module.setDifficulty(Difficulty.EXPERT)
                    module.start(1)
                }
            )
        }
    }
}

/**
 * 准备屏幕（倒计时）
 */
@Composable
private fun ReadyScreen(countdown: Int) {
    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = countdown.toString(),
            fontSize = 72.sp,
            fontWeight = FontWeight.Bold,
            color = Color(0xFF5C6BC0)
        )
    }
}

/**
 * 游戏进行中屏幕
 */
@Composable
private fun PlayingScreen(
    module: GravityGameModule,
    state: GameState.Playing,
    difficultyName: String?,
    levelInDifficulty: Int,
    onBack: () -> Unit,
    onRestart: () -> Unit
) {
    val context = LocalContext.current
    val levelConfig = module.getCurrentLevelConfig()

    // 动画
    val scale by animateFloatAsState(
        targetValue = 1f,
        animationSpec = tween(durationMillis = 400, easing = FastOutSlowInEasing),
        label = "enterScale"
    )

    // 游戏视图回调
    var gameViewRef by remember { mutableStateOf<GameSurfaceView?>(null) }

    // 处理游戏事件
    fun handleGameEvent(event: GameSurfaceEvent) {
        module.onGameEvent(event)
    }

    // 当游戏状态变为 Playing 时，启动游戏
    LaunchedEffect(state) {
        if (state is GameState.Playing) {
            gameViewRef?.startGame()
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFFF8F6FF))
    ) {
        // 顶部导航栏
        GameTopBar(
            title = "平衡小球",
            level = levelInDifficulty,
            difficultyName = difficultyName,
            score = state.score,
            stars = 0,
            onBack = onBack
        )

        // 游戏区域
        Box(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
                .graphicsLayer {
                    scaleX = scale
                    scaleY = scale
                }
        ) {
            // 使用 AndroidView 嵌入 SurfaceView
            AndroidView(
                factory = { ctx ->
                    GameSurfaceView(ctx).also { view ->
                        levelConfig?.let { config ->
                            view.initialize(config, ctx)
                        }

                        view.setStateCallback { surfaceState ->
                            when (surfaceState) {
                                is GameSurfaceState.Playing -> {
                                    // 游戏进行中更新
                                }
                                is GameSurfaceState.Success -> {
                                    handleGameEvent(GameSurfaceEvent.Success(surfaceState.timeMillis))
                                }
                                is GameSurfaceState.Failed -> {
                                    handleGameEvent(GameSurfaceEvent.Failed(surfaceState.message))
                                }
                                is GameSurfaceState.Timeout -> {
                                    handleGameEvent(GameSurfaceEvent.GameOver(surfaceState.elapsedTime))
                                }
                            }
                        }

                        // 启动游戏线程
                        view.startThread()

                        gameViewRef = view
                    }
                },
                modifier = Modifier.fillMaxSize(),
                update = { view ->
                    // 更新时检查是否需要重新初始化
                }
            )
        }

        // 重置按钮
        OutlinedButton(
            onClick = {
                gameViewRef?.stopGame()
                onRestart()
            },
            modifier = Modifier
                .fillMaxWidth()
                .height(44.dp)
                .padding(horizontal = 24.dp, vertical = 16.dp),
            colors = ButtonDefaults.outlinedButtonColors(contentColor = Color(0xFF5C6BC0)),
            border = ButtonDefaults.outlinedButtonBorder.copy(
                brush = androidx.compose.ui.graphics.SolidColor(Color(0xFF5C6BC0).copy(alpha = 0.5f))
            ),
            shape = RoundedCornerShape(16.dp)
        ) {
            Text("重置本关", fontSize = 16.sp)
        }
    }
}

/**
 * 完成屏幕
 */
@Composable
private fun CompletedScreen(
    state: GameState.Completed,
    difficultyName: String?,
    levelInDifficulty: Int,
    isLastInDifficulty: Boolean,
    onNextLevel: () -> Unit,
    onReplay: () -> Unit,
    onBack: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        if (state.isSuccess) {
            Text("恭喜过关！", fontSize = 32.sp, fontWeight = FontWeight.Bold, color = Color(0xFF5C6BC0))
        } else {
            Text("挑战失败", fontSize = 32.sp, fontWeight = FontWeight.Bold, color = Color(0xFFE53935))
        }

        Spacer(modifier = Modifier.height(16.dp))

        // 星星显示
        Row {
            repeat(3) { index ->
                Text(
                    text = if (index < state.stars) "⭐" else "☆",
                    fontSize = 36.sp
                )
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

        Text("得分: ${state.score}", fontSize = 18.sp)
        Text("用时: ${state.timeMillis / 1000}s", fontSize = 16.sp, color = Color(0xFF666666))

        Spacer(modifier = Modifier.height(24.dp))

        // 下一关按钮
        if (!isLastInDifficulty && state.isSuccess) {
            Button(
                onClick = onNextLevel,
                modifier = Modifier.fillMaxWidth().height(56.dp),
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF5C6BC0))
            ) {
                Text("下一关", fontSize = 18.sp)
            }
            Spacer(modifier = Modifier.height(12.dp))
        } else if (isLastInDifficulty && state.isSuccess) {
            Text(
                text = "🎉 ${difficultyName}难度已全部通关！",
                fontSize = 16.sp,
                color = Color(0xFF5C6BC0),
                fontWeight = FontWeight.Medium
            )
            Spacer(modifier = Modifier.height(16.dp))
        }

        Button(
            onClick = onReplay,
            modifier = Modifier.fillMaxWidth().height(56.dp),
            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF81D4FA))
        ) {
            Text("重玩本关", fontSize = 18.sp, color = Color(0xFF1565C0))
        }

        Spacer(modifier = Modifier.height(12.dp))

        OutlinedButton(
            onClick = onBack,
            modifier = Modifier.fillMaxWidth().height(56.dp)
        ) {
            Text("返回", fontSize = 18.sp)
        }
    }
}
