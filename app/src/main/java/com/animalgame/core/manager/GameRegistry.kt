package com.animalgame.core.manager

import com.animalgame.core.game.GameModule

/**
 * 游戏注册表
 * 统一管理所有游戏模块
 * 使用 core.game.GameModule 接口
 */
object GameRegistry {
    private val games = mutableMapOf<String, GameModule>()

    fun register(game: GameModule) {
        games[game.gameId] = game
    }

    fun getGame(id: String): GameModule? = games[id]

    fun getAllGames(): List<GameModule> = games.values.toList()

    fun getGameCount(): Int = games.size

    /**
     * 获取所有游戏的总关卡数
     */
    fun getTotalLevels(): Int = games.values.sumOf { it.totalLevels }
}
