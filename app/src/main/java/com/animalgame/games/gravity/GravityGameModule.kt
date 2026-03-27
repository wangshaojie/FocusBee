package com.animalgame.games.gravity

import android.content.Context
import com.animalgame.core.game.AbstractGameModule
import com.animalgame.core.game.ActionResult
import com.animalgame.core.game.GameAction
import com.animalgame.core.game.GameState
import com.animalgame.core.manager.GameRegistry
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

/**
 * 平衡小球游戏模块
 * 实现重力感应控制小球移动到终点的游戏
 */
class GravityGameModule : AbstractGameModule() {

    override val gameId: String = "gravity"
    override val gameName: String = "平衡小球"
    override val iconAsset: String = "logo1.png"
    override val totalLevels: Int = 40  // 4个难度 × 10关
    override val description: String = "倾斜手机控制小球到达终点"

    // 当前难度
    private var currentDifficulty = Difficulty.EASY

    // 当前难度内的关卡索引 (0-9)
    private var levelIndex = 0

    // 游戏数据
    private var currentLevelConfig: LevelConfig? = null
    private var gameStartTime = 0L

    // 设置难度（由 UI 调用）
    fun setDifficulty(difficulty: Difficulty) {
        currentDifficulty = difficulty
        levelIndex = 0
    }

    // 获取当前难度
    fun getCurrentDifficulty(): Difficulty = currentDifficulty

    // 获取当前难度名称
    fun getCurrentDifficultyName(): String = currentDifficulty.displayName

    // 获取当前难度内的关卡号 (1-10)
    fun getCurrentLevelIndex(): Int = levelIndex + 1

    // 检查是否已完成当前难度
    fun isDifficultyCompleted(): Boolean {
        return levelIndex >= currentDifficulty.levelCount - 1
    }

    // 获取当前关卡配置
    fun getCurrentLevelConfig(): LevelConfig? = currentLevelConfig

    // 根据 level (1-10) 在当前难度内设置关卡
    private fun setLevel(level: Int) {
        levelIndex = (level - 1).coerceIn(0, currentDifficulty.levelCount - 1)
        currentLevelConfig = LevelManager.getLevel(currentDifficulty, levelIndex)
    }

    // 获取当前完整关卡号
    private fun getFullLevel(): Int {
        return levelIndex + 1
    }

    /**
     * 覆盖 start 方法，直接开始游戏（无倒计时）
     */
    override fun start(level: Int) {
        setLevel(level)
        currentScore = 0
        mistakeCount = 0
        gameStartTime = System.currentTimeMillis()

        // 直接开始游戏
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
                "mistakes" to 0,
                "timeLimit" to (currentLevelConfig?.timeLimit ?: 20000L)
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
     * 处理游戏事件
     */
    fun onGameEvent(event: GameSurfaceEvent) {
        when (event) {
            is GameSurfaceEvent.Success -> {
                val timeMillis = event.timeMillis
                val stars = calculateStars(timeMillis, mistakeCount, getFullLevel())
                currentScore = stars * 10  // 1星=10分

                completeLevel(
                    isSuccess = true,
                    timeMillis = timeMillis,
                    score = currentScore,
                    stars = stars
                )
            }

            is GameSurfaceEvent.Failed -> {
                mistakeCount++
                _state.value = GameState.Playing(
                    level = getFullLevel(),
                    elapsedTime = System.currentTimeMillis() - gameStartTime,
                    score = currentScore,
                    data = mapOf(
                        "difficulty" to currentDifficulty.displayName,
                        "levelInDifficulty" to (levelIndex + 1),
                        "mistakes" to mistakeCount,
                        "timeLimit" to (currentLevelConfig?.timeLimit ?: 20000L)
                    )
                )
            }

            is GameSurfaceEvent.GameOver -> {
                val timeMillis = event.timeMillis
                val stars = 1  // 失败也算1星

                completeLevel(
                    isSuccess = false,
                    timeMillis = timeMillis,
                    score = 0,
                    stars = stars
                )
            }
        }
    }

    /**
     * 计算星级
     * - 3星：用时 < 50% 时间限制
     * - 2星：用时 < 80% 时间限制
     * - 1星：完成
     */
    override fun calculateStars(timeMillis: Long, mistakes: Int, level: Int): Int {
        val timeLimit = currentLevelConfig?.timeLimit ?: 20000L

        return when {
            timeMillis < timeLimit * 0.5f -> 3
            timeMillis < timeLimit * 0.8f -> 2
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
 * 游戏表面事件
 */
sealed class GameSurfaceEvent {
    data class Success(val timeMillis: Long) : GameSurfaceEvent()
    data class Failed(val message: String) : GameSurfaceEvent()
    data class GameOver(val timeMillis: Long) : GameSurfaceEvent()
}

/**
 * 注册平衡小球游戏
 */
fun registerGravityGame() {
    GameRegistry.register(GravityGameModule())
}
