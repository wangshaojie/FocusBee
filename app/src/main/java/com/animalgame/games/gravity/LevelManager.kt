package com.animalgame.games.gravity

import android.graphics.PointF

/**
 * 关卡管理器
 * 提供所有40关的配置数据
 */
object LevelManager {

    /**
     * 获取指定难度的关卡
     */
    fun getLevel(difficulty: Difficulty, levelIndex: Int): LevelConfig {
        val levels = when (difficulty) {
            Difficulty.EASY -> getEasyLevels()
            Difficulty.MEDIUM -> getMediumLevels()
            Difficulty.HARD -> getHardLevels()
            Difficulty.EXPERT -> getExpertLevels()
        }
        return levels[levelIndex.coerceIn(0, levels.size - 1)]
    }

    /**
     * 获取难度范围
     */
    fun getDifficultyRange(difficulty: Difficulty): IntRange {
        return 1..difficulty.levelCount
    }

    // 简单难度关卡（3堵墙）
    private fun getEasyLevels(): List<LevelConfig> {
        return listOf(
            // 第1关：简单直线
            LevelConfig(1, Difficulty.EASY,
                PointF(0.1f, 0.9f), PointF(0.9f, 0.1f),
                listOf(Wall(0.3f, 0.3f, 0.3f, 0.7f)),
                25000, 3),

            // 第2关：单墙阻挡
            LevelConfig(2, Difficulty.EASY,
                PointF(0.1f, 0.5f), PointF(0.9f, 0.5f),
                listOf(Wall(0.5f, 0.2f, 0.5f, 0.8f)),
                25000, 3),

            // 第3关：S形路径
            LevelConfig(3, Difficulty.EASY,
                PointF(0.1f, 0.9f), PointF(0.9f, 0.1f),
                listOf(Wall(0.3f, 0.0f, 0.3f, 0.5f), Wall(0.7f, 0.5f, 0.7f, 1.0f)),
                25000, 3),

            // 第4关：中间墙
            LevelConfig(4, Difficulty.EASY,
                PointF(0.5f, 0.9f), PointF(0.5f, 0.1f),
                listOf(Wall(0.2f, 0.5f, 0.8f, 0.5f)),
                25000, 3),

            // 第5关：双墙
            LevelConfig(5, Difficulty.EASY,
                PointF(0.1f, 0.5f), PointF(0.9f, 0.5f),
                listOf(Wall(0.35f, 0.0f, 0.35f, 0.4f), Wall(0.65f, 0.6f, 0.65f, 1.0f)),
                25000, 3),

            // 第6关：U形
            LevelConfig(6, Difficulty.EASY,
                PointF(0.2f, 0.9f), PointF(0.8f, 0.9f),
                listOf(Wall(0.5f, 0.3f, 0.5f, 0.9f)),
                25000, 3),

            // 第7关：斜墙
            LevelConfig(7, Difficulty.EASY,
                PointF(0.1f, 0.9f), PointF(0.9f, 0.1f),
                listOf(Wall(0.0f, 0.5f, 0.5f, 0.5f)),
                25000, 3),

            // 第8关：之字形
            LevelConfig(8, Difficulty.EASY,
                PointF(0.1f, 0.9f), PointF(0.9f, 0.1f),
                listOf(Wall(0.25f, 0.3f, 0.25f, 0.7f), Wall(0.75f, 0.3f, 0.75f, 0.7f)),
                25000, 3),

            // 第9关：通道
            LevelConfig(9, Difficulty.EASY,
                PointF(0.1f, 0.5f), PointF(0.9f, 0.5f),
                listOf(Wall(0.5f, 0.0f, 0.5f, 0.35f), Wall(0.5f, 0.65f, 0.5f, 1.0f)),
                25000, 3),

            // 第10关：简单迷宫
            LevelConfig(10, Difficulty.EASY,
                PointF(0.1f, 0.9f), PointF(0.9f, 0.1f),
                listOf(Wall(0.3f, 0.0f, 0.3f, 0.6f), Wall(0.7f, 0.4f, 0.7f, 1.0f)),
                25000, 3)
        )
    }

