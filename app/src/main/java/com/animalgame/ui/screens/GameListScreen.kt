package com.animalgame.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.animalgame.core.manager.GameRegistry

// 配色方案
private object ScreenColors {
    val BackgroundStart = Color(0xFFE8F4FD)
    val BackgroundEnd = Color(0xFFF3E8FD)
    val CardBackground = Color.White
    val PrimaryText = Color(0xFF2D2D2D)
    val SecondaryText = Color(0xFF757575)
    val Gold = Color(0xFFFFD700)
    val ButtonGreen = Color(0xFF81C784)
    val ButtonBlue = Color(0xFF64B5F6)
}

/**
 * 游戏列表页
 */
@Composable
fun GameListScreen(
    onGameClick: (String) -> Unit,
    onSettingsClick: () -> Unit
) {
    var totalStars by remember { mutableIntStateOf(0) }

    // 获取所有已注册的游戏
    val games = remember { GameRegistry.getAllGames() }

    LaunchedEffect(Unit) {
        // 可以在这里加载星星总数
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    colors = listOf(ScreenColors.BackgroundStart, ScreenColors.BackgroundEnd)
                )
            )
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // 顶部标题 + 设置按钮
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Spacer(modifier = Modifier.weight(1f))
                Text(
                    text = "专注力训练营",
                    fontSize = 28.sp,
                    fontWeight = FontWeight.Bold,
                    color = ScreenColors.PrimaryText
                )
                IconButton(
                    onClick = onSettingsClick,
                    modifier = Modifier.size(48.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Settings,
                        contentDescription = "设置",
                        tint = ScreenColors.PrimaryText
                    )
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = "选择游戏开始训练",
                fontSize = 14.sp,
                color = ScreenColors.SecondaryText
            )

            Spacer(modifier = Modifier.height(32.dp))

            // 游戏网格
            LazyVerticalGrid(
                columns = GridCells.Fixed(2),
                contentPadding = PaddingValues(8.dp),
                horizontalArrangement = Arrangement.spacedBy(16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                items(games) { game ->
                    GameCard(
                        gameName = game.gameName,
                        levelCount = game.totalLevels,
                        description = game.description,
                        onClick = { onGameClick(game.gameId) }
                    )
                }
            }
        }
    }
}

/**
 * 游戏卡片组件
 */
@Composable
private fun GameCard(
    gameName: String,
    levelCount: Int,
    description: String,
    onClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .height(140.dp)
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = ScreenColors.CardBackground),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            // 游戏图标（使用emoji占位）
            Text(
                text = getGameEmoji(gameName),
                fontSize = 40.sp
            )

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = gameName,
                fontSize = 16.sp,
                fontWeight = FontWeight.Bold,
                color = ScreenColors.PrimaryText,
                textAlign = TextAlign.Center
            )

            Spacer(modifier = Modifier.height(4.dp))

            Text(
                text = "$levelCount 关",
                fontSize = 12.sp,
                color = ScreenColors.SecondaryText
            )
        }
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
        else -> "🎮"
    }
}
