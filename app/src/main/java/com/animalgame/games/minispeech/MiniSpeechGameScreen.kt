package com.animalgame.games.minispeech

import androidx.compose.animation.*
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.animalgame.core.game.GameAction
import com.animalgame.core.game.GameModule
import com.animalgame.core.game.GameState
import com.animalgame.ui.components.GameTopBar
import kotlinx.coroutines.delay
import kotlin.random.Random

/**
 * 30秒小演说 游戏 UI
 * 儿童风格，简洁直观
 */

// 柔和儿童色系背景组合（用于演说过程中的背景变化）
private val speechBackgroundPalette = listOf(
    listOf(Color(0xFFFFB6C1), Color(0xFFFFE4B5), Color(0xFFFFCDD2)),
    listOf(Color(0xFFC8E6C9), Color(0xFFA5D6A7), Color(0xFF98FB98)),
    listOf(Color(0xFFBBDEFB), Color(0xFF90CAF9), Color(0xFF87CEEB)),
    listOf(Color(0xFFE1BEE7), Color(0xFFCE93D8), Color(0xFFFFC4E0)),
    listOf(Color(0xFFFFF9C4), Color(0xFFFFFAC1), Color(0xFFFFE082)),
    listOf(Color(0xFFB2EBF2), Color(0xFF80DEEA), Color(0xFFA7FFEB)),
)

@Composable
fun MiniSpeechGameScreen(
    module: GameModule,
    onBack: () -> Unit
) {
    val speechModule = module as? MiniSpeechGameModule
    val gameState by module.state.collectAsState()
    val context = LocalContext.current

    // 加载题库
    LaunchedEffect(Unit) {
        speechModule?.loadTopics(context)
    }

    val handleBack: () -> Unit = {
        when (gameState) {
            is GameState.Idle -> onBack()
            else -> speechModule?.resetToIdle()
        }
    }

    // 演说过程中的背景索引
    var bgIndex by remember { mutableIntStateOf(0) }

    // 渐变背景
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    colors = speechBackgroundPalette[bgIndex % speechBackgroundPalette.size]
                )
            )
    ) {
        when (gameState) {
            is GameState.Idle -> {
                CategorySelectScreen(
                    module = speechModule!!,
                    onBack = handleBack
                )
            }
            is GameState.Playing -> {
                val phase = speechModule?.getCurrentPhase() ?: SpeechPhase.IDLE
                when (phase) {
                    SpeechPhase.IDLE -> IdleScreen(
                        module = speechModule!!,
                        onBack = handleBack
                    )
                    SpeechPhase.COUNTDOWN -> CountdownScreen(
                        module = speechModule!!,
                        onBack = handleBack
                    )
                    SpeechPhase.PLAYING -> PlayingScreen(
                        module = speechModule!!,
                        onBack = handleBack,
                        onBgChange = { bgIndex++ }
                    )
                    SpeechPhase.FINISH -> FinishScreen(
                        module = speechModule!!,
                        onBack = handleBack
                    )
                }
            }
            else -> {}
        }
    }
}

// ==================== 分类选择页面 ====================

