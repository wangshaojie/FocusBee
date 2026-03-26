package com.animalgame.core.game

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.cancel

/**
 * 抽象游戏模块基类
 * 提供通用功能实现
 */
abstract class AbstractGameModule : GameModule {

    /**
     * 协程作用域
     */
    protected val gameScope = CoroutineScope(Dispatchers.Main + Job())

    /**
     * 当前游戏状态
     */
    protected val _state = MutableStateFlow<GameState>(GameState.Idle)
    override val state: kotlinx.coroutines.flow.StateFlow<GameState> = _state.asStateFlow()

    /**
     * 游戏结果流
     */
    protected val _result = MutableStateFlow<GameResult?>(null)
    override val result: Flow<GameResult?> = _result.asSharedFlow()

    /**
     * 当前关卡
     */
    protected var currentLevel: Int = 1

    /**
     * 游戏开始时间
     */
    protected var startTime: Long = 0L

    /**
     * 计时器 Job
     */
    protected var timerJob: Job? = null

    /**
     * 当前得分
     */
    protected var currentScore: Int = 0

    /**
     * 错误次数
     */
    protected var mistakeCount: Int = 0

    /**
     * 计时器是否运行中
     */
    protected var isTimerRunning: Boolean = false

    /**
     * 开始指定关卡
     */
    override fun start(level: Int) {
        currentLevel = level
        currentScore = 0
        mistakeCount = 0

        // 进入准备状态，显示倒计时
        _state.value = GameState.Ready(level = level, countdown = 3)

        // 启动倒计时
        gameScope.launch {
            for (i in 3 downTo 1) {
                _state.value = GameState.Ready(level = level, countdown = i)
                delay(1000)
            }
            // 倒计时结束，开始游戏
            startGame()
        }
    }

    /**
     * 开始游戏（子类实现具体逻辑）
     */
    protected abstract fun startGame()

    /**
     * 处理用户操作
     */
    override fun onUserAction(action: GameAction): ActionResult? {
        return when (action) {
            is GameAction.Start -> {
                start(currentLevel)
                null
            }
            is GameAction.Pause -> {
                pause()
                null
            }
            is GameAction.Resume -> {
                resume()
                null
            }
            is GameAction.Restart -> {
                start(currentLevel)
                null
            }
            is GameAction.NextLevel -> {
                if (currentLevel < totalLevels) {
                    start(currentLevel + 1)
                }
                null
            }
            is GameAction.Quit -> {
                _state.value = GameState.Idle
                null
            }
            else -> null // 由子类处理游戏特定操作
        }
    }

    /**
     * 暂停游戏
     */
    override fun pause() {
        val currentState = _state.value
        if (currentState is GameState.Playing) {
            timerJob?.cancel()
            isTimerRunning = false
            _state.value = GameState.Paused(
                level = currentState.level,
                elapsedTime = currentState.elapsedTime,
                score = currentState.score
            )
        }
    }

    /**
     * 恢复游戏
     */
    override fun resume() {
        val currentState = _state.value
        if (currentState is GameState.Paused) {
            _state.value = GameState.Playing(
                level = currentState.level,
                elapsedTime = currentState.elapsedTime,
                score = currentState.score
            )
            startTimer(currentState.elapsedTime)
        }
    }

    /**
     * 重置游戏
     */
    override fun reset() {
        timerJob?.cancel()
        currentLevel = 1
        currentScore = 0
        mistakeCount = 0
        isTimerRunning = false
        _state.value = GameState.Idle
        _result.value = null
    }

    /**
     * 销毁游戏
     */
    override fun destroy() {
        timerJob?.cancel()
        gameScope.cancel()
    }

    /**
     * 启动计时器
     */
    protected fun startTimer(initialTime: Long = 0L) {
        startTime = System.currentTimeMillis() - initialTime
        isTimerRunning = true

        timerJob?.cancel()
        timerJob = gameScope.launch {
            while (isActive && isTimerRunning) {
                val elapsed = System.currentTimeMillis() - startTime
                val currentState = _state.value
                if (currentState is GameState.Playing) {
                    _state.value = currentState.copy(elapsedTime = elapsed)
                }
                delay(100)
            }
        }
    }

    /**
     * 停止计时器
     */
    protected fun stopTimer(): Long {
        isTimerRunning = false
        timerJob?.cancel()
        return System.currentTimeMillis() - startTime
    }

    /**
     * 完成关卡
     */
    protected fun completeLevel(
        isSuccess: Boolean,
        timeMillis: Long,
        score: Int,
        stars: Int
    ) {
        stopTimer()

        _state.value = GameState.Completed(
            level = currentLevel,
            isSuccess = isSuccess,
            timeMillis = timeMillis,
            score = score,
            stars = stars
        )

        // 发送结果
        _result.value = GameResult(
            gameId = gameId,
            level = currentLevel,
            isSuccess = isSuccess,
            timeMillis = timeMillis,
            score = score,
            stars = stars,
            mistakes = mistakeCount
        )
    }

    /**
     * 进入下一关
     */
    protected fun goToNextLevel() {
        if (currentLevel < totalLevels) {
            start(currentLevel + 1)
        } else {
            // 全部通关
            _state.value = GameState.AllCompleted(
                totalTime = stopTimer(),
                totalScore = currentScore,
                levelResults = emptyList()
            )
        }
    }

    /**
     * 更新状态
     */
    protected fun updateState(state: GameState) {
        _state.value = state
    }

    /**
     * 获取当前状态
     */
    protected fun getCurrentState(): GameState = _state.value
}
