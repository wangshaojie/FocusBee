package com.animalgame.games.memory

import com.animalgame.core.game.AbstractGameModule
import com.animalgame.core.game.ActionResult
import com.animalgame.core.game.GameAction
import com.animalgame.core.game.GameResult
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
    override val totalLevels: Int = 100  // 100关：每25关循环一种难度
    override val description: String = "训练短期记忆与注意力"

    // 游戏数据
    private var cards = listOf<MemoryGameCardData>()
    private var flippedIndices = listOf<Int>()
    private var matchedPairs = 0
    private var flipCount = 0  // 翻牌次数
    private var isChecking = false  // 检查中状态（禁止点击）

    // 关卡配置：gridSize = rows * columns（必须是偶数）
    // pairCount = gridSize / 2
    // 100关：每25关循环一种难度
    // 1-25关: 3x4 (简单), 26-50关: 4x4 (中等), 51-75关: 4x5 (困难), 76-100关: 5x6 (挑战)
    private fun getGridSize(level: Int): GridSize {
        val normalizedLevel = ((level - 1) % 100) + 1
        return when {
            normalizedLevel <= 25 -> GridSize(3, 4)   // 简单: 3x4 = 12张 (6对)
            normalizedLevel <= 50 -> GridSize(4, 4)  // 中等: 4x4 = 16张 (8对)
            normalizedLevel <= 75 -> GridSize(4, 5)  // 困难: 4x5 = 20张 (10对)
            else -> GridSize(5, 6)                    // 挑战: 5x6 = 30张 (15对)
        }
    }

    // 获取难度名称
    private fun getDifficultyName(level: Int): String {
        val normalizedLevel = ((level - 1) % 100) + 1
        return when {
            normalizedLevel <= 25 -> "简单"
            normalizedLevel <= 50 -> "中等"
            normalizedLevel <= 75 -> "困难"
            else -> "挑战"
        }
    }

    // 旧版静态配置（保留以防需要特定关卡配置）
    private val levelGridSize = mapOf(
        1 to GridSize(3, 4),   // 简单: 3x4 = 12张 (6对)
        2 to GridSize(4, 4),   // 中等: 4x4 = 16张 (8对)
        3 to GridSize(4, 5),   // 困难: 4x5 = 20张 (10对)
        4 to GridSize(5, 6)     // 挑战: 5x6 = 30张 (15对)
    )

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

    /**
     * 开始游戏（跳过倒计时，直接开始）
     */
    override fun start(level: Int) {
        currentLevel = level
        currentScore = 0
        mistakeCount = 0

        // 直接进入游戏状态，不显示倒计时
        startGame()
    }

    /**
     * 开始游戏
     */
    override fun startGame() {
        // 获取当前关卡的 gridSize
        val gridSize = getGridSize(currentLevel)

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
            level = currentLevel,
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
                val gridSize = getGridSize(currentLevel)
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
            val gridSize = getGridSize(currentLevel)
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
        val stars = calculateStars(timeMillis, mistakeCount, currentLevel)

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
