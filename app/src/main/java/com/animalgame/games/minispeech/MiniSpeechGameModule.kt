package com.animalgame.games.minispeech

import android.content.Context
import com.animalgame.core.game.AbstractGameModule
import com.animalgame.core.game.ActionResult
import com.animalgame.core.game.GameAction
import com.animalgame.core.game.GameState
import com.animalgame.core.manager.GameRegistry
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.json.JSONObject
import kotlin.random.Random

/**
 * 30秒小演说（Mini Speech Rush）游戏模块
 * 玩法：App提供口才主题，孩子进行30秒表达训练
 */

// ==================== 游戏阶段 ====================

enum class SpeechPhase {
    IDLE,       // 初始状态
    COUNTDOWN,  // 3秒准备倒计时
    PLAYING,    // 30秒演说中
    FINISH      // 结束
}

// ==================== 主题类型 ====================

enum class SpeechCategory(val displayName: String, val jsonName: String) {
    STORY("讲故事类", "讲故事类"),
    OPINION("观点表达类", "观点表达类"),
    IMAGINE("想象力类", "想象力类"),
    LIFE("生活表达类", "生活表达类"),
    RANDOM("随机", "随机")
}

// ==================== 题目数据类 ====================

data class SpeechTopic(
    val id: Int,
    val category: String,
    val question: String,
    val hint: String
)

// ==================== 题目管理器 ====================

object TopicManager {
    private var allTopics = listOf<SpeechTopic>()
    private var currentCategory: SpeechCategory = SpeechCategory.RANDOM
    private var currentIndex: Int = -1
    private var lastTopicId: Int = -1

    /**
     * 从 JSON 文件加载题库
     */
    suspend fun loadTopics(context: Context) = withContext(Dispatchers.IO) {
        try {
            val jsonString = context.assets.open("少儿口才训练题库100题.json")
                .bufferedReader()
                .use { it.readText() }

            val jsonObject = JSONObject(jsonString)
            val questionsArray = jsonObject.getJSONArray("questions")

            val topics = mutableListOf<SpeechTopic>()
            for (i in 0 until questionsArray.length()) {
                val q = questionsArray.getJSONObject(i)
                topics.add(
                    SpeechTopic(
                        id = q.getInt("id"),
                        category = q.getString("category"),
                        question = q.getString("question"),
                        hint = q.optString("hint", "")
                    )
                )
            }
            allTopics = topics
        } catch (e: Exception) {
            e.printStackTrace()
            allTopics = emptyList()
        }
    }

    /**
     * 设置当前类别
     */
    fun setCategory(category: SpeechCategory) {
        currentCategory = category
        currentIndex = -1
    }

    /**
     * 获取随机主题
     */
    fun getRandomTopic(): SpeechTopic {
        val filteredTopics = if (currentCategory == SpeechCategory.RANDOM) {
            allTopics
        } else {
            allTopics.filter { it.category == currentCategory.jsonName }
        }

        if (filteredTopics.isEmpty()) {
            return SpeechTopic(0, "随机", "请简单介绍一下自己", "")
        }

        // 避免短时间重复
        var newIndex: Int
        do {
            newIndex = Random.nextInt(filteredTopics.size)
        } while (filteredTopics.size > 1 && filteredTopics[newIndex].id == lastTopicId)

        lastTopicId = filteredTopics[newIndex].id
        return filteredTopics[newIndex]
    }

    /**
     * 获取当前类别
     */
    fun getCurrentCategory(): SpeechCategory = currentCategory

    /**
     * 获取题目总数
     */
    fun getTotalCount(): Int = allTopics.size

    /**
     * 重置
     */
    fun reset() {
        currentIndex = -1
        lastTopicId = -1
    }
}

// ==================== 游戏模块 ====================

class MiniSpeechGameModule : AbstractGameModule() {

    override val gameId: String = "mini_speech"
    override val gameName: String = "30秒小演说"
    override val iconAsset: String = "minispeech_icon.png"
    override val totalLevels: Int = 100
    override val description: String = "30秒口才训练"

    // 游戏阶段
    private var currentPhase = SpeechPhase.IDLE

    // 当前主题
    private var currentTopic: SpeechTopic? = null

    // 倒计时数值
    private var countdownValue: Int = 3

    // 演说剩余时间（毫秒）
    private var speechTimeRemaining: Long = 30000L

    // 计时器
    private var speechTimerJob: kotlinx.coroutines.Job? = null

    // 是否已加载题库
    private var isLoaded = false

    companion object {
        const val COUNTDOWN_DURATION = 3 // 3秒准备
        const val SPEECH_DURATION = 30000L // 30秒演说
    }

    /**
     * 异步加载题库
     */
    suspend fun loadTopics(context: android.content.Context) {
        if (!isLoaded) {
            TopicManager.loadTopics(context)
            isLoaded = true
        }
    }

    /**
     * 检查是否已加载
     */
    fun isTopicsLoaded(): Boolean = isLoaded

