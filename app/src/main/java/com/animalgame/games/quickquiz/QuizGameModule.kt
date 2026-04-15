package com.animalgame.games.quickquiz

import android.content.Context
import com.animalgame.core.game.AbstractGameModule
import com.animalgame.core.game.ActionResult
import com.animalgame.core.game.GameAction
import com.animalgame.core.game.GameState
import com.animalgame.core.manager.GameRegistry
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject
import kotlin.random.Random

// ==================== 题目状态 ====================

enum class QuizPhase {
    SHOW_QUESTION,   // 显示题目
    SHOW_ANSWER     // 显示答案
}

// ==================== 游戏难度 ====================

enum class QuizDifficulty(val displayName: String, val jsonName: String) {
    EASY("简单", "简单"),
    MEDIUM("进阶", "中等"),
    HARD("挑战", "困难"),
    EXTREME("极限", "挑战")
}

// ==================== 题目数据类 ====================

data class Question(
    val id: Int,
    val difficulty: String,
    val question: String,
    val options: List<String>,
    val answer: String,  // 答案字母如 "B"
    val topic: String
) {
    /**
     * 根据答案字母获取答案选项内容
     * 例如 answer="C", options=["A. 地球", "B. 火星", "C. 木星", "D. 金星"]
     * 返回 "木星"
     */
    fun getAnswerContent(): String {
        val answerPrefix = "$answer."
        val answerOption = options.find { it.startsWith(answerPrefix) }
        return answerOption?.substringAfter(". ") ?: ""
    }
}

// ==================== 题目管理器 ====================

class QuestionManager {
    private var allQuestions: List<Question> = emptyList()
    private var currentQuestions: List<Question> = emptyList()
    private var currentIndex: Int = 0
    private var currentDifficulty: QuizDifficulty = QuizDifficulty.EASY

    /**
     * 从 JSON 文件加载题库
     */
    suspend fun loadQuestions(context: Context) = withContext(Dispatchers.IO) {
        try {
            // 从 assets 读取 JSON 文件
            val jsonString = context.assets.open("儿童百科知识题库400题.json")
                .bufferedReader()
                .use { it.readText() }

            val jsonObject = JSONObject(jsonString)
            val questionsArray = jsonObject.getJSONArray("questions")

            val questions = mutableListOf<Question>()
            for (i in 0 until questionsArray.length()) {
                val q = questionsArray.getJSONObject(i)
                questions.add(
                    Question(
                        id = q.getInt("id"),
                        difficulty = q.getString("difficulty"),
                        question = q.getString("q"),
                        options = q.getJSONArray("o").let { arr ->
                            (0 until arr.length()).map { arr.getString(it) }
                        },
                        answer = q.getString("a"),
                        topic = q.getString("t")
                    )
                )
            }
            allQuestions = questions
        } catch (e: Exception) {
            e.printStackTrace()
            allQuestions = emptyList()
        }
    }

    /**
     * 按难度加载题目（随机洗牌）
     */
    fun loadByDifficulty(difficulty: QuizDifficulty) {
        currentDifficulty = difficulty
        currentQuestions = allQuestions
            .filter { it.difficulty == difficulty.jsonName }
            .shuffled(Random)
        currentIndex = 0
    }

    /**
     * 获取当前题目
     */
    fun getCurrentQuestion(): Question? {
        return currentQuestions.getOrNull(currentIndex)
    }

    /**
     * 获取下一题
     */
    fun getNextQuestion(): Question? {
        if (currentIndex < currentQuestions.size - 1) {
            currentIndex++
            return getCurrentQuestion()
        }
        return null
    }

    /**
     * 是否还有下一题
     */
    fun hasNextQuestion(): Boolean {
        return currentIndex < currentQuestions.size - 1
    }

    /**
     * 获取当前进度 (1-based)
     */
    fun getCurrentProgress(): Int = currentIndex + 1

    /**
     * 获取总题数
     */
    fun getTotalQuestions(): Int = currentQuestions.size

    /**
     * 获取当前难度
     */
    fun getCurrentDifficulty(): QuizDifficulty = currentDifficulty

    /**
     * 重置当前难度题目（重新洗牌）
     */
    fun resetCurrentDifficulty() {
        loadByDifficulty(currentDifficulty)
    }

    /**
     * 是否完成当前难度全部题目
     */
    fun isCompleted(): Boolean = currentIndex >= currentQuestions.size - 1 && currentQuestions.isNotEmpty()
}

// ==================== 游戏模块 ====================

class QuizGameModule : AbstractGameModule() {

    override val gameId: String = "quick_quiz"
    override val gameName: String = "抢答题"
    override val iconAsset: String = "quiz_icon.png"
    override val totalLevels: Int = 400  // 4难度 x 100题
    override val description: String = "儿童百科知识问答"

    // 题目管理器
    private val questionManager = QuestionManager()

    // 当前游戏阶段
    private var currentPhase = QuizPhase.SHOW_QUESTION

    // 是否已加载题库
    private var isQuestionsLoaded = false

    // 当前难度
    private var currentDifficulty = QuizDifficulty.EASY

