package com.animalgame.core.model

/**
 * 勋章定义
 */
data class Medal(
    val id: String,
    val name: String,
    val emoji: String,
    val description: String,
    val condition: MedalCondition
)

/**
 * 勋章解锁条件
 */
sealed class MedalCondition {
    data class StreakDays(val days: Int) : MedalCondition()      // 连续天数
    data class ConsecutiveLevels(val levels: Int) : MedalCondition() // 连续关卡
    data class SingleScore(val score: Int) : MedalCondition()      // 单次得分
    data class Combo(val count: Int) : MedalCondition()            // 连击次数
    data class FirstWin(val gameId: String) : MedalCondition()   // 首次通关
    data class AllPlanets(val gameId: String) : MedalCondition()   // 解锁所有星球
}

/**
 * 预定义勋章
 */
object Medals {
    val all = listOf(
        Medal(
            "early_bird",
            "早起鸟",
            "🐦",
            "连续完成3天",
            MedalCondition.StreakDays(3)
        ),
        Medal(
            "focus_master",
            "专注大师",
            "🎯",
            "连续完成5关",
            MedalCondition.ConsecutiveLevels(5)
        ),
        Medal(
            "high_score",
            "高分达人",
            "🏆",
            "单次得分超过100",
            MedalCondition.SingleScore(100)
        ),
        Medal(
            "combo_king",
            "连击之王",
            "⚡",
            "连击10次",
            MedalCondition.Combo(10)
        ),
        Medal(
            "first_win",
            "首战告捷",
            "🎖️",
            "完成第一关",
            MedalCondition.FirstWin("")
        ),
        Medal(
            "planet_explorer",
            "星球探索者",
            "🌍",
            "解锁所有星球",
            MedalCondition.AllPlanets("")
        )
    )

    fun getById(id: String): Medal? = all.find { it.id == id }
}
