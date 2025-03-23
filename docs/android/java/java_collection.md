java中所有数据结构都是基于数组和链表实现

数组：连续内存空间

链表：LinkedList

其他数据结构

堆：Heap，本质是二叉树的特性来维护一维数组，和树的区别：不是用指针实现，内存由于树，多用于排序

堆排序：二叉树最后一个非叶子节点：length/2-1

大顶堆、小顶堆

栈：Stack

队列：Queue

树：Tree，基于指针实现

图：Graph，有向无环图

散列表：Hash

### <a id="arraylist">ArrayList解析</a>

使用不同构造方法的区别
```java
public ArrayList() {
    this.elementData = DEFAULTCAPACITY_EMPTY_ELEMENTDATA;
}
ArrayList(int initialCapacity)
```
使用默认构造方法
```java
private static final Object[] DEFAULTCAPACITY_EMPTY_ELEMENTDATA = {};

this.elementData = DEFAULTCAPACITY_EMPTY_ELEMENTDATA;
```
会初始化elementData数组，长度为0，可以看出ArrayList是基于数组实现

使用有参构造方法
```java
private static final Object[] EMPTY_ELEMENTDATA = {}

if (initialCapacity > 0) {
    this.elementData = new Object[initialCapacity];
} else if (initialCapacity == 0) {
    this.elementData = EMPTY_ELEMENTDATA;//和默认有区别
} else {
    throw new IllegalArgumentException("Illegal Capacity: "+
                                        initialCapacity);
}
```
如果initialCapacity传0就和默认构造一样，如果大于0就会初始化长度为initialCapacity的数组

#### 问题探讨
既然可以设置初始化数组长度，是不是意味着就可以add(1, xxx)，不一定从第0个开始添加数据？

答案是错误的，当我们调用add方法时直接报下标越界
```java
private int size;
public void add(int index, E element) {
    if (index > size || index < 0)
        throw new IndexOutOfBoundsException(outOfBoundsMsg(index));

    ensureCapacityInternal(size + 1);  // Increments modCount!!
    System.arraycopy(elementData, index, elementData, index + 1,
                        size - index);
    elementData[index] = element;
    size++;
}

```
原因是this.size并没有在构造方法中初始化此时为0，index如果是0就没问题，this.size将正常赋值

**initialCapacity意义何在？**

改变elementData大小，即此数组的长度，当调用add(xx)是会调用ensureCapacityInternal方法
```java
private static final int DEFAULT_CAPACITY = 10;
private void ensureCapacityInternal(int minCapacity) {
    if (elementData == DEFAULTCAPACITY_EMPTY_ELEMENTDATA) {
        minCapacity = Math.max(DEFAULT_CAPACITY, minCapacity);
    }

    ensureExplicitCapacity(minCapacity);
}
private void ensureExplicitCapacity(int minCapacity) {
    modCount++;
    if (minCapacity - elementData.length > 0)
        grow(minCapacity);
}
```
minCapacity是this.size+1，如果调用默认构造则minCapacity就是10，if条件成立
```java
private static final int MAX_ARRAY_SIZE = Integer.MAX_VALUE - 8;
private void grow(int minCapacity) {
    int oldCapacity = elementData.length;
    int newCapacity = oldCapacity + (oldCapacity >> 1);
    if (newCapacity - minCapacity < 0)
        newCapacity = minCapacity;
    if (newCapacity - MAX_ARRAY_SIZE > 0)
        newCapacity = hugeCapacity(minCapacity);
    elementData = Arrays.copyOf(elementData, newCapacity);
}
```
这里因为数组长度是0，默认构造的话oldCapacity=0，minCapacity为10，则newCapacity=10，elementData扩容成10个容量大小。下次add时size变成了1，minCapacity=size+1=2，则minCapacity - elementData.length=-8，不会调用grow。等到size=10时，minCapacity=11，又进入grow，newCapacity=10+10/2=15

#### 总结
1.ArrayList默认容量是10

2.this.size表示的是容器正真的数据个数，并不是用来表示容量大小

3.initialCapacity修改默认容量，优点是可以用来优化扩容次数

4.ArrayList最大容量为Integer.MAX_VALUE - 8

<font color="#dd0000">-8的原因是对象头大小需要32 byte=8 int</font>

5.下次扩容大约为原来的1.5倍(可能除不尽，如13->19约1.46)

6.除了构造方法设置容量，也可以通过ensureCapacity(int minCapacity)

