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
- [ThreadLocal](../java/java_base.md#threadlocal)使用不当泄漏
- Activity泄漏(主线程一直在刷新UI或者动画执行，注意View刷新在onStop改成onPause处理)

AMS兜底机制：启动Activity时，会启动定时10s

ActivityA(一直执行动画) 跳转到ActivityB 透明主题 finish后B内存泄漏问题

onDestory 10s后才执行问题

原因：前者是因为activity存放在ActivityThread的mNewActivities，而mNewActivities=null执行是在

Looper.myQueue().addIdleHandler(new Idler());

IdleHandler是空闲时才执行，由于动画刷新UI导致Handler没有处于空闲导致mNewActivities=null不执行

后者原因也类似ac.activityIdle(a.token, a.createdConfig, stopProfiling)没执行

#### 内存泄漏分析工具

[MAT](./android_mat.md): 分析hprof文件

[LeakCanary](./android_leakcanary.md)：线下使用，实时，产生hprof文件

[Matrix](./android_matrix.md)

[Koom](./android_koom.md)：线上使用，无法实时，产生hprof文件

Android Studio Memory-profiler

使用教程：
https://developer.android.com/studio/profile/memory-profiler#performance

点击Capture heap dump即可得到hprof文件