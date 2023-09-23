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
