
### 各层级解析
1. **RootWindowContainer**  
   • **作用**：根窗口容器，管理所有显示屏幕的顶层窗口层级。  
   • **层级位置**：整个窗口树的根节点。

2. **DisplayContent**  
   • **作用**：对应物理显示屏幕（如手机屏幕、平板等），一个设备可能有多个 `DisplayContent`。  
   • **层级位置**：`RootWindowContainer` 的直接子节点。

3. **TaskDisplayArea**  
   • **作用**：管理应用任务（`Task`）的容器，对应窗口层级中的 **应用层**（Z 轴第 2 层）。  
   • **层级位置**：`DisplayContent` 的子节点。

4. **Task**  
   • **作用**：表示一个应用任务（如多任务切换中的单个应用），包含一个或多个 `ActivityRecord`。  
   • **层级位置**：`TaskDisplayArea` 的子节点，可嵌套其他 `Task`（如对话框任务）。

5. **ActivityRecord**  
   • **作用**：对应应用进程中的 `Activity` 实例，管理 Activity 的窗口和生命周期。  
   • **层级位置**：`Task` 的子节点，继承自 `WindowToken`。

6. **WindowToken**  
   • **作用**：唯一标识窗口的令牌，用于管理窗口的创建、销毁和权限控制。  
   • **层级位置**：`ActivityRecord` 的子节点，直接关联 `WindowState`。

7. **WindowState**  
   • **作用**：表示一个具体的窗口实例（如 Activity 主窗口、对话框窗口），包含窗口属性、绘制逻辑等。  
   • **层级位置**：`WindowToken` 的子节点，是 WMS（窗口管理服务）中实际管理的窗口对象。

8. **WindowStateAnimator**
• **非独立层级节点**：`WindowStateAnimator` 是 `WindowState` 的内部类，负责窗口动画（如窗口进入/退出动画）的实现，不参与窗口层级结构的管理。  
• **层级归属**：其生命周期与 `WindowState` 绑定，属于 `WindowState` 的实现细节，而非独立的层级节点。
### **应用窗口层级结构**
```
RootWindowContainer
└── DisplayContent
    └── TaskDisplayArea
        └── Task
            └── ActivityRecord
                └── WindowToken
                    └── WindowState
```

### 输入法层级结构
1. **窗口层级关系**：  
   在 Android 的窗口管理系统中，`DisplayArea` 是 `RootWindowContainer` 的子节点，负责管理屏幕的显示区域。`Tokens` 是 `DisplayArea` 的直接子节点，用于管理窗口的层级和 Z 轴顺序。输入法窗口（如软键盘）的 `WindowState` 会作为 `Tokens` 的子节点挂载，层级位于普通应用窗口之上（例如窗口层级 15-16）。

2. **输入法窗口的特殊性**：  
   输入法窗口由系统服务（如 `InputMethodManagerService`）直接管理，不依附于某个 `Activity` 的 `ActivityRecord`。其 `WindowToken` 由系统生成，并挂载到 `Tokens` 下，确保输入法窗口可以全局覆盖其他窗口。

3. **层级示例**：  
   ```
   DisplayArea
   └── Tokens
       └── ImeContainer (输入法窗口容器)
           └── WindowToken
               └── WindowState (输入法窗口实例)
   ```  

### 悬浮窗层级结构
悬浮窗的层级结构与普通应用窗口类似，但起点不同：
```
RootWindowContainer
└── DisplayContent
    └── TaskDisplayArea
        └── (悬浮窗的 Task 或 Dialog 对应的窗口)
            └── WindowToken
                └── WindowState
```
• **关键区别**：悬浮窗的直接父节点是 `TaskDisplayArea` 或 `RootWindowContainer`，而不是某个 `Task` 的 `ActivityRecord`。
• **原因**：悬浮窗通常由系统服务（如 `WindowManager`）直接创建，不依附于某个 `Activity` 的生命周期，因此无需通过 `ActivityRecord` 管理。

---

