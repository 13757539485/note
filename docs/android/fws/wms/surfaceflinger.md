### SurfaceFlinger
命令

```shell
adb shell dumpsys SurfaceFlinger > surface.txt
```
### <a id="surface_create">图层挂载</a>
wms中relayoutWindow中创建并挂载到WindowState上
```java
if (shouldRelayout && outSurfaceControl != null) {
    try {
        result = createSurfaceControl(outSurfaceControl, result, win, winAnimator);
    } catch (Exception e) {
        //...
    }
    mWindowPlacerLocked.performSurfacePlacement(true /* force */);
}

private int createSurfaceControl(SurfaceControl outSurfaceControl, int result,
            WindowState win, WindowStateAnimator winAnimator) {
    //...
    WindowSurfaceController surfaceController;
    try {
        surfaceController = winAnimator.createSurfaceLocked();
    } finally {
        Trace.traceEnd(TRACE_TAG_WINDOW_MANAGER);
    }
    //...
    return result;
}
```
WindowStateAnimator中会设置绘制状态，resetDrawState()设置mDrawState = DRAW_PENDING，然后创建WindowSurfaceController
```java
//...
mSurfaceController = new WindowSurfaceController(attrs.getTitle().toString(), format, flags, this, attrs.type);
w.setHasSurface(true);
//...
mLastHidden = true;
//...
```
接着WindowSurfaceController中会创建SurfaceControl并挂载到WindowState中
```java
mSurfaceControl = win.makeSurface()
    .setParent(win.getSurfaceControl())
    .setName(name)
    .setFormat(format)
    .setFlags(flags)
    .setMetadata(METADATA_WINDOW_TYPE, windowType)
    .setMetadata(METADATA_OWNER_UID, mWindowSession.mUid)
    .setMetadata(METADATA_OWNER_PID, mWindowSession.mPid)
    .setCallsite("WindowSurfaceController")
    .setBLASTLayer().build();
```
WindowSurfacePlacer：performSurfacePlacement->performSurfacePlacementLoop

RootWindowContainer: performSurfacePlacement->performSurfacePlacementNoTrace->applySurfaceChangesTransaction

DisplayContent: applySurfaceChangesTransaction->performLayout->performLayoutNoTrace->forAllWindows从顶部到底部遍历WindowState

mPerformLayout：layoutWindowLw

DisplayPolicy: layoutWindowLw

WindowLayout: computeFrames

WindowState: setFrames