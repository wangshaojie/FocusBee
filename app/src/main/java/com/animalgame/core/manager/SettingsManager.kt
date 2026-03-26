package com.animalgame.core.manager

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.floatPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.animalgame.core.model.GameSettings
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

// DataStore 扩展
private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "game_settings")

/**
 * 设置管理器 - 单例模式
 * 负责游戏设置的持久化和全局访问
 */
object SettingsManager {

    private const val DATASTORE_NAME = "game_settings"

    // Preferences Keys
    private val KEY_SOUND_VOLUME = floatPreferencesKey("sound_volume")
    private val KEY_MUSIC_ENABLED = booleanPreferencesKey("music_enabled")
    private val KEY_VIBRATION_ENABLED = booleanPreferencesKey("vibration_enabled")
    private val KEY_LANGUAGE = stringPreferencesKey("language")
    private val KEY_DEFAULT_DIFFICULTY = stringPreferencesKey("default_difficulty")
    private val KEY_ICON_THEME = stringPreferencesKey("icon_theme")

    @Volatile
    private var dataStore: DataStore<Preferences>? = null

    private fun getDataStore(context: Context): DataStore<Preferences> {
        return dataStore ?: synchronized(this) {
            dataStore ?: context.dataStore.also { dataStore = it }
        }
    }

    /**
     * 获取设置 Flow（响应式）
     */
    fun getSettingsFlow(context: Context): Flow<GameSettings> {
        return getDataStore(context).data.map { preferences ->
            GameSettings(
                soundVolume = preferences[KEY_SOUND_VOLUME] ?: GameSettings.DEFAULT.soundVolume,
                musicEnabled = preferences[KEY_MUSIC_ENABLED] ?: GameSettings.DEFAULT.musicEnabled,
                vibrationEnabled = preferences[KEY_VIBRATION_ENABLED] ?: GameSettings.DEFAULT.vibrationEnabled,
                language = preferences[KEY_LANGUAGE] ?: GameSettings.DEFAULT.language,
                defaultDifficulty = preferences[KEY_DEFAULT_DIFFICULTY] ?: GameSettings.DEFAULT.defaultDifficulty,
                iconTheme = preferences[KEY_ICON_THEME] ?: GameSettings.DEFAULT.iconTheme
            )
        }
    }

    /**
     * 获取当前设置（同步版本，用于不需要响应式的情况）
     */
    suspend fun getSettings(context: Context): GameSettings {
        var result = GameSettings.DEFAULT
        getSettingsFlow(context).collect { settings ->
            result = settings
            return@collect
        }
        return result
    }

    /**
     * 更新音效音量
     */
    suspend fun updateSoundVolume(context: Context, volume: Float) {
        val clampedVolume = volume.coerceIn(0f, 1f)
        getDataStore(context).edit { preferences ->
            preferences[KEY_SOUND_VOLUME] = clampedVolume
        }
    }

    /**
     * 更新音乐开关
     */
    suspend fun updateMusicEnabled(context: Context, enabled: Boolean) {
        getDataStore(context).edit { preferences ->
            preferences[KEY_MUSIC_ENABLED] = enabled
        }
    }

    /**
     * 更新震动开关
     */
    suspend fun updateVibrationEnabled(context: Context, enabled: Boolean) {
        getDataStore(context).edit { preferences ->
            preferences[KEY_VIBRATION_ENABLED] = enabled
        }
    }

    /**
     * 更新语言设置
     */
    suspend fun updateLanguage(context: Context, language: String) {
        getDataStore(context).edit { preferences ->
            preferences[KEY_LANGUAGE] = language
        }
    }

    /**
     * 更新默认难度
     */
    suspend fun updateDefaultDifficulty(context: Context, difficulty: String) {
        getDataStore(context).edit { preferences ->
            preferences[KEY_DEFAULT_DIFFICULTY] = difficulty
        }
    }

    /**
     * 更新图标主题
     */
    suspend fun updateIconTheme(context: Context, theme: String) {
        getDataStore(context).edit { preferences ->
            preferences[KEY_ICON_THEME] = theme
        }
    }

    /**
     * 批量更新设置
     */
    suspend fun updateSettings(context: Context, settings: GameSettings) {
        getDataStore(context).edit { preferences ->
            preferences[KEY_SOUND_VOLUME] = settings.soundVolume.coerceIn(0f, 1f)
            preferences[KEY_MUSIC_ENABLED] = settings.musicEnabled
            preferences[KEY_VIBRATION_ENABLED] = settings.vibrationEnabled
            preferences[KEY_LANGUAGE] = settings.language
            preferences[KEY_DEFAULT_DIFFICULTY] = settings.defaultDifficulty
            preferences[KEY_ICON_THEME] = settings.iconTheme
        }
    }

    /**
     * 重置为默认设置
     */
    suspend fun resetToDefault(context: Context) {
        getDataStore(context).edit { preferences ->
            preferences.clear()
        }
    }

    /**
     * 扩展方法：添加新的设置项
     * 未来可以通过此方法扩展
     */
    suspend fun updateExtension(context: Context, key: String, value: String) {
        val preferenceKey = when (key) {
            "language" -> KEY_LANGUAGE
            "defaultDifficulty" -> KEY_DEFAULT_DIFFICULTY
            "iconTheme" -> KEY_ICON_THEME
            else -> stringPreferencesKey("ext_$key")
        }
        getDataStore(context).edit { preferences ->
            preferences[preferenceKey] = value
        }
    }
}
