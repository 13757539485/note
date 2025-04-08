### 基本使用
主题中添加，其中avd_anim可以从源码中获取

development/samples/StartingWindow/res/drawable/avd_anim.xml

```xml
<item name="android:windowSplashScreenBackground">#FFFFFFFF</item>
<item name="android:windowSplashScreenAnimatedIcon">@drawable/avd_anim</item>
<item name="android:windowSplashScreenAnimationDuration">1000</item>
<item name="android:windowSplashScreenIconBackgroundColor">#FFFFFFFF</item>
```
### SplashScreen窗口添加
启动相册通过dumpsys window windows查看到Window #10 Window{3041a83 u0 Splash Screen com.android.gallery3d}，搜索Splash Screen关键字查看java源码路径

frameworks/base/libs/WindowManager/Shell/src/com/android/wm/shell/startingsurface/SplashscreenContentDrawer.java

属于SystemUI进程，调用链如下

TaskOrganizer：addStartingWindow

ShellTaskOrganizer: addStartingWindow

StartingWindowController: addStartingWindow

StartingSurfaceDrawer: addSplashScreenStartingWindow

SplashscreenWindowCreator: addSplashScreenStartingWindow

SplashscreenContentDrawer: createLayoutParameters

通过源码下搜索：jgrep "\\.addStartingWindow(" ./ -rn

TaskOrganizerController: addStartingWindow

属于SystemServer进程，通过断点调用链如下

ams：startActivity->startActivityAsUser

ActivityStarter：execute->executeRequest->startActivityUnchecked->startActivityInner

Task：startActivityLocked

StartingSurfaceController：showStartingWindow

ActivityRecord：showStartingWindow->addStartingWindow->scheduleAddStartingWindow
```java
boolean addStartingWindow(String pkg, int resolvedTheme, ActivityRecord from, boolean newTask,boolean taskSwitch, boolean processRunning, boolean allowTaskSnapshot,
        boolean activityCreated, boolean isSimple,
        boolean activityAllDrawn) {
    //...
    mStartingData = new SplashScreenStartingData(mWmService, resolvedTheme, typeParameter);
    scheduleAddStartingWindow();
    return true;
}
```
ActivityRecord：scheduleAddStartingWindow

SplashScreenStartingData：createStartingSurface

StartingSurfaceController：createSplashScreenStartingSurface

TaskOrganizerController：addStartingWindow
```java
boolean addStartingWindow(Task task, ActivityRecord activity, int launchTheme,
        TaskSnapshot taskSnapshot) {
    final Task rootTask = task.getRootTask();
    //...
    final StartingWindowInfo info = task.getStartingWindowInfo(activity);
    //...
    info.taskSnapshot = taskSnapshot;
    info.appToken = activity.token;//token传递过去
    //...
    lastOrganizer.addStartingWindow(info);
    //...
    return true;
}
```
总体流程就是启动Activity流程中会跨进程调用到SystemUI进程的TaskOrganizer，创建好window后

SplashscreenContentDrawer: createLayoutParameters->createContentView->makeSplashScreenContentView->getWindowAttrs->SplashViewBuilder.build->fillViewWithIcon->SplashScreenView.Builder
```java
static WindowManager.LayoutParams createLayoutParameters(Context context,
        StartingWindowInfo windowInfo,
        @StartingWindowInfo.StartingWindowType int suggestType,
        CharSequence title, int pixelFormat, IBinder appToken) {
    final WindowManager.LayoutParams params = new WindowManager.LayoutParams(
        WindowManager.LayoutParams.TYPE_APPLICATION_STARTING);
    //...
    params.token = appToken;//就是之前传递的token即ActivityRecord的
    //...
    return params;
}
```
makeSplashScreenContentView中主要是创建SplashScreenView

