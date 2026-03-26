package com.animalgame.games.animal

import android.content.Context
import android.content.Intent
import com.animalgame.core.game.AbstractGameModule
import com.animalgame.core.game.ActionResult
import com.animalgame.core.game.GameAction
import com.animalgame.core.game.GameState
import com.animalgame.core.manager.GameRegistry

/**
 * 萌音大挑战游戏模块
 * 实现通用 GameModule 接口
 */
class AnimalGameModule : AbstractGameModule() {

    override val gameId: String = "animal"
    override val gameName: String = "萌音大挑战"
    override val iconAsset: String = "logo1.png"
    override val totalLevels: Int = 10
    override val description: String = "听声音找动物"

    override fun createIntent(context: Context): Intent {
        return Intent(context, AnimalGameActivity::class.java)
    }

    override fun startGame() {
        // 萌音大挑战的具体实现
        // 这里可以添加游戏特定的初始化逻辑
        _state.value = GameState.Playing(
            level = currentLevel,
            elapsedTime = 0L,
            score = 0
        )
        startTimer()
    }

    override fun onUserAction(action: GameAction): ActionResult? {
        // 处理萌音大挑战的特定操作
        return super.onUserAction(action)
    }

    override fun calculateStars(timeMillis: Long, mistakes: Int, level: Int): Int {
        // 萌音大挑战按刷新次数计算，刷新越少越好
        var stars = 1
        if (mistakes <= 2) stars++
        if (mistakes == 0) stars++
        return minOf(stars, 3)
    }
}

/**
 * 注册萌音大挑战游戏
 */
fun registerAnimalGame() {
    GameRegistry.register(AnimalGameModule())
}
