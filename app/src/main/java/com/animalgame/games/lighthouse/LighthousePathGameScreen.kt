package com.animalgame.games.lighthouse

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.animalgame.core.game.GameAction
import com.animalgame.core.game.GameModule
import com.animalgame.core.game.GameState
import com.animalgame.ui.components.DifficultyCard
import com.animalgame.ui.components.DifficultyColors
import com.animalgame.ui.components.GameTopBar

/**
 * 灯塔路径游戏 UI
 */
@Composable
fun LighthousePathGameScreen(
    module: GameModule,
    onBack: () -> Unit
) {
    val lighthouseModule = module as? LighthousePathGameModule
    val gameState by module.state.collectAsState()

    val handleBack: () -> Unit = {
        when (gameState) {
            is GameState.Idle -> onBack()
            else -> lighthouseModule?.resetToIdle() ?: module.onUserAction(GameAction.Quit)
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFFFFF8E1))
    ) {
        when (val state = gameState) {
            is GameState.Idle -> {
                LevelSelectScreen(
                    module = lighthouseModule!!,
                    onBack = handleBack
                )
            }
            is GameState.Ready -> {
                ReadyScreen(countdown = state.countdown)
            }
            is GameState.Playing -> {
                PlayingScreen(
                    state = state,
                    module = lighthouseModule!!,
                    onBack = handleBack
                )
            }
            is GameState.Paused -> {
                PausedScreen(
                    elapsedTime = state.elapsedTime,
                    score = state.score,
                    onResume = { module.onUserAction(GameAction.Resume) },
                    onQuit = { module.onUserAction(GameAction.Quit) }
                )
            }
            is GameState.Completed -> {
                CompletedScreen(
                    state = state,
                    module = lighthouseModule!!,
                    onReplay = { lighthouseModule.replaySequence() },
                    onRestart = { lighthouseModule.restartLevel() },
                    onBack = handleBack
                )
            }
            is GameState.AllCompleted -> {
                AllCompletedScreen(
                    state = state,
                    onBack = handleBack
                )
            }
        }
    }
}

// ==================== 关卡选择 ====================

@Composable
private fun LevelSelectScreen(
    module: LighthousePathGameModule,
    onBack: () -> Unit
) {
    Column(modifier = Modifier.fillMaxSize()) {
        GameTopBar(
            title = "🏠 灯塔路径",
            level = 0,
            score = 0,
            stars = 0,
            onBack = onBack
        )

        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                "🏠 记住灯塔闪烁的顺序！",
                fontSize = 18.sp,
                color = Color(0xFF5D4037),
                textAlign = TextAlign.Center,
                fontWeight = FontWeight.Bold
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                "灯塔会依次亮起，记住顺序后点击",
                fontSize = 14.sp,
                color = Color(0xFF8D6E63),
                textAlign = TextAlign.Center
            )
        }

        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 24.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Text(
                "选择关卡",
                fontSize = 16.sp,
                color = Color(0xFF5D4037),
                fontWeight = FontWeight.Medium
            )

            DifficultyCard(
                emoji = "🌱",
                title = "入门 (1-5关)",
                subtitle = "2-3个灯塔，简单顺序",
                color = DifficultyColors.EasyColor,
                onClick = { module.start(1) }
            )

            DifficultyCard(
                emoji = "🌿",
                title = "进阶 (6-10关)",
                subtitle = "4-5个灯塔，稍复杂",
                color = DifficultyColors.MediumColor,
                onClick = { module.start(6) }
            )

            DifficultyCard(
                emoji = "🌳",
                title = "挑战 (11-20关)",
                subtitle = "6-8个灯塔，需要记忆",
                color = DifficultyColors.HardColor,
                onClick = { module.start(11) }
            )

            DifficultyCard(
                emoji = "🏆",
                title = "极限 (21-30关)",
                subtitle = "8+个灯塔，挑战极限",
                color = DifficultyColors.ExpertColor,
                onClick = { module.start(21) }
            )
        }
    }
}

// ==================== 倒计时 ====================

@Composable
private fun ReadyScreen(countdown: Int) {
    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = countdown.toString(),
            fontSize = 120.sp,
            fontWeight = FontWeight.Bold,
            color = Color(0xFFFFD54F)
        )
    }
}

