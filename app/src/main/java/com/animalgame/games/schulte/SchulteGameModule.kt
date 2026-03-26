package com.animalgame.games.schulte

import android.content.Context
import android.content.Intent
import com.animalgame.core.game.AbstractGameModule
import com.animalgame.core.game.ActionResult
import com.animalgame.core.game.GameAction
import com.animalgame.core.game.GameState
import com.animalgame.core.manager.GameRegistry
import kotlinx.coroutines.delay

/**
 * 舒尔特训练游戏模块
 * 实现通用 GameModule 接口
 */
class SchulteGameModule : AbstractGameModule() {

    override val gameId: String = "schulte"
    override val gameName: String = "舒尔特训练"
    override val iconAsset: String = "logo1.png"
    override val totalLevels: Int = 40
    override val description: String = "按顺序点击数字"

    override fun createIntent(context: Context): Intent {
        return Intent(context, SchulteComposeActivity::class.java)
    }

    // 覆盖 start 方法，跳过倒计时，直接开始游戏
    override fun start(level: Int) {
        currentLevel = level
        currentScore = 0
        mistakeCount = 0

        // 直接开始游戏（跳过倒计时）
        startGame()
    }

    // 公开方法供 UI 调用
    fun nextLevel() {
        if (currentLevel < totalLevels) {
            start(currentLevel + 1)
        }
    }

    fun restartCurrentLevel() {
        startGame()
    }

    // 返回到关卡选择页面
    fun resetToIdle() {
        stopTimer()
        _state.value = GameState.Idle
    }

    override fun startGame() {
        // 根据关卡计算网格大小
        val gridSize = when {
            currentLevel == 1 -> 3
            currentLevel == 2 -> 4
            currentLevel == 3 -> 5
            else -> 6
        }

        // 生成随机数字序列
        val totalNumbers = gridSize * gridSize
        val numbers = (1..totalNumbers).shuffled()

        // 舒尔特训练的具体实现
        _state.value = GameState.Playing(
            level = currentLevel,
            elapsedTime = 0L,
            score = 0,
            data = mapOf(
                "numbers" to numbers,
                "currentNumber" to 1,
                "mistakes" to 0,
                "gridSize" to gridSize,
                "clickedNumbers" to mutableMapOf<Int, Boolean>(),
                "wrongNumber" to -1  // 当前显示红色的错误数字
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
                    clickedNumbers[tappedNumber] = true  // true = correct

                    val nextNumber = currentNumber + 1
                    val totalNumbers = gridSize * gridSize

                    if (nextNumber > totalNumbers) {
                        // 游戏完成
                        stopTimer()
                        val stars = calculateStars(currentState.elapsedTime, mistakes, currentState.level)
                        _state.value = GameState.Completed(
                            level = currentState.level,
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
                    // 错误点击 - 记录错误但不在这里增加，让 UI 显示红色
                    // 将错误标记为 false
                    clickedNumbers[tappedNumber] = false

                    val newMistakes = mistakes + 1
                    val newScore = maxOf(0, 100 - newMistakes * 10)
                    _state.value = currentState.copy(
                        score = newScore,
                        data = currentState.data.toMutableMap().apply {
                            put("mistakes", newMistakes)
                            put("score", newScore)
                            put("clickedNumbers", clickedNumbers)
                            put("wrongNumber", tappedNumber)  // 记录错误点击的数字，UI 会显示红色
                        }
                    )
                }

                return ActionResult.Success
            }

            is GameAction.Restart -> {
                startGame()
                return ActionResult.Success
            }

            is GameAction.NextLevel -> {
                if (currentLevel < totalLevels) {
                    start(currentLevel + 1)
                }
                return ActionResult.Success
            }

            else -> return super.onUserAction(action)
        }
    }

    override fun calculateStars(timeMillis: Long, mistakes: Int, level: Int): Int {
        // 根据用时和错误次数计算星星
        var stars = 1
        val gridSize = when {
            level == 1 -> 3
            level == 2 -> 4
            level == 3 -> 5
            else -> 6
        }

        // 用时少于预期加星
        val expectedTime = gridSize * gridSize * 500L // 每格0.5秒预期
        if (timeMillis < expectedTime) stars++

        // 无错误加星
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
