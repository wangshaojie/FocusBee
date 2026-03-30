package com.animalgame.games.lighthouse

import com.animalgame.core.game.AbstractGameModule
import com.animalgame.core.game.ActionResult
import com.animalgame.core.game.GameAction
import com.animalgame.core.game.GameState
import com.animalgame.core.manager.GameRegistry
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlin.random.Random

/**
 * 灯塔路径（Lighthouse Path）游戏模块
 * 玩法：记忆灯塔闪烁顺序，依次点击
 */

// ==================== 游戏阶段 ====================

enum class GamePhase {
    SHOWING_SEQUENCE,  // 显示序列中
    WAITING_INPUT,      // 等待玩家输入
    RESULT              // 结果
}

// ==================== 格子状态 ====================

enum class CellState {
    NORMAL,         // 普通状态
    HIGHLIGHTED,    // 高亮（序列播放时）
    CORRECT,        // 正确点击
    WRONG           // 错误点击
}

// ==================== 游戏配置 ====================

data class LevelConfig(
    val sequenceLength: Int,
    val highlightDuration: Long,  // 高亮持续时间(ms)
    val intervalDuration: Long     // 间隔时间(ms)
)

// ==================== 游戏模块 ====================

class LighthousePathGameModule : AbstractGameModule() {

    override val gameId: String = "lighthouse_path"
    override val gameName: String = "灯塔路径"
    override val iconAsset: String = "logo1.png"
    override val totalLevels: Int = 30
    override val description: String = "记忆灯塔闪烁顺序"

    // 协程作用域（使用父类的gameScope）

    // 游戏数据
    private var sequence = listOf<Int>()           // 正确序列
    private var playerInput = mutableListOf<Int>()   // 玩家输入
    private var cellStates = Array(16) { CellState.NORMAL }  // 格子视觉状态
    private var cellClickCounts = Array(16) { 0 }   // 每个格子的点击次数追踪
    private var currentPhase = GamePhase.SHOWING_SEQUENCE
    private var highlightedCell = -1                // 当前高亮的格子
    private var sequenceIndex = 0                   // 当前播放到序列第几个
    private var showSequenceComplete = false        // 序列播放是否完成
    private var wrongCellIndex = -1                 // 错误的格子索引

    // 调试模式
    private val DEBUG = true

    companion object {
        const val GRID_SIZE = 16  // 4x4 = 16
    }

    // ==================== 难度配置 ====================

    private fun getLevelConfig(level: Int): LevelConfig {
        return when {
            level <= 5 -> LevelConfig(
                sequenceLength = 2 + (level - 1) / 2,
                highlightDuration = 600L,
                intervalDuration = 300L
            )
            level <= 10 -> LevelConfig(
                sequenceLength = 4 + (level - 5) / 2,
                highlightDuration = 500L,
                intervalDuration = 200L
            )
            level <= 20 -> LevelConfig(
                sequenceLength = 6 + (level - 10) / 3,
                highlightDuration = 400L,
                intervalDuration = 150L
            )
            else -> LevelConfig(
                sequenceLength = 8 + (level - 20) / 3,
                highlightDuration = 300L,
                intervalDuration = 100L
            )
        }
    }

    // ==================== 序列生成 ====================

    private fun generateSequence(length: Int): List<Int> {
        return (0 until length).map { Random.nextInt(GRID_SIZE) }
    }

    // ==================== 游戏生命周期 ====================

    override fun start(level: Int) {
        currentLevel = level.coerceIn(1, totalLevels)
        currentScore = 0
        mistakeCount = 0
        playerInput.clear()
        cellStates = Array(GRID_SIZE) { CellState.NORMAL }
        cellClickCounts = Array(GRID_SIZE) { 0 }
        currentPhase = GamePhase.SHOWING_SEQUENCE
        highlightedCell = -1
        sequenceIndex = 0
        showSequenceComplete = false
        wrongCellIndex = -1

        // 直接开始游戏，不显示倒计时
        startGame()
    }

    override fun startGame() {
        val config = getLevelConfig(currentLevel)
        sequence = generateSequence(config.sequenceLength)
        playerInput.clear()
        cellStates = Array(GRID_SIZE) { CellState.NORMAL }
        cellClickCounts = Array(GRID_SIZE) { 0 }
        currentPhase = GamePhase.SHOWING_SEQUENCE
        highlightedCell = -1
        sequenceIndex = 0
        showSequenceComplete = false
        wrongCellIndex = -1

        if (DEBUG) {
            println("LighthousePath: Level $currentLevel, sequence length = ${sequence.size}")
            println("LighthousePath: Sequence = $sequence")
        }

        // 进入 Playing 状态
        updatePlayingState()

        // 开始播放序列
        playSequence()
    }

    // ==================== 序列播放 ====================

