
由快手团队开发

https://github.com/KwaiAppTeam/KOOM/blob/master/README.zh-CN.md

### 基本使用
```gradle
implementation "com.kuaishou.koom:koom-java-leak:2.2.1"
```

application中初始化
```kotlin
OOMMonitor.init(CommonConfig.Builder().build(),
    OOMMonitorConfig.Builder().setHprofUploader(object : OOMHprofUploader {
        override fun upload(file: File, type: OOMHprofUploader.HprofType) {
            //上传hprof快照
        }
    }).setReportUploader(object : OOMReportUploader {
        override fun upload(file: File, content: String) {
            //上传json泄漏报告
        }
    })
        .build()
)
OOMMonitor.startLoop()
```

### 源码分析
startLoop执行super.startLoop，接着执行LoopMonitor里面的mLoopRunnable，里面首先执行call函数，实现在OOMMonitor中执行trackOOM

trackOOM会先从mOOMTrackers个维度进行检测
```kotlin
private val mOOMTrackers = mutableListOf(
    HeapOOMTracker(), ThreadOOMTracker(), FdOOMTracker(),
    PhysicalMemoryOOMTracker(), FastHugeMemoryOOMTracker()
)
```
其中PhysicalMemoryOOMTracker只是打印一些日志

检测函数track返回为true则添加到mTrackReasons中

KOOM通过周期轮询检测，通过HandlerThread检测以下维度

1. 内存占用率检测HeapOOMTracker，检测内存占用比例超过阈值(根据最大内存算出(0.8,0.85,0.9)或手动设置进来)时且超过上次比例-0.05，记录次数+1，当超过3次触发
2. 线程数检测ThreadOOMTracker，检测线程数量超过阈值(EMUI且版本小于O是450，其他750或手动设置进来)时且超过上次数量-50，记录次数+1，当超过3次触发
3. 文件描述符检测FdOOMTracker，检测打开文件数量超过阈值(默认1000或手动设置进来)时且超过上次数量-50，记录次数+1，当超过3次触发
4. 内存增长检测FastHugeMemoryOOMTracker，检测内存占用比例超过0.9触发，检测到当前内存-上次内存大于350m(增长过快)触发

在dumpAndAnalysis之前会进行触发判断

isExceedAnalysisPeriod() || isExceedAnalysisTimes()

超过15天触发或者超过5次就不触发

接下来进行dumpAndAnalysis主要是dump出hprof以及分析

dump函数：suspendAndFork解决多线程fork问题、Debug.dumpHprofData、resumeAndWait

### KOOM监控

KOOM监控整体内存，而不是监控对象，减少dump频率

### 解决dump慢

是否可以在子线程处理，可以，但是dump会触发stw，stw是停止一切线程(具体代码在art/runtime/thread_list.cc的ScopeSuspendAll中)，因此没啥效果

解决方案：fork一份进程进行dump

#### fork
fork系统调用用于创建子进程，会返回多次，返回值位整型，负值表示创建子进程失败，0返回新建的子进程(即当前处于子进程)，正数返回新进程id(即当前处于父进程)

子进程和父进程是共享同一份内存(read-only)，通过copy-on-write进行写数据(写时复制内存相同空间)

#### fork存在问题
由于java天生是多线程，主进程a线程调用fork，则子进程只复制a线程，对于其他线程b,c,d只复制线程中对象(即Thread对象)，此时stw无法停止这些线程导致卡死(死锁)

解决方案：fork之前先触发SuspendAll，等fork完后恢复ResumeAll

koom加载koom-fast-dump.so去处理，以AndroidR版本SuspendAndFork为例
```cpp
//反射调用libart.so(suspend相关操作编译产物)中的函数
void *handle = kwai::linker::DlFcn::dlopen("libart.so", RTLD_NOW);
//art::Dbg::SuspendVM和art::Dbg::ResumeVM
suspend_vm_fnc_ =
        (void (*)())DlFcn::dlsym(handle, "_ZN3art3Dbg9SuspendVMEv");
    KFINISHV_FNC(suspend_vm_fnc_, DlFcn::dlclose, handle)

pid_t HprofDump::SuspendAndFork() {
  KCHECKI(init_done_)
  if (android_api_ < __ANDROID_API_R__) {
    suspend_vm_fnc_();
  } else if (android_api_ <= __ANDROID_API_S__) {
    //...
  }
//再进行fork
  pid_t pid = fork();
  if (pid == 0) {
    alarm(60);
    prctl(PR_SET_NAME, "forked-dump-process");
  }
  return pid;
}
```
其中_ZN3art3Dbg9SuspendVMEv是c++符号化表现，通过命令查看符号名(正真函数名)
```
run -D libart.so >xx.txt
```

除了JVMIT以外，通过反射hide注解限制也使用到反射so中的函数，需要掌握so的格式和数据格式分析

[JVMTI](android_jvmti.md)：控制JVM行为，可反射调用so中的函数

### hprof

hprof是基于JVMTI实现的内存分析器代理，记录java的内存镜像包括堆详细使用信息，包含许多无用信息(时间、版本、标签等)

#### hprof压缩
android.os.Debug.dumpHprofData肯定是通过IO写到文件中，则可以进行hprof裁剪，通过hook IO的write函数来提取需要(无用信息抛弃掉)的buffer写到hprof文件即可减少hprof大小