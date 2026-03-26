package com.animalgame.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.animalgame.core.manager.SettingsManager
import com.animalgame.core.model.GameSettings
import kotlinx.coroutines.launch

// 配色方案
private object SettingsColors {
    val BackgroundStart = Color(0xFFF0F4FF)
    val BackgroundEnd = Color(0xFFE8F0FE)
    val CardBackground = Color.White
    val PrimaryText = Color(0xFF1E293B)
    val SecondaryText = Color(0xFF64748B)
    val Accent = Color(0xFF6366F1)
    val AccentLight = Color(0xFF818CF8)
}

/**
 * 设置页面
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    onBack: () -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    // 获取设置 Flow
    val settingsFlow = remember { SettingsManager.getSettingsFlow(context) }
    val settings by settingsFlow.collectAsState(initial = GameSettings.DEFAULT)

    // 本地状态
    var soundVolume by remember(settings.soundVolume) { mutableFloatStateOf(settings.soundVolume) }
    var musicEnabled by remember(settings.musicEnabled) { mutableStateOf(settings.musicEnabled) }
    var vibrationEnabled by remember(settings.vibrationEnabled) { mutableStateOf(settings.vibrationEnabled) }

    // 背景
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    colors = listOf(SettingsColors.BackgroundStart, SettingsColors.BackgroundEnd)
                )
            )
    ) {
        Scaffold(
            topBar = {
                TopAppBar(
                    title = {
                        Text(
                            text = "设置",
                            fontSize = 24.sp,
                            fontWeight = FontWeight.Bold,
                            color = SettingsColors.PrimaryText
                        )
                    },
                    navigationIcon = {
                        Text(
                            text = "← 返回",
                            color = SettingsColors.SecondaryText,
                            fontSize = 14.sp,
                            modifier = Modifier
                                .clickable { onBack() }
                                .padding(start = 16.dp)
                        )
                    },
                    colors = TopAppBarDefaults.topAppBarColors(
                        containerColor = Color.Transparent
                    )
                )
            }
        ) { innerPadding ->
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
                    .padding(horizontal = 16.dp)
            ) {
            SettingsCard(
                title = "音效设置",
                icon = null
            ) {
                // 音量滑块
                SettingsSliderItem(
                    title = "音效音量",
                    value = soundVolume,
                    onValueChange = { newValue ->
                        soundVolume = newValue
                        scope.launch {
                            SettingsManager.updateSoundVolume(context, newValue)
                        }
                    }
                )

                Spacer(modifier = Modifier.height(12.dp))

                // 音乐开关
                SettingsSwitchItem(
                    title = "背景音乐",
                    subtitle = "游戏背景音乐",
                    checked = musicEnabled,
                    onCheckedChange = { enabled ->
                        musicEnabled = enabled
                        scope.launch {
                            SettingsManager.updateMusicEnabled(context, enabled)
                        }
                    }
                )

                Spacer(modifier = Modifier.height(12.dp))

                // 震动开关
                SettingsSwitchItem(
                    title = "震动反馈",
                    subtitle = "配对成功时的震动效果",
                    checked = vibrationEnabled,
                    onCheckedChange = { enabled ->
                        vibrationEnabled = enabled
                        scope.launch {
                            SettingsManager.updateVibrationEnabled(context, enabled)
                        }
                    }
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            // 扩展设置（预留）
            SettingsCard(
                title = "更多设置",
                icon = null
            ) {
                // 语言设置（预留）
                SettingsInfoItem(
                    title = "语言",
                    value = when (settings.language) {
                        "zh" -> "简体中文"
                        "en" -> "English"
                        else -> "简体中文"
                    }
                )

                Spacer(modifier = Modifier.height(12.dp))

                // 图标主题（预留）
                SettingsInfoItem(
                    title = "图标主题",
                    value = when (settings.iconTheme) {
                        "default" -> "默认"
                        "colorful" -> "彩色"
                        else -> "默认"
                    }
                )
            }

            Spacer(modifier = Modifier.weight(1f))

            // 版本信息
            Text(
                text = "版本 1.0.0",
                fontSize = 12.sp,
                color = SettingsColors.SecondaryText,
                modifier = Modifier.align(Alignment.CenterHorizontally)
            )
        }
        }
    }
}

/**
 * 设置卡片容器
 */
@Composable
private fun SettingsCard(
    title: String,
    icon: ImageVector? = null,
    content: @Composable () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = SettingsColors.CardBackground),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            Text(
                text = title,
                fontSize = 16.sp,
                fontWeight = FontWeight.Bold,
                color = SettingsColors.Accent
            )
            Spacer(modifier = Modifier.height(16.dp))
            content()
        }
    }
}

/**
 * 滑块设置项
 */
@Composable
private fun SettingsSliderItem(
    title: String,
    value: Float,
    onValueChange: (Float) -> Unit
) {
    Column {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = title,
                fontSize = 14.sp,
                color = SettingsColors.PrimaryText
            )
            Text(
                text = "${(value * 100).toInt()}%",
                fontSize = 14.sp,
                fontWeight = FontWeight.Bold,
                color = SettingsColors.Accent
            )
        }
        Spacer(modifier = Modifier.height(8.dp))
        Slider(
            value = value,
            onValueChange = onValueChange,
            modifier = Modifier.fillMaxWidth(),
            colors = SliderDefaults.colors(
                thumbColor = SettingsColors.Accent,
                activeTrackColor = SettingsColors.Accent,
                inactiveTrackColor = SettingsColors.Accent.copy(alpha = 0.3f)
            )
        )
    }
}

/**
 * 开关设置项
 */
@Composable
private fun SettingsSwitchItem(
    title: String,
    subtitle: String? = null,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title,
                fontSize = 14.sp,
                color = SettingsColors.PrimaryText
            )
            if (subtitle != null) {
                Text(
                    text = subtitle,
                    fontSize = 12.sp,
                    color = SettingsColors.SecondaryText
                )
            }
        }
        Switch(
            checked = checked,
            onCheckedChange = onCheckedChange,
            colors = SwitchDefaults.colors(
                checkedThumbColor = Color.White,
                checkedTrackColor = SettingsColors.Accent,
                uncheckedThumbColor = Color.White,
                uncheckedTrackColor = SettingsColors.SecondaryText.copy(alpha = 0.3f)
            )
        )
    }
}

/**
 * 信息展示项（只读）
 */
@Composable
private fun SettingsInfoItem(
    title: String,
    value: String
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = title,
            fontSize = 14.sp,
            color = SettingsColors.PrimaryText
        )
        Text(
            text = value,
            fontSize = 14.sp,
            color = SettingsColors.SecondaryText
        )
    }
}
