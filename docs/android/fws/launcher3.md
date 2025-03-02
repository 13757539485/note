## 最近多任务
### 定位界面
手机进入多任务界面，执行命令
```
adb shell am stack list
```
获取第一个Rootask的activiy信息，使用命令可查看布局结构，具体看View Hierrarchy部分
```
adb shell dumpsys activity com.android.launcher3/com.android.launcher3.uioverrides.QuickstepLauncher
```

### TaskView创建流程

packages/apps/Launcher3/quickstep/src/com/android/launcher3/uioverrides/QuickstepLauncher.java

创建ViewPool和RecentsModel

com.android.launcher3.util.ViewPool.<init>(ViewPool.java:54)
com.android.quickstep.views.RecentsView.<init>(RecentsView.java:825)
com.android.quickstep.views.LauncherRecentsView.<init>(LauncherRecentsView.java:78)
com.android.quickstep.views.LauncherRecentsView.<init>(LauncherRecentsView.java:74)
com.android.launcher3.statemanager.StatefulActivity.inflateRootView(StatefulActivity.java:72)
com.android.launcher3.Launcher.setupViews(Launcher.java:1365)
com.android.launcher3.uioverrides.QuickstepLauncher.setupViews(QuickstepLauncher.java:263)
com.android.launcher3.Launcher.onCreate(Launcher.java:523)
com.android.launcher3.uioverrides.QuickstepLauncher.onCreate(QuickstepLauncher.java:668)

创建TaskView

com.android.quickstep.views.TaskView.<init>(TaskView.kt:500)
com.android.quickstep.views.TaskView.<init>(TaskView.kt:109)
com.android.quickstep.views.TaskView.<init>(Unknown Source:15)
com.android.launcher3.util.ViewPool.inflateNewView(ViewPool.java:108)
com.android.launcher3.util.ViewPool.lambda$initPool$1(ViewPool.java:71)
com.android.launcher3.util.ViewPool.$r8$lambda$xmRVBUcjhnGyR58wYP_ubVk4Uoo(Unknown Source:0)
com.android.launcher3.util.ViewPool$$ExternalSyntheticLambda1.run(D8$$SyntheticClass:0)

创建RecentsModel和RecentTasksList

com.android.quickstep.RecentTasksList.<init>(RecentTasksList.java:123)
com.android.quickstep.RecentsModel.<init>(RecentsModel.java:94)
com.android.quickstep.RecentsModel.<init>(RecentsModel.java:86)
com.android.quickstep.RecentsModel.$r8$lambda$6vQaiFDqIiLIBNhYUBBc0ue2kH4(Unknown Source:2)
com.android.quickstep.RecentsModel$$ExternalSyntheticLambda0.get(D8$$SyntheticClass:0)
com.android.launcher3.util.MainThreadInitializedObject.lambda$get$0(MainThreadInitializedObject.java:56)
com.android.launcher3.util.MainThreadInitializedObject.$r8$lambda$icoSB2GSttV1yd5beTvcp2Ed9FU(Unknown Source:0)
com.android.launcher3.util.MainThreadInitializedObject$$ExternalSyntheticLambda1.get(D8$$SyntheticClass:0)
com.android.launcher3.util.TraceHelper.allowIpcs(TraceHelper.java:92)
com.android.launcher3.util.MainThreadInitializedObject.get(MainThreadInitializedObject.java:56)
com.android.quickstep.views.RecentsView.<init>(RecentsView.java:813)
com.android.launcher3.statemanager.StatefulActivity.inflateRootView(StatefulActivity.java:72)
com.android.launcher3.Launcher.setupViews(Launcher.java:1365)
com.android.launcher3.uioverrides.QuickstepLauncher.setupViews(QuickstepLauncher.java:263)
com.android.launcher3.Launcher.onCreate(Launcher.java:523)
com.android.launcher3.uioverrides.QuickstepLauncher.onCreate(QuickstepLauncher.java:668)

