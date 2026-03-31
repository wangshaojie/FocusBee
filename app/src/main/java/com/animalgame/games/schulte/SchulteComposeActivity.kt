package com.animalgame.games.schulte

import android.os.Bundle
import androidx.activity.compose.setContent
import androidx.appcompat.app.AppCompatActivity
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.animalgame.ui.components.GameTopBar
import com.animalgame.ui.components.DifficultyCard
import com.animalgame.ui.components.DifficultyColors
import kotlinx.coroutines.delay

class SchulteComposeActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            val module = remember { SchulteGameModule() }

            SchulteGameScreen(
                module = module,
                onBack = { finish() }
            )
        }
    }
}

@Composable
fun SchulteGameScreen(
    module: SchulteGameModule,
    onBack: () -> Unit
) {
    val gameState by module.state.collectAsState()

    val handleBack: () -> Unit = {
        when (gameState) {
            is com.animalgame.core.game.GameState.Idle -> {
                onBack()
            }
            else -> {
                module.resetToIdle()
            }
        }
    }

    // 彩虹太空主题背景
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    colors = listOf(
                        Color(0xFF1A237E),  // 深蓝（太空）
                        Color(0xFF3949AB),  // 靛蓝
                        Color(0xFF5C6BC0),  // 紫蓝
                        Color(0xFF7986CB),  // 浅紫蓝
                        Color(0xFF9FA8DA),  // 更浅
                    )
                )
            )
    ) {
        // 装饰性星星和火箭
        DecoratedSpace()

        Column(
            modifier = Modifier.fillMaxSize()
        ) {
            when (val state = gameState) {
                is com.animalgame.core.game.GameState.Idle -> {
                    LevelSelectContent(
                        module = module,
                        onBack = handleBack
                    )
                }

                is com.animalgame.core.game.GameState.Playing -> {
                    val numbers = (state.data["numbers"] as? List<Int>) ?: emptyList()
                    val currentNumber = state.data["currentNumber"] as? Int ?: 1
                    val mistakes = state.data["mistakes"] as? Int ?: 0
                    val clickedNumbersRaw = state.data["clickedNumbers"] as? Map<*, *> ?: emptyMap<Any, Boolean>()
                    val clickedNumbers: Map<Int, Boolean> = clickedNumbersRaw.mapKeys { it.key as? Int }.mapValues { it.value as? Boolean }.filterKeys { it != null } as Map<Int, Boolean>
                    val wrongNumber = state.data["wrongNumber"] as? Int ?: -1
                    val difficultyName = state.data["difficulty"] as? String
                    val levelInDifficulty = state.data["levelInDifficulty"] as? Int ?: 1

                    PlayingContent(
                        numbers = numbers,
                        currentNumber = currentNumber,
                        mistakes = mistakes,
                        clickedNumbers = clickedNumbers,
                        wrongNumber = wrongNumber,
                        level = levelInDifficulty,
                        difficultyName = difficultyName,
                        score = state.score,
                        onNumberClick = { number ->
                            module.onUserAction(com.animalgame.core.game.GameAction.TapIndex(number - 1))
                        },
                        onBack = handleBack,
                        onRestart = {
                            module.restartCurrentLevel()
                        }
                    )
                }

                is com.animalgame.core.game.GameState.Completed -> {
                    val difficultyName = module.getCurrentDifficultyName()
                    val levelInDifficulty = module.getCurrentLevelIndex()
                    val isLastInDifficulty = module.isDifficultyCompleted()

                    CompletedContent(
                        state = state,
                        difficultyName = difficultyName,
                        levelInDifficulty = levelInDifficulty,
                        isLastInDifficulty = isLastInDifficulty,
                        onNextLevel = {
                            module.nextLevel()
                        },
                        onReplay = {
                            module.restartCurrentLevel()
                        },
                        onBack = handleBack
                    )
                }

                else -> {
                    LevelSelectContent(
                        module = module,
                        onBack = handleBack
                    )
                }
            }
        }
    }
}

// ==================== 装饰性太空元素 ====================

