package com.animalgame.games.gravity

import android.graphics.PointF

/**
 * 物理引擎
 * 简单的欧拉积分物理模拟，不使用重型物理引擎
 */
class PhysicsEngine {

    // 当前速度
    val velocity = PointF(0f, 0f)

    // 物理参数
    var speedFactor: Float = 30f       // 灵敏度系数
    var maxSpeed: Float = 800f        // 最大速度限制
    var friction: Float = 0.95f        // 摩擦系数 (0-1, 越小减速越快)

    /**
     * 更新物理状态
     * @param gravityX 传感器X轴重力
     * @param gravityY 传感器Y轴重力
     * @param deltaTime 帧间隔时间(秒)
     * @return 新的位置偏移量
     */
    fun update(gravityX: Float, gravityY: Float, deltaTime: Float): PointF {

        // 1. 应用重力加速度到速度
        velocity.x += gravityX * speedFactor * deltaTime
        velocity.y += gravityY * speedFactor * deltaTime

        // 2. 应用摩擦力
        velocity.x *= friction
        velocity.y *= friction

        // 3. 速度限制
        velocity.x = velocity.x.coerceIn(-maxSpeed, maxSpeed)
        velocity.y = velocity.y.coerceIn(-maxSpeed, maxSpeed)

        // 4. 计算位置偏移
        val deltaX = velocity.x * deltaTime
        val deltaY = velocity.y * deltaTime

        return PointF(deltaX, deltaY)
    }

    /**
     * 边界反弹
     * 当小球碰到边界时反转速度
     */
    fun bounceOffBoundary(isLeftRight: Boolean) {
        if (isLeftRight) {
            velocity.x = -velocity.x * 0.5f  // 反弹并损失50%速度
        } else {
            velocity.y = -velocity.y * 0.5f
        }
    }

    /**
     * 撞墙反弹
     */
    fun bounceOffWall() {
        // 撞墙后速度衰减
        velocity.x *= -0.3f
        velocity.y *= -0.3f
    }

    /**
     * 重置物理状态
     */
    fun reset() {
        velocity.set(0f, 0f)
    }

    /**
     * 设置灵敏度
     */
    fun setSensitivity(sensitivity: Float) {
        speedFactor = sensitivity
    }

    /**
     * 获取当前速度大小
     */
    fun getSpeed(): Float {
        return kotlin.math.sqrt(velocity.x * velocity.x + velocity.y * velocity.y)
    }
}
