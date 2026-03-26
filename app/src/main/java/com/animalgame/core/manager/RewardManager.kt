package com.animalgame.core.manager

import com.animalgame.core.model.Medal
import com.animalgame.core.model.Medals
import com.animalgame.core.model.Planet
import com.animalgame.core.model.Planets
import com.animalgame.core.model.Character
import com.animalgame.core.model.Characters

/**
 * 奖励管理器
 * 管理勋章解锁、星球解锁、角色解锁
 */
object RewardManager {

    /**
     * 检查并返回新解锁的勋章
     */
    fun checkNewMedals(
        currentMedals: Set<String>,
        gameResult: com.animalgame.core.model.GameResult,
        totalStars: Int,
        streakDays: Int = 0,
        comboCount: Int = 0
    ): List<Medal> {
        val newMedals = mutableListOf<Medal>()

        Medals.all.forEach { medal ->
            if (medal.id in currentMedals) return@forEach

            val unlocked = when (val condition = medal.condition) {
                is com.animalgame.core.model.MedalCondition.StreakDays -> streakDays >= condition.days
                is com.animalgame.core.model.MedalCondition.ConsecutiveLevels -> gameResult.level >= condition.levels
                is com.animalgame.core.model.MedalCondition.SingleScore -> gameResult.score >= condition.score
                is com.animalgame.core.model.MedalCondition.Combo -> comboCount >= condition.count
                is com.animalgame.core.model.MedalCondition.FirstWin -> gameResult.level == 1 && gameResult.isCompleted
                is com.animalgame.core.model.MedalCondition.AllPlanets -> totalStars >= 100
            }

            if (unlocked) {
                newMedals.add(medal)
            }
        }

        return newMedals
    }

    /**
     * 检查新解锁的星球
     */
    fun checkNewPlanets(
        currentPlanets: Set<String>,
        totalCompletedLevels: Int
    ): List<Planet> {
        val newPlanets = mutableListOf<Planet>()

        Planets.all.forEach { planet ->
            if (planet.id in currentPlanets) return@forEach

            if (totalCompletedLevels >= planet.requiredLevels) {
                newPlanets.add(planet)
            }
        }

        return newPlanets
    }

    /**
     * 检查新解锁的角色
     */
    fun checkNewCharacters(
        currentCharacters: Set<String>,
        totalStars: Int
    ): List<Character> {
        val newCharacters = mutableListOf<Character>()

        Characters.all.forEach { character ->
            if (character.id in currentCharacters) return@forEach

            if (totalStars >= character.requiredStars) {
                newCharacters.add(character)
            }
        }

        return newCharacters
    }

    /**
     * 计算星星奖励
     * 基础奖励 + 连击加成
     */
    fun calculateStarReward(
        baseStars: Int,
        comboCount: Int
    ): Int {
        return baseStars + (comboCount * 2)
    }

    /**
     * 获取进度百分比
     */
    fun getPlanetProgress(
        currentLevels: Int,
        planet: Planet
    ): Float {
        val nextPlanet = Planets.all.find { it.requiredLevels > currentLevels }
            ?: return 1f

        val prevLevels = Planets.all
            .filter { it.requiredLevels < planet.requiredLevels }
            .maxOfOrNull { it.requiredLevels } ?: 0

        val progress = (currentLevels - prevLevels).toFloat() /
                (planet.requiredLevels - prevLevels)

        return progress.coerceIn(0f, 1f)
    }

    /**
     * 获取角色解锁进度
     */
    fun getCharacterProgress(
        currentStars: Int,
        character: Character
    ): Float {
        val prevCharacter = Characters.all
            .filter { it.requiredStars < character.requiredStars }
            .maxOfOrNull { it.requiredStars } ?: 0

        val progress = (currentStars - prevStars).toFloat() /
                (character.requiredStars - prevStars)

        return progress.coerceIn(0f, 1f)
    }

    private val prevStars: Int get() = 0
}
