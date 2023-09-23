## [IPC通信](./fws/fws_process.md)

## [Android启动流程](./fws/fws_android_start.md)

## [AMS](./fws/fws_ams.md)

## [WMS](./fws/fws_wms.md)

## [PMS](./fws/fws_pms.md)

### [App进程启动](./fws/fws_app_start.md)

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


