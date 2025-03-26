### Android的事件分发机制

见[事件分发](../android_ui.md#view_dispatch)

### HandlerThread的使用场景和用法

见[HandlerThread](../fws/fws_handler.md#HandlerThread)

### Handler相关问题

见[Handler](../fws/fws_handler.md)

HandlerThread是什么？为什么它会存在？

简述下 Handler 机制的总体原理？

Looper 存在哪？如何可以保证线程独有？

如何理解 ThreadLocal 的作用？

主线程 Main Looper 和一般 Looper 的异同？

Handler 或者说 Looper 如何切换线程？

Looper 的 loop() 死循环为什么不卡死？

Looper 的等待是如何能够准确唤醒的？

Message 如何获取？为什么这么设计？

### AMS相关

### ActivityManagerService是什么？什么时候初始化的？

在SystemServer中的startBootstrapServices创建，使用反射创建内部类Lifecycle，然后内部类new出ams，atms也一样，比ams先创建

### ActivityThread是什么?ApplicationThread是什么?他们的区别

ActivityThread是主线程,通过Instrumentation管理Activity的生命周期

AMS通过ApplicationThread与ActivityThread进行通信。ApplicationThread是ActivityThread的内部类，实现了IApplicationThread接口。AMS通过Binder机制调用ApplicationThread的方法（如scheduleLaunchActivity），ApplicationThread通过调用ActivityThread的mH(Handler)调用ActivityThread中相应方法

### Instrumentation是什么？和ActivityThread是什么关系？

Instrumentation主要用于创建Application和Activity实例以及管理生命周期，可以用来模拟点击，滑动和按键，可以用来获取内存信息以及性能检测，Debug.startMethodTracing()和Debug.stopMethodTracing()：用于方法级别的性能分析，生成trace文件

Instrumentation是ActivityThread的成员变量，在AMS调用bindApplication时初始化

### AMS相关总结
服务端初始化过程

1.创建
```java
ActivityTaskManagerService atm = mSystemServiceManager.startService(
        ActivityTaskManagerService.Lifecycle.class).getService();
mActivityManagerService = ActivityManagerService.Lifecycle.startService(
        mSystemServiceManager, atm);

//atms为例
public Lifecycle(Context context) {
    super(context);
    mService = new ActivityTaskManagerService(context);
}

@Override
public void onStart() {
    publishBinderService(Context.ACTIVITY_TASK_SERVICE, mService);
    mService.start();
}
```
2.添加到集合中
```java
protected final void publishBinderService(String name, IBinder service, boolean allowIsolated, int dumpPriority) {
    ServiceManager.addService(name, service, allowIsolated, dumpPriority);
}
public static void addService(String name, IBinder service, boolean allowIsolated, int dumpPriority) {
    try {
        getIServiceManager().addService(name, service, allowIsolated, dumpPriority);
    } catch (RemoteException e) {
        Log.e(TAG, "error in addService", e);
    }
}
```

客户端调用

getSystemService(Context.ACTIVITY_TASK_SERVICE)流程

最终调用ContextImpl的SystemServiceRegistry.getSystemService(this, name)

首次会初始化SystemServiceRegistry中的static代码块，其中会注册
```java
registerService(Context.ACTIVITY_TASK_SERVICE, ActivityTaskManager.class,
        new CachedServiceFetcher<ActivityTaskManager>() {
    @Override
    public ActivityTaskManager createService(ContextImpl ctx) {
        return ActivityTaskManager.getInstance();
    }});

private static <T> void registerService(@NonNull String serviceName,
            @NonNull Class<T> serviceClass, @NonNull ServiceFetcher<T> serviceFetcher) {
        //...
        SYSTEM_SERVICE_FETCHERS.put(serviceName, serviceFetcher);
        //...
    }
```
getSystemService
```java
public static Object getSystemService(ContextImpl ctx, String name) {
    //...
    final ServiceFetcher<?> fetcher = SYSTEM_SERVICE_FETCHERS.get(name);
    //...
    final Object ret = fetcher.getService(ctx);
    //...
    return ret;
}
```
SYSTEM_SERVICE_FETCHERS.get(name)调用的是
```java
return ActivityTaskManager.getInstance();

public static IActivityTaskManager getService() {
return IActivityTaskManagerSingleton.get();
}

private static final Singleton<IActivityTaskManager> IActivityTaskManagerSingleton =
    new Singleton<IActivityTaskManager>() {
        @Override
        protected IActivityTaskManager create() {
            final IBinder b = ServiceManager.getService(Context.ACTIVITY_TASK_SERVICE);
            return IActivityTaskManager.Stub.asInterface(b);
        
```
ServiceManager.getService(Context.ACTIVITY_TASK_SERVICE)
```java
public static IBinder getService(String name) {
    try {
        IBinder service = sCache.get(name);
        if (service != null) {
            return service;
        } else {
            return Binder.allowBlocking(rawGetService(name));
        }
    } catch (RemoteException e) {
        Log.e(TAG, "error in getService", e);
    }
    return null;
}
```
首次sCache肯定为null，所以走下面的
### ActivityManagerService和zygote进程通信是如何实现的

手写实现简化版AMS

### Binder相关

Binder有什么优势

Binder是如何做到一次拷贝的

MMAP的内存映射原理了解吗

Binder机制是如何跨进程的

说说四大组件的通信机制 


### Android Framework基础
​什么是Android Framework？它包含哪些组件和服务？

​目前的Android Framework版本是什么？相较于之前的版本有哪些改进？

​请解释一下Activity、Service、ContentProvider和BroadcastReceiver的作用和区别。​

​请介绍一下Activity的生命周期，可以画出生命周期图吗？

​Service有哪几种启动方式？它们的区别是什么？

​什么是AIDL？请介绍一下它的作用和使用方式。​

​请解释一下Binder是什么，它在Android Framework中的作用是什么？

​什么是页面调度，它是如何实现的？

​Android Framework提供哪些常用系统服务？它们的作用是什么？

​如何在系统中注册和使用BroadcastReceiver？

### Binder与IPC机制

​Binder驱动如何实现一次内存拷贝？

​Binder线程池饥饿导致TransactionException，如何解决？

​Zygote为什么用Socket而不用Binder？

​Activity启动到底经历几次跨进程调用？

​Binder传输数据量极限是多少？如何优化大文件传输？

### 性能优化
​主线程Looper为什么不会ANR？

​如何检测和解决Handler内存泄漏？

​如何优化应用的启动速度和冷启动时间？

​WMS如何实现流畅渲染？掉帧监控和优化方案有哪些？

### 系统架构与多线程
​Android系统架构是怎样的？各个层次如何协同工作？

​自定义View的绘制流程是怎样的？onMeasure、onLayout和onDraw方法的调用时机是什么？

​如何在Android应用中实现多进程？多进程带来的问题有哪些？

​Android提供了哪些多线程实现方式？它们的优缺点是什么？

### 其他高级话题
​如何通过源码分析理解Android Framework的工作原理？

​Android内存管理机制是怎样的？如何避免内存泄漏？

​Android系统服务和BroadcastReceiver的深入学习资料有哪些？

​如何设计和优化Android应用的架构以提高可扩展性和稳定性？

### AMS（ActivityManagerService）

​什么是AMS？

AMS是Android Framework中的一个系统进程，负责管理应用程序的生命周期，处理应用程序间的交互和协调不同组件之间的启动和销毁。
​AMS的主要职责是什么？

处理应用程序进程的创建、启动、调度和销毁；管理应用程序之间的交互，如Activity、Service、Broadcast等组件之间的启动和协调；进行应用程序进程的优先级管理，确保系统资源的有效分配。
​Activity的启动过程是怎样的？

包括通过Intent向AMS发送启动请求、AMS根据请求启动新的进程或选择已有的进程、通过Zygote创建进程、启动Activity并展示在屏幕上等步骤。
​Service的生命周期是怎样的？

包括onCreate()、onStartCommand()、onBind()、onUnbind()、onDestroy()等方法。
​AMS如何管理应用程序进程的优先级？

通过进程优先级来动态管理应用程序的资源，前台进程优先级最高，空进程优先级最低，系统会根据优先级进行进程的存活与关闭。
​Activity冷启动流程中AMS的作用是什么？

AMS负责对Activity的启动进行管理，判断应该创建新的任务栈还是使用已存在的任务栈，并在内存不足时进行内存管理，调用Activity的onDestroy()方法。

### PMS（PackageManagerService）
​什么是PMS？

PMS是Android Framework中的一个系统服务，负责管理应用程序的安装、卸载、更新和权限管理。
​PMS的主要职责是什么？

管理应用程序的包信息、权限、组件注册、应用程序的安装和卸载过程等。
​PMS如何处理应用程序的安装过程？

包括解析APK文件、验证签名、安装到指定目录、创建应用程序的包信息等步骤。
​PMS如何管理应用程序的权限？

通过解析应用程序的AndroidManifest.xml文件，管理应用程序的权限声明，并在运行时进行权限检查。

### WMS（WindowManagerService）
​什么是WMS？

WMS是Android系统中的一个组件，负责管理应用程序窗口的显示和操作。
​WMS的主要职责是什么？

接收来自应用程序和系统的窗口信息，将窗口组织成树形结构，并在屏幕上正确显示；处理窗口的动画、触摸事件和按键事件等操作。
​如何在Android中添加一个新的窗口？

通过WindowManager.addView()方法来实现，该方法接收一个View对象并将其加入到屏幕中。
​WindowManager.LayoutParams的作用是什么？

用于控制窗口的显示、大小和位置等属性，当应用程序需要添加一个新的窗口时，必须为该窗口指定一个WindowManager.LayoutParams对象。
​如何控制窗口的层级关系？

通过WindowManager.LayoutParams.type属性来控制窗口之间的层级关系，层级值越高的窗口优先级越高，显示在越上方。
综合问题
​AMS、PMS和WMS在Android系统中的协同工作是如何进行的？

AMS负责管理应用程序的生命周期和组件间的交互，PMS负责管理应用程序的安装和权限，WMS负责管理窗口的显示和操作。它们通过Binder机制进行通信，共同确保Android系统的正常运行。
​如何优化Android应用的启动速度？

可以通过减少启动时的初始化任务、优化Activity的冷启动流程、使用启动优化技术（如延迟加载、懒加载等）来提高应用的启动速度。
​在Android开发中，如何处理内存管理问题？

通过合理使用内存、避免内存泄漏、及时释放不再使用的资源、使用内存分析工具（如MAT、LeakCanary等）来检测和解决内存问题