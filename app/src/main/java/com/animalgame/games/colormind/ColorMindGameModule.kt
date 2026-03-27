package com.animalgame.games.colormind

import com.animalgame.core.game.AbstractGameModule
import com.animalgame.core.game.ActionResult
import com.animalgame.core.game.GameAction
import com.animalgame.core.game.GameState
import com.animalgame.core.manager.GameRegistry
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

/**
 * Color Mind（颜色识别训练）游戏模块
 * 实现 Stroop Effect 训练
 * 训练目标：颜色与文字冲突判断
 */
class ColorMindGameModule : AbstractGameModule() {

    override val gameId: String = "color_mind"
    override val gameName: String = "颜色识别"
    override val iconAsset: String = "logo1.png"
    override val totalLevels: Int = 40  // 4个难度 × 10关
    override val description: String = "训练颜色识别与注意力"

    // 难度配置
    enum class Difficulty(
        val levelCount: Int,
        val colorCount: Int,
        val questionCount: Int,
        val timePerQuestion: Long,  // 毫秒
        val displayName: String
    ) {
        EASY(10, 2, 10, 5000, "简单"),      // 红、蓝，10题，每题5秒
        MEDIUM(10, 3, 15, 4000, "中等"),    // 红、蓝、绿，15题，每题4秒
        HARD(10, 4, 20, 3000, "困难"),     // 红、蓝、绿、黄，20题，每题3秒
        EXPERT(10, 5, 25, 2500, "挑战")    // 五色，25题，每题2.5秒
    }

    // 颜色定义（文字 -> 颜色值）
    enum class ColorName(val displayName: String, val colorValue: Long) {
        RED("红", 0xFFE53935),
        BLUE("蓝", 0xFF1E88E5),
        GREEN("绿", 0xFF43A047),
        YELLOW("黄", 0xFFFDD835),
        PURPLE("紫", 0xFF8E24AA)
    }

    // 目标类型
    enum class TargetType {
        TEXT,   // 判断文字内容
        COLOR   // 判断字体颜色
    }

    // 当前题目数据
    data class Question(
        val textColorName: ColorName,    // 文字内容（如"红"）
        val displayColor: ColorName,     // 字体颜色（如蓝色）
        val targetType: TargetType,      // 当前规则
        val options: List<ColorName>     // 颜色选项（包含正确答案和干扰项）
    ) {
        // 根据规则获取正确答案
        fun getCorrectAnswer(): ColorName {
            return if (targetType == TargetType.TEXT) {
                textColorName    // 规则是判断文字，选文字内容
            } else {
                displayColor    // 规则是判断颜色，选字体颜色
            }
        }

        // 获取规则提示文本
        fun getRuleText(): String {
            return if (targetType == TargetType.TEXT) "判断文字内容" else "判断字体颜色"
        }
    }

    // 当前难度
    private var currentDifficulty = Difficulty.EASY

    // 当前难度内的关卡索引 (0-9)
    private var levelIndex = 0

    // 游戏数据
    private var questions = listOf<Question>()
    private var currentQuestionIndex = 0
    private var correctCount = 0
    private var questionStartTime = 0L

    // 计时器
    private var questionTimerJob: kotlinx.coroutines.Job? = null

    // 根据 level (1-10) 在当前难度内设置关卡
    private fun setLevel(level: Int) {
        // level 是当前难度内的关卡号 (1-10)
        levelIndex = (level - 1).coerceIn(0, currentDifficulty.levelCount - 1)
    }

    // 获取当前完整关卡号（仅用于显示）
    private fun getFullLevel(): Int {
        return levelIndex + 1
    }

    // 获取当前难度可用的颜色列表
    private fun getAvailableColors(): List<ColorName> {
        return ColorName.entries.take(currentDifficulty.colorCount)
    }

    // 设置难度（由 UI 调用）
    fun setDifficulty(difficulty: Difficulty) {
        currentDifficulty = difficulty
        levelIndex = 0  // 重置到第一关
    }

