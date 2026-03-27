package com.animalgame.games.gravity

import android.content.Context
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager

/**
 * 重力传感器管理
 * 读取加速度计并应用低通滤波平滑数据
 */
class GravitySensor(context: Context) : SensorEventListener {

    private val sensorManager = context.getSystemService(Context.SENSOR_SERVICE) as SensorManager
    private val accelerometer = sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER)

    // 当前重力值（已滤波）
    var gravityX: Float = 0f
    var gravityY: Float = 0f

    // 原始值（用于调试）
    var rawX: Float = 0f
    var rawY: Float = 0f

    // 传感器是否可用
    val isAvailable: Boolean = accelerometer != null

    // 低通滤波系数 (0-1, 越大越平滑)
    private val ALPHA = 0.8f

    /**
     * 开始监听传感器
     */
    fun start() {
        if (isAvailable) {
            sensorManager.registerListener(
                this,
                accelerometer,
                SensorManager.SENSOR_DELAY_GAME
            )
        }
    }

    /**
     * 停止监听传感器
     */
    fun stop() {
        if (isAvailable) {
            sensorManager.unregisterListener(this)
        }
    }

    /**
     * 重置重力值
     */
    fun reset() {
        gravityX = 0f
        gravityY = 0f
        rawX = 0f
        rawY = 0f
    }

    override fun onSensorChanged(event: SensorEvent) {
        if (event.sensor.type == Sensor.TYPE_ACCELEROMETER) {
            // 保存原始值
            rawX = event.values[0]
            rawY = event.values[1]

            // 低通滤波去除抖动
            gravityX = gravityX * ALPHA + rawX * (1 - ALPHA)
            gravityY = gravityY * ALPHA + rawY * (1 - ALPHA)
        }
    }

    override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) {
        // 不需要处理
    }
}
