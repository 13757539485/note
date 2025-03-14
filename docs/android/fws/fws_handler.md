一般处理线程之间问题，进程之间一般是binder
源码分析基于Android13

### 线程间通信的原理
核心：线程间内存共享(MessageQueue共享)，基于链表结构的Message容器实现

### 内存泄露原因
完全理解前提：需要理解[JVM虚拟机原理](../java/java_jvm.md)

引用记数法
例如：对象Obj被变量a,b,c引用时，计数为3，当c断开引用计数变2，当计数为0时GC即可清理，常见于python虚拟机中

缺陷：相互引用时需要额外机制处理，因此主流虚拟机不使用

安卓中判断对象是否可以释放：引用链是否存在GcRoot，一旦存在无法释放对象

此处引用链:

static sMainLooper->Thread->ThreadLocal.ThreadLocalMap-->Entry(WeakReference)->Looper->MessageQueue->Message->Handler->XXActivity.this

判断GcRoot依据之一：static

解决方案：想办法断开引用链

1. Handler定义成静态+弱引用方式(断开Message和Handler之间的引用)
2. 手动移出Message方式(断开Messagequeue和Message之间的引用)

### ThreadLocal
static final类型，生命周期和app进程一致，用来隔离线程变量，此处用来隔离Looper对象，每个线程只能拥有自身独立的Looper对象，源码主要看set和get方法
```java
public void set(T value) {
    Thread t = Thread.currentThread();
    ThreadLocalMap map = getMap(t);
    if (map != null)
        map.set(this, value);
    else
        createMap(t, value);
}
void createMap(Thread t, T firstValue) {
    t.threadLocals = new ThreadLocalMap(this, firstValue);
}
ThreadLocalMap(ThreadLocal<?> firstKey, Object firstValue) {
    table = new Entry[INITIAL_CAPACITY];//16 Entry是继承WeakReference
    int i = firstKey.threadLocalHashCode & (INITIAL_CAPACITY - 1);
    table[i] = new Entry(firstKey, firstValue);
    size = 1;
    setThreshold(INITIAL_CAPACITY);
}
```
以当前线程作为key获得ThreadLocalMap对象，将value(此处时Looper对象)保存到map中，其中key为this即Threadlocal
```java
public T get() {
    Thread t = Thread.currentThread();
    ThreadLocalMap map = getMap(t);
    if (map != null) {
        ThreadLocalMap.Entry e = map.getEntry(this);
        if (e != null) {
            @SuppressWarnings("unchecked")
            T result = (T)e.value;
            return result;
        }
    }
    return setInitialValue();
}
```
同理get也是先通过当前线程获得map再通过this获取对应Looper对象

### 主线程为何能直接new Handler
因为在Activitythread中已经创建了Looper对像
```java
Looper.prepareMainLooper()
...
Looper.loop()
```
在new Handler时能获取到Looper对象
```java
mLooper = Looper.myLooper();
if (mLooper == null) {
    throw new RuntimeException(
        "Can't create handler inside thread " + Thread.currentThread()
                + " that has not called Looper.prepare()");
}
```
其中mylooper方法是通过ThreadLocal中取出Looper对象，key为线程Thread，value时Looper对象，因此主线程能获取到Looper
### 子线程创建Handler
方式一：
```kotlin
Thread {
    Looper.prepare()
    Handler(Looper.myLooper()!!) { msg: Message ->
        false
    }
    Looper.loop()
}.start()
```
方式二：(推荐)
```kotlin
val thread = HandlerThread("thread-")
thread.start()
val handler = Handler(thread.looper){msg: Message ->
    false
}
```

### HandlerThread
完全理解前提：需要学习[并发](../java/java_lock.md)相关知识点

封装了Looper和加入了并发处理