    companion object {
        const val QUESTIONS_PER_DIFFICULTY = 100

        /**
         * 根据关卡ID解析难度和关卡索引
         */
        fun parseLevelId(level: Int): Pair<QuizDifficulty, Int> {
            return when {
                level in 1..100 -> QuizDifficulty.EASY to level
                level in 101..200 -> QuizDifficulty.MEDIUM to (level - 100)
                level in 201..300 -> QuizDifficulty.HARD to (level - 200)
                else -> QuizDifficulty.EXTREME to (level - 300)
            }
        }
    }

    /**
     * 异步加载题库
     */
    suspend fun loadQuestions(context: android.content.Context) {
        if (!isQuestionsLoaded) {
            questionManager.loadQuestions(context)
            isQuestionsLoaded = true
        }
    }

    override fun start(level: Int) {
        // 如果题库未加载，等待一下（启动协程加载）
        if (!isQuestionsLoaded) {
            // 进入等待加载状态
            _state.value = GameState.Idle
            return
        }

        currentLevel = level.coerceIn(1, totalLevels)
        currentScore = 0
        mistakeCount = 0

        // 解析难度
        val (difficulty, _) = parseLevelId(currentLevel)
        currentDifficulty = difficulty

        // 加载该难度的题目
        questionManager.loadByDifficulty(difficulty)

        // 重置阶段
        currentPhase = QuizPhase.SHOW_QUESTION

        // 直接开始游戏
        startGame()
    }

    override fun startGame() {
        updatePlayingState()
    }

    override fun onUserAction(action: GameAction): ActionResult? {
        return when (action) {
            is GameAction.TapIndex -> {
                // 点击"查看答案"或"下一题"按钮
                handleButtonClick()
                ActionResult.Success
            }
            else -> super.onUserAction(action)
        }
    }

    /**
     * 处理按钮点击
     */
    fun handleButtonClick() {
        when (currentPhase) {
            QuizPhase.SHOW_QUESTION -> {
                // 切换到显示答案
                currentPhase = QuizPhase.SHOW_ANSWER
                updatePlayingState()
            }
            QuizPhase.SHOW_ANSWER -> {
                // 点击"下一题"
                if (questionManager.hasNextQuestion()) {
                    questionManager.getNextQuestion()
                    currentPhase = QuizPhase.SHOW_QUESTION
                    currentScore += 10  // 每答一题得10分
                    updatePlayingState()
                } else {
                    // 完成当前难度
                    handleCompletion()
                }
            }
        }
    }

    /**
     * 处理完成
     */
    private fun handleCompletion() {
        val timeUsed = stopTimer()
        val stars = calculateStars(timeUsed, mistakeCount, currentLevel)

        completeLevel(
            isSuccess = true,
            timeMillis = timeUsed,
            score = currentScore,
            stars = stars
        )
    }

    /**
     * 重玩当前难度
     */
    fun replayDifficulty() {
        questionManager.resetCurrentDifficulty()
        currentPhase = QuizPhase.SHOW_QUESTION
        currentScore = 0
        mistakeCount = 0
        updatePlayingState()
    }

    /**
     * 获取当前难度名称
     */
    fun getDifficultyName(): String = currentDifficulty.displayName

    /**
     * 获取关卡索引 (1-100)
     */
    fun getLevelIndex(): Int = ((currentLevel - 1) % QUESTIONS_PER_DIFFICULTY) + 1

    /**
     * 重置到空闲状态
     */
    fun resetToIdle() {
        stopTimer()
        _state.value = GameState.Idle
    }

    override fun destroy() {
        super.destroy()
    }

    // ==================== 状态更新 ====================

    private fun updatePlayingState() {
        _state.value = GameState.Playing(
            level = currentLevel,
            elapsedTime = System.currentTimeMillis() - startTime,
            score = currentScore,
            data = buildGameData()
        )
    }

    private fun buildGameData(): Map<String, Any> {
        val question = questionManager.getCurrentQuestion()
        return mapOf(
            "currentPhase" to currentPhase.name,
            "question" to (question?.question ?: ""),
            "answer" to (question?.answer ?: ""),
            "answerContent" to (question?.getAnswerContent() ?: ""),
            "topic" to (question?.topic ?: ""),
            "currentProgress" to questionManager.getCurrentProgress(),
            "totalQuestions" to questionManager.getTotalQuestions(),
            "difficulty" to currentDifficulty.name,
            "isCompleted" to questionManager.isCompleted(),
            "levelId" to "${currentDifficulty.displayName}_${getLevelIndex()}"
        )
    }

    // ==================== 星级计算 ====================

    override fun calculateStars(timeMillis: Long, mistakes: Int, level: Int): Int {
        return when {
            mistakes == 0 -> 3
            mistakes <= 3 -> 2
            else -> 1
        }
    }

    // ==================== 公共方法 ====================

    fun getCurrentPhase(): QuizPhase = currentPhase

    fun getCurrentQuestion(): Question? = questionManager.getCurrentQuestion()

    fun getProgress(): Pair<Int, Int> = questionManager.getCurrentProgress() to questionManager.getTotalQuestions()
}

// ==================== 注册 ====================

fun registerQuickQuizGame() {
    GameRegistry.register(QuizGameModule())
}