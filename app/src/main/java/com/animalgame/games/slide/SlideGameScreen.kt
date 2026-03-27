package com.animalgame.games.slide

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import com.animalgame.core.game.GameAction
import com.animalgame.core.game.GameModule
import com.animalgame.core.game.GameState
import com.animalgame.ui.components.GameTopBar

// 启用实验性 Material3 API
@OptIn(ExperimentalMaterial3Api::class)

// 儿童风格配色
private object SlideColors {
    val BackgroundStart = Color(0xFFFFF8E1)  // 暖黄色
    val BackgroundEnd = Color(0xFFE8F5E9)     // 浅绿色
    val Primary = Color(0xFFFF9800)           // 橙色
    val Secondary = Color(0xFF4CAF50)         // 绿色
    val Accent = Color(0xFFE91E63)            // 粉色
    val EasyColor = Color(0xFF81C784)          // 简单 - 绿色
    val MediumColor = Color(0xFFFFB74D)        // 中等 - 橙色
    val HardColor = Color(0xFFE57373)          // 困难 - 红色
    val CardBg = Color.White
    val TextDark = Color(0xFF5D4037)
    val TextLight = Color(0xFF8D6E63)
}

/**
 * 方块推推乐游戏 UI - 儿童卡通风格
 */
@Composable
fun SlideGameScreen(
    module: GameModule,
    onBack: () -> Unit
) {
    val slideModule = module as? SlideGameModule
    val gameState by module.state.collectAsState()

    val handleBack: () -> Unit = {
        when (gameState) {
            is GameState.Idle -> onBack()
            else -> slideModule?.resetToIdle() ?: module.onUserAction(GameAction.Quit)
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    colors = listOf(SlideColors.BackgroundStart, SlideColors.BackgroundEnd)
                )
            )
    ) {
        when (val state = gameState) {
            is GameState.Idle -> {
                LevelSelectScreen(
                    module = slideModule!!,
                    onBack = handleBack
                )
            }

            is GameState.Ready -> {
                val difficultyName = slideModule?.getCurrentDifficultyName()
                PlayingScreen(
                    module = slideModule!!,
                    state = GameState.Playing(level = state.level, elapsedTime = 0, score = 0),
                    difficultyName = difficultyName,
                    levelInDifficulty = state.level,
                    onBack = handleBack,
                    onRestart = { slideModule.restartCurrentLevel() }
                )
            }

            is GameState.Playing -> {
                val difficultyName = slideModule?.getCurrentDifficultyName()
                val levelInDifficulty = slideModule?.getCurrentLevelIndex() ?: state.level

                PlayingScreen(
                    module = slideModule!!,
                    state = state,
                    difficultyName = difficultyName,
                    levelInDifficulty = levelInDifficulty,
                    onBack = handleBack,
                    onRestart = { slideModule.restartCurrentLevel() }
                )
            }

            is GameState.Completed -> {
                val difficultyName = slideModule?.getCurrentDifficultyName()
                val levelInDifficulty = slideModule?.getCurrentLevelIndex() ?: state.level
                val isLastInDifficulty = slideModule?.isDifficultyCompleted() ?: false

                CompletedScreen(
                    state = state,
                    difficultyName = difficultyName,
                    levelInDifficulty = levelInDifficulty,
                    isLastInDifficulty = isLastInDifficulty,
                    onNextLevel = { slideModule?.nextLevel() },
                    onReplay = { slideModule?.restartCurrentLevel() },
                    onBack = handleBack
                )
            }

            is GameState.Paused -> {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        modifier = Modifier
                            .background(SlideColors.CardBg, RoundedCornerShape(24.dp))
                            .padding(32.dp)
                    ) {
                        Text("⏸️ 游戏暂停", fontSize = 24.sp, fontWeight = FontWeight.Bold, color = SlideColors.TextDark)
                        Spacer(modifier = Modifier.height(16.dp))
                        Button(
                            onClick = { module.onUserAction(GameAction.Resume) },
                            colors = ButtonDefaults.buttonColors(containerColor = SlideColors.Secondary),
                            shape = RoundedCornerShape(16.dp)
                        ) {
                            Text("继续玩", fontSize = 18.sp)
                        }
                    }
                }
            }

            is GameState.AllCompleted -> {
                CompletedScreen(
                    state = GameState.Completed(
                        level = state.levelResults.size,
                        isSuccess = true,
                        timeMillis = state.totalTime,
                        score = state.totalScore,
                        stars = state.levelResults.maxOfOrNull { it.stars } ?: 0
                    ),
                    difficultyName = slideModule?.getCurrentDifficultyName(),
                    levelInDifficulty = state.levelResults.size,
                    isLastInDifficulty = true,
                    onNextLevel = {},
                    onReplay = { slideModule?.restartCurrentLevel() },
                    onBack = handleBack
                )
            }
        }
    }
}

