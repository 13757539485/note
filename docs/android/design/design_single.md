### ​饿汉式
```java
public class EagerSingleton {
    private static final EagerSingleton INSTANCE = new EagerSingleton();
    private EagerSingleton() {}
    public static EagerSingleton getInstance() {
        return INSTANCE;
    }
}
```
### 懒汉式
```java
public class LazySingleton {
    private static LazySingleton instance;
    private LazySingleton() {}
    public static synchronized LazySingleton getInstance() {
        if (instance == null) {
            instance = new LazySingleton();
        }
        return instance;
    }
}
```
### 枚举单例
```java
public enum EnumSingleton {
    INSTANCE;
    public void doSomething() {
        // 方法实现
    }
}
```
### <a id="lazy_instance">懒汉式(双重校验)</a>
```java
public class Singleton {
    private static volatile Singleton INSTANCE;
    private Singleton() {
    }
    public static Singleton getInstance() {
        if (INSTANCE == null) {
            synchronized (Singleton.class) {
                if (INSTANCE == null) {
                    INSTANCE = new Singleton();
                }
            }
        }
        return INSTANCE;
    }
}
```
### 静态内部类单例(推荐)
```java
class Singleton {
    private Singleton() {

    }

    public static Singleton getInstance() {
        return Holder.singleton;
    }
    private static class Holder{
        private static Singleton singleton = new Singleton();
    }
}
```
### kotlin的单例
object关键字[单例](../kotlin/kotlin_class.md#single)
#### 懒汉式(双重校验)
```kotlin
class Singleton private constructor() {
    companion object {
        val instance by lazy(mode = LazyThreadSafetyMode.SYNCHRONIZED) {
            Singleton()
        }
    }
}
```
#### 静态内部类单例
```kotlin
class Singleton private constructor() {
    companion object {
        val instance = Holder.instance
    }

    private object Holder {
        val instance = Singleton()
    }
}
```