注意：理论当容量变成最大值后如果再add会变成Integer.MAX_VALUE，再add时minCapacity会变成负数导致内存溢出，实际内存早就不够达不到Integer.MAX_VALUE - 8
```java
private static int hugeCapacity(int minCapacity) {
    if (minCapacity < 0) // overflow
        throw new OutOfMemoryError();
    return (minCapacity > MAX_ARRAY_SIZE) ?
        Integer.MAX_VALUE :
        MAX_ARRAY_SIZE;
}
```
获取对象内存大小

https://repo.maven.apache.org/maven2/org/apache/lucene/lucene-core/
```java
ArrayList<String> list = new ArrayList<>();
long listSize = RamUsageEstimator.sizeOfCollection(list);
list.add("100");
long itemSize = RamUsageEstimator.sizeOfCollection(list);
long oneSize = itemSize - listSize;
double intMaxSize = oneSize * Integer.MAX_VALUE * 1.0 / 1024 / 1024 / 1024;
System.out.println(oneSize + "," + intMaxSize);
```
## <a id="hashmap">HashMap</a>
构造方法
```java
static final float DEFAULT_LOAD_FACTOR = 0.75f;
public HashMap() {
    this.loadFactor = DEFAULT_LOAD_FACTOR;
}
public HashMap(int initialCapacity, float loadFactor) {
    //...
    if (initialCapacity > MAXIMUM_CAPACITY)
        initialCapacity = MAXIMUM_CAPACITY;
    //...
    this.loadFactor = loadFactor;
    this.threshold = tableSizeFor(initialCapacity);
}
```

tableSizeFor用来保证容量是2的倍数
```java
static final int MAXIMUM_CAPACITY = 1 << 30;
static final int tableSizeFor(int cap) {
    int n = cap - 1;
    n |= n >>> 1;
    n |= n >>> 2;
    n |= n >>> 4;
    n |= n >>> 8;
    n |= n >>> 16;
    return (n < 0) ? 1 : (n >= MAXIMUM_CAPACITY) ? MAXIMUM_CAPACITY : n + 1;
}
```
\>>>是无符号右移，>>是位移
```java
System.out.println(tableSizeFor(16));
System.out.println(tableSizeFor(21));
System.out.println(tableSizeFor(43));
输出：16、32、64
```
put方法解析
```java
static final int TREEIFY_THRESHOLD = 8;
final V putVal(int hash, K key, V value, boolean onlyIfAbsent,
                   boolean evict) {
    Node<K,V>[] tab; Node<K,V> p; int n, i;
    if ((tab = table) == null || (n = tab.length) == 0)
        n = (tab = resize()).length;//首次扩容
    if ((p = tab[i = (n - 1) & hash]) == null)
        tab[i] = newNode(hash, key, value, null);//没有发生hash碰撞就直接存到数组中
    else {
        //出现碰撞即数组中已经有值
        Node<K,V> e; K k;
        if (p.hash == hash &&
            ((k = p.key) == key || (key != null && key.equals(k))))
            e = p;//key是同一个，直接赋值
        else if (p instanceof TreeNode)
            e = ((TreeNode<K,V>)p).putTreeVal(this, tab, hash, key, value);//使用红黑树保存
        else {
            for (int binCount = 0; ; ++binCount) {
                if ((e = p.next) == null) {
                    p.next = newNode(hash, key, value, null);
                    if (binCount >= TREEIFY_THRESHOLD - 1)
                        treeifyBin(tab, hash);//如果链表数量都超过8个就转成红黑树保存
                    break;
                }
                //...
            }//使用链表保存
        }
        if (e != null) {
            V oldValue = e.value;
            if (!onlyIfAbsent || oldValue == null)
                e.value = value;
            afterNodeAccess(e);
            return oldValue;//key相同，返回被覆盖的value值
        }
    }
    //...
    if (++size > threshold)// 达到总容量的0.75时会扩容
        resize();
    return null;
}
```

resize方法用来扩容
```java
static final int DEFAULT_INITIAL_CAPACITY = 1 << 4;//16
newCap = DEFAULT_INITIAL_CAPACITY;
newThr = (int)(DEFAULT_LOAD_FACTOR * DEFAULT_INITIAL_CAPACITY);
threshold = newThr;
Node<K,V>[] newTab = (Node<K,V>[])new Node[newCap];//初始化长度为16的数组
```
下次扩容看threshold大小，默认是0.75*16=12，即当容量为12时会进行一次扩容

转化红黑树的前提：数组中的元素链表长度大于8且数组本身长度>64

