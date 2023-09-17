### 线程
生命周期：新建，运⾏，阻塞，等待，计时等待、终⽌
```java
class ThreadState implements Runnable {

    public synchronized void waitForASecond() throws InterruptedException {
        wait(500);
    }
    public synchronized void waitForLong() throws InterruptedException {
        wait();
    }
    public synchronized void notifyNow() throws InterruptedException {
        notify();
    }
    @Override
    public void run() {
        try {
            waitForASecond();
            waitForLong();
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }
}

public static void main(String[] args) throws InterruptedException {
        ThreadState state = new ThreadState();
        Thread thread = new Thread(state);
        System.out.println("新建线程:"+thread.getState());
        thread.start();
        System.out.println("启动线程:"+thread.getState());
        Thread.sleep(100);//运⾏waitForASecond()⽅法
        System.out.println("计时等待:"+thread.getState());
        Thread.sleep(1000);//运⾏waitForLong()⽅法
        System.out.println("等待线程:"+thread.getState());
        state.notifyNow();
        System.out.println("唤醒线程:"+thread.getState());
        Thread.sleep(1000);//当前线程休眠,时新线程结束
        System.out.println("终⽌线程:"+thread.getState());
    }

新建线程:NEW
启动线程:RUNNABLE
计时等待:TIMED_WAITING
等待线程:WAITING
唤醒线程:BLOCKED
终⽌线程:TERMINATED
```

#### 线程安全
##### 表现

