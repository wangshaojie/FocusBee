package com.animalgame.core.model

/**
 * 用户游戏进度
 */
data class UserProgress(
    val totalStars: Int = 0,
    val unlockedPlanets: Set<String> = setOf("forest"),
    val unlockedCharacters: Set<String> = setOf("fox"),
    val earnedMedals: Set<String> = setOf(),
    val gameProgress: Map<String, GameProgress> = emptyMap()
)

/**
 * 单个游戏的进度
 */
data class GameProgress(
    val currentLevel: Int = 1,
    val bestScores: Map<Int, Int> = emptyMap(),
    val totalStars: Int = 0
)

/**
 * 星球定义
 */
data class Planet(
    val id: String,
    val name: String,
    val emoji: String,
    val requiredLevels: Int
)

/**
 * 角色定义
 */
data class Character(
    val id: String,
    val name: String,
    val emoji: String,
    val requiredStars: Int
)

/**
 * 预定义的星球
 */
object Planets {
    val all = listOf(
        Planet("forest", "森林星球", "🌲", 0),
        Planet("ocean", "海洋星球", "🌊", 5),
        Planet("space", "太空星球", "🌙", 15),
        Planet("desert", "沙漠星球", "🏜️", 30)
    )

    fun getUnlocked(totalCompletedLevels: Int): List<Planet> {
        return all.filter { it.requiredLevels <= totalCompletedLevels }
    }
}

/**
 * 预定义的角色
 */
object Characters {
    val all = listOf(
        Character("fox", "狐狸", "🦊", 0),
        Character("cat", "猫咪", "🐱", 100),
        Character("astronaut", "宇航员", "🧑‍🚀", 300),
        Character("robot", "机器人", "🤖", 500),
        Character("dragon", "神龙", "🐉", 1000)
    )

    fun getUnlocked(totalStars: Int): List<Character> {
        return all.filter { it.requiredStars <= totalStars }
    }
}
