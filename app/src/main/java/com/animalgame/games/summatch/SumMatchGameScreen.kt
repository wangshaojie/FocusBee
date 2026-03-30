package com.animalgame.games.summatch

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
import androidx.compose.ui.geometry.Offset
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
import kotlin.math.roundToInt

/**
 * 数字连连看游戏 UI - 游戏感版本
 */
@Composable
fun SumMatchGameScreen(
    module: GameModule,
    onBack: () -> Unit
) {
    val sumMatchModule = module as? SumMatchGameModule
    val gameState by module.state.collectAsState()

    val handleBack: () -> Unit = {
        when (gameState) {
            is GameState.Idle -> onBack()
            else -> sumMatchModule?.resetToIdle() ?: module.onUserAction(GameAction.Quit)
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    colors = listOf(
                        Color(0xFFFFF8E1),
                        Color(0xFFFFECB3).copy(alpha = 0.5f)
                    )
                )
            )
    ) {
        when (val state = gameState) {
            is GameState.Idle -> {
                LevelSelectScreen(
                    module = sumMatchModule!!,
                    onBack = handleBack
                )
            }
            is GameState.Playing -> {
                PlayingScreen(
                    state = state,
                    module = sumMatchModule!!,
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
                    module = sumMatchModule!!,
                    onRestart = { sumMatchModule.restartLevel() },
                    onBack = handleBack
                )
            }
            is GameState.AllCompleted -> {
                AllCompletedScreen(
                    state = state,
                    onBack = handleBack
                )
            }
            else -> {}
        }
    }
}

// ==================== 关卡选择 ====================

@Composable
private fun LevelSelectScreen(
    module: SumMatchGameModule,
    onBack: () -> Unit
) {
    Column(modifier = Modifier.fillMaxSize()) {
        GameTopBar(
            title = "🔢 数字连连看",
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
                "🔢 选择数字，使其总和等于目标和！",
                fontSize = 18.sp,
                color = Color(0xFF5D4037),
                textAlign = TextAlign.Center,
                fontWeight = FontWeight.Bold
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                "点击数字圆圈，至少两个数字相加等于目标即可消除",
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
                subtitle = "2数相加，简单快速",
                color = DifficultyColors.EasyColor,
                onClick = { module.start(1) }
            )

            DifficultyCard(
                emoji = "🌿",
                title = "进阶 (6-10关)",
                subtitle = "2-3数相加，稍复杂",
                color = DifficultyColors.MediumColor,
                onClick = { module.start(6) }
            )

            DifficultyCard(
                emoji = "🌳",
                title = "挑战 (11-20关)",
                subtitle = "3数组合，考验心算",
                color = DifficultyColors.HardColor,
                onClick = { module.start(11) }
            )

            DifficultyCard(
                emoji = "🏆",
                title = "极限 (21-30关)",
                subtitle = "多数字组合，极限挑战",
                color = DifficultyColors.ExpertColor,
                onClick = { module.start(21) }
            )
        }
    }
}

// ==================== 游戏进行中 ====================

