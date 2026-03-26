package com.animalgame.games.memory

import com.animalgame.core.manager.GameRegistry

/**
 * 注册记忆翻牌游戏
 */
fun registerMemoryGame() {
    GameRegistry.register(MemoryGameModule())
}
