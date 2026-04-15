# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## 编译命令

```bash
# 编译 Debug APK
./gradlew assembleDebug

# 编译 Release APK（需要密钥库 app/keys/my-release-key.keystore）
./gradlew assembleRelease

# 清理并重新编译
./gradlew clean assembleDebug
```

**环境要求**: JDK 17+, Gradle 8.0（项目自带）, Android SDK 34

**输出位置**: `app/build/outputs/apk/debug/app-debug.apk`

---

## 架构概述

### 游戏模块模式

所有游戏都通过 `AbstractGameModule` 实现 `GameModule` 接口，遵循以下模式：

1. **状态机**: 游戏发出 `GameState` 状态流转（Idle → Ready → Playing → Completed/Paused）
2. **动作处理**: 用户输入通过 `onUserAction(GameAction)` 处理，返回 `ActionResult?`
3. **数据流**: `state: StateFlow<GameState>` 供 UI 观察，`result: Flow<GameResult?>` 供游戏完成时输出
4. **计时器**: `AbstractGameModule` 内置 `startTimer()` / `stopTimer()` 方法

每个游戏位于 `app/src/main/java/com/animalgame/games/<游戏名>/`，必须：
- 定义 `gameId: String`（唯一标识符，用于导航路由）
- 实现 `startGame()` 游戏逻辑
- 处理 `onUserAction()` 中的用户输入
- 游戏结束时调用 `completeLevel()`

### 导航系统

`GameNavigation.kt` 中使用字符串形式的 `gameId` 路由：
- 路由格式: `game/{gameType}`，其中 `gameType` = `gameId`
- `GameScreen` 通过 `when(gameType)` 分支实例化对应的模块和 UI

### 核心管理器

| 管理器 | 职责 |
|--------|------|
| `GameRegistry` | 所有 `GameModule` 实例的中央注册表 |
| `ScoreManager` | 通过 DataStore 持久化星星/积分 |
| `SettingsManager` | 管理应用设置（音量、音乐等）|
| `LevelManager` | 跟踪游戏进度 |

---

## UI 架构

- **Compose 页面**位于 `ui/screens/`（`GameListScreen`、`GameScreen`、`SettingsScreen`）
- **XML 布局**用于遗留的 Activity（`HomeActivity`、`SettingsActivity`、`AnimalGameActivity`）
- **Compose UI** 随游戏模块放在各自目录下
- **共享组件**位于 `ui/components/`（`GameTopBar`、`DifficultyCard`）
- **主题**定义在 `ui/theme/Theme.kt`

---

## 新增游戏步骤

1. 在 `games/<新游戏>/` 下创建继承 `AbstractGameModule` 的模块类
2. 定义 `gameId`、`gameName`、`iconAsset`、`totalLevels`、`description`
3. 实现 `startGame()` 游戏逻辑
4. 在 `onUserAction()` 中处理用户输入并更新状态
5. 游戏结束时调用 `completeLevel()`
6. 在 `games/<新游戏>/` 下创建对应的 Compose UI 屏幕
7. 在 `GameScreen.kt` 的 `when(gameType)` 分支中注册
8. 使用 Activity 的遗留游戏还需在 `GameNavigation.kt` 中注册

---

## 数据模型

核心模型位于 `core/game/` 和 `core/model/`:
- `GameState` - 游戏生命周期的密封类状态
- `GameResult` - 游戏完成数据（星星、时间、错误次数）
- `GameSettings` - 用户偏好设置
- `GameAction` / `ActionResult` - 输入/输出动作

---

## 依赖库

主要依赖：
- Jetpack Compose + Material3
- Navigation Compose
- DataStore Preferences
- UMeng Analytics（位于 `app/libs/*.aar`）
- Lifecycle ViewModel/Runtime KTX