getWindowAttrs中主要是解析app端主题设置的闪屏页相关属性
```java
context = context.createPackageContextAsUser(activityInfo.packageName,
    CONTEXT_RESTRICTED, UserHandle.of(taskInfo.userId));//创建应用的Context

private static void getWindowAttrs(Context context, SplashScreenWindowAttrs attrs) {
    final TypedArray typedArray = context.obtainStyledAttributes(
            com.android.internal.R.styleable.Window);
    attrs.mWindowBgResId = typedArray.getResourceId(R.styleable.Window_windowBackground, 0);
    attrs.mWindowBgColor = safeReturnAttrDefault((def) -> typedArray.getColor(
            R.styleable.Window_windowSplashScreenBackground, def),
            Color.TRANSPARENT);
    attrs.mSplashScreenIcon = safeReturnAttrDefault((def) -> typedArray.getDrawable(
            R.styleable.Window_windowSplashScreenAnimatedIcon), null);
    attrs.mBrandingImage = safeReturnAttrDefault((def) -> typedArray.getDrawable(
            R.styleable.Window_windowSplashScreenBrandingImage), null);
    attrs.mIconBgColor = safeReturnAttrDefault((def) -> typedArray.getColor(
            R.styleable.Window_windowSplashScreenIconBackgroundColor, def),
            Color.TRANSPARENT);
    typedArray.recycle();
}
```
SplashscreenWindowCreator：addWindow

WindowManagerGlobal：addView，走[悬浮窗显示流程](./fws_window_add.md)

wms: addWindow中
```java
//...
WindowState parentWindow = null;
if (type >= FIRST_SUB_WINDOW && type <= LAST_SUB_WINDOW) {
    parentWindow = windowForClientLocked(null, attrs.token, false);
}
//...
WindowToken token = displayContent.getWindowToken(
    hasParent ? parentWindow.mAttrs.token : attrs.token);//hasParent为null
//...
else if (rootType >= FIRST_APPLICATION_WINDOW
        && rootType <= LAST_APPLICATION_WINDOW) {
    activity = token.asActivityRecord();//不为null
    //...
    else if (type == TYPE_APPLICATION_STARTING) {
        if (activity.mStartingWindow != null) {//mStartingWindow此时为null
            return WindowManagerGlobal.ADD_DUPLICATE_ADD;
        }
        if (activity.mStartingData == null) {//上面已经被创建不为null
            return WindowManagerGlobal.ADD_DUPLICATE_ADD;
        }
    }
}
//...
win.mToken.addWindow(win);//WindowState被挂载
if (type == TYPE_APPLICATION_STARTING && activity != null) {
    activity.attachStartingWindow(win);//调用ActivityRecord的attachStartingWindow
}
```
ActivityRecord：attachStartingWindow
```java
void attachStartingWindow(@NonNull WindowState startingWindow) {
    startingWindow.mStartingData = mStartingData;
    mStartingWindow = startingWindow;//被赋值
    //...
}
```
后续继续悬浮窗的relayout等流程

### SplashScreen窗口移除
**SystemServer进程**

WindowStateAnimator：commitFinishDrawingLocked

WindowState：performShowLocked

ActivityRecord：onFirstWindowDrawn->removeStartingWindow->removeStartingWindowAnimation

StartingSurfaceController->remove

TaskOrganizerController->removeStartingWindow

**SystemUI进程**

TaskOrganizer：removeStartingWindow

ShellTaskOrganizer: removeStartingWindow

StartingSurfaceDrawer：removeStartingWindow

StartingWindowRecordManager：removeWindow

StartingWindowRecord：removeIfPossible

SplashscreenWindowCreator：removeIfPossible->removeWindowInner

WindowManagerGlobal: removeView

#### app端设置自定义动画
app端调用SplashScreen的setOnExitAnimationListener

**应用进程**

ActivityRecord：onFirstWindowDrawn->removeStartingWindow
```java
void removeStartingWindow() {
    //...
    if (transferSplashScreenIfNeeded()) {
        return;
    }
    removeStartingWindowAnimation(true /* prepareAnimation */);
    //...
}
private boolean transferSplashScreenIfNeeded() {
    if (finishing || !mHandleExitSplashScreen || mStartingSurface == null
            || mStartingWindow == null
            || mTransferringSplashScreenState == TRANSFER_SPLASH_SCREEN_FINISH
            // skip copy splash screen to client if it was resized
            || (mStartingData != null && mStartingData.mResizedFromTransfer)) {
        return false;
    }
    //...
    requestCopySplashScreen();
    return isTransferringSplashScreen();
}
```
和没有设置自定义动画的区别是此时mHandleExitSplashScreen为true，原因如下

