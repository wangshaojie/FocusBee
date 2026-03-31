package com.animalgame.games.colorburst

import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.animalgame.core.game.GameAction
import com.animalgame.core.game.GameModule
import com.animalgame.core.game.GameState
import com.animalgame.ui.components.DifficultyCard
import com.animalgame.ui.components.DifficultyColors
import com.animalgame.ui.components.GameTopBar
import kotlin.math.sqrt

/**
 * 色彩突围游戏 UI - 儿童友好版本
 */
@Composable
fun ColorBurstGameScreen(
    module: GameModule,
    onBack: () -> Unit
) {
    val colorBurstModule = module as? ColorBurstGameModule
    val gameState by module.state.collectAsState()

    val handleBack: () -> Unit = {
        when (gameState) {
            is GameState.Idle -> onBack()
            else -> colorBurstModule?.resetToIdle()
        }
    }

    // 彩虹糖果主题背景
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    colors = listOf(
                        Color(0xFFFFB6C1),  // 粉色
                        Color(0xFFFFE4B5),  // 浅橙色
                        Color(0xFFFFFFE0),  // 浅黄色
                        Color(0xFF98FB98),  // 浅绿色
                        Color(0xFF87CEEB),  // 天蓝色
                        Color(0xFFDDA0DD),  // 浅紫色
                    )
                )
            )
    ) {
        // 装饰性彩虹和糖果
        DecoratedColors()

        when (val state = gameState) {
            is GameState.Idle -> {
                LevelSelectScreen(
                    module = colorBurstModule!!,
                    onBack = handleBack
                )
            }
            is GameState.Ready -> {
                ReadyScreen(countdown = state.countdown)
            }
            is GameState.Playing -> {
                PlayingScreen(
                    state = state,
                    module = colorBurstModule!!,
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
                    module = colorBurstModule!!,
                    onReplay = { colorBurstModule.restartLevel() },
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

// ==================== 装饰性彩虹糖果 ====================

@Composable
private fun DecoratedColors() {
    val infiniteTransition = rememberInfiniteTransition(label = "deco")
    val rainbowFloat by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 15f,
        animationSpec = infiniteRepeatable(
            animation = tween(2000),
            repeatMode = RepeatMode.Reverse
        ),
        label = "rainbowFloat"
    )

    Box(modifier = Modifier.fillMaxSize()) {
        // 彩虹
        Text(
            text = "🌈",
            fontSize = 60.sp,
            modifier = Modifier
                .align(Alignment.TopStart)
                .offset(x = 20.dp, y = 60.dp + rainbowFloat.dp)
        )

        // 彩虹2
        Text(
            text = "🌈",
            fontSize = 45.sp,
            modifier = Modifier
                .align(Alignment.TopEnd)
                .offset(x = (-60).dp, y = 120.dp - rainbowFloat.dp)
        )

        // 彩色气球
        Text(
            text = "🎈",
            fontSize = 40.sp,
            modifier = Modifier
                .align(Alignment.TopStart)
                .offset(x = 120.dp, y = 80.dp - rainbowFloat.dp)
        )

        Text(
            text = "🎈",
            fontSize = 35.sp,
            modifier = Modifier
                .align(Alignment.TopEnd)
                .offset(x = (-30).dp, y = 200.dp + rainbowFloat.dp)
        )

        // 星星
        Text(
            text = "⭐",
            fontSize = 30.sp,
            modifier = Modifier
                .align(Alignment.BottomStart)
                .offset(x = 40.dp, y = (-100).dp + rainbowFloat.dp)
        )

        Text(
            text = "✨",
            fontSize = 35.sp,
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .offset(x = (-50).dp, y = (-80).dp - rainbowFloat.dp)
        )
    }
}

// ==================== 关卡选择 ====================

@Composable
private fun LevelSelectScreen(
    module: ColorBurstGameModule,
    onBack: () -> Unit
) {
    Column(modifier = Modifier.fillMaxSize()) {
        GameTopBar(
            title = "🎨 色彩突围",
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
                "👀 找出颜色略有不同的圆点！",
                fontSize = 24.sp,
                color = Color(0xFF5D4037),
                textAlign = TextAlign.Center,
                fontWeight = FontWeight.Bold
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                "颜色小侦探，快来找不同吧！🔍",
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
                subtitle = "2×2网格 · 颜色差异大",
                color = DifficultyColors.EasyColor,
                onClick = { module.start(1) }
            )

            DifficultyCard(
                emoji = "🌿",
                title = "进阶 (6-15关)",
                subtitle = "3×3网格 · 颜色差异中等",
                color = DifficultyColors.MediumColor,
                onClick = { module.start(6) }
            )

            DifficultyCard(
                emoji = "🌳",
                title = "困难 (16-25关)",
                subtitle = "4×4网格 · 颜色差异小",
                color = DifficultyColors.HardColor,
                onClick = { module.start(16) }
            )

            DifficultyCard(
                emoji = "🏆",
                title = "挑战 (26-30关)",
                subtitle = "5×5网格 · 颜色差异极小",
                color = DifficultyColors.ExpertColor,
                onClick = { module.start(26) }
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
                color = Color(0xFFFF7043),
                modifier = Modifier.scale(scale)
            )
            Text(
                text = "🎮 准备好了吗？",
                fontSize = 24.sp,
                color = Color(0xFF5D4037)
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = "👀 睁大眼睛找不同！",
                fontSize = 18.sp,
                color = Color(0xFF8D6E63)
            )
        }
    }
}

// ==================== 游戏进行中 ====================

@Composable
private fun PlayingScreen(
    state: GameState.Playing,
    module: ColorBurstGameModule,
    onBack: () -> Unit
) {
    val dotsData = state.data["dots"] as? List<*> ?: emptyList<Any>()
    val remainingTime = state.data["remainingTime"] as? Long ?: 0L
    val combo = state.data["combo"] as? Int ?: 0
    val showBurst = state.data["showBurst"] as? Boolean ?: false
    val burstIndex = state.data["burstIndex"] as? Int ?: -1
    val showShake = state.data["showShake"] as? Boolean ?: false
    val timeBonus = state.data["timeBonus"] as? Int ?: 0
    val levelConfig = state.data["levelConfig"] as? Map<*, *> ?: emptyMap<Any, Any>()
    val hasFloatEffect = levelConfig["hasFloatEffect"] as? Boolean ?: false

    // Canvas 实际尺寸（像素）
    var canvasSize by remember { mutableStateOf(IntSize.Zero) }

    // 漂浮动画
    val infiniteTransition = rememberInfiniteTransition(label = "float")
    val floatPhase by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(
            animation = tween(3000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "floatPhase"
    )

    // 震动动画
    val shakeOffset by animateFloatAsState(
        targetValue = if (showShake) 10f else 0f,
        animationSpec = keyframes {
            durationMillis = 200
            0f at 0
            -10f at 50
            10f at 100
            -10f at 150
            0f at 200
        },
        label = "shake"
    )

    // 爆发动画
    var burstScale by remember { mutableFloatStateOf(1f) }
    var burstAlpha by remember { mutableFloatStateOf(1f) }

    LaunchedEffect(showBurst, burstIndex) {
        if (showBurst && burstIndex >= 0) {
            burstScale = 2.5f
            burstAlpha = 0f
        }
    }

    val animatedBurstScale by animateFloatAsState(
        targetValue = burstScale,
        animationSpec = tween(400, easing = FastOutSlowInEasing),
        label = "burstScale"
    )
    val animatedBurstAlpha by animateFloatAsState(
        targetValue = burstAlpha,
        animationSpec = tween(400),
        label = "burstAlpha"
    )

    // 当 Canvas 尺寸变化时，更新模块中的尺寸
    LaunchedEffect(canvasSize) {
        if (canvasSize.width > 0 && canvasSize.height > 0) {
            module.setCanvasSize(canvasSize.width.toFloat(), canvasSize.height.toFloat())
        }
    }

    Column(modifier = Modifier.fillMaxSize()) {
        GameTopBar(
            title = "色彩突围",
            level = module.getLevelIndex(),
            difficultyName = module.getDifficultyName(),
            score = state.score,
            stars = 0,
            onBack = onBack
        )

        // 儿童友好时间显示
        KidFriendlyTimeDisplay(
            remainingTime = remainingTime,
            timeBonus = timeBonus,
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
        )

        // 连击显示
        if (combo > 1) {
            ComboDisplay(
                combo = combo,
                modifier = Modifier.align(Alignment.CenterHorizontally)
            )
        }

        // 游戏区域
        Box(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
                .padding(12.dp)
                .graphicsLayer {
                    translationX = shakeOffset
                }
        ) {
            val density = LocalDensity.current

            Canvas(
                modifier = Modifier
                    .fillMaxSize()
                    .onSizeChanged { canvasSize = it }
                    .pointerInput(Unit) {
                        detectTapGestures { offset ->
                            module.onUserAction(
                                GameAction.Tap(
                                    x = offset.x,
                                    y = offset.y
                                )
                            )
                        }
                    }
            ) {
                if (canvasSize == IntSize.Zero || dotsData.isEmpty()) return@Canvas

                val dpValue = with(density) { 1.dp.toPx() }

                for (dotData in dotsData) {
                    if (dotData !is Map<*, *>) continue

                    val index = (dotData["index"] as? Number)?.toInt() ?: continue
                    val x = (dotData["x"] as? Number)?.toFloat() ?: continue
                    val y = (dotData["y"] as? Number)?.toFloat() ?: continue
                    val radius = (dotData["radius"] as? Number)?.toFloat() ?: continue
                    val colorH = (dotData["colorH"] as? Number)?.toFloat() ?: 0f
                    val colorS = (dotData["colorS"] as? Number)?.toFloat() ?: 0f
                    val colorL = (dotData["colorL"] as? Number)?.toFloat() ?: 0f

                    val centerX = x * size.width
                    val centerY = y * size.height

                    val floatOffset = if (hasFloatEffect) {
                        val angle = floatPhase * Math.PI.toFloat() / 180f + index * 0.5f
                        val amplitude = 5f * dpValue
                        androidx.compose.ui.geometry.Offset(
                            x = kotlin.math.cos(angle) * amplitude,
                            y = kotlin.math.sin(angle) * amplitude * 0.5f
                        )
                    } else {
                        androidx.compose.ui.geometry.Offset.Zero
                    }

                    val actualX = centerX + floatOffset.x
                    val actualY = centerY + floatOffset.y
                    val actualRadius = radius * dpValue

                    val isBursting = showBurst && burstIndex == index
                    val scale = if (isBursting) animatedBurstScale else 1f
                    val alpha = if (isBursting) animatedBurstAlpha else 1f

                    val dotColor = HSLColor(colorH, colorS, colorL).toComposeColor()

                    // 更可爱的阴影
                    drawCircle(
                        color = Color.Black.copy(alpha = 0.15f * alpha),
                        radius = actualRadius * scale,
                        center = androidx.compose.ui.geometry.Offset(actualX + 2f * dpValue, actualY + 2f * dpValue)
                    )

                    // 圆点
                    drawCircle(
                        color = dotColor.copy(alpha = alpha),
                        radius = actualRadius * scale,
                        center = androidx.compose.ui.geometry.Offset(actualX, actualY)
                    )

                    // 高光效果
                    drawCircle(
                        color = Color.White.copy(alpha = 0.4f * alpha),
                        radius = actualRadius * 0.3f * scale,
                        center = androidx.compose.ui.geometry.Offset(
                            actualX - actualRadius * 0.2f,
                            actualY - actualRadius * 0.2f
                        )
                    )
                }
            }
        }

        // 底部提示
        KidFriendlyHint()
    }
}

// ==================== 儿童友好时间显示 ====================

@Composable
private fun KidFriendlyTimeDisplay(
    remainingTime: Long,
    timeBonus: Int,
    modifier: Modifier = Modifier
) {
    val seconds = (remainingTime / 1000).toInt()
    val isLow = seconds <= 10

    val timeColor = when {
        timeBonus > 0 -> Color(0xFF4CAF50)
        timeBonus < 0 -> Color(0xFFFF5252)
        isLow -> Color(0xFFFF5252)
        else -> Color(0xFF5D4037)
    }

    Card(
        modifier = modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = timeColor.copy(alpha = 0.15f)
        ),
        shape = RoundedCornerShape(20.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            horizontalArrangement = Arrangement.Center,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "⏱️",
                fontSize = 32.sp
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                text = "${seconds}秒",
                fontSize = 32.sp,
                fontWeight = FontWeight.Bold,
                color = timeColor
            )

            if (timeBonus != 0) {
                Spacer(modifier = Modifier.width(12.dp))
                val bonusText = if (timeBonus > 0) "+${timeBonus}s" else "${timeBonus}s"
                val bonusColor = if (timeBonus > 0) Color(0xFF4CAF50) else Color(0xFFFF5252)
                Text(
                    text = bonusText,
                    fontSize = 24.sp,
                    fontWeight = FontWeight.Bold,
                    color = bonusColor
                )
            }
        }
    }
}

// ==================== 儿童友好提示 ====================

@Composable
private fun KidFriendlyHint() {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 12.dp),
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
                text = "🔍",
                fontSize = 24.sp
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                text = "找出颜色不同的圆点！",
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold,
                color = Color(0xFF1976D2)
            )
        }
    }
}