/**
 * 关卡选择屏幕 - 卡通风格
 */
@Composable
private fun LevelSelectScreen(
    module: SlideGameModule,
    onBack: () -> Unit
) {
    Column(
        modifier = Modifier.fillMaxSize()
    ) {
        // 使用统一的顶部导航栏
        GameTopBar(
            title = "🧩 方块推推乐",
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
                "滑动方块，拼出正确顺序！",
                fontSize = 16.sp,
                color = SlideColors.TextDark,
                textAlign = TextAlign.Center
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                "点击或滑动相邻的方块来移动它们",
                fontSize = 14.sp,
                color = SlideColors.TextLight,
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
                subtitle = "10关 · 适合小朋友",
                color = SlideColors.EasyColor,
                onClick = {
                    module.setDifficulty(SlideDifficulty.EASY)
                    module.start(1)
                }
            )

            DifficultyCard(
                emoji = "⭐",
                title = "中等",
                subtitle = "10关 · 动动脑筋",
                color = SlideColors.MediumColor,
                onClick = {
                    module.setDifficulty(SlideDifficulty.MEDIUM)
                    module.start(1)
                }
            )

            DifficultyCard(
                emoji = "🏆",
                title = "困难",
                subtitle = "10关 · 挑战一下",
                color = SlideColors.HardColor,
                onClick = {
                    module.setDifficulty(SlideDifficulty.HARD)
                    module.start(1)
                }
            )
        }
    }
}

/**
 * 难度选择卡片
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun DifficultyCard(
    emoji: String,
    title: String,
    subtitle: String,
    color: Color,
    onClick: () -> Unit
) {
    Card(
        onClick = onClick,
        modifier = Modifier
            .fillMaxWidth()
            .height(80.dp),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = SlideColors.CardBg),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 20.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // 图标
            Box(
                modifier = Modifier
                    .size(50.dp)
                    .background(color.copy(alpha = 0.2f), CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Text(emoji, fontSize = 24.sp)
            }

            Spacer(modifier = Modifier.width(16.dp))

            // 文字
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    title,
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold,
                    color = SlideColors.TextDark
                )
                Text(
                    subtitle,
                    fontSize = 14.sp,
                    color = SlideColors.TextLight
                )
            }

            // 箭头
            Text("→", fontSize = 24.sp, color = color)
        }
    }
}

/**
 * 游戏进行中屏幕
 */
@Composable
private fun PlayingScreen(
    module: SlideGameModule,
    state: GameState.Playing,
    difficultyName: String?,
    levelInDifficulty: Int,
    onBack: () -> Unit,
    onRestart: () -> Unit
) {
    val levelConfig = module.getCurrentLevelConfig()

    val scale by animateFloatAsState(
        targetValue = 1f,
        animationSpec = tween(durationMillis = 400, easing = FastOutSlowInEasing),
        label = "enterScale"
    )

    var gridViewRef by remember { mutableStateOf<SlideGridView?>(null) }

    fun handleTileMoved() {
        module.onTileMoved()
    }

    fun handleWin() {
        module.onWin()
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(bottom = 8.dp)
    ) {
        // 顶部导航栏
        GameTopBar(
            title = "🧩 方块推推乐",
            level = levelInDifficulty,
            difficultyName = difficultyName,
            score = state.score,
            stars = 0,
            onBack = onBack
        )

        // 步数统计 - 卡通风格
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 24.dp, vertical = 12.dp)
                .background(SlideColors.CardBg, RoundedCornerShape(16.dp))
                .padding(16.dp),
            horizontalArrangement = Arrangement.SpaceEvenly,
            verticalAlignment = Alignment.CenterVertically
        ) {
            // 步数
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text("👣", fontSize = 24.sp)
                val steps = state.data["steps"] as? Int ?: 0
                Text(
                    "$steps",
                    fontSize = 24.sp,
                    fontWeight = FontWeight.Bold,
                    color = SlideColors.Primary
                )
                Text("步数", fontSize = 12.sp, color = SlideColors.TextLight)
            }

            // 分隔线
            Box(
                modifier = Modifier
                    .width(1.dp)
                    .height(40.dp)
                    .background(SlideColors.TextLight.copy(alpha = 0.2f))
            )

            // 目标步数
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text("🎯", fontSize = 24.sp)
                val targetSteps = state.data["targetSteps"] as? Int ?: 10
                Text(
                    "$targetSteps",
                    fontSize = 24.sp,
                    fontWeight = FontWeight.Bold,
                    color = SlideColors.Secondary
                )
                Text("目标", fontSize = 12.sp, color = SlideColors.TextLight)
            }
        }

        // 游戏区域
        Box(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
                .padding(16.dp)
                .graphicsLayer {
                    scaleX = scale
                    scaleY = scale
                },
            contentAlignment = Alignment.Center
        ) {
            // 目标状态显示 - 卡通风格
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 8.dp),
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("📋 目标: ", fontSize = 16.sp, color = SlideColors.TextDark)
                levelConfig?.let { config ->
                    Row {
                        config.targetState.filter { it != 0 }.forEach { num ->
                            Box(
                                modifier = Modifier
                                    .size(28.dp)
                                    .background(SlideColors.Primary, RoundedCornerShape(6.dp)),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = num.toString(),
                                    fontSize = 14.sp,
                                    color = Color.White,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                            Spacer(modifier = Modifier.width(4.dp))
                        }
                    }
                }
            }

            // 网格视图
            AndroidView(
                factory = { ctx ->
                    SlideGridView(ctx).also { view ->
                        levelConfig?.let { config ->
                            view.initialize(config)
                        }

                        view.onTileMoved = { _ ->
                            handleTileMoved()
                        }

                        view.onWin = {
                            handleWin()
                        }

                        gridViewRef = view
                    }
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 24.dp, vertical = 16.dp)
                    .aspectRatio(1f)
            )
        }

        // 重置按钮 - 卡通风格
        Button(
            onClick = {
                gridViewRef?.reset(levelConfig?.initialState ?: emptyList())
                onRestart()
            },
            modifier = Modifier
                .fillMaxWidth()
                .height(56.dp)
                .padding(horizontal = 24.dp),
            colors = ButtonDefaults.buttonColors(containerColor = SlideColors.Accent),
            shape = RoundedCornerShape(16.dp)
        ) {
            Text("🔄 重新开始", fontSize = 18.sp, fontWeight = FontWeight.Bold, color = Color.White)
        }
    }
}

