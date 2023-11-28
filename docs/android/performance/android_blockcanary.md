官网：https://github.com/markzhai/AndroidPerformanceMonitor

接入方式：
```gradle
implementation 'com.github.markzhai:blockcanary-android:1.5.0'
```

### 实现原理
Android消息机制采用的是Handler，利用Handler处理Message时的打印接口

在Looper的loop方法中的loopOnce方法分发msg时有一个Printer对象(接口)
```java
if (logging != null) {
    logging.println(">>>>> Dispatching to " + msg.target + " "
            + msg.callback + ": " + msg.what);
}
//...
msg.target.dispatchMessage(msg);//回调Handler处理消息
//...
if (logging != null) {
    logging.println("<<<<< Finished to " + msg.target + " " + msg.callback);
}
```
客户端可以手动设置一个Printer对象，利用先后两次println方法回调，可以记录时间戳
```java
public void setMessageLogging(@Nullable Printer printer) {
    mLogging = printer;
}
```
### 缺陷
粒度太大，不精准，假设调用链为方法a、b、c，分别耗时400、25、75，无法确定是哪个方法耗时长

解决方案
