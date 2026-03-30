package com.animalgame.games.colorburst

import com.animalgame.core.game.AbstractGameModule
import com.animalgame.core.game.ActionResult
import com.animalgame.core.game.GameAction
import com.animalgame.core.game.GameState
import com.animalgame.core.manager.GameRegistry
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlin.math.sqrt
import kotlin.random.Random

/**
 * 色彩突围（Color Burst）游戏模块
 */

// ==================== 数据结构 ====================

data class HSLColor(
    val h: Float,
    val s: Float,
    val l: Float
) {
    fun toComposeColor(): androidx.compose.ui.graphics.Color {
        val c = (1f - kotlin.math.abs(2f * l / 100f - 1f)) * s / 100f
        val x = c * (1f - kotlin.math.abs((h / 60f) % 2f - 1f))
        val m = l / 100f - c / 2f

        val (r, g, b) = when {
            h < 60f -> Triple(c, x, 0f)
            h < 120f -> Triple(x, c, 0f)
            h < 180f -> Triple(0f, c, x)
            h < 240f -> Triple(0f, x, c)
            h < 300f -> Triple(x, 0f, c)
            else -> Triple(c, 0f, x)
        }

        return androidx.compose.ui.graphics.Color(
            red = (r + m).coerceIn(0f, 1f),
            green = (g + m).coerceIn(0f, 1f),
            blue = (b + m).coerceIn(0f, 1f)
        )
    }
}

data class Dot(
    val index: Int,
    val x: Float,
    val y: Float,
    val radius: Float,
    val color: HSLColor,
    val isTarget: Boolean = false
)

data class LevelConfig(
    val level: Int,
    val gridSize: Int,
    val dotCount: Int,
    val hueDiff: Float,
    val dotRadius: Float,
    val hasFloatEffect: Boolean,
    val hasMoveEffect: Boolean,
    val floatAmplitude: Float
)

// ==================== 游戏模块 ====================

class ColorBurstGameModule : AbstractGameModule() {

    override val gameId: String = "color_burst"
    override val gameName: String = "色彩突围"
    override val iconAsset: String = "logo1.png"
    override val totalLevels: Int = 30
    override val description: String = "找出颜色略微不同的圆点"

    // 游戏数据
    private var dots = listOf<Dot>()
    private var baseColor = HSLColor(0f, 0f, 0f)
    private var targetColor = HSLColor(0f, 0f, 0f)
    private var remainingTime = INITIAL_TIME
    private var correctCount = 0
    private var wrongCount = 0
    private var combo = 0
    private var maxCombo = 0
    private var totalClicks = 0
    private var showBurst = false
    private var burstIndex = -1
    private var showShake = false
    private var timeBonus = 0

    // Canvas 尺寸（像素）
    private var canvasWidth: Float = 1f
    private var canvasHeight: Float = 1f

    // 调试模式
    private val DEBUG = true

    // 计时器
    private var gameTimerJob: kotlinx.coroutines.Job? = null

    companion object {
        const val INITIAL_TIME = 30000L
        const val CORRECT_TIME_BONUS = 3000L
        const val WRONG_TIME_PENALTY = 5000L
        const val TIME_TICK_MS = 100L
    }

    // ==================== 难度配置 ====================

    private fun getLevelConfig(level: Int): LevelConfig {
        return when {
            level <= 5 -> LevelConfig(
                level = level, gridSize = 2, dotCount = 4,
                hueDiff = 30f - (level - 1) * 3f, dotRadius = 45f,
                hasFloatEffect = false, hasMoveEffect = false, floatAmplitude = 0f
            )
            level <= 15 -> LevelConfig(
                level = level, gridSize = 3, dotCount = 9,
                hueDiff = 15f - (level - 5) * 0.8f, dotRadius = 38f,
                hasFloatEffect = true, hasMoveEffect = false, floatAmplitude = 3f
            )
            level <= 25 -> LevelConfig(
                level = level, gridSize = 4, dotCount = 16,
                hueDiff = 8f - (level - 15) * 0.5f, dotRadius = 32f,
                hasFloatEffect = true, hasMoveEffect = true, floatAmplitude = 5f
            )
            else -> LevelConfig(
                level = level, gridSize = 5, dotCount = 25,
                hueDiff = maxOf(2f, 5f - (level - 25) * 0.3f), dotRadius = 28f,
                hasFloatEffect = true, hasMoveEffect = true, floatAmplitude = 7f
            )
        }
    }

    // ==================== 颜色生成 ====================

    private fun generateBaseColor(): HSLColor {
        val level = currentLevel
        val hueRange = when {
            level <= 5 -> 80f..140f
            level <= 15 -> 40f..180f
            else -> 0f..360f
        }
        val h = Random.nextFloat() * (hueRange.endInclusive - hueRange.start) + hueRange.start
        val s = Random.nextFloat() * 20f + 45f
        val l = Random.nextFloat() * 20f + 50f
        return HSLColor(h, s, l)
    }

