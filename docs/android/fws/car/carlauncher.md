### 窗口层级结构
```
RootWindowContainer
└── DisplayContent
    └── TaskDisplayArea
        └── Task(Launcher)
            └── ActivityRecord
                └── WindowToken
                    └── WindowState
                       └── SurfaceView
                          └── Task(Map)
                             └── ActivityRecord
                                └── WindowToken
                                   └── WindowState
```
### 生命周期

Launcher和Map都是Resume状态的原因

TaskFragment: resumeTopActivity

TaskDisplayArea: pauseBackTasks
```java
boolean pauseActivityIfNeeded(@Nullable ActivityRecord resuming, @NonNull String reason) {
    //...
    forAllLeafTaskFragments((taskFrag) -> {
        final ActivityRecord resumedActivity = taskFrag.getResumedActivity();
        if (resumedActivity != null && !taskFrag.canBeResumed(resuming)) {
            if (taskFrag.startPausing(false /* uiSleeping*/, resuming, reason)) {
                someActivityPaused[0]++;
            }
        }
    }, true /* traverseTopToBottom */);

    return someActivityPaused[0] > 0;
}

//TaskFragment
boolean canBeResumed(@Nullable ActivityRecord starting) {
    return isTopActivityFocusable()
            && getVisibility(starting) == TASK_FRAGMENT_VISIBILITY_VISIBLE;
}
```
重点是getVisibility(starting) == TASK_FRAGMENT_VISIBILITY_VISIBLE
```java
int getVisibility(ActivityRecord starting) {
    if (!isAttached() || isForceHidden()) {
        return TASK_FRAGMENT_VISIBILITY_INVISIBLE;
    }

    if (isTopActivityLaunchedBehind()) {
        return TASK_FRAGMENT_VISIBILITY_VISIBLE;
    }
    final WindowContainer<?> parent = getParent();
    final Task thisTask = asTask();
    if (thisTask != null && parent.asTask() == null
            && mTransitionController.isTransientVisible(thisTask)) {
        return TASK_FRAGMENT_VISIBILITY_VISIBLE;
    }
    //...
    boolean gotTranslucentFullscreen = false;
    for (int i = parent.getChildCount() - 1; i >= 0; --i) {
        final WindowContainer other = parent.getChildAt(i);
        if (other == null) continue;

        final boolean hasRunningActivities = hasRunningActivity(other);
        if (other == this) {
            //...
            break;
        }
        //...
        final int otherWindowingMode = other.getWindowingMode();
        if (otherWindowingMode == WINDOWING_MODE_FULLSCREEN
                || (otherWindowingMode != WINDOWING_MODE_PINNED && other.matchParentBounds())) {
            //...
            return TASK_FRAGMENT_VISIBILITY_INVISIBLE;
        }
        //...
    }
    //...
    return gotTranslucentFullscreen
            ? TASK_FRAGMENT_VISIBILITY_VISIBLE_BEHIND_TRANSLUCENT
            : TASK_FRAGMENT_VISIBILITY_VISIBLE;
}
```
最终是走最后的return，Map应用windowMode是WINDOWING_MODE_MULTI_WINDOW，但(otherWindowingMode != WINDOWING_MODE_PINNED && other.matchParentBounds())判断中matchParentBounds为false

### TaskView
Android11及以后的版本, 11过渡ActivityView

继承于SurfaceView

car_launcher中CardView作为地图的容器
```java
private void setupRemoteCarTaskView(ViewGroup parent) {
    mCarLauncherViewModel = new ViewModelProvider(this,
            new CarLauncherViewModelFactory(this, getMapsIntent()))
            .get(CarLauncherViewModel.class);

    getLifecycle().addObserver(mCarLauncherViewModel);
    addOnNewIntentListener(mCarLauncherViewModel.getNewIntentListener());

    //当taskView创建后添加到CardView(parent)中
    mCarLauncherViewModel.getRemoteCarTaskView().observe(this, taskView -> {
        if (taskView == null || taskView.getParent() == parent) {
            return;
        }
        if (taskView.getParent() != null) {
            ((ViewGroup) taskView.getParent()).removeView(taskView);
        }
        parent.removeAllViews(); // Just a defense against a dirty parent.
        parent.addView(taskView);
    });
}

public CarLauncherViewModel(@UiContext Context context, @NonNull Intent mapsIntent) {
    mWindowContext = context.createWindowContext(TYPE_APPLICATION_STARTING, /* options */ null);
    mMapsIntent = mapsIntent;
    mCar = Car.createCar(mWindowContext);
    mCarActivityManager = mCar.getCarManager(CarActivityManager.class);
    mHostLifecycle = new CarTaskViewControllerHostLifecycle();
    mRemoteCarTaskView = new MutableLiveData<>(null);
    ControlledRemoteCarTaskViewCallback controlledRemoteCarTaskViewCallback =
            new ControlledRemoteCarTaskViewCallbackImpl(mRemoteCarTaskView);

    CarTaskViewControllerCallback carTaskViewControllerCallback =
            new CarTaskViewControllerCallbackImpl(controlledRemoteCarTaskViewCallback);

    mCarActivityManager.getCarTaskViewController(mWindowContext, mHostLifecycle,
            mWindowContext.getMainExecutor(), carTaskViewControllerCallback);
}
```

CarActivityManager: getCarTaskViewController

CarTaskViewControllerSupervisor: createCarTaskViewController

CarActivityService: addCarSystemUIProxyCallback,与服务端绑定(packages/services/Car/car-lib/src/android/car/app/)

ICarSystemUIProxyCallback.Stub: onConnected，绑定成功回调

ActivityHolder：onCarSystemUIConnected

CarLauncherViewModel：onConnected