// ==================== 游戏进行中 ====================

@Composable
private fun PlayingScreen(
    state: GameState.Playing,
    module: LighthousePathGameModule,
    onBack: () -> Unit
) {
    // 解析游戏数据
    val cellStatesData = state.data["cellStates"] as? List<*> ?: emptyList<Any>()
    val cellClickCountsData = state.data["cellClickCounts"] as? List<*> ?: emptyList<Any>()
    val highlightedCell = state.data["highlightedCell"] as? Int ?: -1
    val currentPhaseStr = state.data["currentPhase"] as? String ?: "SHOWING_SEQUENCE"
    val playerProgress = state.data["playerProgress"] as? Int ?: 0
    val totalToMatch = state.data["totalToMatch"] as? Int ?: 0
    val wrongCellIndex = state.data["wrongCellIndex"] as? Int ?: -1

    val currentPhase = try {
        GamePhase.valueOf(currentPhaseStr)
    } catch (e: Exception) {
        GamePhase.SHOWING_SEQUENCE
    }

    // 转换格子状态
    val cellStates = remember(cellStatesData) {
        cellStatesData.mapIndexedNotNull { index, item ->
            try {
                CellState.valueOf(item as String)
            } catch (e: Exception) {
                CellState.NORMAL
            }
        }.toTypedArray()
    }.let { states ->
        if (states.size == 16) states else Array(16) { CellState.NORMAL }
    }

    // 转换点击计数
    val cellClickCounts = remember(cellClickCountsData) {
        cellClickCountsData.mapNotNull { (it as? Number)?.toInt() ?: 0 }.toIntArray()
    }.let { counts ->
        if (counts.size == 16) counts else IntArray(16) { 0 }
    }

    Column(modifier = Modifier.fillMaxSize()) {
        GameTopBar(
            title = "灯塔路径",
            level = module.getLevelIndex(),
            difficultyName = module.getDifficultyName(),
            score = state.score,
            stars = 0,
            onBack = onBack
        )

        // 状态提示
        StatusBar(
            currentPhase = currentPhase,
            playerProgress = playerProgress,
            totalToMatch = totalToMatch,
            modifier = Modifier.padding(horizontal = 24.dp, vertical = 8.dp)
        )

        // 游戏网格
        Box(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
                .padding(16.dp),
            contentAlignment = Alignment.Center
        ) {
            GridView(
                cellStates = cellStates,
                cellClickCounts = cellClickCounts,
                highlightedCell = highlightedCell,
                wrongCellIndex = wrongCellIndex,
                isInputEnabled = currentPhase == GamePhase.WAITING_INPUT,
                onCellClick = { index ->
                    module.onUserAction(GameAction.TapIndex(index))
                }
            )
        }

        // 操作提示
        Text(
            text = when (currentPhase) {
                GamePhase.SHOWING_SEQUENCE -> "仔细记住灯塔闪烁的顺序..."
                GamePhase.WAITING_INPUT -> "现在按顺序点击刚才闪烁的格子"
                else -> ""
            },
            fontSize = 14.sp,
            color = Color(0xFF8D6E63),
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 24.dp),
            textAlign = TextAlign.Center
        )
    }
}

// ==================== 网格视图 ====================

@Composable
private fun GridView(
    cellStates: Array<CellState>,
    cellClickCounts: IntArray,
    highlightedCell: Int,
    wrongCellIndex: Int,
    isInputEnabled: Boolean,
    onCellClick: (Int) -> Unit
) {
    val columns = 4

    Column(
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        for (row in 0 until 4) {
            Row(
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                for (col in 0 until 4) {
                    val index = row * columns + col
                    val clickCount = if (index < cellClickCounts.size) cellClickCounts[index] else 0

                    CellView(
                        cellState = cellStates.getOrElse(index) { CellState.NORMAL },
                        clickCount = clickCount,
                        isHighlighted = index == highlightedCell,
                        isWrong = index == wrongCellIndex,
                        isInputEnabled = isInputEnabled,
                        onClick = { onCellClick(index) }
                    )
                }
            }
        }
    }
}

