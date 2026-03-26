package com.animalgame.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

/**
 * 统一的游戏顶部导航栏
 * 包含：返回按钮 + 关卡信息 + 分数/星星
 */
@Composable
fun GameTopBar(
    title: String,
    level: Int = 1,
    score: Int = 0,
    stars: Int = 0,
    difficultyName: String? = null,
    onBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    // 马卡龙色系
    val primaryColor = Color(0xFF81D4FA)  // 浅蓝
    val starColor = Color(0xFFFFD54F)    // 金色
    val grayColor = Color(0xFFE0E0E0)    // 灰色（未点亮星星）

    // 构造关卡显示文本
    val levelText = if (difficultyName != null) {
        "$difficultyName · 第${level}关"
    } else {
        "第${level}关"
    }

    Row(
        modifier = modifier
            .fillMaxWidth()
            .background(Color(0xFFF8F6FF))  // 护眼淡紫背景
            .padding(horizontal = 12.dp, vertical = 12.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        // 左侧：返回按钮（胶囊风格）
        Box(
            modifier = Modifier
                .size(40.dp)
                .clip(RoundedCornerShape(20.dp))
                .background(Color.White)
                .clickable { onBack() },
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = Icons.Filled.ArrowBack,
                contentDescription = "返回",
                tint = primaryColor,
                modifier = Modifier.size(24.dp)
            )
        }

        // 中间：关卡信息
        Row(
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = title,
                fontSize = 16.sp,
                fontWeight = FontWeight.Medium,
                color = Color(0xFF5C6BC0)
            )
            Text(
                text = " · $levelText",
                fontSize = 14.sp,
                color = Color(0xFF9E9E9E)
            )
        }

        // 右侧：分数 + 星星
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            // 分数
            Text(
                text = "$score",
                fontSize = 14.sp,
                fontWeight = FontWeight.Bold,
                color = Color(0xFFFF8A65)
            )

            // 星星
            Row {
                repeat(3) { index ->
                    Text(
                        text = if (index < stars) "⭐" else "☆",
                        fontSize = 14.sp
                    )
                }
            }
        }
    }
}
