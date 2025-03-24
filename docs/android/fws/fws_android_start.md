### Android启动流程
#### BootLoader引导阶段

初始化基本的硬件设备(CPU、内存、Flash等)并建立内存映射，组合键进入Fastboot模式和Recovery模式

#### 装载和启动Linux内核阶段

由BootLoader装载boot.img(包含linux内核和根文件系统)进内存中，linux内核执行整个系统的初始化，装载根文件系统，启动init进程，装载完毕后BootLoader在内存中清除

#### 启动init进程

是系统的第一个进程，启动过程中会解析init.rc(system/core/rootdir/init.rc)脚本文件，根据内容

* 装载Android文件系统
* 创建系统目录
* 初始化属性系统
* 启动Android的重要守护进程，如USB、adb、vold、rild等守护进程
* init作为守护进程，用于执行属性修改请求，重启崩溃进程等操作

#### ServiceManager启动

由init进程启动，管理Binder服务(Binder服务的注册和查找)，作为守护进程存在与后台，当SystemServer启动AMS、WMS、PMS等服务时，都是通过Binder注册到ServiceManager中，统一保存管理

#### 启动Zygote进程

由init进程启动，负责fork出应用进程，初始化时会创建Android虚拟机和预装载系统的资源文件和java类，所有fork出的用户进程都共享，初始化结束后转成守护进程，响应启动应用请求

* 创建DVM/ART虚拟机
* 注册Android SDK所需的JNI方法
* 执行ZygoteInit的main函数

init.zygote64.rc
```
service zygote /system/bin/app_process64 -Xzygote /system/bin --zygote --start-system-server
    class main
```
执行代码：
frameworks/base/cmds/app_process/app_main.cpp
```cpp
runtime.start("com.android.internal.os.ZygoteInit", args, zygote);
```
执行frameworks/base/core/jni/AndroidRuntime.cpp中的start函数
```cpp
// 启动虚拟机
if (startVm(&mJavaVM, &env, zygote, primary_zygote) != 0) {
    return;
}
onVmCreated(env);
// 注册jni
if (startReg(env) < 0) {
    ALOGE("Unable to register all android natives\n");
    return;
}
char* slashClassName = toSlashClassName(className != NULL ? className : "");
jclass startClass = env->FindClass(slashClassName);
if (startClass == NULL) {
    ALOGE("JavaVM unable to locate class '%s'\n", slashClassName);
    /* keep going */
} else {
    // 找到main方法
    jmethodID startMeth = env->GetStaticMethodID(startClass, "main",
        "([Ljava/lang/String;)V");
    if (startMeth == NULL) {
        ALOGE("JavaVM unable to find main() in '%s'\n", className);
        /* keep going */
    } else {
        // 启动java层方法
        env->CallStaticVoidMethod(startClass, startMeth, strArray);
    }
}
```

执行代码：frameworks/base/core/java/com/android/internal/os/ZygoteInit.java中的main方法

#### [启动SystemServer](./fws_system_server.md)

由zygote进程fork出的第一个进程，负责Android中大部分Binder服务，先启动SensorManager，再启动AMS、PMS、WMS等所有java服务

#### 启动MediaServer

由init进程启动，包含媒体相关的Binder服务，包含CameraService、AudioFligerService、MediaPlayerService、AutioPolicyService

#### 启动Launcher

SystemServer加载完所有java服务后，调用AMS的SystemReady方法，Intent(android.intent.category.HOME)

### fork机制

1. 两个进程代码一致，代码执行到的位置也一致
2. 进程PID不一样
3. 父进程返回的是子进程的PID方便跟踪子进程状态，子进程返回的是0

### system_server为什么要在Zygote中启动，而不是由 init 直接启动

system_server就可以直接使用Zygote中的JNI 函数.共享库、常用的类、以及主题资源

### 为什么要专门使用Zygote进程去孵化应用进程，而不是让system_server去孵化

1. system_server相比 Zygote 多运行了 AMS、WMS 等服务，这些对一个应用程序来说是不需要的
2. 进程的fork()对多线程不友好，仅会将发起调用的线程拷贝到子进程，这可能会导致死锁，而system_server中肯定是有很多线程的

死锁的原因：fork出的子进程并没有父进程的所有线程，锁是可以被复制的，锁需要所有者(线程)才能解锁

### Zygote 为什么不采用 Binder 机制进行 IPC 通信

Binder是多线程的，Socket是单线程的

unix程序设计准则：多线程中不允许使用fork

补充：Zygote进程初始化时，Binder尚未被初始化

### 一键退出应用
```kotlin
val manager = getSystemService(Context.ACTIVITY_SERVICE) as ActivityManager
manager.appTasks.forEach {
    it.finishAndRemoveTask()
}
// vm层次，进程退出
android.os.Process.killProcess(android.os.Process.myPid())
exitProcess(0)
```