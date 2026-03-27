package com.animalgame.games.slide

/**
 * 方块推推乐 - 滑块拼图游戏
 * 难度等级
 */
enum class SlideDifficulty(
    val displayName: String,
    val levelCount: Int,
    val maxStepsBonus: Int  // 额外允许的步数
) {
    EASY("简单", 10, 5),
    MEDIUM("中等", 10, 3),
    HARD("困难", 10, 1)
}

/**
 * 关卡配置
 */
data class SlideLevelConfig(
    val level: Int,
    val difficulty: SlideDifficulty,
    val initialState: List<Int>,  // 初始状态，0 表示空位
    val targetState: List<Int>,   // 目标状态
    val optimalSteps: Int          // 最优解步数
) {
    // 最大允许步数 = 最优步数 + 难度加成
    val maxSteps: Int get() = optimalSteps + difficulty.maxStepsBonus
}

/**
 * 网格状态管理
 */
class SlideGrid(initialState: List<Int>) {
    // 9个位置，0 表示空位，1-8 表示方块
    private val _state = initialState.toMutableList()
    val state: List<Int> get() = _state.toList()

    // 获取空位索引
    val emptyIndex: Int get() = _state.indexOf(0)

    // 检查指定位置的方块是否可以移动
    fun canMove(index: Int): Boolean {
        if (index < 0 || index >= 9) return false
        if (_state[index] == 0) return false  // 空位不能移动

        val emptyRow = emptyIndex / 3
        val emptyCol = emptyIndex % 3
        val targetRow = index / 3
        val targetCol = index % 3

        // 只能上下左右相邻
        return (abs(targetRow - emptyRow) + abs(targetCol - emptyCol)) == 1
    }

    // 移动方块到空位
    fun move(index: Int): Boolean {
        if (!canMove(index)) return false

        // 交换位置
        _state[emptyIndex] = _state[index]
        _state[index] = 0
        return true
    }

    // 检查是否达成目标
    fun isTargetReached(target: List<Int>): Boolean {
        return _state == target
    }

    // 克隆当前状态
    fun clone(): SlideGrid {
        return SlideGrid(_state.toList())
    }

    companion object {
        private fun abs(n: Int) = if (n < 0) -n else n
    }
}

/**
 * 使用 BFS 计算最优解步数
 */
fun calculateOptimalSteps(initial: List<Int>, target: List<Int>): Int {
    val visited = mutableSetOf<String>()
    val queue = ArrayDeque<Pair<List<Int>, Int>>()

    queue.add(initial to 0)
    visited.add(initial.joinToString(","))

    while (queue.isNotEmpty()) {
        val (current, steps) = queue.removeFirst()

        if (current == target) return steps

        // 找到空位
        val emptyIdx = current.indexOf(0)
        val row = emptyIdx / 3
        val col = emptyIdx % 3

        // 四个方向尝试
        val directions = listOf(-1 to 0, 1 to 0, 0 to -1, 0 to 1)
        for ((dr, dc) in directions) {
            val newRow = row + dr
            val newCol = col + dc
            if (newRow in 0..2 && newCol in 0..2) {
                val newIdx = newRow * 3 + newCol
                val newState = current.toMutableList().apply {
                    this[emptyIdx] = this[newIdx]
                    this[newIdx] = 0
                }
                val key = newState.joinToString(",")
                if (key !in visited) {
                    visited.add(key)
                    queue.add(newState to steps + 1)
                }
            }
        }
    }

    return 20 // 默认值
}