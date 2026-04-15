package com.animalgame.games.quickquiz

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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.animalgame.core.game.GameAction
import com.animalgame.core.game.GameModule
import com.animalgame.core.game.GameState
import com.animalgame.ui.components.GameTopBar
import kotlinx.coroutines.launch

/**
 * 抢答题游戏 UI - 儿童百科知识问答
 */
@Composable
fun QuizGameScreen(
    module: GameModule,
    onBack: () -> Unit
) {
    val quizModule = module as? QuizGameModule
    val gameState by module.state.collectAsState()
    val context = LocalContext.current

    // 加载题库
    val scope = rememberCoroutineScope()
    var isLoadingQuestions by remember { mutableStateOf(true) }

    LaunchedEffect(Unit) {
        quizModule?.loadQuestions(context)
        isLoadingQuestions = false
    }

    val handleBack: () -> Unit = {
        when (gameState) {
            is GameState.Idle -> onBack()
            else -> quizModule?.resetToIdle()
        }
    }

    // 彩虹渐变背景（儿童友好）
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    colors = listOf(
                        Color(0xFFFFB6C1),  // 浅粉
                        Color(0xFFFFDAB9),  // 桃色
                        Color(0xFF98FB98),  // 浅绿
                        Color(0xFF87CEEB),  // 天蓝
                    )
                )
            )
    ) {
        // 装饰性云朵
        DecoratedClouds()

        when (val state = gameState) {
            is GameState.Idle -> {
                if (isLoadingQuestions) {
                    LoadingScreen()
                } else {
                    DifficultySelectScreen(
                        module = quizModule!!,
                        onBack = handleBack
                    )
                }
            }
            is GameState.Playing -> {
                PlayingScreen(
                    state = state,
                    module = quizModule!!,
                    onBack = handleBack
                )
            }
            is GameState.Completed -> {
                CompletedScreen(
                    state = state,
                    module = quizModule!!,
                    onReplay = { quizModule.replayDifficulty() },
                    onBack = handleBack
                )
            }
            is GameState.AllCompleted -> {
                AllCompletedScreen(
                    state = state,
                    onBack = handleBack
                )
            }
            else -> {
                // 加载中
                LoadingScreen()
            }
        }
    }
}

// ==================== 装饰性云朵 ====================

@Composable
private fun DecoratedClouds() {
    Box(modifier = Modifier.fillMaxSize()) {
        Text("☁️", fontSize = 50.sp, modifier = Modifier.align(Alignment.TopStart).offset(x = 30.dp, y = 60.dp))
        Text("☁️", fontSize = 40.sp, modifier = Modifier.align(Alignment.TopEnd).offset(x = (-60).dp, y = 100.dp))
        Text("☀️", fontSize = 50.sp, modifier = Modifier.align(Alignment.TopEnd).offset(x = (-30).dp, y = 50.dp))
        Text("🌈", fontSize = 40.sp, modifier = Modifier.align(Alignment.TopStart).offset(x = 120.dp, y = 80.dp))
    }
}

// ==================== 加载中 ====================

@Composable
private fun LoadingScreen() {
    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            CircularProgressIndicator(
                color = Color(0xFFFF6B6B),
                modifier = Modifier.size(60.dp)
            )
            Spacer(modifier = Modifier.height(20.dp))
            Text(
                "📚 正在加载题库...",
                fontSize = 24.sp,
                fontWeight = FontWeight.Bold,
                color = Color(0xFF5D4037)
            )
        }
    }
}

// ==================== 难度选择 ====================

@Composable
private fun DifficultySelectScreen(
    module: QuizGameModule,
    onBack: () -> Unit
) {
    Column(modifier = Modifier.fillMaxSize()) {
        GameTopBar(
            title = "📝 抢答题",
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
                "📝 抢答题",
                fontSize = 32.sp,
                color = Color(0xFF5D4037),
                textAlign = TextAlign.Center,
                fontWeight = FontWeight.Bold
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                "想一想答案是什么？点击查看揭晓！",
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
            // 简单
            QuizDifficultyCard(
                emoji = "🌱",
                title = "简单 (1-100关)",
                subtitle = "100道基础百科知识",
                color = Color(0xFF4CAF50),
                onClick = { module.start(1) }
            )

            // 进阶
            QuizDifficultyCard(
                emoji = "🌿",
                title = "进阶 (101-200关)",
                subtitle = "100道进阶百科知识",
                color = Color(0xFF2196F3),
                onClick = { module.start(101) }
            )

            // 挑战
            QuizDifficultyCard(
                emoji = "🌳",
                title = "挑战 (201-300关)",
                subtitle = "100道高难度百科知识",
                color = Color(0xFFFF9800),
                onClick = { module.start(201) }
            )

            // 极限
            QuizDifficultyCard(
                emoji = "🏆",
                title = "极限 (301-400关)",
                subtitle = "100道极限百科知识",
                color = Color(0xFFF44336),
                onClick = { module.start(301) }
            )
        }
    }
}

