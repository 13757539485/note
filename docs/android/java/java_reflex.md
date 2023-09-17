### 获得 Class 对象
获取Class对象的三种方式
1. 通过类名获取 类名.class
2. 通过对象获取 对象名.getClass()
3. 通过全类名获取 Class.forName(全类名) classLoader.loadClass(全类名)

使用 Class 类的 forName 静态方法
```java
public static Class<?> forName(String className)
```
直接获取某一个对象的 class
```java
Class<?> klass = int.class;
Class<?> classInt = Integer.TYPE;
```
调用某个对象的 getClass() 方法
```java
StringBuilder str = new StringBuilder("123");
Class<?> klass = str.getClass();
```
### 判断是否为某个类的实例
一般地，我们用 instanceof 关键字来判断是否为某个类的实例。同时我们也可以借助反射中 Class 对象的isInstance() 方法来判断是否为某个类的实例，它是一个 native 方法：
```java
public native boolean isInstance(Object obj);
```
判断是否为某个类的类型
```java
public boolean isAssignableFrom(Class<?> cls)
```
### 创建实例
通过反射来生成对象主要有两种方式。

使用Class对象的newInstance()方法来创建Class对象对应类的实例。
```java
Class<?> c = String.class;
Object str = c.newInstance();
```
先通过Class对象获取指定的Constructor对象，再调用Constructor对象的newInstance()方法来创建实例。这
种方法可以用指定的构造器构造类的实例。
```java
//获取String所对应的Class对象
Class<?> c = String.class;
//获取String类带一个String参数的构造器
Constructor constructor = c.getConstructor(String.class);
//根据构造器创建实例
Object obj = constructor.newInstance("23333");
System.out.println(obj);
```
### 获取构造器信息
得到构造器的方法
```java
Constructor getConstructor(Class[] params) -- 获得使用特殊的参数类型的public构造函数(包括父类）
Constructor[] getConstructors() -- 获得类的所有公共构造函数
Constructor getDeclaredConstructor(Class[] params) -- 获得使用特定参数类型的构造函数(包括私有)
Constructor[] getDeclaredConstructors() -- 获得类的所有构造函数(与接入级别无关)
```

### 获取类的成员变量（字段）信息
获得字段信息的方法
```java
Field getField(String name) -- 获得命名的公共字段
Field[] getFields() -- 获得类的所有公共字段
Field getDeclaredField(String name) -- 获得类声明的命名的字段
Field[] getDeclaredFields() -- 获得类声明的所有字段
```

### 调用方法
获得方法信息的方法
```java
Method getMethod(String name, Class[] params) -- 使用特定的参数类型，获得命名的公共方法
Method[] getMethods() -- 获得类的所有公共方法
Method getDeclaredMethod(String name, Class[] params) -- 使用特写的参数类型，获得类声明的命名的方法
Method[] getDeclaredMethods() -- 获得类声明的所有方法
```
调用invoke来调用方法
```java
public Object invoke(Object obj, Object... args)
```

### 静态代理
定义抽象接口
```java
/**
 * 代理抽象角色
 */
public interface Massage {
    void massage(String name);
}
public interface Wash {
    void wash();
}
```
定义代理对象
```java
public class Agent implements Massage {
    private final Massage massage;
    public Agent(Massage massage) {
        this.massage = massage;
    }
    @Override
    public void massage(String name) {
        massage.massage(name);
    }
}
public class WashAgent implements Wash {
    private final Wash mWash;
    public WashAgent(Wash wash) {
        this.mWash = wash;
    }
    @Override
    public void wash() {
        this.mWash.wash();
    }
}
```
定义实现类
```java
public class Alvin implements Massage, Wash {
    @Override
    public void massage(String name) {
        System.out.println("massage");
    }
    @Override
    public void wash() {
        System.out.println("wash");
    }
}
```
测试
```java
Massage massage = new Alvin();
Wash wash = new Alvin();
Agent agent = new Agent(massage);
agent.massage("Alvin");
WashAgent washAgent = new WashAgent(wash);
washAgent.wash();
```
### 动态代理
```java
Alvin alvin = new Alvin();
Object o = Proxy.newProxyInstance(MyClass.class.getClassLoader(),
        new Class[]{Massage.class, Wash.class},//传入需要代理的class
        new InvocationHandler() {
            @Override
            public Object invoke(Object o, Method method, Object[] objects) throws Throwable {
                //反射调用方法，method是调用的方法比如massage和wash
                //objects是方法参数值比如“Alvin”
                return method.invoke(alvin, objects);
            }
        });
Massage mag = (Massage) o;
mag.massage("Alvin");//会触发invoke回调

Wash wh = (Wash) o;
wh.wash();//会触发invoke回调
```
