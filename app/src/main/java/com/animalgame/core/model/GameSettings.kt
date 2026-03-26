package com.animalgame.core.model

/**
 * 游戏设置数据类
 */
data class GameSettings(
    val soundVolume: Float = 1.0f,      // 音效音量 0.0 ~ 1.0
    val musicEnabled: Boolean = true,  // 背景音乐开关
    val vibrationEnabled: Boolean = true, // 震动反馈开关
    // 预留扩展字段
    val language: String = "zh",        // 语言设置
    val defaultDifficulty: String = "EASY", // 默认难度
    val iconTheme: String = "default"   // 图标主题
) {
    companion object {
        // 默认设置
        val DEFAULT = GameSettings()

        // 音效类型
        enum class SoundType {
            FLIP,       // 翻牌
            MATCH,      // 配对成功
            UNMATCH,   // 配对失败
            COMPLETE,   // 通关
            CLICK       // 点击
        }

        // 扩展字段键（用于未来扩展）
        val EXTENSION_KEYS = setOf("language", "defaultDifficulty", "iconTheme")
    }
}