@Composable
private fun CategorySelectScreen(
    module: MiniSpeechGameModule,
    onBack: () -> Unit
) {
    Column(modifier = Modifier.fillMaxSize()) {
        GameTopBar(
            title = "🎤 30秒小演说",
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
                "🎤 30秒小演说",
                fontSize = 32.sp,
                color = Color(0xFF5D4037),
                textAlign = TextAlign.Center,
                fontWeight = FontWeight.Bold
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                "选择一个类别，准备开始演说！",
                fontSize = 18.sp,
                color = Color(0xFF8D6E63),
                textAlign = TextAlign.Center
            )
        }

        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 24.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // 讲故事类
            CategoryCard(
                emoji = "📖",
                title = "讲故事类",
                subtitle = "25道题目",
                color = Color(0xFFBA68C8),
                onClick = { module.startWithCategory(SpeechCategory.STORY) }
            )

            // 观点表达类
            CategoryCard(
                emoji = "💭",
                title = "观点表达类",
                subtitle = "25道题目",
                color = Color(0xFF64B5F6),
                onClick = { module.startWithCategory(SpeechCategory.OPINION) }
            )

            // 想象力类
            CategoryCard(
                emoji = "🌟",
                title = "想象力类",
                subtitle = "25道题目",
                color = Color(0xFFFFB74D),
                onClick = { module.startWithCategory(SpeechCategory.IMAGINE) }
            )

            // 生活表达类
            CategoryCard(
                emoji = "🌻",
                title = "生活表达类",
                subtitle = "25道题目",
                color = Color(0xFF81C784),
                onClick = { module.startWithCategory(SpeechCategory.LIFE) }
            )

            // 随机
            CategoryCard(
                emoji = "🎲",
                title = "随机挑战",
                subtitle = "全部100道题目",
                color = Color(0xFFFF8A65),
                onClick = { module.startWithCategory(SpeechCategory.RANDOM) }
            )
        }
    }
}

@Composable
private fun CategoryCard(
    emoji: String,
    title: String,
    subtitle: String,
    color: Color,
    onClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .shadow(8.dp, RoundedCornerShape(20.dp))
            .clip(RoundedCornerShape(20.dp))
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
                onClick = onClick
            ),
        colors = CardDefaults.cardColors(containerColor = color),
        shape = RoundedCornerShape(20.dp)
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
                Text(subtitle, fontSize = 14.sp, color = Color.White.copy(alpha = 0.85f))
            }
        }
    }
}

// ==================== 初始状态（显示主题） ====================

@Composable
private fun IdleScreen(
    module: MiniSpeechGameModule,
    onBack: () -> Unit
) {
    val topic = module.getCurrentTopic()

    Column(modifier = Modifier.fillMaxSize()) {
        GameTopBar(
            title = "🎤 ${module.getCurrentPhase().name}",
            level = 0,
            score = 0,
            stars = 0,
            onBack = onBack
        )

        Column(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
                .padding(20.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            // 主题卡片
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = Color.White),
                shape = RoundedCornerShape(28.dp),
                elevation = CardDefaults.cardElevation(defaultElevation = 12.dp)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(28.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    // 类别标签
                    Surface(
                        color = Color(0xFFE3F2FD),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Text(
                            text = "📝 ${topic?.category ?: ""}",
                            fontSize = 16.sp,
                            color = Color(0xFF1976D2),
                            modifier = Modifier.padding(horizontal = 16.dp, vertical = 6.dp)
                        )
                    }

                    Spacer(modifier = Modifier.height(20.dp))

                    // 主题文字
                    Text(
                        text = topic?.question ?: "",
                        fontSize = 28.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF37474F),
                        textAlign = TextAlign.Center,
                        lineHeight = 40.sp
                    )

                    if (!topic?.hint.isNullOrEmpty()) {
                        Spacer(modifier = Modifier.height(16.dp))
                        Text(
                            text = "💡 $topic.hint",
                            fontSize = 14.sp,
                            color = Color(0xFF9E9E9E),
                            textAlign = TextAlign.Center
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            // 提示文字
            Text(
                text = "准备好后，点击下方按钮开始",
                fontSize = 18.sp,
                color = Color(0xFF5D4037),
                textAlign = TextAlign.Center
            )
        }

        // 开始按钮
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .padding(24.dp),
            contentAlignment = Alignment.Center
        ) {
            StartButton(onClick = { module.startCountdown() })
        }
    }
}

// ==================== 3秒倒计时 ====================

@Composable
private fun CountdownScreen(
    module: MiniSpeechGameModule,
    onBack: () -> Unit
) {
    val countdownValue = module.getCountdownValue()

    // 倒计时动画
    val scale by animateFloatAsState(
        targetValue = 1.3f,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness = Spring.StiffnessLow
        ),
        label = "countdownScale"
    )

    Column(
        modifier = Modifier.fillMaxSize(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text(
            text = "准备好！",
            fontSize = 32.sp,
            fontWeight = FontWeight.Bold,
            color = Color.White
        )

        Spacer(modifier = Modifier.height(40.dp))

        // 倒计时数字
        Box(
            modifier = Modifier
                .size(180.dp)
                .scale(scale)
                .shadow(16.dp, CircleShape)
                .clip(CircleShape)
                .background(Color.White),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = countdownValue.toString(),
                fontSize = 100.sp,
                fontWeight = FontWeight.Bold,
                color = Color(0xFFFF6B6B)
            )
        }

        Spacer(modifier = Modifier.height(40.dp))

        Text(
            text = "即将开始30秒演说...",
            fontSize = 20.sp,
            color = Color.White
        )
    }
}

