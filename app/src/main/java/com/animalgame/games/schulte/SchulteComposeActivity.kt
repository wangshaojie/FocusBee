package com.animalgame.games.schulte

import android.os.Bundle
import androidx.activity.compose.setContent
import androidx.appcompat.app.AppCompatActivity
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.itemsIndexed
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.animalgame.ui.components.GameTopBar
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

    // 处理返回逻辑：根据当前状态决定返回行为
    // Playing/Completed 状态返回到 Idle（关卡选择）
    // Idle 状态直接退出
    val handleBack: () -> Unit = {
        when (gameState) {
            is com.animalgame.core.game.GameState.Idle -> {
                // 在关卡选择页面，直接退出
                onBack()
            }
            else -> {
                // 在游戏进行中或完成页面，返回到关卡选择
                module.resetToIdle()
            }
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFFF8F6FF))
    ) {
        when (val state = gameState) {
            is com.animalgame.core.game.GameState.Idle -> {
                // 关卡选择
                LevelSelectContent(
                    onLevelSelect = { level ->
                        module.start(level)
                    },
                    onBack = handleBack
                )
            }

            is com.animalgame.core.game.GameState.Playing -> {
                // 游戏进行中
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
                // 游戏完成 - 从 module 获取当前难度信息
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
                // 其他状态（如 Ready 倒计时），显示关卡选择
                LevelSelectContent(
                    onLevelSelect = { level ->
                        module.start(level)
                    },
                    onBack = handleBack
                )
            }
        }
    }
}

@Composable
private fun LevelSelectContent(
    onLevelSelect: (Int) -> Unit,
    onBack: () -> Unit
) {
    Column(
        modifier = Modifier.fillMaxSize()
    ) {
        GameTopBar(
            title = "舒尔特训练",
            level = 0,
            score = 0,
            stars = 0,
            onBack = onBack
        )

        // 关卡选择 - 使用 LazyColumn 支持 100 关
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            verticalArrangement = Arrangement.spacedBy(8.dp),
            contentPadding = PaddingValues(24.dp)
        ) {
            item {
                Text("舒尔特训练游戏", fontSize = 28.sp, fontWeight = FontWeight.Bold, color = Color(0xFF5C6BC0))
                Spacer(modifier = Modifier.height(16.dp))
                Text("在网格中按顺序点击数字，训练专注力", fontSize = 14.sp, color = Color(0xFF666666))
                Spacer(modifier = Modifier.height(24.dp))
            }

            items(100) { index ->
                val level = index + 1
                val gridSize = when {
                    level <= 25 -> "3×3"
                    level <= 50 -> "4×4"
                    level <= 75 -> "5×5"
                    else -> "6×6"
                }
                val difficulty = when {
                    level <= 25 -> "简单"
                    level <= 50 -> "中等"
                    level <= 75 -> "困难"
                    else -> "挑战"
                }
                Button(
                    onClick = { onLevelSelect(level) },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(56.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF4CAF50))
                ) {
                    Text("第 $level 关 ($difficulty · $gridSize)", fontSize = 16.sp)
                }
            }
        }
    }
}

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
    // 存储本地错误数字状态，用于 1 秒后自动清除红色
    var localWrongNumber by remember { mutableIntStateOf(wrongNumber) }

    // 监听 wrongNumber 变化，自动清除红色
    LaunchedEffect(wrongNumber) {
        if (wrongNumber > 0) {
            localWrongNumber = wrongNumber
            delay(1000)  // 1秒后清除红色
            localWrongNumber = -1
        }
    }

    // 计算网格大小 - 根据难度名称
    val gridSize = when (difficultyName) {
        "简单" -> 3
        "中等" -> 4
        "困难" -> 5
        "挑战" -> 6
        else -> 3
    }

    // 获取正确的关卡号用于显示
    val displayLevel = level

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

        Spacer(modifier = Modifier.height(16.dp))

        // 当前数字提示
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 24.dp),
            colors = CardDefaults.cardColors(containerColor = Color.White),
            elevation = CardDefaults.cardElevation(defaultElevation = 4.dp),
            shape = RoundedCornerShape(16.dp)
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "当前: $currentNumber",
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFF5C6BC0)
                )
                Text(
                    text = "错误: $mistakes",
                    fontSize = 16.sp,
                    color = if (mistakes > 0) Color(0xFFE53935) else Color(0xFF4CAF50)
                )
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

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
                colors = CardDefaults.cardColors(containerColor = Color(0xFFEDE7F6)),
                elevation = CardDefaults.cardElevation(defaultElevation = 8.dp),
                shape = RoundedCornerShape(20.dp)
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
                            NumberCell(
                                number = number,
                                isCorrect = isCorrect,
                                isWrong = isWrong,
                                onClick = { onNumberClick(number) }
                            )
                        }
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // 重置按钮
        OutlinedButton(
            onClick = onRestart,
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

        Spacer(modifier = Modifier.height(24.dp))
    }
}

@Composable
private fun NumberCell(
    number: Int,
    isCorrect: Boolean,
    isWrong: Boolean,
    onClick: () -> Unit
) {
    // 默认：白色背景 + 紫色文字
    // 正确点击后：绿色背景 + 白色文字 + 更高阴影
    // 错误点击（1秒内）：红色背景 + 白色文字
    val backgroundColor = when {
        isWrong -> Color(0xFFE53935)  // 红色 - 错误
        isCorrect -> Color(0xFF4CAF50)  // 绿色 - 正确
        else -> Color.White  // 默认白色
    }
    val textColor = if (isCorrect || isWrong) Color.White else Color(0xFF5C6BC0)
    val elevation = if (isCorrect) 8.dp else 4.dp

    Card(
        modifier = Modifier
            .aspectRatio(1f)
            .fillMaxWidth(0.8f)
            .clickable(onClick = onClick),
        colors = CardDefaults.cardColors(containerColor = backgroundColor),
        elevation = CardDefaults.cardElevation(defaultElevation = elevation),
        shape = RoundedCornerShape(12.dp)
    ) {
        Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = number.toString(),
                fontSize = 48.sp,
                fontWeight = FontWeight.Bold,
                color = textColor,
                textAlign = TextAlign.Center
            )
        }
    }
}

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
            Text("恭喜过关！", fontSize = 32.sp, fontWeight = FontWeight.Bold, color = Color(0xFF4CAF50))

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
                    modifier = Modifier.fillMaxWidth().height(56.dp)
                ) {
                    Text("下一关", fontSize = 18.sp)
                }
                Spacer(modifier = Modifier.height(12.dp))
            } else {
                // 当前难度已完成，提示用户
                Text(
                    text = "🎉 ${difficultyName}难度已全部通关！",
                    fontSize = 16.sp,
                    color = Color(0xFF4CAF50),
                    fontWeight = FontWeight.Medium
                )
                Spacer(modifier = Modifier.height(16.dp))
            }

            Button(
                onClick = onReplay,
                modifier = Modifier.fillMaxWidth().height(56.dp)
            ) {
                Text("重玩本关", fontSize = 18.sp)
            }
        }
    }
}