SplashScreen:setOnExitAnimationListener

SplashScreen.SplashScreenManagerGlobal：addImpl

app启动流程走resume(真正开始绘制的生命周期)时

ResumeActivityItem: postExecute->isHandleSplashScreenExit

ActivityThread: isHandleSplashScreenExit
```java
public boolean isHandleSplashScreenExit(@NonNull IBinder token) {
    synchronized (this) {
        return mSplashScreenGlobal != null && mSplashScreenGlobal.containsExitListener(token);
    }
}
public boolean containsExitListener(IBinder token) {
    synchronized (mGlobalLock) {
        final SplashScreenImpl impl = findImpl(token);
        return impl != null && impl.mExitAnimationListener != null;//mExitAnimationListener不为null
    }
}
```
ActivityClientController: activityResumed(isHandleSplashScreenExit)，传入为true

ActivityRecord: activityResumedLocked->setCustomizeSplashScreenExitAnimation(isHandleSplashScreenExit)赋值给mHandleExitSplashScreen

所以继续走requestCopySplashScreen
```java
private void requestCopySplashScreen() {
    mTransferringSplashScreenState = TRANSFER_SPLASH_SCREEN_COPYING;
    if (mStartingSurface == null || !mAtmService.mTaskOrganizerController.copySplashScreenView(
            getTask(), mStartingSurface.mTaskOrganizer)) {
        mTransferringSplashScreenState = TRANSFER_SPLASH_SCREEN_FINISH;
        removeStartingWindow();
    }
    scheduleTransferSplashScreenTimeout();
}
```
TaskOrganizerController：copySplashScreenView

**SystemUI进程**

TaskOrganizer：copySplashScreenView

ShellTaskOrganizer: copySplashScreenView

StartingWindowController: copySplashScreenView

StartingSurfaceDrawer: copySplashScreenView

SplashscreenWindowCreator: copySplashScreenView
```java
public void copySplashScreenView(int taskId) {
    final StartingSurfaceDrawer.StartingWindowRecord record =
            mStartingWindowRecordManager.getRecord(taskId);
    final SplashWindowRecord preView = record instanceof SplashWindowRecord
            ? (SplashWindowRecord) record : null;
    SplashScreenView.SplashScreenViewParcelable parcelable;
    SplashScreenView splashScreenView = preView != null ? preView.mSplashView : null;
    if (splashScreenView != null && splashScreenView.isCopyable()) {
        parcelable = new SplashScreenView.SplashScreenViewParcelable(splashScreenView);
        parcelable.setClientCallback(
                new RemoteCallback((bundle) -> mSplashScreenExecutor.execute(
                        () -> onAppSplashScreenViewRemoved(taskId, false))));
        splashScreenView.onCopied();
        mAnimatedSplashScreenSurfaceHosts.append(taskId, splashScreenView.getSurfaceHost());
    } else {
        parcelable = null;
    }
    ProtoLog.v(ShellProtoLogGroup.WM_SHELL_STARTING_WINDOW,
            "Copying splash screen window view for task: %d with parcelable %b",
            taskId, parcelable != null);
    ActivityTaskManager.getInstance().onSplashScreenViewCopyFinished(taskId, parcelable);
}
```
atms: onSplashScreenViewCopyFinished

**应用进程**

ActivityRecord: onCopySplashScreenFinish

ActivityThread: handleAttachSplashScreenView->createSplashScreen

