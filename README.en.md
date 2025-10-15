# ClickDevice Android Automation Click Tool

## Project Introduction
This is an automation click tool based on the Android Accessibility Service, mainly implementing the following functions:
- Screen coordinate automated clicking
- Gesture trajectory simulation
- Script recording and playback
- Script group management
- Floating window control
- Desktop shortcut creation

## Key Features
1. **Automation Clicking**
   - Supports precise coordinate clicking
   - Supports gesture trajectory simulation
   - Supports random area clicking

2. **Script Management**
   - Supports script recording and playback
   - Supports script group management
   - Supports complex operations such as delays and loops


## Core Components
- **MyService**: Core accessibility service for handling clicks and gestures
- **ScriptExecutor**: Script execution engine
- **RecordScriptExecutor**: Recorded script executor
- **AppDatabase**: Local database based on Room for storing scripts and configurations
- **SmallWindowView**: Floating window view component
- **DesktopIconHelper**: Desktop shortcut creation tool

## Usage Instructions
1. **Launching the App**
   - Accessibility permission needs to be granted on first use
   - Requires floating window permission support

2. **Creating Scripts**
   - Add clicks/gestures/delays manually
   - Supports recording screen operations to generate scripts

3. **Executing Scripts**
   - Select a saved script and click to execute
   - Supports loop execution and speed adjustment

4. **Script Management**
   - Supports script group management
   - Supports importing/exporting scripts
   - Supports creating desktop shortcuts

## Developer Guide
1. **Project Structure**
   - `activity/`: Main Activity components
   - `adapter/`: RecyclerView adapters
   - `bean/`: Data model classes
   - `db/`: Database-related classes
   - `helper/`: Utility and helper classes
   - `view/`: Custom view components
   - `vm/`: ViewModel components
