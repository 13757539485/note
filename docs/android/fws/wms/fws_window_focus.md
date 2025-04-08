adb shell dumpsys window的实现在RootWindowContainer

调用DisplayContent.dump，主要看mCurrentFocus和mFocusedApp

mCurrentFocus赋值的地方为
```java
ProtoLog.d(WM_DEBUG_FOCUS_LIGHT, "Changing focus from %s to %s displayId=%d Callers=%s",
        mCurrentFocus, newFocus, getDisplayId(), Debug.getCallers(4));
final WindowState oldFocus = mCurrentFocus;
mCurrentFocus = newFocus;
```
mFocusedApp赋值的地方为
```java
ProtoLog.i(WM_DEBUG_FOCUS_LIGHT, "setFocusedApp %s displayId=%d Callers=%s", newFocus, getDisplayId(), Debug.getCallers(4));
final Task oldTask = mFocusedApp != null ? mFocusedApp.getTask() : null;
final Task newTask = newFocus != null ? newFocus.getTask() : null;
mFocusedApp = newFocus;
```
可以打开调用栈WM_DEBUG_FOCUS_LIGHT

adb shell wm logging enable-text WM_DEBUG_FOCUS_LIGHT

adb logcat -s WindowManager

[ANR分析](../../performance/android_anr.md#focus_anr_away)