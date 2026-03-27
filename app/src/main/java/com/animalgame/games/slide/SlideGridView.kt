package com.animalgame.games.slide

import android.animation.ValueAnimator
import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.RectF
import android.graphics.drawable.GradientDrawable
import android.util.AttributeSet
import android.view.GestureDetector
import android.view.MotionEvent
import android.view.View
import android.view.animation.DecelerateInterpolator

/**
 * 滑块拼图网格视图 - 儿童卡通风格
 */
class SlideGridView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null
) : View(context, attrs) {

    // 儿童卡通配色 - 明亮活泼
    private val tileColors = listOf(
        Color.parseColor("#FF6B6B"),  // 珊瑚红
        Color.parseColor("#4ECDC4"),  // 清新青
        Color.parseColor("#45B7D1"),  // 天空蓝
        Color.parseColor("#96CEB4"),  // 草地绿
        Color.parseColor("#FFE66D"),  // 向日葵黄
        Color.parseColor("#DDA0DD"),  // 梦幻紫
        Color.parseColor("#F8B500"),  // 阳光橙
        Color.parseColor("#FF8C94")   // 樱花粉
    )

    // 背景色 - 温暖渐变
    private val bgColorTop = Color.parseColor("#FFF8E1")    // 米白色
    private val bgColorBottom = Color.parseColor("#E8F5E9") // 浅绿色

    // 边框和装饰色
    private val borderColor = Color.parseColor("#FFB74D")     // 橙色边框
    private val shadowColor = Color.parseColor("#40000000")   // 阴影

    // 画笔
    private val tilePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.FILL
    }

    private val emptyPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.FILL
        color = Color.parseColor("#F5F5F5")  // 浅灰色空位
    }

    private val textPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.WHITE
        textAlign = Paint.Align.CENTER
        isFakeBoldText = true  // 粗体
    }

    private val bgPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.FILL
    }

    private val highlightPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.STROKE
        strokeWidth = 8f
        color = Color.parseColor("#4CAF50")  // 绿色高亮表示可移动
    }

    private val gridBorderPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.STROKE
        strokeWidth = 12f
        color = borderColor
    }

    // 网格数据
    private var currentState = listOf<Int>()
    private var targetState = listOf<Int>()
    private var canMoveTile = listOf<Boolean>()

    // 动画相关
    private var animatingIndex = -1
    private var animProgress = 0f
    private var animator: ValueAnimator? = null

    // 回调
    var onTileMoved: ((Int) -> Unit)? = null
    var onWin: (() -> Unit)? = null

    // 手势检测
    private val gestureDetector = GestureDetector(context, object : GestureDetector.SimpleOnGestureListener() {
        private val minFlingDistance = 30f

        override fun onDown(e: MotionEvent): Boolean = true

        override fun onSingleTapUp(e: MotionEvent): Boolean {
            handleTap(e.x, e.y)
            return true
        }

        override fun onFling(e1: MotionEvent?, e2: MotionEvent, velocityX: Float, velocityY: Float): Boolean {
            if (width <= 0 || height <= 0) return false

            val dx = e2.x - (e1?.x ?: e2.x)
            val dy = e2.y - (e1?.y ?: e2.y)

            if (kotlin.math.abs(dx) < minFlingDistance && kotlin.math.abs(dy) < minFlingDistance) {
                return false
            }

            if (kotlin.math.abs(dx) > kotlin.math.abs(dy)) {
                if (dx > 0) handleSwipe(Direction.RIGHT) else handleSwipe(Direction.LEFT)
            } else {
                if (dy > 0) handleSwipe(Direction.DOWN) else handleSwipe(Direction.UP)
            }
            return true
        }
    })

    private enum class Direction { LEFT, RIGHT, UP, DOWN }

    private fun handleTap(x: Float, y: Float) {
        if (width <= 0 || height <= 0) return
        val cellSize = width / 3f
        val col = (x / cellSize).toInt().coerceIn(0, 2)
        val row = (y / cellSize).toInt().coerceIn(0, 2)
        val index = row * 3 + col

        val emptyIndex = currentState.indexOf(0)
        val emptyRow = emptyIndex / 3
        val emptyCol = emptyIndex % 3
        val tileRow = index / 3
        val tileCol = index % 3

        if (kotlin.math.abs(tileRow - emptyRow) + kotlin.math.abs(tileCol - emptyCol) == 1) {
            moveTile(index)
        }
    }

    private fun handleSwipe(direction: Direction) {
        val emptyIndex = currentState.indexOf(0)
        val emptyRow = emptyIndex / 3
        val emptyCol = emptyIndex % 3

        val targetIndex = when (direction) {
            Direction.RIGHT -> if (emptyCol > 0) emptyIndex - 1 else -1
            Direction.LEFT -> if (emptyCol < 2) emptyIndex + 1 else -1
            Direction.DOWN -> if (emptyRow > 0) emptyIndex - 3 else -1
            Direction.UP -> if (emptyRow < 2) emptyIndex + 3 else -1
        }

        if (targetIndex in 0..8 && currentState[targetIndex] != 0) {
            moveTile(targetIndex)
        }
    }

    private fun moveTile(index: Int) {
        if (animatingIndex != -1) return
        val emptyIndex = currentState.indexOf(0)
        val tileRow = index / 3
        val tileCol = index % 3
        val emptyRow = emptyIndex / 3
        val emptyCol = emptyIndex % 3
        val distance = kotlin.math.abs(tileRow - emptyRow) + kotlin.math.abs(tileCol - emptyCol)
        if (distance != 1) return

        animatingIndex = index
        animator?.cancel()
        animator = ValueAnimator.ofFloat(0f, 1f).apply {
            duration = 150
            interpolator = DecelerateInterpolator()
            addUpdateListener { animation ->
                animProgress = animation.animatedValue as Float
                if (animProgress >= 1f) {
                    currentState = currentState.toMutableList().apply {
                        this[emptyIndex] = this[index]
                        this[index] = 0
                    }
                    animatingIndex = -1
                    animProgress = 0f
                    updateCanMove()
                    if (currentState == targetState) onWin?.invoke()
                    onTileMoved?.invoke(index)
                }
                invalidate()
            }
            start()
        }
    }

    fun initialize(config: SlideLevelConfig) {
        currentState = config.initialState
        targetState = config.targetState
        updateCanMove()
        post { invalidate() }
    }

    private fun updateCanMove() {
        val emptyIndex = currentState.indexOf(0)
        canMoveTile = List(9) { index ->
            if (currentState[index] == 0) false
            else {
                val emptyRow = emptyIndex / 3
                val emptyCol = emptyIndex % 3
                val tileRow = index / 3
                val tileCol = index % 3
                kotlin.math.abs(tileRow - emptyRow) + kotlin.math.abs(tileCol - emptyCol) == 1
            }
        }
    }

    fun reset(initialState: List<Int>) {
        currentState = initialState
        updateCanMove()
        invalidate()
    }

    override fun onTouchEvent(event: MotionEvent): Boolean {
        return gestureDetector.onTouchEvent(event) || super.onTouchEvent(event)
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)

        if (width <= 0 || height <= 0) return

        val cellSize = width / 3f
        val padding = cellSize * 0.06f
        val tileSize = cellSize - padding * 2

        // 绘制背景渐变
        bgPaint.shader = android.graphics.LinearGradient(
            0f, 0f, 0f, height.toFloat(),
            intArrayOf(bgColorTop, bgColorBottom),
            floatArrayOf(0f, 1f),
            android.graphics.Shader.TileMode.CLAMP
        )
        canvas.drawRect(0f, 0f, width.toFloat(), height.toFloat(), bgPaint)

        // 绘制每个方块
        for (i in 0 until 9) {
            val row = i / 3
            val col = i % 3
            val left = col * cellSize + padding
            val top = row * cellSize + padding

            var drawLeft = left
            var drawTop = top

            // 动画偏移
            if (i == animatingIndex) {
                val emptyIndex = currentState.indexOf(0)
                val fromRow = i / 3
                val fromCol = i % 3
                val toRow = emptyIndex / 3
                val toCol = emptyIndex % 3
                drawLeft += (toCol - fromCol) * cellSize * animProgress
                drawTop += (toRow - fromRow) * cellSize * animProgress
            }

            val tileRect = RectF(drawLeft, drawTop, drawLeft + tileSize, drawTop + tileSize)
            val radius = tileSize * 0.2f

            val tileValue = if (i == animatingIndex) currentState[i] else currentState.getOrElse(i) { 0 }

            if (tileValue == 0) {
                // 空位 - 浅色
                canvas.drawRoundRect(tileRect, radius, radius, emptyPaint)
            } else {
                // 方块 - 卡通风格
                tilePaint.color = tileColors[(tileValue - 1) % tileColors.size]
                canvas.drawRoundRect(tileRect, radius, radius, tilePaint)

                // 绘制高光效果
                val highlightRect = RectF(
                    tileRect.left + tileSize * 0.1f,
                    tileRect.top + tileSize * 0.1f,
                    tileRect.right - tileSize * 0.3f,
                    tileRect.top + tileSize * 0.25f
                )
                val highlightPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                    color = Color.WHITE
                    alpha = 60
                }
                canvas.drawRoundRect(highlightRect, radius, radius, highlightPaint)

                // 绘制数字
                textPaint.textSize = tileSize * 0.5f
                val textY = tileRect.centerY() - (textPaint.descent() + textPaint.ascent()) / 2
                canvas.drawText(tileValue.toString(), tileRect.centerX(), textY, textPaint)
            }

            // 可移动高亮 - 绿色边框
            if (canMoveTile.getOrElse(i) { false } && i != animatingIndex) {
                canvas.drawRoundRect(tileRect, radius, radius, highlightPaint)
            }
        }
    }

    override fun onDetachedFromWindow() {
        super.onDetachedFromWindow()
        animator?.cancel()
    }
}