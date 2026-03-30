package com.animalgame.games.summatch

import com.animalgame.core.game.AbstractGameModule
import com.animalgame.core.game.ActionResult
import com.animalgame.core.game.GameAction
import com.animalgame.core.game.GameState
import com.animalgame.core.manager.GameRegistry
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlin.math.abs
import kotlin.random.Random

/**
 * 数字连连看（Sum Match）游戏模块
 * 玩法：选择数字圆圈，使其总和等于目标和
 */

// ==================== 游戏阶段 ====================

enum class GamePhase {
    SHOWING_NUMBERS,   // 显示数字中
    SELECTING,         // 玩家选择中
    SUCCESS_ANIM,     // 成功动画中
    FAIL_ANIM         // 失败动画中
}

// ==================== 数字圆圈数据 ====================

data class NumberNode(
    val id: Int,
    val value: Int,
    val x: Float,       // 相对位置 0-1
    val y: Float,       // 相对位置 0-1
    val isSelected: Boolean = false,
    val isMatched: Boolean = false,  // 已匹配消除
    val isWrong: Boolean = false     // 错误提示
)

// ==================== 关卡配置 ====================

data class LevelConfig(
    val nodeCount: Int,        // 圆圈数量
    val targetMin: Int,        // 目标和最小值
    val targetMax: Int,         // 目标和最大值
    val minSolutionSize: Int,   // 最少需要几个数字
    val maxSolutionSize: Int,   // 最多需要几个数字
    val initialTime: Long,      // 初始时间(ms)
    val timeBonus: Long,        // 成功奖励时间
    val timePenalty: Long       // 失败扣除时间
)

// ==================== 游戏模块 ====================

class SumMatchGameModule : AbstractGameModule() {

    override val gameId: String = "sum_match"
    override val gameName: String = "数字连连看"
    override val iconAsset: String = "logo1.png"
    override val totalLevels: Int = 30
    override val description: String = "找出数字组合使之和等于目标和"

    // 游戏数据
    private var nodes = listOf<NumberNode>()              // 数字圆圈列表
    private var selectedIndices = mutableSetOf<Int>()       // 选中的圆圈索引
    private var currentPhase = GamePhase.SHOWING_NUMBERS
    private var target = 0                                 // 目标和
    private var correctSolution = listOf<Int>()            // 正确答案（用于验证）
    private var remainingTime = 0L                          // 剩余时间
    private var successCount = 0                            // 成功次数
    private var wrongCount = 0                             // 错误次数
    private var showWrong = false                           // 显示错误提示
    private var showSuccess = false                         // 显示成功提示
    private var requiredMatches = 0                         // 需要完成的匹配次数
    private var currentMatches = 0                          // 当前完成次数

    // 计时器
    private var gameTimerJob: kotlinx.coroutines.Job? = null

    private val DEBUG = true

    companion object {
        const val GRID_SIZE = 16
    }

    // ==================== 难度配置 ====================

    private fun getLevelConfig(level: Int): LevelConfig {
        return when {
            level <= 5 -> LevelConfig(
                nodeCount = 6 + level,
                targetMin = 5 + level,
                targetMax = 10 + level * 2,
                minSolutionSize = 2,
                maxSolutionSize = 2,
                initialTime = 30000L,
                timeBonus = 5000L,
                timePenalty = 2000L
            )
            level <= 10 -> LevelConfig(
                nodeCount = 8 + level,
                targetMin = 10 + level,
                targetMax = 15 + level * 2,
                minSolutionSize = 2,
                maxSolutionSize = 3,
                initialTime = 35000L,
                timeBonus = 4000L,
                timePenalty = 3000L
            )
            level <= 20 -> LevelConfig(
                nodeCount = 10 + level,
                targetMin = 15 + level,
                targetMax = 20 + level * 2,
                minSolutionSize = 2,
                maxSolutionSize = 3,
                initialTime = 40000L,
                timeBonus = 3000L,
                timePenalty = 3000L
            )
            else -> LevelConfig(
                nodeCount = 12 + level,
                targetMin = 20 + level,
                targetMax = 25 + level * 2,
                minSolutionSize = 3,
                maxSolutionSize = 4,
                initialTime = 45000L,
                timeBonus = 2000L,
                timePenalty = 4000L
            )
        }
    }

