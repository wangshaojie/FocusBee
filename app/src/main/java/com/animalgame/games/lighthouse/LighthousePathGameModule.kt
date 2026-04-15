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
 * 玩法：记忆灯塔闪烁顺序，依次点击还原
 *
 * 核心规则：
 * - 每个格子在序列中最多只能出现一次
 * - 序列播放完后，用户按顺序点击还原
 */

// ==================== 格子状态 ====================

enum class CellState {
    NORMAL,         // 普通状态
    HIGHLIGHTED,    // 高亮（序列播放时）
    CORRECT,        // 正确点击
    WRONG           // 错误点击
}

// ==================== 游戏阶段 ====================

enum class GamePhase {
    SHOWING_SEQUENCE,  // 显示序列中
    WAITING_INPUT,     // 等待玩家输入
    RESULT             // 结果
}

// ==================== 难度等级 ====================

enum class Difficulty {
    BEGINNER,      // 入门：5x5, 短序列
    INTERMEDIATE,  // 进阶：5x5, 中等序列
    ADVANCED,      // 挑战：5x5, 较长序列
    EXTREME        // 极限：5x5, 长序列
}

// ==================== 难度配置 ====================

data class DifficultyConfig(
    val gridSize: Int,           // 网格尺寸
    val sequenceLength: IntRange, // 序列长度范围
    val highlightDuration: Long,  // 高亮持续时间(ms)
    val intervalDuration: Long    // 间隔时间(ms)
)

// ==================== 游戏模块 ====================

class LighthousePathGameModule : AbstractGameModule() {

    override val gameId: String = "lighthouse_path"
    override val gameName: String = "灯塔路径"
    override val iconAsset: String = "logo1.png"
    override val totalLevels: Int = 200  // 4难度 x 50关
    override val description: String = "记忆灯塔闪烁顺序"

    // 调试模式
    private val DEBUG = true

    // ==================== 游戏数据 ====================

    private var sequence = listOf<Int>()           // 正确序列（每个索引唯一）
    private var playerInput = mutableListOf<Int>()   // 玩家输入
    private var cellStates = Array(16) { CellState.NORMAL }  // 格子视觉状态
    private var currentPhase = GamePhase.SHOWING_SEQUENCE
    private var highlightedCell = -1                // 当前高亮的格子
    private var sequenceIndex = 0                   // 当前播放到序列第几个
    private var showSequenceComplete = false        // 序列播放是否完成
    private var wrongCellIndex = -1                 // 错误的格子索引
    private var sequenceJob: kotlinx.coroutines.Job? = null  // 序列播放协程
    private var currentDifficulty = Difficulty.BEGINNER

    companion object {
        const val GRID_SIZE = 16  // 4x4 = 16 格子

        // 难度配置
        private val DIFFICULTY_CONFIGS = mapOf(
            Difficulty.BEGINNER to DifficultyConfig(
                gridSize = 16,
                sequenceLength = 3..4,
                highlightDuration = 600L,
                intervalDuration = 300L
            ),
            Difficulty.INTERMEDIATE to DifficultyConfig(
                gridSize = 16,
                sequenceLength = 5..6,
                highlightDuration = 500L,
                intervalDuration = 250L
            ),
            Difficulty.ADVANCED to DifficultyConfig(
                gridSize = 16,
                sequenceLength = 7..8,
                highlightDuration = 400L,
                intervalDuration = 200L
            ),
            Difficulty.EXTREME to DifficultyConfig(
                gridSize = 16,
                sequenceLength = 9..10,
                highlightDuration = 300L,
                intervalDuration = 150L
            )
        )
    }

    // ==================== 关卡解析 ====================

    /**
     * 根据关卡ID解析难度和关卡索引
     */
    private fun parseLevelId(level: Int): Pair<Difficulty, Int> {
        return when {
            level in 1..50 -> Difficulty.BEGINNER to level
            level in 51..100 -> Difficulty.INTERMEDIATE to (level - 50)
            level in 101..150 -> Difficulty.ADVANCED to (level - 100)
            else -> Difficulty.EXTREME to (level - 150)
        }
    }

    /**
     * 获取关卡名称
     */
    fun getLevelId(): String {
        val (difficulty, index) = parseLevelId(currentLevel)
        return when (difficulty) {
            Difficulty.BEGINNER -> "beginner_$index"
            Difficulty.INTERMEDIATE -> "intermediate_$index"
            Difficulty.ADVANCED -> "advanced_$index"
            Difficulty.EXTREME -> "extreme_$index"
        }
    }

    // ==================== 游戏初始化 ====================

    override fun start(level: Int) {
        currentLevel = level.coerceIn(1, totalLevels)
        currentScore = 0
        mistakeCount = 0
        playerInput.clear()
        cellStates = Array(GRID_SIZE) { CellState.NORMAL }
        currentPhase = GamePhase.SHOWING_SEQUENCE
        highlightedCell = -1
        sequenceIndex = 0
        showSequenceComplete = false
        wrongCellIndex = -1

        // 解析难度
        val (difficulty, _) = parseLevelId(currentLevel)
        currentDifficulty = difficulty

        // 直接开始游戏，不显示倒计时
        startGame()
    }