    // 中等难度关卡（5堵墙）
    private fun getMediumLevels(): List<LevelConfig> {
        return listOf(
            LevelConfig(1, Difficulty.MEDIUM,
                PointF(0.1f, 0.9f), PointF(0.9f, 0.1f),
                listOf(Wall(0.3f, 0.0f, 0.3f, 0.5f), Wall(0.6f, 0.5f, 0.6f, 1.0f),
                    Wall(0.0f, 0.5f, 0.2f, 0.5f)),
                20000, 2),

            LevelConfig(2, Difficulty.MEDIUM,
                PointF(0.5f, 0.9f), PointF(0.5f, 0.1f),
                listOf(Wall(0.2f, 0.3f, 0.8f, 0.3f), Wall(0.2f, 0.6f, 0.8f, 0.6f),
                    Wall(0.35f, 0.0f, 0.35f, 0.3f)),
                20000, 2),

            LevelConfig(3, Difficulty.MEDIUM,
                PointF(0.1f, 0.5f), PointF(0.9f, 0.5f),
                listOf(Wall(0.3f, 0.0f, 0.3f, 0.4f), Wall(0.5f, 0.6f, 0.5f, 1.0f),
                    Wall(0.7f, 0.0f, 0.7f, 0.4f)),
                20000, 2),

            LevelConfig(4, Difficulty.MEDIUM,
                PointF(0.1f, 0.9f), PointF(0.9f, 0.9f),
                listOf(Wall(0.3f, 0.3f, 0.3f, 0.7f), Wall(0.7f, 0.3f, 0.7f, 0.7f),
                    Wall(0.5f, 0.5f, 0.5f, 0.5f)),
                20000, 2),

            LevelConfig(5, Difficulty.MEDIUM,
                PointF(0.1f, 0.1f), PointF(0.9f, 0.9f),
                listOf(Wall(0.25f, 0.25f, 0.25f, 0.75f), Wall(0.75f, 0.25f, 0.75f, 0.75f),
                    Wall(0.0f, 0.5f, 0.25f, 0.5f)),
                20000, 2),

            LevelConfig(6, Difficulty.MEDIUM,
                PointF(0.5f, 0.9f), PointF(0.5f, 0.1f),
                listOf(Wall(0.2f, 0.0f, 0.2f, 0.4f), Wall(0.2f, 0.6f, 0.2f, 1.0f),
                    Wall(0.8f, 0.0f, 0.8f, 0.4f), Wall(0.8f, 0.6f, 0.8f, 1.0f)),
                20000, 2),

            LevelConfig(7, Difficulty.MEDIUM,
                PointF(0.1f, 0.9f), PointF(0.9f, 0.1f),
                listOf(Wall(0.4f, 0.0f, 0.4f, 0.6f), Wall(0.6f, 0.4f, 0.6f, 1.0f),
                    Wall(0.2f, 0.0f, 0.2f, 0.3f)),
                20000, 2),

            LevelConfig(8, Difficulty.MEDIUM,
                PointF(0.2f, 0.5f), PointF(0.8f, 0.5f),
                listOf(Wall(0.1f, 0.2f, 0.9f, 0.2f), Wall(0.1f, 0.8f, 0.9f, 0.8f),
                    Wall(0.5f, 0.2f, 0.5f, 0.4f)),
                20000, 2),

            LevelConfig(9, Difficulty.MEDIUM,
                PointF(0.1f, 0.1f), PointF(0.9f, 0.9f),
                listOf(Wall(0.3f, 0.0f, 0.3f, 0.7f), Wall(0.6f, 0.3f, 0.6f, 1.0f),
                    Wall(0.0f, 0.4f, 0.3f, 0.4f)),
                20000, 2),

            LevelConfig(10, Difficulty.MEDIUM,
                PointF(0.1f, 0.5f), PointF(0.9f, 0.5f),
                listOf(Wall(0.25f, 0.0f, 0.25f, 0.35f), Wall(0.5f, 0.65f, 0.5f, 1.0f),
                    Wall(0.75f, 0.0f, 0.75f, 0.35f), Wall(0.4f, 0.4f, 0.6f, 0.4f)),
                20000, 2)
        )
    }

