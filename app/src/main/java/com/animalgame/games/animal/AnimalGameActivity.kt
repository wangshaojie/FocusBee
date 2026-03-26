package com.animalgame.games.animal

import android.animation.AnimatorSet
import android.animation.ObjectAnimator
import android.content.Context
import android.content.Intent
import android.graphics.BitmapFactory
import android.media.MediaPlayer
import android.os.Bundle
import android.os.CountDownTimer
import android.os.Handler
import android.os.Looper
import android.view.Gravity
import android.view.View
import android.view.animation.AccelerateDecelerateInterpolator
import android.widget.GridLayout
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.lifecycle.lifecycleScope
import com.animalgame.R
import com.animalgame.core.manager.SettingsManager
import com.animalgame.databinding.ActivityAnimalGameBinding
import com.animalgame.ui.SettingsActivity
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

class AnimalGameActivity : AppCompatActivity() {

    private lateinit var binding: ActivityAnimalGameBinding
    private var mediaPlayer: MediaPlayer? = null
    private var isMusicEnabled: Boolean = true
    private var musicVolume: Float = 1.0f

    private val animals = listOf(
        Animal("狗", "🐕"),
        Animal("猫", "🐱"),
        Animal("羊", "🐑"),
        Animal("牛", "🐮"),
        Animal("猪", "🐷"),
        Animal("鸡", "🐔"),
        Animal("鸭", "🐦"),
        Animal("蛙", "🐸")
    )

    private val requiredAnimals = setOf("狗", "猫", "羊")

    private var countDownTimer: CountDownTimer? = null
    private var isGameStarted = false
    private var autoRefreshHandler: Handler? = null
    private var autoRefreshRunnable: Runnable? = null
    private var isAutoRefreshEnabled = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityAnimalGameBinding.inflate(layoutInflater)
        setContentView(binding.root)