    // ==================== 数字生成算法（保证有解） ====================

    /**
     * 生成关卡数字
     * 核心原则：先确定解法，再填充干扰数字
     */
    private fun generateLevelNumbers(config: LevelConfig): Pair<List<Int>, Int> {
        // 1. 确定解法数量
        val solutionSize = Random.nextInt(config.minSolutionSize, config.maxSolutionSize + 1)

        // 2. 生成目标和（在合理范围内）
        val targetValue = Random.nextInt(config.targetMin, config.targetMax.coerceAtMost(50))

        // 3. 生成正确答案组合
        val solution = generateSolution(targetValue, solutionSize)

        // 4. 生成干扰数字（不能形成有效解）
        val distractorCount = config.nodeCount - solution.size
        val distractors = generateDistractors(distractorCount, targetValue, solution.toSet())

        // 5. 混合并打乱
        val allNumbers = (solution + distractors).shuffled()

        return Pair(allNumbers, targetValue)
    }

    /**
     * 生成一个加起来等于target的数字组合
     * 例如: target=10, size=2 → [3,7] 或 [4,6]
     */
    private fun generateSolution(target: Int, size: Int): List<Int> {
        if (size <= 0 || target <= 0) return emptyList()

        if (size == 1) {
            // 单个数字，直接返回（但这种情况不常见用于求和）
            return listOf(target)
        }

        if (size == 2) {
            // 两个数字：随机分配
            val a = Random.nextInt(1, target.coerceAtMost(20))
            val b = target - a
            if (b > 0) return listOf(a, b)
        }

        // size >= 3: 递归生成
        val result = mutableListOf<Int>()
        var remaining = target
        var count = size

        while (count > 0) {
            if (count == 1) {
                // 最后一个数字
                if (remaining > 0 && remaining <= 30) {
                    result.add(remaining)
                } else {
                    // 回溯，重新生成
                    return generateSolution(target, size)
                }
                break
            }

            // 随机生成本轮数字（确保不会让剩余数字无法分配）
            val maxVal = (remaining / count).coerceAtMost(25)
            val minVal = 1
            val value = Random.nextInt(minVal, maxVal.coerceAtLeast(minVal + 1) + 1)

            result.add(value)
            remaining -= value
            count--

            // 防止死循环
            if (remaining <= 0 && count > 0) {
                return generateSolution(target, size)
            }
        }

        // 验证解法正确
        if (result.sum() == target && result.all { it > 0 && it <= 30 }) {
            return result
        }

        // 无效，回溯
        return generateSolution(target, size)
    }

    /**
     * 生成干扰数字
     * 规则：不能与现有数字组合形成target
     */
    private fun generateDistractors(count: Int, target: Int, existingNumbers: Set<Int>): List<Int> {
        val distractors = mutableListOf<Int>()
        var attempts = 0

        while (distractors.size < count && attempts < 100) {
            attempts++
            val candidate = Random.nextInt(1, 31)

            // 检查是否会产生巧合解（与现有数字组合等于target）
            var wouldCreateSolution = false
            for (existing in existingNumbers) {
                if (existing + candidate == target && distractors.size + 1 < count) {
                    wouldCreateSolution = true
                    break
                }
            }

            // 也检查干扰数字之间的组合
            if (!wouldCreateSolution && distractors.isNotEmpty()) {
                for (d in distractors) {
                    if (d + candidate == target) {
                        wouldCreateSolution = true
                        break
                    }
                }
            }

            if (!wouldCreateSolution && candidate !in distractors) {
                distractors.add(candidate)
            }
        }

        // 如果生成不足，用安全数字填充
        while (distractors.size < count) {
            val safeNum = Random.nextInt(1, 31)
            if (safeNum !in distractors && safeNum + (existingNumbers.firstOrNull() ?: 0) != target) {
                distractors.add(safeNum)
            }
        }

        return distractors
    }