    private fun generateTargetColor(base: HSLColor, hueDiff: Float): HSLColor {
        val direction = if (Random.nextBoolean()) 1f else -1f
        val diff = Random.nextFloat() * (hueDiff * 0.5f) + hueDiff * 0.5f
        val newH = (base.h + direction * diff + 360f) % 360f
        return HSLColor(newH, base.s, base.l)
    }

    // ==================== 圆点生成 ====================

    private fun generateDots(config: LevelConfig): List<Dot> {
        val dotList = mutableListOf<Dot>()
        val targetIndex = Random.nextInt(config.dotCount)

        for (i in 0 until config.dotCount) {
            val row = i / config.gridSize
            val col = i % config.gridSize

            val baseX = (col + 0.5f) / config.gridSize
            val baseY = (row + 0.5f) / config.gridSize

            val offsetX = (Random.nextFloat() - 0.5f) * 0.1f
            val offsetY = (Random.nextFloat() - 0.5f) * 0.1f

            val x = (baseX + offsetX).coerceIn(0.1f, 0.9f)
            val y = (baseY + offsetY).coerceIn(0.1f, 0.9f)

            val isTarget = (i == targetIndex)
            val color = if (isTarget) targetColor else baseColor

            dotList.add(Dot(i, x, y, config.dotRadius, color, isTarget))
        }

        return dotList
    }

    // ==================== 点击检测 ====================

    fun setCanvasSize(width: Float, height: Float) {
        canvasWidth = width
        canvasHeight = height
        if (DEBUG) {
            println("ColorBurst: Canvas size = ${canvasWidth}x${canvasHeight}")
        }
    }

    private fun checkHit(tapX: Float, tapY: Float): Int {
        if (canvasWidth <= 0 || canvasHeight <= 0 || dots.isEmpty()) {
            return -1
        }

        if (DEBUG) {
            println("ColorBurst: checkHit tap=(${tapX}, ${tapY}) canvas=${canvasWidth}x${canvasHeight}")
        }

        var closestIndex = -1
        var closestDist = Float.MAX_VALUE

        for (dot in dots) {
            val dotCenterX = dot.x * canvasWidth
            val dotCenterY = dot.y * canvasHeight
            val dotRadiusPx = dot.radius

            val dx = tapX - dotCenterX
            val dy = tapY - dotCenterY
            val dist = sqrt(dx * dx + dy * dy)

            val hitRadius = dotRadiusPx * 1.3f

            if (DEBUG && dot.isTarget) {
                println("ColorBurst: TARGET Dot${dot.index} center=(${dotCenterX}, ${dotCenterY}) radius=${dotRadiusPx} dist=${dist} hitRadius=${hitRadius}")
            }

            if (dist <= hitRadius && dist < closestDist) {
                closestDist = dist
                closestIndex = dot.index
            }
        }

        if (DEBUG) {
            if (closestIndex >= 0) {
                val hitDot = dots[closestIndex]
                println("ColorBurst: HIT Dot${closestIndex} isTarget=${hitDot.isTarget}")
            } else {
                println("ColorBurst: MISS")
            }
        }

        return closestIndex
    }

    // ==================== 游戏生命周期 ====================

    override fun start(level: Int) {
        currentLevel = level.coerceIn(1, totalLevels)
        currentScore = 0
        mistakeCount = 0
        correctCount = 0
        wrongCount = 0
        combo = 0
        maxCombo = 0
        totalClicks = 0
        remainingTime = INITIAL_TIME
        showBurst = false
        burstIndex = -1
        showShake = false
        timeBonus = 0

        // 直接开始，不显示倒计时
        startGame()
    }

    override fun startGame() {
        val config = getLevelConfig(currentLevel)
        baseColor = generateBaseColor()
        targetColor = generateTargetColor(baseColor, config.hueDiff)
        dots = generateDots(config)

        startGameTimer()
        updatePlayingState()
    }

    private fun startGameTimer() {
        gameTimerJob?.cancel()
        gameTimerJob = gameScope.launch {
            while (true) {
                delay(TIME_TICK_MS)
                remainingTime -= TIME_TICK_MS

                if (remainingTime <= 0) {
                    remainingTime = 0
                    gameOver()
                    break
                }
                updatePlayingState()
            }
        }
    }

    private fun updatePlayingState() {
        _state.value = GameState.Playing(
            level = currentLevel,
            elapsedTime = INITIAL_TIME - remainingTime,
            score = currentScore,
            data = buildGameData()
        )
    }

    private fun buildGameData(): Map<String, Any> {
        return mapOf(
            "dots" to dots.map { dot ->
                mapOf(
                    "index" to dot.index,
                    "x" to dot.x,
                    "y" to dot.y,
                    "radius" to dot.radius,
                    "colorH" to dot.color.h,
                    "colorS" to dot.color.s,
                    "colorL" to dot.color.l,
                    "isTarget" to dot.isTarget
                )
            },
            "remainingTime" to remainingTime,
            "correctCount" to correctCount,
            "wrongCount" to wrongCount,
            "combo" to combo,
            "maxCombo" to maxCombo,
            "totalClicks" to totalClicks,
            "showBurst" to showBurst,
            "burstIndex" to burstIndex,
            "showShake" to showShake,
            "timeBonus" to timeBonus,
            "levelConfig" to getLevelConfig(currentLevel).let {
                mapOf(
                    "gridSize" to it.gridSize,
                    "dotCount" to it.dotCount,
                    "hasFloatEffect" to it.hasFloatEffect,
                    "hasMoveEffect" to it.hasMoveEffect,
                    "floatAmplitude" to it.floatAmplitude
                )
            }
        )
    }

