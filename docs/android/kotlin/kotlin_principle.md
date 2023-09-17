### by lazy
委托的必要条件是有get/setValue函数以及operator
```kotlin
public interface Lazy<out T> {
    public val value: T

    public fun isInitialized(): Boolean
}

public actual fun <T> lazy(initializer: () -> T): Lazy<T> = SynchronizedLazyImpl(initializer)
```

无论是Lazy还是SynchronizedLazyImpl都没有看到get/setValue函数以及operator

真正实现在拓展函数中
```kotlin
public inline operator fun <T> Lazy<T>.getValue(thisRef: Any?, property: KProperty<*>): T = value
```
lazy还有一个重载函数
```kotlin
public actual fun <T> lazy(mode: LazyThreadSafetyMode, initializer: () -> T): Lazy<T> =
    when (mode) {
        LazyThreadSafetyMode.SYNCHRONIZED -> SynchronizedLazyImpl(initializer)
        LazyThreadSafetyMode.PUBLICATION -> SafePublicationLazyImpl(initializer)
        LazyThreadSafetyMode.NONE -> UnsafeLazyImpl(initializer)
    }
```
其中SynchronizedLazyImpl使用synchronized+双重校验保证线程安全，代码块只会调用1次

SafePublicationLazyImpl是使用CS操作保证线程安全，可能会调用多次代码块，但数值只取第一次

LazyThreadSafetyMode不保证线程安全

### 枚举
枚举只能继承接口不能继承类的原因

通过反编译可以看到枚举其实是个Class继承于java/lang/Enum，而对于java来说是单继承规则

优化点：

1.枚举有两个属性name和ordinal，其中ordinal一般开发者使用不到，当需要使用到枚举的数值时，建议手动添加字段去记录

2.当枚举需要使用数值关联时，使用EnumMap和EnumSet

