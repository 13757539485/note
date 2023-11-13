#### 表现

[原⼦性](#primitive)、[可⻅性](#possibility)、[有序性](#order)

#### 解决方向
1.不可变

final关键字修饰的类或数据不可修改，如String类，Integer类

2.线程封闭

Ad-hoc 线程封闭、ThreadLocal、堆栈封闭

3.同步

悲观锁和乐观锁(⾮阻塞同步)

悲观锁：synchronized、Lock(ReentrantReadWriteLock/ReentrantLock)、可见性

乐观锁：CAS、volatile、有序性、原子性

### CAS
乐观锁，比较与交换的无锁算法，非阻塞同步

#### 原理

1. CAS指令每次只允许一个线程执行成功，以i=0,i++为例

2. 线程1、线程2、线程3同时执行时，分别会在自己内存空间计算好i++的值，即为1

3. 线程1执行CAS指令，比较原i和自己计算的值如果不同则进行交换，即主内存i变成1

4. 线程2执行CAS指令发现i和自己的值相同，则重新从主内存中获取i并进行i++，该过程成为自旋，再执行CAS执行，比较和交换i变成2。线程3和线程2类似

缺点：循环时间长话开销大，ABA问题(数据中途被修改过)，不能保证多个共享变量的原子性

#### 原子操作类
更新基本类型类：AtomicBoolean、AtomicInteger、AtomicLong

更新数组类：AtomicIntegerArray、AtomicLongArray、AtomicReferenceArray

更新引用类型：AtomicReference、AtomicMarkableReference、AtomicStampedReference

AtomicMarkableReference和AtomicStampedReference用来解决ABA问题，通过版本控制前者是boolean类型，后者是int类型

### Synchronized
保证互斥的，⼀块代码不能同时被两个线程访问，能保证可见性、有序性、原子性

### Semaphore类
进⾏并发控制，限制访问⼀个资源（或代码块）的线程数⽬。当设定的线程数⽬是1时，并发其实就退化到了互斥

### Lock类
可以取代Object的notify和wait

### 对象锁
Synchronized+Object的Notify和wait⼀起构成了同步

### volatile
轻量级的synchronized，是⼀个变量修饰符，只能⽤来修饰变量，能保证缓存一致性

缓存⼀致性协议：每个处理器通过嗅探在总线上传播的数据来检查⾃⼰缓存的值是否过期，当处理器发现缓存⾏对应的内存地址被修改，就会将当前处理器的缓存⾏设置成⽆效状态，当处理器要对这个数据进⾏修改操作的时候，会强制重新从系统内存⾥把数据读到处理器缓存⾥

#### <a id="possibility">可⻅性</a>
指当多个线程访问同⼀个变量时，⼀个线程修改了这个变量的值，其他线程能够⽴即看得到修改的值

volatile保证可见性：被其修饰的变量在被修改后可以⽴即同步到主内存，被其修饰的变量在每次使⽤之前都从主内存刷新

#### <a id="order">有序性</a>
指程序执⾏的顺序按照代码的先后顺序执⾏（happens-before原则）

volatile保证有序性：禁⽌指令重排优化等，保证代码的程序会严格按照代码的先后顺序执⾏

#### <a id="primitive">原⼦性</a>
指⼀个操作是不可中断的，要全部执⾏完成，要不就都不执⾏，同一时刻只能有一个线程来对它进行操作

<font color="#dd0000">volatile无法保证原⼦性：</font>保证原⼦性需要通过字节码指令monitorenter和monitorexit，volatile和这两个指令之间是没有任何关系的

```java
private volatile int i = 0;
//如果是kotlin采用注解形式
@Volatile
private i: Int = 0

i++;
```

线程A和线程B同时修改i++时为啥会有问题？

线程A在i++时i变成1，会立即刷新主内存使得线程B的i值无效重写从主内存读取，按理不会出现异常

理解

1、线程读取i

2、temp = i + 1

3、i = temp

当 i=0 的时候A,B两个线程同时读入了 i 的值， 然后A线程执行了temp = i + 1的操作，然后B线程也执行了temp = i + 1的操作，此时A，B两个线程保存的 i 的值都是0，temp的值都是1，然后A线程执行了 i = temp的操作，此时i的值会立即刷新到主存并通知其他线程保存的 i 值失效，此时B线程需要重新读取 i 的值那么此时B线程保存的 i 就是1，同时B线程保存的temp还仍然是1，然后B线程执行 i=temp，所以导致了计算结果比预期少了1

使用场景：一写多读，一个线程写多个线程读

### 懒汉双重校验和volatile
[懒汉式](../design/design_single.md#lazy_instance)
既然synchronized保证了有序性那为啥还需要volatile?

synchronized只能保证让闭包里代码同一时间只有一个线程执行，当线程A执行INSTANCE = new Singleton()时，分为开辟空间创建对象、调用构造方法、赋值给INSTANCE三步，由于虚拟机指令重排机制导致后面两步可能顺序颠倒，所以当线程B走到if (INSTANCE == null) {时不为null
### synchronized vs volatile

|   | synchronized  | volatile  |
|  ----  | ----  | ----  |
| 使用  | 变量、方法、类、同步代码块等 | 变量 |
| 线程阻塞  | 可能会 | 不会 |
| 性能  | 有锁，略低 | 优于前者 |
| 保证  | 有序、可见、原子 | 有序、可见 |
| 原理  | 加锁(monitorenter指令)释放锁(monitorexit指令)保证原子性，内存屏障保证有序、可见性 | 内存屏障保证有序、可见性 |

### synchronized vs lock

|   | synchronized  | Lock接口  |
|  ----  | ----  | ----  |
| 使用  | 关键字 | 类 |
| 锁状态  | 不可判断 | 可判断 |
| 锁类型  | 可重入，不可中断，[非公平锁](#fairUnfair) | 可重入，可中断，[公/非公平锁](#fairUnfair) |

|存在层次 |volatile关键字 |synchronized关键字 |Lock接⼝ |Atomic变量|
|  ----  | ----  | ----  | ----  | ----  |
|[原⼦性](#primitive)| ⽆法保障 |可以保障 |可以保障 |可以保障|
|[可⻅性](#possibility)| 可以保障 |可以保障 |可以保障 |可以保障|
|[有序性](#order)| ⼀定程度保障 |可以保障 |可以保障 |⽆法保障|

4.工具类

同步容器（已过时） 同步容器的⼯具有Vector、HashTable、Collections.synchroniedXXX()

并发容器（JUC） ConcurrentHashMap、CopyOnWriteArrayList

JUC同步器 AQS

### 并发工具类
#### CountDownLatch
相对于join，更灵活的控制线程执行顺序，如线程123，按照321执行后最终main执行

原理是通过内部的计数扣除来控制，每次countDown()会减1
```kotlin
fun main() {
    val mainLatch = CountDownLatch(1)
    val latch1 = CountDownLatch(2)
    val th1 = Thread {
        latch1.await()
        println("${Thread.currentThread().name} run finish")
        mainLatch.countDown()
    }.apply { name = "thread1" }
    val th2 = Thread {
        println("${Thread.currentThread().name} run finish")
        latch1.countDown()
    }.apply { name = "thread2" }
    val th3 = Thread {
        println("${Thread.currentThread().name} run finish")
        latch1.countDown()
    }.apply { name = "thread3" }
    th1.start()
    th2.start()
    th3.start()
    mainLatch.await()
    println("main")
}
```
#### CyclicBarrier
用来多个线程汇总使用，可以多次调用await
```kotlin
fun main() {
    val cyclic = CyclicBarrier(4){
        println("result--------")
    }
    for (i in 0..3) {
        Thread(MyRun(cyclic)).start()
    }
}

class MyRun(private val cyclic: CyclicBarrier) : Runnable {
    override fun run() {
        println("${Thread.currentThread().name} is run")
        cyclic.await()
        println("${Thread.currentThread().name} is finish")
        cyclic.await()
    }
Thread-1 is run
Thread-0 is run
Thread-2 is run
Thread-3 is run
result--------
Thread-3 is finish
Thread-1 is finish
Thread-0 is finish
Thread-2 is finish
result--------
```

#### Semaphore
信号量，主要用于流控使用

### CountDownLatch vs CyclicBarrier
场景举例，Android项目：A模块，B模块，C模块

CyclicBarrier：当ABC模块完成后才能打包apk

CountDownLatch：当A模块完成才能继续BC模块

|   | CountDownLatch  | CyclicBarrier  |
|  ----  | ----  | ----  |
|  计数器和线程数  | 不相等，取决于countDown调用次数  | 相等  |
|  线程控制  | 由其他线程通过countDown控制  | 由本身控制，只有await  |
|  主线程  | 不阻塞  | 阻塞  |
|  重复使用  | 不可以  | 可以  |
|  计数方式  | 倒数计数  | 正数计数  |
|  实现  | 内部类Sync继承AQS实现  | 通过重入锁ReentrantLock实现  |

### transient关键字
使成员变量不被序列化，被static修饰的成员变量无效，需要实现Serializable接⼝，如果实现Externalizable接⼝则也无效，需要自行在readExternal方法里处理(手动设置null)

### 线程池
ThreadPoolExecutor

拒绝策略

ThreadPoolExecutor.DiscardPolicy：不会抛异常也不会执⾏

ThreadPoolExecutor.AbortPolicy：默认策略，拒绝后抛异常

ThreadPoolExecutor.CallerRunsPolicy：拒绝后直接执行run，可能会阻塞主线程

ThreadPoolExecutor.DiscardOldestPolicy：拒绝后会抛弃任务队列
中最旧的任务

自定义策略

实现RejectedExecutionHandler接口

实现方式

1. newSingleThreadExecutor：一个线程的线程池，存活时间无限，缓存到LinkedBlockingQueue，适合一个任务一个任务执行的场景
2. newCachedThreadPool：缓存到SynchronousQueue是同步队列，适合执行很多短期异步的场景
3. newFixedThreadPool: 创建可容纳固定数量的线程池，存活时间是无限，适合长期执行的场景
4. newScheduledThreadPool：创建固定大小的线程池，缓存到DelayedWorkQueue，适合周期性执行的场景

ThreadPoolExecutor的参数

1. corePoolSize：线程池中的常驻核心线程数
2. maximumPoolSize：线程池能够容纳同时执行的最大线程数，必须大于等于1
3. keepAliveTime：空闲线程的存活时间
4. unit：keepAliveTime的单位
5. workQueue：[阻塞队列](./java_base.md#block_queue)，被提交但尚未被执行的任务
6. threadFactory：生成线程池中工作线程池的线程工厂，用于创建线程一般默认即可
7. handler：拒绝策略，当队列满了并且工作线程大于等于线程池最大线程数maximumPoolSize时处理方式

corePollSize满了添加到workQueue中，满了再maximumPoolSize之内启动线程执行任务，再满拒绝策略handler

#### 合理配置

任务特性：

1. CPU密集型，maximumPoolSize不要超过机器的cpu核心数+1
```kotlin
//机器的cpu核心数获取
Runtime.getRuntime().availableProcessors()
```
2. IO密集型(磁盘、网络)，maximumPoolSize一般是机器的cpu核心数*2
3. 混合型