// ==================== 30秒演说中 ====================

@Composable
private fun PlayingScreen(
    module: MiniSpeechGameModule,
    onBack: () -> Unit,
    onBgChange: () -> Unit
) {
    val topic = module.getCurrentTopic()
    val timeRemaining = module.getSpeechTimeRemainingSeconds()
    val progress = module.getProgress()
    val isLastFive = module.isLastFiveSeconds()

    // 最后5秒闪烁动画
    val blinkAlpha by rememberInfiniteTransition(label = "blink").animateFloat(
        initialValue = 1f,
        targetValue = if (isLastFive) 0.5f else 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(300),
            repeatMode = RepeatMode.Reverse
        ),
        label = "blinkAlpha"
    )

    // 背景变化（每5秒变化一次）
    LaunchedEffect(progress) {
        if (progress < 1f) {
            delay(5000)
            onBgChange()
        }
    }

    Column(modifier = Modifier.fillMaxSize()) {
        GameTopBar(
            title = "🎤 演说中",
            level = 0,
            score = 0,
            stars = 0,
            onBack = onBack
        )

        Column(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
                .padding(20.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            // 主题卡片（半透明显示）
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = Color.White.copy(alpha = 0.9f)),
                shape = RoundedCornerShape(24.dp),
                elevation = CardDefaults.cardElevation(defaultElevation = 8.dp)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = topic?.question ?: "",
                        fontSize = 26.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF37474F),
                        textAlign = TextAlign.Center,
                        lineHeight = 36.sp
                    )
                }
            }

            Spacer(modifier = Modifier.height(40.dp))

            // 倒计时圆环
            Box(
                modifier = Modifier.size(160.dp),
                contentAlignment = Alignment.Center
            ) {
                // 背景圆环
                CircularProgressIndicator(
                    progress = 1f,
                    modifier = Modifier.fillMaxSize(),
                    color = Color.White.copy(alpha = 0.3f),
                    strokeWidth = 12.dp
                )

                // 进度圆环
                CircularProgressIndicator(
                    progress = progress,
                    modifier = Modifier.fillMaxSize(),
                    color = if (isLastFive) Color(0xFFFF5252) else Color(0xFF4CAF50),
                    strokeWidth = 12.dp
                )

                // 时间文字
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        text = timeRemaining.toString(),
                        fontSize = 56.sp,
                        fontWeight = FontWeight.Bold,
                        color = if (isLastFive) Color(0xFFFF5252) else Color.White,
                        modifier = Modifier.graphicsLayer { this.alpha = blinkAlpha }
                    )
                    Text(
                        text = "秒",
                        fontSize = 18.sp,
                        color = Color.White.copy(alpha = 0.8f)
                    )
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            // 提示
            if (isLastFive) {
                Text(
                    text = "⏰ 时间即将结束！",
                    fontSize = 24.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFFFF5252),
                    modifier = Modifier.graphicsLayer { this.alpha = blinkAlpha }
                )
            } else {
                Text(
                    text = "🎙️ 大声说出你的想法...",
                    fontSize = 20.sp,
                    color = Color.White,
                    textAlign = TextAlign.Center
                )
            }
        }
    }
}