获取TaskView，手势上滑触发

com.android.quickstep.views.RecentsView.getTaskViewFromPool(RecentsView.java:2613)
com.android.quickstep.views.RecentsView.applyLoadPlan(RecentsView.java:1821)
com.android.quickstep.views.RecentsView$$ExternalSyntheticLambda21.accept(D8$$SyntheticClass:0)
com.android.quickstep.RecentTasksList.lambda$getTasks$3(RecentTasksList.java:185)
com.android.quickstep.RecentTasksList.$r8$lambda$6-PS2E-VkARt1ElIEKmm9qge-Ok(Unknown Source:0)
com.android.quickstep.RecentTasksList$$ExternalSyntheticLambda1.run(D8$$SyntheticClass:0)

### 数据获取流程

上面获取TaskView会调用getTasks

com.android.wm.shell.recents.IRecentTasks.getRecentTasks
com.android.quickstep.SystemUiProxy.getRecentTasks
com.android.quickstep.RecentTasksList.loadTasksInBackground
com.android.quickstep.RecentTasksList.getTasks

IRecentTasks是在SystemUI中，即与SystemUI进行通信

frameworks/base/packages/SystemUI/src/com/android/systemui/SystemUIInitializer.java

android.app.ActivityTaskManager.getRecentTasks
com.android.wm.shell.recents.RecentTasksController$RecentTasksImpl.getRecentTasks
com.android.systemui.dagger.WMComponent.getRecentTasks
com.android.systemui.SystemUIInitializer.init

applyLoadPlan中获取到的TaskView进行数据bind

### 数据持久化

从atms中获取的task是保存在哪里的？从getRecentTasks源码入手，ActivityTaskManager最终调用的是

frameworks/base/services/core/java/com/android/server/wm/ActivityTaskManagerService.java

中的getRecentTasks


```java
public ParceledListSlice<ActivityManager.RecentTaskInfo> getRecentTasks(int maxNum, int flags,
    int userId) {
    //...
    mRecentTasks.loadRecentTasksIfNeeded(userId);
    synchronized (mGlobalLock) {
        return mRecentTasks.getRecentTasks(maxNum, flags, allowed, userId, callingUid);
    }
}
```
frameworks/base/services/core/java/com/android/server/wm/RecentTasks.java

userId默认为0,loadRecentTasksIfNeeded是还原数据操作
```java
void loadRecentTasksIfNeeded(int userId) {
    AtomicBoolean userLoaded;
    //...
    final SparseBooleanArray persistedTaskIds =
            mTaskPersister.readPersistedTaskIdsFromFileForUser(userId);
    final TaskPersister.RecentTaskFiles taskFiles = TaskPersister.loadTasksForUser(userId);
    synchronized (mService.mGlobalLock) {
        restoreRecentTasksLocked(userId, persistedTaskIds, taskFiles);
    }
    //...
}
从文件中读取task的id集合
```java
SparseBooleanArray readPersistedTaskIdsFromFileForUser(int userId) {
    //...
    reader = new BufferedReader(new FileReader(getUserPersistedTaskIdsFile(userId)));
    while ((line = reader.readLine()) != null) {
        for (String taskIdString : line.split("\\s+")) {
            int id = Integer.parseInt(taskIdString);
            persistedTaskIds.put(id, true);
        }
    }
    //...
}
```
其中getUserPersistedTaskIdsFile获取的File路径为Environment.getDataSystemDirectory()/system_de/userid/persisted_taskIds.txt，即/data/system_de/0/persisted_taskIds.txt

loadTasksForUser读取的目录文件为/data/system_ce/0/recent_tasks/taskId_task.xml

restoreRecentTasksLocked主要是解析xml文件生成对应的Task对象
```java
final ArrayList<Task> tasks = mTaskPersister.restoreTasksForUserLocked(userId, taskFiles,
    existedTaskIds);//解析xml
