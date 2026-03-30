package com.animalgame.games.mirrordraw

import com.animalgame.core.game.AbstractGameModule
import com.animalgame.core.game.ActionResult
import com.animalgame.core.game.GameAction
import com.animalgame.core.game.GameState
import com.animalgame.core.manager.GameRegistry
import kotlin.math.abs
import kotlin.math.sqrt
import kotlin.random.Random

/**
 * 镜像绘图（Mirror Draw）游戏模块
 * 玩法：在右侧画出左侧路径的水平镜像
 */

// ==================== 数据结构 ====================

data class PointF(
    val x: Float,
    val y: Float
) {
    fun distanceTo(other: PointF): Float {
        val dx = x - other.x
        val dy = y - other.y
        return sqrt(dx * dx + dy * dy)
    }
}

enum class ComparisonResult {
    Excellent,
    Good,
    Partial,
    NeedImprovement,
    Incomplete
}

// ==================== 游戏模块 ====================

class MirrorDrawGameModule : AbstractGameModule() {

    override val gameId: String = "mirror_draw"
    override val gameName: String = "镜像绘图"
    override val iconAsset: String = "logo.png"
    override val totalLevels: Int = 15
    override val description: String = "画出路径的水平镜像"

    // 游戏数据
    private var targetPath = listOf<PointF>()           // 左侧目标路径 (相对坐标 0-1)
    private var mirroredPath = listOf<PointF>()         // 右侧镜像目标路径 (相对坐标 0-1)
    private var playerPath = mutableListOf<PointF>()    // 玩家绘制路径 (相对坐标 0-1)
    private var currentComparisonResult = ComparisonResult.Incomplete
    private var errorCount = 0
    private var gameStartTime = 0L
    private var isDrawing = false

    // Canvas 尺寸（像素）
    private var canvasWidth: Float = 1f
    private var canvasHeight: Float = 1f

    private val DEBUG = true

    companion object {
        const val INITIAL_TIME = 60000L
        const val TIME_PENALTY = 5000L
    }

    // ==================== 难度配置 ====================

    private fun getPointCount(level: Int): Int {
        return when {
            level <= 5 -> 3 + (level - 1) / 2
            level <= 10 -> 5 + (level - 5) / 3
            else -> 7 + (level - 10) / 5
        }.coerceIn(3, 8)
    }

    /**
     * 获取误差阈值（相对坐标）
     * easy: 0.12 (约10%屏幕宽度)
     * medium: 0.08
     * hard: 0.05
     */
    private fun getToleranceThreshold(): Float {
        return when {
            currentLevel <= 5 -> 0.12f
            currentLevel <= 10 -> 0.08f
            else -> 0.05f
        }
    }

    // ==================== 路径生成 ====================

    /**
     * 生成左侧目标路径（相对坐标 0-1）
     */
    private fun generateTargetPath(pointCount: Int): List<PointF> {
        val leftBound = 0.08f
        val rightBound = 0.38f
        val topMargin = 0.12f
        val bottomMargin = 0.12f
        val usableHeight = 1f - topMargin - bottomMargin
        val segmentHeight = usableHeight / (pointCount + 1)

        val points = mutableListOf<PointF>()
        var currentY = topMargin + segmentHeight

        for (i in 0 until pointCount) {
            val x = Random.nextFloat() * (rightBound - leftBound) + leftBound
            val y = currentY + Random.nextFloat() * segmentHeight * 0.3f - segmentHeight * 0.15f
            points.add(PointF(x, y.coerceIn(topMargin, 1f - bottomMargin)))
            currentY += segmentHeight
        }

        return points
    }

    /**
     * 计算镜像路径（相对坐标）
     */
    private fun calculateMirroredPath(path: List<PointF>): List<PointF> {
        return path.map { PointF(1f - it.x, it.y) }
    }

    // ==================== 路径采样 ====================

    /**
     * 将路径采样为指定数量的点
     */
    private fun samplePath(path: List<PointF>, sampleCount: Int): List<PointF> {
        if (path.size < 2) return path
        if (path.size == sampleCount) return path

        val result = mutableListOf<PointF>()
        val totalLength = calculatePathLength(path)
        val segmentLength = totalLength / (sampleCount - 1)

        result.add(path.first())

        var currentDist = 0f
        var pathIndex = 0
        var accumulatedDist = 0f

        while (result.size < sampleCount - 1 && pathIndex < path.size - 1) {
            val p1 = path[pathIndex]
            val p2 = path[pathIndex + 1]
            val segmentDist = p1.distanceTo(p2)

            if (accumulatedDist + segmentDist >= segmentLength * result.size) {
                // 在这个线段上采样
                val ratio = (segmentLength * result.size - accumulatedDist) / segmentDist
                val sampledX = p1.x + (p2.x - p1.x) * ratio
                val sampledY = p1.y + (p2.y - p1.y) * ratio
                result.add(PointF(sampledX, sampledY))
            }

            accumulatedDist += segmentDist
            pathIndex++
        }

        // 确保最后一个点是路径终点
        while (result.size < sampleCount) {
            result.add(path.last())
        }

        return result.take(sampleCount)
    }

