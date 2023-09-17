### 1.泛型定义
#### 泛型类
```java
public class NormalGeneric<K> {
    private K data;
}
```
#### 泛型接口
```java
public interface Genertor<T> {
    public T next();
}
```
##### 接口实现一
```java
public class ImplGenertor<T> implements Genertor<T> {
    @Override
    public T next() {
        return null;
    }
}
```
##### 接口实现二
```java
public class ImplGenertor2 implements Genertor<String> {
    @Override
    public String next() {
        return null;
    }
}
```
#### 泛型方法
```java
public class GenericMethod {
    public <T> T genericMethod(T...a){
        return a[a.length/2];
    }
}
```
注：有<T>才是泛型方法，且和泛型类无关，如
```java
public class GenericMethod<T> {
    public <T> T genericMethod(T...a){
        return a[a.length/2];
    }
}
```
此处类中的T和方法中的T毫无关联
### 2.泛型限定
利用extends关键字，可以是类或接口，若有类则只能存在一个且必须排列在首个
```java
public static <T extends ArrayList & Comparable> T min(T a, T b){
    if(a.compareTo(b)>0) return a; else return b;
}
```
### 3.泛型局限性、约束
1.不能实例化即new
```java
new T();
```
2.static修饰中不可使用，如下编译报错，除了静态泛型方法如上面min方法
```java
private static T instance;
private static void getInstance(T t){}
```
不可使用原因：静态先于对象创建，泛型是在对象创建时才确定

3.基本数据类型不允许作为泛型参数传入
```java
Restrict<int>
```
4.不允许使用instanceof判断，如下编译报错
```java
if(restrict instanceof Restrict<Double>)
```
5.数组可以申明但不能实例化，如下第2行编译报错
```java
Restrict<Double>[] restrictArray;
Restrict<Double>[] restricts = new Restrict<Double>[10];
```
6.不能使用在extends Exception/Throwable中，如下编译报错
```java
class Problem<T> extends Exception{
}
```
7.不能捕获泛型异常
```java
public <T extends Throwable> void doWork(T x){//不报错
    try{

    }catch(T t){//编译报错
        //do sth;
    }
}
```
正确写法
```java
public <T extends Throwable> void doWorkSuccess(T t) throws T{
    try{

    }catch(Throwable e){
        throw t;
    }
}
```
### 4.泛型中的继承和通配符
```java
Pair<Employee>和Pair<Worker>没有任何继承关系
public class Worker extends Employee {
}
public class Pair<T> {
    Pair<Employee> employeePair = new Pair<Worker>();//编译报错
}
```
正确方式
```java
private class ExtendPair<T> extends Pair<T>{
}
public class Pair<T> {
    Pair<String> pair = new ExtendPair<>();
}
```
#### 通配符产生原因
```java
public class Fruit{}
public class Orange extends Fruit {}
public class GenericType<T> {}
public static void print(GenericType<Fruit> p){}
public static void use(){
GenericType<Fruit> a = new GenericType<>();
print(a);
GenericType<Orange> b = new GenericType<>();
//print(b);//Orange属于Fruit但不能使用不合理
}
```
使用通配符解决，如下
```java
public static void print(GenericType<? extends Fruit> p){}
```
此处限定了上界为Fruit，如
```java
public class Fruit extends Food{}
print(new GenericType<Food>());//编译报错，超过泛型上界，最大是Fruit
print(new GenericType<Fruit>());//编译通过
```
注：?一般使用在变量中
#### 通配符缺陷
```java
public class GenericType<T> {
    private T data;
    public T getData() {
        return data;
    }
    public void setData(T data) {
        this.data = data;
    }
}
GenericType<? extends Fruit> c = new GenericType<>();
c.setData(new Orange());//报错
c.setData(new Fruit());//报错
c.setData(null);//编译通过
Fruit x = c.getData();//编译通过
```
注：null是可以set的

extends限制上界，下界使用super
```java
public static void print(GenericType<? super Fruit> p){}
public class Fruit extends Food{}
print(new GenericType<Orange>());//编译报错，没达到下界，至少是Fruit
print(new GenericType<Fruit>());//编译通过
```
离谱现象
```java
GenericType<? super Fruit> c = new GenericType<>();
c.setData(new Orange());//编译通过
c.setData(new Fruit());//编译通过
c.setData(new Food());//报错
Object x = c.getData();//编译通过
```
#### 总结
1.对于方法来说(print)，? extends X只能传<=X类型，? super X只能传>=X类型

2.对于实例来说

? extends X能安全读取(get)，null可以set

? super X能安全写入(set)，get只能拿到Object，set只能传<=X的类

?表示无限制，GenericType<?>

3.只能申明在变量中(临时变量、方法参数变量、成员变量等)

### 5.虚拟机泛型实现(伪泛型：类型擦除)
即泛型只在编译器有效
```java
public class GenericRaw<T> {
    private T data;
    public T getData() {
        return data;
    }
    public void setData(T data) {
        this.data = data;
    }
}
```
虚拟机编译后会将T用Object代替
```java
public class GenericRaw<T extends ArrayList & Comparable> {
    private T data;
    public T getData() {
        return data;
    }
    public void setData(T data) {
        this.data = data;
    }
    public void test(){
        data.compareTo()
        //((Comparable)data).compareTo()
    }
}
```
虚拟机编译后会将T用ArrayList代替，在使用到Comparable相关方法时，自动插入强转代码，如上面test方法中
对于重载方法
```java
public static String method(List<String> stringList){
    return "OK";
}
public static Integer method(List<Integer> stringList){
    return 1;
}
```
对于编译器来说上面两个方法不属于重载，通过方法名+方法参数
由于泛型擦除此处方法参数被认定为一致

了解：泛型擦除，原始泛型类型其实存在于Signature中，泛型擦除也是为了兼容jdk1.5之前版本(没有泛型概念)
### 6.泛型的好处
1.多种数据类型执行相同的代码

2.避免类型强转报类型转化异常