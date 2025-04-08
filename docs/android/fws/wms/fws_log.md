### 日志分类
命令过滤如：adb logcat -b main

默认main,system,crash,kernel

1. main: Log.i()

2. system: Slog.i()

3. radio

4. events: ams关键日志，查看应用的生命周期情况adb logcat -b events

5. crash

6. default

7. all

#### ProtoLog
adb shell wm logging enable-text/disable-text tag

在状态栏相关时aosp14以后使用

adb shell dumpsys activity service SystemUIService WMShell protolog enable-text/disable-text tag

其中tag查找源码ProtoLog.v(tag,...)获取

搜索wm_on可过滤Activity回调，更全可以直接搜wm_

dumpsys package xxx

### 打印堆栈
```java
android.util.Log.i(TAG,  Log.getStackTraceString(new Throwable()));
android.util.Log.d(TAG, "xxx", new Exception());
```

### 跨进程打印
```shell
adb shell am trace-ipc start
```
触发场景
```shell
adb shell am trace-ipc stop --dump-file /data/local/ipc.txt 
adb pull /data/local/ipc.txt .
```