    // 获取当前难度
    fun getCurrentDifficulty(): Difficulty = currentDifficulty

    // 生成选项（包含正确答案 + 干扰项）
    private fun generateOptions(correctAnswer: ColorName, availableColors: List<ColorName>): List<ColorName> {
        val options = mutableListOf(correctAnswer)
        val otherColors = availableColors.filter { it != correctAnswer }.shuffled()

        // 添加干扰项，选项数量 = min(4, 可用颜色数量)
        val optionCount = minOf(4, availableColors.size)
        options.addAll(otherColors.take(optionCount - 1))

        return options.shuffled()
    }

    // 生成题目
    private fun generateQuestions(): List<Question> {
        val colors = getAvailableColors()
        return List(currentDifficulty.questionCount) {
            val textColor = colors.random()
            val displayColor = colors.random()
            val targetType = if (Math.random() < 0.5) TargetType.TEXT else TargetType.COLOR

            // 根据规则确定正确答案
            val correctAnswer = if (targetType == TargetType.TEXT) textColor else displayColor

            // 生成选项
            val options = generateOptions(correctAnswer, colors)

            Question(textColor, displayColor, targetType, options)
        }
    }

    /**
     * 覆盖 start 方法，直接开始游戏（无倒计时）
     */
    override fun start(level: Int) {
        setLevel(level)
        currentScore = 0
        mistakeCount = 0

        // 直接开始游戏
        startGame()
    }

    // 公开方法供 UI 调用
    fun nextLevel() {
        if (levelIndex < currentDifficulty.levelCount - 1) {
            levelIndex++
            start(levelIndex + 1)
        }
    }

    fun restartCurrentLevel() {
        start(levelIndex + 1)
    }

    // 返回到关卡选择页面
    fun resetToIdle() {
        questionTimerJob?.cancel()
        stopTimer()
        _state.value = GameState.Idle
    }

    // 获取当前难度名称
    fun getCurrentDifficultyName(): String = currentDifficulty.displayName

    // 获取当前难度内的关卡号 (1-10)
    fun getCurrentLevelIndex(): Int = levelIndex + 1

    // 检查是否已完成当前难度
    fun isDifficultyCompleted(): Boolean {
        return levelIndex >= currentDifficulty.levelCount - 1
    }

    // 获取当前题目选项（供 UI 使用）
    fun getCurrentOptions(): List<ColorName> {
        return questions.getOrNull(currentQuestionIndex)?.options ?: emptyList()
    }

    /**
     * 开始游戏
     */
    override fun startGame() {
        // 生成题目
        questions = generateQuestions()
        currentQuestionIndex = 0
        correctCount = 0

        // 显示第一题
        showQuestion()
    }

    /**
     * 显示当前题目
     */
    private fun showQuestion() {
        if (currentQuestionIndex >= questions.size) {
            // 题目完成
            completeGame()
            return
        }

        val question = questions[currentQuestionIndex]
        questionStartTime = System.currentTimeMillis()

        // 进入游戏状态
        _state.value = GameState.Playing(
            level = getFullLevel(),
            elapsedTime = System.currentTimeMillis() - startTime,
            score = currentScore,
            data = mapOf(
                "questionIndex" to currentQuestionIndex,
                "totalQuestions" to questions.size,
                "textColorName" to question.textColorName.displayName,
                "displayColorValue" to question.displayColor.colorValue,
                "targetType" to question.targetType.name,
                "ruleText" to question.getRuleText(),
                "options" to question.options.map { it.name },
                "isTimeout" to false
            )
        )

        // 启动题目计时器
        startQuestionTimer()
    }

    /**
     * 启动题目计时器
     */
    private fun startQuestionTimer() {
        questionTimerJob?.cancel()
        questionTimerJob = gameScope.launch {
            delay(currentDifficulty.timePerQuestion)
            // 超时，未答题
            handleTimeout()
        }
    }