    // 困难难度关卡（7堵墙）
    private fun getHardLevels(): List<LevelConfig> {
        return listOf(
            LevelConfig(1, Difficulty.HARD,
                PointF(0.1f, 0.9f), PointF(0.9f, 0.1f),
                listOf(Wall(0.3f, 0.0f, 0.3f, 0.5f), Wall(0.6f, 0.5f, 0.6f, 1.0f),
                    Wall(0.0f, 0.3f, 0.3f, 0.3f), Wall(0.7f, 0.7f, 1.0f, 0.7f)),
                15000, 1),

            LevelConfig(2, Difficulty.HARD,
                PointF(0.5f, 0.9f), PointF(0.5f, 0.1f),
                listOf(Wall(0.2f, 0.0f, 0.2f, 0.35f), Wall(0.4f, 0.65f, 0.4f, 1.0f),
                    Wall(0.6f, 0.0f, 0.6f, 0.35f), Wall(0.8f, 0.65f, 0.8f, 1.0f),
                    Wall(0.35f, 0.35f, 0.45f, 0.35f)),
                15000, 1),

            LevelConfig(3, Difficulty.HARD,
                PointF(0.1f, 0.5f), PointF(0.9f, 0.5f),
                listOf(Wall(0.25f, 0.0f, 0.25f, 0.4f), Wall(0.5f, 0.6f, 0.5f, 1.0f),
                    Wall(0.75f, 0.0f, 0.75f, 0.4f), Wall(0.0f, 0.6f, 0.25f, 0.6f)),
                15000, 1),

            LevelConfig(4, Difficulty.HARD,
                PointF(0.1f, 0.9f), PointF(0.9f, 0.9f),
                listOf(Wall(0.35f, 0.3f, 0.35f, 0.7f), Wall(0.65f, 0.3f, 0.65f, 0.7f),
                    Wall(0.15f, 0.5f, 0.35f, 0.5f), Wall(0.65f, 0.5f, 0.85f, 0.5f)),
                15000, 1),

            LevelConfig(5, Difficulty.HARD,
                PointF(0.1f, 0.1f), PointF(0.9f, 0.9f),
                listOf(Wall(0.3f, 0.0f, 0.3f, 0.6f), Wall(0.6f, 0.4f, 0.6f, 1.0f),
                    Wall(0.0f, 0.4f, 0.3f, 0.4f), Wall(0.4f, 0.7f, 0.6f, 0.7f)),
                15000, 1),

            LevelConfig(6, Difficulty.HARD,
                PointF(0.2f, 0.5f), PointF(0.8f, 0.5f),
                listOf(Wall(0.1f, 0.2f, 0.9f, 0.2f), Wall(0.1f, 0.8f, 0.9f, 0.8f),
                    Wall(0.4f, 0.2f, 0.4f, 0.45f), Wall(0.6f, 0.55f, 0.6f, 0.8f),
                    Wall(0.35f, 0.4f, 0.65f, 0.4f)),
                15000, 1),

            LevelConfig(7, Difficulty.HARD,
                PointF(0.1f, 0.9f), PointF(0.9f, 0.1f),
                listOf(Wall(0.25f, 0.0f, 0.25f, 0.5f), Wall(0.5f, 0.5f, 0.5f, 1.0f),
                    Wall(0.75f, 0.0f, 0.75f, 0.5f), Wall(0.0f, 0.3f, 0.25f, 0.3f)),
                15000, 1),

            LevelConfig(8, Difficulty.HARD,
                PointF(0.5f, 0.9f), PointF(0.5f, 0.1f),
                listOf(Wall(0.2f, 0.0f, 0.2f, 0.3f), Wall(0.2f, 0.5f, 0.2f, 0.8f),
                    Wall(0.8f, 0.0f, 0.8f, 0.3f), Wall(0.8f, 0.5f, 0.8f, 0.8f),
                    Wall(0.35f, 0.3f, 0.65f, 0.3f)),
                15000, 1),

            LevelConfig(9, Difficulty.HARD,
                PointF(0.1f, 0.5f), PointF(0.9f, 0.5f),
                listOf(Wall(0.3f, 0.0f, 0.3f, 0.35f), Wall(0.3f, 0.65f, 0.3f, 1.0f),
                    Wall(0.6f, 0.0f, 0.6f, 0.35f), Wall(0.6f, 0.65f, 0.6f, 1.0f),
                    Wall(0.45f, 0.35f, 0.55f, 0.35f)),
                15000, 1),

            LevelConfig(10, Difficulty.HARD,
                PointF(0.1f, 0.1f), PointF(0.9f, 0.9f),
                listOf(Wall(0.25f, 0.25f, 0.25f, 0.75f), Wall(0.75f, 0.25f, 0.75f, 0.75f),
                    Wall(0.0f, 0.5f, 0.25f, 0.5f), Wall(0.75f, 0.5f, 1.0f, 0.5f),
                    Wall(0.4f, 0.0f, 0.4f, 0.25f)),
                15000, 1)
        )
    }

