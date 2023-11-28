短时间内存反复发生增长和回收，可能会导致卡顿和oom

卡顿：gc回收中会有stw使得工作线程(包括主/子线程)暂停

oom：如果垃圾回收采用的时标记-清除法会出现大量内存碎片，此时创建一个比较大的对象(如数组)时由于没有连续内存空间导致oom

#### 监控工具

[BlockCanary](./android_blockcanary.md)

[Matrix](./android_matrix.md)

都是采用AOP思想

切面

BaseActivity：侵入性太强

Message

#### 常见内存抖动
- 字符串拼接(StringBuilder代替+)，Color.parseColor()
- 在循环里面创建对象
- 在onDraw里面创建对象
- bitmap

#### 内存抖动分析工具

Android Studio Memory-profiler中

Record native allocations

Record Java/Kotlin allocations

根据上下文环境优化对象创建

#### 优化手段
优化本质：减少对象的创建

对象池，可参考Handler、Glide、Okhttp等

Handler：采用单项链表实现

Glide：双Map实现

缺陷就是数组对象池中key不是基本数据类型会产生大量Integer对象

优化方式是使用仿照SparseArray结合TreeMap实现，TreeMap主要有ceilingKey方法需要模仿。

ceilingKey方法作用

数组如byte[]需要25个大小，而对象池中只有20或30的大小，此时通过ceilingKey可以获取到30大小

为何不直接创建25个大小的数组呢？ 违反本质是较少对象创建