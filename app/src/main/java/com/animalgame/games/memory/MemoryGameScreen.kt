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
import androidx.compose.foundation.lazy.LazyColumn
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
import com.animalgame.ui.components.GameTopBar
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

    // 处理返回逻辑：根据当前状态决定返回行为
    val handleBack: () -> Unit = {
        when (gameState) {
            is GameState.Idle -> {
                // 在关卡选择页面，直接退出
                onBack()
            }
            else -> {
                // 在游戏进行中或完成页面，返回到关卡选择
                module.onUserAction(GameAction.Quit)
            }
        }
    }

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
                    onBack = handleBack
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
                    onBack = handleBack,
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
                    onBack = handleBack
                )
            }

            is GameState.AllCompleted -> {
                // 全部通关
                AllCompletedScreen(
                    state = state,
                    onBack = handleBack
                )
            }
        }
    }
}

/**
 * 关卡选择屏幕
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
            .background(Color(0xFFF8F6FF))
    ) {
        GameTopBar(title = "记忆翻牌", level = 0, score = 0, stars = 0, onBack = onBack)
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
        Text(
            text = "训练短期记忆与注意力",
            fontSize = 14.sp,
            color = Color(0xFF666666)
        )
        Spacer(modifier = Modifier.height(32.dp))

        // 关卡按钮 - 使用 LazyColumn 支持 100 关
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            verticalArrangement = Arrangement.spacedBy(8.dp),
            contentPadding = PaddingValues(vertical = 16.dp)
        ) {
            items(totalLevels) { index ->
                val level = index + 1
                val pairs = when {
                    level <= 25 -> 6   // 简单: 3x4 = 12张 (6对)
                    level <= 50 -> 8   // 中等: 4x4 = 16张 (8对)
                    level <= 75 -> 10  // 困难: 4x5 = 20张 (10对)
                    else -> 15         // 挑战: 5x6 = 30张 (15对)
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
                    Text("第 $level 关 ($difficulty · $pairs 对)", fontSize = 16.sp)
                }
            }
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
    ) {
        // 统一顶部导航栏
        GameTopBar(
            title = "记忆翻牌",
            level = state.level,
            score = state.score,
            stars = calculateStars(state.score, totalPairs),
            onBack = onBack
        )

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
                            horizontalArrangement = Arrangement.spacedBy(10.dp),
                            verticalArrangement = Arrangement.spacedBy(10.dp),
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

        // 重置按钮
        OutlinedButton(
            onClick = onReset,
            modifier = Modifier
                .fillMaxWidth()
                .height(44.dp),
            colors = ButtonDefaults.outlinedButtonColors(contentColor = Color(0xFF5C6BC0)),
            border = ButtonDefaults.outlinedButtonBorder.copy(
                brush = androidx.compose.ui.graphics.SolidColor(Color(0xFF5C6BC0).copy(alpha = 0.5f))
            ),
            shape = RoundedCornerShape(16.dp)
        ) {
            Text("重置本关", fontSize = 16.sp)
        }
    }
}

/**
 * 计算星级
 */
private fun calculateStars(score: Int, totalPairs: Int): Int {
    // 由于UI无法直接获取错误次数，我们通过分数来估算
    // 每对正确配对得10分，所以总分为 totalPairs * 10
    // 无错误时分数应该接近满分，少量错误时分数会略低
    val maxScore = totalPairs * 10
    return when {
        score >= maxScore -> 3  // 无错误
        score >= maxScore - 20 -> 2  // 少量错误（<=2）
        else -> 1  // 多错误
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
