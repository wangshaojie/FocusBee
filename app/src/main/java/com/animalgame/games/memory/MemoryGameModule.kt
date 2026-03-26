package com.animalgame.games.memory

import com.animalgame.core.game.AbstractGameModule
import com.animalgame.core.game.ActionResult
import com.animalgame.core.game.GameAction
import com.animalgame.core.game.GameState
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

/**
 * 记忆翻牌游戏模块
 * 实现 GameModule 接口
 * 训练目标：短期记忆 + 注意力
 */
class MemoryGameModule : AbstractGameModule() {

    override val gameId: String = "memory"
    override val gameName: String = "记忆翻牌"
    override val iconAsset: String = "logo1.png"
    override val totalLevels: Int = 200  // 4个难度 × 50关
    override val description: String = "训练短期记忆与注意力"

    // 难度配置
    enum class Difficulty(val levelCount: Int, val gridRows: Int, val gridColumns: Int, val displayName: String) {
        EASY(50, 3, 4, "简单"),       // 3x4 = 12张 (6对)
        MEDIUM(50, 4, 4, "中等"),     // 4x4 = 16张 (8对)
        HARD(50, 4, 5, "困难"),      // 4x5 = 20张 (10对)
        EXPERT(50, 5, 6, "挑战")     // 5x6 = 30张 (15对)
    }

    // 当前难度
    private var currentDifficulty = Difficulty.EASY

    // 当前难度内的关卡索引 (0-49)
    private var levelIndex = 0

    // 游戏数据
    private var cards = listOf<MemoryGameCardData>()
    private var flippedIndices = listOf<Int>()
    private var matchedPairs = 0
    private var flipCount = 0  // 翻牌次数
    private var isChecking = false  // 检查中状态（禁止点击）

    /**
     * 网格尺寸数据类
     */
    data class GridSize(val rows: Int, val columns: Int) {
        val totalCards: Int = rows * columns

        // 校验：必须是偶数
        init {
            require(totalCards % 2 == 0) { "gridSize 必须是偶数，当前: $totalCards" }
        }

        // 计算 pairCount
        val pairCount: Int = totalCards / 2
    }

    // 根据 level (1-200) 设置难度和关卡索引
    private fun setLevel(level: Int) {
        // level 1-50 -> EASY, 51-100 -> MEDIUM, 101-150 -> HARD, 151-200 -> EXPERT
        currentDifficulty = when {
            level <= 50 -> Difficulty.EASY
            level <= 100 -> Difficulty.MEDIUM
            level <= 150 -> Difficulty.HARD
            else -> Difficulty.EXPERT
        }
        levelIndex = (level - 1) % 50  // 0-49
    }

    // 获取当前完整关卡号 (1-200)
    private fun getFullLevel(): Int {
        val difficultyIndex = Difficulty.entries.indexOf(currentDifficulty)
        return difficultyIndex * 50 + levelIndex + 1
    }

