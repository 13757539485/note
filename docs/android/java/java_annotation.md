
### 1.基本使用
```java
@Target(ElementType.TYPE)
@Retention(RetentionPolicy.SOURCE)
public @interface MyAnnotation {
    String value();
}
```
value比较特殊，在使用时无需指定
```java
@MyAnnotation("MyClass")
public class MyClass {
｝
```
如果换成别的名字如id，则需要如下写法
```java
@MyAnnotation(id = "MyClass")
public class MyClass {
｝
```
如果是多个元素则必须全部以xxx = aa，yyy = bb写法

#### @Target
作用于什么对象上，如下表示可以作用在类上、属性上、方法上
```java
@Target({ElementType.TYPE, ElementType.FIELD, ElementType.METHOD})
```

* ElementType.ANNOTATION_TYPE 可以应用于注解类型。
* ElementType.CONSTRUCTOR 可以应用于构造函数。
* ElementType.FIELD 可以应用于字段或属性。
* ElementType.LOCAL_VARIABLE 可以应用于局部变量。
* ElementType.METHOD 可以应用于方法级注解。
* ElementType.PACKAGE 可以应用于包声明。
* ElementType.PARAMETER 可以应用于方法的参数。
* ElementType.TYPE 可以应用于类的任何元素。

#### @Retention
* RetentionPolicy.SOURCE仅保留在源级别中，并被编译器忽略
* RetentionPolicy.CLASS在编译时由编译器保留，但 Java 虚拟机(JVM)会忽略
* RetentionPolicy.RUNTIME由 JVM 保留，因此运行时环境可以使用它
#### @Inherited 
注解作⽤被⼦类继承
#### 场景
SOURCE：

1.APT技术，一般用于生成辅助类，例如Arouter、butterKnidnife等框架

为啥APT只需要SOURCE级别，因为注解程序运行时机是在javac解析源码的时候，SOURCE注解级别刚好是保留到源码级别，即能被解析到

2.语法检查，比如@IntDef、DrawableRes、@Override等

CLASS：
1.AspectJ、热修复Roubust

RUNTIME：
一般结合反射技术，如Retrofit

TypeVariable  
  	泛型类型变量。可以泛型上下限等信息；

ParameterizedType
  	具体的泛型类型，可以获得元数据中泛型签名类型(泛型真实类型)

GenericArrayType
  	当需要描述的类型是泛型类的数组时，比如List[],Map[]，此接口会作为Type的实现。

WildcardType
  	通配符泛型，获得上下限信息；
