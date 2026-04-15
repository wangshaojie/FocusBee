package com.animalgame.games.memory

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.outlined.StarOutline
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.animalgame.core.game.GameAction
import com.animalgame.core.game.GameModule
import com.animalgame.core.game.GameState
import kotlinx.coroutines.flow.collectLatest

/**
 * 记忆翻牌游戏 UI
 * 原则：
 * - 只订阅 state，不写业务逻辑
 * - 点击事件只调用 module.onUserAction()
 * - 所有逻辑在模块内部处理
 */
@Composable
fun MemoryGameUI(
    module: GameModule,
    onBack: () -> Unit
) {
    // 收集状态
    val gameState by module.state.collectAsState()

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFFF0F4FF))
    ) {
        when (val state = gameState) {
            is GameState.Idle -> {
                // 关卡选择
                LevelSelectScreen(
                    totalLevels = module.totalLevels,
                    onLevelSelect = { level ->
                        module.start(level)
                    },
                    onBack = onBack
                )
            }

            is GameState.Ready -> {
                // 倒计时
                ReadyScreen(countdown = state.countdown)
            }

            is GameState.Playing -> {
                // 游戏进行中
                PlayingScreen(
                    state = state,
                    onCardClick = { index ->
                        module.onUserAction(GameAction.TapIndex(index))
                    },
                    onBack = onBack,
                    onReset = {
                        module.onUserAction(GameAction.Restart)
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
                // 游戏完成
                CompletedScreen(
                    state = state,
                    onNextLevel = {
                        module.onUserAction(GameAction.NextLevel)
                    },
                    onReplay = {
                        module.onUserAction(GameAction.Restart)
                    },
                    onBack = onBack
                )
            }

            is GameState.AllCompleted -> {
                // 全部通关
                AllCompletedScreen(
                    state = state,
                    onBack = onBack
                )
            }
        }
    }
}

/**
 * 关卡选择屏幕 - 难度选择模式
 */
@Composable
private fun LevelSelectScreen(
    totalLevels: Int,
    onLevelSelect: (Int) -> Unit,
    onBack: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    colors = listOf(
                        Color(0xFFFFB6C1),  // 浅粉色
                        Color(0xFFFFE4B5),  // 浅橙色
                        Color(0xFF98FB98),  // 浅绿色
                        Color(0xFF87CEEB),  // 天蓝色
                    )
                )
            )
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // 顶部标题
        Column(
            modifier = Modifier.fillMaxWidth(),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Spacer(modifier = Modifier.height(16.dp))
            Text(
                text = "🃏 记忆翻牌",
                fontSize = 28.sp,
                fontWeight = FontWeight.Bold,
                color = Color(0xFF5D4037)
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = "找出相同的配对，训练记忆力！",
                fontSize = 14.sp,
                color = Color(0xFF8D6E63)
            )
        }

        Spacer(modifier = Modifier.height(24.dp))

        // 难度选择卡片
        Column(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Text(
                text = "选择难度",
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold,
                color = Color(0xFF5D4037)
            )

            // 简单 - 3x4 网格
            DifficultyCard(
                emoji = "🌱",
                title = "简单 (1-50关)",
                subtitle = "3×4 网格，6对卡片",
                color = Color(0xFF4CAF50),
                onClick = { onLevelSelect(1) }
            )

            // 中等 - 4x4 网格
            DifficultyCard(
                emoji = "🌿",
                title = "中等 (51-100关)",
                subtitle = "4×4 网格，8对卡片",
                color = Color(0xFF2196F3),
                onClick = { onLevelSelect(51) }
            )

            // 困难 - 4x5 网格
            DifficultyCard(
                emoji = "🌳",
                title = "困难 (101-150关)",
                subtitle = "4×5 网格，10对卡片",
                color = Color(0xFFFF9800),
                onClick = { onLevelSelect(101) }
            )

            // 挑战 - 5x6 网格
            DifficultyCard(
                emoji = "🏆",
                title = "挑战 (151-200关)",
                subtitle = "5×6 网格，15对卡片",
                color = Color(0xFFF44336),
                onClick = { onLevelSelect(151) }
            )
        }

        Spacer(modifier = Modifier.height(16.dp))

        // 返回按钮
        OutlinedButton(
            onClick = onBack,
            modifier = Modifier.fillMaxWidth().height(56.dp),
            colors = ButtonDefaults.outlinedButtonColors(contentColor = Color(0xFF5D4037)),
            shape = RoundedCornerShape(16.dp)
        ) {
            Text("返回主页", fontSize = 18.sp)
        }
    }
}