    override fun startGame() {
        val config = DIFFICULTY_CONFIGS[currentDifficulty]!!
        // 生成序列（每个格子最多出现一次）
        sequence = generateUniqueSequence(config.sequenceLength)
        playerInput.clear()
        cellStates = Array(GRID_SIZE) { CellState.NORMAL }
        currentPhase = GamePhase.SHOWING_SEQUENCE
        highlightedCell = -1
        sequenceIndex = 0
        showSequenceComplete = false
        wrongCellIndex = -1

        if (DEBUG) {
            println("LighthousePath: Level $currentLevel (${currentDifficulty.name}), sequence = $sequence")
        }

        // 进入 Playing 状态
        updatePlayingState()

        // 开始播放序列
        playSequence()
    }

    // ==================== 序列生成（核心修复） ====================

    /**
     * 生成不重复的序列
     * 每个格子索引在序列中最多只出现一次
     */
    private fun generateUniqueSequence(lengthRange: IntRange): List<Int> {
        val length = lengthRange.random()
        // 创建所有可用格子的列表
        val availableCells = (0 until GRID_SIZE).toMutableList()
        // 打乱顺序
        availableCells.shuffle()
        // 取前length个（如果length大于可用格子数，则取所有）
        return availableCells.take(length.coerceAtMost(GRID_SIZE))
    }

    // ==================== 序列播放 ====================

    private fun playSequence() {
        val config = DIFFICULTY_CONFIGS[currentDifficulty]!!
        currentPhase = GamePhase.SHOWING_SEQUENCE

        sequenceJob?.cancel()
        sequenceJob = gameScope.launch {
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
                    println("LighthousePath: Highlight cell ${highlightedCell} ($i/${sequence.size})")
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
        playerInput.clear()
        updatePlayingState()

        playSequence()
    }

    // ==================== 处理玩家输入 ====================

    override fun onUserAction(action: GameAction): ActionResult? {
        return when (action) {
            is GameAction.TapIndex -> {
                handleCellClick(action.index)
            }
            is GameAction.NextLevel -> {
                // 每个难度的最后一关通关后不进入下一难度
                if (currentLevel == totalLevels) {
                    _state.value = GameState.AllCompleted(
                        totalTime = stopTimer(),
                        totalScore = currentScore,
                        levelResults = emptyList()
                    )
                    null
                } else if (currentLevel % 50 == 0) {
                    // 难度最后一关，显示通关界面
                    _state.value = GameState.AllCompleted(
                        totalTime = stopTimer(),
                        totalScore = currentScore,
                        levelResults = emptyList()
                    )
                    null
                } else {
                    super.onUserAction(action)
                }
            }
            else -> super.onUserAction(action)
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
            println("LighthousePath: Click cell $index, expected $expectedIndex, progress ${playerInput.size}/${sequence.size}")
        }

        if (index == expectedIndex) {
            // 正确 - 显示点亮效果
            cellStates[index] = CellState.CORRECT
            playerInput.add(index)
            updatePlayingState()

            // 检查是否完成
            if (playerInput.size == sequence.size) {
                handleSuccess()
            }

            return ActionResult.Success
        } else {
            // 错误 - 显示红色闪烁效果，然后恢复继续游戏
            wrongCellIndex = index
            mistakeCount++
            updatePlayingState()

            // 延迟500ms后清除错误状态，让玩家看到红色闪烁
            gameScope.launch {
                delay(500)
                wrongCellIndex = -1
                updatePlayingState()
            }

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
            "highlightedCell" to highlightedCell,
            "currentPhase" to currentPhase.name,
            "sequenceIndex" to sequenceIndex,
            "showSequenceComplete" to showSequenceComplete,
            "wrongCellIndex" to wrongCellIndex,
            "playerProgress" to playerInput.size,
            "totalToMatch" to sequence.size,
            "difficulty" to currentDifficulty.name,
            "levelId" to getLevelId()
        )
    }

    // ==================== 星级计算 ====================

    override fun calculateStars(timeMillis: Long, mistakes: Int, level: Int): Int {
        return when {
            mistakes == 0 && timeMillis < level * 1000 -> 3
            mistakes <= 1 -> 2
            else -> 1
        }
    }

    // ==================== 公共方法 ====================

    fun getDifficultyName(): String {
        return when (currentDifficulty) {
            Difficulty.BEGINNER -> "入门"
            Difficulty.INTERMEDIATE -> "进阶"
            Difficulty.ADVANCED -> "挑战"
            Difficulty.EXTREME -> "极限"
        }
    }

    fun getLevelIndex(): Int {
        return ((currentLevel - 1) % 50) + 1
    }

    fun restartLevel() {
        start(currentLevel)
    }

    fun resetToIdle() {
        stopTimer()
        sequenceJob?.cancel()
        sequenceJob = null
        wrongCellIndex = -1
        cellStates = Array(GRID_SIZE) { CellState.NORMAL }
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
