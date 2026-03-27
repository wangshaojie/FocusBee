package com.animalgame.core.manager

/**
 * 关卡管理器
 * 统一管理所有游戏的关卡
 */
object LevelManager {

    /**
     * 游戏难度配置
     */
    data class LevelConfig(
        val level: Int,
        val gridSize: Int,
        val baseStars: Int,
        val description: String
    )

    /**
     * 获取指定游戏的关卡配置
     */
    fun getLevelsForGame(gameId: String): List<LevelConfig> {
        return when (gameId) {
            "schulte" -> getSchulteLevels()
            "animal" -> getAnimalLevels()
            "memory" -> getMemoryLevels()
            "color_mind" -> getColorMindLevels()
            else -> emptyList()
        }
    }

    /**
     * 舒尔特训练关卡配置
     * 4个难度，每个难度50关
     */
    private fun getSchulteLevels(): List<LevelConfig> {
        val levels = mutableListOf<LevelConfig>()
        val difficulties = listOf(
            Triple("简单", 3, 30),
            Triple("中等", 4, 40),
            Triple("困难", 5, 50),
            Triple("挑战", 6, 60)
        )

        difficulties.forEachIndexed { diffIndex, (name, size, baseStars) ->
            for (level in 1..50) {
                levels.add(
                    LevelConfig(
                        level = diffIndex * 50 + level,
                        gridSize = size,
                        baseStars = baseStars,
                        description = "$name 第$level 关"
                    )
                )
            }
        }
        return levels
    }

    /**
     * 萌音大挑战关卡配置
     */
    private fun getAnimalLevels(): List<LevelConfig> {
        return listOf(
            LevelConfig(1, 4, 10, "简单"),
            LevelConfig(2, 4, 10, "简单"),
            LevelConfig(3, 4, 10, "简单"),
            LevelConfig(4, 4, 10, "简单"),
            LevelConfig(5, 6, 15, "中等"),
            LevelConfig(6, 6, 15, "中等"),
            LevelConfig(7, 6, 15, "中等"),
            LevelConfig(8, 6, 15, "中等"),
            LevelConfig(9, 8, 20, "困难"),
            LevelConfig(10, 8, 20, "困难")
        )
    }

    /**
     * 记忆翻牌关卡配置
     * 4个难度，每个难度50关
     */
    private fun getMemoryLevels(): List<LevelConfig> {
        val levels = mutableListOf<LevelConfig>()
        val difficulties = listOf(
            Triple("简单", 6, 15),   // 3x4 = 6对
            Triple("中等", 8, 20),   // 4x4 = 8对
            Triple("困难", 10, 25),  // 4x5 = 10对
            Triple("挑战", 15, 30)   // 5x6 = 15对
        )

        difficulties.forEachIndexed { diffIndex, (name, pairs, baseStars) ->
            for (level in 1..50) {
                levels.add(
                    LevelConfig(
                        level = diffIndex * 10 + level,
                        gridSize = pairs,
                        baseStars = baseStars,
                        description = "$name 第$level 关"
                    )
                )
            }
        }
        return levels
    }

    /**
     * 根据难度获取关卡范围
     */
    fun getLevelRange(gameId: String, difficulty: Int): IntRange {
        return when (difficulty) {
            0 -> 1..10      // 简单
            1 -> 11..20     // 中等
            2 -> 21..30     // 困难
            3 -> 31..40     // 挑战
            else -> 1..10
        }
    }

    /**
     * 获取难度名称
     */
    fun getDifficultyName(difficulty: Int): String {
        return when (difficulty) {
            0 -> "简单"
            1 -> "中等"
            2 -> "困难"
            3 -> "挑战"
            else -> "未知"
        }
    }

    /**
     * 获取难度图标
     */
    fun getDifficultyEmoji(difficulty: Int): String {
        return when (difficulty) {
            0 -> "⭐"
            1 -> "⭐⭐"
            2 -> "⭐⭐⭐"
            3 -> "👑"
            else -> "⭐"
        }
    }

    /**
     * 颜色识别训练关卡配置
     * 4个难度，每个难度50关
     */
    private fun getColorMindLevels(): List<LevelConfig> {
        val levels = mutableListOf<LevelConfig>()
        val difficulties = listOf(
            Triple("简单", 10, 15),   // 颜色数2，基础星星15
            Triple("中等", 15, 20),   // 颜色数3，基础星星20
            Triple("困难", 20, 25),   // 颜色数4，基础星星25
            Triple("挑战", 25, 30)   // 颜色数5，基础星星30
        )

        difficulties.forEachIndexed { diffIndex, (name, questionCount, baseStars) ->
            for (level in 1..50) {
                levels.add(
                    LevelConfig(
                        level = diffIndex * 10 + level,
                        gridSize = questionCount,
                        baseStars = baseStars,
                        description = "$name 第$level 关"
                    )
                )
            }
        }
        return levels
    }
}