    /**
     * 计算路径总长度
     */
    private fun calculatePathLength(path: List<PointF>): Float {
        var length = 0f
        for (i in 0 until path.size - 1) {
            length += path[i].distanceTo(path[i + 1])
        }
        return length
    }

    // ==================== 路径比对 ====================

    /**
     * 比对玩家路径与目标镜像路径
     * 使用采样算法进行比对
     */
    private fun comparePaths(): ComparisonResult {
        if (playerPath.size < 2) return ComparisonResult.Incomplete

        val sampleCount = mirroredPath.size.coerceAtLeast(3)
        val playerSamples = samplePath(playerPath.toList(), sampleCount)
        val targetSamples = samplePath(mirroredPath, sampleCount)

        var totalError = 0f
        for (i in playerSamples.indices) {
            totalError += playerSamples[i].distanceTo(targetSamples[i])
        }
        val avgError = totalError / sampleCount

        val threshold = getToleranceThreshold()

        if (DEBUG) {
            println("MirrorDraw: avgError=${String.format("%.4f", avgError)} threshold=${String.format("%.4f", threshold)}")
        }

        return when {
            avgError < threshold * 0.4f -> ComparisonResult.Excellent
            avgError < threshold -> ComparisonResult.Good
            avgError < threshold * 1.8f -> ComparisonResult.Partial
            else -> ComparisonResult.NeedImprovement
        }
    }

    // ==================== 公共方法 ====================

    fun setCanvasSize(width: Float, height: Float) {
        canvasWidth = width
        canvasHeight = height
        if (DEBUG) {
            println("MirrorDraw: Canvas size = ${width}x${height}")
        }
    }

    fun getTargetPath(): List<PointF> = targetPath
    fun getMirroredPath(): List<PointF> = mirroredPath
    fun getPlayerPath(): List<PointF> = playerPath.toList()
    fun getComparisonResult(): ComparisonResult = currentComparisonResult

    fun getDrawingState(): Boolean = isDrawing

    // ==================== 游戏生命周期 ====================

    override fun start(level: Int) {
        currentLevel = level.coerceIn(1, totalLevels)
        currentScore = 0
        mistakeCount = 0
        errorCount = 0
        playerPath.clear()
        currentComparisonResult = ComparisonResult.Incomplete
        isDrawing = false

        startGame()
    }

    override fun startGame() {
        val pointCount = getPointCount(currentLevel)
        targetPath = generateTargetPath(pointCount)
        mirroredPath = calculateMirroredPath(targetPath)

        if (DEBUG) {
            println("MirrorDraw: Level ${currentLevel}, points = ${targetPath.size}")
            println("MirrorDraw: Target = ${targetPath.joinToString { "(${String.format("%.2f", it.x)}, ${String.format("%.2f", it.y)})" }}")
            println("MirrorDraw: Mirror = ${mirroredPath.joinToString { "(${String.format("%.2f", it.x)}, ${String.format("%.2f", it.y)})" }}")
        }

        gameStartTime = System.currentTimeMillis()
        updatePlayingState()
    }

    private fun updatePlayingState() {
        _state.value = GameState.Playing(
            level = currentLevel,
            elapsedTime = System.currentTimeMillis() - gameStartTime,
            score = currentScore,
            data = buildGameData()
        )
    }

    private fun buildGameData(): Map<String, Any> {
        return mapOf(
            "targetPath" to targetPath.map { mapOf("x" to it.x, "y" to it.y) },
            "mirroredPath" to mirroredPath.map { mapOf("x" to it.x, "y" to it.y) },
            "playerPath" to playerPath.map { mapOf("x" to it.x, "y" to it.y) },
            "comparisonResult" to currentComparisonResult.name,
            "isDrawing" to isDrawing
        )
    }

    override fun onUserAction(action: GameAction): ActionResult? {
        when (action) {
            is GameAction.Tap -> {
                // 点击用于开始绘制或继续
                return null
            }
            else -> return super.onUserAction(action)
        }
    }