### 悬浮窗与 `ActivityRecord`
• **普通应用窗口**：每个 `Activity` 对应一个 `ActivityRecord`，通过 `WindowToken` 关联 `WindowState`，最终形成层级结构。
• **悬浮窗**：
  • **无 `ActivityRecord`**：悬浮窗（如 `Dialog`）的 `WindowToken` 由 `WindowManager` 直接生成，不绑定到任何 `Activity`。
  • **独立层级**：悬浮窗的 `WindowState` 直接挂载在 `RootWindowContainer` 或 `TaskDisplayArea` 下，层级高于普通应用窗口。

在 Android 系统中，**壁纸（Wallpaper）** 是作为整个窗口层级结构的最底层存在的，其层级编号为 **0**。壁纸的主要作用是为所有上层窗口（如应用窗口、系统栏等）提供一个背景基础。以下是壁纸在窗口层级结构中的详细说明：

---

### 壁纸层级结构

```
RootWindowContainer
└── DisplayContent
    └── WallpaperContainer (壁纸容器)
        └── WallpaperWindowToken
            └── WallpaperWindowState (壁纸窗口实例)
```

• **RootWindowContainer**：整个窗口管理系统的根容器，包含所有显示区域（`DisplayContent`）。
• **DisplayContent**：对应一个物理显示屏幕，管理该屏幕上的所有窗口和壁纸。
• **WallpaperContainer**：专门用于管理壁纸的容器，通常是一个 `WindowContainer` 的子类。
  • **WallpaperWindowToken**：壁纸的窗口令牌（`WindowToken`），标识和管理壁纸窗口。
    ◦ **WallpaperWindowState**：实际的壁纸窗口实例（`WindowState`），负责绘制和显示壁纸内容。

### 层级编号
#### WindowManager.LayoutParams 的 type 分类

`WindowManager.LayoutParams.type` 定义了窗口的类型和层级，不同类型的窗口有不同的 Z-order 范围和特性。以下是具体的分类和数值范围：

#### 应用窗口 (Application Windows) - 1~99

| 类型常量 | 值 | 描述 |
|---------|---|------|
| `TYPE_BASE_APPLICATION` | 1 | 应用的基础窗口，所有其他应用窗口的父窗口 |
| `TYPE_APPLICATION` | 2 | 普通的应用窗口（Activity 的主窗口） |
| `TYPE_APPLICATION_STARTING` | 3 | 应用启动时显示的窗口（已弃用） |
| `TYPE_DRAWN_APPLICATION` | 4 | 当应用绘制完成后替换 STARTING 类型的窗口 |

#### 子窗口 (Sub Windows) - 1000~1999

这些窗口必须依附于一个父窗口（应用窗口）

| 类型常量 | 值 | 描述 |
|---------|---|------|
| `TYPE_APPLICATION_PANEL` | 1000 | 应用面板窗口 |
| `TYPE_APPLICATION_MEDIA` | 1001 | 媒体窗口（如视频播放） |
| `TYPE_APPLICATION_SUB_PANEL` | 1002 | 子面板窗口 |
| `TYPE_APPLICATION_ATTACHED_DIALOG` | 1003 | 附着于Activity的对话框 |
| `TYPE_APPLICATION_MEDIA_OVERLAY` | 1004 | 媒体覆盖窗口（如视频字幕） |
| `TYPE_APPLICATION_ABOVE_SUB_PANEL` | 1005 | 位于子面板之上的窗口 |

#### 系统窗口 (System Windows) - 2000~2999

这些是系统级窗口，不需要依附于应用窗口