// ==================== 连击显示 ====================

@Composable
private fun ComboDisplay(combo: Int, modifier: Modifier = Modifier) {
    val scale by animateFloatAsState(
        targetValue = 1.3f,
        animationSpec = repeatable(
            iterations = 2,
            animation = tween(100, easing = FastOutSlowInEasing)
        ),
        label = "comboScale"
    )

    Card(
        modifier = modifier
            .padding(vertical = 8.dp),
        colors = CardDefaults.cardColors(
            containerColor = Color(0xFFFF7043).copy(alpha = 0.3f)
        ),
        shape = RoundedCornerShape(20.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 8.dp)
    ) {
        Row(
            modifier = Modifier
                .padding(horizontal = 20.dp, vertical = 8.dp)
                .graphicsLayer {
                    scaleX = scale
                    scaleY = scale
                },
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "🔥",
                fontSize = 28.sp
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                text = "${combo}连击！",
                fontSize = 22.sp,
                fontWeight = FontWeight.Bold,
                color = Color(0xFFE64A19)
            )
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
        Text("得分: $score", fontSize = 22.sp, color = Color(0xFF5D4037), fontWeight = FontWeight.Bold)
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
    module: ColorBurstGameModule,
    onReplay: () -> Unit,
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
        // 成功/失败大表情
        Text(
            text = if (state.isSuccess) "🎉" else "⏰",
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

        if (state.isSuccess && module.getLevelIndex() < 10) {
            Button(
                onClick = { module.start(state.level + 1) },
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
            onClick = onReplay,
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