    override fun onUserAction(action: GameAction): ActionResult? {
        when (action) {
            is GameAction.Tap -> {
                return handleTap(action.x, action.y)
            }
            else -> return super.onUserAction(action)
        }
    }

    private fun handleTap(tapX: Float, tapY: Float): ActionResult? {
        val currentState = _state.value
        if (currentState !is GameState.Playing) return null

        val hitIndex = checkHit(tapX, tapY)
        if (hitIndex < 0) return null

        totalClicks++
        val hitDot = dots.find { it.index == hitIndex } ?: return null

        if (hitDot.isTarget) {
            handleCorrectTap(hitIndex)
            return ActionResult.Success
        } else {
            handleWrongTap()
            return ActionResult.Error("点错了", shake = true)
        }
    }

    private fun handleCorrectTap(index: Int) {
        correctCount++
        combo++
        if (combo > maxCombo) maxCombo = combo
        currentScore += 10 + combo * 2

        remainingTime += CORRECT_TIME_BONUS
        if (remainingTime > 60000) remainingTime = 60000
        timeBonus = (CORRECT_TIME_BONUS / 1000).toInt()

        showBurst = true
        burstIndex = index

        updatePlayingState()

        gameScope.launch {
            delay(600)
            showBurst = false
            burstIndex = -1
            timeBonus = 0

            // 完成本关，显示结果弹窗，不自动跳转
            gameComplete()
        }
    }

    private fun handleWrongTap() {
        wrongCount++
        mistakeCount++
        combo = 0

        remainingTime -= WRONG_TIME_PENALTY
        if (remainingTime < 0) remainingTime = 0
        timeBonus = -(WRONG_TIME_PENALTY / 1000).toInt()

        showShake = true

        updatePlayingState()

        gameScope.launch {
            delay(300)
            showShake = false
            timeBonus = 0
            updatePlayingState()
        }
    }

    private fun advanceToNextLevel() {
        currentLevel++
        if (currentLevel > totalLevels) {
            gameComplete()
            return
        }

        val config = getLevelConfig(currentLevel)
        baseColor = generateBaseColor()
        targetColor = generateTargetColor(baseColor, config.hueDiff)
        dots = generateDots(config)
        combo = 0

        updatePlayingState()
    }

    private fun gameComplete() {
        gameTimerJob?.cancel()
        val timeUsed = INITIAL_TIME * (currentLevel - 1) + (INITIAL_TIME - remainingTime)
        val stars = calculateStars(timeUsed, wrongCount, currentLevel)

        completeLevel(
            isSuccess = true,
            timeMillis = timeUsed,
            score = currentScore,
            stars = stars
        )
    }

    private fun gameOver() {
        gameTimerJob?.cancel()
        val timeUsed = INITIAL_TIME * (currentLevel - 1) + (INITIAL_TIME - remainingTime)
        val stars = calculateStars(timeUsed, wrongCount, currentLevel)

        _state.value = GameState.Completed(
            level = currentLevel,
            isSuccess = false,
            timeMillis = timeUsed,
            score = currentScore,
            stars = stars
        )

        _result.value = com.animalgame.core.game.GameResult(
            gameId = gameId,
            level = currentLevel,
            isSuccess = false,
            timeMillis = timeUsed,
            score = currentScore,
            stars = stars,
            mistakes = wrongCount
        )
    }

    override fun calculateStars(timeMillis: Long, mistakes: Int, level: Int): Int {
        if (totalClicks == 0) return 1
        val accuracy = correctCount.toFloat() / totalClicks
        val comboBonus = maxCombo * 0.05f
        val levelBonus = level.toFloat() / totalLevels * 20f
        val score = accuracy * 60 + comboBonus * 20 + levelBonus

        return when {
            score >= 75 && accuracy >= 0.85f -> 3
            score >= 50 || accuracy >= 0.6f -> 2
            else -> 1
        }
    }

    // ==================== 公共方法 ====================

    fun getDifficultyName(): String {
        return when {
            currentLevel <= 5 -> "入门"
            currentLevel <= 15 -> "进阶"
            currentLevel <= 25 -> "困难"
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
        gameTimerJob?.cancel()
        _state.value = GameState.Idle
    }

    fun getTargetIndex(): Int {
        return dots.indexOfFirst { it.isTarget }
    }

    override fun destroy() {
        gameTimerJob?.cancel()
        super.destroy()
    }
}

// ==================== 注册 ====================

fun registerColorBurstGame() {
    GameRegistry.register(ColorBurstGameModule())
}