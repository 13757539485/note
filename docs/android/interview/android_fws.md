### Android的事件分发机制

见[事件分发](../android_ui.md#view_dispatch)

### HandlerThread的使用场景和用法

见[HandlerThread](../fws/fws_handler.md#HandlerThread)

### Handler相关问题

见[Handler](../fws/fws_handler.md)

HandlerThread是什么？为什么它会存在？

简述下 Handler 机制的总体原理？

Looper 存在哪？如何可以保证线程独有？

如何理解 ThreadLocal 的作用？

主线程 Main Looper 和一般 Looper 的异同？

Handler 或者说 Looper 如何切换线程？

Looper 的 loop() 死循环为什么不卡死？

Looper 的等待是如何能够准确唤醒的？

Message 如何获取？为什么这么设计？

### AMS相关

ActivityManagerService是什么？什么时候初始化的？有什么作用？

ActivityThread是什么?ApplicationThread是什么?他们的区别

Instrumentation是什么？和ActivityThread是什么关系？

ActivityManagerService和zygote进程通信是如何实现的。

手写实现简化版AMS

### Binder相关

Binder有什么优势

Binder是如何做到一次拷贝的

MMAP的内存映射原理了解吗

Binder机制是如何跨进程的

说说四大组件的通信机制 
