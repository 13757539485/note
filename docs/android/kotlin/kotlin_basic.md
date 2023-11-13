### 简介
[官网](https://book.kotlincn.net/)

判断静态语言、动态语言？

静态语言是编译期能校验数据类型，如java
```java
String a = "xx";
a = 1;//报错
```

如kotlin
```kotlin
var a = "xxx"
a = 1//报错
```

基本数据类型

Byte,Short,Int,Long,Float,Double,Char,String,Boolean

kotlin只有引用类型，在编译后会转化成java的基本数据类型

val表示只读变量

常量const修饰，不能写在函数里面，因为常量是编译时就需要确定，而函数是运行时才被调用
```kotlin
const val msg = "msg"
fun main() {
    val info = "info"
}
```
表达式是有返回值的

java中的if是语句而kotlin中的是表达式

range表达式

in 12..89

when表达式相当于java中的switch

String模版使用${}方式，其中{}部分场景可省略

### field和property
[Java](../java/java_basic.md)中可以单独申明字段，但kotlin中不行，只能是属性

原因是kotlin必须使用var或val申明，且会自动生成get/set方法，而属性=字段+get/set

get方法不能被private修饰