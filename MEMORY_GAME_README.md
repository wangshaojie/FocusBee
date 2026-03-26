# 记忆翻牌游戏 - 开发规范

## 1. 网格配置规则

### 关卡设置
| 难度 | rows × columns | cards.size | pairCount | 关卡范围 |
|------|----------------|------------|-----------|----------|
| 简单 | 3 × 4 | 12 | 6 | 1-50 |
| 中等 | 4 × 4 | 16 | 8 | 51-100 |
| 困难 | 4 × 5 | 20 | 10 | 101-150 |
| 挑战 | 5 × 6 | 30 | 15 | 151-200 |

### 关卡结构
- **总关卡数**: 200关（4个难度 × 50关）
- **难度分离**: 难度与关卡分离管理，下一关只在同难度内跳转
- **难度完成提示**: 难度内全部通关后显示 "🎉 XX难度已全部通关！"

### 难度配置示例（MemoryGameModule.kt）
```kotlin
enum class Difficulty(val levelCount: Int, val gridRows: Int, val gridColumns: Int, val displayName: String) {
    EASY(50, 3, 4, "简单"),       // 3x4 = 12张 (6对)
    MEDIUM(50, 4, 4, "中等"),     // 4x4 = 16张 (8对)
    HARD(50, 4, 5, "困难"),      // 4x5 = 20张 (10对)
    EXPERT(50, 5, 6, "挑战")     // 5x6 = 30张 (15对)
}

// 根据 level (1-200) 设置难度和关卡索引
private fun setLevel(level: Int) {
    currentDifficulty = when {
        level <= 50 -> Difficulty.EASY
        level <= 100 -> Difficulty.MEDIUM
        level <= 150 -> Difficulty.HARD
        else -> Difficulty.EXPERT
    }
    levelIndex = (level - 1) % 50  // 0-49
}

// 下一关只在本难度内跳转
fun nextLevel() {
    if (levelIndex < currentDifficulty.levelCount - 1) {
        levelIndex++
        startGame()
    }
    // 难度完成后不自动跳转，由 UI 显示完成提示
}
```

### 核心规则
- **gridSize** = rows × columns（必须是偶数）
- **pairCount** = gridSize / 2（动态计算，禁止写死）
- 卡片数量必须严格等于 `rows * columns`

### 配置示例（GridSize 数据类）
```kotlin
data class GridSize(val rows: Int, val columns: Int) {
    val totalCards: Int = rows * columns
    val pairCount: Int = totalCards / 2  // 动态计算

    init {
        require(totalCards % 2 == 0) { "gridSize 必须是偶数" }
    }
}
```

---

## 2. 必需的开发规范

### 禁止事项
- ❌ 禁止写死 pairCount（如 `pairs = 8`）
- ❌ 禁止在 UI 层面硬编码网格列数
- ❌ 禁止使用固定 padding 定位

### 强制校验
```kotlin
// 生成卡片后必须校验
val expectedCount = gridSize.rows * gridSize.columns
if (cards.size != expectedCount) {
    throw IllegalStateException(
        "卡片数量错误！期望: $expectedCount, 实际: ${cards.size}"
    )
}
```

### 数据传递
- 将 `gridRows`、`gridColumns` 传入 GameState.data
- UI 层从 state.data 获取网格配置，不自行计算

---

## 3. UI 布局规范

### 主容器结构
```kotlin
Column(
    modifier = Modifier.fillMaxSize(),
    verticalArrangement = Arrangement.Center,  // 垂直居中
    horizontalAlignment = Alignment.CenterHorizontally
) {
    // 顶部信息栏
    TopBar()

    // 游戏区域 - 使用 weight 居中
    Box(modifier = Modifier.weight(1f)) {
        LazyVerticalGrid(
            columns = GridCells.Fixed(gridColumns),  // 使用配置的值
            // ...
        )
    }

    // 底部按钮
    BottomButton()
}
```

### 列数设置
- 直接使用 `state.data["gridColumns"]` 获取列数
- 不需要根据 totalPairs 动态计算

---

## 4. 动画规范

### 进入动画（注意事项）
⚠️ **避免使用 `LaunchedEffect(Unit)` 触发的重复动画**
- 这会导致每次状态变化都触发动画
- 正确做法：使用 `remember(state.level)` 只在关卡切换时触发

### 错误示例（会导致点击时界面变大）
```kotlin
var isVisible by remember { mutableStateOf(false) }
LaunchedEffect(Unit) { isVisible = true }

val scale by animateFloatAsState(
    targetValue = if (isVisible) 1f else 0.8f,  // ❌ 动态目标值
    // ...
)
```

### 正确示例
```kotlin
// 使用 remember(state.level) 保存状态，只在关卡变化时重算
var isVisible by remember(state.level) { mutableStateOf(true) }

val scale by animateFloatAsState(
    targetValue = 1f,  // ✅ 固定目标值
    // ...
)
```

### 推荐动画配置
```kotlin
val scale by animateFloatAsState(
    targetValue = 1f,
    animationSpec = tween(durationMillis = 400, easing = FastOutSlowInEasing),
    label = "enterScale"
)

val alpha by animateFloatAsState(
    targetValue = 1f,
    animationSpec = tween(durationMillis = 400),
    label = "enterAlpha"
)
```

---

## 5. 文件结构

```
app/src/main/java/com/animalgame/games/memory/
├── MemoryGameModule.kt      # 游戏逻辑核心
├── MemoryGameScreen.kt      # Compose UI
├── MemoryGameActivity.kt    # Activity 封装
├── GameIconManager.kt       # 图标资源管理
└── MemoryGameCardData.kt    # 卡片数据结构
```

---

## 6. 常见问题

### Q: 图标数量不够怎么办？
A: GameIconManager 提供 28+ 个图标（动物8个、食物8个、交通5个、符号5个、物品2个），足够 30 张卡片（15对）使用。

### Q: 如何确保卡片能一屏显示？
A:
1. 减小间距：`Arrangement.spacedBy(4.dp)`
2. 紧凑顶部信息栏
3. 使用合适的 gridColumns（4列或6列）

### Q: 动画点击时闪烁怎么办？
A: 检查是否使用了动态 targetValue，确保使用 `targetValue = 1f` 固定值。
