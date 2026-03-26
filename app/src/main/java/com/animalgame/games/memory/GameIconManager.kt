package com.animalgame.games.memory

import androidx.annotation.DrawableRes
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb

/**
 * 游戏图标数据结构
 */
data class GameIcon(
    val id: String,
    @DrawableRes val resourceId: Int,
    val category: IconCategory,
    val defaultColor: Color = Color.White
)

/**
 * 图标分类
 */
enum class IconCategory {
    ANIMAL,
    FOOD,
    TRANSPORT,
    SYMBOL,
    OBJECT
}

/**
 * 图标管理器 - 提供统一的图标加载方式
 */
object GameIconManager {

    // 动物图标 (8个)
    val animals = listOf(
        GameIcon("dog", com.animalgame.R.drawable.ic_icon_dog, IconCategory.ANIMAL),
        GameIcon("cat", com.animalgame.R.drawable.ic_icon_cat, IconCategory.ANIMAL),
        GameIcon("mouse", com.animalgame.R.drawable.ic_icon_mouse, IconCategory.ANIMAL),
        GameIcon("rabbit", com.animalgame.R.drawable.ic_icon_rabbit, IconCategory.ANIMAL),
        GameIcon("fox", com.animalgame.R.drawable.ic_icon_fox, IconCategory.ANIMAL),
        GameIcon("bear", com.animalgame.R.drawable.ic_icon_bear, IconCategory.ANIMAL),
        GameIcon("panda", com.animalgame.R.drawable.ic_icon_panda, IconCategory.ANIMAL),
        GameIcon("lion", com.animalgame.R.drawable.ic_icon_lion, IconCategory.ANIMAL)
    )

    // 食物图标 (8个)
    val foods = listOf(
        GameIcon("apple", com.animalgame.R.drawable.ic_icon_apple, IconCategory.FOOD),
        GameIcon("banana", com.animalgame.R.drawable.ic_icon_banana, IconCategory.FOOD),
        GameIcon("grape", com.animalgame.R.drawable.ic_icon_grape, IconCategory.FOOD),
        GameIcon("orange", com.animalgame.R.drawable.ic_icon_orange, IconCategory.FOOD),
        GameIcon("strawberry", com.animalgame.R.drawable.ic_icon_strawberry, IconCategory.FOOD),
        GameIcon("watermelon", com.animalgame.R.drawable.ic_icon_watermelon, IconCategory.FOOD),
        GameIcon("cake", com.animalgame.R.drawable.ic_icon_cake, IconCategory.FOOD),
        GameIcon("pizza", com.animalgame.R.drawable.ic_icon_pizza, IconCategory.FOOD)
    )

    // 交通图标 (5个)
    val transports = listOf(
        GameIcon("car", com.animalgame.R.drawable.ic_icon_car, IconCategory.TRANSPORT),
        GameIcon("airplane", com.animalgame.R.drawable.ic_icon_airplane, IconCategory.TRANSPORT),
        GameIcon("boat", com.animalgame.R.drawable.ic_icon_boat, IconCategory.TRANSPORT),
        GameIcon("train", com.animalgame.R.drawable.ic_icon_train, IconCategory.TRANSPORT),
        GameIcon("bicycle", com.animalgame.R.drawable.ic_icon_bicycle, IconCategory.TRANSPORT)
    )

    // 符号图标 (5个)
    val symbols = listOf(
        GameIcon("star", com.animalgame.R.drawable.ic_icon_star, IconCategory.SYMBOL),
        GameIcon("heart", com.animalgame.R.drawable.ic_icon_heart, IconCategory.SYMBOL),
        GameIcon("flower", com.animalgame.R.drawable.ic_icon_flower, IconCategory.SYMBOL),
        GameIcon("sun", com.animalgame.R.drawable.ic_icon_sun, IconCategory.SYMBOL),
        GameIcon("moon", com.animalgame.R.drawable.ic_icon_moon, IconCategory.SYMBOL)
    )

    // 物品图标 (2个)
    val objects = listOf(
        GameIcon("book", com.animalgame.R.drawable.ic_icon_book, IconCategory.OBJECT),
        GameIcon("ball", com.animalgame.R.drawable.ic_icon_ball, IconCategory.OBJECT)
    )

    /**
     * 获取所有图标
     */
    fun getAllIcons(): List<GameIcon> = animals + foods + transports + symbols + objects

    /**
     * 按分类获取图标
     */
    fun getIconsByCategory(category: IconCategory): List<GameIcon> {
        return when (category) {
            IconCategory.ANIMAL -> animals
            IconCategory.FOOD -> foods
            IconCategory.TRANSPORT -> transports
            IconCategory.SYMBOL -> symbols
            IconCategory.OBJECT -> objects
        }
    }

    /**
     * 根据ID获取图标
     */
    fun getIconById(id: String): GameIcon? {
        return getAllIcons().find { it.id == id }
    }

    /**
     * 获取随机图标列表（指定数量）
     */
    fun getRandomIcons(count: Int): List<GameIcon> {
        return getAllIcons().shuffled().take(count)
    }

    /**
     * 获取足够数量的随机图标（用于配对）
     */
    fun getIconsForPairing(pairCount: Int): List<GameIcon> {
        val allIcons = getAllIcons()
        val shuffled = allIcons.shuffled()
        return shuffled.take(pairCount)
    }
}