[原⼦性](#原⼦性)、[可⻅性](#可⻅性)、[有序性](#有序性)

##### 解决方向
1.不可变

final关键字修饰的类或数据不可修改，如String类，Integer类

2.线程封闭

Ad-hoc 线程封闭、ThreadLocal、堆栈封闭

3.同步

悲观锁和乐观锁(⾮阻塞同步)

悲观锁：synchronized、Lock(ReentrantReadWriteLock/ReentrantLock)、可见性

乐观锁：CAS、volatile、有序性、原子性

#### Synchronized
保证互斥的，⼀块代码不能同时被两个线程访问，能保证可见性、有序性、原子性

#### Semaphore类
进⾏并发控制，限制访问⼀个资源（或代码块）的线程数⽬。当设定的线程数⽬是1时，并发其实就退化到了互斥

#### Lock类
可以取代Object的notify和wait

#### 对象锁
Synchronized+Object的Notify和wait⼀起构成了同步

#### volatile
轻量级的synchronized，是⼀个变量修饰符，只能⽤来修饰变量，能保证缓存一致性

缓存⼀致性协议：每个处理器通过嗅探在总线上传播的数据来检查⾃⼰缓存的值是否过期，当处理器发现缓存⾏对应的内存地址被修改，就会将当前处理器的缓存⾏设置成⽆效状态，当处理器要对这个数据进⾏修改操作的时候，会强制重新从系统内存⾥把数据读到处理器缓存⾥

##### 可⻅性
指当多个线程访问同⼀个变量时，⼀个线程修改了这个变量的值，其他线程能够⽴即看得到修改的值

volatile保证可见性：被其修饰的变量在被修改后可以⽴即同步到主内存，被其修饰的变量在每次使⽤之前都从主内存刷新

##### 有序性
指程序执⾏的顺序按照代码的先后顺序执⾏（happens-before原则）

volatile保证有序性：禁⽌指令重排优化等，保证代码的程序会严格按照代码的先后顺序执⾏

##### 原⼦性
指⼀个操作是不可中断的，要全部执⾏完成，要不就都不执⾏，同一时刻只能有一个线程来对它进行操作

<font color="#dd0000">volatile无法保证原⼦性：</font>保证原⼦性需要通过字节码指令monitorenter和monitorexit，volatile和这两个指令之间是没有任何关系的

```java
private volatile int i = 0;
i++;
```
线程A和线程B同时修改i++时为啥会有问题？

线程A在i++时i变成1，会立即刷新主内存使得线程B的i值无效重写从主内存读取，按理不会出现异常

理解

1、线程读取i

2、temp = i + 1

3、i = temp

当 i=0 的时候A,B两个线程同时读入了 i 的值， 然后A线程执行了temp = i + 1的操作，然后B线程也执行了temp = i + 1的操作，此时A，B两个线程保存的 i 的值都是0，temp的值都是1，然后A线程执行了 i = temp的操作，此时i的值会立即刷新到主存并通知其他线程保存的 i 值失效，此时B线程需要重新读取 i 的值那么此时B线程保存的 i 就是1，同时B线程保存的temp还仍然是1，然后B线程执行 i=temp，所以导致了计算结果比预期少了1

#### 懒汉双重校验和volatile
[懒汉式](../design/design_single.md#懒汉式双重校验)
既然synchronized保证了有序性那为啥还需要volatile?

synchronized只能保证让闭包里代码同一时间只有一个线程执行，当线程A执行INSTANCE = new Singleton()时，分为开辟空间创建对象、调用构造方法、赋值给INSTANCE三步，由于虚拟机指令重排机制导致后面两步可能顺序颠倒，所以当线程B走到if (INSTANCE == null) {时不为null
#### synchronized vs volatile

|   | synchronized  | volatile  |
|  ----  | ----  | ----  |
| 使用  | 变量、方法、类、同步代码块等 | 变量 |
| 线程阻塞  | 可能会 | 不会 |
| 性能  | 有锁，略低 | 优于前者 |
| 保证  | 有序、可见、原子 | 有序、可见 |
| 原理  | 加锁(monitorenter指令)释放锁(monitorexit指令)保证原子性，内存屏障保证有序、可见性 | 内存屏障保证有序、可见性 |

#### synchronized vs lock

|   | synchronized  | Lock接口  |
|  ----  | ----  | ----  |
| 使用  | 关键字 | 类 |
| 锁状态  | 不可判断 | 可判断 |
| 锁类型  | 可重入，不可中断，[非公平锁](#公平锁和非公平锁) | 可重入，可中断，[公/非公平锁](#公平锁和非公平锁) |

|存在层次 |volatile关键字 |synchronized关键字 |Lock接⼝ |Atomic变量|
|  ----  | ----  | ----  | ----  | ----  |
|[原⼦性](#原⼦性)| ⽆法保障 |可以保障 |可以保障 |可以保障|
|[可⻅性](#可⻅性)| 可以保障 |可以保障 |可以保障 |可以保障|
|[有序性](#有序性)| ⼀定程度保障 |可以保障 |可以保障 |⽆法保障|

4.工具类

同步容器（已过时） 同步容器的⼯具有Vector、HashTable、Collections.synchroniedXXX()

并发容器（JUC） ConcurrentHashMap、CopyOnWriteArrayList

JUC同步器 AQS

#### Sleep
Thread.Sleep(0)的作⽤，触发操作系统⽴刻重新进⾏⼀次CPU竞争

#### sleep和wait的区别
1、sleep是Thread的静态⽅法，wait是Object的⽅法，任何对象实例都能调⽤。

2、sleep不会释放锁，它也不需要占⽤锁。wait会释放锁，但调⽤它的前提是当前线程占有锁(即代码要在synchronized中)。

3、它们都可以被interrupt⽅法中断

|   | sleep  | wait  |
|  ----  | ----  | ----  |
| 同步  | 无限制 | synchronized中使用 |
| 作用对象  | 定义在Thread中，作用域当前线程 | 定义在Object中，作用于本身 |
| 释放锁  | 否 | 是 |
| 唤醒条件  | 超时或调用interrupt方法 | 其他线程调用notify或notifyAll |
| 方法属性  | 实例方法 | 静态⽅法 |

#### 公平锁和非公平锁
|   | 公平锁  | 非公平锁  |
|  ----  | ----  | ----  |
| 定义  | 多个线程排队申请锁 | 多个线程都尝试获得锁，先占先得 |
| 优点  | 所有线程都能获得锁 | 减少cpu唤醒开销，吞吐效率变高 |
| 缺点  | 除了第一个线程，后续线程会阻塞导致增加cpu唤醒开销变大，吞吐效率变低 | 有可能导致某些线程一直无法获得锁 |

ReentrantLock默认是非公平锁，ReentrantLock(true)为公平锁，分别对应NonfairSync和FairSync

#### 并发工具类
CountDownLatch、CyclicBarrier

### transient关键字
是成员变量不被序列化，被static修饰的成员变量无效，需要实现Serializable接⼝，如果实现Externalizable接⼝则也无效，需要自行在readExternal方法里处理(手动设置null)

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

### ThreadLocal

线程级别变量，并发模式下是绝对安全的变量

### 同步、异步、阻塞、非阻塞

同步和异步、阻塞和非阻塞本质上是一对相对的概念。

在进程通信这个层面，同步和异步针对的是发送方而言，取决于将数据写到内核缓冲区进程的行为，继续等待发送则为同步，反之即为异步。

在进程通信这个层面，阻塞非阻塞针对的是接收方而言，取决于将内核中的数据能否立即拷贝到用户空间，如果不能直接拷贝则为阻塞，反之则为非阻塞

同步异步是输入到结果需不需要等待决定，阻塞和非阻塞是是一种状态由的等待结果时是否可以做其他事情决定

### interrupted和isInterrupted⽅法的区别
```java
public static boolean interrupted() {
    Thread t = currentThread();
    boolean interrupted = t.interrupted;
    if (interrupted) {
        t.interrupted = false;
        clearInterruptEvent();
    }

    return interrupted;
}
public boolean isInterrupted() {
    return this.interrupted;
}
```
interrupted是静态方法，用来中断当前线程

isInterrupted是实例方法，用来判断线程是否中端