package com.animalgame.games.mirrordraw

import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onSizeChanged
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

/**
 * 镜像绘图游戏 UI
 */
@Composable
fun MirrorDrawGameScreen(
    module: GameModule,
    onBack: () -> Unit
) {
    val mirrorDrawModule = module as? MirrorDrawGameModule
    val gameState by module.state.collectAsState()

    val handleBack: () -> Unit = {
        when (gameState) {
            is GameState.Idle -> onBack()
            else -> mirrorDrawModule?.resetToIdle() ?: module.onUserAction(GameAction.Quit)
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFFF5F0E6))
    ) {
        when (val state = gameState) {
            is GameState.Idle -> {
                LevelSelectScreen(
                    module = mirrorDrawModule!!,
                    onBack = handleBack
                )
            }
            is GameState.Playing -> {
                PlayingScreen(
                    state = state,
                    module = mirrorDrawModule!!,
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
                    module = mirrorDrawModule!!,
                    onNextLevel = { mirrorDrawModule.start(state.level + 1) },
                    onReplay = { mirrorDrawModule.restartLevel() },
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
    module: MirrorDrawGameModule,
    onBack: () -> Unit
) {
    Column(modifier = Modifier.fillMaxSize()) {
        GameTopBar(
            title = "🪞 镜像绘图",
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
                "🎯 在右侧画出左侧路径的水平镜像！",
                fontSize = 18.sp,
                color = Color(0xFF5D4037),
                textAlign = TextAlign.Center,
                fontWeight = FontWeight.Bold
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                "用手指在右侧拖动画出镜像路径",
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
                subtitle = "3-4个点，简单折线",
                color = DifficultyColors.EasyColor,
                onClick = { module.start(1) }
            )

            DifficultyCard(
                emoji = "🌿",
                title = "进阶 (6-10关)",
                subtitle = "5-6个点，有转折",
                color = DifficultyColors.MediumColor,
                onClick = { module.start(6) }
            )

            DifficultyCard(
                emoji = "🌳",
                title = "挑战 (11-15关)",
                subtitle = "7-8个点，复杂路径",
                color = DifficultyColors.HardColor,
                onClick = { module.start(11) }
            )
        }
    }
}

// ==================== 游戏进行中 ====================

@Composable
private fun PlayingScreen(
    state: GameState.Playing,
    module: MirrorDrawGameModule,
    onBack: () -> Unit
) {
    // 解析游戏数据
    val targetPathData = state.data["targetPath"] as? List<*> ?: emptyList<Any>()
    val mirroredPathData = state.data["mirroredPath"] as? List<*> ?: emptyList<Any>()
    val playerPathData = state.data["playerPath"] as? List<*> ?: emptyList<Any>()
    val comparisonResultStr = state.data["comparisonResult"] as? String ?: "Incomplete"

    val comparisonResult = try {
        ComparisonResult.valueOf(comparisonResultStr)
    } catch (e: Exception) {
        ComparisonResult.Incomplete
    }

    // Canvas 尺寸
    var canvasSize by remember { mutableStateOf(IntSize.Zero) }

    // 转换数据
    val targetPath = remember(targetPathData) {
        targetPathData.mapNotNull {
            if (it is Map<*, *>) {
                PointF(
                    (it["x"] as? Number)?.toFloat() ?: 0f,
                    (it["y"] as? Number)?.toFloat() ?: 0f
                )
            } else null
        }
    }

    val mirroredPath = remember(mirroredPathData) {
        mirroredPathData.mapNotNull {
            if (it is Map<*, *>) {
                PointF(
                    (it["x"] as? Number)?.toFloat() ?: 0f,
                    (it["y"] as? Number)?.toFloat() ?: 0f
                )
            } else null
        }
    }

    val playerPath = remember(playerPathData) {
        playerPathData.mapNotNull {
            if (it is Map<*, *>) {
                PointF(
                    (it["x"] as? Number)?.toFloat() ?: 0f,
                    (it["y"] as? Number)?.toFloat() ?: 0f
                )
            } else null
        }
    }

    // 当 Canvas 尺寸变化时，更新模块
    LaunchedEffect(canvasSize) {
        if (canvasSize.width > 0 && canvasSize.height > 0) {
            module.setCanvasSize(canvasSize.width.toFloat(), canvasSize.height.toFloat())
        }
    }

    // 根据比对结果设置颜色
    val pathColor = when (comparisonResult) {
        ComparisonResult.Excellent -> Color(0xFF4CAF50)
        ComparisonResult.Good -> Color(0xFF8BC34A)
        ComparisonResult.Partial -> Color(0xFFFFC107)
        ComparisonResult.NeedImprovement -> Color(0xFFFF5722)
        ComparisonResult.Incomplete -> Color(0xFF2196F3)
    }

    Column(modifier = Modifier.fillMaxSize()) {
        GameTopBar(
            title = "镜像绘图",
            level = module.getLevelIndex(),
            difficultyName = module.getDifficultyName(),
            score = state.score,
            stars = 0,
            onBack = onBack
        )

        // 状态提示
        StatusBar(
            comparisonResult = comparisonResult,
            pointCount = playerPath.size,
            modifier = Modifier.padding(horizontal = 24.dp, vertical = 8.dp)
        )

        // 游戏区域
        Box(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            Canvas(
                modifier = Modifier
                    .fillMaxSize()
                    .onSizeChanged { canvasSize = it }
                    .pointerInput(Unit) {
                        detectDragGestures(
                            onDragStart = { offset ->
                                module.startDrawing(offset.x, offset.y)
                            },
                            onDrag = { change, _ ->
                                module.continueDrawing(change.position.x, change.position.y)
                            },
                            onDragEnd = {
                                module.endDrawing()
                            },
                            onDragCancel = {
                                module.endDrawing()
                            }
                        )
                    }
            ) {
                val width = size.width
                val height = size.height
                val axisX = width / 2f

                // 1. 绘制背景区域分隔
                drawRect(
                    color = Color(0xFFE3F2FD).copy(alpha = 0.3f),
                    topLeft = Offset(0f, 0f),
                    size = androidx.compose.ui.geometry.Size(axisX, height)
                )
                drawRect(
                    color = Color(0xFFE8F5E9).copy(alpha = 0.3f),
                    topLeft = Offset(axisX, 0f),
                    size = androidx.compose.ui.geometry.Size(width - axisX, height)
                )

                // 2. 绘制中线 (镜像轴)
                drawLine(
                    color = Color(0xFFBDBDBD),
                    start = Offset(axisX, 0f),
                    end = Offset(axisX, height),
                    strokeWidth = 2f
                )

                // 3. 绘制左侧目标路径 (蓝色实线)
                if (targetPath.isNotEmpty()) {
                    val pathObj = Path().apply {
                        moveTo(targetPath[0].x * width, targetPath[0].y * height)
                        for (i in 1 until targetPath.size) {
                            lineTo(targetPath[i].x * width, targetPath[i].y * height)
                        }
                    }
                    drawPath(
                        path = pathObj,
                        color = Color(0xFF2196F3),
                        style = Stroke(
                            width = 8f,
                            cap = StrokeCap.Round,
                            join = StrokeJoin.Round
                        )
                    )

                    // 绘制目标点
                    for (point in targetPath) {
                        drawCircle(
                            color = Color(0xFF1565C0),
                            radius = 12f,
                            center = Offset(point.x * width, point.y * height)
                        )
                        drawCircle(
                            color = Color.White,
                            radius = 6f,
                            center = Offset(point.x * width, point.y * height)
                        )
                    }
                }

                // 4. 绘制玩家的路径 (根据比对结果变色)
                if (playerPath.size >= 2) {
                    val playerPathObj = Path().apply {
                        moveTo(playerPath[0].x * width, playerPath[0].y * height)
                        for (i in 1 until playerPath.size) {
                            lineTo(playerPath[i].x * width, playerPath[i].y * height)
                        }
                    }
                    drawPath(
                        path = playerPathObj,
                        color = pathColor,
                        style = Stroke(
                            width = 8f,
                            cap = StrokeCap.Round,
                            join = StrokeJoin.Round
                        )
                    )
                }

                // 5. 绘制玩家路径的端点
                if (playerPath.isNotEmpty()) {
                    val startPoint = playerPath.first()
                    val endPoint = playerPath.last()

                    // 起点 (绿色)
                    drawCircle(
                        color = Color(0xFF4CAF50),
                        radius = 14f,
                        center = Offset(startPoint.x * width, startPoint.y * height)
                    )
                    drawCircle(
                        color = Color.White,
                        radius = 7f,
                        center = Offset(startPoint.x * width, startPoint.y * height)
                    )

                    // 当前终点
                    if (playerPath.size > 1) {
                        drawCircle(
                            color = pathColor,
                            radius = 14f,
                            center = Offset(endPoint.x * width, endPoint.y * height)
                        )
                        drawCircle(
                            color = Color.White,
                            radius = 7f,
                            center = Offset(endPoint.x * width, endPoint.y * height)
                        )
                    }
                }

            }
        }

        // 清除按钮
        if (playerPath.isNotEmpty()) {
            Button(
                onClick = { module.clearDrawing() },
                modifier = Modifier
                    .align(Alignment.CenterHorizontally)
                    .padding(bottom = 8.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = Color(0xFFFFAB91)
                ),
                shape = RoundedCornerShape(16.dp)
            ) {
                Text("清除重画", color = Color(0xFF5D4037))
            }
        }

        // 操作提示
        Text(
            text = "用手指在右侧拖动画出镜像路径",
            fontSize = 14.sp,
            color = Color(0xFF8D6E63),
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 16.dp),
            textAlign = TextAlign.Center
        )
    }
}

// ==================== 状态栏 ====================

@Composable
private fun StatusBar(
    comparisonResult: ComparisonResult,
    pointCount: Int,
    modifier: Modifier = Modifier
) {
    val (text, color) = when (comparisonResult) {
        ComparisonResult.Excellent -> "✨ 完美镜像！" to Color(0xFF4CAF50)
        ComparisonResult.Good -> "👍 基本正确" to Color(0xFF8BC34A)
        ComparisonResult.Partial -> "🤔 还需要调整" to Color(0xFFFFC107)
        ComparisonResult.NeedImprovement -> "❌ 偏差较大" to Color(0xFFFF5722)
        ComparisonResult.Incomplete -> "⏳ 继续绘制..." to Color(0xFF9E9E9E)
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
    module: MirrorDrawGameModule,
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
        Text(
            text = if (state.isSuccess) "🎉" else "⏰",
            fontSize = 64.sp
        )
        Spacer(modifier = Modifier.height(16.dp))
        Text(
            text = if (state.isSuccess) "绘制完成！" else "时间耗尽",
            fontSize = 28.sp,
            fontWeight = FontWeight.Bold,
            color = if (state.isSuccess) Color(0xFF81C784) else Color(0xFFE57373)
        )
        Spacer(modifier = Modifier.height(24.dp))

        Row {
            repeat(3) { index ->
                val starScale by animateFloatAsState(
                    targetValue = if (index < state.stars) 1.2f else 1f,
                    animationSpec = tween(300, delayMillis = index * 100),
                    label = "star$index"
                )
                Text(
                    text = if (index < state.stars) "⭐" else "☆",
                    fontSize = 40.sp,
                    modifier = Modifier.graphicsLayer {
                        scaleX = starScale
                        scaleY = starScale
                    }
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

        if (state.isSuccess && module.getLevelIndex() < 10) {
            Button(
                onClick = onNextLevel,
                modifier = Modifier.fillMaxWidth().height(56.dp),
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF81C784)),
                shape = RoundedCornerShape(16.dp)
            ) {
                Text("下一关", fontSize = 18.sp, fontWeight = FontWeight.Bold)
            }
            Spacer(modifier = Modifier.height(12.dp))
        }

        Button(
            onClick = onReplay,
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