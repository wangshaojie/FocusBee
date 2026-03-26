package com.animalgame.core.manager

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringSetPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.animalgame.core.model.GameProgress
import com.animalgame.core.model.GameResult
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map

private val Context.scoreDataStore: DataStore<Preferences> by preferencesDataStore(name = "user_progress")

/**
 * 分数管理器
 * 统一管理所有游戏的分数和星星
 */
class ScoreManager(private val context: Context) {

    companion object {
        private val TOTAL_STARS = intPreferencesKey("total_stars")
        private val UNLOCKED_PLANETS = stringSetPreferencesKey("unlocked_planets")
        private val UNLOCKED_CHARACTERS = stringSetPreferencesKey("unlocked_characters")
        private val EARNED_MEDALS = stringSetPreferencesKey("earned_medals")

        // 游戏进度 key
        private val gameProgressKeys = mutableMapOf<String, Preferences.Key<Int>>()
        private fun currentLevelKey(gameId: String): Preferences.Key<Int> {
            return gameProgressKeys.getOrPut("${gameId}_level") {
                intPreferencesKey("${gameId}_current_level")
            }
        }
        private fun gameStarsKey(gameId: String): Preferences.Key<Int> {
            return gameProgressKeys.getOrPut("${gameId}_stars") {
                intPreferencesKey("${gameId}_stars")
            }
        }

        @Volatile
        private var instance: ScoreManager? = null

        fun getInstance(context: Context): ScoreManager {
            return instance ?: synchronized(this) {
                instance ?: ScoreManager(context.applicationContext).also { instance = it }
            }
        }
    }

    // 总星星数
    val totalStars: Flow<Int> = context.scoreDataStore.data.map { prefs ->
        prefs[TOTAL_STARS] ?: 0
    }

    // 已解锁星球
    val unlockedPlanets: Flow<Set<String>> = context.scoreDataStore.data.map { prefs ->
        prefs[UNLOCKED_PLANETS] ?: setOf("forest")
    }

    // 已解锁角色
    val unlockedCharacters: Flow<Set<String>> = context.scoreDataStore.data.map { prefs ->
        prefs[UNLOCKED_CHARACTERS] ?: setOf("fox")
    }

    // 已获得勋章
    val earnedMedals: Flow<Set<String>> = context.scoreDataStore.data.map { prefs ->
        prefs[EARNED_MEDALS] ?: emptySet()
    }

    /**
     * 获取单个游戏的进度
     */
    suspend fun getGameProgress(gameId: String): GameProgress {
        val prefs = context.scoreDataStore.data.first()
        return GameProgress(
            currentLevel = prefs[currentLevelKey(gameId)] ?: 1,
            totalStars = prefs[gameStarsKey(gameId)] ?: 0
        )
    }

    /**
     * 报告游戏结果
     */
    suspend fun reportResult(result: GameResult): List<String> {
        val newMedals = mutableListOf<String>()

        context.scoreDataStore.edit { prefs ->
            // 更新总星星
            val currentStars = prefs[TOTAL_STARS] ?: 0
            prefs[TOTAL_STARS] = currentStars + result.stars

            // 更新游戏星星
            val gameStars = prefs[gameStarsKey(result.gameId)] ?: 0
            prefs[gameStarsKey(result.gameId)] = gameStars + result.stars

            // 更新游戏关卡
            val currentLevel = prefs[currentLevelKey(result.gameId)] ?: 1
            if (result.isCompleted && result.level >= currentLevel) {
                prefs[currentLevelKey(result.gameId)] = result.level + 1
            }

            // 检查星球解锁
            checkPlanetUnlock(prefs)

            // 检查角色解锁
            checkCharacterUnlock(prefs, currentStars + result.stars)

            // 检查勋章
            val medal = checkMedal(result, currentStars)
            if (medal != null) {
                newMedals.add(medal)
                val medals = prefs[EARNED_MEDALS] ?: emptySet()
                prefs[EARNED_MEDALS] = medals + medal
            }
        }

        return newMedals
    }

    private fun checkPlanetUnlock(prefs: androidx.datastore.preferences.core.MutablePreferences) {
        val currentPlanets = prefs[UNLOCKED_PLANETS] ?: setOf("forest")
        val totalCompleted = currentPlanets.size * 5 // 简化计算

        val newPlanets = mutableSetOf<String>()
        if (totalCompleted >= 0) newPlanets.add("forest")
        if (totalCompleted >= 5) newPlanets.add("ocean")
        if (totalCompleted >= 15) newPlanets.add("space")
        if (totalCompleted >= 30) newPlanets.add("desert")

        prefs[UNLOCKED_PLANETS] = newPlanets
    }

    private fun checkCharacterUnlock(prefs: androidx.datastore.preferences.core.MutablePreferences, totalStars: Int) {
        val currentCharacters = prefs[UNLOCKED_CHARACTERS] ?: setOf("fox")

        val newCharacters = mutableSetOf<String>()
        if (totalStars >= 0) newCharacters.add("fox")
        if (totalStars >= 100) newCharacters.add("cat")
        if (totalStars >= 300) newCharacters.add("astronaut")
        if (totalStars >= 500) newCharacters.add("robot")
        if (totalStars >= 1000) newCharacters.add("dragon")

        prefs[UNLOCKED_CHARACTERS] = newCharacters
    }

    private fun checkMedal(result: GameResult, currentStars: Int): String? {
        // 首战告捷
        if (result.level == 1 && result.isCompleted) {
            return "first_win"
        }
        // 高分达人
        if (result.score >= 100) {
            return "high_score"
        }
        return null
    }

    /**
     * 添加勋章（手动）
     */
    suspend fun addMedal(medalId: String) {
        context.scoreDataStore.edit { prefs ->
            val medals = prefs[EARNED_MEDALS] ?: emptySet()
            prefs[EARNED_MEDALS] = medals + medalId
        }
    }

    /**
     * 重置所有进度
     */
    suspend fun resetProgress() {
        context.scoreDataStore.edit { prefs ->
            prefs.clear()
            prefs[UNLOCKED_PLANETS] = setOf("forest")
            prefs[UNLOCKED_CHARACTERS] = setOf("fox")
        }
    }
}
