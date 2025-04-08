## 悬浮窗窗口显示
### <a id= "window_add">Window的add过程</a>
WindowManager: addView

WindowManagerImpl: addView

WindowManagerGlobal: addView

创建ViewRootImpl
```java
IWindowSession windowlessSession = null;
//...
if (windowlessSession == null) {
    root = new ViewRootImpl(view.getContext(), display);
} else {
    root = new ViewRootImpl(view.getContext(), display,
            windowlessSession, new WindowlessWindowLayout());
}
//...
root.setView(view, wparams, panelParentView, userId);
```
主要看setView方法
```java
//...
requestLayout();// 检查主线程更新UI
InputChannel inputChannel = null;
if ((mWindowAttributes.inputFeatures
        & WindowManager.LayoutParams.INPUT_FEATURE_NO_INPUT_CHANNEL) == 0) {
    inputChannel = new InputChannel();
}
//...
res = mWindowSession.addToDisplayAsUser(mWindow, mWindowAttributes,
    getHostVisibility(), mDisplay.getDisplayId(), userId,
    mInsetsController.getRequestedVisibleTypes(), inputChannel, mTempInsets,
    mTempControls, attachedFrame, compatScale);
```
mWindowSession用于wms通信，此处是ViewRootImpl被new的时候传入的
```java
public ViewRootImpl(Context context, Display display) {
    this(context, display, WindowManagerGlobal.getWindowSession(), new WindowLayout());
}
```
Server端是Session，最终调用wms的addWindow
```java
public int addToDisplayAsUser(IWindow window, WindowManager.LayoutParams attrs,
        int viewVisibility, int displayId, int userId, @InsetsType int requestedVisibleTypes,
        InputChannel outInputChannel, InsetsState outInsetsState,
        InsetsSourceControl.Array outActiveControls, Rect outAttachedFrame,
        float[] outSizeCompatScale) {
    return mService.addWindow(this, window, attrs, viewVisibility, displayId, userId,
            requestedVisibleTypes, outInputChannel, outInsetsState, outActiveControls,
            outAttachedFrame, outSizeCompatScale);
}
```
WindowManagerService：addWindow
```java
//...
final DisplayContent displayContent = getDisplayContentOrCreate(displayId, attrs.token);
if (type >= FIRST_SUB_WINDOW && type <= LAST_SUB_WINDOW) {//子窗口处理
    parentWindow = windowForClientLocked(null, attrs.token, false);
    if (parentWindow == null) {
        return WindowManagerGlobal.ADD_BAD_SUBWINDOW_TOKEN;
    }
    if (parentWindow.mAttrs.type >= FIRST_SUB_WINDOW
            && parentWindow.mAttrs.type <= LAST_SUB_WINDOW) {
        return WindowManagerGlobal.ADD_BAD_SUBWINDOW_TOKEN;
    }
}
//...
ActivityRecord activity = null;
final boolean hasParent = parentWindow != null;
WindowToken token = displayContent.getWindowToken(
        hasParent ? parentWindow.mAttrs.token : attrs.token);
final int rootType = hasParent ? parentWindow.mAttrs.type : type;
if (token == null) {
    if (hasParent) {
        //...
    } else if (mWindowContextListenerController.hasListene(windowContextToken)){
        //...
    } else {
        // 悬浮窗走这里
        final IBinder binder = attrs.token != null ? attrs.token : client.asBinder();
        token = new WindowToken.Builder(this, binder, type)
            .setDisplayContent(displayContent)
            .setOwnerCanManageAppTokens(session.mCanAddInternalSystemWindow)
            .setRoundedCornerOverlay(isRoundedCornerOverlay)
            .build();
    }
} 
```
WindowToken中
```java
if (dc != null) {
    dc.addWindowToken(token, this);
}
```
WindowToken被创建用于挂载WindowState
```java
final WindowState win = new WindowState(this, session, client, token, parentWindow,appOp[0], attrs, viewVisibility, session.mUid, userId,
session.mCanAddInternalSystemWindow);
//...
win.mToken.addWindow(win);//挂载到WindowToken上
```
悬浮窗的mToken一般不会设置就是client.asBinder()
```java
void addWindow(final WindowState win) {
    //...
    if (mSurfaceControl == null) {
        createSurfaceControl(true /* force */);
        reassignLayer(getSyncTransaction());
    }
    if (!mChildren.contains(win)) {
        addChild(win, mWindowComparator);
        //...
    }
}
```
WindowContainer：createSurfaceControl->setInitialSurfaceControlProperties
```java
void setInitialSurfaceControlProperties(Builder b) {
    setSurfaceControl(b.setCallsite("WindowContainer.setInitialSurfaceControlProperties").build());//mSurfaceControl被赋值
    if (showSurfaceOnCreation()) {
        getSyncTransaction().show(mSurfaceControl);
    }
    updateSurfacePositionNonOrganized();
    if (mLastMagnificationSpec != null) {
        applyMagnificationSpec(getSyncTransaction(), mLastMagnificationSpec);
    }
}
```
至此数据结构树添加完成，内容还没显示
### <a id= "window_layout">Window的relayout过程</a>
Window绘制的5种状态(mDrawState)，在WindowStateAnimator中

