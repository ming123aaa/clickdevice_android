# ClickDevice Android Automation Click Tool

## Project Introduction
This is an automation click tool based on the Android Accessibility Service, with all UI built using **Jetpack Compose**. It implements the following features:
- Screen coordinate automated clicking
- Gesture trajectory simulation
- Script recording and playback
- Script group management
- Floating window control
- Key binding with floating window shortcut buttons
- Desktop shortcut creation

## Key Features

### 1. Automation Clicking
- Precise coordinate clicking
- Gesture trajectory simulation (swipe operations)
- Random area clicking
- Click duration and delay control

### 2. Script Management
- **Regular Scripts**: Manually add click/gesture/delay/loop commands with drag-to-reorder support
- **Record Scripts**: Record screen operations to automatically generate scripts, with playback support
- **Custom Scripts (Script Groups)**: Combine multiple scripts into custom action flows with key-value mapping

### 3. Key Binding System
- Up to 5 floating window shortcut buttons
- Each button can bind a script, tap to run/stop
- Customizable button font color and size
- Lockable floating window position with auto-restore on next launch
- Configurable script run parameters (interval, count, speed, coordinate scaling, etc.)
- Persistent floating window display state, auto-restore after restart

### 4. Floating Window Control
- SmallWindowView supports free dragging
- SmallWindowsHelper manages floating window lifecycle
- Floating window position lock/unlock support

### 5. Desktop Shortcuts
- Create desktop shortcuts for scripts, recorded scripts, and custom scripts
- Create desktop shortcut for the key binding settings page

## Tech Stack
- **UI Framework**: Jetpack Compose + Material3
- **Database**: Room (SQLite)
- **Architecture**: Compose State + ViewModel
- **Language**: Kotlin / Java mixed
- **Min SDK**: Android API 21

## Core Components

| Component | Description |
|-----------|-------------|
| `MyService` | Core accessibility service, handles click and gesture events |
| `ScriptExecutor` | Script execution engine |
| `RecordScriptExecutor` | Recorded script executor |
| `AppDatabase` | Room-based local database for scripts and configurations |
| `SmallWindowView` | Floating window view component |
| `KeyFloatWindowManager` | Key floating window manager with multi-window and script binding support |
| `DesktopIconHelper` | Desktop shortcut creation utility |
| `SmallWindowsHelper` | Floating window helper manager |

## Usage Instructions

### Launching the App
1. Accessibility permission needs to be granted on first use
2. Floating window permission is required
3. ADB authorization is also supported: `adb shell pm grant <package_name> android.permission.WRITE_SECURE_SETTINGS`

### Creating Scripts
1. **Regular Scripts**: Manually add click/gesture/delay/loop/random-click commands with drag-to-reorder and JSON import support
2. **Record Scripts**: Record screen operations via floating window to automatically generate scripts
3. **Custom Scripts**: Combine multiple scripts into custom action flows

### Executing Scripts
- Select a saved script and tap to execute
- Supports loop execution and speed adjustment
- Supports coordinate scaling (0.25x ~ 5x)
- Auto-stop on app switch detection

### Key Binding
1. Go to the "Key Settings" page
2. Add a key, set name, color, and font size
3. Select script type and bind a script
4. Configure run parameters
5. Enable floating window, tap the button to run/stop the script
6. Support locking the floating window position

### Desktop Shortcuts
- Tap the desktop icon button on the list page to create desktop shortcuts for scripts or key settings

## Project Structure
```
com.example.clickdevice/
├── activity/           # Compose UI screens
│   ├── MainActivityCompose           # Main screen
│   ├── ScriptActivityCompose         # Regular script runner
│   ├── ScriptEditActivityCompose     # Script editor
│   ├── ScriptListActivityCompose     # Script list
│   ├── RecordScriptActivityCompose   # Record script
│   ├── RecordScriptPlayActivityCompose # Record script playback
│   ├── ScriptGroupEditActivityCompose  # Custom script editor
│   ├── ScriptGroupPlayActivityCompose  # Custom script playback
│   ├── KeyBindingListActivity        # Key binding list
│   ├── KeyBindingEditActivity        # Key binding editor
│   ├── LauncherScriptActivityCompose # Desktop shortcut launcher
│   └── LauncherActivityCompose       # Splash screen
├── bean/               # Data models
│   ├── ScriptCmdBean                 # Script commands
│   ├── ScriptRunParams               # Run parameters
│   ├── ScriptGroupBean               # Script group
│   └── ScriptExtensions              # Extension methods
├── db/                 # Room database
│   ├── AppDatabase                   # Database definition
│   ├── ScriptDataBean / ScriptDao    # Script table
│   ├── RecordScriptBean / RecordScriptDao # Record script table
│   ├── ScriptGroupBean / ScriptGroupDao   # Script group table
│   └── KeyBindingBean / KeyBindingDao     # Key binding table
├── helper/             # Utility classes
│   ├── KeyFloatWindowManager         # Key floating window manager
│   ├── SmallWindowsHelper            # Floating window helper
│   └── DesktopIconHelper             # Desktop shortcut utility
├── ui/theme/           # Compose theme
├── view/               # Custom views
└── vm/                 # ViewModel
```
