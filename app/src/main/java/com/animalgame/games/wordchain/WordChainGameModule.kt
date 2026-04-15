package com.animalgame.games.wordchain

import com.animalgame.core.game.AbstractGameModule
import com.animalgame.core.game.ActionResult
import com.animalgame.core.game.GameAction
import com.animalgame.core.game.GameState
import com.animalgame.core.manager.GameRegistry
import kotlin.random.Random

/**
 * 谁说得快（词语接龙）游戏模块
 * 玩法：App提供主题词，玩家轮流说出符合该类别的词
 * App不做答案校验，仅提供主题和控制节奏
 */

// ==================== 难度级别 ====================

enum class WordChainDifficulty(val displayName: String, val description: String) {
    EASY("入门", "简单类别"),
    MEDIUM("进阶", "日常类别"),
    HARD("挑战", "广泛类别"),
    EXTREME("极限", "全部类别")
}

// ==================== 主题词列表 ====================

object CategoryManager {
    // 入门级别 - 基础常见类别
    private val easyCategories = listOf(
        "动物", "水果", "蔬菜", "颜色", "玩具"
    )

    // 进阶级别 - 日常类别
    private val mediumCategories = listOf(
        "动物", "水果", "蔬菜", "植物", "交通工具", "文具", "颜色", "玩具", "生活用品"
    )

    // 挑战级别 - 更广泛
    private val hardCategories = listOf(
        "动物", "水果", "蔬菜", "植物", "交通工具", "文具", "生活用品", "家用电器", "颜色", "职业", "学习用品", "玩具", "体育用品"
    )

    // 极限级别 - 全部类别
    private val extremeCategories = listOf(
        "动物", "水果", "蔬菜", "植物", "交通工具", "文具", "生活用品", "家用电器", "颜色", "职业", "景点", "学习用品", "玩具", "体育用品"
    )

    private var currentCategories: List<String> = emptyList()
    private var currentIndex: Int = -1

    /**
     * 根据难度加载类别
     */
    fun loadByDifficulty(difficulty: WordChainDifficulty) {
        currentCategories = when (difficulty) {
            WordChainDifficulty.EASY -> easyCategories
            WordChainDifficulty.MEDIUM -> mediumCategories
            WordChainDifficulty.HARD -> hardCategories
            WordChainDifficulty.EXTREME -> extremeCategories
        }
        currentIndex = -1
    }

    /**
     * 获取随机主题（避免重复当前主题）
     */
    fun getRandomCategory(): String {
        if (currentCategories.isEmpty()) return "动物"
        if (currentCategories.size <= 1) return currentCategories.first()

        var newIndex: Int
        do {
            newIndex = Random.nextInt(currentCategories.size)
        } while (newIndex == currentIndex && currentCategories.size > 1)

        currentIndex = newIndex
        return currentCategories[newIndex]
    }

    /**
     * 获取所有主题数量
     */
    fun getCategoryCount(): Int = currentCategories.size

    /**
     * 重置（重新开始时调用）
     */
    fun reset() {
        currentIndex = -1
    }
}

// ==================== 难度解析 ====================

object WordChainLevelParser {
    /**
     * 根据关卡ID解析难度和关卡索引
     */
    fun parseLevelId(level: Int): Pair<WordChainDifficulty, Int> {
        return when {
            level in 1..25 -> WordChainDifficulty.EASY to level
            level in 26..50 -> WordChainDifficulty.MEDIUM to (level - 25)
            level in 51..75 -> WordChainDifficulty.HARD to (level - 50)
            else -> WordChainDifficulty.EXTREME to (level - 75)
        }
    }

    /**
     * 获取难度对应的起始关卡
     */
    fun getStartLevel(difficulty: WordChainDifficulty): Int {
        return when (difficulty) {
            WordChainDifficulty.EASY -> 1
            WordChainDifficulty.MEDIUM -> 26
            WordChainDifficulty.HARD -> 51
            WordChainDifficulty.EXTREME -> 76
        }
    }
}

// ==================== 游戏模块 ====================

class WordChainGameModule : AbstractGameModule() {

    override val gameId: String = "word_chain"
    override val gameName: String = "谁说得快"
    override val iconAsset: String = "wordchain_icon.png"
    override val totalLevels: Int = 100
    override val description: String = "词语接龙，轮流说词"

    // 当前主题
    private var currentCategory: String = ""

    // 当前难度
    private var currentDifficulty = WordChainDifficulty.EASY

    /**
     * 获取难度名称
     */
    fun getDifficultyName(): String = currentDifficulty.displayName

    /**
     * 获取关卡索引 (1-25)
     */
    fun getLevelIndex(): Int {
        val (_, index) = WordChainLevelParser.parseLevelId(currentLevel)
        return index
    }

    override fun start(level: Int) {
        currentLevel = level.coerceIn(1, totalLevels)
        currentScore = 0
        mistakeCount = 0

        // 解析难度
        val (difficulty, _) = WordChainLevelParser.parseLevelId(currentLevel)
        currentDifficulty = difficulty

        // 加载该难度的类别
        CategoryManager.loadByDifficulty(difficulty)

        // 获取初始主题
        currentCategory = CategoryManager.getRandomCategory()

        // 直接进入游戏状态
        startGame()
    }

    override fun startGame() {
        updatePlayingState()
    }

    override fun onUserAction(action: GameAction): ActionResult? {
        return when (action) {
            is GameAction.TapIndex -> {
                // 点击切换主题按钮
                changeCategory()
                ActionResult.Success
            }
            else -> super.onUserAction(action)
        }
    }

    /**
     * 切换到下一个随机主题
     */
    fun changeCategory() {
        currentCategory = CategoryManager.getRandomCategory()
        updatePlayingState()
    }

    /**
     * 获取当前主题
     */
    fun getCurrentCategory(): String = currentCategory

    /**
     * 重置到空闲状态
     */
    fun resetToIdle() {
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
            "currentCategory" to currentCategory,
            "categoryCount" to CategoryManager.getCategoryCount(),
            "difficulty" to currentDifficulty.name,
            "difficultyName" to currentDifficulty.displayName,
            "levelId" to "${currentDifficulty.displayName}_${getLevelIndex()}"
        )
    }

    override fun destroy() {
        super.destroy()
    }
}

// ==================== 注册 ====================

fun registerWordChainGame() {
    GameRegistry.register(WordChainGameModule())
}