    // ==================== 生成圆圈位置 ====================

    private fun generateNodePositions(count: Int): List<Pair<Float, Float>> {
        val positions = mutableListOf<Pair<Float, Float>>()
        val padding = 0.15f
        val cellSize = (1f - padding * 2) / 4f

        // 4x4网格布局，带随机偏移
        for (i in 0 until count) {
            val row = i / 4
            val col = i % 4

            val baseX = padding + col * cellSize + cellSize / 2
            val baseY = padding + row * cellSize + cellSize / 2

            // 添加小幅随机偏移，避免完全对齐
            val offsetX = (Random.nextFloat() - 0.5f) * cellSize * 0.3f
            val offsetY = (Random.nextFloat() - 0.5f) * cellSize * 0.3f

            positions.add(Pair(
                (baseX + offsetX).coerceIn(padding, 1f - padding),
                (baseY + offsetY).coerceIn(padding, 1f - padding)
            ))
        }

        return positions
    }

    // ==================== 游戏生命周期 ====================

    override fun start(level: Int) {
        currentLevel = level.coerceIn(1, totalLevels)
        currentScore = 0
        mistakeCount = 0
        selectedIndices.clear()
        successCount = 0
        wrongCount = 0
        showWrong = false
        showSuccess = false

        // 直接开始游戏
        startGame()
    }

    override fun startGame() {
        val config = getLevelConfig(currentLevel)
        remainingTime = config.initialTime

        // 计算本关需要完成的匹配次数
        requiredMatches = when {
            currentLevel <= 5 -> 2
            currentLevel <= 10 -> 3
            currentLevel <= 20 -> 4
            else -> 5
        }
        currentMatches = 0

        // 生成关卡
        generateRound()

        // 启动计时器
        startGameTimer()

        // 进入选择阶段
        currentPhase = GamePhase.SHOWING_NUMBERS
        updatePlayingState()

        // 短暂显示后进入选择阶段
        gameScope.launch {
            delay(500)
            currentPhase = GamePhase.SELECTING
            updatePlayingState()
        }
    }

    private fun generateRound() {
        val config = getLevelConfig(currentLevel)
        val (numbers, targetValue) = generateLevelNumbers(config)

        target = targetValue

        // 生成带防重叠的位置
        val positions = generateScatteredPositions(numbers.size)

        // 创建节点
        nodes = numbers.mapIndexed { index, value ->
            NumberNode(
                id = index,
                value = value,
                x = positions[index].first,
                y = positions[index].second,
                isSelected = false,
                isMatched = false,
                isWrong = false
            )
        }

        selectedIndices.clear()
        showWrong = false
        showSuccess = false

        if (DEBUG) {
            println("SumMatch: Round generated, target=$target, nodes=${nodes.map { it.value }}")
        }
    }

    /**
     * 防重叠的散点分布算法
     */
    private fun generateScatteredPositions(count: Int): List<Pair<Float, Float>> {
        val positions = mutableListOf<Pair<Float, Float>>()
        val padding = 0.12f  // 边距
        val minDistance = 0.18f  // 最小间距（相对于容器尺寸）
        val maxAttempts = 100  // 最大尝试次数

        // 根据数量调整
        val adjustedMinDistance = when {
            count <= 6 -> 0.22f
            count <= 9 -> 0.18f
            count <= 12 -> 0.15f
            else -> 0.13f
        }

        for (i in 0 until count) {
            var attempts = 0
            var validPosition = false

            while (!validPosition && attempts < maxAttempts) {
                attempts++

                // 在安全区域内随机生成位置
                val x = Random.nextFloat() * (1f - padding * 2) + padding
                val y = Random.nextFloat() * (1f - padding * 2) + padding

                // 检查与所有现有位置的距离
                var tooClose = false
                for (existing in positions) {
                    val dx = x - existing.first
                    val dy = y - existing.second
                    val distance = kotlin.math.sqrt(dx * dx + dy * dy)
                    if (distance < adjustedMinDistance) {
                        tooClose = true
                        break
                    }
                }

                if (!tooClose) {
                    positions.add(Pair(x, y))
                    validPosition = true
                }
            }

            // 如果找不到合适位置，使用备用网格位置
            if (!validPosition) {
                val col = i % 4
                val row = i / 4
                val gridX = padding + col * (1f - padding * 2) / 3
                val gridY = padding + row * (1f - padding * 2) / (count / 4 + 1)
                positions.add(Pair(gridX, gridY))
            }
        }

        return positions
    }

