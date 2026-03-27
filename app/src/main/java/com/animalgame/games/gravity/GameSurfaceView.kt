package com.animalgame.games.gravity

import android.content.Context
import android.graphics.Canvas
import android.graphics.PointF
import android.util.AttributeSet
import android.view.SurfaceHolder
import android.view.SurfaceView

/**
 * 游戏 SurfaceView
 * 负责游戏循环和渲染
 */
class GameSurfaceView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null
) : SurfaceView(context, attrs), SurfaceHolder.Callback, Runnable {

    // 游戏线程
    private var gameThread: Thread? = null
    private var isRunning = false

    // 游戏子系统
    private var sensor: GravitySensor? = null
    private var physics: PhysicsEngine? = null
    private var collision: CollisionDetector? = null
    private var renderer: LevelRenderer? = null

    // 游戏状态
    private var levelConfig: LevelConfig? = null
    private var ballPosition = PointF(0f, 0f)
    private var gameStateCallback: ((GameSurfaceState) -> Unit)? = null
    private var isGameActive = false

    // 轨迹
    private val trailPositions = mutableListOf<PointF>()
    private val maxTrailSize = 20

    // 计时
    private var gameStartTime = 0L
    private var elapsedTime = 0L
    private var mistakeCount = 0
    private var timeLimit = 20000L

    // 帧时间控制
    private var lastFrameTime = 0L
    private val targetFPS = 60
    private val frameTime = 1000L / targetFPS

    init {
        holder.addCallback(this)
        isFocusable = true
    }

    /**
     * 初始化游戏组件
     */
    fun initialize(config: LevelConfig, context: Context) {
        this.levelConfig = config

        // 初始化子系统
        sensor = GravitySensor(context)
        physics = PhysicsEngine()
        renderer = LevelRenderer()

        // 计算屏幕坐标
        val width = width.coerceAtLeast(1)
        val height = height.coerceAtLeast(1)
        collision = CollisionDetector(config, width, height)

        // 设置小球初始位置
        collision?.let {
            ballPosition = it.getStartPosition()
        }

        // 重置状态
        trailPositions.clear()
        mistakeCount = 0
        timeLimit = config.timeLimit
        physics?.reset()
    }

    /**
     * 设置状态回调
     */
    fun setStateCallback(callback: (GameSurfaceState) -> Unit) {
        gameStateCallback = callback
    }

    /**
     * 开始游戏
     */
    fun startGame() {
        levelConfig?.let { config ->
            // 重新初始化位置
            collision = CollisionDetector(config, width, height)
            ballPosition = collision?.getStartPosition() ?: PointF(0f, 0f)
        }

        isGameActive = true
        gameStartTime = System.currentTimeMillis()
        mistakeCount = 0
        trailPositions.clear()
        physics?.reset()
        sensor?.start()

        // 通知 UI 开始
        gameStateCallback?.invoke(GameSurfaceState.Playing(0, 0))
    }

    /**
     * 暂停游戏
     */
    fun pauseGame() {
        isGameActive = false
        sensor?.stop()
    }

    /**
     * 恢复游戏
     */
    fun resumeGame() {
        if (isGameActive) {
            sensor?.start()
        }
    }

    /**
     * 停止游戏
     */
    fun stopGame() {
        isGameActive = false
        sensor?.stop()
    }

    override fun surfaceCreated(holder: SurfaceHolder) {
        // Surface 创建后可以开始绘制
    }

    override fun surfaceChanged(holder: SurfaceHolder, format: Int, width: Int, height: Int) {
        // 屏幕大小变化时重新初始化碰撞检测
        levelConfig?.let { config ->
            collision = CollisionDetector(config, width, height)
            if (!isGameActive) {
                ballPosition = collision?.getStartPosition() ?: PointF(0f, 0f)
            }
        }
    }

    override fun surfaceDestroyed(holder: SurfaceHolder) {
        stopGame()
    }

    override fun run() {
        while (isRunning) {
            val startTime = System.currentTimeMillis()

            if (isGameActive) {
                updateFrame()
            }

            // 渲染
            if (holder.surface.isValid) {
                val canvas = holder.lockCanvas()
                if (canvas != null) {
                    try {
                        drawFrame(canvas)
                    } finally {
                        holder.unlockCanvasAndPost(canvas)
                    }
                }
            }

            // 帧时间控制
            val workTime = System.currentTimeMillis() - startTime
            val sleepTime = frameTime - workTime
            if (sleepTime > 0) {
                try {
                    Thread.sleep(sleepTime)
                } catch (e: InterruptedException) {
                    e.printStackTrace()
                }
            }

            lastFrameTime = System.currentTimeMillis()
        }
    }

    /**
     * 每帧更新
     */
    private fun updateFrame() {
        val config = levelConfig ?: return
        val sensorData = sensor ?: return
        val physicsEngine = physics ?: return
        val collisionDetector = collision ?: return

        // 计算时间
        elapsedTime = System.currentTimeMillis() - gameStartTime

        // 检查超时
        if (elapsedTime > timeLimit) {
            gameStateCallback?.invoke(GameSurfaceState.Timeout(elapsedTime))
            isGameActive = false
            return
        }

        // 物理更新
        val deltaTime = 0.016f  // 16ms
        val delta = physicsEngine.update(sensorData.gravityX, sensorData.gravityY, deltaTime)

        // 新位置
        var newX = ballPosition.x + delta.x
        var newY = ballPosition.y + delta.y

        // 碰撞检测
        val collisionType = collisionDetector.checkCollision(newX, newY)

        when (collisionType) {
            CollisionType.GOAL -> {
                // 到达终点
                isGameActive = false
                sensor?.stop()
                val finalTime = elapsedTime
                gameStateCallback?.invoke(GameSurfaceState.Success(finalTime))
                return
            }

            CollisionType.WALL -> {
                // 撞墙
                mistakeCount++
                physicsEngine.bounceOffWall()

                // 检查是否超过容错
                if (mistakeCount > config.tolerance) {
                    isGameActive = false
                    sensor?.stop()
                    gameStateCallback?.invoke(GameSurfaceState.Failed("撞墙次数过多"))
                    return
                }

                // 撞墙后不移动
                newX = ballPosition.x
                newY = ballPosition.y
            }

            CollisionType.BOUNDARY -> {
                // 边界反弹
                physicsEngine.bounceOffBoundary(true)
                physicsEngine.bounceOffBoundary(false)
                newX = newX.coerceIn(30f, width - 30f)
                newY = newY.coerceIn(30f, height - 30f)
            }

            else -> {}
        }

        // 更新位置
        ballPosition = PointF(newX, newY)

        // 更新轨迹
        trailPositions.add(PointF(ballPosition.x, ballPosition.y))
        if (trailPositions.size > maxTrailSize) {
            trailPositions.removeAt(0)
        }

        // 通知状态
        gameStateCallback?.invoke(GameSurfaceState.Playing(elapsedTime, mistakeCount))
    }

    /**
     * 绘制帧
     */
    private fun drawFrame(canvas: Canvas) {
        val config = levelConfig ?: return
        val rendererObj = renderer ?: return

        if (isGameActive) {
            rendererObj.render(canvas, config, ballPosition.x, ballPosition.y, trailPositions)
        } else if (elapsedTime > 0) {
            // 游戏结束时的最后一帧
            rendererObj.render(canvas, config, ballPosition.x, ballPosition.y, trailPositions)
        } else {
            // 初始状态
            rendererObj.render(canvas, config, ballPosition.x, ballPosition.y, emptyList())
        }
    }

    /**
     * 开始游戏线程
     */
    fun startThread() {
        isRunning = true
        gameThread = Thread(this)
        gameThread?.start()
    }

    /**
     * 停止游戏线程
     */
    fun stopThread() {
        isRunning = false
        try {
            gameThread?.join(500)
        } catch (e: InterruptedException) {
            e.printStackTrace()
        }
        gameThread = null
    }
}

/**
 * SurfaceView 状态
 */
sealed class GameSurfaceState {
    data class Playing(val elapsedTime: Long, val mistakes: Int) : GameSurfaceState()
    data class Success(val timeMillis: Long) : GameSurfaceState()
    data class Failed(val message: String) : GameSurfaceState()
    data class Timeout(val elapsedTime: Long) : GameSurfaceState()
}
