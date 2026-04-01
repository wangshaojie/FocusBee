package com.animalgame.ui.screens

import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.scale
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.unit.min
import com.animalgame.core.manager.GameRegistry

/**
 * 游戏列表页 - 儿童友好版本（适配5-8寸屏幕）
 */
@Composable
fun GameListScreen(
    onGameClick: (String) -> Unit,
    onSettingsClick: () -> Unit
) {
    var totalStars by remember { mutableIntStateOf(0) }
    val games = remember { GameRegistry.getAllGames() }

    // 获取屏幕尺寸
    val configuration = LocalConfiguration.current
    val screenWidthDp = configuration.screenWidthDp.dp
    val screenHeightDp = configuration.screenHeightDp.dp

    // 根据屏幕尺寸计算响应式数值
    val isSmallScreen = screenWidthDp < 360.dp
    val isLargeScreen = screenWidthDp >= 600.dp

    // 响应式尺寸
    val basePadding = if (isSmallScreen) 8.dp else if (isLargeScreen) 20.dp else 16.dp
    val titleSize = if (isSmallScreen) 22.sp else if (isLargeScreen) 32.sp else 28.sp
    val subtitleSize = if (isSmallScreen) 11.sp else if (isLargeScreen) 16.sp else 14.sp
    val gridSpacing = if (isSmallScreen) 10.dp else if (isLargeScreen) 18.dp else 14.dp
    val cardHeight = when {
        screenWidthDp < 360.dp -> 130.dp
        screenWidthDp < 400.dp -> 145.dp
        screenWidthDp >= 600.dp -> 180.dp
        else -> 160.dp
    }
    val emojiSize = when {
        screenWidthDp < 360.dp -> 50.dp
        screenWidthDp < 400.dp -> 58.dp
        screenWidthDp >= 600.dp -> 75.dp
        else -> 65.dp
    }
    val gameNameSize = when {
        screenWidthDp < 360.dp -> 14.sp
        screenWidthDp < 400.dp -> 16.sp
        else -> 18.sp
    }
    val bottomHintPadding = if (isSmallScreen) 8.dp else 16.dp

    // 装饰性动画
    val infiniteTransition = rememberInfiniteTransition(label = "deco")
    val cloudFloat by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 12f,
        animationSpec = infiniteRepeatable(
            animation = tween(3000, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "cloudFloat"
    )
    val star1Alpha by infiniteTransition.animateFloat(
        initialValue = 0.4f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(1500),
            repeatMode = RepeatMode.Reverse
        ),
        label = "star1"
    )
    val star2Alpha by infiniteTransition.animateFloat(
        initialValue = 0.5f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(2000),
            repeatMode = RepeatMode.Reverse
        ),
        label = "star2"
    )

    // 彩虹糖果背景
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
                    )
                )
            )
    ) {
        // 装饰性云朵和星星
        DecoratedElements(
            cloudFloat = cloudFloat,
            star1Alpha = star1Alpha,
            star2Alpha = star2Alpha,
            emojiSize = emojiSize,
            screenWidthDp = screenWidthDp
        )

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(basePadding),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // 顶部标题区域
            TopTitleSection(
                onSettingsClick = onSettingsClick,
                titleSize = titleSize,
                subtitleSize = subtitleSize,
                buttonSize = if (isSmallScreen) 40.dp else 50.dp
            )

            Spacer(modifier = Modifier.height(basePadding))

            // 游戏网格
            BoxWithConstraints(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
            ) {
                val availableWidth = maxWidth
                val columns = if (availableWidth < 300.dp) 2 else 2

                LazyVerticalGrid(
                    columns = GridCells.Fixed(columns),
                    contentPadding = PaddingValues(4.dp),
                    horizontalArrangement = Arrangement.spacedBy(gridSpacing),
                    verticalArrangement = Arrangement.spacedBy(gridSpacing),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    items(games) { game ->
                        KidFriendlyGameCard(
                            gameName = game.gameName,
                            levelCount = game.totalLevels,
                            description = game.description,
                            emoji = getGameEmoji(game.gameName),
                            cardHeight = cardHeight,
                            emojiSize = emojiSize,
                            gameNameSize = gameNameSize,
                            onClick = { onGameClick(game.gameId) }
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(bottomHintPadding))

            // 底部提示
            BottomHint()
        }
    }
}

/**
 * 装饰性元素
 */
@Composable
private fun DecoratedElements(
    cloudFloat: Float,
    star1Alpha: Float,
    star2Alpha: Float,
    emojiSize: Dp,
    screenWidthDp: Dp
) {
    val decorSize = min(emojiSize, 50.dp)

    Box(modifier = Modifier.fillMaxSize()) {
        // 云朵
        Text(
            text = "☁️",
            fontSize = decorSize.value.sp,
            modifier = Modifier
                .align(Alignment.TopStart)
                .offset(x = 15.dp, y = 50.dp + cloudFloat.dp)
        )
        Text(
            text = "☁️",
            fontSize = (decorSize.value * 0.8f).sp,
            modifier = Modifier
                .align(Alignment.TopEnd)
                .offset(x = (-30).dp, y = 80.dp - cloudFloat.dp)
        )
        Text(
            text = "☁️",
            fontSize = (decorSize.value * 0.7f).sp,
            modifier = Modifier
                .align(Alignment.BottomStart)
                .offset(x = 50.dp, y = (-50).dp - cloudFloat.dp)
        )

        // 星星
        Text(
            text = "⭐",
            fontSize = (decorSize.value * 0.6f).sp,
            modifier = Modifier
                .align(Alignment.TopStart)
                .offset(x = screenWidthDp * 0.3f, y = 60.dp)
                .alpha(star1Alpha)
        )
        Text(
            text = "✨",
            fontSize = (decorSize.value * 0.5f).sp,
            modifier = Modifier
                .align(Alignment.TopEnd)
                .offset(x = (-50).dp, y = 120.dp)
                .alpha(star2Alpha)
        )

        // 彩虹
        Text(
            text = "🌈",
            fontSize = (decorSize.value * 1.2f).sp,
            modifier = Modifier
                .align(Alignment.TopEnd)
                .offset(x = (-15).dp, y = 30.dp)
        )

        // 游戏手柄装饰
        Text(
            text = "🎮",
            fontSize = decorSize.value.sp,
            modifier = Modifier
                .align(Alignment.BottomStart)
                .offset(x = 20.dp, y = (-30).dp)
        )

        // 奖杯装饰
        Text(
            text = "🏆",
            fontSize = (decorSize.value * 0.7f).sp,
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .offset(x = (-40).dp, y = (-20).dp)
        )
    }
}

/**
 * 顶部标题区域
 */
@Composable
private fun TopTitleSection(
    onSettingsClick: () -> Unit,
    titleSize: androidx.compose.ui.unit.TextUnit,
    subtitleSize: androidx.compose.ui.unit.TextUnit,
    buttonSize: Dp
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        // 设置按钮
        IconButton(
            onClick = onSettingsClick,
            modifier = Modifier.size(buttonSize)
        ) {
            Box(
                modifier = Modifier
                    .size(buttonSize - 6.dp)
                    .shadow(6.dp, RoundedCornerShape(10.dp))
                    .background(
                        color = Color.White,
                        shape = RoundedCornerShape(10.dp)
                    ),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.Settings,
                    contentDescription = "设置",
                    tint = Color(0xFF5D4037),
                    modifier = Modifier.size(buttonSize * 0.5f)
                )
            }
        }

        // 标题
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.weight(1f)
        ) {
            Text(
                text = "🎯 专注力训练营",
                fontSize = titleSize,
                fontWeight = FontWeight.Bold,
                color = Color(0xFF5D4037)
            )
            Text(
                text = "选择喜欢的游戏开始玩吧！",
                fontSize = subtitleSize,
                color = Color(0xFF8D6E63)
            )
        }

        // 占位保持对称
        Spacer(modifier = Modifier.size(buttonSize))
    }
}