    private fun startGameTimer() {
        gameTimerJob?.cancel()
        gameTimerJob = gameScope.launch {
            while (true) {
                delay(100)
                remainingTime -= 100

                if (remainingTime <= 0) {
                    remainingTime = 0
                    gameOver()
                    break
                }
                updatePlayingState()
            }
        }
    }

    // ==================== 处理玩家输入 ====================

    override fun onUserAction(action: GameAction): ActionResult? {
        when (action) {
            is GameAction.TapIndex -> {
                return handleNodeClick(action.index)
            }
            else -> return super.onUserAction(action)
        }
    }

    private fun handleNodeClick(index: Int): ActionResult? {
        if (currentPhase != GamePhase.SELECTING) {
            return null
        }

        if (index < 0 || index >= nodes.size) {
            return null
        }

        val node = nodes[index]
        if (node.isMatched) {
            return null
        }

        // 切换选择状态
        if (selectedIndices.contains(index)) {
            // 取消选择
            selectedIndices.remove(index)
            nodes = nodes.toMutableList().apply {
                this[index] = node.copy(isSelected = false)
            }
        } else {
            // 添加选择
            selectedIndices.add(index)
            nodes = nodes.toMutableList().apply {
                this[index] = node.copy(isSelected = true)
            }
        }

        updatePlayingState()

        // 计算当前选择之和
        val currentSum = calculateSelectedSum()

        if (DEBUG) {
            println("SumMatch: Selected $selectedIndices, sum=$currentSum, target=$target")
        }

        // 检查是否满足条件
        if (selectedIndices.size >= 2) {
            when {
                currentSum == target -> {
                    // 成功！
                    return handleSuccess()
                }
                currentSum > target -> {
                    // 超出，清空并提示
                    return handleSumTooHigh()
                }
            }
        }

        return null
    }

    private fun calculateSelectedSum(): Int {
        return selectedIndices.sumOf { index ->
            nodes.getOrNull(index)?.value ?: 0
        }
    }

    private fun handleSuccess(): ActionResult? {
        currentPhase = GamePhase.SUCCESS_ANIM
        showSuccess = true
        successCount++
        currentMatches++

        // 增加时间
        val config = getLevelConfig(currentLevel)
        remainingTime = (remainingTime + config.timeBonus).coerceAtMost(60000)

        // 标记匹配
        nodes = nodes.mapIndexed { index, node ->
            if (selectedIndices.contains(index)) {
                node.copy(isMatched = true, isSelected = false)
            } else {
                node
            }
        }
        selectedIndices.clear()

        updatePlayingState()

        // 检查是否过关
        if (currentMatches >= requiredMatches) {
            gameScope.launch {
                delay(800)
                handleLevelComplete()
            }
        } else {
            // 生成下一轮
            gameScope.launch {
                delay(800)
                generateRound()
                currentPhase = GamePhase.SHOWING_NUMBERS
                updatePlayingState()
                delay(500)
                currentPhase = GamePhase.SELECTING
                updatePlayingState()
            }
        }

        return ActionResult.Success
    }

