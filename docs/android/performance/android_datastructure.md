线上APM(Android performance monitor)相关知识点

技术点

Bytecode、Hook、JS注入、Gradle、ASM、kotlinpoet

Java层实现功能
- CPU指标
- 内存指标
- FPS指标
- ANR
- 卡顿
- GC/OOM
- 网络
- 功耗
- 远程下发 日志回捞 支持远程shell动态代码下发

APM实现思路
1. 配置(注解+json)
2. 数据链的保存
App启动、结束、界面跳转LifecycleCallbacks
3. Crash
Thread
4. CPU GC 电量
linux知识点 /proc/stat /proc/pid/stat BatteryMonitor
5. ANR FPS
文件检测/data/anr/trace.txt Choreographer.FrameCallback

adb shell dumpsys battery
获取手机电池相关信息

adb shell getprop ro.product.model
通过手机机型判断中高端手机

adb shell dumpsys window displays

mDisplayId: 显示屏编号

init：是初始分辨率和屏幕密度

app的高度比init小表示底部有虚拟按键

adb shell settings get secure android_id

adb shell service call iphonesubinfo 1
获取IEMI

adb shell cat /proc/cpuinfo
adb shell cat /proc/meminfo

MemAvailable=MemFree+Buffers+Cached+Slab

kernel动态内存分配：命令中无法查看

xlog

syscall 用户态 内核态

dumpsys meminfo

dumpsys meminfo 包名

RSS 共享库so 动态链接库

YSS>=RSS>=PSS>=USS

vmstart

cpu时间片 用户cpu时间 系统cpu时间 linux TMS

oom_adj: ps查看pid 然后使用cat/pid/oom_adj，内存不足时根据此值进行kill(从高到底) 保活可以用workmanager

ReferenceQueue

WeakHashMap：Key是弱引用可以被回收，把Activity作为key，当key被回收说明activity已经销毁了，结合registerActivityLifecycleCallbacks，stop中gc

SystemClock.sleep System.runFinalization

Debug.M

KOOM