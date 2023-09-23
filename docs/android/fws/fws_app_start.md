冷启动和热启动

startActivity

Instrumentation: execStartActivity->
ActivityTaskManager.getService().startActivity
-> ServiceManager.getService(Context.ACTIVITY_TASK_SERVICE)

通过[AMS](./fws/fws_ams.md)启动流程可知在publishBinderService(Context.ACTIVITY_TASK_SERVICE, mService)会将atms添加到ServiceManager中

ActivityTaskManagerService: startAcitivity->startActivityAsUser

ActivityStater: execute->executeRequest->startActivityUnchecked->startActivityInner

RootWindowContainer: resumeFocusedTasksTopActivities

Task: resumeTopActivityUncheckedLocked->resumeTopActivityInnerLocked

TaskFragment: resumeTopActivity

ActivityTaskSupervisor: startSpecificActivity

如果进程已经存在：realStartActivityLocked([应用内启动](https://zhuanlan.zhihu.com/p/612833473))，否则交给atsm处理如下(桌面启动)

ActivityTaskManagerService: startProcessAsync
```java
final Message m = PooledLambda.obtainMessage(ActivityManagerInternal::startProcess,
                mAmInternal, activity.processName, activity.info.applicationInfo, knownToBeDead,
                isTop, hostingType, activity.intent.getComponent());
mH.sendMessage(m);
```
AMS在初始化的时候会在Lifecycle的onStart方法中调用start方法
LocalServices.addService(ActivityManagerInternal.class, mInternal)，LocalService继承于ActivityManagerInternal

LocalService：startProcess->startProcessLocked

ProcessList: startProcessLocked->startProcess
```java
if (hostingRecord.usesAppZygote()) {
    final AppZygote appZygote = createAppZygoteForProcessIfNeeded(app);
    startResult = appZygote.getProcess().start(xxx);
 }
```
AppZygote: getProcess->connectToZygoteIfNeededLocked

ZygoteProcess: startChildZygotestartViaZygote->zygoteSendArgsAndGetResult->attemptZygoteSendArgsAndGetResult

ChildZygoteProcess: new

ZygoteProcess: waitForConnectionToZygote->connect->preloadApp->start->startViaZygote->zygoteSendArgsAndGetResult->attemptZygoteSendArgsAndGetResult


ZygoteServer: runSelectLoop->acceptCommandPeer

ZygoteConnection: handleChildProc

ZygoteInit: zygoteInit

RuntimeInit: applicationInit->findStaticMain

反射调用ActivityThread的main方法
```java
Looper.prepareMainLooper();
ActivityThread thread = new ActivityThread();
thread.attach(false, startSeq);
Looper.loop();
```

注意：在SystemServer中也会创建ActivityThread，createSystemContext方法中
```java
ActivityThread activityThread = ActivityThread.systemMain();

public static ActivityThread systemMain() {
    ThreadedRenderer.initForSystemProcess();
    ActivityThread thread = new ActivityThread();
    thread.attach(true, 0);
    return thread;
}
```
attach参数不同，false情况下
ams：attachApplication->attachApplicationLocked




### <a id="app_start_n">普通app启动</a>
<a id="app_start_launch">handleLaunchActivity流程</a>
```java
public Activity handleLaunchActivity(ActivityClientRecord r,
PendingTransactionActions pendingActions, Intent customIntent) {
final Activity a = performLaunchActivity(r, customIntent);
}

private Activity performLaunchActivity(ActivityClientRecord r, Intent customIntent) {
    ContextImpl appContext = createBaseContextForActivity(r);//创建BaseContext
    Activity activity = null;

    //反射创建Activity
    java.lang.ClassLoader cl = appContext.getClassLoader();
    activity = mInstrumentation.newActivity(cl, component.getClassName(), r.intent);

    //...
    Application app = r.packageInfo.makeApplicationInner(false, mInstrumentation);//创建Applicaton

    //...
    //调用attch方法
    activity.attach(appContext, this, getInstrumentation(), r.token,r.ident, app, r.intent, r.activityInfo, title, r.parent,
        r.embeddedID, r.lastNonConfigurationInstances, config,
        r.referrer, r.voiceInteractor, window, r.activityConfigCallback,
        r.assistToken, r.shareableActivityToken);
    //...
    //设置Activity的主题
    int theme = r.activityInfo.getThemeResource();
    if (theme != 0) {
        activity.setTheme(theme);
    }
    //...

    if (r.isPersistable()) {
        mInstrumentation.callActivityOnCreate(activity, r.state, r.persistentState);
    } else {
        mInstrumentation.callActivityOnCreate(activity, r.state);
    }

    //...
    r.setState(ON_CREATE);//lifecycle状态设置
}
```
attach方法
```java
final void attach(Context context, ActivityThread aThread,
    Instrumentation instr, IBinder token, int ident,
    Application application, Intent intent, ActivityInfo info,
    CharSequence title, Activity parent, String id,
    NonConfigurationInstances lastNonConfigurationInstances,
    Configuration config, String referrer, IVoiceInteractor voiceInteractor,
    Window window, ActivityConfigCallback activityConfigCallback, IBinder assistToken,
    IBinder shareableActivityToken) {
    //...
    mWindow = new PhoneWindow(this, window, activityConfigCallback);// 创建window唯一实现类
    //...
}
```
callActivityOnCreate方法
```java
public void callActivityOnCreate(Activity activity, Bundle icicle,
            PersistableBundle persistentState) {
    prePerformCreate(activity);
    activity.performCreate(icicle, persistentState);
    postPerformCreate(activity);
}

final void performCreate(Bundle icicle, PersistableBundle persistentState) {
    final int windowingMode = getResources().getConfiguration().windowConfiguration.getWindowingMode();
    //多窗模式
    mIsInMultiWindowMode = inMultiWindowMode(windowingMode);
    //画中画模式
    mIsInPictureInPictureMode = windowingMode == WINDOWING_MODE_PINNED;
    //...
    //回调到Activity生命周期的onCreate方法
    if (persistentState != null) {
        onCreate(icicle, persistentState);
    } else {
        onCreate(icicle);
    }
    //...
    //fragment生命周期分发
    mFragments.dispatchActivityCreated();
    //...
```

<a id="app_start_resume">handleResumeActivity流程</a>
```java
ActivityThread.java
public void handleResumeActivity(ActivityClientRecord r, boolean finalStateRequest, boolean isForward, String reason) {
    if (!performResumeActivity(r, finalStateRequest, reason)) {
        return;
    }
    //...
}

public boolean performResumeActivity(ActivityClientRecord r, boolean finalStateRequest, String reason) {
    //...
    r.activity.performResume(r.startsNotResumed, reason);
    //...
}

 final void performResume(boolean followedByPause, String reason) {
        //...
        dispatchActivityPreResumed();
        performRestart(true /* start */, reason);
        //...
        mInstrumentation.callActivityOnResume(this);
        //...
        mFragments.dispatchResume();
        //...
        dispatchActivityPostResumed();
    }

public void callActivityOnResume(Activity activity) {
        activity.mResumed = true;
        activity.onResume();
        //...
    }
```
回调到Activity生命周期的onResume方法，回到handleResumeActivity接着往下走
```java
public void handleResumeActivity(ActivityClientRecord r, boolean finalStateRequest, boolean isForward, String reason) {
    //...
    if (!r.activity.mFinished && willBeVisible && r.activity.mDecor != null && !r.hideForNow) {
        //...
        r.activity.mVisibleFromServer = true;
        mNumVisibleActivities++;
        if (r.activity.mVisibleFromClient) {
            r.activity.makeVisible();
        }
    }
}

void makeVisible() {
    if (!mWindowAdded) {
        ViewManager wm = getWindowManager();
        wm.addView(mDecor, getWindow().getAttributes());
        mWindowAdded = true;
    }
    mDecor.setVisibility(View.VISIBLE);
}

//WindowManagerImpl.java
public void addView(@NonNull View view, @NonNull ViewGroup.LayoutParams params) {
    applyTokens(params);
    mGlobal.addView(view, params, mContext.getDisplayNoVerify(), mParentWindow, mContext.getUserId());
}

//WindowManagerGlobal.java
public void addView(View view, ViewGroup.LayoutParams params,
    Display display, Window parentWindow, int userId) {
    ViewRootImpl root;
    //...
    if (windowlessSession == null) {
        root = new ViewRootImpl(view.getContext(), display);
    } else {
        root = new ViewRootImpl(view.getContext(), display,
                windowlessSession);
    }
    view.setLayoutParams(wparams);
    mViews.add(view);
    mRoots.add(root);
    mParams.add(wparams);

    root.setView(view, wparams, panelParentView, userId);
    //...
}
```
setView中通过IWindowSession(Binder)与wms交互
