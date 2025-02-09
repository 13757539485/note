### 日志分类
命令过滤如：adb logcat -b main

默认main,system,crash,kernel

1. main: Log.i()

2. system: Slog.i()

3. radio

4. events: ams关键日志，查看应用的生命周期情况

5. crash

6. default

7. all

#### ProtoLog
adb logcat wm enable-text/disable-text tag

其中tag查找源码ProtoLog.v(tag,...)获取

搜索wm_on可过滤Activity回调，更全可以直接搜wm_

dumpsys package xxx