其中handleAttachSplashScreenView中
```java
public void handleAttachSplashScreenView(@NonNull ActivityClientRecord r,
        @Nullable SplashScreenView.SplashScreenViewParcelable parcelable,
        @NonNull SurfaceControl startingWindowLeash) {
    final DecorView decorView = (DecorView) r.window.peekDecorView();
    if (parcelable != null && decorView != null) {
        createSplashScreen(r, decorView, parcelable, startingWindowLeash);
    } else {
        // shouldn't happen!
        Slog.e(TAG, "handleAttachSplashScreenView failed, unable to attach");
    }
}
```
走createSplashScreen
```java
private void createSplashScreen(ActivityClientRecord r, DecorView decorView,
        SplashScreenView.SplashScreenViewParcelable parcelable,
        @NonNull SurfaceControl startingWindowLeash) {
    final SplashScreenView.Builder builder = new SplashScreenView.Builder(r.activity);
    final SplashScreenView view = builder.createFromParcel(parcelable).build();
    view.attachHostWindow(r.window);
    decorView.addView(view);//添加到当前应用的DecorView中
    view.requestLayout();

    view.getViewTreeObserver().addOnPreDrawListener(new ViewTreeObserver.OnPreDrawListener() {
        private boolean mHandled = false;
        @Override
        public boolean onPreDraw() {
            if (mHandled) {
                return true;
            }
            mHandled = true;
            // 保证平稳从窗口形式过渡到view形式
            syncTransferSplashscreenViewTransaction(
                    view, r.token, decorView, startingWindowLeash);
            view.post(() -> view.getViewTreeObserver().removeOnPreDrawListener(this));
            return true;
        }
    });
}
private void syncTransferSplashscreenViewTransaction(SplashScreenView view, IBinder token,
        View decorView, @NonNull SurfaceControl startingWindowLeash) {
    final SurfaceControl.Transaction transaction = new SurfaceControl.Transaction();
    transaction.hide(startingWindowLeash);//隐藏SplashScreen窗口

    decorView.getViewRootImpl().applyTransactionOnDraw(transaction);
    view.syncTransferSurfaceOnDraw();
    decorView.postOnAnimation(() -> reportSplashscreenViewShown(token, view));
}
```
ActivityThread: reportSplashscreenViewShown

ActivityClient： reportSplashScreenAttached

ActivityClientController： splashScreenAttached

ActivityRecord： splashScreenAttachedLocked->onSplashScreenAttachComplete
```java
private void onSplashScreenAttachComplete() {
    removeTransferSplashScreenTimeout();
    // Client has draw the splash screen, so we can remove the starting window.
    if (mStartingWindow != null) {
        mStartingWindow.cancelAnimation();
        mStartingWindow.hide(false, false);
    }
    // no matter what, remove the starting window.
    mTransferringSplashScreenState = TRANSFER_SPLASH_SCREEN_FINISH;
    removeStartingWindowAnimation(false /* prepareAnimation */);
}
```
removeStartingWindowAnimation就是SplashScreen窗口移除中TaskOrganizerController->removeStartingWindow的步骤

总结：SystemUI中创建Splash创建并启动动画，结束后通过binder将SplashScreenView传输到应用进程，应用进程获取到最后一帧来做自定义动画

### 调试
```shell
adb shell dumpsys activity service SystemUIService WMShell protolog enable-text WM_SHELL_STARTING_WINDOW
```

### 实战

1.关闭SplashScreen动画

SplashScreen动画是在Task的startActivityLocked启动的
```java
//...
} else if (SHOW_APP_STARTING_PREVIEW && doShow) {
    Task baseTask = r.getTask();
    final ActivityRecord prev = baseTask.getActivity(
            a -> a.mStartingData != null && a.showToCurrentUser());
    mWmService.mStartingSurfaceController.showStartingWindow(r, prev, newTask,
            isTaskSwitch, sourceRecord);
}
```
SHOW_APP_STARTING_PREVIEW默认为true，改成false即可

也可以在ActivityRecord中的addStartingWindow中加return处理

2.设置默认logo方案

解析getWindowAttrs中
```java
attrs.mSplashScreenIcon = safeReturnAttrDefault((def) -> typedArray.getDrawable(
    R.styleable.Window_windowSplashScreenAnimatedIcon), null);
```
将null改成new ColorDrawable(Color.TRANSPARENT)