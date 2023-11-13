### Android虚拟机
Dalvik内存区域块

Linear Alloc：匿名共享内存，只读区域

Zygote Space：Zygote进程和应用进程之间共享，对象在启动时会重新创建

Alloc Space：每个进程独占，分配对象区域

Art内存区域块

Non Moving Space

Zygote Space

Alloc Space

Image Space：预加载的类信息，Zygote进程和应用进程之间共享

Large Obj Space：分配大对象，如bitmap，此处对象只创建一次

运行时堆都分为前3个

| |Dalvik|Art|
|--|--|--|
回收算法|标记-清除算法|CMS？|

| |JVM|Android虚拟机|
|--|--|--|
|虚拟机栈|局部变量表和操作数栈|寄存器|
|堆|Eden、S0、S1和老年代|Alloc Space先当于新生代，Large Obj Space相当于老年代
|加载|class|dex|

### 进程内存分配
通过getprop获取

app启动初始堆分配的内存：

\[dalvik.vm.heapstartsize]: [8m]

app最大内存限制：

\[dalvik.vm.heapgrowthlimit]: [256m]

开启largeHeap=true后的最大限制

\[dalvik.vm.heapsize]: [512m]

largeHeap为true，则内存到heapsize才会OOM，否则达到 heapgrowthlimit就会OOM

### 低内存kill进程机制
进程分为：空进程、后台进程、服务进程、可见进程、前台进程

AMS会对进程进行评分，记录到adj中

oom_adj: ps查看pid 然后使用cat/pid/oom_adj，内存不足时根据此值进行kill(从高到底) 

保活可以用workmanager

### 内存抖动
短时间内存反复发生增长和回收，可能会导致卡顿和oom

卡顿：gc回收中会有stw使得工作线程(包括主/子线程)暂停

oom：如果垃圾回收采用的时标记-清除法会出现大量内存碎片，此时创建一个比较大的对象(如数组)时由于没有连续内存空间导致oom

#### 监控工具

[BlockCanary](./android_blockcanary.md)

[Matrix](./android_matrix.md)

都是采用AOP思想

切面

BaseActivity：侵入性太强

Message

#### 常见内存抖动
- 字符串拼接(StringBuilder代替+)，Color.parseColor()
- 在循环里面创建对象
- 在onDraw里面创建对象

#### 内存抖动分析工具

Android Studio Memory-profiler中

Record native allocations

Record Java/Kotlin allocations

根据上下文环境优化对象创建

优化本质：减少对象的创建

优化手段：对象池，可参考Handler、Glide、Okhttp等

Handler：采用单项链表实现

Glide：双Map实现

缺陷就是数组对象池中key不是基本数据类型会产生大量Integer对象

优化方式是使用仿照SparseArray结合TreeMap实现，TreeMap主要有ceilingKey方法需要模仿。

ceilingKey方法作用

数组如byte[]需要25个大小，而对象池中只有20或30的大小，此时通过ceilingKey可以获取到30大小

为何不直接创建25个大小的数组呢？ 违反本质是较少对象创建

### 内存泄漏
当对象不能被回收导致内存越来越少，最终可能会oom

[java对象存活](./java/java_jvm.md#obj_live)

常见泄漏：单例context(applicationContext或者手动将单例置null)、handler(弱引用+销毁removeMessage操作)、、

#### 常见内存泄漏原因

- 静态变量或单例(不要持有activity或view等对象的引用，改成弱引用)
- 非静态类持有静态实例引用(改成静态内部类)
- 匿名内部类(Handler，可改成静态内部类或者弱引用方式+销毁removeMessage操作)
- webview(通过动态创建+addView/removeView的形式或独立进程退出调用System.exit)
- 动画问题(activity销毁，需要cancel动画)
- 流、Cursor、File等没有close，Bitmap的recycle(高版本其实)
- 线程池没有shutdown
- Activity泄漏(主线程一直在刷新UI或者动画执行，注意View刷新在onStop改成onPause处理)

AMS兜底机制：启动Activity时，会启动定时10s

ActivityA(一直执行动画) 跳转到ActivityB 透明主题 finish后B内存泄漏问题

onDestory 10s后才执行问题

原因：前者是因为activity存放在ActivityThread的mNewActivities，而mNewActivities=null执行是在

Looper.myQueue().addIdleHandler(new Idler());

IdleHandler是空闲时才执行，由于动画刷新UI导致Handler没有处于空闲导致mNewActivities=null不执行

后者原因也类似ac.activityIdle(a.token, a.createdConfig, stopProfiling)没执行

#### 内存泄漏分析工具

[MAT](./android_mat.md)

[LeakCanary](./android_leakcanary.md)

[Matrix](./android_matrix.md)

[Koom](./android_koom.md)

Android Studio Memory-profiler

使用教程：
https://developer.android.com/studio/profile/memory-profiler#performance

Capture heap dump

### 内存溢出(OOM)
分类：Java堆内存溢出(常见)、无足够连续内存空间、FD数量超出限制、线程数超出限制、虚拟内存不足

#### 常见内存溢出
- 内存抖动
- 内存泄漏
- 文件数达上限
- 线程数达上限
- 内存不足

### 分析内存命令

#### dumpsys meminfo

可以看到不同标准排序

dumpsys meminfo 包名

内存指标
||含义|等价|
|--|--|--|
|USS|物理内存|进程独占的内存|
|PSS|物理内存|PSS=USS+按比例包含共享库|
|RSS|物理内存|RSS=USS+包含共享库|
|VSS|虚拟内存|VSS=RSS+未分配实际物理内存|

一般分析PSS

可以在界面跳转后前后dumpsys对比数据，只能大概判断是否有泄漏

#### procrank
#### cat /proc/meminfo
#### free
#### showmap
#### vmstat
#### 总结
1. dumpsys meminfo 适用场景： 查看进程的oom adj，或者dalvik/native等区域内存情况，或者某
个进程或apk的内存情况，功能非常强大；
2. procrank 适用场景： 查看进程的VSS/RSS/PSS/USS各个内存指标；
3. cat /proc/meminfo 适用场景： 查看系统的详尽内存信息，包含内核情况；
4. free 适用场景： 只查看系统的可用内存；
5. showmap 适用场景： 查看进程的虚拟地址空间的内存分配情况

### 分配大内存
AndroidManifest中android:largeHeap="true"，获取当前配置内存大小和最大内存大小
```kotlin
getSystemService(Context.ACTIVITY_SERVICE).let {it as ActivityManager
    val info = ActivityManager.MemoryInfo()// 获取系统内存
    it.getMemoryInfo(info)
    Log.e("tag", "memory: ${it.memoryClass}, large memory： ${it.largeMemoryClass}, info: ${info.availMem} ${info.totalMem}")
}
```

### 监听释放内存响应
根据内存level处理
```kotlin
class MainActivity : ComponentActivity(), ComponentCallbacks2 {
    override fun onTrimMemory(level: Int) {
        super.onTrimMemory(level)
    }
```