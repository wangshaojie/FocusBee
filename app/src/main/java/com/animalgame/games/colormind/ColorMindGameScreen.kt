package com.animalgame.games.colormind

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.ui.draw.clip
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.animalgame.core.game.GameAction
import com.animalgame.core.game.GameModule
import com.animalgame.core.game.GameState
import com.animalgame.ui.components.GameTopBar
import com.animalgame.ui.components.DifficultyCard
import com.animalgame.ui.components.DifficultyColors

/**
 * Color Mind 游戏 UI
 * 原则：
 * - 只订阅 state，不写业务逻辑
 * - 点击事件只调用 module.onUserAction()
 * - 所有逻辑在模块内部处理
 */
@Composable
fun ColorMindGameScreen(
    module: GameModule,
    onBack: () -> Unit
) {
    // 转换为具体类型以访问扩展方法
    val colorMindModule = module as? ColorMindGameModule

    // 收集状态
    val gameState by module.state.collectAsState()

    // 处理返回逻辑：根据当前状态决定返回行为
    val handleBack: () -> Unit = {
        when (gameState) {
            is GameState.Idle -> {
                // 在关卡选择页面，直接退出
                onBack()
            }
            else -> {
                // 在游戏进行中或完成页面，返回到关卡选择
                colorMindModule?.resetToIdle() ?: module.onUserAction(GameAction.Quit)
            }
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFFF0F4FF))
    ) {
        when (val state = gameState) {
            is GameState.Idle -> {
                // 关卡选择
                LevelSelectScreen(
                    module = colorMindModule!!,
                    onBack = handleBack
                )
            }

            is GameState.Ready -> {
                // 倒计时
                ReadyScreen(countdown = state.countdown)
            }

            is GameState.Playing -> {
                // 游戏进行中
                // 获取难度信息
                val difficultyName = colorMindModule?.getCurrentDifficultyName()
                val levelInDifficulty = colorMindModule?.getCurrentLevelIndex() ?: state.level

                PlayingScreen(
                    state = state,
                    difficultyName = difficultyName,
                    levelInDifficulty = levelInDifficulty,
                    onAnswer = { selectedColor ->
                        module.onUserAction(GameAction.ColorMindAnswer(selectedColor))
                    },
                    onBack = handleBack,
                    onReset = {
                        colorMindModule?.restartCurrentLevel() ?: module.onUserAction(GameAction.Restart)
                    }
                )
            }

            is GameState.Paused -> {
                // 暂停
                PausedScreen(
                    elapsedTime = state.elapsedTime,
                    score = state.score,
                    onResume = {
                        module.onUserAction(GameAction.Resume)
                    },
                    onQuit = {
                        module.onUserAction(GameAction.Quit)
                    }
                )
            }

            is GameState.Completed -> {
                // 游戏完成 - 从 module 获取当前难度信息
                val difficultyName = colorMindModule?.getCurrentDifficultyName()
                val levelInDifficulty = colorMindModule?.getCurrentLevelIndex() ?: state.level
                val isLastInDifficulty = colorMindModule?.isDifficultyCompleted() ?: false

                CompletedScreen(
                    state = state,
                    difficultyName = difficultyName,
                    levelInDifficulty = levelInDifficulty,
                    isLastInDifficulty = isLastInDifficulty,
                    onNextLevel = {
                        colorMindModule?.nextLevel()
                    },
                    onReplay = {
                        colorMindModule?.restartCurrentLevel() ?: module.onUserAction(GameAction.Restart)
                    },
                    onBack = handleBack
                )
            }

            is GameState.AllCompleted -> {
                // 全部通关
                AllCompletedScreen(
                    state = state,
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
    module: ColorMindGameModule,
    onBack: () -> Unit
) {
    Column(
        modifier = Modifier.fillMaxSize()
    ) {
        GameTopBar(
            title = "🎨 颜色识别",
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
                "看看你的眼睛有多厉害！",
                fontSize = 16.sp,
                color = Color(0xFF5D4037),
                textAlign = TextAlign.Center
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                "判断文字颜色还是文字内容？",
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
                subtitle = "红、蓝2色 · 10题",
                color = DifficultyColors.EasyColor,
                onClick = {
                    module.setDifficulty(ColorMindGameModule.Difficulty.EASY)
                    module.start(1)
                }
            )

            DifficultyCard(
                emoji = "⭐",
                title = "中等",
                subtitle = "红、蓝、绿3色 · 15题",
                color = DifficultyColors.MediumColor,
                onClick = {
                    module.setDifficulty(ColorMindGameModule.Difficulty.MEDIUM)
                    module.start(1)
                }
            )

            DifficultyCard(
                emoji = "🏆",
                title = "困难",
                subtitle = "红、蓝、绿、黄4色 · 20题",
                color = DifficultyColors.HardColor,
                onClick = {
                    module.setDifficulty(ColorMindGameModule.Difficulty.HARD)
                    module.start(1)
                }
            )

            DifficultyCard(
                emoji = "💎",
                title = "挑战",
                subtitle = "5种颜色 · 25题",
                color = DifficultyColors.ExpertColor,
                onClick = {
                    module.setDifficulty(ColorMindGameModule.Difficulty.EXPERT)
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
    state: GameState.Playing,
    difficultyName: String?,
    levelInDifficulty: Int,
    onAnswer: (selectedColor: String) -> Unit,
    onBack: () -> Unit,
    onReset: () -> Unit = {}
) {
    // 从 state.data 获取游戏数据
    val questionIndex = state.data["questionIndex"] as? Int ?: 0
    val totalQuestions = state.data["totalQuestions"] as? Int ?: 1
    val textColorName = state.data["textColorName"] as? String ?: ""
    val displayColorValue = state.data["displayColorValue"] as? Long ?: 0xFF000000
    val ruleText = state.data["ruleText"] as? String ?: "判断文字内容"
    val isTimeout = state.data["isTimeout"] as? Boolean ?: false

    // 解析选项
    val optionsRaw = state.data["options"] as? List<*> ?: emptyList<String>()
    val options = optionsRaw.filterIsInstance<String>()

    // 颜色
    val displayColor = Color(displayColorValue)
    val backgroundColor = Color(0xFFF8F6FF) // 护眼淡紫背景
    val stageColor = Color(0xFFEDE7F6) // 舞台区域淡紫色

    // 进入动画
    val scale by animateFloatAsState(
        targetValue = 1f,
        animationSpec = tween(durationMillis = 400, easing = FastOutSlowInEasing),
        label = "enterScale"
    )

    val alpha by animateFloatAsState(
        targetValue = 1f,
        animationSpec = tween(durationMillis = 400),
        label = "enterAlpha"
    )

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(backgroundColor)
    ) {
        // 统一顶部导航栏
        GameTopBar(
            title = "颜色识别",
            level = levelInDifficulty,
            difficultyName = difficultyName,
            score = state.score,
            stars = 0,  // 游戏中不显示星级
            onBack = onBack
        )

        Spacer(modifier = Modifier.height(12.dp))

        // 进度显示
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 24.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "进度: ${questionIndex + 1}/$totalQuestions",
                fontSize = 14.sp,
                color = Color(0xFF666666)
            )
            if (isTimeout) {
                Text(
                    text = "超时!",
                    fontSize = 14.sp,
                    color = Color(0xFFE53935),
                    fontWeight = FontWeight.Bold
                )
            }
        }

        // 进度条
        LinearProgressIndicator(
            progress = (questionIndex + 1).toFloat() / totalQuestions,
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 24.dp)
                .height(8.dp)
                .clip(RoundedCornerShape(4.dp)),
            color = Color(0xFF5C6BC0),
            trackColor = Color(0xFFD1C4E9),
        )

        // 游戏区域 - 舞台效果
        Box(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth(),
            contentAlignment = Alignment.Center
        ) {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .graphicsLayer {
                        scaleX = scale
                        scaleY = scale
                        this.alpha = alpha
                    },
                colors = CardDefaults.cardColors(containerColor = stageColor),
                elevation = CardDefaults.cardElevation(defaultElevation = 8.dp),
                shape = RoundedCornerShape(20.dp)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    // 规则提示
                    Text(
                        text = "👉 当前规则: $ruleText",
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF5C6BC0)
                    )

                    Spacer(modifier = Modifier.height(24.dp))

                    // 题目文字 - 使用显示颜色
                    Text(
                        text = textColorName,
                        fontSize = 56.sp,
                        fontWeight = FontWeight.Bold,
                        color = displayColor
                    )

                    Spacer(modifier = Modifier.height(16.dp))

                    // 提示文字
                    Text(
                        text = "上面是什么颜色？",
                        fontSize = 14.sp,
                        color = Color(0xFF666666)
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // 颜色选项按钮区域
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 24.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text(
                text = "请选择正确答案：",
                fontSize = 14.sp,
                color = Color(0xFF666666),
                modifier = Modifier.align(Alignment.CenterHorizontally)
            )

            Spacer(modifier = Modifier.height(4.dp))

            // 根据选项数量决定布局
            when (options.size) {
                2 -> {
                    // 2个选项：水平排列
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        options.forEach { colorName ->
                            ColorButton(
                                colorName = colorName,
                                onClick = { onAnswer(colorName) },
                                modifier = Modifier.weight(1f)
                            )
                        }
                    }
                }
                3, 4 -> {
                    // 3-4个选项：2x2网格
                    val rows = (options.size + 1) / 2
                    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                        for (row in 0 until rows) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(12.dp)
                            ) {
                                val startIdx = row * 2
                                val endIdx = minOf(startIdx + 2, options.size)
                                options.subList(startIdx, endIdx).forEach { colorName ->
                                    ColorButton(
                                        colorName = colorName,
                                        onClick = { onAnswer(colorName) },
                                        modifier = Modifier.weight(1f)
                                    )
                                }
                                // 补齐空白
                                if (endIdx - startIdx < 2) {
                                    Spacer(modifier = Modifier.weight(1f))
                                }
                            }
                        }
                    }
                }
                else -> {
                    // 默认：水平排列
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        options.forEach { colorName ->
                            ColorButton(
                                colorName = colorName,
                                onClick = { onAnswer(colorName) },
                                modifier = Modifier.weight(1f)
                            )
                        }
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // 重置按钮
        OutlinedButton(
            onClick = onReset,
            modifier = Modifier
                .fillMaxWidth()
                .height(44.dp)
                .padding(horizontal = 24.dp),
            colors = ButtonDefaults.outlinedButtonColors(contentColor = Color(0xFF5C6BC0)),
            border = ButtonDefaults.outlinedButtonBorder.copy(
                brush = androidx.compose.ui.graphics.SolidColor(Color(0xFF5C6BC0).copy(alpha = 0.5f))
            ),
            shape = RoundedCornerShape(16.dp)
        ) {
            Text("重置本关", fontSize = 16.sp)
        }

        Spacer(modifier = Modifier.height(16.dp))
    }
}

/**
 * 颜色选项按钮组件
 */
@Composable
private fun ColorButton(
    colorName: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val colorValue = when (colorName) {
        "RED" -> 0xFFE53935
        "BLUE" -> 0xFF1E88E5
        "GREEN" -> 0xFF43A047
        "YELLOW" -> 0xFFFDD835
        "PURPLE" -> 0xFF8E24AA
        else -> 0xFF9E9E9E
    }

    val displayName = when (colorName) {
        "RED" -> "红"
        "BLUE" -> "蓝"
        "GREEN" -> "绿"
        "YELLOW" -> "黄"
        "PURPLE" -> "紫"
        else -> "?"
    }

    Button(
        onClick = onClick,
        modifier = modifier.height(56.dp),
        colors = ButtonDefaults.buttonColors(containerColor = Color(colorValue)),
        shape = RoundedCornerShape(12.dp)
    ) {
        Text(
            text = displayName,
            fontSize = 20.sp,
            fontWeight = FontWeight.Bold,
            color = Color.White
        )
    }
}

/**
 * 暂停屏幕
 */
@Composable
private fun PausedScreen(
    elapsedTime: Long,
    score: Int,
    onResume: () -> Unit,
    onQuit: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text("游戏暂停", fontSize = 24.sp, fontWeight = FontWeight.Bold)
        Spacer(modifier = Modifier.height(16.dp))
        Text("得分: $score", fontSize = 18.sp)
        Text("用时: ${elapsedTime / 1000}s", fontSize = 18.sp)
        Spacer(modifier = Modifier.height(24.dp))
        Button(onClick = onResume, modifier = Modifier.fillMaxWidth().height(56.dp)) {
            Text("继续")
        }
        Spacer(modifier = Modifier.height(12.dp))
        OutlinedButton(onClick = onQuit, modifier = Modifier.fillMaxWidth().height(56.dp)) {
            Text("退出")
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
        Text("恭喜过关！", fontSize = 32.sp, fontWeight = FontWeight.Bold, color = Color(0xFF5C6BC0))

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

        // 下一关按钮 - 只在当前难度未完成时显示
        if (!isLastInDifficulty) {
            Button(
                onClick = onNextLevel,
                modifier = Modifier.fillMaxWidth().height(56.dp),
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF5C6BC0))
            ) {
                Text("下一关", fontSize = 18.sp)
            }
            Spacer(modifier = Modifier.height(12.dp))
        } else {
            // 当前难度已完成，提示用户
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

/**
 * 全部通关屏幕
 */
@Composable
private fun AllCompletedScreen(
    state: GameState.AllCompleted,
    onBack: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text("🎉 全部通关！", fontSize = 32.sp, fontWeight = FontWeight.Bold, color = Color(0xFFFFD700))
        Spacer(modifier = Modifier.height(16.dp))
        Text("总得分: ${state.totalScore}", fontSize = 18.sp)
        Text("总用时: ${state.totalTime / 1000}s", fontSize = 18.sp)
        Spacer(modifier = Modifier.height(24.dp))
        OutlinedButton(onClick = onBack, modifier = Modifier.fillMaxWidth().height(56.dp)) {
            Text("返回", fontSize = 18.sp)
        }
    }
}
