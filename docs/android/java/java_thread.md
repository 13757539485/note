### 生命周期

新建，运⾏，阻塞，等待，计时等待、终⽌

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

### 启动线程的方式
两种方式，Thread源码中明确指出

方式一：
```kotlin
class PrimeThread(private val minPrime: Long) : Thread() {
    override fun run() {
        println(minPrime)
    }
}
PrimeThread(1000L).start()
```

方式二：
```kotlin
class PrimeRun(private val minPrime: Long) : Runnable {
    override fun run() {
        println(minPrime)
    }
}
Thread(PrimeRun(1000L)).start()
```

Thread和Runnable的区别：Thread是线程的唯一抽象，Runnable只是业务逻辑的抽象

### 停止线程
API中的suspend、resume、stop不建议使用(已被标记过时)，容易引发死锁，原因是线程不会释放中有的资源如锁，并且不会给予释放资源的机会

线程工作是协作式，即只是通知

isInterrupted：判断线程是否被中断

interrupt()：实例方法用来打断线程，只是发出中断请求，并不会立即停止线程

interrupted()：static方法，判断线程是否被中断

```kotlin
fun main() {
    val thread = PrimeThread()
    thread.start()
    Thread.sleep(20)
    thread.interrupt()
}

class PrimeThread : Thread() {
    override fun run() {
        val threadName = currentThread().name
        println("$threadName, status: $isInterrupted")
        while (!isInterrupted) {
    //while (!interrupted()) {
            println("isInterrupted: $isInterrupted")//false
        }
        println("end isInterrupted: $isInterrupted")//true
    }
}
```
调用interrupted()区别是会将isInterrupted设置成false

#### interrupted和isInterrupted⽅法的区别
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
sleep、join方法会抛出InterruptedException，抛出后会将isInterrupted设置成true，开发者可以通过捕获异常来重新中断

### start和run方法
run：普通方法

start：正真意义的启动线程的方法

```kotlin
fun main() {
    val thread = PrimeThread()
    thread.name = "PrimeThread"
    thread.run()//thread: main
    thread.start()//thread: PrimeThread
}

class PrimeThread : Thread() {
    override fun run() {
        println("thread: ${currentThread().name}")
    }
}
```
### join方法
保证两个线程顺序执行
```kotlin
fun main() {
    val run1 = PrimeRun1()
    val th1 = Thread(run1)
    val run = PrimeRun(th1)
    val th = Thread(run)
    th.start()
    th1.start()
}

class PrimeRun(private var thread: Thread?) : Runnable  {
    override fun run() {
        thread?.join()
        Thread.sleep(2)
        println("run")
    }
}
class PrimeRun1: Runnable  {
    override fun run() {
        Thread.sleep(2)
        println("run1")
    }
}

输出结果：
run1
run
```
如果没有执行join方法，按照代码顺序是th先执行，th1后执行

### 线程优先级
priority：不能保证线程优先执行，了解即可

### 守护线程
java天然是多线程，除了main都是守护线程
```kotlin
val threadMXBean = ManagementFactory.getThreadMXBean()
threadMXBean.dumpAllThreads(false, false).forEach {
    println("[${it.threadId}] ${it.threadName} ${it.threadState}")
}

[1] main RUNNABLE
[2] Reference Handler RUNNABLE
[3] Finalizer WAITING
[4] Signal Dispatcher RUNNABLE
[5] Attach Listener RUNNABLE
[21] Common-Cleaner TIMED_WAITING
[22] Monitor Ctrl-Break RUNNABLE
[23] Notification Thread RUNNABLE
```
默认用户线程都是非守护线程，主线程运行完成后，用户线程依然会执行
```kotlin
fun main() {
    val th = Thread(PrimeRun())
    th.start()
    Thread.sleep(2)
    println("main end")
}

class PrimeRun : Runnable {
    override fun run() {
        var i = 5
        while (i > 0) {
            println("run")
            Thread.sleep(2)
            i--
        }
    }
}
输出结果：
run
main end
run
run
run
run
```
用户线程转成守护进程
```kotlin
th.isDaemon = true
输出结果：
run
run
main end
```
<font color="#dd0000">注：finally在守护进程中不一定会执行</font>