package com.animalgame.games.schulte

import android.content.Context
import android.content.Intent
import com.animalgame.core.game.AbstractGameModule
import com.animalgame.core.game.ActionResult
import com.animalgame.core.game.GameAction
import com.animalgame.core.game.GameState
import com.animalgame.core.manager.GameRegistry

/**
 * 舒尔特训练游戏模块
 * 实现通用 GameModule 接口
 */
class SchulteGameModule : AbstractGameModule() {

    override val gameId: String = "schulte"
    override val gameName: String = "舒尔特训练"
    override val iconAsset: String = "logo1.png"
    override val totalLevels: Int = 200  // 4个难度 × 50关
    override val description: String = "按顺序点击数字"

    // 难度配置
    enum class Difficulty(val levelCount: Int, val gridSize: Int, val displayName: String) {
        EASY(50, 3, "简单"),
        MEDIUM(50, 4, "中等"),
        HARD(50, 5, "困难"),
        EXPERT(50, 6, "挑战")
    }

    // 当前难度（不随关卡变化）
    private var currentDifficulty = Difficulty.EASY

    // 当前难度内的关卡索引 (0-49)
    private var levelIndex = 0

    override fun createIntent(context: Context): Intent {
        return Intent(context, SchulteComposeActivity::class.java)
    }

    // 设置难度（由 UI 调用）
    fun setDifficulty(difficulty: Difficulty) {
        currentDifficulty = difficulty
        levelIndex = 0  // 重置到第一关
    }

    // 根据 level (1-50) 在当前难度内设置关卡
    private fun setLevel(level: Int) {
        // level 是当前难度内的关卡号 (1-50)
        levelIndex = (level - 1).coerceIn(0, currentDifficulty.levelCount - 1)
    }

    // 获取当前完整关卡号（仅用于显示）
    private fun getFullLevel(): Int {
        return levelIndex + 1
    }

    // 覆盖 start 方法，跳过倒计时，直接开始游戏
    override fun start(level: Int) {
        setLevel(level)
        currentScore = 0
        mistakeCount = 0

        // 直接开始游戏（跳过倒计时）
        startGame()
    }

    // 公开方法供 UI 调用
    fun nextLevel() {
        // 检查当前难度内是否还有下一关
        if (levelIndex < currentDifficulty.levelCount - 1) {
            // 在当前难度内继续
            levelIndex++
            startGame()
        }
        // 如果当前难度已全部完成，不自动跳转，让 UI 显示完成状态
    }

    fun restartCurrentLevel() {
        startGame()
    }

    // 返回到关卡选择页面
    fun resetToIdle() {
        stopTimer()
        _state.value = GameState.Idle
    }

    // 获取当前难度名称
    fun getCurrentDifficultyName(): String = currentDifficulty.displayName

    // 获取当前难度内的关卡号 (1-50)
    fun getCurrentLevelIndex(): Int = levelIndex + 1

    // 检查是否已完成当前难度
    fun isDifficultyCompleted(): Boolean {
        return levelIndex >= currentDifficulty.levelCount - 1
    }

    // 获取当前难度
    fun getCurrentDifficulty(): Difficulty = currentDifficulty

    override fun startGame() {
        // 使用当前难度的网格大小
        val gridSize = currentDifficulty.gridSize

        // 生成随机数字序列
        val totalNumbers = gridSize * gridSize
        val numbers = (1..totalNumbers).shuffled()

        // 舒尔特训练的具体实现
        _state.value = GameState.Playing(
            level = getFullLevel(),
            elapsedTime = 0L,
            score = 0,
            data = mapOf(
                "numbers" to numbers,
                "currentNumber" to 1,
                "mistakes" to 0,
                "gridSize" to gridSize,
                "clickedNumbers" to mutableMapOf<Int, Boolean>(),
                "wrongNumber" to -1,
                "difficulty" to currentDifficulty.displayName,
                "levelInDifficulty" to (levelIndex + 1)
            )
        )
        startTimer()
    }

    override fun onUserAction(action: GameAction): ActionResult? {
        val currentState = _state.value
        if (currentState !is GameState.Playing) return null

        when (action) {
            is GameAction.TapIndex -> {
                val tappedNumber = action.index + 1
                val currentNumber = currentState.data["currentNumber"] as? Int ?: 1
                val mistakes = currentState.data["mistakes"] as? Int ?: 0
                val gridSize = currentState.data["gridSize"] as? Int ?: 3
                val clickedNumbers = currentState.data["clickedNumbers"] as? MutableMap<Int, Boolean> ?: mutableMapOf()

                if (tappedNumber == currentNumber) {
                    // 正确点击
                    clickedNumbers[tappedNumber] = true

                    val nextNumber = currentNumber + 1
                    val totalNumbers = gridSize * gridSize

                    if (nextNumber > totalNumbers) {
                        // 游戏完成
                        stopTimer()
                        val stars = calculateStars(currentState.elapsedTime, mistakes, getFullLevel())
                        _state.value = GameState.Completed(
                            level = getFullLevel(),
                            score = currentState.score,
                            stars = stars,
                            timeMillis = currentState.elapsedTime,
                            isSuccess = true
                        )
                    } else {
                        // 继续游戏
                        _state.value = currentState.copy(
                            data = currentState.data.toMutableMap().apply {
                                put("currentNumber", nextNumber)
                                put("clickedNumbers", clickedNumbers)
                            }
                        )
                    }
                } else {
                    // 错误点击
                    clickedNumbers[tappedNumber] = false

                    val newMistakes = mistakes + 1
                    val newScore = maxOf(0, 100 - newMistakes * 10)
                    _state.value = currentState.copy(
                        score = newScore,
                        data = currentState.data.toMutableMap().apply {
                            put("mistakes", newMistakes)
                            put("score", newScore)
                            put("clickedNumbers", clickedNumbers)
                            put("wrongNumber", tappedNumber)
                        }
                    )
                }

                return ActionResult.Success
            }

            is GameAction.Restart -> {
                startGame()
                return ActionResult.Success
            }

            else -> return super.onUserAction(action)
        }
    }

    override fun calculateStars(timeMillis: Long, mistakes: Int, level: Int): Int {
        var stars = 1
        val gridSize = currentDifficulty.gridSize

        val expectedTime = gridSize * gridSize * 500L
        if (timeMillis < expectedTime) stars++

        if (mistakes == 0) stars++

        return minOf(stars, 3)
    }
}

/**
 * 注册舒尔特训练游戏
 */
fun registerSchulteGame() {
    GameRegistry.register(SchulteGameModule())
}