@Composable
private fun PlayingScreen(
    state: GameState.Playing,
    module: SumMatchGameModule,
    onBack: () -> Unit
) {
    // 解析游戏数据
    val nodesData = state.data["nodes"] as? List<*> ?: emptyList<Any>()
    val target = state.data["target"] as? Int ?: 0
    val selectedSum = state.data["selectedSum"] as? Int ?: 0
    val selectedCount = state.data["selectedCount"] as? Int ?: 0
    val remainingTime = state.data["remainingTime"] as? Long ?: 0L
    val currentMatches = state.data["currentMatches"] as? Int ?: 0
    val requiredMatches = state.data["requiredMatches"] as? Int ?: 1
    val showWrong = state.data["showWrong"] as? Boolean ?: false
    val showSuccess = state.data["showSuccess"] as? Boolean ?: false
    val currentPhaseStr = state.data["currentPhase"] as? String ?: "SELECTING"

    val currentPhase = try {
        GamePhase.valueOf(currentPhaseStr)
    } catch (e: Exception) {
        GamePhase.SELECTING
    }

    // 转换节点数据
    val nodes = remember(nodesData) {
        nodesData.mapNotNull { item ->
            try {
                val map = item as? Map<*, *> ?: return@mapNotNull null
                NumberNode(
                    id = (map["id"] as? Number)?.toInt() ?: 0,
                    value = (map["value"] as? Number)?.toInt() ?: 0,
                    x = (map["x"] as? Number)?.toFloat() ?: 0f,
                    y = (map["y"] as? Number)?.toFloat() ?: 0f,
                    isSelected = (map["isSelected"] as? Boolean) ?: false,
                    isMatched = (map["isMatched"] as? Boolean) ?: false,
                    isWrong = (map["isWrong"] as? Boolean) ?: false
                )
            } catch (e: Exception) {
                null
            }
        }
    }

    Column(modifier = Modifier.fillMaxSize()) {
        GameTopBar(
            title = "数字连连看",
            level = module.getLevelIndex(),
            difficultyName = module.getDifficultyName(),
            score = state.score,
            stars = 0,
            onBack = onBack
        )

        // 顶部信息栏
        TopInfoBar(
            target = target,
            selectedSum = selectedSum,
            remainingTime = remainingTime,
            currentMatches = currentMatches,
            requiredMatches = requiredMatches,
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
        )

        // 游戏区域 - 散点布局
        Box(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
                .padding(horizontal = 4.dp, vertical = 4.dp),
            contentAlignment = Alignment.Center
        ) {
            // 根据节点数量计算圆圈大小（更大）
            val circleSize = when {
                nodes.size <= 6 -> 85.dp
                nodes.size <= 9 -> 75.dp
                nodes.size <= 12 -> 65.dp
                else -> 58.dp
            }

            // 绘制所有圆圈
            nodes.forEach { node ->
                FloatingNumberCircle(
                    node = node,
                    circleSize = circleSize,
                    isInputEnabled = currentPhase == GamePhase.SELECTING,
                    onClick = { module.onUserAction(GameAction.TapIndex(node.id)) }
                )
            }
        }

        // 清空按钮
        if (selectedCount > 0 && currentPhase == GamePhase.SELECTING) {
            Button(
                onClick = { module.clearSelection() },
                modifier = Modifier
                    .align(Alignment.CenterHorizontally)
                    .padding(bottom = 12.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = Color(0xFFFFAB91)
                ),
                shape = RoundedCornerShape(20.dp),
                contentPadding = PaddingValues(horizontal = 24.dp, vertical = 8.dp)
            ) {
                Text("清空", color = Color(0xFF5D4037), fontWeight = FontWeight.Bold)
            }
        }
    }
}

// ==================== 浮动数字圆圈 ====================

