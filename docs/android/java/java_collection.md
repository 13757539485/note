### ArrayList解析

使用不同构造方法的区别
```java
ArrayList()
ArrayList(int initialCapacity)
```
使用默认构造方法
```java
private static final Object[] DEFAULTCAPACITY_EMPTY_ELEMENTDATA = new Object[0];

this.elementData = DEFAULTCAPACITY_EMPTY_ELEMENTDATA;
```
会初始化elementData数组，长度为0，可以看出ArrayList是基于数组实现

使用有参构造方法
```java
if (initialCapacity > 0) {
    this.elementData = new Object[initialCapacity];
} else {
    if (initialCapacity != 0) {
        throw new IllegalArgumentException("Illegal Capacity: " + initialCapacity);
    }

    this.elementData = EMPTY_ELEMENTDATA;
}
```
如果initialCapacity传0就和默认构造一样，如果大于0就会初始化长度为initialCapacity的数组

#### 问题探讨
既然可以设置初始化数组长度，是不是意味着就可以add(1, xxx)，不一定从第0个开始添加数据？

答案是错误的，当我们调用add方法时直接报下标越界
```java
private int size;
public void add(int index, E element) {
    this.rangeCheckForAdd(index);
    ...
    int s;
    ...
    this.size = s + 1;
}

private void rangeCheckForAdd(int index) {
    if (index > this.size || index < 0) {
        throw new IndexOutOfBoundsException(this.outOfBoundsMsg(index));
    }
}
```
原因是this.size并没有在构造方法中初始化此时为0，index如果是0就没问题，this.size将正常赋值

**initialCapacity意义何在？**

当调用add(xx)是会调用三个参数的add方法
```java
private void add(E e, Object[] elementData, int s) {
    if (s == elementData.length) {
        elementData = this.grow();
    }

    elementData[s] = e;
    this.size = s + 1;
}
```
s是this.size，如果使用默认构造方法，此时if判断是成立的
```java
int oldCapacity = this.elementData.length;
if (oldCapacity <= 0 && this.elementData == DEFAULTCAPACITY_EMPTY_ELEMENTDATA) {
    return this.elementData = new Object[Math.max(10, minCapacity)];
} else {
    int newCapacity = ArraysSupport.newLength(oldCapacity, minCapacity - oldCapacity, oldCapacity >> 1);
    return this.elementData = Arrays.copyOf(this.elementData, newCapacity);
}

public static int newLength(int oldLength, int minGrowth, int prefGrowth) {
    int prefLength = oldLength + Math.max(minGrowth, prefGrowth);
    return 0 < prefLength && prefLength <= Integer.MAX_VALUE - 8 ? prefLength : hugeLength(oldLength, minGrowth);
}
```
这里因为数组长度是0，grow()最终会返回一个长度为10的数组，即此方法是用来扩容操作的；else是下次扩容逻辑

假设oldCapacity=10，则minCapacity - oldCapacity=1

oldCapacity >> 1

1010 = 10
右移后：0101 = 5

prefLength = 10+max(1,5) -> 15

#### 总结
1.ArrayList默认容量是10

2.this.size表示的是容器正真的数据个数，并不是用来表示容量大小

3.initialCapacity修改默认容量，优点是可以用来优化扩容次数

4.ArrayList最大容量为Integer.MAX_VALUE - 8

<font color="#dd0000">-8的原因是对象头大小需要32 byte=8 int</font>

5.下次扩容大约为原来的1.5倍(可能除不尽，如13->19约1.46)

6.除了构造方法设置容量，也可以通过ensureCapacity(int minCapacity)