    private fun playSequence() {
        val config = getLevelConfig(currentLevel)
        currentPhase = GamePhase.SHOWING_SEQUENCE

        gameScope.launch {
            // 等待一小段时间让玩家准备
            delay(500)

            for (i in sequence.indices) {
                if (currentPhase != GamePhase.SHOWING_SEQUENCE) break

                sequenceIndex = i
                highlightedCell = sequence[i]
                cellStates = Array(GRID_SIZE) { CellState.NORMAL }
                cellStates[highlightedCell] = CellState.HIGHLIGHTED
                updatePlayingState()

                if (DEBUG) {
                    println("LighthousePath: Highlight cell ${highlightedCell}")
                }

                // 高亮持续时间
                delay(config.highlightDuration)

                // 熄灭
                cellStates[highlightedCell] = CellState.NORMAL
                highlightedCell = -1
                updatePlayingState()

                // 间隔时间（最后一个之后不等待）
                if (i < sequence.size - 1) {
                    delay(config.intervalDuration)
                }
            }

            // 序列播放完成
            showSequenceComplete = true
            currentPhase = GamePhase.WAITING_INPUT
            updatePlayingState()

            if (DEBUG) {
                println("LighthousePath: Sequence complete, waiting for input")
            }
        }
    }

    // ==================== 播放提示（失败后重新播放） ====================

    fun replaySequence() {
        if (currentPhase != GamePhase.RESULT) return

        currentPhase = GamePhase.SHOWING_SEQUENCE
        sequenceIndex = 0
        showSequenceComplete = false
        wrongCellIndex = -1
        cellStates = Array(GRID_SIZE) { CellState.NORMAL }
        cellClickCounts = Array(GRID_SIZE) { 0 }
        playerInput.clear()
        updatePlayingState()

        playSequence()
    }

    // ==================== 处理玩家输入 ====================

    override fun onUserAction(action: GameAction): ActionResult? {
        when (action) {
            is GameAction.TapIndex -> {
                return handleCellClick(action.index)
            }
            else -> return super.onUserAction(action)
        }
    }

    private fun handleCellClick(index: Int): ActionResult? {
        if (currentPhase != GamePhase.WAITING_INPUT) {
            return null
        }

        if (index < 0 || index >= GRID_SIZE) {
            return null
        }

        val expectedIndex = sequence.getOrNull(playerInput.size)

        if (DEBUG) {
            println("LighthousePath: Click cell $index, expected ${expectedIndex}, playerInput size ${playerInput.size}")
        }

        if (index == expectedIndex) {
            // 正确：增加点击计数，保持格子为可点击状态
            cellClickCounts[index]++
            playerInput.add(index)
            updatePlayingState()

            // 检查是否完成
            if (playerInput.size == sequence.size) {
                handleSuccess()
            }

            return ActionResult.Success
        } else {
            // 错误
            cellStates[index] = CellState.WRONG
            wrongCellIndex = index
            mistakeCount++
            updatePlayingState()

            handleFailure()
            return ActionResult.Error("点错了", shake = true)
        }
    }

    // ==================== 游戏结果 ====================

    private fun handleSuccess() {
        currentPhase = GamePhase.RESULT
        val timeUsed = stopTimer()
        val stars = calculateStars(timeUsed, mistakeCount, currentLevel)
        currentScore += 50 + maxOf(0, 100 - mistakeCount * 20)

        completeLevel(
            isSuccess = true,
            timeMillis = timeUsed,
            score = currentScore,
            stars = stars
        )
    }

    private fun handleFailure() {
        currentPhase = GamePhase.RESULT
        val timeUsed = stopTimer()

        _state.value = GameState.Completed(
            level = currentLevel,
            isSuccess = false,
            timeMillis = timeUsed,
            score = currentScore,
            stars = 1
        )

        _result.value = com.animalgame.core.game.GameResult(
            gameId = gameId,
            level = currentLevel,
            isSuccess = false,
            timeMillis = timeUsed,
            score = currentScore,
            stars = 1,
            mistakes = mistakeCount
        )
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
        return mapOf(
            "cellStates" to cellStates.map { it.name },
            "cellClickCounts" to cellClickCounts.toList(),
            "highlightedCell" to highlightedCell,
            "currentPhase" to currentPhase.name,
            "sequenceIndex" to sequenceIndex,
            "showSequenceComplete" to showSequenceComplete,
            "wrongCellIndex" to wrongCellIndex,
            "playerProgress" to playerInput.size,
            "totalToMatch" to sequence.size
        )
    }

    // ==================== 星级计算 ====================

    override fun calculateStars(timeMillis: Long, mistakes: Int, level: Int): Int {
        // 无错误 + 快速 → 3星
        // 少量错误(1次) → 2星
        // 多次错误 → 1星
        return when {
            mistakes == 0 && timeMillis < level * 1500 -> 3
            mistakes <= 1 -> 2
            else -> 1
        }
    }

    // ==================== 公共方法 ====================

    fun getDifficultyName(): String {
        return when {
            currentLevel <= 5 -> "入门"
            currentLevel <= 10 -> "进阶"
            currentLevel <= 20 -> "挑战"
            else -> "极限"
        }
    }

    fun getLevelIndex(): Int {
        return ((currentLevel - 1) % 10) + 1
    }

    fun restartLevel() {
        start(currentLevel)
    }

    fun resetToIdle() {
        stopTimer()
        _state.value = GameState.Idle
    }

    fun getCurrentPhase(): GamePhase = currentPhase

    fun getSequenceLength(): Int = sequence.size

    fun getSequence(): List<Int> = sequence

    override fun destroy() {
        super.destroy()
    }
}

// ==================== 注册 ====================

fun registerLighthousePathGame() {
    GameRegistry.register(LighthousePathGameModule())
}
