### 谈谈List,Set,Map的区别

List：有序的集合，允许重复元素，主要实现类​ArrayList和​LinkedList

Set：不保证顺序(LinkedHashSet和TreeSet除外)，不允许重复元素，主要实现类​HashSet、LinkedHashSet和TreeSet

Map：键值对，key不允许重复，value允许重复，不保证顺序(LinkedHashMap和TreeMap除外)，主要实现类HashMap、LinkedHashMap和TreeMap

### 各个集合的时间复杂度

|特性|ArrayList|LinkedList|HashSet|LinkedHashSet|TreeSet|HashMap|LinkedHashMap|TreeMap|
|--|--|--|--|--|--|--|--|--|
|​插入|O(1)|O(1)|O(1)|O(1)|O(log n)|O(1)|O(1)|O(log n)|
|​删除|O(n)|O(1)|O(1)|O(1)|O(log n)|O(1)|O(1)|O(log n)|
|​查找|O(1)|O(n)|O(1)|O(1)|O(log n)|O(1)|O(1)|O(log n)|
|​访问元素|O(1)|O(n)|不支持|不支持|不支持|不支持|不支持|不支持|
|​有序性|插入顺序|插入顺序|无序|插入顺序|自然顺序或自定义|无序|插入顺序|然顺序或自定义|

### 谈谈ArrayList和LinkedList的区别

​ArrayList：

- 基于动态数组实现，支持快速随机访问。
- 插入和删除操作在列表中间位置时效率较低。

​LinkedList：

- 基于双向链表实现，插入和删除操作效率高。
- 随机访问效率较低。

### 请说一下HashMap与HashTable的区别

HashMap：非线程安全，key/value允许null值，初始容量为16，每次扩容为2倍

​HashTable：线程安全(synchronized)，不允许null值，初始容量为11，每次扩容为原来的2n+1

### 谈一谈ArrayList的扩容机制

见[ArrayList](../java/java_collection.md#arraylist)

### HashMap的实现原理

见[HashMap](../java/java_collection.md#hashmap)

哈希表指的是

- ​哈希桶数组（Node<K,V>[] table）​：这是存储键值对的核心数组，每个元素是一个链表的头节点
- ​节点类（Node<K,V>）​：用于表示哈希表中的每个键值对，包含键、值、哈希码和指向下一个节点的引用
- ​哈希函数（hash方法）​：用于计算键的哈希值，并将其映射到数组的索引位置

### 请简述LinkedHashMap的工作原理和使用方式

和HashMap类似不是线程安全的,除了哈希表维护了一个双向链表,LRU（最近最少使用）缓存，通过重写removeEldestEntry，见[LinkedHashMap](../java/java_collection.md#linkedhashmap)

使用方式和HashMap相同

### 谈谈对于ConcurrentHashMap的理解

高性能、线程安全的哈希表实现，相比HashTable和Collections.synchronizedMap()，ConcurrentHashMap提供了更高的并发性能，允许多个线程同时读取和写入数据，而不会导致数据不一致或性能瓶颈

​JDK 1.7：使用分段锁（Segment）机制，将数据分成多个段，每个段独立加锁，从而提高并发度。

​JDK 1.8：移除了分段锁，采用 CAS + synchronized机制，直接使用Node<K,V>[] table结构，简化了数据结构并提高了性能

扩容机制和HashMap相同，区别是HashMap扩容是单线程，它是多线程，并发扩容性能影响更少
什么是冒泡排序？如何优化？


请说一说HashMap，SparseArrary原理，SparseArrary相比HashMap的优点、ConcurrentHashMap如何实现线程安全？

请说一说HashMap原理，存取过程，为什么用红黑树，红黑树与完全二叉树对比，HashTab、concurrentHashMap，concurrent包里有啥?

请说一说hashmap put()底层原理,发生冲突时，如何去添加(顺着链表去遍历，挨个比较key值是否一致，如果一致，就覆盖替换，不一致遍历结束后，插入该位置) ？

请用 Java 实现一个简单的单链表？

如何反转一个单链表？

谈谈你对时间复杂度和空间复杂度的理解？

谈一谈如何判断一个链表成环？

什么是红黑树？为什么要用红黑树？

什么是快速排序？如何优化？

说说循环队列？

如何判断单链表交叉

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