    // 挑战难度关卡（10堵墙）
    private fun getExpertLevels(): List<LevelConfig> {
        return listOf(
            LevelConfig(1, Difficulty.EXPERT,
                PointF(0.1f, 0.9f), PointF(0.9f, 0.1f),
                listOf(Wall(0.2f, 0.0f, 0.2f, 0.4f), Wall(0.4f, 0.6f, 0.4f, 1.0f),
                    Wall(0.6f, 0.0f, 0.6f, 0.4f), Wall(0.8f, 0.6f, 0.8f, 1.0f),
                    Wall(0.0f, 0.3f, 0.2f, 0.3f)),
                12000, 0),

            LevelConfig(2, Difficulty.EXPERT,
                PointF(0.5f, 0.9f), PointF(0.5f, 0.1f),
                listOf(Wall(0.15f, 0.0f, 0.15f, 0.35f), Wall(0.35f, 0.65f, 0.35f, 1.0f),
                    Wall(0.55f, 0.0f, 0.55f, 0.35f), Wall(0.75f, 0.65f, 0.75f, 1.0f),
                    Wall(0.25f, 0.35f, 0.35f, 0.35f), Wall(0.65f, 0.35f, 0.75f, 0.35f)),
                12000, 0),

            LevelConfig(3, Difficulty.EXPERT,
                PointF(0.1f, 0.5f), PointF(0.9f, 0.5f),
                listOf(Wall(0.2f, 0.0f, 0.2f, 0.35f), Wall(0.4f, 0.65f, 0.4f, 1.0f),
                    Wall(0.6f, 0.0f, 0.6f, 0.35f), Wall(0.8f, 0.65f, 0.8f, 1.0f),
                    Wall(0.0f, 0.4f, 0.2f, 0.4f), Wall(0.3f, 0.4f, 0.35f, 0.4f)),
                12000, 0),

            LevelConfig(4, Difficulty.EXPERT,
                PointF(0.1f, 0.9f), PointF(0.9f, 0.9f),
                listOf(Wall(0.3f, 0.3f, 0.3f, 0.6f), Wall(0.6f, 0.4f, 0.6f, 0.7f),
                    Wall(0.15f, 0.45f, 0.3f, 0.45f), Wall(0.6f, 0.45f, 0.75f, 0.45f),
                    Wall(0.45f, 0.6f, 0.55f, 0.6f)),
                12000, 0),

            LevelConfig(5, Difficulty.EXPERT,
                PointF(0.1f, 0.1f), PointF(0.9f, 0.9f),
                listOf(Wall(0.25f, 0.0f, 0.25f, 0.5f), Wall(0.5f, 0.5f, 0.5f, 1.0f),
                    Wall(0.75f, 0.0f, 0.75f, 0.5f), Wall(0.0f, 0.5f, 0.25f, 0.5f),
                    Wall(0.5f, 0.0f, 0.5f, 0.25f)),
                12000, 0),

            LevelConfig(6, Difficulty.EXPERT,
                PointF(0.2f, 0.5f), PointF(0.8f, 0.5f),
                listOf(Wall(0.1f, 0.2f, 0.9f, 0.2f), Wall(0.1f, 0.8f, 0.9f, 0.8f),
                    Wall(0.3f, 0.2f, 0.3f, 0.4f), Wall(0.5f, 0.6f, 0.5f, 0.8f),
                    Wall(0.7f, 0.2f, 0.7f, 0.4f), Wall(0.4f, 0.35f, 0.6f, 0.35f)),
                12000, 0),

            LevelConfig(7, Difficulty.EXPERT,
                PointF(0.1f, 0.9f), PointF(0.9f, 0.1f),
                listOf(Wall(0.2f, 0.0f, 0.2f, 0.45f), Wall(0.45f, 0.55f, 0.45f, 1.0f),
                    Wall(0.7f, 0.0f, 0.7f, 0.45f), Wall(0.0f, 0.35f, 0.2f, 0.35f),
                    Wall(0.3f, 0.45f, 0.45f, 0.45f)),
                12000, 0),

            LevelConfig(8, Difficulty.EXPERT,
                PointF(0.5f, 0.9f), PointF(0.5f, 0.1f),
                listOf(Wall(0.2f, 0.0f, 0.2f, 0.3f), Wall(0.2f, 0.5f, 0.2f, 0.8f),
                    Wall(0.8f, 0.0f, 0.8f, 0.3f), Wall(0.8f, 0.5f, 0.8f, 0.8f),
                    Wall(0.35f, 0.3f, 0.65f, 0.3f), Wall(0.35f, 0.3f, 0.35f, 0.5f)),
                12000, 0),

            LevelConfig(9, Difficulty.EXPERT,
                PointF(0.1f, 0.5f), PointF(0.9f, 0.5f),
                listOf(Wall(0.2f, 0.0f, 0.2f, 0.35f), Wall(0.4f, 0.65f, 0.4f, 1.0f),
                    Wall(0.6f, 0.0f, 0.6f, 0.35f), Wall(0.8f, 0.65f, 0.8f, 1.0f),
                    Wall(0.3f, 0.35f, 0.4f, 0.35f), Wall(0.5f, 0.35f, 0.6f, 0.35f),
                    Wall(0.35f, 0.35f, 0.35f, 0.5f)),
                12000, 0),

            LevelConfig(10, Difficulty.EXPERT,
                PointF(0.1f, 0.1f), PointF(0.9f, 0.9f),
                listOf(Wall(0.25f, 0.25f, 0.25f, 0.75f), Wall(0.75f, 0.25f, 0.75f, 0.75f),
                    Wall(0.0f, 0.5f, 0.25f, 0.5f), Wall(0.75f, 0.5f, 1.0f, 0.5f),
                    Wall(0.4f, 0.0f, 0.4f, 0.25f), Wall(0.6f, 0.75f, 0.6f, 1.0f)),
                12000, 0)
        )
    }
}