//...
mTasks.addAll(tasks);
```
对Task进行校验并返回RecentTaskInfo数据
```java
ParceledListSlice<ActivityManager.RecentTaskInfo> getRecentTasks(int maxNum, int flags,
        boolean getTasksAllowed, int userId, int callingUid) {
    return new ParceledListSlice<>(getRecentTasksImpl(maxNum, flags, getTasksAllowed,
            userId, callingUid));
}
private ArrayList<ActivityManager.RecentTaskInfo> getRecentTasksImpl(int maxNum, int flags,
            boolean getTasksAllowed, int userId, int callingUid) {
    final ArrayList<ActivityManager.RecentTaskInfo> res = new ArrayList<>();
    for (int i = 0; i < size; i++) {
        final Task task = mTasks.get(i);
        //...
    }
    //...
    return res;
}
```
### 增加和删除Task

在frameworks/base/services/core/java/com/android/server/wm/RecentTasks.java

添加堆栈调试
```java
void add(Task task) {}
void remove(Task task) {}
```

### 缩略图获取

最终调用的是

packages/SystemUI/shared/src/com/android/systemui/shared/system/ActivityManagerWrapper.java

的getTaskThumbnail方法，接着调用atms中的getTaskSnapshot，然后调用TaskSnapshotController

frameworks/base/services/core/java/com/android/server/wm/TaskSnapshotController.java
```java
TaskSnapshot getSnapshot(int taskId, int userId, boolean restoreFromDisk,
            boolean isLowResolution) {
    return mCache.getSnapshot(taskId, userId, restoreFromDisk, isLowResolution
            && mPersistInfoProvider.enableLowResSnapshots());
}
```
frameworks/base/services/core/java/com/android/server/wm/TaskSnapshotCache.java
```java
TaskSnapshot getSnapshot(int taskId, int userId, boolean restoreFromDisk,
            boolean isLowResolution) {
    final TaskSnapshot snapshot = getSnapshot(taskId);//从缓存中获取
    if (snapshot != null) {
        return snapshot;
    }

    if (!restoreFromDisk) {
        return null;
    }
    return tryRestoreFromDisk(taskId, userId, isLowResolution);//从磁盘中获取
}
```
磁盘目录：data/system_ce/0/snapshots/

保存3种文件
1. taskId.jpg
2. taskId.proto
3. taskId_reduces.jpg(低分辨率)

### 截图线程

是在wms服务创建的时候启动的

frameworks/base/services/core/java/com/android/server/wm/WindowManagerService.java
```java
public void systemReady() {
    //...
    mSnapshotController.systemReady();
    //...
}
```
具体线程名
```java
private final Thread mPersister = new Thread("TaskSnapshotPersister")
```
四循环，通过WriteQueueItem的write，由子类实现

### 上滑手势
packages/apps/Launcher3/quickstep/src/com/android/quickstep/TouchInteractionService.java
```java

注册全局触摸监听
private void initInputMonitor(String reason) {
    //...
    mInputMonitorCompat = new InputMonitorCompat("swipe-up", mDeviceState.getDisplayId());
    mInputEventReceiver = mInputMonitorCompat.getInputReceiver(Looper.getMainLooper(),
            mMainChoreographer, this::onInputEvent);
    //...
}
```
可以使用adb shell dumpsys input查看到swipe-up
```shell
0: name='[Gesture Monitor] swipe-up', inputChannelToken=android.os.BinderProxy@a8797d6 displayId=0
```
具体事件处理逻辑在各个Consumer中，如上滑进入多任务是OverviewInputConsumer

packages/apps/Launcher3/quickstep/src/com/android/quickstep/inputconsumers/OverviewInputConsumer.java

全屏应用进入多任务走OtherActivityInputConsumer

这个进入多任务应用不会调用onPause和onStop，launcher会调用onReStart,onStart，onResume

https://blog.csdn.net/learnframework/article/details/123032419

https://blog.csdn.net/learnframework/article/details/132567567?spm=1001.2014.3001.5501