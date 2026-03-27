package com.animalgame.games.gravity

import android.graphics.PointF

/**
 * 平衡小球游戏关卡配置
 */
data class LevelConfig(
    val level: Int,                    // 关卡号（难度内）
    val difficulty: Difficulty,       // 难度
    val startPoint: PointF,            // 起点坐标 (0-1范围，相对屏幕)
    val endPoint: PointF,              // 终点坐标
    val walls: List<Wall>,             // 墙体列表
    val timeLimit: Long = 20000L,      // 时间限制(ms)
    val tolerance: Int = 2             // 容错次数（撞墙允许次数）
)

/**
 * 墙体线段
 */
data class Wall(
    val x1: Float, val y1: Float,      // 起点 (0-1范围)
    val x2: Float, val y2: Float       // 终点
)

/**
 * 难度配置
 */
enum class Difficulty(
    val levelCount: Int,
    val wallCount: Int,               // 墙体数量
    val tolerance: Int,               // 容错次数
    val timeLimit: Long,              // 时间限制(ms)
    val displayName: String
) {
    EASY(10, 3, 3, 25000L, "简单"),       // 简单：3堵墙，3次容错，25秒
    MEDIUM(10, 5, 2, 20000L, "中等"),     // 中等：5堵墙，2次容错，20秒
    HARD(10, 7, 1, 15000L, "困难"),      // 困难：7堵墙，1次容错，15秒
    EXPERT(10, 10, 0, 12000L, "挑战")    // 挑战：10堵墙，0次容错，12秒
}

/**
 * 碰撞类型
 */
enum class CollisionType {
    NONE,       // 无碰撞
    WALL,       // 撞墙
    BOUNDARY,   // 边界
    GOAL        // 到达终点
}