#### 源码解析
问题1：必须要先执行start吗？<font color="#dd0000">不需要</font>，start的作用是Thread调用start后会调用run方法，然后就执行力Looper.prepare和Looper.loop，从而可以通过getlooper()拿到looper对象给handler，并发原理先看问题2再看源码注释
```java
public void run() {
    ...
    Looper.prepare();
    synchronized (this) {
        mLooper = Looper.myLooper();
        notifyAll();// 为啥不是notify？因为getlooper可能被多个Handler使用时调用，需要全部通知
        // wait是立刻挂起和释放锁，notify是需要等synchronized包裹的所有代码执行完成才释放锁，所以不是说notify了wait就立刻执行，而是通知wait可以准备就绪
        // notifyAll代码可以调换顺序，只要synchronized包裹范围效果一致
    }
    ...
    Looper.loop();
    ...
}
public Looper getLooper() {
    ...
    synchronized (this) {
        while (isAlive() && mLooper == null) {// 为啥不是if？防止其他地方也使用并释放了this锁，而此时looper可能依然是null
            try {
                wait(); // 挂起，释放对象锁，防止万一此方法先执行拿到锁，需要先释放锁让run中拿到锁
                // sleep：挂起，但不释放锁
            } catch (InterruptedException e) {
                wasInterrupted = true;
            }
        }
    }
    ...    
    return mLooper;
}
```
问题2：为啥源码中需要用到synchronized同步锁机制
因为需要保证start后Handler(thread.looper)能确保拿到looper，Handler创建是在主线程中，而looper是在子线程中创建，不加锁可能会导致looper为null
synchronized：内置锁，功能：互斥访问，作用于方法，代码块
小案例：
```kotlin
class A {
    fun method1() { synchronized(this){} }
    fun method2() { synchronized(this){} }
}
val a = A()
val b = A()
a.method1()
a.method2()
b.method1()
```
a.method1()和a.method2()是互斥，不能同时调用，锁是this即A对象

a.method1()和b.method1()不是互斥，锁是this，分别是A和B对象

问题3：如果不调用start会如何？<font color="#dd0000">会报空指针异常</font>
```java
public Looper getLooper() {
    if (!isAlive()) {
        return null;
    }
    ...
}
```
isAlive用来判断线程是否已经start
```java
public Handler(@NonNull Looper looper, @Nullable Callback callback, boolean async) {
    mLooper = looper;
    mQueue = looper.mQueue;
    mCallback = callback;
    mAsynchronous = async;
}
```
由于getlooper返回null，所以再创建Handler时会报空指针
如果没有isAlive()处理会如何？主线程会挂起，因为执行了wait，当前线程没有start，调用者是主线程，所以会将主线程wait挂起

<font color="#dd0000">注：wait会释放锁，CPU，资源，但会挂起当前线程；sleep既不释放锁CPU资源也会挂起</font>

### MessageQueue中添加数据时，各个Handler可能处于不同线程，如何处理
添加数据enqueueMessage()：各个子线程

取出数据next()：主线程

都使用了synchronized(this)，前者锁可以保证<font color="#dd0000">各个子线程</font>添加数据<font color="#dd0000">线程安全</font>，后者用来保证数据<font color="#dd0000">添加和取出线程安全</font>

### Message对象获取方式，是否可以直接new
不推荐直接new，使用Handler.obtainMessage方式

原因：在GC回收对象时存在stw机制让所有线程短暂停一下，如果stw过于频繁会导致卡顿。而obtainMessage方式优化内存抖动问题

