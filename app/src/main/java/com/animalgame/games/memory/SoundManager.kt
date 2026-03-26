package com.animalgame.games.memory

import android.content.Context
import android.media.AudioAttributes
import android.media.SoundPool
import android.os.Build
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

/**
 * 音效类型
 */
enum class SoundType {
    FLIP,       // 翻牌
    MATCH,      // 配对成功
    UNMATCH,   // 配对失败
    COMPLETE,   // 通关
    CLICK       // 点击
}

/**
 * 音效管理器 - 为记忆翻牌游戏提供音效支持
 * 统一管理所有音效播放，自动读取音量设置
 */
class SoundManager(private val context: Context) {

    private var soundPool: SoundPool? = null

    // 音效ID
    private var flipSound: Int = 0
    private var matchSound: Int = 0
    private var unmatchSound: Int = 0
    private var completeSound: Int = 0
    private var clickSound: Int = 0

    private var isInitialized = false

    // 振动器
    private val vibrator: Vibrator by lazy {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            val vibratorManager = context.getSystemService(Context.VIBRATOR_MANAGER_SERVICE) as VibratorManager
            vibratorManager.defaultVibrator
        } else {
            @Suppress("DEPRECATION")
            context.getSystemService(Context.VIBRATOR_SERVICE) as Vibrator
        }
    }

    // 当前音量（默认值）
    private var currentVolume: Float = 1.0f
    private var vibrationEnabled: Boolean = true

    // 使用简单的作用域来加载设置
    private val scope = CoroutineScope(Dispatchers.IO)

    init {
        initialize()
        // 异步加载音量设置
        scope.launch {
            try {
                val settings = com.animalgame.core.manager.SettingsManager
                    .getSettingsFlow(context)
                    .first()
                currentVolume = settings.soundVolume
                vibrationEnabled = settings.vibrationEnabled
            } catch (e: Exception) {
                // 使用默认值
            }
        }
    }

    private fun initialize() {
        if (isInitialized) return

        try {
            val audioAttributes = AudioAttributes.Builder()
                .setUsage(AudioAttributes.USAGE_GAME)
                .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                .build()

            soundPool = SoundPool.Builder()
                .setMaxStreams(4)
                .setAudioAttributes(audioAttributes)
                .build()

            soundPool?.let { pool ->
                flipSound = loadSound(pool, "flip")
                matchSound = loadSound(pool, "match")
                unmatchSound = loadSound(pool, "unmatch")
                completeSound = loadSound(pool, "complete")
                clickSound = loadSound(pool, "click")
            }

            isInitialized = true
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    private fun loadSound(pool: SoundPool, fileName: String): Int {
        return try {
            val resId = context.resources.getIdentifier(
                fileName,
                "raw",
                context.packageName
            )
            if (resId != 0) {
                pool.load(context, resId, 1)
            } else {
                0
            }
        } catch (e: Exception) {
            0
        }
    }

    /**
     * 播放音效 - 统一入口
     * 根据 soundVolume 调整音量
     * 如果 volume = 0 则不播放
     */
    fun play(type: SoundType) {
        // 如果音量为0，不播放
        if (currentVolume <= 0f) return

        when (type) {
            SoundType.FLIP -> {
                if (flipSound != 0) {
                    soundPool?.play(flipSound, currentVolume * 0.8f, currentVolume * 0.8f, 1, 0, 1f)
                }
            }
            SoundType.MATCH -> {
                if (matchSound != 0) {
                    soundPool?.play(matchSound, currentVolume, currentVolume, 1, 0, 1f)
                }
                // 配对成功也震动
                if (vibrationEnabled) {
                    vibrate(50)
                }
            }
            SoundType.UNMATCH -> {
                if (unmatchSound != 0) {
                    soundPool?.play(unmatchSound, currentVolume * 0.6f, currentVolume * 0.6f, 1, 0, 1f)
                }
            }
            SoundType.COMPLETE -> {
                if (completeSound != 0) {
                    soundPool?.play(completeSound, currentVolume, currentVolume, 1, 0, 1f)
                }
                // 通关震动
                if (vibrationEnabled) {
                    vibrate(200)
                }
            }
            SoundType.CLICK -> {
                if (clickSound != 0) {
                    soundPool?.play(clickSound, currentVolume * 0.5f, currentVolume * 0.5f, 1, 0, 1f)
                }
            }
        }
    }

    /**
     * 快捷方法 - 播放翻牌音效
     */
    fun playFlip() = play(SoundType.FLIP)

    /**
     * 快捷方法 - 播放配对成功音效
     */
    fun playMatch() = play(SoundType.MATCH)

    /**
     * 快捷方法 - 播放配对失败音效
     */
    fun playUnmatch() = play(SoundType.UNMATCH)

    /**
     * 快捷方法 - 播放通关音效
     */
    fun playComplete() = play(SoundType.COMPLETE)

    /**
     * 快捷方法 - 播放点击音效
     */
    fun playClick() = play(SoundType.CLICK)

    /**
     * 震动反馈
     */
    private fun vibrate(duration: Long) {
        if (!vibrationEnabled) return

        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                vibrator.vibrate(VibrationEffect.createOneShot(duration, VibrationEffect.DEFAULT_AMPLITUDE))
            } else {
                @Suppress("DEPRECATION")
                vibrator.vibrate(duration)
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    /**
     * 释放资源
     */
    fun release() {
        soundPool?.release()
        soundPool = null
        isInitialized = false
    }
}
