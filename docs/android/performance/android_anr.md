## ANR
### ANR类型
|类型|产生条件|logcat关键字|
|--|--|--|
|KeyDispatch Timeout|input事件5s内未处理完|Input event dispatching timed out|
|Broadcast Timeout|onReceive(前台广播10s、后台60s)未处理完|Timeout of broadcast BroaddcastRecord|
|Service Timeout|onCreate、onBind等生命周期(前台服务20s、后台200s)未处理完|Timeout exxecuting service|
|ContentProvider Timeout|10s未处理完|timeout publishing content providers|

### ANR常见原因
1. 主线程频繁进行耗时IO操作
2. 多线程操作的死锁，主线程被block
3. 主线程被Binder对端block
4. System Server中WatchDog
5. service binder的连接达到上限无法和System Server通信
6. 系统资源耗尽

### 处理手段
#### 线下

1. 通过logcat和/data/anr/trace_*.txt文件定位确定anr时间点
2. CPU使用率
3. 主线程状态、其他线程状态

关键信息：ANR时间、进程pid、进程名、ANR类型

#### 线程状态
UNDEFINED=-1

ZOMBIDE=0//terminated

RUNNING=1

TIMED_WAIT=2

MONITOR=3//blocked

WAIT=4

INITIALIZING=5

STARTING=6

NATIVE=7

VMWAIT=8

SUSPENDED=9