@Composable
private fun QuizDifficultyCard(
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

// ==================== 游戏进行中 ====================

@Composable
private fun PlayingScreen(
    state: GameState.Playing,
    module: QuizGameModule,
    onBack: () -> Unit
) {
    val phaseStr = state.data["currentPhase"] as? String ?: "SHOW_QUESTION"
    val currentPhase = try { QuizPhase.valueOf(phaseStr) } catch (e: Exception) { QuizPhase.SHOW_QUESTION }
    val questionText = state.data["question"] as? String ?: ""
    val answerLetter = state.data["answer"] as? String ?: ""
    val answerContent = state.data["answerContent"] as? String ?: ""
    val topic = state.data["topic"] as? String ?: ""
    val progress = state.data["currentProgress"] as? Int ?: 1
    val total = state.data["totalQuestions"] as? Int ?: 100
    val difficultyName = state.data["difficulty"] as? String ?: "EASY"
    val levelId = state.data["levelId"] as? String ?: ""

    // 按钮点击动画
    val buttonScale by animateFloatAsState(
        targetValue = 1f,
        animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy, stiffness = Spring.StiffnessLow),
        label = "buttonScale"
    )

    Column(modifier = Modifier.fillMaxSize()) {
        GameTopBar(
            title = "📝 ${QuizDifficulty.valueOf(difficultyName).displayName}",
            level = module.getLevelIndex(),
            score = state.score,
            stars = 0,
            onBack = onBack
        )

        // 进度条
        ProgressBar(progress = progress, total = total)

        // 题目卡片
        Column(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
                .padding(20.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            // 题目卡片
            QuestionCard(
                question = questionText,
                topic = topic,
                modifier = Modifier.weight(1f)
            )

            // 答案区域（点击查看后显示）
            AnimatedVisibility(
                visible = currentPhase == QuizPhase.SHOW_ANSWER,
                enter = fadeIn() + expandVertically(),
                exit = fadeOut() + shrinkVertically()
            ) {
                AnswerCard(
                    answerLetter = answerLetter,
                    answerContent = answerContent
                )
            }
        }

        // 底部按钮
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .padding(24.dp),
            contentAlignment = Alignment.Center
        ) {
            BigActionButton(
                text = if (currentPhase == QuizPhase.SHOW_QUESTION) "🤔 查看答案" else "➡️ 下一题",
                color = if (currentPhase == QuizPhase.SHOW_QUESTION) Color(0xFFFF6B6B) else Color(0xFF4CAF50),
                modifier = Modifier.scale(buttonScale)
            ) {
                module.handleButtonClick()
            }
        }
    }
}

// ==================== 进度条 ====================

@Composable
private fun ProgressBar(progress: Int, total: Int) {
    val animatedProgress by animateFloatAsState(
        targetValue = progress.toFloat() / total.toFloat(),
        animationSpec = tween(300),
        label = "progress"
    )

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp, vertical = 12.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(
                text = "📝 第 $progress 题",
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold,
                color = Color(0xFF5D4037)
            )
            Text(
                text = "共 $total 题",
                fontSize = 16.sp,
                color = Color(0xFF8D6E63)
            )
        }
        Spacer(modifier = Modifier.height(8.dp))
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(16.dp)
                .clip(RoundedCornerShape(8.dp))
                .background(Color.White.copy(alpha = 0.5f))
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth(animatedProgress)
                    .height(16.dp)
                    .clip(RoundedCornerShape(8.dp))
                    .background(
                        Brush.horizontalGradient(
                            colors = listOf(Color(0xFFFF6B6B), Color(0xFFFFD93D))
                        )
                    )
            )
        }
    }
}

// ==================== 题目卡片 ====================

@Composable
private fun QuestionCard(
    question: String,
    topic: String,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        shape = RoundedCornerShape(24.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 8.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(28.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            // 主题标签
            Surface(
                color = Color(0xFFE3F2FD),
                shape = RoundedCornerShape(12.dp)
            ) {
                Text(
                    text = "🏷️ $topic",
                    fontSize = 16.sp,
                    color = Color(0xFF1976D2),
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 6.dp)
                )
            }

            Spacer(modifier = Modifier.height(24.dp))

            // 题目文字（大字体）
            Text(
                text = question,
                fontSize = 34.sp,
                fontWeight = FontWeight.Bold,
                color = Color(0xFF37474F),
                textAlign = TextAlign.Center,
                lineHeight = 46.sp
            )
        }
    }
}

// ==================== 答案卡片 ====================