设计模式：[享元设计模式](../design/design_enyuan.md#handler)

### Looper死循环为啥不会ANR
```java
for (;;) {
    if (!loopOnce(me, ident, thresholdOverride)) {
        return;
    }
}
```
需要理解[ANR产生原理](../performance/android_anr.md)

### epoll
用来管理IO阻塞的机制

场景：有n个IO事件需要同时处理
```kotlin
while(true){
    //select(stream[])
    for(i in stream[]){
        xxx    
    }
}
```
缺陷：如果流里面没有数据时，会浪费CPU资源

添加select后，当流里面有数据需要处理时会唤醒CPU执行for循环

缺陷：无法得知那些流需要处理

为了解决这个问题epoll登场
```kotlin
while(true){
    active_stream[] = epoll_wait()
    for(i in active_stream[]){
        xxx    
    }
}
```
当有流需要处理数据时将其存到stream中，大大减少时间复杂度O(n)->O(1)

#### Android源码中应用
```java
Message next() {
    ...
    int nextPollTimeoutMillis = 0;
    for (;;) {
        ...
        nativePollOnce(ptr, nextPollTimeoutMillis);
        synchronized (this) {
            final long now = SystemClock.uptimeMillis();
            Message prevMsg = null;
            Message msg = mMessages;
            ...
            if (msg != null) {
                if (now < msg.when) {
                    nextPollTimeoutMillis = (int) Math.min(msg.when - now, Integer.MAX_VALUE);
                } else {
                    ...
                }
            } else {
                nextPollTimeoutMillis = -1;
            }
           ...
        }
        ...
        nextPollTimeoutMillis = 0;
    }
}
```
nextPollTimeoutMillis: 数值为0时会立即返回，-1不确定，具体时间即等待计时后返回

唤醒在enqueueMessage方法中nativeWake

### <a id="handler_barrier">消息屏障</a>
<font color="#dd0000">作用：为UI刷新“开绿灯”，提高优先级</font>

消息分为：消息屏障，异步消息，同步消息

消息屏障：target为null，postSyncBarrier(ViewRootImpl刷新UI时调用)

消息屏障和异步消息肯定成对出现，因为只能framework调用且目前只在ViewRootimpl中使用
```java
void scheduleTraversals() {
    if (!mTraversalScheduled) {
        mTraversalScheduled = true;
        mTraversalBarrier = mHandler.getLooper().getQueue().postSyncBarrier();
        mChoreographer.postCallback(
                Choreographer.CALLBACK_TRAVERSAL, mTraversalRunnable, null);
        //当执行mTraversalRunnable时run中会移除消息屏障
        //mHandler.getLooper().getQueue().removeSyncBarrier(mTraversalBarrier);
        notifyRendererOfFramePending();
        pokeDrawLockIfNeeded();
    }
}
@hide//仅限系统调用
public int postSyncBarrier() {
    return postSyncBarrier(SystemClock.uptimeMillis());
}
private int postSyncBarrier(long when) {
    synchronized (this) {
        ...
        final Message msg = Message.obtain();
        ...
        Message p = mMessages;//mMessages变量保存之前的消息
        ...
            msg.next = p;//放到消息屏障msg后面
            mMessages = msg;//mMessages变量赋值成消息屏障msg
        ...
        return token;
    }
}
//mChoreographer.postCallback最终调用
private void postCallbackDelayedInternal(int callbackType,
        Object action, Object token, long delayMillis) {
      ...
      scheduleFrameLocked(now);
      ...
    }
}
private void scheduleFrameLocked(long now) {
    if (!mFrameScheduled) {
        mFrameScheduled = true;
        if (USE_VSYNC) { // 4.1系统后默认为true
            ...
            scheduleVsyncLocked();
            ...
        } else {
            ...
        }
    }
}
scheduleVsyncLocked调用native最终调用FrameDisplayEventReceiver(Runnable)的onVsync
public void onVsync(long timestampNanos, long physicalDisplayId, int frame,
        VsyncEventData vsyncEventData) {
        ....
        Message msg = Message.obtain(mHandler, this);
        msg.setAsynchronous(true);//异步消息
        mHandler.sendMessageAtTime(msg, timestampNanos / TimeUtils.NANOS_PER_MS);
        ...
}
```
消息屏障会添加到Message最开始处，然后mChoreographer的msg放在其后

<font color="#dd0000">消息屏障添加到头部达到保证UI刷新优先级，在next方法中</font>
```java
Message prevMsg = null;
Message msg = mMessages;
if (msg != null && msg.target == null) { // 消息屏障target未设置因此为null
    // Stalled by a barrier.  Find the next asynchronous message in the queue.
    do {
        prevMsg = msg;
        msg = msg.next;
    } while (msg != null && !msg.isAsynchronous());// Choreographer发送的消息设置为true
}
```
<font color="#dd0000">注：消息屏障只能尽可能保证UI刷新优先，所以会存在以下问题</font>
常见引发的问题(log在Choreographer中执行run方法后调用doFrame)
Skipped xx frames!  The application may be doing too much work on its main thread
原因：刚好在Choreographer之前存在其他异步消息(isAsynchronous=true)，导致while循环遍历耗时或者异步消息处理耗时

### Idlehandler
特殊的Message，优先级：异步Msg>同步Msg>idlehandler

执行前提：没有Msg需要处理

使用场景：线程空闲的时候

Activity启动生命周期中比较耗时的操作(案例在ActivityThread中的handleResumeActivity最后)