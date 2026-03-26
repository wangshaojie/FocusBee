package com.animalgame.ui

import android.content.Context
import android.os.Bundle
import android.widget.EditText
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.animalgame.R
import com.animalgame.databinding.ActivitySettingsBinding

class SettingsActivity : AppCompatActivity() {

    private lateinit var binding: ActivitySettingsBinding

    companion object {
        const val PREFS_NAME = "animal_game_prefs"
        const val KEY_AUTO_REFRESH = "auto_refresh"
        const val KEY_INTERVAL = "interval"
        const val DEFAULT_INTERVAL = 10
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivitySettingsBinding.inflate(layoutInflater)
        setContentView(binding.root)

        loadSettings()
        setupListeners()
    }

    private fun loadSettings() {
        val prefs = getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val interval = prefs.getInt(KEY_INTERVAL, DEFAULT_INTERVAL)
        val autoRefresh = prefs.getBoolean(KEY_AUTO_REFRESH, true)

        binding.intervalInput.setText(interval.toString())
        binding.autoRefreshSwitch.isChecked = autoRefresh
    }

    private fun setupListeners() {
        binding.saveButton.setOnClickListener {
            saveSettings()
        }

        binding.backButton.setOnClickListener {
            finish()
        }
    }

    private fun saveSettings() {
        val intervalStr = binding.intervalInput.text.toString()
        val interval = intervalStr.toIntOrNull()

        if (interval == null || interval < 1 || interval > 999) {
            Toast.makeText(this, "请输入1-999之间的数字", Toast.LENGTH_SHORT).show()
            return
        }

        val prefs = getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        prefs.edit().apply {
            putInt(KEY_INTERVAL, interval)
            putBoolean(KEY_AUTO_REFRESH, binding.autoRefreshSwitch.isChecked)
            apply()
        }

        Toast.makeText(this, "设置已保存", Toast.LENGTH_SHORT).show()
        finish()
    }
}
