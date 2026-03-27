package com.animalgame.games.gravity

import android.graphics.Canvas
import android.graphics.Color
import android.graphics.DashPathEffect
import android.graphics.Paint
import android.graphics.PointF

/**
 * 关卡渲染器
 * 使用 Canvas 绘制游戏元素，60FPS 优化
 */
class LevelRenderer {

    // 墙体画笔（复用）
    private val wallPaint = Paint().apply {
        color = Color.parseColor("#5C6BC0")
        strokeWidth = 12f
        style = Paint.Style.STROKE
        isAntiAlias = true
        strokeCap = Paint.Cap.ROUND
    }

    // 墙体背景线（虚线）
    private val wallBackgroundPaint = Paint().apply {
        color = Color.parseColor("#D1C4E9")
        strokeWidth = 20f
        style = Paint.Style.STROKE
        isAntiAlias = true
        pathEffect = DashPathEffect(floatArrayOf(20f, 10f), 0f)
    }

    // 小球画笔
    private val ballPaint = Paint().apply {
        color = Color.parseColor("#FF5722")
        style = Paint.Style.FILL
        isAntiAlias = true
    }

    // 小球高光
    private val ballHighlightPaint = Paint().apply {
        color = Color.parseColor("#FF8A65")
        style = Paint.Style.FILL
        isAntiAlias = true
    }

    // 起点画笔
    private val startPaint = Paint().apply {
        color = Color.parseColor("#4CAF50")
        style = Paint.Style.FILL
        isAntiAlias = true
    }

    // 起点文字
    private val startTextPaint = Paint().apply {
        color = Color.WHITE
        textSize = 24f
        textAlign = Paint.Align.CENTER
        isAntiAlias = true
    }

    // 终点画笔
    private val endPaint = Paint().apply {
        color = Color.parseColor("#FFC107")
        style = Paint.Style.FILL
        isAntiAlias = true
    }

    // 终点文字
    private val endTextPaint = Paint().apply {
        color = Color.parseColor("#5D4037")
        textSize = 24f
        textAlign = Paint.Align.CENTER
        isAntiAlias = true
    }

    // 轨迹画笔（显示小球运动轨迹）
    private val trailPaint = Paint().apply {
        color = Color.parseColor("#FFCCBC")
        strokeWidth = 8f
        style = Paint.Style.STROKE
        isAntiAlias = true
    }

    companion object {
        const val BALL_RADIUS = 30f
        const val GOAL_RADIUS = 40f
        const val START_RADIUS = 40f
    }

    /**
     * 渲染游戏画面
     */
    fun render(
        canvas: Canvas,
        levelConfig: LevelConfig,
        ballX: Float,
        ballY: Float,
        trailPositions: List<PointF> = emptyList()
    ) {
        val width = canvas.width.toFloat()
        val height = canvas.height.toFloat()

        // 1. 清屏（背景色）
        canvas.drawColor(Color.parseColor("#F8F6FF"))

        // 2. 绘制轨迹
        if (trailPositions.isNotEmpty()) {
            for (i in 1 until trailPositions.size) {
                val prev = trailPositions[i - 1]
                val curr = trailPositions[i]
                canvas.drawLine(prev.x, prev.y, curr.x, curr.y, trailPaint)
            }
        }

        // 3. 绘制起点（绿色圆）
        val startX = levelConfig.startPoint.x * width
        val startY = levelConfig.startPoint.y * height
        canvas.drawCircle(startX, startY, START_RADIUS, startPaint)
        canvas.drawText("起", startX, startY + 8f, startTextPaint)

        // 4. 绘制终点（黄色圆）
        val endX = levelConfig.endPoint.x * width
        val endY = levelConfig.endPoint.y * height
        canvas.drawCircle(endX, endY, GOAL_RADIUS, endPaint)
        canvas.drawText("终", endX, endY + 8f, endTextPaint)

        // 5. 绘制墙体
        for (wall in levelConfig.walls) {
            val x1 = wall.x1 * width
            val y1 = wall.y1 * height
            val x2 = wall.x2 * width
            val y2 = wall.y2 * height

            // 先画背景线
            canvas.drawLine(x1, y1, x2, y2, wallBackgroundPaint)
            // 再画主体线
            canvas.drawLine(x1, y1, x2, y2, wallPaint)
        }

        // 6. 绘制小球
        canvas.drawCircle(ballX, ballY, BALL_RADIUS, ballPaint)

        // 小球高光（左上角）
        canvas.drawCircle(ballX - 8f, ballY - 8f, 10f, ballHighlightPaint)
    }

    /**
     * 渲染失败状态
     */
    fun renderFailed(canvas: Canvas, message: String = "撞墙了!") {
        val width = canvas.width.toFloat()
        val height = canvas.height.toFloat()

        // 半透明遮罩
        val overlayPaint = Paint().apply {
            color = Color.parseColor("#80FF0000")
            style = Paint.Style.FILL
        }
        canvas.drawRect(0f, 0f, width, height, overlayPaint)

        // 失败文字
        val failPaint = Paint().apply {
            color = Color.WHITE
            textSize = 48f
            textAlign = Paint.Align.CENTER
            isAntiAlias = true
        }
        canvas.drawText(message, width / 2, height / 2, failPaint)
    }

    /**
     * 渲染成功状态
     */
    fun renderSuccess(canvas: Canvas, timeMillis: Long) {
        val width = canvas.width.toFloat()
        val height = canvas.height.toFloat()

        // 半透明遮罩
        val overlayPaint = Paint().apply {
            color = Color.parseColor("#804CAF50")
            style = Paint.Style.FILL
        }
        canvas.drawRect(0f, 0f, width, height, overlayPaint)

        // 成功文字
        val successPaint = Paint().apply {
            color = Color.WHITE
            textSize = 48f
            textAlign = Paint.Align.CENTER
            isAntiAlias = true
        }
        canvas.drawText("过关!", width / 2, height / 2, successPaint)

        // 用时
        val timePaint = Paint().apply {
            color = Color.WHITE
            textSize = 24f
            textAlign = Paint.Align.CENTER
            isAntiAlias = true
        }
        canvas.drawText("用时: ${timeMillis / 1000}秒", width / 2, height / 2 + 50f, timePaint)
    }
}