/**
 * 底部提示
 */
@Composable
private fun BottomHint() {
    Card(
        colors = CardDefaults.cardColors(
            containerColor = Color.White.copy(alpha = 0.85f)
        ),
        shape = RoundedCornerShape(16.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "🎮",
                fontSize = 18.sp
            )
            Spacer(modifier = Modifier.width(6.dp))
            Text(
                text = "更多新游筹备中，敬请期待！✨",
                fontSize = 13.sp,
                fontWeight = FontWeight.Medium,
                color = Color(0xFF5D4037)
            )
        }
    }
}

/**
 * 儿童友好游戏卡片
 */
@Composable
private fun KidFriendlyGameCard(
    gameName: String,
    levelCount: Int,
    description: String,
    emoji: String,
    cardHeight: Dp,
    emojiSize: Dp,
    gameNameSize: androidx.compose.ui.unit.TextUnit,
    onClick: () -> Unit
) {
    // 点击动画
    var isPressed by remember { mutableStateOf(false) }
    val scale by animateFloatAsState(
        targetValue = if (isPressed) 0.95f else 1f,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness = Spring.StiffnessMedium
        ),
        label = "cardScale"
    )

    // 获取游戏主题色
    val (gradientColors, textColor) = getGameThemeColors(gameName)

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .height(cardHeight)
            .scale(scale)
            .shadow(
                elevation = 10.dp,
                shape = RoundedCornerShape(20.dp),
                spotColor = gradientColors.first()
            )
            .clickable {
                isPressed = true
                onClick()
            },
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(
            containerColor = Color.White
        )
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    Brush.verticalGradient(
                        colors = listOf(
                            gradientColors.first().copy(alpha = 0.15f),
                            gradientColors.last().copy(alpha = 0.05f)
                        )
                    )
                )
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(12.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                // 游戏图标
                Box(
                    modifier = Modifier
                        .size(emojiSize)
                        .shadow(6.dp, RoundedCornerShape(16.dp))
                        .background(
                            brush = Brush.verticalGradient(gradientColors),
                            shape = RoundedCornerShape(16.dp)
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = emoji,
                        fontSize = (emojiSize.value * 0.55f).sp
                    )
                }

                Spacer(modifier = Modifier.height(8.dp))

                // 游戏名称
                Text(
                    text = gameName,
                    fontSize = gameNameSize,
                    fontWeight = FontWeight.Bold,
                    color = textColor,
                    textAlign = TextAlign.Center,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )

                Spacer(modifier = Modifier.height(2.dp))

                // 关卡数
                Text(
                    text = "$levelCount 关",
                    fontSize = (gameNameSize.value - 3).sp,
                    color = Color(0xFF9E9E9E),
                    fontWeight = FontWeight.Medium
                )
            }
        }
    }

    LaunchedEffect(isPressed) {
        if (isPressed) {
            kotlinx.coroutines.delay(100)
            isPressed = false
        }
    }
}