为什么是8而不是TREEIFY_THRESHOLD - 1=7，因为binCount是从0开始的
```java
final void treeifyBin(Node<K,V>[] tab, int hash) {
    int n, index; Node<K,V> e;
    if (tab == null || (n = tab.length) < MIN_TREEIFY_CAPACITY)//64
        resize();
    else if ((e = tab[index = (n - 1) & hash]) != null) {
        TreeNode<K,V> hd = null, tl = null;
        do {
            TreeNode<K,V> p = replacementTreeNode(e, null);
            if (tl == null)
                hd = p;
            else {
                p.prev = tl;
                tl.next = p;
            }
            tl = p;
        } while ((e = e.next) != null);
        if ((tab[index] = hd) != null)
            hd.treeify(tab);
    }
}
```
自定义首次扩容大小流程(扩容针对的是数组，并非容量大小，容量=size)

调用两个参数构造方法
```java
new HashMap<>(27, 0.5F)

->HashMap()
this.loadFactor = 0.5;
this.threshold = tableSizeFor(27);//32

->resize()
int newCap, newThr = 0;
int oldThr = threshold;//32
else if (oldThr > 0) 
    newCap = oldThr;//32
if (newThr == 0) {
    float ft = (float)newCap * loadFactor;//32*0.5=16
    newThr = (newCap < MAXIMUM_CAPACITY && ft < (float)MAXIMUM_CAPACITY ?
                (int)ft : Integer.MAX_VALUE);//16
}
threshold = newThr;
Node<K,V>[] newTab = (Node<K,V>[])new Node[newCap];
table = newTab;
```

下次扩容都是原来的2倍
```java
int oldThr = threshold;
else if ((newCap = oldCap << 1) < MAXIMUM_CAPACITY &&
    oldCap >= DEFAULT_INITIAL_CAPACITY)
newThr = oldThr << 1;
```
## SparseArray
二分查找法、结构是两个数组，默认容量是10(非集合大小)
```java
private int[] mKeys;
private Object[] mValues;
public SparseArray() {
    this(10);
}
```

扩容机制，如果初始容量小于4直接扩容成8，否则都是扩容成原来的2倍
```java
public static int growSize(int currentSize) {
    return currentSize <= 4 ? 8 : currentSize * 2;
}
```
## ConcurrentHashMap

## <a id="linkedhashmap">LinkedHashMap</a>
继承于HashMap
```kotlin
val map = object : LinkedHashMap<String, String>() {
    override fun removeEldestEntry(eldest: MutableMap.MutableEntry<String, String>): Boolean {
        return size > 3
    }
}
```
限制map大小，FIFO淘汰机制，上面超过3个后会从第0个开始移除，此方法是在父类HashMap中的afterNodeInsertion里面调用的

## 算法
### 二分查找
前提：已经有序排列

### 排序算法
冒泡、选择、插入、归并、堆、快速、希尔、计数、基数、桶排序

#### 冒泡排序
核心思想：两两比较，将最小的或最大的排到最后

比如：5 8 1 75 升序

第一轮：5,8 8,1->1,8 8,75变成5 1 8 75，此时75无需比较

第二轮：5,1->1,5 5,8变成1 5 8 75，此时8也无需比较

第三轮：1,5结束

所以轮数是数组长度-1，每轮比较次数是数组长度-1-第几轮

```kotlin
fun bubbleSort(arr: IntArray, isAsc: Boolean) {
    if (arr.isNotEmpty()) {
        for (i in arr.indices) {
            for (j in 0 until arr.size - 1 - i) {
                val asc = if (isAsc) arr[j + 1] > arr[j]
                else arr[j + 1] < arr[j]
                if (asc) {
                    val temp = arr[j + 1]
                    arr[j + 1] = arr[j]
                    arr[j] = temp
                }
            }
        }
    }
    println(arr.contentToString())
}
```

#### 选择排序
核心思想：找到最大或最小值用下标记录，放到最前面

比如：5 8 1 75 升序(找最小值) 下标index

第一轮：index = 2，1和5交换，此时变成8 5 75

第二轮：index = 1，8和5交换，此时变成8 75

第三轮：index = 0结束

```kotlin
fun selectSort(arr: IntArray, isAsc: Boolean) {
    for (i in arr.indices) {
        var index: Int = i
        for (j in i+1 until arr.size) {
            val asc = if (isAsc) arr[j] < arr[index]
            else arr[j] > arr[index]
            if (asc) {
                index = j
            }
        }
        val temp = arr[index]
        arr[index] = arr[i]
        arr[i] = temp
    }
    println(arr.contentToString())
}
```