/**
 * 难度选择卡片
 */
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
        modifier = Modifier
            .fillMaxWidth()
            .height(80.dp),
        colors = CardDefaults.cardColors(containerColor = color.copy(alpha = 0.9f)),
        shape = RoundedCornerShape(16.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 20.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(text = emoji, fontSize = 36.sp)
            Spacer(modifier = Modifier.width(16.dp))
            Column {
                Text(
                    text = title,
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.White
                )
                Text(
                    text = subtitle,
                    fontSize = 14.sp,
                    color = Color.White.copy(alpha = 0.85f)
                )
            }
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
            color = Color(0xFF4CAF50)
        )
    }
}

/**
 * 游戏进行中屏幕
 */
@Composable
private fun PlayingScreen(
    state: GameState.Playing,
    onCardClick: (Int) -> Unit,
    onBack: () -> Unit,
    onReset: () -> Unit = {}
) {
    // 从 state.data 获取游戏数据
    val cards = (state.data["cards"] as? List<*>)?.filterIsInstance<MemoryGameCardData>() ?: emptyList()
    val matchedPairs = state.data["matchedPairs"] as? Int ?: 0
    val isChecking = state.data["isChecking"] as? Boolean ?: false
    val flipCount = state.data["flipCount"] as? Int ?: 0

    // 从 state 获取网格配置
    val gridRows = state.data["gridRows"] as? Int ?: 2
    val gridColumns = state.data["gridColumns"] as? Int ?: 2
    val totalPairs = cards.size / 2

    // 直接使用配置的列数
    val columns = gridColumns

    // 马卡龙色系
    val primaryColor = Color(0xFF81D4FA) // 浅蓝色
    val secondaryColor = Color(0xFFFFB74D) // 浅橙色
    val accentColor = Color(0xFFA5D6A7) // 浅绿色
    val backgroundColor = Color(0xFFF8F6FF) // 护眼淡紫背景
    val stageColor = Color(0xFFEDE7F6) // 舞台区域淡紫色

    // 进入动画 - 使用 remember 保存状态，只在关卡变化时触发
    var isVisible by remember(state.level) { mutableStateOf(true) }

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
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // 顶部信息栏
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = Color.White),
            elevation = CardDefaults.cardElevation(defaultElevation = 4.dp),
            shape = RoundedCornerShape(16.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(12.dp)
            ) {
                // 第一行：关卡和重置按钮
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "第 ${state.level} 关",
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF5C6BC0)
                    )

                    // 重置按钮
                    IconButton(
                        onClick = onReset,
                        modifier = Modifier
                            .size(36.dp)
                            .background(primaryColor.copy(alpha = 0.2f), CircleShape)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Refresh,
                            contentDescription = "重置",
                            tint = Color(0xFF5C6BC0),
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(6.dp))

                // 第二行：步数、星级、进度
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // 步数
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier
                            .background(secondaryColor.copy(alpha = 0.2f), RoundedCornerShape(8.dp))
                            .padding(horizontal = 8.dp, vertical = 4.dp)
                    ) {
                        Text(text = "🔄", fontSize = 12.sp)
                        Spacer(modifier = Modifier.width(2.dp))
                        Text(text = "$flipCount", fontSize = 12.sp, fontWeight = FontWeight.Medium, color = Color(0xFF5D4037))
                    }

                    // 星级
                    val stars = calculateStars(state.score, totalPairs)
                    Row {
                        repeat(3) { index ->
                            Icon(
                                imageVector = if (index < stars) Icons.Filled.Star else Icons.Outlined.StarOutline,
                                contentDescription = null,
                                tint = if (index < stars) Color(0xFFFFD54F) else Color(0xFFE0E0E0),
                                modifier = Modifier.size(18.dp)
                            )
                        }
                    }

                    // 进度
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier
                            .background(accentColor.copy(alpha = 0.2f), RoundedCornerShape(8.dp))
                            .padding(horizontal = 8.dp, vertical = 4.dp)
                    ) {
                        Text(text = "✨", fontSize = 12.sp)
                        Spacer(modifier = Modifier.width(2.dp))
                        Text(text = "$matchedPairs/$totalPairs", fontSize = 12.sp, fontWeight = FontWeight.Medium, color = Color(0xFF2E7D32))
                    }
                }
            }
        }

        // 空白区域，使用权重让游戏区域居中
        Spacer(modifier = Modifier.height(12.dp))

        // 游戏区域 - 舞台效果，居中显示
        Box(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth(),
            contentAlignment = Alignment.Center
        ) {
            // 游戏区域背景卡片 - 舞台效果
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
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(12.dp),
                    contentAlignment = Alignment.Center
                ) {
                    if (cards.isNotEmpty()) {
                        LazyVerticalGrid(
                            columns = GridCells.Fixed(columns),
                            horizontalArrangement = Arrangement.spacedBy(4.dp),
                            verticalArrangement = Arrangement.spacedBy(4.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            itemsIndexed(cards) { index, card ->
                                val enabled = !card.isMatched && !card.isFlipped && !isChecking
                                FlipCard(
                                    card = card,
                                    enabled = enabled,
                                    onClick = { onCardClick(index) },
                                    modifier = Modifier
                                        .aspectRatio(1f)
                                        .fillMaxWidth()
                                )
                            }
                        }
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        // 返回按钮
        OutlinedButton(
            onClick = onBack,
            modifier = Modifier
                .fillMaxWidth()
                .height(44.dp),
            colors = ButtonDefaults.outlinedButtonColors(contentColor = Color(0xFF5C6BC0)),
            border = ButtonDefaults.outlinedButtonBorder.copy(
                brush = androidx.compose.ui.graphics.SolidColor(Color(0xFF5C6BC0).copy(alpha = 0.5f))
            ),
            shape = RoundedCornerShape(16.dp)
        ) {
            Text("返回主页", fontSize = 16.sp)
        }
    }
}

/**
 * 计算星级
 */
private fun calculateStars(score: Int, totalPairs: Int): Int {
    return when {
        score >= totalPairs * 100 -> 3
        score >= totalPairs * 50 -> 2
        score >= totalPairs * 20 -> 1
        else -> 0
    }
}

/**
 * 翻转卡片组件 - 儿童卡通风格
 */
@Composable
private fun FlipCard(
    card: MemoryGameCardData,
    enabled: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    // 马卡龙色系
    val cardBackColor = Color(0xFF81D4FA) // 浅蓝色云朵背景
    val cardBackGradientStart = Color(0xFF81D4FA) // 浅蓝
    val cardBackGradientEnd = Color(0xFF4FC3F7) // 稍深的蓝
    val cardFrontColor = Color.White // 白色正面
    val matchedColor = Color(0xFFA5D6A7) // 浅绿成功

    val rotation by animateFloatAsState(
        targetValue = if (card.isFlipped || card.isMatched) 180f else 0f,
        animationSpec = tween(durationMillis = 500),
        label = "cardRotation"
    )

    // 配对成功时的缩放动画
    val scale by animateFloatAsState(
        targetValue = if (card.isMatched) 1.05f else 1f,
        animationSpec = tween(durationMillis = 300),
        label = "cardScale"
    )

    val isShowingFront = rotation > 90f

    Box(
        modifier = modifier
            .graphicsLayer {
                rotationY = rotation
                cameraDistance = 12f * density
                scaleX = scale
                scaleY = scale
            }
            .clip(RoundedCornerShape(12.dp)) // 大圆角
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
                enabled = enabled
            ) { onClick() },
        contentAlignment = Alignment.Center
    ) {
        if (isShowingFront) {
            // 卡片正面 - 显示图标
            Card(
                modifier = Modifier.fillMaxSize(),
                colors = CardDefaults.cardColors(
                    containerColor = if (card.isMatched) matchedColor else cardFrontColor
                ),
                elevation = CardDefaults.cardElevation(
                    defaultElevation = if (card.isMatched) 8.dp else 4.dp
                ),
                shape = RoundedCornerShape(12.dp)
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(
                            if (card.isMatched) matchedColor.copy(alpha = 0.3f)
                            else Color.Transparent
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Image(
                        painter = painterResource(id = card.resourceId),
                        contentDescription = card.iconId,
                        modifier = Modifier
                            .fillMaxSize(0.65f)
                            .padding(6.dp),
                        contentScale = ContentScale.Fit
                    )
                }
            }
        } else {
            // 卡片背面 - 蓝色云朵图案
            Card(
                modifier = Modifier.fillMaxSize(),
                colors = CardDefaults.cardColors(containerColor = cardBackColor),
                elevation = CardDefaults.cardElevation(defaultElevation = 6.dp),
                shape = RoundedCornerShape(12.dp)
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(
                            brush = androidx.compose.ui.graphics.Brush.verticalGradient(
                                colors = listOf(cardBackGradientStart, cardBackGradientEnd)
                            )
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    // 绘制云朵图案
                    CloudIcon(
                        modifier = Modifier.fillMaxSize(0.5f)
                    )
                }
            }
        }
    }
}

