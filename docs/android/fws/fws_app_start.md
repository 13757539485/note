## app启动流程
### 冷启动
1.Launcher(或其他应用)进程

launcher调用Activity:startActivity->startActivityyForResult->

Instrumentation:execStartActivity

Instrumentation从哪里来？由SystemServer在bindApplication时创建的

2.system_server进程

通过binder调用atms的startActivity->startActivityAsUser

ActivityStarter:execute->executeRequest创建ActivityRecord

ActivityRecord创建会setState为INITIALIZING，生命周期相关

->startActivityUnchecked这里有个doResume参数为true

->startActivityInner调用setInitialState对mDoResume赋值

->getLaunchRootTask

RootWindowContainer:getLaunchRootTask

TaskDisplayArea:getOrCreateRootTask

Task:startActivityLocked

ActivityRecord:showStartingWindow->addStartingWindow启动闪屏页相关SplashScreen

RootWindowContainer: resumeFocusedTasksTopActivities

Task: resumeTopActivityUncheckedLocked->resumeTopActivityInnerLocked

TaskFragment: resumeTopActivity

ActivityTaskSupervisor:startSpecificActivity

如果next.attachedToProcess()则说明进程已经创建

ActivityTaskSupervisor:realStartActivityLocked见热启动流程

否则走atms:startProcessAsync

ams:startProcess

ProcessList: startProcessLocked->主要是创建newProcessRecordLocked，校验flag参数虚拟机相关，最终异步或同步都调用startProcess

如果usesWebviewZygote则调用startWebView

如果usesAppZygote则调用createAppZygoteForProcessIfNeeded

其他走Process.start方法

普通应用走usesAppZygote，创建AppZygote

AppZygote: getProcess->connectToZygoteIfNeededLocked

ZygoteProcess: startChildZygote创建LocalSocketAddress

startViaZygote->zygoteSendArgsAndGetResult->attemptZygoteSendArgsAndGetResult

返回ChildZygoteProcess对象mZygote

ZygoteProcess: waitForConnectionToZygote传入mZygote的socket地址然后建立连接->connect

->preloadApp

接着getProcess最终返回的是mZygote对象->start->startViaZygote->zygoteSendArgsAndGetResult->attemptZygoteSendArgsAndGetResult

zygote进程启动时main方法中会创建ZygoteServer

3.zygote进程

ZygoteServer: runSelectLoop->acceptCommandPeer等待接连->fillUsapPool

Zygote:forkUsap->nativeForkApp返回childMain，pid是0则表示在子进程(就是fork出来的应用进程)

ZygoteInit:zygoteInit

RuntimeInit:applicationInit->findStaticMain反射调用ActivityThread的main

创建ActivityThread对象调用attach方法

如果是系统(SystemServer)调用则创建Instrumentation，创建Application调用onCreate

如果不是系统调用则通过ams调用attachApplication(跨进程)，并传递IApplicationThread这个binder对象，这个对象是为了system_server进程方便能与这个应用进程通信

4.system_server进程

ams: attachApplication

IApplicationThread: bindApplication->handleBindApplication主要是创建Instrumentation

然后创建Application->makeApplication

Instrumentation:newApplication

Application:attach

初始化ContentProvider->installContentProviders->installProvider->instantiateProvider通过反射创建ContentProvider

ContentProvider:attachInfo->onCreate

调用Application的onCreate方法callApplicationOnCreate

Application:onCreate

接着回到atms:attachApplication

RootWindowContainer:attachApplication->startActivityForAttachedApplicationIfNeeded->realStartActivityLocked走热启动流程

5.app进程

### 热启动
1.Launcher(或其他应用)进程阶段一致

2.system_server进程

走到TaskFragment: resumeTopActivity

ActivityTaskSupervisor:startSpecificActivity->realStartActivityLocked

创建ClientTransaction，添加addCallback:LaunchActivityItem

设置请求setLifecycleStateRequest:ResumeActivityItem

atms:scheduleTransaction

ClientTransaction:schedule

ApplicationThread:通过IApplicationThread这个binder对象->scheduleTransaction

ActivityThread:scheduleTransaction通过Handler最终调用

TransactionExecutor:execute->executeCallbacks即LauncherActivityItem

LauncherActivityItem:execute->handleLaunchActivity

ActivityThread:走[handleLaunchActivity](./fws_app_start.md#app_start_launch)流程

回到TransactionExecutor:execute->executeLifecycleState->cycleToPath->performLifecycleSequence

TransactionHandler:handleStartActivity

接着ResumeActivityItem

ResumeActivityItem:execute->handleResumeActivity

ActivityThread:走[handleResumeActivity](./fws_app_start.md#app_start_resume)流程

补充：冷启动中的热启动流程

RootWindowContainer:realStartActivityLocked

ClientTransaction:schedule

## launcher启动流程
SystemServer:main->run->startOtherServices

ams:systemReady

atms:startHomeOnAllDisplays

RootWindowContainer:startHomeOnAllDisplays->startHomeOnDisplay->startHomeOnTaskDisplayArea

ActivityStartController:startHomeActivity创建Task

ActivityStarter:execute接着启动应用一样

## 细节补充
ServiceManager.getService(Context.ACTIVITY_TASK_SERVICE)

通过[AMS](./fws/fws_ams.md)启动流程可知在publishBinderService(Context.ACTIVITY_TASK_SERVICE, mService)会将atms添加到ServiceManager中

ActivityTaskManagerService: startProcessAsync
```java
final Message m = PooledLambda.obtainMessage(ActivityManagerInternal::startProcess,
                mAmInternal, activity.processName, activity.info.applicationInfo, knownToBeDead,
                isTop, hostingType, activity.intent.getComponent());
mH.sendMessage(m);
```
AMS在初始化的时候会在Lifecycle的onStart方法中调用start方法
LocalServices.addService(ActivityManagerInternal.class, mInternal)，LocalService继承于ActivityManagerInternal

反射调用ActivityThread的main方法，attach传入的是false
```java
Looper.prepareMainLooper();
ActivityThread thread = new ActivityThread();
thread.attach(false, startSeq);
Looper.loop();
```

在SystemServer中也会创建ActivityThread，createSystemContext方法中，attach传入的是true
```java
ActivityThread activityThread = ActivityThread.systemMain();

public static ActivityThread systemMain() {
    ThreadedRenderer.initForSystemProcess();
    ActivityThread thread = new ActivityThread();
    thread.attach(true, 0);
    return thread;
}
```

### <a id="app_start_n">Activity生命周期流程</a>
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


## 总结
1.onRestoreInstanceState是在onStart之后，onPostCreate之前调用

2.DecorView在onCreate中调用setContentView，ViewRootImpl在onResume创建后WindowManager会调用addView中创建并添加

3.PhoneWindow在attach中创建，在onCreate之前

4.Provider在Application的onCreate之前创建

5.onWindowFocusChanged在onResume之后调用

6.onStart被调用表示Activity可见但不可交互，onResume表示可见并可以交互