// ==================== 结束页面 ====================

@Composable
private fun FinishScreen(
    module: MiniSpeechGameModule,
    onBack: () -> Unit
) {
    val topic = module.getCurrentTopic()

    Column(
        modifier = Modifier.fillMaxSize(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        // 庆祝动画
        val scale by animateFloatAsState(
            targetValue = 1f,
            animationSpec = spring(
                dampingRatio = Spring.DampingRatioMediumBouncy,
                stiffness = Spring.StiffnessLow
            ),
            label = "celebrateScale"
        )

        Text(
            text = "🎉",
            fontSize = 80.sp,
            modifier = Modifier.scale(scale)
        )

        Spacer(modifier = Modifier.height(16.dp))

        Text(
            text = "时间到！",
            fontSize = 40.sp,
            fontWeight = FontWeight.Bold,
            color = Color.White
        )

        Spacer(modifier = Modifier.height(8.dp))

        Text(
            text = "你完成了这次演说！",
            fontSize = 20.sp,
            color = Color.White.copy(alpha = 0.9f)
        )

        Spacer(modifier = Modifier.height(32.dp))

        // 主题回顾
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 32.dp),
            colors = CardDefaults.cardColors(containerColor = Color.White.copy(alpha = 0.9f)),
            shape = RoundedCornerShape(20.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(20.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = "刚才的主题：",
                    fontSize = 14.sp,
                    color = Color(0xFF9E9E9E)
                )
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = topic?.question ?: "",
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFF37474F),
                    textAlign = TextAlign.Center
                )
            }
        }

        Spacer(modifier = Modifier.height(40.dp))

        // 按钮组
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 32.dp),
            horizontalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // 再来一个
            Button(
                onClick = { module.nextTopic() },
                modifier = Modifier
                    .weight(1f)
                    .height(60.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = Color(0xFF4CAF50)
                ),
                shape = RoundedCornerShape(30.dp)
            ) {
                Text(
                    text = "📝 再来一个",
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold
                )
            }

            // 重新开始
            Button(
                onClick = { module.restartSpeech() },
                modifier = Modifier
                    .weight(1f)
                    .height(60.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = Color(0xFFFF9800)
                ),
                shape = RoundedCornerShape(30.dp)
            ) {
                Text(
                    text = "🔄 重说",
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold
                )
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // 返回
        OutlinedButton(
            onClick = onBack,
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 32.dp)
                .height(60.dp),
            shape = RoundedCornerShape(30.dp)
        ) {
            Text(
                text = "🏠 返回选择",
                fontSize = 18.sp
            )
        }
    }
}

// ==================== 开始按钮 ====================

@Composable
private fun StartButton(onClick: () -> Unit) {
    val infiniteTransition = rememberInfiniteTransition(label = "startButton")
    val scale by infiniteTransition.animateFloat(
        initialValue = 1f,
        targetValue = 1.05f,
        animationSpec = infiniteRepeatable(
            animation = tween(800, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "buttonPulse"
    )

    Button(
        onClick = onClick,
        modifier = Modifier
            .fillMaxWidth()
            .height(80.dp)
            .padding(horizontal = 40.dp)
            .scale(scale),
        colors = ButtonDefaults.buttonColors(
            containerColor = Color(0xFFFF6B6B)
        ),
        shape = RoundedCornerShape(40.dp),
        elevation = ButtonDefaults.buttonElevation(
            defaultElevation = 12.dp,
            pressedElevation = 6.dp
        )
    ) {
        Text(
            text = "🎤 开始演说",
            fontSize = 28.sp,
            fontWeight = FontWeight.Bold,
            color = Color.White
        )
    }
}