package com.animalgame.core.game

import android.content.Context
import android.content.Intent
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.StateFlow

/**
 * 通用游戏模块接口
 * 所有小游戏必须实现此接口
 */
interface GameModule {

    /**
     * 游戏唯一标识
     */
    val gameId: String

    /**
     * 游戏显示名称
     */
    val gameName: String

    /**
     * 游戏图标资源名
     */
    val iconAsset: String

    /**
     * 总关卡数
     */
    val totalLevels: Int

    /**
     * 游戏描述
     */
    val description: String

    /**
     * 当前游戏状态（可观察）
     * 使用 StateFlow 以便 UI 层观察状态变化
     */
    val state: StateFlow<GameState>

    /**
     * 当前游戏结果（完成后输出）
     * 游戏结束后才有值，平时为 null
     */
    val result: Flow<GameResult?>

    /**
     * 创建游戏Activity的Intent（可选实现）
     * 用于从首页启动游戏
     */
    fun createIntent(context: Context): Intent? = null

    /**
     * 开始指定关卡
     * @param level 关卡编号（从1开始）
     */
    fun start(level: Int)

    /**
     * 处理用户操作
     * @param action 用户操作
     * @return 操作结果（可选，用于即时反馈）
     */
    fun onUserAction(action: GameAction): ActionResult?

    /**
     * 暂停游戏
     */
    fun pause()

    /**
     * 恢复游戏
     */
    fun resume()

    /**
     * 重置游戏
     */
    fun reset()

    /**
     * 销毁游戏，释放资源
     */
    fun destroy()

    /**
     * 计算星级（可选实现）
     * @param timeMillis 用时（毫秒）
     * @param mistakes 错误次数
     * @param level 当前关卡
     * @return 星级（1-3）
     */
    fun calculateStars(timeMillis: Long, mistakes: Int, level: Int): Int = 1
}

/**
 * 游戏状态
 */
sealed class GameState {

    /**
     * 等待开始 - 显示关卡选择
     */
    data object Idle : GameState()

    /**
     * 准备中 - 倒计时、动画等
     */
    data class Ready(
        val level: Int,
        val countdown: Int = 3
    ) : GameState()

    /**
     * 进行中
     */
    data class Playing(
        val level: Int,
        val elapsedTime: Long = 0L,     // 已用时间（毫秒）
        val score: Int = 0,             // 当前得分
        val data: Map<String, Any> = emptyMap() // 游戏特定数据
    ) : GameState()

    /**
     * 已暂停
     */
    data class Paused(
        val level: Int,
        val elapsedTime: Long,
        val score: Int
    ) : GameState()

    /**
     * 已完成
     */
    data class Completed(
        val level: Int,
        val isSuccess: Boolean,
        val timeMillis: Long,
        val score: Int,
        val stars: Int
    ) : GameState()

    /**
     * 全部通关
     */
    data class AllCompleted(
        val totalTime: Long,
        val totalScore: Int,
        val levelResults: List<LevelResult>
    ) : GameState()
}

/**
 * 游戏结果
 */
data class GameResult(
    val gameId: String,
    val level: Int,
    val isSuccess: Boolean,
    val timeMillis: Long,     // 用时（毫秒）
    val score: Int,          // 得分
    val stars: Int,          // 星级（1-3）
    val mistakes: Int = 0,   // 错误次数
    val extraData: Map<String, Any> = emptyMap()
)

/**
 * 单关结果
 */
data class LevelResult(
    val level: Int,
    val isSuccess: Boolean,
    val timeMillis: Long,
    val score: Int,
    val stars: Int
)

/**
 * 用户操作
 */
sealed class GameAction {
    // 通用操作
    data object Start : GameAction()           // 开始游戏
    data object Pause : GameAction()           // 暂停
    data object Resume : GameAction()          // 继续
    data object Restart : GameAction()         // 重新开始
    data object NextLevel : GameAction()       // 下一关
    data object Quit : GameAction()            // 退出

    // 游戏特定操作
    data class Tap(val x: Float, val y: Float) : GameAction()  // 点击
    data class TapIndex(val index: Int) : GameAction()          // 按索引点击（网格用）
    data class Swipe(val direction: SwipeDirection) : GameAction() // 滑动
    data class Input(val text: String) : GameAction()           // 文本输入
}

/**
 * 滑动方向
 */
enum class SwipeDirection {
    UP, DOWN, LEFT, RIGHT
}

/**
 * 操作结果
 * 用于即时反馈
 */
sealed class ActionResult {
    // 成功反馈
    data object Success : ActionResult()

    // 错误反馈
    data class Error(val message: String, val shake: Boolean = false) : ActionResult()

    // 得分变化
    data class ScoreChanged(val delta: Int, val newScore: Int) : ActionResult()

    // 动画触发
    data class Animation(val type: AnimationType) : ActionResult()

    // 状态变化
    data class StateChanged(val newState: GameState) : ActionResult()
}

/**
 * 动画类型
 */
enum class AnimationType {
    CORRECT,      // 正确
    WRONG,        // 错误
    COMPLETE,     // 完成
    STAR_GAIN,    // 获得星星
    REWARD,       // 奖励
    SHAKE,        // 抖动
    POP,          // 弹出
    SLIDE         // 滑入
}