    private fun handleSumTooHigh(): ActionResult {
        currentPhase = GamePhase.FAIL_ANIM
        showWrong = true
        wrongCount++
        mistakeCount++

        // 扣除时间
        val config = getLevelConfig(currentLevel)
        remainingTime = (remainingTime - config.timePenalty).coerceAtLeast(0)

        // 标记错误
        nodes = nodes.map { node ->
            if (selectedIndices.contains(node.id)) {
                node.copy(isWrong = true)
            } else {
                node
            }
        }

        updatePlayingState()

        // 延迟清空
        gameScope.launch {
            delay(500)
            // 清空选择
            selectedIndices.clear()
            nodes = nodes.map { it.copy(isSelected = false, isWrong = false) }
            showWrong = false
            currentPhase = GamePhase.SELECTING
            updatePlayingState()
        }

        return ActionResult.Error("超出目标", shake = true)
    }

    private fun handleLevelComplete() {
        gameTimerJob?.cancel()
        val timeUsed = getLevelConfig(currentLevel).initialTime - remainingTime
        val stars = calculateStars(timeUsed, wrongCount, currentLevel)
        currentScore += 50 + maxOf(0, 100 - wrongCount * 10) + successCount * 20

        completeLevel(
            isSuccess = true,
            timeMillis = timeUsed,
            score = currentScore,
            stars = stars
        )
    }

    private fun gameOver() {
        gameTimerJob?.cancel()
        val timeUsed = getLevelConfig(currentLevel).initialTime - remainingTime
        val stars = calculateStars(timeUsed, wrongCount, currentLevel)

        _state.value = GameState.Completed(
            level = currentLevel,
            isSuccess = false,
            timeMillis = timeUsed,
            score = currentScore,
            stars = stars
        )

        _result.value = com.animalgame.core.game.GameResult(
            gameId = gameId,
            level = currentLevel,
            isSuccess = false,
            timeMillis = timeUsed,
            score = currentScore,
            stars = stars,
            mistakes = mistakeCount
        )
    }

    // ==================== 状态更新 ====================

    private fun updatePlayingState() {
        _state.value = GameState.Playing(
            level = currentLevel,
            elapsedTime = getLevelConfig(currentLevel).initialTime - remainingTime,
            score = currentScore,
            data = buildGameData()
        )
    }

    private fun buildGameData(): Map<String, Any> {
        return mapOf(
            "nodes" to nodes.map { node ->
                mapOf(
                    "id" to node.id,
                    "value" to node.value,
                    "x" to node.x,
                    "y" to node.y,
                    "isSelected" to node.isSelected,
                    "isMatched" to node.isMatched,
                    "isWrong" to node.isWrong
                )
            },
            "target" to target,
            "selectedSum" to calculateSelectedSum(),
            "selectedCount" to selectedIndices.size,
            "currentPhase" to currentPhase.name,
            "remainingTime" to remainingTime,
            "successCount" to successCount,
            "wrongCount" to wrongCount,
            "currentMatches" to currentMatches,
            "requiredMatches" to requiredMatches,
            "showWrong" to showWrong,
            "showSuccess" to showSuccess
        )
    }

    // ==================== 星级计算 ====================

    override fun calculateStars(timeMillis: Long, mistakes: Int, level: Int): Int {
        val accuracy = if (successCount + wrongCount > 0) {
            successCount.toFloat() / (successCount + wrongCount)
        } else 0f

        return when {
            accuracy >= 0.9f && mistakes <= 1 -> 3
            accuracy >= 0.7f && mistakes <= 3 -> 2
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
        gameTimerJob?.cancel()
        _state.value = GameState.Idle
    }

    fun clearSelection() {
        if (currentPhase != GamePhase.SELECTING) return
        selectedIndices.clear()
        nodes = nodes.map { it.copy(isSelected = false, isWrong = false) }
        showWrong = false
        updatePlayingState()
    }

    fun getCurrentPhase(): GamePhase = currentPhase

    fun getTarget(): Int = target

    fun getCurrentSum(): Int = calculateSelectedSum()

    fun getRequiredMatches(): Int = requiredMatches

    fun getCurrentMatches(): Int = currentMatches

    override fun destroy() {
        gameTimerJob?.cancel()
        super.destroy()
    }
}

// ==================== 注册 ====================

fun registerSumMatchGame() {
    GameRegistry.register(SumMatchGameModule())
}
