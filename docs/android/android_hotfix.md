### 插件化必备知识点
1. Binder
2. AIDL
3. AMS
4. 四大组件的工作原理
5. PMS
6. App安装过程
7. ClassLoader以及双亲委托
8. 反射

#### ClassLoader以及双亲委托
Android4.4含以下：

+ DexClassLoader：加载jar、apk、dex，可以从指定目录中加载
+ PathClassLoader：只能加载已安装到系统中的apk文件，即/data/app目录

Android5.0-Android8.0：

+ DexClassLoader：不变
+ PathClassLoader：和DexClassLoader基本相同，除了无法进行dex2oat操作

Android8.0以上：完全一致

双亲委托机制：类加载时优先请求父类加载器加载，找不到指定类时才由子类加载器尝试加载

加载步骤：缓存加载--父类加载器加载--自己加载
```java
protected Class<?> loadClass(String name, boolean resolve)
        throws ClassNotFoundException
{
    // 缓存加载
    Class<?> c = findLoadedClass(name);
    if (c == null) {
        try {
            if (parent != null) {
                c = parent.loadClass(name, false); // 父类加载器加载
            } else {
                c = findBootstrapClassOrNull(name);
            }
        } catch (ClassNotFoundException e) {
        }

        if (c == null) {
            // 自己尝试加载
            c = findClass(name);
        }
    }
    return c;
}
```
双亲委托机制的作用
1. 防止重复加载
2. 安全，保证系统类不能被篡改

打破双亲委托机制：自定义类加载器

创建类继承于ClassLoader，重写loadClass方法，实现findClass方法

Android应用场景：插件化开发、热更新

类加载可以通过Class.forName("")