CarTaskViewController：createControlledRemoteCarTaskView，创建ControlledRemoteCarTaskView，父类是SurfaceView

CarSystemUIProxyImpl：createControlledCarTaskView

RemoteCarTaskViewServerImpl：被创建

调用ControlledRemoteCarTaskViewCallback的onTaskViewCreated

ControlledRemoteCarTaskViewCallbackImpl：MutableLiveData.setValue更新view，然后被addView到CardView中

RemoteCarTaskView：触发SurfaceView的surfaceCreated
```java
if (mICarTaskViewHost != null) {
    if (!mInitialized) {
        onInitialized();
        mInitialized = true;
    }
}
mICarTaskViewHost.notifySurfaceCreated(
    SurfaceControlHelper.copy(getSurfaceControl()))//复制一份(new)
```

ControlledRemoteCarTaskView：onInitialized->startActivity

RemoteCarTaskView：startActivity

RemoteCarTaskViewServerImpl：startActivity
```java
private void prepareActivityOptions(ActivityOptions options, Rect launchBounds) {
    final Binder launchCookie = new Binder();
    mShellExecutor.execute(() -> {
        mTaskOrganizer.setPendingLaunchCookieListener(launchCookie, this);
    });
    options.setLaunchBounds(launchBounds);//设置bounds
    options.setLaunchCookie(launchCookie);
    options.setLaunchWindowingMode(WINDOWING_MODE_MULTI_WINDOW);//设置窗口mode
    options.setRemoveWithTaskOrganizer(true);
}
```

TaskViewTaskController: onTaskAppeared

RemoteCarTaskViewServerImpl：notifySurfaceCreated

TaskViewTaskController：surfaceCreated
```java
mTransaction.reparent(mTaskLeash, mSurfaceControl)
    .show(mTaskLeash)
    .apply();
```
### ActivityView
Android11及之前版本

继承于ViewGroup，内部有一个SurfaceView

Android10版本使用的是虚拟屏方案，主要代码如下，在surfaceCreated的时候
```java
if (mVirtualDisplay == null) {
    initVirtualDisplay(new SurfaceSession());
    if (mVirtualDisplay != null && mActivityViewCallback != null) {
        mActivityViewCallback.onActivityViewReady(ActivityView.this);
    }
} else {
    mTmpTransaction.reparent(mRootSurfaceControl,
            mSurfaceView.getSurfaceControl()).apply();
}

if (mVirtualDisplay != null) {
    mVirtualDisplay.setDisplayState(true);
}

updateLocationAndTapExcludeRegion();
```
initVirtualDisplay如下
```java
final int width = mSurfaceView.getWidth();
final int height = mSurfaceView.getHeight();
final DisplayManager displayManager = mContext.getSystemService(DisplayManager.class);
//创建虚拟屏和SurfaceView相同大小
mVirtualDisplay = displayManager.createVirtualDisplay(
        DISPLAY_NAME + "@" + System.identityHashCode(this), width, height,
        getBaseDisplayDensity(), null,
        VIRTUAL_DISPLAY_FLAG_PUBLIC | VIRTUAL_DISPLAY_FLAG_OWN_CONTENT_ONLY
                | VIRTUAL_DISPLAY_FLAG_DESTROY_CONTENT_ON_REMOVAL);
if (mVirtualDisplay == null) {
    Log.e(TAG, "Failed to initialize ActivityView");
    return;
}

final int displayId = mVirtualDisplay.getDisplay().getDisplayId();
final IWindowManager wm = WindowManagerGlobal.getWindowManagerService();
//额外创建图层，并设置为SurfaceView的子节点
mRootSurfaceControl = new SurfaceControl.Builder(surfaceSession)
        .setContainerLayer()
        .setParent(mSurfaceView.getSurfaceControl())
        .setName(DISPLAY_NAME)
        .build();

try {
    // 挂载到DisplayContent中，调用到Session(SystemServer端)
    WindowManagerGlobal.getWindowSession().reparentDisplayContent(
            getWindow(), mRootSurfaceControl, displayId);
    wm.dontOverrideDisplayInfo(displayId);
    if (mSingleTaskInstance) {
        mActivityTaskManager.setDisplayToSingleTaskInstance(displayId);
    }
    wm.setForwardedInsets(displayId, mForwardedInsets);
} catch (RemoteException e) {
    e.rethrowAsRuntimeException();
}
//显示图层
mTmpTransaction.show(mRootSurfaceControl).apply();
```
挂载完成后启动Activity，mActivityViewCallback.onActivityViewReady(ActivityView.this)是在CarLauncher中
```java
public void onActivityViewReady(ActivityView view) {
    mActivityViewReady = true;
    startMapsInActivityView();
    maybeLogReady();
}

mActivityView.startActivity(getMapsIntent());

public void startActivity(@NonNull Intent intent) {
    final ActivityOptions options = prepareActivityOptions();
    getContext().startActivity(intent, options.toBundle());
}

 private ActivityOptions prepareActivityOptions() {
    final ActivityOptions options = ActivityOptions.makeBasic();
    //用来指定启动到某个屏幕中，这里是启动到这个虚拟屏上
    options.setLaunchDisplayId(mVirtualDisplay.getDisplay().getDisplayId());
    return options;
}
```
Android11将虚拟屏相关封装到VirtualDisplayTaskEmbedder类中，CarActivityView继承于ActivityView

### TaskView vs ActivityView
ActivityView没有引用WindowManager-Shell，即多屏操作不依赖SystemUI

挂载方式不同

ActivityView是创建虚拟屏幕(DisplayContent)挂载到SurfaceView上的mRootSurfaceControl下

TaskView是直接将Task挂载到SurfaceView

ActivityView是ViewGroup，内部有个SurfaceView

Task是继承于SurfaceView