# ClickDevice Android 自动化点击工具

## 项目简介
这是一个基于 Android 无障碍服务的自动化点击工具，使用 **Jetpack Compose** 构建全部 UI 界面。主要实现以下功能：
- 屏幕坐标点击自动化
- 手势轨迹模拟
- 脚本录制与回放
- 脚本分组管理
- 悬浮窗控制
- 按键绑定与悬浮窗快捷按钮
- 桌面快捷方式创建

## 主要功能特性

### 1. 自动化点击
- 支持精确坐标点击
- 支持手势轨迹模拟（滑屏操作）
- 支持随机区域点击
- 支持点击时长和延时控制

### 2. 脚本管理
- **普通脚本**：手动添加点击/手势/延迟/循环等命令，支持拖拽排序
- **录制脚本**：录制屏幕操作自动生成脚本，支持回放
- **自定义脚本（脚本组）**：将多个脚本组合为自定义动作，支持键值映射

### 3. 按键绑定系统
- 最多支持 5 个悬浮窗快捷按钮
- 每个按钮可绑定一个脚本，点击即可运行/停止
- 支持自定义按钮字体颜色和大小
- 支持锁定悬浮窗位置，下次自动恢复
- 支持设置脚本运行参数（间隔、次数、速度、坐标系数等）
- 持久化悬浮窗显示状态，重启后自动恢复

### 4. 悬浮窗控制
- SmallWindowView 支持自由拖拽
- SmallWindowsHelper 管理悬浮窗生命周期
- 支持悬浮窗位置锁定/解锁

### 5. 桌面快捷方式
- 支持为脚本、录制脚本、自定义脚本创建桌面快捷方式
- 支持为按键设置页面创建桌面快捷方式

## 技术栈
- **UI 框架**：Jetpack Compose + Material3
- **数据库**：Room（SQLite）
- **架构**：Compose State + ViewModel
- **语言**：Kotlin / Java 混合
- **最低 SDK**：Android API 21

## 核心组件

| 组件 | 说明 |
|------|------|
| `MyService` | 核心无障碍服务，处理点击和手势事件 |
| `ScriptExecutor` | 脚本执行引擎 |
| `RecordScriptExecutor` | 录制脚本执行器 |
| `AppDatabase` | 基于 Room 的本地数据库，存储脚本和配置 |
| `SmallWindowView` | 悬浮窗视图组件 |
| `KeyFloatWindowManager` | 按键悬浮窗管理器，支持多窗口和脚本绑定 |
| `DesktopIconHelper` | 桌面快捷方式创建工具 |
| `SmallWindowsHelper` | 悬浮窗辅助管理类 |

## 使用说明

### 启动应用
1. 首次使用需要授予无障碍权限
2. 需要悬浮窗权限支持
3. 也可通过 ADB 命令授权：`adb shell pm grant <包名> android.permission.WRITE_SECURE_SETTINGS`

### 创建脚本
1. **普通脚本**：手动添加点击/手势/延迟/循环/随机点击等命令，支持拖拽排序和 JSON 导入
2. **录制脚本**：通过悬浮窗录制屏幕操作自动生成脚本
3. **自定义脚本**：将多个脚本组合为自定义动作流程

### 执行脚本
- 选择保存的脚本点击执行
- 支持循环执行和速度调节
- 支持坐标系数缩放（0.25x ~ 5x）
- 支持检测应用切换时自动停止

### 按键绑定
1. 进入「按键设置」页面
2. 添加按键，设置名称、颜色、字体大小
3. 选择脚本类型并绑定脚本
4. 设置运行参数
5. 开启悬浮窗，点击按钮即可运行/停止脚本
6. 支持锁定悬浮窗位置

### 桌面快捷方式
- 在列表页面点击桌面图标按钮，可为脚本或按键设置创建桌面快捷方式

## 项目结构
```
com.example.clickdevice/
├── activity/           # Compose 界面组件
│   ├── MainActivityCompose           # 主界面
│   ├── ScriptActivityCompose         # 普通脚本运行
│   ├── ScriptEditActivityCompose     # 脚本编辑器
│   ├── ScriptListActivityCompose     # 脚本列表
│   ├── RecordScriptActivityCompose   # 录制脚本
│   ├── RecordScriptPlayActivityCompose # 录制脚本播放
│   ├── ScriptGroupEditActivityCompose  # 自定义脚本编辑
│   ├── ScriptGroupPlayActivityCompose  # 自定义脚本播放
│   ├── KeyBindingListActivity        # 按键列表
│   ├── KeyBindingEditActivity        # 按键编辑
│   ├── LauncherScriptActivityCompose # 桌面快捷方式启动器
│   └── LauncherActivityCompose       # 启动页
├── bean/               # 数据模型
│   ├── ScriptCmdBean                 # 脚本命令
│   ├── ScriptRunParams               # 运行参数
│   ├── ScriptGroupBean               # 脚本组
│   └── ScriptExtensions              # 扩展方法
├── db/                 # Room 数据库
│   ├── AppDatabase                   # 数据库定义
│   ├── ScriptDataBean / ScriptDao    # 脚本表
│   ├── RecordScriptBean / RecordScriptDao # 录制脚本表
│   ├── ScriptGroupBean / ScriptGroupDao   # 脚本组表
│   └── KeyBindingBean / KeyBindingDao     # 按键绑定表
├── helper/             # 工具类
│   ├── KeyFloatWindowManager         # 按键悬浮窗管理
│   ├── SmallWindowsHelper            # 悬浮窗辅助
│   └── DesktopIconHelper             # 桌面快捷方式
├── ui/theme/           # Compose 主题
├── view/               # 自定义 View
└── vm/                 # ViewModel
```