        loadGameIcon()
        loadSettingsSync()
        playBackgroundMusic()
        setupClickListeners()
    }

    private fun loadGameIcon() {
        try {
            val inputStream = assets.open("logo1.png")
            val bitmap = BitmapFactory.decodeStream(inputStream)
            binding.gameIcon.setImageBitmap(bitmap)
            inputStream.close()
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    // 同步加载设置
    private fun loadSettingsSync() {
        val prefs = getSharedPreferences(SettingsActivity.PREFS_NAME, Context.MODE_PRIVATE)
        isAutoRefreshEnabled = prefs.getBoolean(SettingsActivity.KEY_AUTO_REFRESH, true)

        lifecycleScope.launch {
            try {
                val settings = SettingsManager.getSettingsFlow(this@AnimalGameActivity).first()
                isMusicEnabled = settings.musicEnabled
                musicVolume = settings.soundVolume

                if (mediaPlayer != null) {
                    mediaPlayer?.setVolume(musicVolume * 0.5f, musicVolume * 0.5f)
                    if (!isMusicEnabled) {
                        mediaPlayer?.pause()
                    }
                }
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

    override fun onResume() {
        super.onResume()
        playBackgroundMusic()
        loadSettings()
    }

    override fun onPause() {
        super.onPause()
        mediaPlayer?.pause()
    }

    private fun loadSettings() {
        val prefs = getSharedPreferences(SettingsActivity.PREFS_NAME, Context.MODE_PRIVATE)
        isAutoRefreshEnabled = prefs.getBoolean(SettingsActivity.KEY_AUTO_REFRESH, true)

        lifecycleScope.launch {
            SettingsManager.getSettingsFlow(this@AnimalGameActivity).collect { settings ->
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

        if (isGameStarted && isAutoRefreshEnabled) {
            startAutoRefresh()
        }
    }

    private fun setupClickListeners() {
        // 返回按钮
        binding.backButton.setOnClickListener {
            finish()
        }

        binding.startButton.setOnClickListener {
            if (!isGameStarted) {
                startGame()
            }
        }

        binding.refreshButton.setOnClickListener {
            refreshAnimals()
        }

        binding.settingsButton.setOnClickListener {
            startActivity(Intent(this, SettingsActivity::class.java))
        }
    }

    private fun startGame() {
        isGameStarted = true
        binding.startScreenContainer.visibility = View.GONE
        binding.countdownText.visibility = View.VISIBLE

        startCountdown()
    }

    private fun startCountdown() {
        countDownTimer?.cancel()

        countDownTimer = object : CountDownTimer(3000, 1000) {
            override fun onTick(millisUntilFinished: Long) {
                val secondsRemaining = (millisUntilFinished / 1000).toInt() + 1
                binding.countdownText.text = secondsRemaining.toString()
                animateCountdown(binding.countdownText)
            }

            override fun onFinish() {
                binding.countdownText.visibility = View.GONE
                binding.animalGridLayout.visibility = View.VISIBLE
                binding.refreshButton.visibility = View.VISIBLE
                showAnimals()

                if (isAutoRefreshEnabled) {
                    startAutoRefresh()
                }
            }
        }.start()
    }

    private fun startAutoRefresh() {
        val prefs = getSharedPreferences(SettingsActivity.PREFS_NAME, Context.MODE_PRIVATE)
        val interval = prefs.getInt(SettingsActivity.KEY_INTERVAL, SettingsActivity.DEFAULT_INTERVAL)

        stopAutoRefresh()

        autoRefreshHandler = Handler(Looper.getMainLooper())
        autoRefreshRunnable = object : Runnable {
            override fun run() {
                if (isGameStarted && isAutoRefreshEnabled) {
                    refreshAnimals()
                    autoRefreshHandler?.postDelayed(this, (interval * 1000).toLong())
                }
            }
        }
        autoRefreshHandler?.postDelayed(autoRefreshRunnable!!, (interval * 1000).toLong())
    }

    private fun stopAutoRefresh() {
        autoRefreshRunnable?.let {
            autoRefreshHandler?.removeCallbacks(it)
        }
        autoRefreshHandler = null
        autoRefreshRunnable = null
    }

    private fun animateCountdown(textView: TextView) {
        val scaleX = ObjectAnimator.ofFloat(textView, "scaleX", 0.5f, 1.2f, 1f)
        val scaleY = ObjectAnimator.ofFloat(textView, "scaleY", 0.5f, 1.2f, 1f)

        AnimatorSet().apply {
            playTogether(scaleX, scaleY)
            duration = 300
            interpolator = AccelerateDecelerateInterpolator()
            start()
        }
    }

    private fun showAnimals() {
        val selectedAnimals = getRandomAnimals()
        binding.animalGridLayout.removeAllViews()

        selectedAnimals.forEachIndexed { index, animal ->
            val cardView = createAnimalCard(animal)
            binding.animalGridLayout.addView(cardView)

            cardView.alpha = 0f
            cardView.animate()
                .alpha(1f)
                .setDuration(300)
                .setStartDelay((index * 80).toLong())
                .start()
        }
    }

    private fun getRandomAnimals(): List<Animal> {
        val result = animals.filter { it.name in requiredAnimals }.toMutableList()
        val remainingAnimals = animals.filter { it.name !in requiredAnimals }
        val shuffled = remainingAnimals.shuffled().take(5)
        result.addAll(shuffled)
        return result.shuffled()
    }

    private fun createAnimalCard(animal: Animal): View {
        val card = android.widget.LinearLayout(this).apply {
            orientation = android.widget.LinearLayout.VERTICAL
            gravity = Gravity.CENTER
            setBackgroundResource(R.drawable.animal_card_background)

            val params = GridLayout.LayoutParams().apply {
                width = resources.getDimensionPixelSize(R.dimen.animal_card_width)
                height = resources.getDimensionPixelSize(R.dimen.animal_card_height)
                setMargins(16, 16, 16, 16)
            }
            layoutParams = params
        }

        val emojiText = TextView(this).apply {
            text = animal.emoji
            textSize = 48f
            gravity = Gravity.CENTER
        }

        val nameText = TextView(this).apply {
            text = animal.name
            textSize = 16f
            setTextColor(ContextCompat.getColor(this@AnimalGameActivity, R.color.text_primary))
            gravity = Gravity.CENTER
            setPadding(0, 8, 0, 0)
        }

        card.addView(emojiText)
        card.addView(nameText)

        return card
    }

    private fun refreshAnimals() {
        for (i in 0 until binding.animalGridLayout.childCount) {
            val child = binding.animalGridLayout.getChildAt(i)
            child.animate()
                .alpha(0f)
                .setDuration(200)
                .withEndAction {
                    if (i == binding.animalGridLayout.childCount - 1) {
                        showAnimals()
                    }
                }
                .start()
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        countDownTimer?.cancel()
        stopAutoRefresh()
        mediaPlayer?.stop()
        mediaPlayer?.release()
        mediaPlayer = null
    }

    data class Animal(val name: String, val emoji: String)
}
