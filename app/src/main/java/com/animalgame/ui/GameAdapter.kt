package com.animalgame.ui

import android.content.Context
import android.graphics.BitmapFactory
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.animalgame.R
import java.io.File

data class GameInfo(
    val name: String,
    val iconAsset: String?,
    val activityClass: Class<*>
)

class GameAdapter(
    private val context: Context,
    private val games: List<GameInfo>
) : RecyclerView.Adapter<GameAdapter.GameViewHolder>() {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): GameViewHolder {
        val view = LayoutInflater.from(context).inflate(R.layout.item_game, parent, false)
        return GameViewHolder(view)
    }

    override fun onBindViewHolder(holder: GameViewHolder, position: Int) {
        val game = games[position]
        holder.bind(game)
    }

    override fun getItemCount(): Int = games.size

    inner class GameViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val gameIcon: ImageView = itemView.findViewById(R.id.gameIcon)
        private val gameName: TextView = itemView.findViewById(R.id.gameName)

        fun bind(game: GameInfo) {
            gameName.text = game.name

            // Load icon from assets
            game.iconAsset?.let { assetName ->
                try {
                    val inputStream = context.assets.open(assetName)
                    val bitmap = BitmapFactory.decodeStream(inputStream)
                    gameIcon.setImageBitmap(bitmap)
                    inputStream.close()
                } catch (e: Exception) {
                    gameIcon.setImageResource(R.drawable.ic_launcher_background)
                }
            } ?: gameIcon.setImageResource(R.drawable.ic_launcher_background)

            itemView.setOnClickListener {
                val intent = android.content.Intent(context, game.activityClass)
                context.startActivity(intent)
            }
        }
    }
}