    /**
     * 处理超时
     */
    private fun handleTimeout() {
        mistakeCount++
        // 显示超时状态
        val question = questions[currentQuestionIndex]
        _state.value = GameState.Playing(
            level = getFullLevel(),
            elapsedTime = System.currentTimeMillis() - startTime,
            score = currentScore,
            data = mapOf(
                "questionIndex" to currentQuestionIndex,
                "totalQuestions" to questions.size,
                "textColorName" to question.textColorName.displayName,
                "displayColorValue" to question.displayColor.colorValue,
                "targetType" to question.targetType.name,
                "ruleText" to question.getRuleText(),
                "options" to question.options.map { it.name },
                "isTimeout" to true
            )
        )

        // 延迟后进入下一题
        gameScope.launch {
            delay(500)
            currentQuestionIndex++
            showQuestion()
        }
    }

    /**
     * 处理用户操作
     */
    override fun onUserAction(action: GameAction): ActionResult? {
        when (action) {
            is GameAction.ColorMindAnswer -> {
                return handleAnswer(action.selectedColor)
            }
            else -> return super.onUserAction(action)
        }
    }

    /**
     * 处理玩家回答
     * @param selectedColor 玩家选择的颜色
     */
    private fun handleAnswer(selectedColor: String): ActionResult? {
        val currentState = _state.value
        if (currentState !is GameState.Playing) return null

        // 取消计时器
        questionTimerJob?.cancel()

        val question = questions[currentQuestionIndex]
        val selectedColorName = ColorName.entries.find { it.name == selectedColor }
        val correctAnswer = question.getCorrectAnswer()

        val isCorrect = selectedColorName == correctAnswer

        if (isCorrect) {
            // 正确
            correctCount++
            currentScore += 10

            // 计算奖励分（根据答题速度）
            val timeUsed = System.currentTimeMillis() - questionStartTime
            val timeBonus = maxOf(0, (currentDifficulty.timePerQuestion - timeUsed) / 100).toInt()
            currentScore += timeBonus

            updateState()
            return ActionResult.Success
        } else {
            // 错误
            mistakeCount++
            updateState()
            return ActionResult.Error("回答错误", shake = true)
        }
    }

    /**
     * 更新状态并进入下一题
     */
    private fun updateState() {
        currentQuestionIndex++

        if (currentQuestionIndex >= questions.size) {
            // 全部完成
            gameScope.launch {
                delay(500)
                completeGame()
            }
        } else {
            // 显示下一题
            gameScope.launch {
                delay(300)  // 短暂延迟让玩家看到结果
                showQuestion()
            }
        }
    }

    /**
     * 完成游戏
     */
    private fun completeGame() {
        questionTimerJob?.cancel()
        val timeMillis = stopTimer()
        val stars = calculateStars(timeMillis, mistakeCount, getFullLevel())

        // 计算正确率
        val accuracy = if (questions.isNotEmpty()) {
            correctCount.toFloat() / questions.size
        } else 0f

        // 使用基类方法完成关卡
        completeLevel(
            isSuccess = true,
            timeMillis = timeMillis,
            score = currentScore,
            stars = stars
        )
    }

    /**
     * 计算星级
     * - 正确率 >= 90% → 3星
     * - 正确率 >= 70% → 2星
     * - 正确率 >= 50% → 1星
     */
    override fun calculateStars(timeMillis: Long, mistakes: Int, level: Int): Int {
        val accuracy = if (questions.isNotEmpty()) {
            correctCount.toFloat() / questions.size
        } else 0f

        return when {
            accuracy >= 0.9f -> 3
            accuracy >= 0.7f -> 2
            accuracy >= 0.5f -> 1
            else -> 1
        }
    }

    /**
     * 销毁游戏
     */
    override fun destroy() {
        questionTimerJob?.cancel()
        super.destroy()
    }
}

/**
 * 注册 Color Mind 游戏
 */
fun registerColorMindGame() {
    GameRegistry.register(ColorMindGameModule())
}
