### Android虚拟机
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

| |JVM|Android虚拟机|
|--|--|--|
|虚拟机栈|局部变量表和操作数栈|寄存器|
|堆|Eden、S0、S1和老年代|Alloc Space先当于新生代，Large Obj Space相当于老年代
|加载|class|dex|

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