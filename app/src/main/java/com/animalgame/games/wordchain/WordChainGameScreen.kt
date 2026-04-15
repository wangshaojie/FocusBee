package com.animalgame.games.wordchain

import androidx.compose.animation.*
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import kotlinx.coroutines.delay
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.animalgame.core.game.GameAction
import com.animalgame.core.game.GameModule
import com.animalgame.core.game.GameState
import com.animalgame.ui.components.GameTopBar
import kotlin.random.Random

/**
 * 谁说得快（词语接龙）游戏 UI
 * 儿童风格，简洁直观
 */

// 柔和儿童色系背景组合
private val backgroundPalette = listOf(
    // 粉色系
    listOf(Color(0xFFFFB6C1), Color(0xFFFFE4B5), Color(0xFFFFCDD2)),
    // 橙色系
    listOf(Color(0xFFFFE0B2), Color(0xFFFFCC80), Color(0xFFFFE4B5)),
    // 绿色系
    listOf(Color(0xFFC8E6C9), Color(0xFFA5D6A7), Color(0xFF98FB98)),
    // 蓝色系
    listOf(Color(0xFFBBDEFB), Color(0xFF90CAF9), Color(0xFF87CEEB)),
    // 紫色系
    listOf(Color(0xFFE1BEE7), Color(0xFFCE93D8), Color(0xFFFFC4E0)),
    // 黄色系
    listOf(Color(0xFFFFF9C4), Color(0xFFFFFAC1), Color(0xFFFFE082)),
    // 青色系
    listOf(Color(0xFFB2EBF2), Color(0xFF80DEEA), Color(0xFFA7FFEB)),
    // 橙红色系
    listOf(Color(0xFFFFAB91), Color(0xFFFF8A65), Color(0xFFFFCCBC)),
    // 蓝绿色系
    listOf(Color(0xFFB2DFDB), Color(0xFF80CBC4), Color(0xFFA5EFEB)),
    // 浅粉色系
    listOf(Color(0xFFF8BBD9), Color(0xFFF48FB1), Color(0xFFFFC4E0)),
)

@Composable
fun WordChainGameScreen(
    module: GameModule,
    onBack: () -> Unit
) {
    val wordChainModule = module as? WordChainGameModule
    val gameState by module.state.collectAsState()

    // 当前背景颜色索引
    var targetBgIndex by remember { mutableIntStateOf(0) }

    // 背景颜色动画
    val animatedBgIndex by animateIntAsState(
        targetValue = targetBgIndex,
        animationSpec = tween(500, easing = FastOutSlowInEasing),
        label = "bgTransition"
    )

    val handleBack: () -> Unit = {
        when (gameState) {
            is GameState.Idle -> onBack()
            else -> wordChainModule?.resetToIdle()
        }
    }

    // 获取当前背景颜色
    val currentBgColors = backgroundPalette.getOrElse(animatedBgIndex % backgroundPalette.size) {
        backgroundPalette[0]
    }

    // 渐变背景
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    colors = currentBgColors
                )
            )
    ) {
        // 装饰性云朵
        DecoratedClouds()

        when (gameState) {
            is GameState.Idle -> {
                DifficultySelectScreen(
                    module = wordChainModule!!,
                    onBack = onBack
                )
            }
            is GameState.Playing -> {
                PlayingScreen(
                    module = wordChainModule!!,
                    onBack = handleBack,
                    onChangeTheme = {
                        // 选择新的随机背景（避免重复）
                        var newIndex: Int
                        do {
                            newIndex = Random.nextInt(backgroundPalette.size)
                        } while (newIndex == targetBgIndex && backgroundPalette.size > 1)
                        targetBgIndex = newIndex
                    }
                )
            }
            else -> {}
        }
    }
}

// ==================== 难度选择 ====================

@Composable
private fun DifficultySelectScreen(
    module: WordChainGameModule,
    onBack: () -> Unit
) {
    Column(modifier = Modifier.fillMaxSize()) {
        GameTopBar(
            title = "💬 谁说得快",
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
                "💬 谁说得快",
                fontSize = 32.sp,
                color = Color(0xFF5D4037),
                textAlign = TextAlign.Center,
                fontWeight = FontWeight.Bold
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                "轮流说一个相关词，看谁说得又快又好！",
                fontSize = 18.sp,
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
            Text("选择难度", fontSize = 18.sp, color = Color(0xFF5D4037), fontWeight = FontWeight.Bold)

            // 入门
            DifficultyCard(
                emoji = "🌱",
                title = "入门 (1-25关)",
                subtitle = "5个简单类别",
                color = Color(0xFF4CAF50),
                onClick = { module.start(1) }
            )

            // 进阶
            DifficultyCard(
                emoji = "🌿",
                title = "进阶 (26-50关)",
                subtitle = "9个日常类别",
                color = Color(0xFF2196F3),
                onClick = { module.start(26) }
            )

            // 挑战
            DifficultyCard(
                emoji = "🌳",
                title = "挑战 (51-75关)",
                subtitle = "13个广泛类别",
                color = Color(0xFFFF9800),
                onClick = { module.start(51) }
            )

            // 极限
            DifficultyCard(
                emoji = "🏆",
                title = "极限 (76-100关)",
                subtitle = "14个全部类别",
                color = Color(0xFFF44336),
                onClick = { module.start(76) }
            )
        }
    }
}

