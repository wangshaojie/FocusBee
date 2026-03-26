package com.animalgame.core.model

/**
 * 统一游戏结果模型
 */
data class GameResult(
    val gameId: String,
    val level: Int,
    val score: Int = 0,
    val stars: Int = 0,
    val isCompleted: Boolean = false,
    val timeMillis: Long = 0L,
    val mistakes: Int = 0
) {
    companion object {
        fun calculateStars(timeMillis: Long, mistakes: Int, baseStars: Int): Int {
            // 基础1星，每10秒内完成+1星，无错误+1星
            var stars = 1
            if (timeMillis < 10000) stars++
            if (mistakes == 0) stars++
            return minOf(stars, 3) // 最多3星
        }
    }
}