@Composable
private fun DecoratedSpace() {
    val infiniteTransition = rememberInfiniteTransition(label = "space")
    val star1Float by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 10f,
        animationSpec = infiniteRepeatable(
            animation = tween(2000),
            repeatMode = RepeatMode.Reverse
        ),
        label = "star1"
    )
    val star2Float by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 15f,
        animationSpec = infiniteRepeatable(
            animation = tween(1500),
            repeatMode = RepeatMode.Reverse
        ),
        label = "star2"
    )
    val star3Float by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 12f,
        animationSpec = infiniteRepeatable(
            animation = tween(2500),
            repeatMode = RepeatMode.Reverse
        ),
        label = "star3"
    )

    Box(modifier = Modifier.fillMaxSize()) {
        // 星星们
        Text(
            text = "⭐",
            fontSize = 25.sp,
            modifier = Modifier
                .align(Alignment.TopStart)
                .offset(x = 30.dp, y = 100.dp + star1Float.dp)
        )
        Text(
            text = "⭐",
            fontSize = 20.sp,
            modifier = Modifier
                .align(Alignment.TopEnd)
                .offset(x = (-60).dp, y = 150.dp - star2Float.dp)
        )
        Text(
            text = "✨",
            fontSize = 30.sp,
            modifier = Modifier
                .align(Alignment.TopStart)
                .offset(x = 100.dp, y = 200.dp + star3Float.dp)
        )
        Text(
            text = "⭐",
            fontSize = 18.sp,
            modifier = Modifier
                .align(Alignment.TopEnd)
                .offset(x = (-30).dp, y = 250.dp + star1Float.dp)
        )
        Text(
            text = "✨",
            fontSize = 22.sp,
            modifier = Modifier
                .align(Alignment.TopStart)
                .offset(x = 180.dp, y = 120.dp - star2Float.dp)
        )

        // 火箭
        Text(
            text = "🚀",
            fontSize = 50.sp,
            modifier = Modifier
                .align(Alignment.TopEnd)
                .offset(x = (-20).dp, y = 80.dp)
        )

        // 月亮
        Text(
            text = "🌙",
            fontSize = 45.sp,
            modifier = Modifier
                .align(Alignment.TopStart)
                .offset(x = 40.dp, y = 60.dp)
        )

        // 数字装饰
        Text(
            text = "123",
            fontSize = 24.sp,
            color = Color.White.copy(alpha = 0.3f),
            fontWeight = FontWeight.Bold,
            modifier = Modifier
                .align(Alignment.BottomStart)
                .offset(x = 20.dp, y = (-80).dp + star3Float.dp)
        )
        Text(
            text = "456",
            fontSize = 24.sp,
            color = Color.White.copy(alpha = 0.3f),
            fontWeight = FontWeight.Bold,
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .offset(x = (-40).dp, y = (-60).dp + star1Float.dp)
        )
    }
}

// ==================== 关卡选择 ====================

@Composable
private fun LevelSelectContent(
    module: SchulteGameModule,
    onBack: () -> Unit
) {
    Column(
        modifier = Modifier.fillMaxSize()
    ) {
        GameTopBar(
            title = "🔢 舒尔特训练",
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
                "🚀 按顺序点击数字！",
                fontSize = 24.sp,
                color = Color.White,
                textAlign = TextAlign.Center,
                fontWeight = FontWeight.Bold
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                "训练你的专注力成为学霸！📚",
                fontSize = 16.sp,
                color = Color(0xFFB3E5FC),
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
                subtitle = "3×3网格 · 50关",
                color = DifficultyColors.EasyColor,
                onClick = {
                    module.setDifficulty(SchulteGameModule.Difficulty.EASY)
                    module.start(1)
                }
            )

            DifficultyCard(
                emoji = "⭐",
                title = "中等",
                subtitle = "4×4网格 · 50关",
                color = DifficultyColors.MediumColor,
                onClick = {
                    module.setDifficulty(SchulteGameModule.Difficulty.MEDIUM)
                    module.start(1)
                }
            )

            DifficultyCard(
                emoji = "🏆",
                title = "困难",
                subtitle = "5×5网格 · 50关",
                color = DifficultyColors.HardColor,
                onClick = {
                    module.setDifficulty(SchulteGameModule.Difficulty.HARD)
                    module.start(1)
                }
            )

            DifficultyCard(
                emoji = "💎",
                title = "挑战",
                subtitle = "6×6网格 · 50关",
                color = DifficultyColors.ExpertColor,
                onClick = {
                    module.setDifficulty(SchulteGameModule.Difficulty.EXPERT)
                    module.start(1)
                }
            )
        }
    }
}

