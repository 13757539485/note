```java
if ("start-system-server".equals(argv[i])) {
    startSystemServer = true;
}
if (startSystemServer) {
    Runnable r = forkSystemServer(abiList, zygoteSocketName, zygoteServer);

    // {@code r == null} in the parent (zygote) process, and {@code r != null} in the
    // child (system_server) process.
    if (r != null) {
        r.run();
        return;
    }
}
if (!enableLazyPreload) {
    ...
    preload(bootTimingsTraceLog);// 加载系统res资源、sdk class和其他libc
    ...
}
caller = zygoteServer.runSelectLoop(abiList);// 死循环，接收其他进程的Socket消息
```
forkSystemServer方法
```java
String[] args = {
        "--setuid=1000",
        "--setgid=1000",
        "--setgroups=1001,1002,1003,1004,1005,1006,1007,1008,1009,1010,1018,1021,1023,"
                + "1024,1032,1065,3001,3002,3003,3005,3006,3007,3009,3010,3011,3012",
        "--capabilities=" + capabilities + "," + capabilities,
        "--nice-name=system_server",
        "--runtime-args",
        "--target-sdk-version=" + VMRuntime.SDK_VERSION_CUR_DEVELOPMENT,
        "com.android.server.SystemServer",
}
ZygoteCommandBuffer commandBuffer = new ZygoteCommandBuffer(args);
try {
    parsedArgs = ZygoteArguments.getInstance(commandBuffer);//解析参数
} catch (EOFException e) {
    throw new AssertionError("Unexpected argument error for forking system server", e);
}
// 创建SystemServer进程
pid = Zygote.forkSystemServer(
    parsedArgs.mUid, parsedArgs.mGid,
    parsedArgs.mGids,
    parsedArgs.mRuntimeFlags,
    null,
    parsedArgs.mPermittedCapabilities,
    parsedArgs.mEffectiveCapabilities);
return handleSystemServerProcess(parsedArgs);
```

handleSystemServerProcess方法中执行zygoteInit方法
```java
return ZygoteInit.zygoteInit(parsedArgs.mTargetSdkVersion,
        parsedArgs.mDisabledCompatChanges,
        parsedArgs.mRemainingArgs, cl);

```
zygoteInit方法中
```java
ZygoteInit.nativeZygoteInit();
return RuntimeInit.applicationInit(targetSdkVersion, disabledCompatChanges, argv,classLoader);
```
nativeZygoteInit是AndroidRuntime.cpp中注册的jni函数
```cpp
static void com_android_internal_os_ZygoteInit_nativeZygoteInit(JNIEnv* env, jobject clazz)
{
    gCurRuntime->onZygoteInit();
}
```
最终会在app_main.cpp中
```cpp
virtual void onZygoteInit()
{
    sp<ProcessState> proc = ProcessState::self();
    ALOGV("App process: starting thread pool.\n");
    proc->startThreadPool();//启动Binder线程池
}
```
frameworks/base/core/java/com/android/internal/os/RuntimeInit.java
最终通过反射调用com.android.server.SystemServer的main方法
```java
return findStaticMain(args.startClass, args.startArgs, classLoader);//返回的时Runnable，回到最开始的r.run()会通过反射执行main方法

public void run() {
    省略...
    mMethod.invoke(null, new Object[] { mArgs });
    省略...
}
```
frameworks/base/services/java/com/android/server/SystemServer.java
```java
public static void main(String[] args) {
    new SystemServer().run();
}
```
调用run方法，重要代码如下
```java
private void run() {
    Looper.prepareMainLooper();
    System.loadLibrary("android_servers");//加载libandroid_servers.so
    createSystemContext();//初始化系统上下文
    mSystemServiceManager = new SystemServiceManager(mSystemContext);
    startBootstrapServices(t);
    startCoreServices(t);
    startOtherServices(t);
    startApexServices(t);
    Looper.loop();// looper死循环，保证进程不会退出
}
```