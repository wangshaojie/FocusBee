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
 * 数字连连看游戏 UI - 儿童友好版本
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
            else -> sumMatchModule?.resetToIdle()
        }
    }

    // 儿童友好的彩虹渐变背景
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    colors = listOf(
                        Color(0xFFFFB6C1),  // 浅粉色
                        Color(0xFFFFE4B5),  // 浅橙色
                        Color(0xFF98FB98),  // 浅绿色
                        Color(0xFF87CEEB),  // 天蓝色
                        Color(0xFFDDA0DD),  // 浅紫色
                    )
                )
            )
    ) {
        // 装饰性云朵
        DecoratedClouds()

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

// ==================== 装饰性云朵 ====================

@Composable
private fun DecoratedClouds() {
    val infiniteTransition = rememberInfiniteTransition(label = "clouds")
    val cloudFloat by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 10f,
        animationSpec = infiniteRepeatable(
            animation = tween(3000, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "cloudFloat"
    )

    Box(modifier = Modifier.fillMaxSize()) {
        // 左上角云朵
        Text(
            text = "☁️",
            fontSize = 60.sp,
            modifier = Modifier
                .offset(x = 20.dp, y = 60.dp + cloudFloat.dp)
        )

        // 右上角云朵
        Text(
            text = "☁️",
            fontSize = 50.sp,
            modifier = Modifier
                .align(Alignment.TopEnd)
                .offset(x = (-40).dp, y = 100.dp - cloudFloat.dp)
        )

        // 左下角云朵
        Text(
            text = "☁️",
            fontSize = 45.sp,
            modifier = Modifier
                .align(Alignment.BottomStart)
                .offset(x = 60.dp, y = (-80).dp - cloudFloat.dp)
        )

        // 右下角云朵
        Text(
            text = "☁️",
            fontSize = 55.sp,
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .offset(x = (-30).dp, y = (-60).dp + cloudFloat.dp)
        )
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
            // 大标题配表情
            Text(
                "🎮 选择数字，加出目标！",
                fontSize = 24.sp,
                color = Color(0xFF5D4037),
                textAlign = TextAlign.Center,
                fontWeight = FontWeight.Bold
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                "点击可爱的数字圆圈，让它们加起来等于🎯目标！",
                fontSize = 16.sp,
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
                fontSize = 18.sp,
                color = Color(0xFF5D4037),
                fontWeight = FontWeight.Bold
            )

            DifficultyCard(
                emoji = "🌱",
                title = "入门 (1-5关)",
                subtitle = "2数相加，简单趣味",
                color = DifficultyColors.EasyColor,
                onClick = { module.start(1) }
            )

            DifficultyCard(
                emoji = "🌿",
                title = "进阶 (6-10关)",
                subtitle = "2-3数相加，趣味升级",
                color = DifficultyColors.MediumColor,
                onClick = { module.start(6) }
            )

            DifficultyCard(
                emoji = "🌳",
                title = "挑战 (11-20关)",
                subtitle = "3数组合，动动脑筋",
                color = DifficultyColors.HardColor,
                onClick = { module.start(11) }
            )

            DifficultyCard(
                emoji = "🏆",
                title = "极限 (21-30关)",
                subtitle = "多数字组合，挑战极限",
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

        // 儿童友好的顶部信息栏
        KidFriendlyInfoBar(
            target = target,
            selectedSum = selectedSum,
            remainingTime = remainingTime,
            currentMatches = currentMatches,
            requiredMatches = requiredMatches,
            selectedCount = selectedCount,
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
            // 根据节点数量计算圆圈大小
            val circleSize = when {
                nodes.size <= 6 -> 90.dp
                nodes.size <= 9 -> 80.dp
                nodes.size <= 12 -> 70.dp
                else -> 62.dp
            }

            // 绘制所有圆圈
            nodes.forEach { node ->
                KidFriendlyNumberCircle(
                    node = node,
                    circleSize = circleSize,
                    isInputEnabled = currentPhase == GamePhase.SELECTING,
                    onClick = { module.onUserAction(GameAction.TapIndex(node.id)) }
                )
            }
        }

        // 清空按钮 - 儿童友好样式
        if (selectedCount > 0 && currentPhase == GamePhase.SELECTING) {
            Button(
                onClick = { module.clearSelection() },
                modifier = Modifier
                    .align(Alignment.CenterHorizontally)
                    .padding(bottom = 16.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = Color(0xFFFF7043)
                ),
                shape = RoundedCornerShape(25.dp),
                contentPadding = PaddingValues(horizontal = 32.dp, vertical = 12.dp),
                elevation = ButtonDefaults.buttonElevation(
                    defaultElevation = 8.dp,
                    pressedElevation = 4.dp
                )
            ) {
                Text(
                    text = "🧹 清空选择",
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.White
                )
            }
        }
    }
}

// ==================== 儿童友好数字圆圈 ====================

@Composable
private fun KidFriendlyNumberCircle(
    node: NumberNode,
    circleSize: androidx.compose.ui.unit.Dp,
    isInputEnabled: Boolean,
    onClick: () -> Unit
) {
    // 浮动动画 - 更明显
    val floatAnim = rememberInfiniteTransition(label = "float")
    val floatOffset by floatAnim.animateFloat(
        initialValue = -6f,
        targetValue = 6f,
        animationSpec = infiniteRepeatable(
            animation = tween(
                durationMillis = 1200 + (node.id % 5) * 150,
                easing = FastOutSlowInEasing
            ),
            repeatMode = RepeatMode.Reverse
        ),
        label = "floatOffset"
    )

    // 选中/错误时的弹跳动画
    val scale by animateFloatAsState(
        targetValue = when {
            node.isMatched -> 0f
            node.isWrong -> 1.2f
            node.isSelected -> 1.15f
            else -> 1f
        },
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness = Spring.StiffnessLow
        ),
        label = "scale"
    )

    // 透明度动画
    val alpha by animateFloatAsState(
        targetValue = when {
            node.isMatched -> 0f
            else -> 1f
        },
        animationSpec = tween(400),
        label = "alpha"
    )

    // 错误时的抖动动画
    val shakeOffset by animateFloatAsState(
        targetValue = if (node.isWrong) 10f else 0f,
        animationSpec = keyframes {
            durationMillis = 300
            0f at 0
            -10f at 75
            10f at 150
            -10f at 225
            0f at 300
        },
        label = "shake"
    )

    // 根据数字值选择不同的彩虹颜色
    val gradientColors = when {
        node.isWrong -> listOf(Color(0xFFFF6B6B), Color(0xFFFF5252))
        node.isSelected -> listOf(Color(0xFFFFD54F), Color(0xFFFFC107))
        else -> getNumberGradientColors(node.value)
    }

    // 跳过已消除
    if (node.isMatched && alpha == 0f) {
        return
    }

    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        Box(
            modifier = Modifier
                .offset(
                    x = ((node.x - 0.5f) * 340).dp + shakeOffset.dp,
                    y = ((node.y - 0.5f) * 400).dp + floatOffset.dp
                )
                .size(circleSize)
                .alpha(alpha)
                .scale(scale)
                .shadow(
                    elevation = if (node.isSelected) 16.dp else 8.dp,
                    shape = CircleShape,
                    spotColor = if (node.isSelected) Color(0xFFFF9800) else Color(0xFF666666)
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
            // 数字
            Text(
                text = node.value.toString(),
                fontSize = (circleSize.value / 2.2f).sp,
                fontWeight = FontWeight.Bold,
                color = Color.White
            )
        }
    }
}

// 根据数字值获取彩虹颜色
private fun getNumberGradientColors(value: Int): List<Color> {
    return when (value % 7) {
        0 -> listOf(Color(0xFFFF6B6B), Color(0xFFFF5252))  // 红色
        1 -> listOf(Color(0xFFFFB74D), Color(0xFFFFA726))  // 橙色
        2 -> listOf(Color(0xFFFFF176), Color(0xFFFFEE58))  // 黄色
        3 -> listOf(Color(0xFF81C784), Color(0xFF66BB6A))  // 绿色
        4 -> listOf(Color(0xFF4FC3F7), Color(0xFF29B6F6))  // 蓝色
        5 -> listOf(Color(0xFF9575CD), Color(0xFF7E57C2))  // 紫色
        else -> listOf(Color(0xFFF06292), Color(0xFFEC407A))  // 粉色
    }
}

// ==================== 儿童友好信息栏 ====================

@Composable
private fun KidFriendlyInfoBar(
    target: Int,
    selectedSum: Int,
    remainingTime: Long,
    currentMatches: Int,
    requiredMatches: Int,
    selectedCount: Int,
    modifier: Modifier = Modifier
) {
    val timeColor = when {
        remainingTime <= 5000 -> Color(0xFFFF5252)
        remainingTime <= 10000 -> Color(0xFFFF9800)
        else -> Color(0xFF4CAF50)
    }

    // 当前和与目标的关系
    val sumColor = when {
        selectedCount == 0 -> Color(0xFF9E9E9E)
        selectedSum == target -> Color(0xFF4CAF50)
        selectedSum > target -> Color(0xFFFF5252)
        else -> Color(0xFFFF9800)
    }

    val sumEmoji = when {
        selectedCount == 0 -> "🤔"
        selectedSum == target -> "🎉"
        selectedSum > target -> "❌"
        selectedSum > target - 3 -> "🔔"
        else -> "✨"
    }

    Column(modifier = modifier.fillMaxWidth()) {
        // 主要信息栏 - 卡通卡片风格
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(
                containerColor = Color.White
            ),
            elevation = CardDefaults.cardElevation(defaultElevation = 8.dp),
            shape = RoundedCornerShape(24.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp)
            ) {
                // 第一行：目标和 + 当前和
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceEvenly,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // 🎯 目标卡片
                    Box(
                        modifier = Modifier
                            .background(
                                color = Color(0xFFFF7043).copy(alpha = 0.2f),
                                shape = RoundedCornerShape(20.dp)
                            )
                            .padding(horizontal = 20.dp, vertical = 12.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text(
                                text = "🎯 目标",
                                fontSize = 16.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFFFF7043)
                            )
                            Text(
                                text = target.toString(),
                                fontSize = 42.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFFFF7043)
                            )
                        }
                    }

                    // ➕ VS ➡️
                    Text(
                        text = "=？",
                        fontSize = 32.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF9E9E9E)
                    )

                    // 😃 当前和
                    Box(
                        modifier = Modifier
                            .background(
                                color = sumColor.copy(alpha = 0.2f),
                                shape = RoundedCornerShape(20.dp)
                            )
                            .padding(horizontal = 20.dp, vertical = 12.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text(
                                text = "$sumEmoji 当前",
                                fontSize = 16.sp,
                                fontWeight = FontWeight.Bold,
                                color = sumColor
                            )
                            Text(
                                text = selectedSum.toString(),
                                fontSize = 42.sp,
                                fontWeight = FontWeight.Bold,
                                color = sumColor
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                // 第二行：时间和进度
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceEvenly,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // ⏱️ 时间
                    Box(
                        modifier = Modifier
                            .background(
                                color = timeColor.copy(alpha = 0.15f),
                                shape = RoundedCornerShape(16.dp)
                            )
                            .padding(horizontal = 24.dp, vertical = 8.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                text = "⏱️",
                                fontSize = 24.sp
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "${remainingTime / 1000}s",
                                fontSize = 28.sp,
                                fontWeight = FontWeight.Bold,
                                color = timeColor
                            )
                        }
                    }

                    // 📊 进度
                    Box(
                        modifier = Modifier
                            .background(
                                color = Color(0xFF4CAF50).copy(alpha = 0.15f),
                                shape = RoundedCornerShape(16.dp)
                            )
                            .padding(horizontal = 24.dp, vertical = 8.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                text = "⭐",
                                fontSize = 24.sp
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "$currentMatches/$requiredMatches",
                                fontSize = 28.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFF4CAF50)
                            )
                        }
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

        // 💡 提示条
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(
                containerColor = Color(0xFF42A5F5).copy(alpha = 0.2f)
            ),
            shape = RoundedCornerShape(20.dp)
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(14.dp),
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "💡 至少选2个数字，加起来等于🎯目标",
                    fontSize = 17.sp,
                    fontWeight = FontWeight.Bold,
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
        Text("⏸️ 游戏暂停", fontSize = 32.sp, fontWeight = FontWeight.Bold, color = Color(0xFF5D4037))
        Spacer(modifier = Modifier.height(24.dp))
        Text("得分: $score", fontSize = 20.sp, color = Color(0xFF5D4037))
        Text("用时: ${elapsedTime / 1000}s", fontSize = 18.sp, color = Color(0xFF8D6E63))
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
    module: SumMatchGameModule,
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
            color = if (state.isSuccess) Color(0xFF4CAF50) else Color(0xFFFF7043)
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
        Text("得分: ${state.score}", fontSize = 22.sp, color = Color(0xFF5D4037), fontWeight = FontWeight.Bold)
        Text(
            "关卡: ${module.getDifficultyName()} · 第${module.getLevelIndex()}关",
            fontSize = 18.sp,
            color = Color(0xFF8D6E63)
        )
        Spacer(modifier = Modifier.height(32.dp))

        if (state.isSuccess) {
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
        Text("🎉 全部通关！", fontSize = 36.sp, fontWeight = FontWeight.Bold, color = Color(0xFFFFB300))
        Spacer(modifier = Modifier.height(24.dp))
        Text("总得分: ${state.totalScore}", fontSize = 24.sp, color = Color(0xFF5D4037), fontWeight = FontWeight.Bold)
        Text("总用时: ${state.totalTime / 1000}s", fontSize = 20.sp, color = Color(0xFF8D6E63))
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