| 类型常量 | 值 | 描述 |
|---------|---|------|
| `TYPE_STATUS_BAR` | 2000 | 状态栏 |
| `TYPE_SEARCH_BAR` | 2001 | 搜索栏 |
| `TYPE_PHONE` | 2002 | 电话窗口（来电显示） |
| `TYPE_SYSTEM_ALERT` | 2003 | 系统提示窗口（需要 SYSTEM_ALERT_WINDOW 权限） |
| `TYPE_KEYGUARD` | 2004 | 锁屏窗口 |
| `TYPE_TOAST` | 2005 | Toast 提示窗口 |
| `TYPE_SYSTEM_OVERLAY` | 2006 | 系统覆盖窗口（已弃用，用 TYPE_APPLICATION_OVERLAY 替代） |
| `TYPE_PRIORITY_PHONE` | 2007 | 高优先级电话窗口 |
| `TYPE_SYSTEM_DIALOG` | 2008 | 系统对话框 |
| `TYPE_KEYGUARD_DIALOG` | 2009 | 锁屏对话框 |
| `TYPE_SYSTEM_ERROR` | 2010 | 系统错误窗口（如ANR对话框） |
| `TYPE_INPUT_METHOD` | 2011 | 输入法窗口 |
| `TYPE_INPUT_METHOD_DIALOG` | 2012 | 输入法对话框 |
| `TYPE_WALLPAPER` | 2013 | 壁纸窗口 |
| `TYPE_STATUS_BAR_PANEL` | 2014 | 状态栏面板 |
| `TYPE_SECURE_SYSTEM_OVERLAY` | 2015 | 安全系统覆盖窗口 |
| `TYPE_DRAG` | 2016 | 拖放窗口 |
| `TYPE_STATUS_BAR_SUB_PANEL` | 2017 | 状态栏子面板 |
| `TYPE_POINTER` | 2018 | 鼠标指针窗口 |
| `TYPE_NAVIGATION_BAR` | 2019 | 导航栏 |
| `TYPE_VOLUME_OVERLAY` | 2020 | 音量控制覆盖窗口 |
| `TYPE_BOOT_PROGRESS` | 2021 | 启动进度窗口 |
| `TYPE_INPUT_CONSUMER` | 2022 | 输入消费者窗口 |
| `TYPE_NAVIGATION_BAR_PANEL` | 2024 | 导航栏面板 |
| `TYPE_DISPLAY_OVERLAY` | 2026 | 显示覆盖窗口 |
| `TYPE_MAGNIFICATION_OVERLAY` | 2027 | 放大镜覆盖窗口 |
| `TYPE_ACCESSIBILITY_OVERLAY` | 2028 | 无障碍服务覆盖窗口 |
| `TYPE_APPLICATION_OVERLAY` | 2038 | 应用覆盖窗口（替代 SYSTEM_OVERLAY） |

#### 其他特殊窗口类型

| 类型常量 | 值 | 描述 |
|---------|---|------|
| `TYPE_PRIVATE_PRESENTATION` | 2037 | 私有演示窗口 |
| `TYPE_QS_DIALOG` | 2039 | 快速设置对话框 |
| `TYPE_SCREENSHOT` | 2040 | 截图窗口 |
| `TYPE_PRESENTATION` | 2041 | 演示窗口（如投屏） |
| `TYPE_APPLICATION_MUSIC` | 2043 | 音乐播放窗口 |

#### 使用注意事项

1. 应用窗口 (1-99) 和子窗口 (1000-1999) 通常由应用内部使用
2. 系统窗口 (2000+) 大多需要特殊权限，如：
   - `SYSTEM_ALERT_WINDOW` 用于 TYPE_SYSTEM_ALERT
   - `TYPE_APPLICATION_OVERLAY` 需要 Android O (8.0) 及以上版本
3. 从 Android O 开始，`TYPE_SYSTEM_OVERLAY` 被弃用，推荐使用 `TYPE_APPLICATION_OVERLAY`
4. 最终 Z-order = type (主层级) + sub-layer (子顺序) + 窗口添加顺序 + 其他标志位

在 Android 窗口系统中，**其他标志位（Other Flags）** 是指 `WindowManager.LayoutParams.flags` 中定义的一系列二进制标志，用于微调窗口的行为、交互和层级关系。这些标志通过按位或（`|`）组合使用，可以显著影响窗口的 Z-order、触摸事件处理、显示方式等。

#### 主要标志位分类及作用

##### **1. 影响窗口 Z-order 的标志位**
这些标志会直接或间接改变窗口的层级关系：

