package com.animalgame.ui

import android.media.MediaPlayer
import android.os.Bundle
import androidx.activity.compose.setContent
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.animalgame.core.manager.SettingsManager
import com.animalgame.games.animal.registerAnimalGame
import com.animalgame.games.schulte.registerSchulteGame
import com.animalgame.games.memory.registerMemoryGame
import com.animalgame.games.colormind.registerColorMindGame
import com.animalgame.games.gravity.registerGravityGame
import com.animalgame.games.slide.registerSlideGame
import kotlinx.coroutines.launch

class HomeActivity : AppCompatActivity() {

    private var backgroundMusic: MediaPlayer? = null
    private var isMusicEnabled: Boolean = true
    private var musicVolume: Float = 1.0f

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // 加载设置
        loadSettings()

        // 注册所有游戏模块
        registerGames()

        // 使用 Compose 设置内容
        setContent {
            MainScreen()
        }
    }

    private fun loadSettings() {
        lifecycleScope.launch {
            SettingsManager.getSettingsFlow(this@HomeActivity).collect { settings ->
                val wasEnabled = isMusicEnabled
                isMusicEnabled = settings.musicEnabled
                musicVolume = settings.soundVolume

                // 音乐开关状态变化
                if (wasEnabled != settings.musicEnabled) {
                    if (settings.musicEnabled) {
                        playBackgroundMusic()
                    } else {
                        stopBackgroundMusic()
                    }
                }

                // 音量变化（即使开关没变）
                if (settings.musicEnabled && backgroundMusic != null) {
                    backgroundMusic?.setVolume(musicVolume * 0.5f, musicVolume * 0.5f)
                }
            }
        }
    }

    private fun registerGames() {
        registerAnimalGame()
        registerSchulteGame()
        registerMemoryGame()
        registerColorMindGame()
        registerGravityGame()
        registerSlideGame()
    }

    override fun onResume() {
        super.onResume()
        if (isMusicEnabled) {
            playBackgroundMusic()
        }
    }

    override fun onPause() {
        super.onPause()
        stopBackgroundMusic()
    }

    private fun playBackgroundMusic() {
        // 检查音乐开关和音量
        if (!isMusicEnabled || musicVolume <= 0f) return

        try {
            if (backgroundMusic == null) {
                val afd = assets.openFd("backgroud-music.mp3")
                backgroundMusic = MediaPlayer()
                backgroundMusic?.setDataSource(afd.fileDescriptor, afd.startOffset, afd.length)
                afd.close()
                backgroundMusic?.prepare()
                backgroundMusic?.isLooping = true
                backgroundMusic?.setVolume(musicVolume * 0.5f, musicVolume * 0.5f)
            }
            backgroundMusic?.start()
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    private fun stopBackgroundMusic() {
        backgroundMusic?.pause()
    }

    override fun onDestroy() {
        super.onDestroy()
        backgroundMusic?.release()
        backgroundMusic = null
    }
}