    /**
     * 开始游戏（跳过倒计时，直接开始）
     */
    override fun start(level: Int) {
        setLevel(level)
        currentScore = 0
        mistakeCount = 0

        // 直接进入游戏状态，不显示倒计时
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

    /**
     * 开始游戏
     */
    override fun startGame() {
        // 使用当前难度的网格大小
        val gridSize = GridSize(currentDifficulty.gridRows, currentDifficulty.gridColumns)

        // 动态计算 pairCount = gridSize / 2
        val pairCount = gridSize.pairCount

        // 使用 GameIconManager 生成卡片
        val icons = GameIconManager.getIconsForPairing(pairCount)
        cards = icons.flatMapIndexed { index, icon ->
            listOf(
                MemoryGameCardData(id = index * 2, iconId = icon.id, resourceId = icon.resourceId),
                MemoryGameCardData(id = index * 2 + 1, iconId = icon.id, resourceId = icon.resourceId)
            )
        }.shuffled()

        // ===== 校验卡片数量 =====
        val expectedCount = gridSize.rows * gridSize.columns
        if (cards.size != expectedCount) {
            throw IllegalStateException(
                "卡片数量错误！期望: $expectedCount (${gridSize.rows}x${gridSize.columns}), 实际: ${cards.size}"
            )
        }

        flippedIndices = emptyList()
        matchedPairs = 0
        flipCount = 0
        isChecking = false
        currentScore = 0
        mistakeCount = 0

        // 进入游戏状态
        _state.value = GameState.Playing(
            level = getFullLevel(),
            elapsedTime = 0L,
            score = 0,
            data = mapOf(
                "cards" to cards,
                "flippedIndices" to flippedIndices,
                "matchedPairs" to 0,
                "isChecking" to false,
                "gridRows" to gridSize.rows,
                "gridColumns" to gridSize.columns
            )
        )

        // 启动计时器
        startTimer()
    }

    /**
     * 处理用户操作
     */
    override fun onUserAction(action: GameAction): ActionResult? {
        when (action) {
            is GameAction.TapIndex -> {
                return handleCardTap(action.index)
            }
            else -> return super.onUserAction(action)
        }
    }

    /**
     * 处理卡片点击
     * 内部逻辑：
     * 1. 检查状态
     * 2. 翻开卡片
     * 3. 检查配对
     * 4. 延迟翻转（如果在 Checking）
     */
    private fun handleCardTap(index: Int): ActionResult? {
        val state = _state.value
        if (state !is GameState.Playing) return null

        // Checking 状态禁止点击 - 立即设置，禁止后续点击
        if (isChecking) return null

        val card = cards.getOrNull(index) ?: return null
        if (card.isMatched || card.isFlipped) return null

        flipCount++

        // 翻开卡片
        cards = cards.toMutableList().apply {
            this[index] = card.copy(isFlipped = true)
        }
        flippedIndices = flippedIndices + index

        // 检查配对
        if (flippedIndices.size == 2) {
            // 立即设置 isChecking，防止快速点击
            isChecking = true

            val card1 = cards[flippedIndices[0]]
            val card2 = cards[flippedIndices[1]]

            if (card1.iconId == card2.iconId) {
                // 配对成功
                matchedPairs++
                currentScore += 10

                cards = cards.toMutableList().apply {
                    this[flippedIndices[0]] = card1.copy(isMatched = true)
                    this[flippedIndices[1]] = card2.copy(isMatched = true)
                }
                flippedIndices = emptyList()
                isChecking = false  // 配对成功，立即解锁

                // 检查是否完成
                val gridSize = GridSize(currentDifficulty.gridRows, currentDifficulty.gridColumns)
                if (matchedPairs == gridSize.pairCount) {
                    completeGame()
                }

                // 更新状态
                updateState()
                return ActionResult.Success
            } else {
                // 配对失败，记录错误
                mistakeCount++
                updateState()

                // 延迟翻转回去（模块内部处理）
                gameScope.launch {
                    delay(1000)
                    flipBackAndCheck()
                }

                return ActionResult.Error("配对错误", shake = true)
            }
        } else {
            updateState()
        }

        return null
    }

    /**
     * 翻转回去并重置 Checking 状态
     */
    private fun flipBackAndCheck() {
        if (flippedIndices.size == 2) {
            val card1 = cards[flippedIndices[0]]
            val card2 = cards[flippedIndices[1]]
            cards = cards.toMutableList().apply {
                this[flippedIndices[0]] = card1.copy(isFlipped = false)
                this[flippedIndices[1]] = card2.copy(isFlipped = false)
            }
            flippedIndices = emptyList()
        }
        isChecking = false
        updateState()
    }

    /**
     * 更新状态
     */
    private fun updateState() {
        val state = _state.value
        if (state is GameState.Playing) {
            val gridSize = GridSize(currentDifficulty.gridRows, currentDifficulty.gridColumns)
            _state.value = state.copy(
                elapsedTime = System.currentTimeMillis() - startTime,
                score = currentScore,
                data = mapOf(
                    "cards" to cards,
                    "flippedIndices" to flippedIndices,
                    "matchedPairs" to matchedPairs,
                    "isChecking" to isChecking,
                    "flipCount" to flipCount,
                    "mistakeCount" to mistakeCount,
                    "gridRows" to gridSize.rows,
                    "gridColumns" to gridSize.columns
                )
            )
        }
    }

    /**
     * 完成游戏
     */
    private fun completeGame() {
        val timeMillis = stopTimer()
        val stars = calculateStars(timeMillis, mistakeCount, getFullLevel())

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
     * - 无错误 → 3星
     * - 少量错误（<=1对）→ 2星
     * - 多错误 → 1星
     */
    override fun calculateStars(timeMillis: Long, mistakes: Int, level: Int): Int {
        return when {
            mistakes == 0 -> 3      // 无错误
            mistakes <= 2 -> 2       // 少量错误
            else -> 1               // 多错误
        }
    }

    /**
     * 销毁游戏
     */
    override fun destroy() {
        super.destroy()
        cards = emptyList()
        flippedIndices = emptyList()
    }
}

/**
 * 记忆卡片数据（模块内部使用）
 */
data class MemoryGameCardData(
    val id: Int,
    val iconId: String,
    val resourceId: Int,
    val isFlipped: Boolean = false,
    val isMatched: Boolean = false
)