// ==================== 格子视图 ====================

@Composable
private fun CellView(
    cellState: CellState,
    clickCount: Int,
    isHighlighted: Boolean,
    isWrong: Boolean,
    isInputEnabled: Boolean,
    onClick: () -> Unit
) {
    // 是否已正确点击过（点击次数 > 0）
    val hasCorrectClicks = clickCount > 0

    // 目标颜色
    val targetColor = when {
        isWrong -> Color(0xFFE57373)                                // 红色
        isHighlighted -> Color(0xFFFFD54F)                          // 亮黄色（灯塔）
        hasCorrectClicks -> Color(0xFF81C784)                      // 绿色（已正确点击）
        else -> Color(0xFFE0E0E0)                                   // 普通灰色
    }

    val backgroundColor by animateColorAsState(
        targetValue = targetColor,
        animationSpec = tween(durationMillis = 150),
        label = "cellColor"
    )

    // 高亮时的发光效果
    val elevation = if (isHighlighted) 12.dp else 4.dp

    // 是否可以点击：输入阶段 且 未错误 且 未高亮（高亮时不能点击）
    val canClick = isInputEnabled && !isWrong && !isHighlighted

    Card(
        modifier = Modifier
            .size(72.dp)
            .then(
                if (canClick) {
                    Modifier.clickable(onClick = onClick)
                } else {
                    Modifier
                }
            ),
        colors = CardDefaults.cardColors(containerColor = backgroundColor),
        elevation = CardDefaults.cardElevation(defaultElevation = elevation),
        shape = RoundedCornerShape(16.dp)
    ) {
        Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.Center
        ) {
            // 灯塔图标（当高亮时显示）
            if (isHighlighted) {
                Text(
                    text = "💡",
                    fontSize = 32.sp
                )
            }
            // 错误标记
            else if (isWrong) {
                Text(
                    text = "✗",
                    fontSize = 32.sp,
                    color = Color.White,
                    fontWeight = FontWeight.Bold
                )
            }
            // 正确标记（已点击过）
            else if (hasCorrectClicks) {
                Text(
                    text = "✓",
                    fontSize = 32.sp,
                    color = Color.White,
                    fontWeight = FontWeight.Bold
                )
            }
        }
    }
}

// ==================== 状态栏 ====================

@Composable
private fun StatusBar(
    currentPhase: GamePhase,
    playerProgress: Int,
    totalToMatch: Int,
    modifier: Modifier = Modifier
) {
    val (text, color) = when (currentPhase) {
        GamePhase.SHOWING_SEQUENCE -> "观看灯塔闪烁..." to Color(0xFFFFD54F)
        GamePhase.WAITING_INPUT -> "已记住: $playerProgress / $totalToMatch" to Color(0xFF81C784)
        GamePhase.RESULT -> "结果" to Color(0xFF9E9E9E)
    }

    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.Center,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = text,
            fontSize = 18.sp,
            fontWeight = FontWeight.Medium,
            color = color
        )
    }
}

// ==================== 暂停 ====================

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
        Text("⏸️ 游戏暂停", fontSize = 28.sp, fontWeight = FontWeight.Bold, color = Color(0xFF5D4037))
        Spacer(modifier = Modifier.height(24.dp))
        Text("得分: $score", fontSize = 18.sp, color = Color(0xFF5D4037))
        Text("用时: ${elapsedTime / 1000}s", fontSize = 18.sp, color = Color(0xFF8D6E63))
        Spacer(modifier = Modifier.height(32.dp))
        Button(
            onClick = onResume,
            modifier = Modifier.fillMaxWidth().height(56.dp),
            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF81C784)),
            shape = RoundedCornerShape(16.dp)
        ) {
            Text("继续", fontSize = 18.sp, fontWeight = FontWeight.Bold)
        }
        Spacer(modifier = Modifier.height(12.dp))
        OutlinedButton(
            onClick = onQuit,
            modifier = Modifier.fillMaxWidth().height(56.dp),
            shape = RoundedCornerShape(16.dp)
        ) {
            Text("退出", fontSize = 18.sp)
        }
    }
}

// ==================== 完成 ====================

