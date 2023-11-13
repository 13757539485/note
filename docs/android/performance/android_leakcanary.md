由Square公司开发

https://github.com/square/leakcanary

接入方式：
```gradle
debugImplementation 'com.squareup.leakcanary:leakcanary-android:2.12'
```
[依赖版本获取](https://square.github.io/leakcanary/getting_started/)

新版本无需在application中初始化，采用ContentProvider形式自动初始化

发生内存泄漏后通知栏显示，log通过tag(LeakCanary)查看信息包括dump文件地址

监听Activity：从Activity源码入手dispatchActivityCreated会调用到Application中的callback

监听Fragment：从FragmentManager源码入手dispatchOnFragmentCreated

Fragment的本质是View

LeakCanary实现原理就是利用registerActivityLifecycleCallbacks监听Activity，然后利用registerFragmentLifecycleCallbacks监听Fragment

检测Activity是否被回收，可以把Activity对象放到弱引用中，手动触发gc后判断Activity对象是否为null

ReferenceQueue用来保存所有弱引用对象

分析dump使用haha库或者shark库

不能使用线上原因

1. 频繁gc容易导致卡顿
2. 每次检测到泄漏就会dump快照文件，同一个也会触发
3. dump出的hprof(内存镜像)文件耗时，容易造成程序未响应
4. hprof文件太大，解析耗时

线上使用[KOOM](./android_koom.md)