/**
 * 获取游戏主题色
 */
private fun getGameThemeColors(gameName: String): Pair<List<Color>, Color> {
    return when (gameName) {
        "萌音大挑战" -> listOf(Color(0xFFFFB74D), Color(0xFFFFA726)) to Color(0xFFE65100)
        "舒尔特训练" -> listOf(Color(0xFF7986CB), Color(0xFF5C6BC0)) to Color(0xFF283593)
        "记忆翻牌" -> listOf(Color(0xFFBA68C8), Color(0xFFAB47BC)) to Color(0xFF6A1B9A)
        "颜色识别" -> listOf(Color(0xFFFF7043), Color(0xFFFF5722)) to Color(0xFFBF360C)
        "平衡小球" -> listOf(Color(0xFF4FC3F7), Color(0xFF29B6F6)) to Color(0xFF0277BD)
        "方块推推乐" -> listOf(Color(0xFFFFD54F), Color(0xFFFFC107)) to Color(0xFFF57F17)
        "镜像绘图" -> listOf(Color(0xFF81C784), Color(0xFF66BB6A)) to Color(0xFF2E7D32)
        "灯塔路径" -> listOf(Color(0xFFFFE082), Color(0xFFFFD54F)) to Color(0xFFF9A825)
        "数字连连看" -> listOf(Color(0xFF4DD0E1), Color(0xFF26C6DA)) to Color(0xFF00838F)
        else -> listOf(Color(0xFF90A4AE), Color(0xFF78909C)) to Color(0xFF455A64)
    }
}

/**
 * 获取游戏对应的图标
 */
private fun getGameEmoji(gameName: String): String {
    return when (gameName) {
        "萌音大挑战" -> "🐕"
        "舒尔特训练" -> "🔢"
        "记忆翻牌" -> "🃏"
        "颜色识别" -> "🎨"
        "平衡小球" -> "🎯"
        "方块推推乐" -> "🔲"
        "镜像绘图" -> "🖌️"
        "灯塔路径" -> "🏠"
        "数字连连看" -> "🔢"
        else -> "🎮"
    }
}