@Composable
private fun CompletedScreen(
    state: GameState.Completed,
    module: LighthousePathGameModule,
    onReplay: () -> Unit,
    onRestart: () -> Unit,
    onBack: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text(
            text = if (state.isSuccess) "🎉" else "😢",
            fontSize = 64.sp
        )
        Spacer(modifier = Modifier.height(16.dp))
        Text(
            text = if (state.isSuccess) "灯塔之旅完成！" else "记忆有误",
            fontSize = 28.sp,
            fontWeight = FontWeight.Bold,
            color = if (state.isSuccess) Color(0xFF81C784) else Color(0xFFE57373)
        )
        Spacer(modifier = Modifier.height(24.dp))

        Row {
            repeat(3) { index ->
                Text(
                    text = if (index < state.stars) "⭐" else "☆",
                    fontSize = 40.sp
                )
            }
        }

        Spacer(modifier = Modifier.height(16.dp))
        Text("得分: ${state.score}", fontSize = 20.sp, color = Color(0xFF5D4037))
        Text(
            "关卡: ${module.getDifficultyName()} · 第${module.getLevelIndex()}关",
            fontSize = 16.sp,
            color = Color(0xFF8D6E63)
        )
        Text(
            "序列长度: ${module.getSequenceLength()} 个灯塔",
            fontSize = 14.sp,
            color = Color(0xFF8D6E63)
        )
        Spacer(modifier = Modifier.height(32.dp))

        // 失败后显示：重新播放提示 + 重玩按钮
        if (!state.isSuccess) {
            Text(
                text = "📺 可以重新观看灯塔闪烁来帮助记忆",
                fontSize = 14.sp,
                color = Color(0xFF8D6E63),
                textAlign = TextAlign.Center
            )
            Spacer(modifier = Modifier.height(12.dp))

            Button(
                onClick = onReplay,
                modifier = Modifier.fillMaxWidth().height(48.dp),
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFFFD54F)),
                shape = RoundedCornerShape(16.dp)
            ) {
                Text("🔄 重新观看灯塔", fontSize = 16.sp, fontWeight = FontWeight.Bold, color = Color(0xFF5D4037))
            }
            Spacer(modifier = Modifier.height(12.dp))
        }

        if (state.isSuccess && module.getLevelIndex() < 10) {
            Button(
                onClick = { module.onUserAction(GameAction.NextLevel) },
                modifier = Modifier.fillMaxWidth().height(56.dp),
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF81C784)),
                shape = RoundedCornerShape(16.dp)
            ) {
                Text("下一关", fontSize = 18.sp, fontWeight = FontWeight.Bold)
            }
            Spacer(modifier = Modifier.height(12.dp))
        }

        Button(
            onClick = onRestart,
            modifier = Modifier.fillMaxWidth().height(56.dp),
            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFFFAB91)),
            shape = RoundedCornerShape(16.dp)
        ) {
            Text("重玩本关", fontSize = 18.sp, fontWeight = FontWeight.Bold, color = Color(0xFF5D4037))
        }
        Spacer(modifier = Modifier.height(12.dp))
        OutlinedButton(
            onClick = onBack,
            modifier = Modifier.fillMaxWidth().height(56.dp),
            shape = RoundedCornerShape(16.dp)
        ) {
            Text("返回", fontSize = 18.sp)
        }
    }
}

// ==================== 全部通关 ====================

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
        Text("🏆", fontSize = 80.sp)
        Spacer(modifier = Modifier.height(16.dp))
        Text("🎉 全部通关！", fontSize = 32.sp, fontWeight = FontWeight.Bold, color = Color(0xFFFFD700))
        Spacer(modifier = Modifier.height(24.dp))
        Text("总得分: ${state.totalScore}", fontSize = 20.sp, color = Color(0xFF5D4037))
        Text("总用时: ${state.totalTime / 1000}s", fontSize = 18.sp, color = Color(0xFF8D6E63))
        Spacer(modifier = Modifier.height(32.dp))
        OutlinedButton(
            onClick = onBack,
            modifier = Modifier.fillMaxWidth().height(56.dp),
            shape = RoundedCornerShape(16.dp)
        ) {
            Text("返回", fontSize = 18.sp)
        }
    }
}