java中Thread封装的[生命周期](../java/java_thread.md#thread_life)

#### 线上

通过友盟、bugly等监控，提取日志手机配置信息大致定位，有条件可本地复现

#### 自行实现ANR监听

1.利用FileObserver，但受selinux限制，系统开发不影响，三方app不行
```kotlin
class AnrObserver : FileObserver {
    constructor(path: String) : super(path)

    @RequiresApi(Build.VERSION_CODES.Q)
    constructor(file: File) : super(file)

    override fun onEvent(event: Int, path: String?) {
        when (event) {
            ACCESS -> {}//文件被访问
            ATTRIB -> {}//文件属性被修改，如chmod、chown、touch等
            CLOSE_NOWRITE -> {}//不可写文件被close
            CLOSE_WRITE -> {}//可写文件被close
            CREATE -> {}//创建新文件
            DELETE -> {}//文件被删除
            DELETE_SELF -> {}//自删除，即一个可执行文件在执行时删除自己
            MODIFY -> {}//文件被修改
            MOVE_SELF -> {}//自移动，即一个可指向性文件在执行时移动自己
            MOVED_FROM -> {}//文件被移走，如mv
            MOVED_TO -> {}//文件被移来，如mv、cp
            OPEN -> {}//文件被打开
            else -> {}//ALL_EVENTS包括以上所有事件
        }
    }
}
```
调用startWatching和stopWatching来启动监听

2.参考系统的WatchDog实现

原理是使用Handler每个5s执行一个后台线程，如果能执行成功则没有出现ANR

```kotlin
class ANRWatchDog private constructor() : Thread("ANR-WatchDog-Thread") {
    companion object {
        val TAG: String = "ANR"
        val TIMEOUT: Long = 5000L
        val instance: ANRWatchDog = Holder.instance
    }

    private var ignoreDebugger: Boolean = true

    private val mainHandler by lazy { Handler(Looper.getMainLooper()) }

    private val anrChecker: ANRChecker by lazy { ANRChecker() }

    private val lock = Object()

    var onAnrHappened: ((stackTraceInfo: String) -> Unit)? = null

    override fun run() {
        Process.setThreadPriority(Process.THREAD_PRIORITY_BACKGROUND) //后台线程
        while (true) {
            while (!isInterrupted) {
                synchronized(lock) {
                    anrChecker.schedule()
                    var waitTime = TIMEOUT
                    val start = SystemClock.uptimeMillis()
                    while (waitTime > 0) {
                        try {
                            lock.wait(waitTime)
                        } catch (e: InterruptedException) {
                            Log.w(TAG, e.toString())
                        }
                        waitTime = TIMEOUT - (SystemClock.uptimeMillis() - start)
                    }
                }
                if (!anrChecker.isBlocked()) {
                    continue
                }
                if (!ignoreDebugger && Debug.isDebuggerConnected()) {
                    continue
                }
                onAnrHappened?.invoke(getStackTraceInfo())
            }
            onAnrHappened = null
        }
    }

    private fun getStackTraceInfo(): String {
        val sb = StringBuilder()
        for (stElement in Looper.getMainLooper().thread.stackTrace) {
            sb.append(stElement.toString()).append("\r\n")
        }
        return sb.toString()
    }

    private object Holder {
        val instance: ANRWatchDog = ANRWatchDog()
    }

    private inner class ANRChecker : Runnable {
        private var completed: Boolean = false
        private var startTime: Long = 0
        private var executeTime: Long = SystemClock.uptimeMillis()

        override fun run() {
            synchronized(lock) {
                completed = true
                executeTime = SystemClock.uptimeMillis()
            }
        }

        fun schedule() {
            completed = false
            startTime = SystemClock.uptimeMillis()
            mainHandler.postAtFrontOfQueue(this@ANRChecker)
        }

        fun isBlocked(): Boolean = !completed || executeTime - startTime >= 5000L
    }
}
```
测试代码
```kotlin
ANRWatchDog.instance.apply {
    onAnrHappened = { info: String ->
        Log.e("tag", "info: $info")
    }
    start()
}
val handler = Handler()
handler.postDelayed({
    Thread.sleep(1000 * 10)
}, 3000)
```
[ANRWatchDog-java版本](ANRWatchDog.java)

### <a id="focus_anr_away">分析手段</a>
过滤anr日志
```
adb logcat -b events |grep anr
```
#### 分类
Input dispatching timed out (%s)

Application does not have a focused window

产生条件：窗口没有焦点，发生key事件

复现场景：在Activity的onCreate中添加代码，返回键触发anr(或者其他key事件)
```kotlin
window.attributes = window.attributes.also {
    it.flags = WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE
}
```
焦点窗口查看

**方式一**

adb shell dumpsys input(主要)

搜索Input Dispatcher State
```
Input Dispatcher State:
  DispatchEnabled: true
  DispatchFrozen: false
  InputFilterEnabled: false
  FocusedDisplayId: 0
  FocusedApplications:
    displayId=0, name='ActivityRecord{f0c9060 u0 com.android.launcher3/.uioverrides.QuickstepLauncher t208}', dispatchingTimeout=5000ms
  FocusedWindows:
    displayId=0, name='fc51d49 NotificationShade'
```
FocusedWindows不为null说明能正常派发key事件

搜索ANR
```
Input Dispatcher State at time of last ANR:
  ANR:
    Time: 2025-04-07 20:17:01
    Reason: ActivityRecord{713c197 u0 com.hfc.test/.TestActivity t257} does not have a focused window
    Window: ActivityRecord{713c197 u0 com.hfc.test/.TestActivity t257}
  DispatchEnabled: true
  DispatchFrozen: false
  InputFilterEnabled: false
  FocusedDisplayId: 0
  FocusedApplications:
    displayId=0, name='ActivityRecord{713c197 u0 com.hfc.test/.TestActivity t257}', dispatchingTimeout=5000ms
  FocusedWindows: <none>
```
FocusedWindows为空导致

查看焦点切换
```
adb logcat -b events |grep input_focus
```
**方式二**

adb shell dumpsys window

搜索dumpsys window displays，可以看到mCurrentFocus和mFocusedApp
```
mCurrentFocus=Window{5a1af43 u0 com.android.launcher3/com.android.launcher3.uioverrides.QuickstepLauncher}
mFocusedApp=ActivityRecord{f0c9060 u0 com.android.launcher3/.uioverrides.QuickstepLauncher t208}
```
mCurrentFocus和mFocusedApp不一定是相同的，比如下拉状态栏时
```
mCurrentFocus=Window{fc51d49 u0 NotificationShade}
mFocusedApp=ActivityRecord{f0c9060 u0 com.android.launcher3/.uioverrides.QuickstepLauncher t208}
```

搜索dumpsys window lastanr可以查看最近一次anr信息，比如
```
WINDOW MANAGER LAST ANR (dumpsys window lastanr)
  ANR time: 2025年4月7日 20:17:01
  Application at fault: ActivityRecord{713c197 u0 com.hfc.test/.TestActivity
  Reason: Input dispatching timed out (Application does not have a focused window).

  Display #0 currentFocus=null focusedApp=ActivityRecord{713c197 u0 com.hfc.test/.TestActivity t257}
```
明显看到currentFocus(焦点窗口)为null

分析步骤

1.adb logcat -s InputDispatcher(查看时间点，比较准确)

2.adb logcat -b events |grep anr(主要查看时间和类型,时间点不太准确SystemServer打印的)

3.adb logcat -b events |grep input_focus

通过2查看大概时间，再通过1查看准确时间，查看3的情况request、leaving和entering结合wms中ProtoLog，在结合dumpsys窗口信息(比如焦点窗口是否有了，窗口绘制状态)