package com.animalgame.games.schulte

import android.content.Context
import android.graphics.BitmapFactory
import android.graphics.Color
import android.media.MediaPlayer
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.View
import android.widget.GridLayout
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.cardview.widget.CardView
import com.animalgame.R
import com.animalgame.databinding.ActivitySchulteGameBinding

class SchulteGameActivity : AppCompatActivity() {

    private lateinit var binding: ActivitySchulteGameBinding
    private var mediaPlayer: MediaPlayer? = null

    // 难度级别：3x3, 4x4, 5x5, 6x6
    private val levels = listOf(
        Level(3, "简单"),
        Level(4, "中等"),
        Level(5, "困难"),
        Level(6, "挑战")
    )

    private var currentLevelIndex = 0
    private var numbers = mutableListOf<Int>()
    private var currentNumber = 1
    private var startTime = 0L
    private var isGameStarted = false

    private var levelStartTime = 0L
    private var levelTimes = mutableListOf<Long>()

    // 存储格尺寸，在创建Grid时计算
    private var cellSize: Int = 0

    data class Level(val size: Int, val name: String)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivitySchulteGameBinding.inflate(layoutInflater)
        setContentView(binding.root)

        loadGameIcon()
        playBackgroundMusic()
        setupClickListeners()
        showLevelSelect()
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

    override fun onResume() {
        super.onResume()
        playBackgroundMusic()
    }

    override fun onPause() {
        super.onPause()
        mediaPlayer?.pause()
    }

