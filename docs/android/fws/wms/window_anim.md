### 动画类型
本地动画：运行在SystemServer进程, 通过LocalAnimationAdapter实现

远端动画：运行在非SystemServer进程，如桌面图标启动动画，通过RemoteAnimationAdapterWrapper实现

动画一般名为leash，动画开始时会将Task挂载到leash节点下，leash挂载到TaskDisplayArea，结束后Task挂回TaskDisplayArea

### 悬浮窗动画(本地动画)
#### 进入动画
设置WindowManager.LayoutParams.windowAnimations后才有

引用styles.xml中的
```xml
<style name="xxx">
    <item name="android:windowEnterAnimation">@anim/xxx_enter</item>
    <item name="android:windowExitAnimation">@anim/xxx_exit</item>
</style>
```
DisplayContent：mApplySurfaceChangesTransaction

WindowStateAnimator：commitFinishDrawingLocked

WindowState：performShowLocked

WindowStateAnimator：applyEnterAnimationLocked->applyAnimationLocked
```java
void applyEnterAnimationLocked() {
    final int transit;
    if (mEnterAnimationPending) {
        mEnterAnimationPending = false;
        transit = WindowManagerPolicy.TRANSIT_ENTER;
    } else {
        transit = WindowManagerPolicy.TRANSIT_SHOW;
    }

    if (mAttrType != TYPE_BASE_APPLICATION && !mIsWallpaper
            && !(mWin.mActivityRecord != null && mWin.mActivityRecord.hasStartingWindow())) {
        applyAnimationLocked(transit, true);
    }
    //...
}
```
applyAnimationLocked中
```java
if (mWin.mToken.okToAnimate()) {
    int anim = mWin.getDisplayContent().getDisplayPolicy().selectAnimation(mWin, transit);//返回DisplayPolicy.ANIMATION_STYLEABLE
    if (anim != DisplayPolicy.ANIMATION_STYLEABLE) {
        //...
    } else {
        switch (transit) {
            case WindowManagerPolicy.TRANSIT_ENTER:
                attr = com.android.internal.R.styleable.WindowAnimation_windowEnterAnimation;
                break;
            case WindowManagerPolicy.TRANSIT_EXIT:
                attr = com.android.internal.R.styleable.WindowAnimation_windowExitAnimation;
                break;
            case WindowManagerPolicy.TRANSIT_SHOW:
                attr = com.android.internal.R.styleable.WindowAnimation_windowShowAnimation;
                break;
            case WindowManagerPolicy.TRANSIT_HIDE:
                attr = com.android.internal.R.styleable.WindowAnimation_windowHideAnimation;
                break;
        }
        if (attr >= 0) {//加载应用xml中配置的动画
            a = mWin.getDisplayContent().mAppTransition.loadAnimationAttr(
                    mWin.mAttrs, attr, TRANSIT_OLD_NONE);
        }
        //...
        mWin.startAnimation(a);
        //...
    }
    //...
}
```
startAnimation中
```java
//...
final AnimationAdapter adapter = new LocalAnimationAdapter(
    new WindowAnimationSpec(anim, position, false,0),
    mWmService.mSurfaceAnimationRunner);
startAnimation(getPendingTransaction(), adapter);
commitPendingTransaction();
```
WindowContainer：startAnimation

