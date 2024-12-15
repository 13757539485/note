#### aosp15无法开启自由窗口

设置-开发者选项-启用可自由调整的窗口，开启(对应下面DEVELOPMENT_ENABLE_FREEFORM_WINDOWS_SUPPORT)

packages/apps/Launcher3/quickstep/src/com/android/quickstep/TaskShortcutFactory.java
```java
private boolean isAvailable(RecentsViewContainer container) {
    return Settings.Global.getInt(
            container.asContext().getContentResolver(),
            Settings.Global.DEVELOPMENT_ENABLE_FREEFORM_WINDOWS_SUPPORT, 0) != 0
            && !enableDesktopWindowingMode();
}
```
将!enableDesktopWindowingMode()改成enableDesktopWindowingMode()