    private fun playBackgroundMusic() {
        try {
            if (mediaPlayer == null) {
                val afd = assets.openFd("music.mp3")
                mediaPlayer = MediaPlayer()
                mediaPlayer?.setDataSource(afd.fileDescriptor, afd.startOffset, afd.length)
                afd.close()
                mediaPlayer?.prepare()
                mediaPlayer?.isLooping = true
            }
            mediaPlayer?.start()
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    private fun setupClickListeners() {
        binding.backButton.setOnClickListener {
            finish()
        }

        // 难度选择按钮
        binding.level1Button.setOnClickListener { startLevel(0) }
        binding.level2Button.setOnClickListener { startLevel(1) }
        binding.level3Button.setOnClickListener { startLevel(2) }
        binding.level4Button.setOnClickListener { startLevel(3) }

        binding.restartButton.setOnClickListener {
            showLevelSelect()
        }

        binding.nextLevelButton.setOnClickListener {
            if (currentLevelIndex < levels.size - 1) {
                currentLevelIndex++
                startLevel(currentLevelIndex)
            } else {
                showAllCompleted()
            }
        }
    }

    private fun showLevelSelect() {
        currentLevelIndex = 0
        levelTimes.clear()

        binding.startScreenContainer.visibility = View.VISIBLE
        binding.gameContainer.visibility = View.GONE
        binding.resultCard.visibility = View.GONE
        binding.levelSelectCard.visibility = View.VISIBLE

        binding.levelTitleText.text = "选择难度开始挑战"
    }

    private fun startLevel(levelIndex: Int) {
        currentLevelIndex = levelIndex
        val level = levels[levelIndex]

        isGameStarted = true
        currentNumber = 1
        levelStartTime = System.currentTimeMillis()
        startTime = System.currentTimeMillis()

        // 生成该级别的数字
        val totalNumbers = level.size * level.size
        numbers = (1..totalNumbers).shuffled().toMutableList()

        binding.startScreenContainer.visibility = View.GONE
        binding.gameContainer.visibility = View.VISIBLE
        binding.resultCard.visibility = View.GONE
        binding.levelSelectCard.visibility = View.GONE

        binding.levelIndicatorText.text = "第${levelIndex + 1}关 / ${levels.size}关 (${level.size}x${level.size})"
        binding.timeText.text = "00:00"

        createGrid(level.size)
    }

    private fun createGrid(size: Int) {
        // 延迟计算格子尺寸，确保在视图测量完成后计算
        binding.gridLayout.post {
            val density = resources.displayMetrics.density
            val screenWidth = resources.displayMetrics.widthPixels

            // 容器左右padding 16dp * 2 = 32dp
            val containerPadding = (32 * density).toInt()
            // grid内部padding 4dp * 2 = 8dp
            val gridPadding = (8 * density).toInt()
            // 格子之间的间隙 (size-1) * 2dp
            val totalSpacing = ((size - 1) * 2 * density).toInt()

            // 总边距
            val totalPadding = containerPadding + gridPadding + totalSpacing

            // 使用屏幕宽度计算，确保居中
            cellSize = (screenWidth - totalPadding) / size

            // 创建格子
            binding.gridLayout.removeAllViews()
            binding.gridLayout.columnCount = size
            binding.gridLayout.rowCount = size

            for (num in numbers) {
                val cell = createCell(num, size)
                binding.gridLayout.addView(cell)
            }
        }
    }

    private fun createCell(number: Int, gridSize: Int): CardView {
        val card = CardView(this).apply {
            layoutParams = GridLayout.LayoutParams().apply {
                width = cellSize
                height = cellSize
                setMargins(1, 1, 1, 1)
            }
            radius = 6f
            setCardBackgroundColor(Color.WHITE)
            cardElevation = 4f
            isClickable = true
            isFocusable = true
        }

        val textView = TextView(this).apply {
            text = number.toString()
            textSize = when {
                gridSize <= 4 -> 28f
                gridSize == 5 -> 24f
                else -> 20f
            }
            setTextColor(Color.parseColor("#424242"))
            gravity = android.view.Gravity.CENTER
            layoutParams = android.widget.LinearLayout.LayoutParams(
                android.widget.LinearLayout.LayoutParams.MATCH_PARENT,
                android.widget.LinearLayout.LayoutParams.MATCH_PARENT
            )
        }

        card.addView(textView)
        card.tag = number

        card.setOnClickListener {
            if (isGameStarted) {
                checkNumber(number, card)
            }
        }

        return card
    }

    private fun checkNumber(number: Int, card: CardView) {
        if (number == currentNumber) {
            // 正确 - 变绿色
            card.setCardBackgroundColor(Color.parseColor("#4CAF50"))
            val textView = card.getChildAt(0) as TextView
            textView.setTextColor(Color.WHITE)

            currentNumber++

            // 更新计时
            updateTimer()

            if (currentNumber > numbers.size) {
                // 完成该关卡
                finishLevel()
            }
        } else {
            // 错误 - 闪红提示
            card.setCardBackgroundColor(Color.parseColor("#FF6B6B"))
            val textView = card.getChildAt(0) as TextView
            textView.setTextColor(Color.WHITE)

            Handler(Looper.getMainLooper()).postDelayed({
                if (isGameStarted) {
                    card.setCardBackgroundColor(Color.WHITE)
                    textView.setTextColor(Color.parseColor("#424242"))
                }
            }, 200)
        }
    }

    private fun updateTimer() {
        val elapsed = System.currentTimeMillis() - startTime
        val seconds = elapsed / 1000
        val minutes = seconds / 60
        val remainingSeconds = seconds % 60
        binding.timeText.text = String.format("%02d:%02d", minutes, remainingSeconds)
    }

    private fun finishLevel() {
        isGameStarted = false
        val levelTime = System.currentTimeMillis() - levelStartTime
        levelTimes.add(levelTime)

        val seconds = levelTime / 1000
        val minutes = seconds / 60
        val remainingSeconds = seconds % 60
        val timeStr = String.format("%02d:%02d", minutes, remainingSeconds)

        binding.timeText.text = timeStr
        binding.resultCard.visibility = View.VISIBLE

        if (currentLevelIndex < levels.size - 1) {
            // 有关卡继续
            binding.resultTitleText.text = "第${currentLevelIndex + 1}关完成！"
            binding.resultTimeText.text = "用时: $timeStr"
            binding.nextLevelButton.visibility = View.VISIBLE
            binding.nextLevelButton.text = "下一关 (${levels[currentLevelIndex + 1].size}x${levels[currentLevelIndex + 1].size})"
            binding.restartButton.text = "重新选择"
        } else {
            // 全部完成
            showAllCompleted()
        }
    }

    private fun showAllCompleted() {
        binding.resultCard.visibility = View.VISIBLE
        binding.resultTitleText.text = "🎉 全部通关！"
        binding.nextLevelButton.visibility = View.GONE

        // 计算总时间
        val totalTime = levelTimes.sum()
        val seconds = totalTime / 1000
        val minutes = seconds / 60
        val remainingSeconds = seconds % 60

        binding.resultTimeText.text = "总用时: ${String.format("%02d:%02d", minutes, remainingSeconds)}\n\n各关用时:\n" +
                levelTimes.mapIndexed { index, time ->
                    val s = time / 1000
                    "第${index + 1}关: ${s}秒"
                }.joinToString("\n")

        binding.restartButton.text = "再玩一次"
    }
}
