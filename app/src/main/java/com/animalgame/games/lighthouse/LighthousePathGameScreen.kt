package com.animalgame.games.lighthouse

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.animalgame.core.game.GameAction
import com.animalgame.core.game.GameModule
import com.animalgame.core.game.GameState
import com.animalgame.ui.components.GameTopBar

/**
 * 灯塔路径游戏 UI - 记忆序列版本
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
            else -> lighthouseModule?.resetToIdle()
        }
    }

    // 夜空主题背景
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    colors = listOf(
                        Color(0xFF1A237E),
                        Color(0xFF283593),
                        Color(0xFF3F51B5),
                        Color(0xFF5C6BC0),
                    )
                )
            )
    ) {
        // 装饰性星星
        DecoratedStars()

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

// ==================== 装饰性星星 ====================

@Composable
private fun DecoratedStars() {
    val infiniteTransition = rememberInfiniteTransition(label = "stars")

    val star1Alpha by infiniteTransition.animateFloat(
        initialValue = 0.3f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(1000),
            repeatMode = RepeatMode.Reverse
        ),
        label = "star1"
    )
    val star2Alpha by infiniteTransition.animateFloat(
        initialValue = 0.5f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(800),
            repeatMode = RepeatMode.Reverse
        ),
        label = "star2"
    )
    val star3Alpha by infiniteTransition.animateFloat(
        initialValue = 0.4f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(1200),
            repeatMode = RepeatMode.Reverse
        ),
        label = "star3"
    )

    Box(modifier = Modifier.fillMaxSize()) {
        Text("⭐", fontSize = 20.sp, modifier = Modifier.align(Alignment.TopStart).offset(x = 30.dp, y = 80.dp).alpha(star1Alpha))
        Text("⭐", fontSize = 16.sp, modifier = Modifier.align(Alignment.TopEnd).offset(x = (-50).dp, y = 120.dp).alpha(star2Alpha))
        Text("✨", fontSize = 24.sp, modifier = Modifier.align(Alignment.TopStart).offset(x = 80.dp, y = 150.dp).alpha(star3Alpha))
        Text("⭐", fontSize = 14.sp, modifier = Modifier.align(Alignment.TopEnd).offset(x = (-30).dp, y = 200.dp).alpha(star1Alpha))
        Text("✨", fontSize = 18.sp, modifier = Modifier.align(Alignment.TopStart).offset(x = 150.dp, y = 100.dp).alpha(star2Alpha))
        Text("⭐", fontSize = 12.sp, modifier = Modifier.align(Alignment.TopEnd).offset(x = (-80).dp, y = 160.dp).alpha(star3Alpha))
        Text("🌙", fontSize = 50.sp, modifier = Modifier.align(Alignment.TopEnd).offset(x = (-20).dp, y = 50.dp))
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
                fontSize = 24.sp,
                color = Color.White,
                textAlign = TextAlign.Center,
                fontWeight = FontWeight.Bold
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                "灯塔会依次亮起 💡 记住顺序后点击！",
                fontSize = 16.sp,
                color = Color(0xFFB3E5FC),
                textAlign = TextAlign.Center
            )
        }

        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 24.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Text("选择难度", fontSize = 18.sp, color = Color.White, fontWeight = FontWeight.Bold)

            // 入门 1-50
            DifficultyCard(
                emoji = "🌱",
                title = "入门 (1-50关)",
                subtitle = "3-4个灯塔，简单顺序",
                color = Color(0xFF4CAF50),
                onClick = { module.start(1) }
            )

            // 进阶 51-100
            DifficultyCard(
                emoji = "🌿",
                title = "进阶 (51-100关)",
                subtitle = "5-6个灯塔，稍复杂",
                color = Color(0xFF2196F3),
                onClick = { module.start(51) }
            )

            // 挑战 101-150
            DifficultyCard(
                emoji = "🌳",
                title = "挑战 (101-150关)",
                subtitle = "7-8个灯塔，需要记忆",
                color = Color(0xFFFF9800),
                onClick = { module.start(101) }
            )

            // 极限 151-200
            DifficultyCard(
                emoji = "🏆",
                title = "极限 (151-200关)",
                subtitle = "9-10个灯塔，挑战极限",
                color = Color(0xFFF44336),
                onClick = { module.start(151) }
            )
        }
    }
}

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
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = color.copy(alpha = 0.8f)),
        shape = RoundedCornerShape(16.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(20.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(emoji, fontSize = 36.sp)
            Spacer(modifier = Modifier.width(16.dp))
            Column {
                Text(title, fontSize = 20.sp, fontWeight = FontWeight.Bold, color = Color.White)
                Text(subtitle, fontSize = 14.sp, color = Color.White.copy(alpha = 0.8f))
            }
        }
    }
}

// ==================== 倒计时 ====================

@Composable
private fun ReadyScreen(countdown: Int) {
    val scale by animateFloatAsState(
        targetValue = 1f,
        animationSpec = repeatable(
            iterations = 1,
            animation = tween(300, easing = FastOutSlowInEasing)
        ),
        label = "countdownScale"
    )

    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(
                text = countdown.toString(),
                fontSize = 150.sp,
                fontWeight = FontWeight.Bold,
                color = Color(0xFFFFD54F),
                modifier = Modifier.scale(scale)
            )
            Text("🎮 准备好了吗？", fontSize = 24.sp, color = Color.White)
        }
    }
}

// ==================== 游戏进行中 ====================

@Composable
private fun PlayingScreen(
    state: GameState.Playing,
    module: LighthousePathGameModule,
    onBack: () -> Unit
) {
    val cellStatesData = state.data["cellStates"] as? List<*> ?: emptyList<Any>()
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
        cellStatesData.mapIndexedNotNull { _, item ->
            try {
                CellState.valueOf(item as String)
            } catch (e: Exception) {
                CellState.NORMAL
            }
        }.toTypedArray()
    }.let { states ->
        if (states.size == 16) states else Array(16) { CellState.NORMAL }
    }

    Column(modifier = Modifier.fillMaxSize()) {
        GameTopBar(
            title = "🏠 ${module.getLevelId()}",
            level = module.getLevelIndex(),
            difficultyName = module.getDifficultyName(),
            score = state.score,
            stars = 0,
            onBack = onBack
        )

        // 儿童友好状态栏
        KidFriendlyStatusBar(
            currentPhase = currentPhase,
            playerProgress = playerProgress,
            totalToMatch = totalToMatch,
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
        )

        // 游戏网格
        Box(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
                .padding(12.dp),
            contentAlignment = Alignment.Center
        ) {
            KidFriendlyGridView(
                cellStates = cellStates,
                highlightedCell = highlightedCell,
                wrongCellIndex = wrongCellIndex,
                playerProgress = playerProgress,
                isInputEnabled = currentPhase == GamePhase.WAITING_INPUT,
                onCellClick = { index ->
                    android.util.Log.d("LighthouseDebug", "Cell clicked: index=$index, phase=$currentPhase, playerProgress=$playerProgress")
                    module.onUserAction(GameAction.TapIndex(index))
                }
            )
        }

        // 操作提示
        Text(
            text = when (currentPhase) {
                GamePhase.SHOWING_SEQUENCE -> "💡 仔细记住灯塔闪烁的顺序..."
                GamePhase.WAITING_INPUT -> "👆 现在按顺序点击刚才闪烁的格子"
                else -> ""
            },
            fontSize = 16.sp,
            color = Color(0xFFB3E5FC),
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 20.dp),
            textAlign = TextAlign.Center
        )
    }
}

// ==================== 儿童友好状态栏 ====================

@Composable
private fun KidFriendlyStatusBar(
    currentPhase: GamePhase,
    playerProgress: Int,
    totalToMatch: Int,
    modifier: Modifier = Modifier
) {
    val (emoji, text, bgColor) = when (currentPhase) {
        GamePhase.SHOWING_SEQUENCE -> Triple("👀", "观看灯塔闪烁...", Color(0xFFFFD54F).copy(alpha = 0.3f))
        GamePhase.WAITING_INPUT -> Triple("🧠", "已记住: $playerProgress / $totalToMatch", Color(0xFF81C784).copy(alpha = 0.3f))
        GamePhase.RESULT -> Triple("✨", "结果", Color(0xFF9E9E9E).copy(alpha = 0.3f))
    }

    Card(
        modifier = modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = bgColor),
        shape = RoundedCornerShape(20.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalArrangement = Arrangement.Center,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(text = emoji, fontSize = 28.sp)
            Spacer(modifier = Modifier.width(12.dp))
            Text(
                text = text,
                fontSize = 22.sp,
                fontWeight = FontWeight.Bold,
                color = Color.White
            )
        }
    }
}

// ==================== 儿童友好网格视图 ====================

@Composable
private fun KidFriendlyGridView(
    cellStates: Array<CellState>,
    highlightedCell: Int,
    wrongCellIndex: Int,
    playerProgress: Int,
    isInputEnabled: Boolean,
    onCellClick: (Int) -> Unit
) {
    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        for (row in 0 until 4) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                for (col in 0 until 4) {
                    val index = row * 4 + col
                    KidFriendlyCellView(
                        cellState = cellStates.getOrElse(index) { CellState.NORMAL },
                        isHighlighted = index == highlightedCell,
                        isWrong = index == wrongCellIndex,
                        isCorrect = cellStates.getOrElse(index) { CellState.NORMAL } == CellState.CORRECT,
                        isInputEnabled = isInputEnabled,
                        onClick = { onCellClick(index) },
                        modifier = Modifier.weight(1f)
                    )
                }
            }
        }
    }
}

// ==================== 儿童友好格子视图 ====================

@Composable
private fun KidFriendlyCellView(
    cellState: CellState,
    isHighlighted: Boolean,
    isWrong: Boolean,
    isCorrect: Boolean,
    isInputEnabled: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    // 高亮时的放大动画
    val scale by animateFloatAsState(
        targetValue = when {
            isHighlighted -> 1.1f
            isWrong -> 1.05f
            isCorrect -> 1.02f
            else -> 1f
        },
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness = Spring.StiffnessLow
        ),
        label = "cellScale"
    )

    // 错误时的抖动
    val shakeOffsetX by animateFloatAsState(
        targetValue = if (isWrong) 4f else 0f,
        animationSpec = keyframes {
            durationMillis = 300
            0f at 0
            -4f at 75
            4f at 150
            -4f at 225
            0f at 300
        },
        label = "shakeX"
    )

    // 背景颜色
    val backgroundColor = when {
        isWrong -> Color(0xFFFF5252)
        isHighlighted -> Color(0xFFFFD54F)
        isCorrect -> Color(0xFF4CAF50)
        else -> Color(0xFF5C6BC0)
    }

    Box(
        modifier = modifier
            .aspectRatio(1f)
            .shadow(elevation = if (isHighlighted) 8.dp else 4.dp, shape = RoundedCornerShape(12.dp))
            .clip(RoundedCornerShape(12.dp))
            .background(backgroundColor)
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
                onClick = onClick
            ),
        contentAlignment = Alignment.Center
    ) {
        Box(
            modifier = Modifier.offset(x = shakeOffsetX.dp)
                .graphicsLayer(scaleX = scale, scaleY = scale),
            contentAlignment = Alignment.Center
        ) {
            when {
                isHighlighted -> Text("🏠", fontSize = 36.sp)
                isWrong -> Text("❌", fontSize = 32.sp)
                isCorrect -> Text("🏠", fontSize = 32.sp)
                else -> Text("🏠", fontSize = 32.sp, color = Color.White.copy(alpha = 0.7f))
            }
        }
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
        Text("⏸️ 游戏暂停", fontSize = 32.sp, fontWeight = FontWeight.Bold, color = Color.White)
        Spacer(modifier = Modifier.height(24.dp))
        Text("得分: $score", fontSize = 22.sp, color = Color(0xFFFFD54F), fontWeight = FontWeight.Bold)
        Text("用时: ${elapsedTime / 1000}s", fontSize = 18.sp, color = Color(0xFFB3E5FC))
        Spacer(modifier = Modifier.height(32.dp))
        Button(
            onClick = onResume,
            modifier = Modifier.fillMaxWidth().height(60.dp),
            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF4CAF50)),
            shape = RoundedCornerShape(20.dp)
        ) {
            Text("▶️ 继续", fontSize = 20.sp, fontWeight = FontWeight.Bold)
        }
        Spacer(modifier = Modifier.height(12.dp))
        OutlinedButton(
            onClick = onQuit,
            modifier = Modifier.fillMaxWidth().height(60.dp),
            shape = RoundedCornerShape(20.dp)
        ) {
            Text("🏠 返回", fontSize = 20.sp)
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
    val successScale by animateFloatAsState(
        targetValue = if (state.isSuccess) 1f else 0f,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness = Spring.StiffnessLow
        ),
        label = "successScale"
    )

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text(
            text = if (state.isSuccess) "🎉" else "😢",
            fontSize = 80.sp,
            modifier = Modifier.scale(if (state.isSuccess) successScale else 1f)
        )
        Spacer(modifier = Modifier.height(16.dp))

        Text(
            text = if (state.isSuccess) "太棒了！🎊" else "加油哦！💪",
            fontSize = 32.sp,
            fontWeight = FontWeight.Bold,
            color = if (state.isSuccess) Color(0xFF69F0AE) else Color(0xFFFF7043)
        )
        Spacer(modifier = Modifier.height(24.dp))

        // 星星
        Row {
            repeat(3) { index ->
                val starScale by animateFloatAsState(
                    targetValue = if (index < state.stars) 1.2f else 1f,
                    animationSpec = tween(300, delayMillis = index * 150),
                    label = "star$index"
                )
                Text(
                    text = if (index < state.stars) "⭐" else "☆",
                    fontSize = 48.sp,
                    modifier = Modifier.scale(starScale)
                )
            }
        }

        Spacer(modifier = Modifier.height(16.dp))
        Text("得分: ${state.score}", fontSize = 22.sp, color = Color.White, fontWeight = FontWeight.Bold)
        Text(
            "关卡: ${module.getDifficultyName()} · 第${module.getLevelIndex()}关",
            fontSize = 18.sp,
            color = Color(0xFFB3E5FC)
        )
        Text(
            "序列长度: ${module.getSequenceLength()} 个灯塔",
            fontSize = 16.sp,
            color = Color(0xFFB3E5FC)
        )
        Spacer(modifier = Modifier.height(32.dp))

        // 失败后显示：重新播放提示 + 重玩按钮
        if (!state.isSuccess) {
            Card(
                colors = CardDefaults.cardColors(containerColor = Color(0xFF42A5F5).copy(alpha = 0.3f)),
                shape = RoundedCornerShape(16.dp)
            ) {
                Row(modifier = Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
                    Text(text = "💡", fontSize = 24.sp)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "可以重新观看灯塔闪烁来帮助记忆",
                        fontSize = 15.sp,
                        color = Color.White
                    )
                }
            }
            Spacer(modifier = Modifier.height(12.dp))

            Button(
                onClick = onReplay,
                modifier = Modifier.fillMaxWidth().height(56.dp),
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFFFD54F)),
                shape = RoundedCornerShape(20.dp)
            ) {
                Text("🔄 重新观看灯塔", fontSize = 18.sp, fontWeight = FontWeight.Bold, color = Color(0xFF5D4037))
            }
            Spacer(modifier = Modifier.height(12.dp))
        }

        if (state.isSuccess && module.getLevelIndex() < 50) {
            Button(
                onClick = { module.onUserAction(GameAction.NextLevel) },
                modifier = Modifier.fillMaxWidth().height(60.dp),
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF4CAF50)),
                shape = RoundedCornerShape(20.dp)
            ) {
                Text("下一关 ➡️", fontSize = 20.sp, fontWeight = FontWeight.Bold)
            }
            Spacer(modifier = Modifier.height(12.dp))
        }

        Button(
            onClick = onRestart,
            modifier = Modifier.fillMaxWidth().height(60.dp),
            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFFF7043)),
            shape = RoundedCornerShape(20.dp)
        ) {
            Text("🔄 重玩本关", fontSize = 20.sp, fontWeight = FontWeight.Bold, color = Color.White)
        }
        Spacer(modifier = Modifier.height(12.dp))
        OutlinedButton(
            onClick = onBack,
            modifier = Modifier.fillMaxWidth().height(60.dp),
            shape = RoundedCornerShape(20.dp)
        ) {
            Text("🏠 返回", fontSize = 20.sp)
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
        Text("🏆", fontSize = 100.sp)
        Spacer(modifier = Modifier.height(16.dp))
        Text("🎉 全部通关！", fontSize = 36.sp, fontWeight = FontWeight.Bold, color = Color(0xFFFFD54F))
        Spacer(modifier = Modifier.height(24.dp))
        Text("总得分: ${state.totalScore}", fontSize = 24.sp, color = Color.White, fontWeight = FontWeight.Bold)
        Text("总用时: ${state.totalTime / 1000}s", fontSize = 20.sp, color = Color(0xFFB3E5FC))
        Spacer(modifier = Modifier.height(32.dp))
        OutlinedButton(
            onClick = onBack,
            modifier = Modifier.fillMaxWidth().height(60.dp),
            shape = RoundedCornerShape(20.dp)
        ) {
            Text("🏠 返回", fontSize = 20.sp)
        }
    }
}
