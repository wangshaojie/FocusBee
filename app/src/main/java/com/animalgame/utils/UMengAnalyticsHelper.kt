package com.animalgame.utils

import android.content.Context
import com.umeng.analytics.MobclickAgent
import java.util.HashMap

object UMengAnalyticsHelper {

    /**
     * 简单事件
     */
    fun trackEvent(context: Context, eventId: String) {
        MobclickAgent.onEvent(context, eventId)
    }

    /**
     * 带标签的事件
     */
    fun trackEvent(context: Context, eventId: String, label: String) {
        MobclickAgent.onEvent(context, eventId, label)
    }

    /**
     * 带参数的事件
     */
    fun trackEvent(context: Context, eventId: String, params: Map<String, Any>) {
        val map = HashMap<String, String>()
        params.forEach { (k, v) -> map[k] = v.toString() }
        MobclickAgent.onEvent(context, eventId, map)
    }

    /**
     * 游戏开始
     */
    fun trackGameStart(context: Context, gameName: String, level: Int = 1) {
        val params = HashMap<String, String>()
        params["game_name"] = gameName
        params["level"] = level.toString()
        MobclickAgent.onEvent(context, "game_start", params)
    }

    /**
     * 游戏完成
     */
    fun trackGameComplete(context: Context, gameName: String, level: Int, score: Int, timeMillis: Long) {
        val params = HashMap<String, String>()
        params["game_name"] = gameName
        params["level"] = level.toString()
        params["score"] = score.toString()
        params["time_seconds"] = (timeMillis / 1000).toString()
        MobclickAgent.onEvent(context, "game_complete", params)
    }

    /**
     * 游戏退出
     */
    fun trackGameQuit(context: Context, gameName: String, level: Int, progress: Int) {
        val params = HashMap<String, String>()
        params["game_name"] = gameName
        params["level"] = level.toString()
        params["progress"] = progress.toString()
        MobclickAgent.onEvent(context, "game_quit", params)
    }

    /**
     * 关卡切换
     */
    fun trackLevelChange(context: Context, level: Int) {
        val params = HashMap<String, String>()
        params["level"] = level.toString()
        MobclickAgent.onEvent(context, "level_change", params)
    }

    /**
     * 按钮点击
     */
    fun trackButtonClick(context: Context, buttonName: String, screen: String) {
        val params = HashMap<String, String>()
        params["button_name"] = buttonName
        params["screen"] = screen
        MobclickAgent.onEvent(context, "button_click", params)
    }

    /**
     * 设置变更
     */
    fun trackSettingsChange(context: Context, settingName: String, value: String) {
        val params = HashMap<String, String>()
        params["setting_name"] = settingName
        params["value"] = value
        MobclickAgent.onEvent(context, "settings_change", params)
    }

    /**
     * 音乐开关
     */
    fun trackMusicToggle(context: Context, enabled: Boolean) {
        val params = HashMap<String, String>()
        params["enabled"] = if (enabled) "1" else "0"
        MobclickAgent.onEvent(context, "music_toggle", params)
    }

    /**
     * 音量变化
     */
    fun trackVolumeChange(context: Context, volume: Float) {
        val params = HashMap<String, String>()
        params["volume"] = volume.toString()
        MobclickAgent.onEvent(context, "volume_change", params)
    }
}