package com.animalgame.games.memory

import android.media.MediaPlayer
import android.os.Bundle
import android.view.WindowManager
import androidx.activity.compose.setContent
import androidx.appcompat.app.AppCompatActivity
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.lifecycle.lifecycleScope
import com.animalgame.core.manager.ScoreManager
import com.animalgame.core.model.GameResult
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import com.animalgame.core.manager.SettingsManager

class MemoryGameActivity : AppCompatActivity() {
    private var mediaPlayer: MediaPlayer? = null
    private var isMusicEnabled: Boolean = true
    private var musicVolume: Float = 1.0f

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)

        loadSettingsSync()
        playBackgroundMusic()

        setContent {
            val scoreManager = remember { ScoreManager.getInstance(this@MemoryGameActivity) }
            val module = remember { MemoryGameModule() }

            // 收集结果并保存
            LaunchedEffect(Unit) {
                module.result.collectLatest { result ->
                    result?.let {
                        // 转换为模型使用的 GameResult
                        val modelResult = GameResult(
                            gameId = it.gameId,
                            level = it.level,
                            score = it.score,
                            stars = it.stars,
                            isCompleted = it.isSuccess,
                            timeMillis = it.timeMillis,
                            mistakes = it.mistakes
                        )
                        scoreManager.reportResult(modelResult)
                    }
                }
            }

            // 监听设置变化
            LaunchedEffect(Unit) {
                SettingsManager.getSettingsFlow(this@MemoryGameActivity).collect { settings ->
                    val wasEnabled = isMusicEnabled
                    isMusicEnabled = settings.musicEnabled
                    musicVolume = settings.soundVolume

                    if (wasEnabled != settings.musicEnabled) {
                        if (settings.musicEnabled) {
                            playBackgroundMusic()
                        } else {
                            mediaPlayer?.pause()
                        }
                    }

                    if (settings.musicEnabled && mediaPlayer != null) {
                        mediaPlayer?.setVolume(musicVolume * 0.5f, musicVolume * 0.5f)
                    }
                }
            }

            MemoryGameUI(
                module = module,
                onBack = { finish() }
            )
        }
    }

    private fun loadSettingsSync() {
        lifecycleScope.launch {
            try {
                val settings = SettingsManager.getSettingsFlow(this@MemoryGameActivity).first()
                isMusicEnabled = settings.musicEnabled
                musicVolume = settings.soundVolume
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    private fun playBackgroundMusic() {
        if (!isMusicEnabled || musicVolume <= 0f) return

        try {
            if (mediaPlayer == null) {
                val afd = assets.openFd("music.mp3")
                mediaPlayer = MediaPlayer()
                mediaPlayer?.setDataSource(afd.fileDescriptor, afd.startOffset, afd.length)
                afd.close()
                mediaPlayer?.prepare()
                mediaPlayer?.isLooping = true
                mediaPlayer?.setVolume(musicVolume * 0.5f, musicVolume * 0.5f)
            }
            mediaPlayer?.start()
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    override fun onPause() {
        super.onPause()
        mediaPlayer?.pause()
    }

    override fun onDestroy() {
        super.onDestroy()
        mediaPlayer?.release()
    }
}