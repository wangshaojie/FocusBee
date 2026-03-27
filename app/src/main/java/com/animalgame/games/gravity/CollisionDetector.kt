package com.animalgame.games.gravity

import android.graphics.PointF
import kotlin.math.sqrt

/**
 * 碰撞检测器
 * 检测小球与墙体、边界、终点的碰撞
 */
class CollisionDetector(
    private val levelConfig: LevelConfig,
    private val screenWidth: Int,
    private val screenHeight: Int
) {

    companion object {
        const val BALL_RADIUS = 30f          // 小球半径(像素)
        const val GOAL_RADIUS = 40f         // 终点半径(像素)
        const val WALL_THICKNESS = 20f      // 墙体检测厚度
    }

    /**
     * 检测碰撞类型
     * @param x 小球中心X坐标(像素)
     * @param y 小球中心Y坐标(像素)
     * @return 碰撞类型
     */
    fun checkCollision(x: Float, y: Float): CollisionType {

        // 1. 检查是否到达终点（优先检查）
        if (isNearGoal(x, y)) {
            return CollisionType.GOAL
        }

        // 2. 检查边界碰撞
        if (isOutOfBounds(x, y)) {
            return CollisionType.BOUNDARY
        }

        // 3. 检查墙体碰撞
        for (wall in levelConfig.walls) {
            if (isCollidingWithWall(x, y, wall)) {
                return CollisionType.WALL
            }
        }

        return CollisionType.NONE
    }

    /**
     * 检查是否到达终点
     */
    private fun isNearGoal(x: Float, y: Float): Boolean {
        val endX = levelConfig.endPoint.x * screenWidth
        val endY = levelConfig.endPoint.y * screenHeight
        val distance = distance(x, y, endX, endY)
        return distance < GOAL_RADIUS + BALL_RADIUS
    }

    /**
     * 检查是否超出边界
     */
    private fun isOutOfBounds(x: Float, y: Float): Boolean {
        return x < BALL_RADIUS || x > screenWidth - BALL_RADIUS ||
                y < BALL_RADIUS || y > screenHeight - BALL_RADIUS
    }

    /**
     * 检查是否与墙体碰撞
     */
    private fun isCollidingWithWall(x: Float, y: Float, wall: Wall): Boolean {
        val x1 = wall.x1 * screenWidth
        val y1 = wall.y1 * screenHeight
        val x2 = wall.x2 * screenWidth
        val y2 = wall.y2 * screenHeight

        val dist = pointToLineDistance(x, y, x1, y1, x2, y2)
        return dist < BALL_RADIUS + WALL_THICKNESS / 2
    }

    /**
     * 计算两点间距离
     */
    private fun distance(x1: Float, y1: Float, x2: Float, y2: Float): Float {
        val dx = x2 - x1
        val dy = y2 - y1
        return sqrt(dx * dx + dy * dy)
    }

    /**
     * 计算点到线段的最短距离
     */
    private fun pointToLineDistance(
        px: Float, py: Float,
        x1: Float, y1: Float,
        x2: Float, y2: Float
    ): Float {
        val A = px - x1
        val B = py - y1
        val C = x2 - x1
        val D = y2 - y1

        val dot = A * C + B * D
        val lenSq = C * C + D * D

        var param = -1f
        if (lenSq != 0f) {
            param = dot / lenSq
        }

        val xx: Float
        val yy: Float

        if (param < 0f) {
            xx = x1
            yy = y1
        } else if (param > 1f) {
            xx = x2
            yy = y2
        } else {
            xx = x1 + param * C
            yy = y1 + param * D
        }

        return distance(px, py, xx, yy)
    }

    /**
     * 获取终点坐标（像素）
     */
    fun getGoalPosition(): PointF {
        return PointF(
            levelConfig.endPoint.x * screenWidth,
            levelConfig.endPoint.y * screenHeight
        )
    }

    /**
     * 获取起点坐标（像素）
     */
    fun getStartPosition(): PointF {
        return PointF(
            levelConfig.startPoint.x * screenWidth,
            levelConfig.startPoint.y * screenHeight
        )
    }
}
