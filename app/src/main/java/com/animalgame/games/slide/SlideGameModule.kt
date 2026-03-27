package com.animalgame.games.slide

import android.content.Context
import com.animalgame.core.game.AbstractGameModule
import com.animalgame.core.game.ActionResult
import com.animalgame.core.game.GameAction
import com.animalgame.core.game.GameState
import com.animalgame.core.manager.GameRegistry

/**
 * 方块推推乐游戏模块
 * 滑块拼图 - 逻辑训练游戏
 */
class SlideGameModule : AbstractGameModule() {

    override val gameId: String = "slide"
    override val gameName: String = "方块推推乐"
    override val iconAsset: String = "logo.png"
    override val totalLevels: Int = 30  // 3个难度 × 10关
    override val description: String = "滑动方块，按目标顺序排列"

    // 当前难度
    private var currentDifficulty = SlideDifficulty.EASY

    // 当前难度内的关卡索引 (0-9)
    private var levelIndex = 0

    // 游戏数据
    private var currentLevelConfig: SlideLevelConfig? = null
    private var stepCount = 0
    private var gameStartTime = 0L

    // 设置难度（由 UI 调用）
    fun setDifficulty(difficulty: SlideDifficulty) {
        currentDifficulty = difficulty
        levelIndex = 0
    }

    // 获取当前难度
    fun getCurrentDifficulty(): SlideDifficulty = currentDifficulty

    // 获取当前难度名称
    fun getCurrentDifficultyName(): String = currentDifficulty.displayName

    // 获取当前难度内的关卡号 (1-10)
    fun getCurrentLevelIndex(): Int = levelIndex + 1

    // 检查是否已完成当前难度
    fun isDifficultyCompleted(): Boolean {
        return levelIndex >= currentDifficulty.levelCount - 1
    }

    // 获取当前关卡配置
    fun getCurrentLevelConfig(): SlideLevelConfig? = currentLevelConfig

    // 根据 level (1-10) 在当前难度内设置关卡
    private fun setLevel(level: Int) {
        levelIndex = (level - 1).coerceIn(0, currentDifficulty.levelCount - 1)
        currentLevelConfig = SlideLevelManager.getLevel(currentDifficulty, levelIndex)
    }

    // 获取当前完整关卡号
    private fun getFullLevel(): Int {
        return levelIndex + 1
    }

    /**
     * 开始关卡
     */
    override fun start(level: Int) {
        setLevel(level)
        stepCount = 0
        currentScore = 0
        mistakeCount = 0
        gameStartTime = System.currentTimeMillis()

        // 直接开始游戏（无倒计时）
        startGame()
    }

    /**
     * 开始游戏
     */
    override fun startGame() {
        gameStartTime = System.currentTimeMillis()

        _state.value = GameState.Playing(
            level = getFullLevel(),
            elapsedTime = 0L,
            score = 0,
            data = mapOf(
                "difficulty" to currentDifficulty.displayName,
                "levelInDifficulty" to (levelIndex + 1),
                "steps" to 0,
                "targetSteps" to (currentLevelConfig?.optimalSteps ?: 10)
            )
        )
    }

    /**
     * 公开方法供 UI 调用 - 下一关
     */
    fun nextLevel() {
        if (levelIndex < currentDifficulty.levelCount - 1) {
            levelIndex++
            start(levelIndex + 1)
        }
    }

    /**
     * 重新开始当前关
     */
    fun restartCurrentLevel() {
        start(levelIndex + 1)
    }

    /**
     * 返回到关卡选择页面
     */
    fun resetToIdle() {
        stopTimer()
        _state.value = GameState.Idle
    }

    /**
     * 处理方块移动事件
     */
    fun onTileMoved() {
        stepCount++

        // 更新状态
        val elapsed = System.currentTimeMillis() - gameStartTime
        _state.value = GameState.Playing(
            level = getFullLevel(),
            elapsedTime = elapsed,
            score = currentScore,
            data = mapOf(
                "difficulty" to currentDifficulty.displayName,
                "levelInDifficulty" to (levelIndex + 1),
                "steps" to stepCount,
                "targetSteps" to (currentLevelConfig?.optimalSteps ?: 10)
            )
        )
    }

    /**
     * 处理获胜事件
     */
    fun onWin() {
        val elapsed = System.currentTimeMillis() - gameStartTime
        val stars = calculateStars(elapsed, 0, getFullLevel())
        currentScore = stars * 10

        completeLevel(
            isSuccess = true,
            timeMillis = elapsed,
            score = currentScore,
            stars = stars
        )
    }

    /**
     * 计算星级
     * - 3星：步数 <= 最优步数 + 2
     * - 2星：步数 <= 最优步数 + 5
     * - 1星：完成
     */
    override fun calculateStars(timeMillis: Long, mistakes: Int, level: Int): Int {
        val optimalSteps = currentLevelConfig?.optimalSteps ?: 10

        return when {
            stepCount <= optimalSteps + 2 -> 3
            stepCount <= optimalSteps + 5 -> 2
            else -> 1
        }
    }

    override fun onUserAction(action: GameAction): ActionResult? {
        return when (action) {
            is GameAction.Restart -> {
                restartCurrentLevel()
                ActionResult.Success
            }
            else -> super.onUserAction(action)
        }
    }

    /**
     * 销毁游戏
     */
    override fun destroy() {
        super.destroy()
    }
}

/**
 * 注册方块推推乐游戏
 */
fun registerSlideGame() {
    GameRegistry.register(SlideGameModule())
}