/**
 * 云朵图标组件
 */
@Composable
private fun CloudIcon(modifier: Modifier = Modifier) {
    Box(modifier = modifier, contentAlignment = Alignment.Center) {
        // 使用简单的布局模拟云朵
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            // 云朵主体
            Box(
                modifier = Modifier
                    .size(50.dp)
                    .background(
                        color = Color.White.copy(alpha = 0.9f),
                        shape = RoundedCornerShape(25.dp)
                    )
            )
            Row {
                Box(
                    modifier = Modifier
                        .size(30.dp)
                        .offset(x = (-15).dp, y = (-15).dp)
                        .background(
                            color = Color.White.copy(alpha = 0.9f),
                            shape = RoundedCornerShape(15.dp)
                        )
                )
                Box(
                    modifier = Modifier
                        .size(40.dp)
                        .offset(x = (-10).dp, y = (-25).dp)
                        .background(
                            color = Color.White.copy(alpha = 0.95f),
                            shape = RoundedCornerShape(12.dp)
                        )
                )
                Box(
                    modifier = Modifier
                        .size(30.dp)
                        .offset(x = 10.dp, y = (-25).dp)
                        .background(
                            color = Color.White.copy(alpha = 0.9f),
                            shape = RoundedCornerShape(15.dp)
                        )
                )
            }
        }
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

        // 显示额外数据
        val extraData = (state as? GameState.Completed)?.let { null } ?: state

        Button(
            onClick = onNextLevel,
            modifier = Modifier.fillMaxWidth().height(56.dp)
        ) {
            Text("下一关", fontSize = 18.sp)
        }

        Spacer(modifier = Modifier.height(12.dp))

        Button(
            onClick = onReplay,
            modifier = Modifier.fillMaxWidth().height(56.dp)
        ) {
            Text("重玩本关", fontSize = 18.sp)
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
