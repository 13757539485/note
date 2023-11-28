### Java方面
#### Java基础部分

抽象类与接口的区别？

分别讲讲 final，static，synchronized

请简述一下String、StringBuffer和StringBuilder

“equals”与“==”、“hashCode”的区别和使用场景？

Java 中深拷贝与浅拷贝的区别？

谈谈Error和Exception的区别？

什么是反射机制？反射机制的应用场景有哪些？

谈谈如何重写equals()方法？为什么还要重写hashCode()？

谈谈你对Java泛型中类型擦除的理解，并说说其局限性？ -String为什么要设计成不可变的？

说说你对Java注解的理解？
#### Java集合

谈谈List,Set,Map的区别？

谈谈ArrayList和LinkedList的区别？

请说一下HashMap与HashTable的区别

谈一谈ArrayList的扩容机制？

HashMap 的实现原理？

请简述 LinkedHashMap 的工作原理和使用方式？

谈谈对于ConcurrentHashMap的理解?
#### Java多线程

Java 中使用多线程的方式有哪些？

说一下线程的几种状态？

如何实现多线程中的同步？

谈谈线程死锁，如何有效的避免线程死锁？

谈谈线程阻塞的原因？

请谈谈 Thread 中 run() 与 start() 的区别？

synchronized和volatile关键字的区别？

如何保证线程安全？

谈谈ThreadLocal用法和原理？

java线程中notify 和 notifyAll有什么区别？

什么是线程池？如何创建一个线程池？

谈一谈java线程常见的几种锁？

谈一谈线程sleep()和wait()的区别？
#### Java虚拟机

谈一谈JAVA垃圾回收机制？

回答一下什么是强、软、弱、虚引用以及它们之间的区别？

简述JVM中类的加载机制与加载过程？

JVM、Dalvik、ART三者的原理和区别？

请谈谈Java的内存回收机制？

JMM是什么？它存在哪些问题？该如何解决？

### Android方面
#### 四大组件

Activity 与 Fragment 之间常见的几种通信方式？

LaunchMode 的应用场景？

对于 Context，你了解多少?

IntentFilter是什么？有哪些使用场景？

谈一谈startService和bindService的区别，生命周期以及使用场景？

Service如何进行保活？

简单介绍下ContentProvider是如何实现数据共享的？

说下切换横竖屏时Activity的生命周期?

Intent传输数据的大小有限制吗？如何解决？
#### Android 异步任务和消息机制

HandlerThread 的使用场景和用法？

IntentService 的应用场景和使用姿势？

AsyncTask的优点和缺点？

谈谈你对 Activity.runOnUiThread 的理解？

子线程能否更新UI？为什么？

谈谈 Handler 机制和原理？

为什么在子线程中创建Handler会抛异常？

Handler中有Loop死循环，为什么没有阻塞主线程，原理是什么?
#### 数据结构

什么是冒泡排序？如何优化？

请用 Java 实现一个简单的单链表？

如何反转一个单链表？

谈谈你对时间复杂度和空间复杂度的理解？

谈一谈如何判断一个链表成环？

什么是红黑树？为什么要用红黑树？

什么是快速排序？如何优化？

说说循环队列？

如何判断单链表交叉

#### Binder

Binder有什么优势

Binder是如何做到一次拷贝的

MMAP的内存映射原理了解吗

Binder机制是如何跨进程的

说说四大组件的通信机制

为什么Intent不能传递大数据

#### Handler

HandlerThread是什么？为什么它会存在？

简述下 Handler 机制的总体原理？

Looper 存在哪？如何可以保证线程独有？

如何理解 ThreadLocal 的作用？

主线程 Main Looper 和一般 Looper 的异同？

Handler 或者说 Looper 如何切换线程？

Looper 的 loop() 死循环为什么不卡死？

Looper 的等待是如何能够准确唤醒的？

Message 如何获取？为什么这么设计？
#### AMS

ActivityManagerService是什么？什么时候初始化的？有什么作用？

ActivityThread是什么?ApplicationThread是什么?他们的区别

Instrumentation是什么？和ActivityThread是什么关系？

ActivityManagerService和zygote进程通信是如何实现的。

手写实现简化版AMS

### 算法方面
如何运⽤⼆分查找算法

如何⾼效解决接⾬⽔问题

⼆分查找⾼效判定⼦序列

如何去除有序数组的重复元素

如何寻找最⻓回⽂⼦串

如何⾼效进⾏模幂运算

如何运用贪心思想广域玩跳跃游戏

如何⾼效判断回⽂链表

如何在无线序列中随机抽取元素

如何判定括号合法性

如何寻找缺失和重复的元素

请说一说HashMap，SparseArrary原理，SparseArrary相比HashMap的优点、ConcurrentHashMap如何实现线程安全？

请说一说HashMap原理，存取过程，为什么用红黑树，红黑树与完全二叉树对比，HashTab、concurrentHashMap，concurrent包里有啥?

请说一说hashmap put()底层原理,发生冲突时，如何去添加(顺着链表去遍历，挨个比较key值是否一致，如果一致，就覆盖替换，不一致遍历结束后，插入该位置) ？

### Kotlin方面
请简述一下什么是 Kotlin？它有哪些特性？

Kotlin中实现单例的几种常见方式？

在Kotlin中，什么是内联函数？有什么作用？

请谈谈Kotlin中的Coroutines，它与线程有什么区别？有哪些优点？

说说Kotlin中的Any与Java中的Object 有何异同？

Kotlin中的数据类型有隐式转换吗？为什么？

Kotlin中集合遍历有哪几种方式

Kotlin内置标准函数let的原理是什么？

Kotlin语言的run高阶函数的原理是什么？

### 音视频方面
怎么做到直播秒开优化？

数字图像滤波有哪些方法？

图像可以提取的特征有哪些？

FFMPEG:图片如何合成视频

常见的音视频格式有哪些？

请叙述MPEG视频基本码流结构？

说一说ffffmpeg的数据结构？

如何降低延迟？如何保证流畅性？如何解决卡顿？解决网络抖动？

平时说的软解和硬解，具体是什么？

### Flutter方面
Dart 语言的特性？

Dart 多任务如何并行的？

dart是值传递还是引用传递？

Flutter 特性有哪些？

Widget 和 element 和 RenderObject 之间的关系？

使用mixins的条件是什么？

Stream 两种订阅模式？

Flutter中的Widget、State、Context 的核心概念？是为了解决什么问题？

说一下Hot Reload，Hot Restart，热更新三者的区别和原理

Flutter 如何与 Android iOS 通信？

说一下什么是状态管理，为什么需要它？