/**
 * 完成屏幕 - 卡通风格
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
        // 成功/失败图标
        Text(
            text = if (state.isSuccess) "🎉" else "😢",
            fontSize = 64.sp
        )

        Spacer(modifier = Modifier.height(16.dp))

        Text(
            text = if (state.isSuccess) "恭喜过关！" else "再试试吧",
            fontSize = 28.sp,
            fontWeight = FontWeight.Bold,
            color = if (state.isSuccess) SlideColors.Secondary else SlideColors.HardColor
        )

        Spacer(modifier = Modifier.height(24.dp))

        // 星星显示 - 卡通风格
        Row {
            repeat(3) { index ->
                Text(
                    text = if (index < state.stars) "⭐" else "☆",
                    fontSize = 40.sp
                )
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // 得分信息
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = SlideColors.CardBg)
        ) {
            Column(
                modifier = Modifier.padding(20.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text("🏆 得分: ${state.score}", fontSize = 20.sp, color = SlideColors.Primary)
                Text("⏱️ 用时: ${state.timeMillis / 1000}秒", fontSize = 16.sp, color = SlideColors.TextLight)
            }
        }

        Spacer(modifier = Modifier.height(32.dp))

        // 按钮 - 卡通风格
        if (!isLastInDifficulty && state.isSuccess) {
            Button(
                onClick = onNextLevel,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp),
                colors = ButtonDefaults.buttonColors(containerColor = SlideColors.Secondary),
                shape = RoundedCornerShape(16.dp)
            ) {
                Text("下一关 ➡️", fontSize = 18.sp, fontWeight = FontWeight.Bold, color = Color.White)
            }
            Spacer(modifier = Modifier.height(12.dp))
        } else if (isLastInDifficulty && state.isSuccess) {
            Text(
                text = "🎊 $difficultyName 已全部通关！",
                fontSize = 18.sp,
                color = SlideColors.Primary,
                fontWeight = FontWeight.Bold
            )
            Spacer(modifier = Modifier.height(16.dp))
        }

        Button(
            onClick = onReplay,
            modifier = Modifier
                .fillMaxWidth()
                .height(56.dp),
            colors = ButtonDefaults.buttonColors(containerColor = SlideColors.Primary),
            shape = RoundedCornerShape(16.dp)
        ) {
            Text("🔄 再玩一次", fontSize = 18.sp, fontWeight = FontWeight.Bold, color = Color.White)
        }

        Spacer(modifier = Modifier.height(12.dp))

        OutlinedButton(
            onClick = onBack,
            modifier = Modifier
                .fillMaxWidth()
                .height(56.dp),
            shape = RoundedCornerShape(16.dp)
        ) {
            Text("🏠 返回", fontSize = 18.sp)
        }
    }
}