    /**
     * 设置类别并开始
     */
    fun startWithCategory(category: SpeechCategory) {
        TopicManager.setCategory(category)
        resetGame()
    }

    override fun start(level: Int) {
        currentLevel = level.coerceIn(1, totalLevels)
        currentScore = 0
        mistakeCount = 0
        currentPhase = SpeechPhase.IDLE
        currentTopic = TopicManager.getRandomTopic()
        countdownValue = COUNTDOWN_DURATION
        speechTimeRemaining = SPEECH_DURATION
        startGame()
    }

    override fun startGame() {
        updatePlayingState()
    }

    private fun resetGame() {
        currentPhase = SpeechPhase.IDLE
        currentTopic = TopicManager.getRandomTopic()
        countdownValue = COUNTDOWN_DURATION
        speechTimeRemaining = SPEECH_DURATION
        updatePlayingState()
    }

    /**
     * 开始3秒倒计时
     */
    fun startCountdown() {
        currentPhase = SpeechPhase.COUNTDOWN
        countdownValue = COUNTDOWN_DURATION
        updatePlayingState()

        speechTimerJob?.cancel()
        speechTimerJob = gameScope.launch {
            for (i in COUNTDOWN_DURATION downTo 1) {
                countdownValue = i
                updatePlayingState()
                delay(1000)
            }
            // 倒计时结束，开始演说
            startSpeech()
        }
    }

    /**
     * 开始30秒演说
     */
    private fun startSpeech() {
        currentPhase = SpeechPhase.PLAYING
        speechTimeRemaining = SPEECH_DURATION
        updatePlayingState()

        speechTimerJob?.cancel()
        speechTimerJob = gameScope.launch {
            val startTime = System.currentTimeMillis()
            while (speechTimeRemaining > 0) {
                delay(100)
                speechTimeRemaining = SPEECH_DURATION - (System.currentTimeMillis() - startTime)
                if (speechTimeRemaining <= 0) {
                    speechTimeRemaining = 0
                    endSpeech()
                    break
                }
                updatePlayingState()
            }
        }
    }

    /**
     * 结束演说
     */
    private fun endSpeech() {
        speechTimerJob?.cancel()
        currentPhase = SpeechPhase.FINISH
        updatePlayingState()
    }

    /**
     * 再来一个（同一类别）
     */
    fun nextTopic() {
        currentTopic = TopicManager.getRandomTopic()
        currentPhase = SpeechPhase.IDLE
        countdownValue = COUNTDOWN_DURATION
        speechTimeRemaining = SPEECH_DURATION
        updatePlayingState()
    }

    /**
     * 重新开始（同一主题）
     */
    fun restartSpeech() {
        countdownValue = COUNTDOWN_DURATION
        speechTimeRemaining = SPEECH_DURATION
        currentPhase = SpeechPhase.IDLE
        updatePlayingState()
    }

    override fun onUserAction(action: GameAction): ActionResult? {
        return when (action) {
            is GameAction.TapIndex -> {
                when (currentPhase) {
                    SpeechPhase.IDLE -> {
                        startCountdown()
                    }
                    else -> {}
                }
                ActionResult.Success
            }
            else -> super.onUserAction(action)
        }
    }

    // ==================== 公共方法 ====================

    fun getCurrentPhase(): SpeechPhase = currentPhase

    fun getCurrentTopic(): SpeechTopic? = currentTopic

    fun getCountdownValue(): Int = countdownValue

    fun getSpeechTimeRemaining(): Long = speechTimeRemaining

    fun getSpeechTimeRemainingSeconds(): Int = (speechTimeRemaining / 1000).toInt()

    fun isLastFiveSeconds(): Boolean = speechTimeRemaining <= 5000 && speechTimeRemaining > 0

    fun getProgress(): Float = speechTimeRemaining.toFloat() / SPEECH_DURATION.toFloat()

    fun resetToIdle() {
        speechTimerJob?.cancel()
        _state.value = GameState.Idle
    }

    // ==================== 状态更新 ====================

    private fun updatePlayingState() {
        _state.value = GameState.Playing(
            level = currentLevel,
            elapsedTime = 0L,
            score = 0,
            data = buildGameData()
        )
    }

    private fun buildGameData(): Map<String, Any> {
        return mapOf(
            "phase" to currentPhase.name,
            "topic" to (currentTopic?.question ?: ""),
            "hint" to (currentTopic?.hint ?: ""),
            "category" to (currentTopic?.category ?: ""),
            "countdownValue" to countdownValue,
            "speechTimeRemaining" to speechTimeRemaining,
            "speechTimeSeconds" to getSpeechTimeRemainingSeconds(),
            "isLastFiveSeconds" to isLastFiveSeconds(),
            "progress" to getProgress()
        )
    }

    override fun destroy() {
        speechTimerJob?.cancel()
        super.destroy()
    }
}

// ==================== 注册 ====================

fun registerMiniSpeechGame() {
    GameRegistry.register(MiniSpeechGameModule())
}