// ==================== 游戏进行中 ====================

@Composable
private fun PlayingContent(
    numbers: List<Int>,
    currentNumber: Int,
    mistakes: Int,
    clickedNumbers: Map<Int, Boolean>,
    wrongNumber: Int,
    level: Int,
    difficultyName: String?,
    score: Int,
    onNumberClick: (Int) -> Unit,
    onBack: () -> Unit,
    onRestart: () -> Unit
) {
    var localWrongNumber by remember { mutableIntStateOf(wrongNumber) }

    LaunchedEffect(wrongNumber) {
        if (wrongNumber > 0) {
            localWrongNumber = wrongNumber
            delay(800)
            localWrongNumber = -1
        }
    }

    val gridSize = when (difficultyName) {
        "简单" -> 3
        "中等" -> 4
        "困难" -> 5
        "挑战" -> 6
        else -> 3
    }

    val displayLevel = level

    // 显示提示状态（点击后显示答案）
    var showHint by remember { mutableStateOf(false) }

    // 点击显示提示，3秒后自动隐藏
    LaunchedEffect(showHint) {
        if (showHint) {
            delay(3000)
            showHint = false
        }
    }

    Column(
        modifier = Modifier.fillMaxSize()
    ) {
        // 顶部导航栏
        GameTopBar(
            title = "舒尔特训练",
            level = displayLevel,
            difficultyName = difficultyName,
            score = score,
            stars = when {
                score >= 100 -> 3
                score >= 50 -> 2
                else -> 1
            },
            onBack = onBack
        )

        Spacer(modifier = Modifier.height(12.dp))

        // 儿童友好当前数字提示
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp),
            colors = CardDefaults.cardColors(
                containerColor = Color.White.copy(alpha = 0.95f)
            ),
            elevation = CardDefaults.cardElevation(defaultElevation = 8.dp),
            shape = RoundedCornerShape(24.dp)
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                horizontalArrangement = Arrangement.SpaceEvenly,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // 当前要点击的数字（点击显示，3秒后隐藏）
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier.clickable { showHint = !showHint }
                ) {
                    Text(
                        text = if (showHint) "👆 答案！" else "👆 点击显示",
                        fontSize = 14.sp,
                        color = if (showHint) Color(0xFFFF6F00) else Color(0xFF5C6BC0)
                    )
                    Text(
                        text = if (showHint) currentNumber.toString() else "?",
                        fontSize = 48.sp,
                        fontWeight = FontWeight.Bold,
                        color = if (showHint) Color(0xFFFF6F00) else Color(0xFFBDBDBD)
                    )
                }

                // 分割线
                Box(
                    modifier = Modifier
                        .width(2.dp)
                        .height(50.dp)
                        .background(Color(0xFFE0E0E0))
                )

                // 错误次数
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = if (mistakes == 0) "✅ 无错误" else "❌ 错误",
                        fontSize = 14.sp,
                        color = if (mistakes > 0) Color(0xFFE53935) else Color(0xFF4CAF50)
                    )
                    Text(
                        text = mistakes.toString(),
                        fontSize = 36.sp,
                        fontWeight = FontWeight.Bold,
                        color = if (mistakes > 0) Color(0xFFE53935) else Color(0xFF4CAF50)
                    )
                }

                // 已完成
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = "⭐ 完成",
                        fontSize = 14.sp,
                        color = Color(0xFFFFB300)
                    )
                    val completedCount = clickedNumbers.size
                    val totalCount = numbers.size
                    Text(
                        text = "$completedCount/$totalCount",
                        fontSize = 36.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFFFFB300)
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        // 数字网格
        Box(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
                .padding(horizontal = 16.dp),
            contentAlignment = Alignment.Center
        ) {
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(
                    containerColor = Color(0xFFE8EAF6).copy(alpha = 0.9f)
                ),
                elevation = CardDefaults.cardElevation(defaultElevation = 12.dp),
                shape = RoundedCornerShape(24.dp)
            ) {
                if (numbers.isNotEmpty()) {
                    LazyVerticalGrid(
                        columns = GridCells.Fixed(gridSize),
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(12.dp),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        itemsIndexed(numbers) { _, number ->
                            val isCorrect = clickedNumbers[number] == true
                            val isWrong = number == localWrongNumber
                            KidFriendlyNumberCell(
                                number = number,
                                isCorrect = isCorrect,
                                isWrong = isWrong,
                                isNextNumber = number == currentNumber,
                                onClick = { onNumberClick(number) }
                            )
                        }
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        // 重置按钮
        Button(
            onClick = onRestart,
            modifier = Modifier
                .fillMaxWidth()
                .height(56.dp)
                .padding(horizontal = 24.dp),
            colors = ButtonDefaults.buttonColors(
                containerColor = Color(0xFFFF7043)
            ),
            shape = RoundedCornerShape(20.dp),
            elevation = ButtonDefaults.buttonElevation(defaultElevation = 8.dp)
        ) {
            Text("🔄 重新开始", fontSize = 18.sp, fontWeight = FontWeight.Bold, color = Color.White)
        }

        Spacer(modifier = Modifier.height(20.dp))
    }
}

// ==================== 儿童友好数字格子 ====================

@Composable
private fun KidFriendlyNumberCell(
    number: Int,
    isCorrect: Boolean,
    isWrong: Boolean,
    isNextNumber: Boolean,
    onClick: () -> Unit
) {
    // 点击动画
    val scale by animateFloatAsState(
        targetValue = when {
            isWrong -> 1.15f
            isCorrect -> 0.95f
            else -> 1f
        },
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness = Spring.StiffnessLow
        ),
        label = "cellScale"
    )

    // 错误抖动
    val shakeOffset by animateFloatAsState(
        targetValue = if (isWrong) 8f else 0f,
        animationSpec = keyframes {
            durationMillis = 300
            0f at 0
            -8f at 75
            8f at 150
            -8f at 225
            0f at 300
        },
        label = "shake"
    )

    val backgroundColor by animateColorAsState(
        targetValue = when {
            isWrong -> Color(0xFFFF5252)
            isCorrect -> Color(0xFF69F0AE)
            isNextNumber -> Color(0xFFFFE082)
            else -> Color.White
        },
        animationSpec = tween(200),
        label = "bgColor"
    )

    val textColor = when {
        isWrong -> Color.White
        isCorrect -> Color.White
        isNextNumber -> Color(0xFFFF6F00)
        else -> Color(0xFF5C6BC0)
    }

    val fontSize = when {
        number >= 100 -> 32.sp
        number >= 10 -> 38.sp
        else -> 42.sp
    }

    Card(
        modifier = Modifier
            .aspectRatio(1f)
            .fillMaxWidth(0.85f)
            .offset(x = shakeOffset.dp)
            .scale(scale)
            .shadow(
                elevation = if (isCorrect || isNextNumber) 12.dp else 6.dp,
                shape = RoundedCornerShape(16.dp),
                spotColor = when {
                    isCorrect -> Color(0xFF69F0AE)
                    isNextNumber -> Color(0xFFFFD54F)
                    else -> Color(0xFF5C6BC0)
                }
            )
            .clip(RoundedCornerShape(16.dp))
            .background(backgroundColor)
            .clickable(onClick = onClick),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
        shape = RoundedCornerShape(16.dp)
    ) {
        Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.Center
        ) {
            when {
                isCorrect -> {
                    Text(
                        text = "✓",
                        fontSize = fontSize,
                        fontWeight = FontWeight.Bold,
                        color = textColor
                    )
                }
                isWrong -> {
                    Text(
                        text = "✗",
                        fontSize = fontSize,
                        fontWeight = FontWeight.Bold,
                        color = textColor
                    )
                }
                else -> {
                    Text(
                        text = number.toString(),
                        fontSize = fontSize,
                        fontWeight = FontWeight.Bold,
                        color = textColor,
                        textAlign = TextAlign.Center
                    )
                }
            }
        }
    }
}