| 标志位 | 值（十六进制） | 说明 |
|--------|--------------|------|
| **`FLAG_LAYOUT_IN_SCREEN`** | `0x00000100` | 强制窗口占满整个屏幕，忽略系统装饰（如状态栏） |
| **`FLAG_LAYOUT_NO_LIMITS`** | `0x00000200` | 允许窗口延伸到屏幕之外（如悬浮球） |
| **`FLAG_NOT_FOCUSABLE`** | `0x00000008` | 窗口不获取焦点，但仍可显示（如 Toast） |
| **`FLAG_NOT_TOUCHABLE`** | `0x00000010` | 窗口不接受任何触摸事件 |
| **`FLAG_NOT_TOUCH_MODAL`** | `0x00000020` | 窗口只处理自身区域内的触摸事件，外部事件传递给下层窗口 |
| **`FLAG_ALT_FOCUSABLE_IM`** | `0x00020000` | 与输入法窗口交互相关，影响焦点获取 |
| **`FLAG_WATCH_OUTSIDE_TOUCH`** | `0x00040000` | 允许监听窗口外部的触摸事件（需配合 `FLAG_NOT_TOUCH_MODAL`） |
| **`FLAG_LAYOUT_INSET_DECOR`** | `0x00010000` | 窗口内容避开系统 UI（如状态栏、导航栏） |
| **`FLAG_LAYOUT_IN_OVERSCAN`** | `0x00080000` | 允许窗口绘制到屏幕的过扫描区域（如某些游戏全屏模式） |
| **`FLAG_DIM_BEHIND`** | `0x00000002` | 使窗口后面的内容变暗（类似对话框效果） |
| **`FLAG_BLUR_BEHIND`** | `0x00000004` | 使窗口后面的内容模糊（部分设备支持） |
| **`FLAG_KEEP_SCREEN_ON`** | `0x00000080` | 保持屏幕常亮 |
| **`FLAG_SHOW_WHEN_LOCKED`** | `0x00080000` | 在锁屏界面上显示窗口（如来电界面） |
| **`FLAG_IGNORE_CHEEK_PRESSES`** | `0x00008000` | 忽略脸颊误触（大屏设备优化） |
| **`FLAG_TURN_SCREEN_ON`** | `0x00200000` | 窗口显示时自动点亮屏幕 |
| **`FLAG_DISMISS_KEYGUARD`** | `0x00400000` | 窗口显示时自动解锁键盘锁屏 |
| **`FLAG_SPLIT_TOUCH`** | `0x00800000` | 允许窗口接收多点触控事件 |
| **`FLAG_HARDWARE_ACCELERATED`** | `0x01000000` | 启用硬件加速渲染 |
| **`FLAG_LOCAL_FOCUS_MODE`** | `0x10000000` | 窗口仅接收本地焦点事件（不传递到 IME） |

##### **2. 影响窗口行为的标志位**
| 标志位 | 说明 |
|--------|------|
| **`FLAG_FULLSCREEN`** | 全屏模式，隐藏状态栏 |
| **`FLAG_FORCE_NOT_FULLSCREEN`** | 强制不全屏，即使请求全屏也无效 |
| **`FLAG_SECURE`** | 防止窗口内容被截图或录屏 |
| **`FLAG_SCALED`** | 窗口支持缩放 |
| **`FLAG_COMPATIBLE_WINDOW`** | 兼容模式窗口 |
| **`FLAG_SYSTEM_ERROR`** | 系统错误窗口（如 ANR 对话框） |

##### **3. 窗口层级调整相关标志位**
| 标志位 | 说明 |
|--------|------|
| **`FLAG_LAYER_ALWAYS_ON_TOP`** | 强制窗口保持在最顶层（即使 `type` 较低） |
| **`FLAG_LAYER_MULTIPLIED`** | 窗口层级乘以一个系数（用于特殊效果） |
| **`FLAG_LAYER_TRANSLUCENT`** | 窗口半透明，可能影响 Z-order |
