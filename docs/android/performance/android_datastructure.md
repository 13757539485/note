https://developer.android.com/guide?hl=zh-cn

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

Tinker的so的加载流程
https://github.com/KeepSafe/Relinker

YSS>=RSS>=PSS>=USS

vmstart

cpu时间片 用户cpu时间 系统cpu时间 linux TMS


activity销毁如何得知？

ReferenceQueue

WeakHashMap：Key是弱引用可以被回收，把Activity作为key，当key被回收说明activity已经销毁了，结合registerActivityLifecycleCallbacks，stop中gc

阈值处理：KOOM > Matrix

SystemClock.sleep System.runFinalization

Debug.M

### KOOM
Native Heap泄漏监控

bionic

xhook

性能优化：

1.启动优化

启动背景优化SplashScreen

有向无环图DAG、拓扑排序、CountDownLatch控制线程顺序(app_startup、Android_startup)

架构上Application初始化分离到个模块

SplashActivity优化，责任链模式，onStop中finish防止Main失败时出现问题

懒加载、延迟加载

2.内存优化

内存泄漏、ANR、内存抖动、OOM、弱引用、JVM的gc回收算法(根可达)

bitmap、数据结构HashMap和SparseArray(ArrayMap)

线上监控KOOM、线下监控LeakCanary

3.UI优化

布局优化、webview优化、recycleview优化、自定义view(onDraw)

4.安装包瘦身

图片webp(svg、json)、so库、混淆、加固