    /**
     * 开始绘制（手指按下）
     */
    fun startDrawing(x: Float, y: Float) {
        val currentState = _state.value
        if (currentState !is GameState.Playing) return

        // 转换为相对坐标
        if (canvasWidth <= 0 || canvasHeight <= 0) return

        val relativeX = (x / canvasWidth).coerceIn(0f, 1f)
        val relativeY = (y / canvasHeight).coerceIn(0f, 1f)

        // 只允许在右侧区域开始
        if (relativeX <= 0.5f) return

        // 清空之前的路径
        playerPath.clear()
        playerPath.add(PointF(relativeX, relativeY))
        isDrawing = true

        if (DEBUG) {
            println("MirrorDraw: Start drawing at (${String.format("%.2f", relativeX)}, ${String.format("%.2f", relativeY)})")
        }

        updatePlayingState()
    }

    /**
     * 继续绘制（手指移动）
     */
    fun continueDrawing(x: Float, y: Float) {
        if (!isDrawing) return

        val currentState = _state.value
        if (currentState !is GameState.Playing) return

        val relativeX = (x / canvasWidth).coerceIn(0f, 1f)
        val relativeY = (y / canvasHeight).coerceIn(0f, 1f)

        // 只允许在右侧区域绘制
        if (relativeX <= 0.5f) return

        // 添加点（避免点太密集）
        val lastPoint = playerPath.last()
        val dist = kotlin.math.sqrt(
            (relativeX - lastPoint.x) * (relativeX - lastPoint.x) +
            (relativeY - lastPoint.y) * (relativeY - lastPoint.y)
        )

        // 只有移动了一定距离才添加点
        if (dist > 0.01f) {  // 约1%屏幕宽度
            playerPath.add(PointF(relativeX, relativeY))

            // 实时比对
            currentComparisonResult = comparePaths()

            if (DEBUG) {
                println("MirrorDraw: Drawing point (${String.format("%.2f", relativeX)}, ${String.format("%.2f", relativeY)}) result=$currentComparisonResult")
            }

            updatePlayingState()
        }
    }

    /**
     * 结束绘制（手指抬起）
     */
    fun endDrawing() {
        if (!isDrawing) return

        isDrawing = false

        val currentState = _state.value
        if (currentState !is GameState.Playing) return

        // 最终比对
        currentComparisonResult = comparePaths()

        if (DEBUG) {
            println("MirrorDraw: End drawing, result=$currentComparisonResult playerPoints=${playerPath.size}")
        }

        // 检查是否完成
        if (currentComparisonResult == ComparisonResult.Excellent || currentComparisonResult == ComparisonResult.Good) {
            // 完成本关
            val timeUsed = System.currentTimeMillis() - gameStartTime
            val stars = calculateStars(timeUsed, errorCount, currentLevel)
            currentScore += 50 + maxOf(0, 100 - errorCount * 10)

            completeLevel(
                isSuccess = true,
                timeMillis = timeUsed,
                score = currentScore,
                stars = stars
            )
        } else {
            // 未完成，可以继续重画
            updatePlayingState()
        }
    }

    /**
     * 清除当前绘制
     */
    fun clearDrawing() {
        playerPath.clear()
        currentComparisonResult = ComparisonResult.Incomplete
        isDrawing = false
        updatePlayingState()
    }

    /**
     * 游戏失败（时间耗尽）
     */
    private fun gameOver() {
        val timeUsed = System.currentTimeMillis() - gameStartTime

        _state.value = GameState.Completed(
            level = currentLevel,
            isSuccess = false,
            timeMillis = timeUsed,
            score = currentScore,
            stars = 1
        )

        _result.value = com.animalgame.core.game.GameResult(
            gameId = gameId,
            level = currentLevel,
            isSuccess = false,
            timeMillis = timeUsed,
            score = currentScore,
            stars = 1,
            mistakes = mistakeCount
        )
    }

    override fun calculateStars(timeMillis: Long, errors: Int, level: Int): Int {
        val timeBonus = maxOf(0, (INITIAL_TIME - timeMillis) / 1000)
        val baseScore = timeBonus * 2 - errors * 10 + level * 5

        return when {
            baseScore >= 80 -> 3
            baseScore >= 50 -> 2
            else -> 1
        }
    }

    // ==================== 公共方法 ====================

    fun getDifficultyName(): String {
        return when {
            currentLevel <= 5 -> "入门"
            currentLevel <= 10 -> "进阶"
            else -> "挑战"
        }
    }

    fun getLevelIndex(): Int {
        return ((currentLevel - 1) % 10) + 1
    }

    fun restartLevel() {
        start(currentLevel)
    }

    fun resetToIdle() {
        _state.value = GameState.Idle
    }

    override fun destroy() {
        super.destroy()
    }
}

// ==================== 注册 ====================

fun registerMirrorDrawGame() {
    GameRegistry.register(MirrorDrawGameModule())
}