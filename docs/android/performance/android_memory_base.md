### Android虚拟机
#### 基本概念
- .dex文件：App所有java源代码编译后生成众多class文件，由DX/D8，编译为一个/多个（multiDex）dex文件，由Android虚拟机编译执行
- odex文件：dex文件经过验证和优化后的产物，art下的odex文件包含经过AOT编译后的代码以及dex的完整内容，但Android8.0之后odex中的dex内容移动到了.vdex文件
- .art文件：art下根据配置文件生成odex文件时同时生成.art文件，主要是为了提升运行时加载odex中热点代码的速度，包含了类信息和odex中热点方法的索引，运行App时会首先根据这个文件来加载odex中已经编译过的代码
- 解释器（Interpreter）：用于程序运行时对代码进行逐行解释，翻译成对应平台的机器码执行；
- JIT编译（Just In Time）：由于解释器方式运行太慢引入，对于频繁运行的热点代码（判定标准一般是在某个时间段内执行次数达到某个阈值）进行实时编译（在ART下以方法为粒度）执行，并且缓存JIT编译后的代码在内存中用于下次执行。由于以方法为粒度（ArtMethod）进行编译，JIT编较于解释器可以生成效率更高的代码，运行更快；
- AOT编译（Ahead-Of-Time）：应用安装时全量编译所有代码为本地机器码，运行时直接执行机器码；

Dalvik内存区域块

Linear Alloc：匿名共享内存，只读区域

Zygote Space：Zygote进程和应用进程之间共享，对象在启动时会重新创建

Alloc Space：每个进程独占，分配对象区域

Art内存区域块

Non Moving Space

Zygote Space

Alloc Space

Image Space：预加载的类信息，Zygote进程和应用进程之间共享

Large Obj Space：分配大对象，如bitmap，此处对象只创建一次

运行时堆都分为前3个

| |Dalvik|Art|
|--|--|--|
回收算法|标记-清除算法|CMS？|
|GC暂停|两次|一次|

- 4.4被引入，5.0开始替换Dalvik虚拟机，7.0和8.0分别进行一系列改动
- 4.4~7.0 ART只采用AOT编译，在App安装时就编译所有代码存储在本地，打开App直接运行，这样做的优点是应用运行速度变快，缺点也很明显，App安装时间明显变长，而且占用存储空间较大
- 7.0 引入了JIT编译，结合使用AOT/JIT混合编译
- Android 8.0引入了.vdex文件，它里面包含 APK 的未压缩 DEX 代码，以及一些用于加快验证速度的元数据

| |JVM|Android虚拟机|
|--|--|--|
|虚拟机栈|局部变量表和操作数栈|寄存器|
|堆|Eden、S0、S1和老年代|Alloc Space先当于新生代，Large Obj Space相当于老年代
|加载|class或jar|dex|

- 基于寄存器执行效率好，但是可移植性差，难跨平台

- 加载速度快，dex相比于Jar文件会把所有包含的信息整合在一起，减少了冗余信息。这样就减少I/O操作，提高类的查找速度

### 进程内存分配
通过getprop获取

app启动初始堆分配的内存：

\[dalvik.vm.heapstartsize]: [8m]

app最大内存限制：

\[dalvik.vm.heapgrowthlimit]: [256m]

开启largeHeap=true后的最大限制

\[dalvik.vm.heapsize]: [512m]

largeHeap为true，则内存到heapsize才会OOM，否则达到 heapgrowthlimit就会OOM

### 低内存kill进程机制
进程分为：空进程、后台进程、服务进程、可见进程、前台进程

AMS会对进程进行评分，记录到adj中

oom_adj: ps查看pid 然后使用cat/pid/oom_adj，内存不足时根据此值进行kill(从高到底) 

保活可以用workmanager