@Composable
private fun DifficultyCard(
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
                .padding(24.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(emoji, fontSize = 44.sp)
            Spacer(modifier = Modifier.width(20.dp))
            Column {
                Text(title, fontSize = 22.sp, fontWeight = FontWeight.Bold, color = Color.White)
                Text(subtitle, fontSize = 16.sp, color = Color.White.copy(alpha = 0.85f))
            }
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
        Text(
            text = "☁️",
            fontSize = 50.sp,
            modifier = Modifier
                .align(Alignment.TopStart)
                .offset(x = 20.dp, y = 60.dp + cloudFloat.dp)
        )
        Text(
            text = "☁️",
            fontSize = 40.sp,
            modifier = Modifier
                .align(Alignment.TopEnd)
                .offset(x = (-40).dp, y = 100.dp - cloudFloat.dp)
        )
        Text(
            text = "☀️",
            fontSize = 40.sp,
            modifier = Modifier
                .align(Alignment.TopEnd)
                .offset(x = (-30).dp, y = 50.dp)
        )
    }
}

// ==================== 游戏进行中 ====================

@Composable
private fun PlayingScreen(
    module: WordChainGameModule,
    onBack: () -> Unit,
    onChangeTheme: () -> Unit
) {
    val currentCategory = module.getCurrentCategory()

    // 动画状态
    var animationKey by remember { mutableIntStateOf(0) }
    var isAnimating by remember { mutableStateOf(false) }

    // 缩放动画（点击按钮时触发）
    val scale by animateFloatAsState(
        targetValue = if (isAnimating) 1.1f else 1f,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness = Spring.StiffnessLow
        ),
        label = "scale"
    )

    Column(modifier = Modifier.fillMaxSize()) {
        // 顶部导航栏
        GameTopBar(
            title = "💬 ${module.getDifficultyName()}",
            level = module.getLevelIndex(),
            difficultyName = module.getDifficultyName(),
            score = 0,
            stars = 0,
            onBack = onBack
        )

        // 主题显示区域
        Column(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
                .padding(20.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            // 主题卡片
            CategoryCard(
                category = currentCategory,
                modifier = Modifier.scale(scale)
            )

            Spacer(modifier = Modifier.height(24.dp))

            // 提示文字
            Text(
                text = "🏃 轮流说一个相关词",
                fontSize = 22.sp,
                fontWeight = FontWeight.Medium,
                color = Color(0xFF5D4037),
                textAlign = TextAlign.Center
            )

            Spacer(modifier = Modifier.height(12.dp))

            // 参与人数提示
            Text(
                text = "👨‍👩‍👧‍👦 多人一起玩更开心！",
                fontSize = 18.sp,
                color = Color(0xFF8D6E63),
                textAlign = TextAlign.Center
            )
        }

        // 底部切换按钮
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .padding(24.dp),
            contentAlignment = Alignment.Center
        ) {
            ChangeButton(
                onClick = {
                    isAnimating = true
                    animationKey++
                    module.changeCategory()
                    onChangeTheme()
                }
            )

            // 自动重置动画状态
            LaunchedEffect(isAnimating) {
                if (isAnimating) {
                    delay(200)
                    isAnimating = false
                }
            }
        }
    }
}

// ==================== 主题卡片 ====================

@Composable
private fun CategoryCard(
    category: String,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        shape = RoundedCornerShape(32.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 12.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(40.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // 主题标签
            Surface(
                color = Color(0xFFE3F2FD),
                shape = RoundedCornerShape(12.dp)
            ) {
                Text(
                    text = "📝 当前主题",
                    fontSize = 18.sp,
                    color = Color(0xFF1976D2),
                    modifier = Modifier.padding(horizontal = 20.dp, vertical = 8.dp)
                )
            }

            Spacer(modifier = Modifier.height(24.dp))

            // 主题词（超大字体）
            Text(
                text = "【$category】",
                fontSize = 56.sp,
                fontWeight = FontWeight.Bold,
                color = Color(0xFFFF6B6B),
                textAlign = TextAlign.Center
            )
        }
    }
}

// ==================== 切换按钮 ====================

@Composable
private fun ChangeButton(onClick: () -> Unit) {
    val infiniteTransition = rememberInfiniteTransition(label = "button")
    val buttonScale by infiniteTransition.animateFloat(
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
            .scale(buttonScale),
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
            text = "🔄 换一个",
            fontSize = 28.sp,
            fontWeight = FontWeight.Bold,
            color = Color.White
        )
    }
}