SurfaceAnimator：startAnimation
```java
//...
final SurfaceControl surface = mAnimatable.getSurfaceControl();//mAnimatable就是WindowContainer,是WindowState的父类
mLeash = freezer != null ? freezer.takeLeashForAnimation() : null;
if (mLeash == null) {
    mLeash = createAnimationLeash(mAnimatable, surface, t, type,
            mAnimatable.getSurfaceWidth(), mAnimatable.getSurfaceHeight(), 0 /* x */,
            0 /* y */, hidden, mService.mTransactionFactory);
    mAnimatable.onAnimationLeashCreated(t, mLeash);
    //...
    mAnimation.startAnimation(mLeash, t, type, mInnerAnimationFinishedCallback);
    //...
}
```
其中mAnimatable的SurfaceControl是在[addWindow](./fws_window_add.md#window_add)中createSurfaceControl的时候初始化的
```java
static SurfaceControl createAnimationLeash(Animatable animatable, SurfaceControl surface,
        Transaction t, @AnimationType int type, int width, int height, int x, int y,
        boolean hidden, Supplier<Transaction> transactionFactory) {
    ProtoLog.i(WM_DEBUG_ANIM, "Reparenting to leash for %s", animatable);
    final SurfaceControl.Builder builder = animatable.makeAnimationLeash()
            .setParent(animatable.getAnimationLeashParent())//父节点为WindowState的父节点即WindowToken
            .setName(surface + " - animation-leash of " + animationTypeToString(type))
            .setHidden(hidden)
            .setEffectLayer()
            .setCallsite("SurfaceAnimator.createAnimationLeash");
    final SurfaceControl leash = builder.build();
    t.setWindowCrop(leash, width, height);
    t.setPosition(leash, x, y);
    t.show(leash);
    t.setAlpha(leash, hidden ? 0 : 1);

    t.reparent(surface, leash);
    return leash;
}
```
创建leash图层，将surface(WindowState)挂载到leash上, mAnimation是LocalAnimationAdapter

LocalAnimationAdapter：startAnimation

SurfaceAnimationRunner：startAnimation->startAnimationLocked->applyTransformation，SurfaceAnimationRunner是wms初始化创建的

WindowAnimationSpec: apply

注：动画使用了ValueAnimator实现的，addUpdateListener中调用applyTransformation

#### 退出动画
技巧：在SurfaceControl源码remove中添加堆栈打印
```java
if(sc.toString().contains("animation-leash of window_animation")){}
```
ViewRootImpl: dispatchDetachedFromWindow

Session: remove

wms: removeClientToken

WindowState: removeIfPossible

WindowStateAnimator: applyAnimationLocked

之后流程和进入动画一样

#### leash移除流程
WindowContainer初始化时SurfaceAnimator被new出来
```java
mSurfaceAnimator = new SurfaceAnimator(this, this::onAnimationFinished, wms);
```
在SurfaceAnimator启动动画时，传入mInnerAnimationFinishedCallback
```java
mAnimation.startAnimation(mLeash, t, type, mInnerAnimationFinishedCallback);
```
接着调用LocalAnimationAdapter的startAnimation
```java
mAnimator.startAnimation(mSpec, animationLeash, t,
    () -> finishCallback.onAnimationFinished(type, this));
```
其中staticAnimationFinishedCallback
```java
SurfaceAnimator(Animatable animatable,
        @Nullable OnAnimationFinishedCallback staticAnimationFinishedCallback,
        WindowManagerService service) {
    mAnimatable = animatable;
    mService = service;
    mStaticAnimationFinishedCallback = staticAnimationFinishedCallback;
    mInnerAnimationFinishedCallback = getFinishedCallback(staticAnimationFinishedCallback);
}
```
最终在SurfaceAnimationRunner启动动画设置的动画监听中回调onAnimationEnd的时候回调
到staticAnimationFinishedCallback.onAnimationFinished即回调到getFinishedCallback
```java
private OnAnimationFinishedCallback getFinishedCallback(
    @Nullable OnAnimationFinishedCallback staticAnimationFinishedCallback) {
    //...
    reset(mAnimatable.getSyncTransaction(), true);
    //...
}
```
SurfaceAnimator: removeLeash，重新挂载回WindowToken并移除leash图层
```java
//...
t.reparent(surface, parent);
t.remove(leash);
//...
```
### 桌面启动应用动画
共5个动画

adb shell wm logging enable-text WM_DEBUG_REMOTE_ANIMATIONS WM_DEBUG_ANIM WM_DEBUG_APP_TRANSITIONS_ANIM WM_DEBUG_APP_TRANSITIONS WM_DEBUG_STARTING_WINDOW WM_DEBUG_STATES WM_SHOW_SURFACE_ALLOC

### 远程动画
AppTransitionController: handleAppTransitionReady

WindowContainer: applyAnimationUnchecked->getAnimationAdapter
```java
final AppTransition appTransition = getDisplayContent().mAppTransition;
final RemoteAnimationController controller = appTransition.getRemoteAnimationController();
if (controller != null && !mSurfaceAnimator.isAnimationStartDelayed()) {
    //RemoteAnimationAdapterWrapper
} else if (isChanging) {
    //LocalAnimationAdapter
} else {
    //LocalAnimationAdapter
}
```
RemoteAnimationController有值才走远程动画

launcher进程

QuickstepTransitionManager: getActivityLaunchOptions

ActivityOptions: makeRemoteAnimation
```java
mAppLaunchRunner = new AppLaunchAnimationRunner(v, onEndCallback);
//...
RemoteAnimationRunnerCompat runner = new LauncherAnimationRunner(
    mHandler, mAppLaunchRunner, true);//true为优先插入到消息队列中
ActivityOptions options = ActivityOptions.makeRemoteAnimation(
    new RemoteAnimationAdapter(runner, duration, statusBarTransitionDelay),
    new RemoteTransition(runner.toRemoteTransition(),
            mLauncher.getIApplicationThread(), "QuickstepLaunch"));

public static ActivityOptions makeRemoteAnimation(RemoteAnimationAdapter remoteAnimationAdapter,
        RemoteTransition remoteTransition) {
    final ActivityOptions opts = new ActivityOptions();
    opts.mRemoteAnimationAdapter = remoteAnimationAdapter;
    opts.mAnimationType = ANIM_REMOTE_ANIMATION;
    opts.mRemoteTransition = remoteTransition;
    return opts;
}
```
SystemServer进程

AppTransition：overridePendingAppTransitionRemote中初始化RemoteAnimationController

ActivityRecord：applyOptionsAnimation
```java
if (mPendingRemoteAnimation != null) {
    mDisplayContent.mAppTransition.overridePendingAppTransitionRemote(
            mPendingRemoteAnimation);
    mTransitionController.setStatusBarTransitionDelay(
            mPendingRemoteAnimation.getStatusBarTransitionDelay());
}
```
mPendingRemoteAnimation赋值实在setOptions
```java
private void setOptions(@NonNull ActivityOptions options) {
    mLaunchedFromBubble = options.getLaunchedFromBubble();
    mPendingOptions = options;
    if (options.getAnimationType() == ANIM_REMOTE_ANIMATION) {
        mPendingRemoteAnimation = options.getRemoteAnimationAdapter();
    }
    mPendingRemoteTransition = options.getRemoteTransition();
}
```
getRemoteAnimationAdapter就是获取到opts.mRemoteAnimationAdapter = remoteAnimationAdapter

回到之前RemoteAnimationController就有值，创建RemoteAnimationRecord
```java
//...
final RemoteAnimationController.RemoteAnimationRecord adapters;
if (!isChanging && !enter && isClosingWhenResizing()) {
    final Rect closingStartBounds = getDisplayContent().mClosingChangingContainers
            .remove(this);
    adapters = controller.createRemoteAnimationRecord(
            this, mTmpPoint, localBounds, screenBounds, closingStartBounds,
            showBackdrop, false /* shouldCreateSnapshot */);
} else {
    final Rect startBounds = isChanging ? mSurfaceFreezer.mFreezeBounds : null;
    adapters = controller.createRemoteAnimationRecord(
            this, mTmpPoint, localBounds, screenBounds, startBounds, showBackdrop);
}
//...
```
```java
RemoteAnimationRecord createRemoteAnimationRecord(WindowContainer windowContainer,
        Point position, Rect localBounds, Rect endBounds, Rect startBounds,
        boolean showBackdrop, boolean shouldCreateSnapshot) {
    final RemoteAnimationRecord adapters = new RemoteAnimationRecord(windowContainer, position,
            localBounds, endBounds, startBounds, showBackdrop, shouldCreateSnapshot);
    mPendingAnimations.add(adapters);//添加到集合中
    return adapters;
}
```
adapter准备好后启动动画
```java
//...
animationRunnerBuilder.build()
.startAnimation(getPendingTransaction(), adapter, !isVisible(),
        ANIMATION_TYPE_APP_TRANSITION, thumbnailAdapter);
//...
```
SurfaceAnimator：startAnimation

回到handleAppTransitionReady
```java
//...
try {
    applyAnimations(tmpOpenApps, tmpCloseApps, transit, animLp, voiceInteraction); //准备好动画后
    handleClosingApps();
    handleOpeningApps();
    handleChangingApps(transit);
    handleClosingChangingContainers();

    appTransition.setLastAppTransition(transit, topOpeningApp,
            topClosingApp, topChangingApp);

    final int flags = appTransition.getTransitFlags();
    layoutRedo = appTransition.goodToGo(transit, topOpeningApp);
    appTransition.postAnimationCallback();
} finally {
    appTransition.clear();
    mService.mSurfaceAnimationRunner.continueStartingAnimations();
}
//...
```
AppTransition：goodToGo

RemoteAnimationController: goodToGo
```java
//...
mRemoteAnimationAdapter.getRunner().onAnimationStart(transit, appTargets, wallpaperTargets, nonAppTargets, mFinishedCallback);
//...
```
onAnimationStart回回调到launcher进程中LauncherAnimationRunner的
onAnimationStart
```java
public void onAnimationStart(
        int transit,
        RemoteAnimationTarget[] appTargets,
        RemoteAnimationTarget[] wallpaperTargets,
        RemoteAnimationTarget[] nonAppTargets,
        Runnable runnable) {
    Runnable r = () -> {
        finishExistingAnimation();
        mAnimationResult = new AnimationResult(() -> mAnimationResult = null, runnable);
        getFactory().onAnimationStart(transit, appTargets, wallpaperTargets, nonAppTargets,
                mAnimationResult);
    };
    if (mStartAtFrontOfQueue) {//此处为true
        postAtFrontOfQueueAsynchronously(mHandler, r);
    } else {
        postAsyncCallback(mHandler, r);
    }
}
```
getFactory().onAnimationStart调用的是AppLaunchAnimationRunner里面的onAnimationStart，然后根据点击类型实现不同的动画

组件：composeWidgetLaunchAnimator

图标：composeIconLaunchAnimator

任务中心：composeRecentsLaunchAnimator

图标是View，窗口是传递过来的SurfaceControl(leash)

动画结束后通过AnimationResult的runnable跨进程通知SystemServer