@Composable
private fun AnswerCard(
    answerLetter: String,
    answerContent: String
) {
    // 答案揭晓动画
    val scale by animateFloatAsState(
        targetValue = 1f,
        animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy, stiffness = Spring.StiffnessLow),
        label = "answerScale"
    )

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 16.dp)
            .scale(scale),
        colors = CardDefaults.cardColors(containerColor = Color(0xFFFFF9C4)),
        shape = RoundedCornerShape(24.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 6.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = "✨ 答案是...",
                fontSize = 20.sp,
                color = Color(0xFF8D6E63)
            )

            Spacer(modifier = Modifier.height(16.dp))

            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.Center
            ) {
                // 答案字母
                Surface(
                    color = Color(0xFFFF6B6B),
                    shape = RoundedCornerShape(16.dp)
                ) {
                    Text(
                        text = answerLetter,
                        fontSize = 48.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White,
                        modifier = Modifier.padding(horizontal = 24.dp, vertical = 8.dp)
                    )
                }

                Spacer(modifier = Modifier.width(20.dp))

                // 答案内容
                Text(
                    text = answerContent,
                    fontSize = 36.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFF5D4037)
                )
            }
        }
    }
}

// ==================== 大按钮 ====================

@Composable
private fun BigActionButton(
    text: String,
    color: Color,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    Button(
        onClick = onClick,
        modifier = modifier
            .fillMaxWidth()
            .height(70.dp),
        colors = ButtonDefaults.buttonColors(containerColor = color),
        shape = RoundedCornerShape(35.dp),
        elevation = ButtonDefaults.buttonElevation(defaultElevation = 8.dp)
    ) {
        Text(
            text = text,
            fontSize = 24.sp,
            fontWeight = FontWeight.Bold,
            color = Color.White
        )
    }
}

// ==================== 完成页面 ====================

@Composable
private fun CompletedScreen(
    state: GameState.Completed,
    module: QuizGameModule,
    onReplay: () -> Unit,
    onBack: () -> Unit
) {
    val successScale by animateFloatAsState(
        targetValue = 1f,
        animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy, stiffness = Spring.StiffnessLow),
        label = "successScale"
    )

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text(
            text = "🎉",
            fontSize = 100.sp,
            modifier = Modifier.scale(successScale)
        )

        Spacer(modifier = Modifier.height(16.dp))

        Text(
            text = "恭喜完成本难度！",
            fontSize = 32.sp,
            fontWeight = FontWeight.Bold,
            color = Color(0xFF5D4037)
        )

        Spacer(modifier = Modifier.height(24.dp))

        // 星星
        Row {
            repeat(3) { index ->
                val starScale by animateFloatAsState(
                    targetValue = if (index < state.stars) 1.3f else 1f,
                    animationSpec = tween(300, delayMillis = index * 150),
                    label = "star$index"
                )
                Text(
                    text = if (index < state.stars) "⭐" else "☆",
                    fontSize = 56.sp,
                    modifier = Modifier.scale(starScale)
                )
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        Text(
            text = "得分: ${state.score}",
            fontSize = 26.sp,
            fontWeight = FontWeight.Bold,
            color = Color(0xFFFF6B6B)
        )

        Text(
            text = "${module.getDifficultyName()} · 第${module.getLevelIndex()}关",
            fontSize = 20.sp,
            color = Color(0xFF8D6E63)
        )

        Spacer(modifier = Modifier.height(32.dp))

        // 重新开始按钮
        Button(
            onClick = onReplay,
            modifier = Modifier.fillMaxWidth().height(60.dp),
            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF4CAF50)),
            shape = RoundedCornerShape(30.dp)
        ) {
            Text("🔄 重新开始", fontSize = 20.sp, fontWeight = FontWeight.Bold, color = Color.White)
        }

        Spacer(modifier = Modifier.height(12.dp))

        // 返回按钮
        OutlinedButton(
            onClick = onBack,
            modifier = Modifier.fillMaxWidth().height(60.dp),
            shape = RoundedCornerShape(30.dp)
        ) {
            Text("🏠 返回选择难度", fontSize = 20.sp, color = Color(0xFF5D4037))
        }
    }
}

// ==================== 全部完成 ====================

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
        Text("🏆", fontSize = 120.sp)

        Spacer(modifier = Modifier.height(16.dp))

        Text(
            text = "🎊 全部通关！",
            fontSize = 36.sp,
            fontWeight = FontWeight.Bold,
            color = Color(0xFFFF6B6B)
        )

        Spacer(modifier = Modifier.height(24.dp))

        Text(
            text = "总得分: ${state.totalScore}",
            fontSize = 26.sp,
            fontWeight = FontWeight.Bold,
            color = Color(0xFF5D4037)
        )

        Text(
            text = "总用时: ${state.totalTime / 1000}秒",
            fontSize = 20.sp,
            color = Color(0xFF8D6E63)
        )

        Spacer(modifier = Modifier.height(32.dp))

        OutlinedButton(
            onClick = onBack,
            modifier = Modifier.fillMaxWidth().height(60.dp),
            shape = RoundedCornerShape(30.dp)
        ) {
            Text("🏠 返回", fontSize = 20.sp)
        }
    }
}