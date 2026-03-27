package com.animalgame.games.slide

/**
 * 关卡管理器
 * 生成有解的滑块拼图关卡
 */
object SlideLevelManager {

    // 生成初始状态（使用随机移动模拟打乱，确保有解）
    private fun shuffleTarget(target: List<Int>): List<Int> {
        // 先把目标状态调整为空位在左上角
        val adjusted = target.toMutableList()
        val zeroIdx = adjusted.indexOf(0)
        if (zeroIdx != 0 && zeroIdx != -1) {
            // 交换到位置0
            val temp = adjusted[0]
            adjusted[0] = 0
            adjusted[zeroIdx] = temp
        }

        // 随机打乱（使用移动模拟）
        var state = adjusted.toMutableList()
        var emptyIdx = 0
        var moves = 0
        val maxMoves = 25

        val rand = java.util.Random()

        while (moves < maxMoves) {
            val row = emptyIdx / 3
            val col = emptyIdx % 3
            val possibleMoves = mutableListOf<Int>()

            if (row > 0) possibleMoves.add(emptyIdx - 3)
            if (row < 2) possibleMoves.add(emptyIdx + 3)
            if (col > 0) possibleMoves.add(emptyIdx - 1)
            if (col < 2) possibleMoves.add(emptyIdx + 1)

            val moveTo = possibleMoves[rand.nextInt(possibleMoves.size)]
            state[emptyIdx] = state[moveTo]
            state[moveTo] = 0
            emptyIdx = moveTo
            moves++
        }

        return state
    }

    // 不同关卡的目标状态（递增难度）
    private fun getTargetState(levelIndex: Int): List<Int> {
        // 目标状态：空位在左上角，按顺序排列
        return when (levelIndex % 10) {
            0 -> listOf(0, 1, 2, 3, 4, 5, 6, 7, 8)
            1 -> listOf(0, 1, 2, 3, 4, 5, 6, 7, 8)
            2 -> listOf(0, 1, 2, 3, 4, 5, 6, 7, 8)
            3 -> listOf(0, 1, 2, 3, 4, 5, 6, 7, 8)
            4 -> listOf(0, 1, 2, 3, 4, 5, 6, 7, 8)
            5 -> listOf(0, 1, 2, 3, 4, 5, 6, 7, 8)
            6 -> listOf(0, 1, 2, 3, 4, 5, 6, 7, 8)
            7 -> listOf(0, 1, 2, 3, 4, 5, 6, 7, 8)
            8 -> listOf(0, 1, 2, 3, 4, 5, 6, 7, 8)
            9 -> listOf(0, 1, 2, 3, 4, 5, 6, 7, 8)
            else -> listOf(0, 1, 2, 3, 4, 5, 6, 7, 8)
        }
    }

    /**
     * 获取指定难度和关卡索引的关卡配置
     */
    fun getLevel(difficulty: SlideDifficulty, levelIndex: Int): SlideLevelConfig {
        val targetState = getTargetState(levelIndex)
        val initialState = shuffleTarget(targetState)

        // 计算最优步数
        val optimalSteps = calculateOptimalSteps(initialState, targetState)

        return SlideLevelConfig(
            level = levelIndex + 1,
            difficulty = difficulty,
            initialState = initialState,
            targetState = targetState,
            optimalSteps = optimalSteps.coerceIn(4, 20)
        )
    }

    /**
     * 获取某难度下的总关卡数
     */
    fun getLevelCount(difficulty: SlideDifficulty): Int = difficulty.levelCount
}