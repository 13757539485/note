### 简介
享元模式是对象池的一种实现，享元模式用来尽可能减少内存使用量，它适合用于可能存在大量重复对象的场景，来缓存可共享的对象，达到对象共享、避免创建过多对象的效果，这样一来就可以提升性能、避免内存移除等。
### 使用场景
1. 系统中存在大量的相似对象。
2. 细粒度的对象都具备较接近的外部状态，而且内部状态与环境无关，也就是说对象没有特定身份。
3. 需要缓冲池的场景。
### Android中的应用
#### <a id="handler">Handler中</a>
```java
// Handler
public final Message obtainMessage(){
    return Message.obtain(this);
}
// Message
public static Message obtain(Handler h) {
    Message m = obtain();
    m.target = h;
    return m;
}
public static Message obtain() {
    synchronized (sPoolSync) {
        if (sPool != null) { // 从缓存中取Message
            Message m = sPool;
            sPool = m.next;
            m.next = null;
            m.flags = 0; // clear in-use flag
            sPoolSize--;
            return m;
        }
    }
    return new Message();
}
// Looper中loopOnce方法最后会回收message
msg.recycleUnchecked(); 
void recycleUnchecked() {
    flags = FLAG_IN_USE;
    what = 0;
    arg1 = 0;
    arg2 = 0;
    obj = null;
    replyTo = null;
    sendingUid = UID_NONE;
    workSourceUid = UID_NONE;
    when = 0;
    target = null;
    callback = null;
    data = null;

    synchronized (sPoolSync) { // 将Message内容清空后存放到缓存中以便于复用
        if (sPoolSize < MAX_POOL_SIZE) { // 上限50个
            next = sPool;
            sPool = this;
            sPoolSize++;
        }
    }
}
```
设计优势：

1. 屏幕刷新也需要Handler处理消息(60hz,120hz等每16ms或8ms消息处理一次比较频繁)，如果不进行缓存复用，可能会出现内存抖动
2. 有利于少内存碎片，如果直接new的方式会产生内存碎片，当创建比较大又需要连续内存时可能导致oom(如创建bitmap)

#### <a id="recyclerView">RecyclerView中</a>