// ==================== 完成页面 ====================

@Composable
private fun CompletedContent(
    state: com.animalgame.core.game.GameState.Completed,
    difficultyName: String?,
    levelInDifficulty: Int,
    isLastInDifficulty: Boolean,
    onNextLevel: () -> Unit,
    onReplay: () -> Unit,
    onBack: () -> Unit
) {
    val successScale by animateFloatAsState(
        targetValue = 1f,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness = Spring.StiffnessLow
        ),
        label = "successScale"
    )

    Column(
        modifier = Modifier.fillMaxSize()
    ) {
        GameTopBar(
            title = "舒尔特训练",
            level = levelInDifficulty,
            difficultyName = difficultyName,
            score = state.score,
            stars = state.stars,
            onBack = onBack
        )

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            // 成功大表情
            Text(
                text = "🎉",
                fontSize = 100.sp,
                modifier = Modifier.scale(successScale)
            )

            Spacer(modifier = Modifier.height(16.dp))

            Text(
                text = "太棒了！🎊",
                fontSize = 36.sp,
                fontWeight = FontWeight.Bold,
                color = Color(0xFF69F0AE)
            )

            Spacer(modifier = Modifier.height(24.dp))

            // 星星
            Row {
                repeat(3) { index ->
                    val starScale by animateFloatAsState(
                        targetValue = if (index < state.stars) 1.3f else 1f,
                        animationSpec = tween(300, delayMillis = index * 150),
                        label = "star$index"
                    )
                    Text(
                        text = if (index < state.stars) "⭐" else "☆",
                        fontSize = 50.sp,
                        modifier = Modifier.scale(starScale)
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            Text(
                "得分: ${state.score}",
                fontSize = 24.sp,
                color = Color.White,
                fontWeight = FontWeight.Bold
            )
            Text(
                "用时: ${state.timeMillis / 1000}秒",
                fontSize = 18.sp,
                color = Color(0xFFB3E5FC)
            )

            Spacer(modifier = Modifier.height(32.dp))

            // 下一关按钮
            if (!isLastInDifficulty) {
                Button(
                    onClick = onNextLevel,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(60.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Color(0xFF4CAF50)
                    ),
                    shape = RoundedCornerShape(20.dp),
                    elevation = ButtonDefaults.buttonElevation(defaultElevation = 8.dp)
                ) {
                    Text("下一关 ➡️", fontSize = 20.sp, fontWeight = FontWeight.Bold)
                }
                Spacer(modifier = Modifier.height(12.dp))
            } else {
                Card(
                    colors = CardDefaults.cardColors(
                        containerColor = Color(0xFFFFB300).copy(alpha = 0.3f)
                    ),
                    shape = RoundedCornerShape(16.dp)
                ) {
                    Row(
                        modifier = Modifier.padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(text = "🎉", fontSize = 24.sp)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "${difficultyName}难度已全部通关！",
                            fontSize = 16.sp,
                            color = Color(0xFFFFB300),
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
                Spacer(modifier = Modifier.height(12.dp))
            }

            Button(
                onClick = onReplay,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(60.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = Color(0xFFFF7043)
                ),
                shape = RoundedCornerShape(20.dp),
                elevation = ButtonDefaults.buttonElevation(defaultElevation = 8.dp)
            ) {
                Text("🔄 重玩本关", fontSize = 20.sp, fontWeight = FontWeight.Bold, color = Color.White)
            }

            Spacer(modifier = Modifier.height(12.dp))

            OutlinedButton(
                onClick = onBack,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(60.dp),
                shape = RoundedCornerShape(20.dp)
            ) {
                Text("🏠 返回", fontSize = 20.sp)
            }
        }
    }
}
