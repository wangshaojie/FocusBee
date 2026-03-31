package com.animalgame.games.lighthouse

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
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
 * 灯塔路径游戏 UI - 儿童友好版本
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

    // 夜空主题背景（灯塔主题）
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    colors = listOf(
                        Color(0xFF1A237E),  // 深蓝（夜空）
                        Color(0xFF283593),  // 蓝色
                        Color(0xFF3F51B5),  // 中蓝
                        Color(0xFF5C6BC0),  // 浅蓝
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

    // 星星闪烁动画
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
        // 各种小星星
        Text(
            text = "⭐",
            fontSize = 20.sp,
            modifier = Modifier
                .align(Alignment.TopStart)
                .offset(x = 30.dp, y = 80.dp)
                .alpha(star1Alpha)
        )
        Text(
            text = "⭐",
            fontSize = 16.sp,
            modifier = Modifier
                .align(Alignment.TopEnd)
                .offset(x = (-50).dp, y = 120.dp)
                .alpha(star2Alpha)
        )
        Text(
            text = "✨",
            fontSize = 24.sp,
            modifier = Modifier
                .align(Alignment.TopStart)
                .offset(x = 80.dp, y = 150.dp)
                .alpha(star3Alpha)
        )
        Text(
            text = "⭐",
            fontSize = 14.sp,
            modifier = Modifier
                .align(Alignment.TopEnd)
                .offset(x = (-30).dp, y = 200.dp)
                .alpha(star1Alpha)
        )
        Text(
            text = "✨",
            fontSize = 18.sp,
            modifier = Modifier
                .align(Alignment.TopStart)
                .offset(x = 150.dp, y = 100.dp)
                .alpha(star2Alpha)
        )
        Text(
            text = "⭐",
            fontSize = 12.sp,
            modifier = Modifier
                .align(Alignment.TopEnd)
                .offset(x = (-80).dp, y = 160.dp)
                .alpha(star3Alpha)
        )

        // 月亮
        Text(
            text = "🌙",
            fontSize = 50.sp,
            modifier = Modifier
                .align(Alignment.TopEnd)
                .offset(x = (-20).dp, y = 50.dp)
        )
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
            // 大标题
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
            Text(
                "选择关卡",
                fontSize = 18.sp,
                color = Color.White,
                fontWeight = FontWeight.Bold
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
    val scale by animateFloatAsState(
        targetValue = 1f,
        animationSpec = repeatable(
            iterations = 1,
            animation = tween(300, easing = FastOutSlowInEasing)
        ),
        label = "countdownScale"
    )

    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(
                text = countdown.toString(),
                fontSize = 150.sp,
                fontWeight = FontWeight.Bold,
                color = Color(0xFFFFD54F),
                modifier = Modifier.scale(scale)
            )
            Text(
                text = "🎮 准备好了吗？",
                fontSize = 24.sp,
                color = Color.White
            )
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
        colors = CardDefaults.cardColors(
            containerColor = bgColor
        ),
        shape = RoundedCornerShape(20.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalArrangement = Arrangement.Center,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = emoji,
                fontSize = 28.sp
            )
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
    cellClickCounts: IntArray,
    highlightedCell: Int,
    wrongCellIndex: Int,
    isInputEnabled: Boolean,
    onCellClick: (Int) -> Unit
) {
    val columns = 4

    Column(
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        for (row in 0 until 4) {
            Row(
                horizontalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                for (col in 0 until 4) {
                    val index = row * columns + col
                    val clickCount = if (index < cellClickCounts.size) cellClickCounts[index] else 0

                    KidFriendlyCellView(
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

// ==================== 儿童友好格子视图 ====================

@Composable
private fun KidFriendlyCellView(
    cellState: CellState,
    clickCount: Int,
    isHighlighted: Boolean,
    isWrong: Boolean,
    isInputEnabled: Boolean,
    onClick: () -> Unit
) {
    val hasCorrectClicks = clickCount > 0

    // 高亮时的放大动画
    val scale by animateFloatAsState(
        targetValue = when {
            isHighlighted -> 1.15f
            isWrong -> 1.1f
            hasCorrectClicks -> 1.05f
            else -> 1f
        },
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness = Spring.StiffnessLow
        ),
        label = "cellScale"
    )

    // 颜色动画
    val backgroundColor by animateColorAsState(
        targetValue = when {
            isWrong -> Color(0xFFFF5252)
            isHighlighted -> Color(0xFFFFD54F)
            hasCorrectClicks -> Color(0xFF69F0AE)
            else -> Color(0xFF5C6BC0).copy(alpha = 0.6f)
        },
        animationSpec = tween(durationMillis = 200),
        label = "cellColor"
    )

    // 错误时的抖动
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

    // 是否可以点击
    val canClick = isInputEnabled && !isWrong && !isHighlighted && !hasCorrectClicks

    Box(
        modifier = Modifier
            .size(78.dp)
            .offset(x = shakeOffset.dp)
            .scale(scale)
            .shadow(
                elevation = if (isHighlighted) 16.dp else 8.dp,
                shape = RoundedCornerShape(20.dp),
                spotColor = when {
                    isHighlighted -> Color(0xFFFFD54F)
                    isWrong -> Color(0xFFFF5252)
                    hasCorrectClicks -> Color(0xFF69F0AE)
                    else -> Color(0xFF3F51B5)
                }
            )
            .clip(RoundedCornerShape(20.dp))
            .background(
                brush = Brush.verticalGradient(
                    colors = when {
                        isWrong -> listOf(Color(0xFFFF5252), Color(0xFFD32F2F))
                        isHighlighted -> listOf(Color(0xFFFFE082), Color(0xFFFFD54F))
                        hasCorrectClicks -> listOf(Color(0xFF81C784), Color(0xFF4CAF50))
                        else -> listOf(Color(0xFF7986CB), Color(0xFF5C6BC0))
                    }
                )
            )
            .then(
                if (canClick) {
                    Modifier.clickable(
                        interactionSource = remember { MutableInteractionSource() },
                        indication = null,
                        onClick = onClick
                    )
                } else {
                    Modifier
                }
            ),
        contentAlignment = Alignment.Center
    ) {
        // 根据状态显示不同图标
        when {
            isHighlighted -> {
                // 灯塔发光效果
                Text(
                    text = "🏠",
                    fontSize = 40.sp
                )
            }
            isWrong -> {
                // 错误标记
                Text(
                    text = "❌",
                    fontSize = 36.sp
                )
            }
            hasCorrectClicks -> {
                // 正确标记
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = "🏠",
                        fontSize = 28.sp
                    )
                    Text(
                        text = "✓",
                        fontSize = 18.sp,
                        color = Color.White,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
            else -> {
                // 普通格子显示小房子
                Text(
                    text = "🏠",
                    fontSize = 32.sp,
                    color = Color.White.copy(alpha = 0.7f)
                )
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
            shape = RoundedCornerShape(20.dp),
            elevation = ButtonDefaults.buttonElevation(defaultElevation = 8.dp)
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
    // 成功动画
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
        // 成功/失败大表情
        Text(
            text = if (state.isSuccess) "🎉" else "😢",
            fontSize = 80.sp,
            modifier = Modifier.scale(if (state.isSuccess) successScale else 1f)
        )
        Spacer(modifier = Modifier.height(16.dp))

        // 文字
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
                colors = CardDefaults.cardColors(
                    containerColor = Color(0xFF42A5F5).copy(alpha = 0.3f)
                ),
                shape = RoundedCornerShape(16.dp)
            ) {
                Row(
                    modifier = Modifier.padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "💡",
                        fontSize = 24.sp
                    )
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
                shape = RoundedCornerShape(20.dp),
                elevation = ButtonDefaults.buttonElevation(defaultElevation = 8.dp)
            ) {
                Text("🔄 重新观看灯塔", fontSize = 18.sp, fontWeight = FontWeight.Bold, color = Color(0xFF5D4037))
            }
            Spacer(modifier = Modifier.height(12.dp))
        }

        if (state.isSuccess && module.getLevelIndex() < 10) {
            Button(
                onClick = { module.onUserAction(GameAction.NextLevel) },
                modifier = Modifier.fillMaxWidth().height(60.dp),
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF4CAF50)),
                shape = RoundedCornerShape(20.dp),
                elevation = ButtonDefaults.buttonElevation(defaultElevation = 8.dp)
            ) {
                Text("下一关 ➡️", fontSize = 20.sp, fontWeight = FontWeight.Bold)
            }
            Spacer(modifier = Modifier.height(12.dp))
        }

        Button(
            onClick = onRestart,
            modifier = Modifier.fillMaxWidth().height(60.dp),
            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFFF7043)),
            shape = RoundedCornerShape(20.dp),
            elevation = ButtonDefaults.buttonElevation(defaultElevation = 8.dp)
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