@Composable
private fun FloatingNumberCircle(
    node: NumberNode,
    circleSize: androidx.compose.ui.unit.Dp,
    isInputEnabled: Boolean,
    onClick: () -> Unit
) {
    // 浮动动画 - 每个圆有不同的相位
    val floatAnim = rememberInfiniteTransition(label = "float")
    val floatOffset by floatAnim.animateFloat(
        initialValue = -4f,
        targetValue = 4f,
        animationSpec = infiniteRepeatable(
            animation = tween(
                durationMillis = 1500 + (node.id % 5) * 200,
                easing = FastOutSlowInEasing
            ),
            repeatMode = RepeatMode.Reverse
        ),
        label = "floatOffset"
    )

    // 选中/错误时的特殊动画
    val scale by animateFloatAsState(
        targetValue = when {
            node.isMatched -> 0f  // 消除
            node.isWrong -> 1.15f  // 抖动放大
            node.isSelected -> 1.1f  // 选中
            else -> 1f
        },
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness = Spring.StiffnessLow
        ),
        label = "scale"
    )

    // 透明度动画（用于消除效果）
    val alpha by animateFloatAsState(
        targetValue = when {
            node.isMatched -> 0f
            else -> 1f
        },
        animationSpec = tween(300),
        label = "alpha"
    )

    // 背景色
    val backgroundColor by animateColorAsState(
        targetValue = when {
            node.isMatched -> Color(0xFF81C784).copy(alpha = 0f)
            node.isWrong -> Color(0xFFE57373)
            node.isSelected -> Color(0xFFFFB74D)
            else -> Color(0xFF81D4FA)
        },
        animationSpec = tween(200),
        label = "bgColor"
    )

    // 渐变色
    val gradientColors = when {
        node.isWrong -> listOf(Color(0xFFE57373), Color(0xFFEF5350))
        node.isSelected -> listOf(Color(0xFFFFB74D), Color(0xFFFFA726))
        else -> listOf(Color(0xFF81D4FA), Color(0xFF4FC3F7))
    }

    // 跳过已消除
    if (node.isMatched && alpha == 0f) {
        return
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .wrapContentSize(Alignment.Center),
        contentAlignment = Alignment.Center
    ) {
        Box(
            modifier = Modifier
                .offset(
                    x = ((node.x - 0.5f) * 340).dp,
                    y = ((node.y - 0.5f) * 420).dp + floatOffset.dp
                )
                .size(circleSize)
                .alpha(alpha)
                .scale(scale)
                .shadow(
                    elevation = if (node.isSelected) 12.dp else 6.dp,
                    shape = CircleShape
                )
                .clip(CircleShape)
                .background(
                    brush = Brush.verticalGradient(gradientColors)
                )
                .then(
                    if (isInputEnabled && !node.isMatched) {
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
            Text(
                text = node.value.toString(),
                fontSize = (circleSize.value / 2.5f).sp,
                fontWeight = FontWeight.Bold,
                color = Color.White
            )
        }
    }
}

// ==================== 信息栏 ====================

@Composable
private fun TopInfoBar(
    target: Int,
    selectedSum: Int,
    remainingTime: Long,
    currentMatches: Int,
    requiredMatches: Int,
    modifier: Modifier = Modifier
) {
    val timeColor = when {
        remainingTime <= 5000 -> Color(0xFFE57373)
        remainingTime <= 10000 -> Color(0xFFFFB74D)
        else -> Color(0xFF5D4037)
    }

    Column(modifier = modifier.fillMaxWidth()) {
        // 主要信息栏（在上面）
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(
                containerColor = Color.White.copy(alpha = 0.95f)
            ),
            elevation = CardDefaults.cardElevation(defaultElevation = 6.dp),
            shape = RoundedCornerShape(24.dp)
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                horizontalArrangement = Arrangement.SpaceEvenly,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // 目标和 - 更醒目
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier
                        .background(
                            color = Color(0xFFFF7043).copy(alpha = 0.15f),
                            shape = RoundedCornerShape(16.dp)
                        )
                        .padding(horizontal = 20.dp, vertical = 8.dp)
                ) {
                    Text(
                        text = "🎯 目标",
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFFFF7043)
                    )
                    Text(
                        text = target.toString(),
                        fontSize = 36.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFFFF7043)
                    )
                }

                // 当前和
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = "当前",
                        fontSize = 12.sp,
                        color = Color(0xFF8D6E63)
                    )
                    Text(
                        text = selectedSum.toString(),
                        fontSize = 28.sp,
                        fontWeight = FontWeight.Bold,
                        color = when {
                            selectedSum == target -> Color(0xFF4CAF50)
                            selectedSum > target -> Color(0xFFE57373)
                            else -> Color(0xFF5D4037)
                        }
                    )
                }

                // 剩余时间
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = "⏱️ 时间",
                        fontSize = 12.sp,
                        color = Color(0xFF8D6E63)
                    )
                    Text(
                        text = "${remainingTime / 1000}s",
                        fontSize = 28.sp,
                        fontWeight = FontWeight.Bold,
                        color = timeColor
                    )
                }

                // 进度
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = "进度",
                        fontSize = 12.sp,
                        color = Color(0xFF8D6E63)
                    )
                    Text(
                        text = "$currentMatches/$requiredMatches",
                        fontSize = 28.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF5D4037)
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

        // 顶部提示（移动到下面）
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(
                containerColor = Color(0xFF42A5F5).copy(alpha = 0.15f)
            ),
            shape = RoundedCornerShape(16.dp)
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(12.dp),
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "💡 请选择至少2个数字，使它们的和等于目标值",
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Medium,
                    color = Color(0xFF1976D2)
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
    module: SumMatchGameModule,
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
            text = if (state.isSuccess) "🎉" else "⏰",
            fontSize = 64.sp
        )
        Spacer(modifier = Modifier.height(16.dp))
        Text(
            text = if (state.isSuccess) "过关成功！" else "时间耗尽",
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
        Spacer(modifier = Modifier.height(32.dp))

        if (state.isSuccess) {
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