1.NO_SURFACE(0)

Window 尚未创建或没有可用的 Surface

触发时机:当Window被创建但尚未分配Surface，或被销毁（例如窗口被移除或系统资源不足）

作用：标记窗口尚未准备好进行绘制

2.DRAW_PENDING(1)

Window的绘制请求已经提交，但尚未开始绘制

触发时机:ViewRootImpl接收到绘制请求（例如通过invalidate()或requestLayout()）时，createSurface完成

作用：标记绘制请求已发出，但绘制操作尚未执行

3.COMMIT_DRAW_PENDING(2)

绘制内容已经提交，但尚未完成绘制

触发时机:ViewRootImpl的绘制流程中，当 performTraversals()方法被调用并且绘制任务被提交到绘制线程时，调用wms的finishDrawingWindow被设置

作用：标记绘制任务已经提交，正在等待绘制线程完成

4.READY_TO_SHOW(3)

Window 的绘制已经完成，并且内容已经准备好显示

触发时机:ViewRootImpl完成draw()方法的执行，并且绘制结果已经提交到Surface时，但尚未真正显示在屏幕上（例如需要等待 V-Sync 信号）

作用：标记绘制已完成，内容已经准备好显示

5.HAS_DRAW(4)：已经绘制到SurfaceFlinger

Window至少已经成功绘制过一次

触发时机：Window 的绘制完成并且至少成功显示过一次后。

​作用：标记窗口已经成功绘制，后续的绘制可能会基于此状态进行优化

定位问题时处于0，1说明应用端还没准备好，处于2，3，4说明wms端绘制问题

ViewRootImpl：requestLayout->scheduleTraversals->doTraversal->performTraversals->relayoutWindow

WindowSession: relayout

Session: relayout

wms: relayoutWindow

[SurfaceControl流程](./surfaceflinger.md#surface_create)

SurfaceControl被挂载完成，区域大小限制计算完成，返回给客户端

### Window的finishDraw过程
ViewRootImpl：performTraversals->createSyncIfNeeded->reportDrawFinished

Session: finishDrawing

wms: finishDrawingWindow

WindowState: finishDrawing

WindowStateAnimator: finishDrawingLocked
```java
//...
if (mDrawState == DRAW_PENDING) {
    //...
    mDrawState = COMMIT_DRAW_PENDING;
    layoutNeeded = true;
}
```
wms：继续走mWindowPlacerLocked.requestTraversal()

WindowSurfacePlacer：mPerformSurfacePlacement->performSurfacePlacement->performSurfacePlacementLoop

RootWindowContainer: performSurfacePlacement->performSurfacePlacementNoTrace->applySurfaceChangesTransaction

DisplayContent: applySurfaceChangesTransaction->forAllWindows->mApplySurfaceChangesTransaction

在relayout中创建完WindowSurfaceController后会执行WindowState的setHasSurface(true)，继续mApplySurfaceChangesTransaction回调中
```java
if (w.mHasSurface) {
    final boolean committed = w.mWinAnimator.commitFinishDrawingLocked();
    //...
}
```
WindowStateAnimator
```java
boolean commitFinishDrawingLocked() {
    //...
    mDrawState = READY_TO_SHOW;
    boolean result = false;
    final ActivityRecord activity = mWin.mActivityRecord;
    if (activity == null || activity.canShowWindows()
            || mWin.mAttrs.type == TYPE_APPLICATION_STARTING) {
        result = mWin.performShowLocked();
    }
    return result;
}
```
由于是悬浮窗所以activity为null

WindowState: performShowLocked
```java
boolean performShowLocked() {
    //...
    final int drawState = mWinAnimator.mDrawState;
    if ((drawState == HAS_DRAWN || drawState == READY_TO_SHOW) && mActivityRecord != null) {
        if (mAttrs.type != TYPE_APPLICATION_STARTING) {
            mActivityRecord.onFirstWindowDrawn(this);
        } else {
            mActivityRecord.onStartingWindowDrawn();
        }
    }
    //...
    mWmService.enableScreenIfNeededLocked();
    mWinAnimator.applyEnterAnimationLocked();
    //...
    mWinAnimator.mDrawState = HAS_DRAWN;
    mWmService.scheduleAnimationLocked();
    //...
    return true;
}
```
目前状态已经变成绘制，待提交到SurfaceFlinger显示到屏幕

DisplayContent：prepareSurfaces

DisplayArea: prepareSurfaces

WindowContainer: prepareSurfaces

WindowState: prepareSurfaces

WindowStateAnimator: prepareSurfaceLocked
```java
//...
if (!w.isOnScreen()) {
//...
} else if (mLastAlpha != mShownAlpha || mLastHidden) {
    boolean prepared =
                mSurfaceController.prepareToShowInTransaction(t, mShownAlpha);

    if (prepared && mDrawState == HAS_DRAWN) {
        if (mLastHidden) {
            mSurfaceController.showRobustly(t);
            mLastHidden = false;
            //...
        }
    }
}
```
其中mLastHidden是在WindowSurfaceController创建之后设置成true的

WindowSurfaceController：showRobustly
```java
void showRobustly(SurfaceControl.Transaction t) {
    //...
    t.show(mSurfaceControl);
    //...
}
```