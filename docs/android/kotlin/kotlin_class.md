### 初识类
声明属性默认会有get，set方法，可进行修改如下
```kotlin
class MyClass {
    var name = "AFREE"
        get() = field.lowercase()
        set(value) {
            field = "[$value]"
        }
}
MyClass().name//获取
MyClass().name = "xxx"//赋值
```
### 主构造函数
```kotlin
class MyClass(_pwd: String) {//参数是临时类型
    var pwd: String? = _pwd
    fun getAge(): String {
        return pwd?.run {
            ifBlank {
                "is blank"
            }
        } ?: "is null"
    }
}
```
以上pwd参数是类java写法，kt一般以下写法
```kotlin
class MyClass(var pwd: String?) {
}
```
如果是var则自动生成get、set方法，如果是val则只有get方法

### init代码块

init是主构造调用时触发，而次构造必须调用主构造函数

### 次构造函数
```kotlin
class MyClass(pwd: String) {
    init {
        println("主构造被调用，密码：$pwd")
    }
    constructor() : this("mini", "123"){
        println("我是次构造1")
    }
    constructor(userName: String, pwd: String) : this(userName){
        println("我是次构造2")
    }
}
fun main() {
    MyClass()
}
```
constructor都是次构造

<font color="#dd0000">注：init中能拿到主构造中的参数，无论是否是临时属性</font>

执行顺序

类成员和init块同时生成，取决于编写顺序

### 继承
类和函数编译后默认是final修饰无法继承，使用open关键字即可移除final
```kotlin
open class Person{
    open fun showName() {
        println("我是Person")
    }
}

class Student : Person() {
    override fun showName() {
        println("我是Student")
    }
    fun study() {
        println("我爱学习")
    }
}
```
#### is
判断类型相当于java的instanceOf

#### as
转化类型，强转
```kotlin
    val p: Person = Student()
//    (p as Student).study()
    p.study()
```
如果没有第二行代码，第三行代码会报错，或者可以
```kotlin
if (p is Student) {
    p.study()
}
```
kt的根父类Any相当于java的Object

Any类只提供标准，看不到源码具体实现，由各个平台处理

除了可以转化类型，还可以对导入函数进行重命名，如
```kotlin
import com.hfc.lib.util.abcdfjdksalfjdlsajflglsdjfsak as method

fun main() {
//    abcdfjdksalfjdlsajflglsdjfsak()
    method()
}
```
### object关键字
#### <a id="single">单例</a>
表示的类将变成单例对象
```kotlin
object Student : Person() {
}
Student.study()
```
<font color="#dd0000">注：构造函数将会被私有化</font>

#### 匿名对象(object : X())
```kotlin
val p = object : Person() {
    override fun showName() = println()
}
p.showName()
```
注：kt里面只能这样写，如果是java接口可以省略object，如java中的Runnable接口
```kotlin
Runnable { 
    
}
```
#### 伴生对象(companion object)
只会初始化一次，可以利用代替java中的静态常量、静态方法
```kotlin
companion object my {
    const val NAME = "" // 常量
    var AGE = 2 // 静态变量
    fun hello(){} // 静态方法
}
```
编译后会生成一个静态内部类，my表示自定义名字(一般不写)

### 嵌套类(静态内部类)
```kotlin
fun main() {
    A.B().b()
}

class A {
    class B {
        fun b() = println("b")
    }
}
```
<font color="#dd0000">注：B无法访问A中的内容</font>

<font color="#dd0000">原因：编译后会生成静态内部类，所以无法访问非静态内容</font>

### 内部类(inner class)
```kotlin
fun main() {
    A().B().b()
}

class A {
    val a = ""
    inner class B {
        fun b() = println("$a")
    }
}
```
编译后就是java的内部类

### 数据类(data class)
和普通类区别：

普通类编译后只会生成get set方法和构造方法

数据类额外会生成equals，hasCode，copy，component1(解构)，toString

<font color="#dd0000">注：数据类生成时只生成主构造的属性，忽略其他属性</font>

使用场景：
1.服务器返回响应的bean，必须要有主构造且参数至少一个，必须使用var或val修饰参数，不能使用其他修饰符修饰类，如open，inner等

2.需要用到toString，copy等场景

<font color="#dd0000">注：默认生成的hashcode和toStirng都是按照构造函数里的参数</font>
```kotlin
data class MyClass(var id: String?) {
    var name: String?= null
}
```
以上只会根据id生成hashCode和toString

### 枚举(enum class)
也是class
```kotlin
fun main() {
    println(Week.Monday) // Monday
    println(Week.Friday is Week) //true
}

enum class Week{
    Monday,
    Tuesday,
    Wednesday,
    Thursday,
    Friday,
    Saturday,
    Sunday
}
```
kt中枚举一般用法
```kotlin
fun main() {
    Clock.DATA_OLD.showClock()
    Clock.DATA_NOW.showClock()
    Clock.DATA_OLD.updateClock(SimpleClock("2000", "16:25"))
    Clock.DATA_NOW.updateClock(SimpleClock("2050", "17:58"))
}

enum class Clock(var clock: SimpleClock) {
    DATA_OLD(SimpleClock("1990", "22:15")),
    DATA_NOW(SimpleClock("2022", "12:28"));

    fun updateClock(updateClock: SimpleClock) {
        this.clock = updateClock
        showClock()
    }

    fun showClock() {
        println(this.clock)
    }
}

data class SimpleClock(var data: String, var time: String)
```
其中如DATA_OLD就是枚举本身，调用主构造赋值

枚举和when结合是可以不写else，<font color="#dd0000">但需要穷举所有枚举值(kt1.6以后会编译不通过)</font>
```kotlin
class WeekTest {
    fun switch(week: Week) =
        when (week) {
            Week.Monday -> "Monday"
            Week.Tuesday -> "Tuesday"
            Week.Wednesday -> "Wednesday"
            Week.Thursday -> "Thursday"
            Week.Friday -> "Friday"
            Week.Saturday -> "Saturday"
            Week.Sunday -> "Sunday"
        }
}
println(WeekTest().switch(Week.Monday))
```
### 密封类(sealed class)
密封类使用sealed修饰，主要用来解决使用when时不写else，在when时可使用is判断，实现上述逻辑
```kotlin
fun main() {
    println(WeekTest().switch(Week.Monday))
    println(WeekTest().switch(Week.Sunday("10:55")))
}

class WeekTest {
    fun switch(week: Week) =
        when (week) {
            is Week.Monday -> "Monday"
            is Week.Tuesday -> "Tuesday"
            is Week.Wednesday -> "Wednesday"
            is Week.Thursday -> "Thursday"
            is Week.Friday -> "Friday"
            is Week.Saturday -> "Saturday"
            is Week.Sunday -> "Sunday: ${week.time}"
        }
}

sealed class Week {
    object Monday: Week()//单例，必须继承本类
    object Tuesday: Week()
    object Wednesday: Week()
    object Thursday: Week()
    object Friday: Week()
    object Saturday: Week()
    class Sunday(val time: String): Week()//可以非单例，因为有参数
}
```

### <a id="kotlin_copy">拷贝</a>
#### 浅拷贝
即两个对象指向同一地址，当其中某一个对象的属性内容修改后，另一个对象也同步变化，举例：

1.java中clone方法和kotlin中data数据类的copy都是浅拷贝

2.list或其他集合add，addAll或构造方法初始化都是浅拷贝

3.System.arraycopy()也是浅拷贝
#### 深拷贝
两个对象地址不同且数据变化互不影响，基本数据类型和String不在深拷贝考虑

实现思路大致有几种

1.如果是普通class，如果是java重写clone，如果是kotlin需要继承Cloneable再重写clone，对引用属性进行创建对象赋值，kt为例
```kotlin
class C(var d: D): Cloneable {
    override fun clone(): Any {
        return C(D(d.age))
    }
}

class D(var age: Int)
```

2.kt直接使用data class
```kotlin
data class A(var name: String, var b: B)
data class B(var age: Int）

val a = A("3", B(4))
a.copy(name = "4", b = B(5))
```

3.使用自定义interface
```kotlin
interface DeepCopyable<out R> {
    fun deepCopyFun(): R
}
data class A(var name: String, var b: B) : DeepCopyable<B> {
    override fun deepCopyFun(): B = B(b.age)
}
data class B(var age: Int)
```

4.如果是序列化类，可以进行序列化和反序列化进行拷贝
```kotlin
fun <T : Any> T.deepCopy(): T {
    val cls = this::class
    if (cls is Serializable || cls is Parcelable) {
        val byteArrayOutputStream = ByteArrayOutputStream()
        val outputStream = ObjectOutputStream(byteArrayOutputStream)
        outputStream.writeObject(this)

        outputStream.close()
        byteArrayOutputStream.close()

        val byteArrayInputStream = ByteArrayInputStream(byteArrayOutputStream.toByteArray())
        val inputStream = ObjectInputStream(byteArrayInputStream)
        val copiedList = inputStream.readObject()

        inputStream.close()
        byteArrayInputStream.close()
        return copiedList as T
    }
    return this
}
```

5.kt对于data或者自定义接口的class，可以使用反射+拓展函数

依赖kotlin反射api
```Groovy
implementation "org.jetbrains.kotlin:kotlin-reflect:$kotlin_version"
```
```kotlin
fun <T : Any> T.deepCopy(): T {
    val cls = this::class
    if (cls.isData) {
        val dataClassCopyMethod = cls.java.declaredMethods.first { copyMethod ->
            copyMethod.name == "copy"
        }
        val params = cls.primaryConstructor?.parameters?.map { parameter ->
            val value = (this::class as KClass<T>).memberProperties.first {
                it.name == parameter.name
            }.get(this)

            if (value is DeepCopyable<*>) {
                value.deepCopyFun()
            } else if (value != null && value::class.isData) {
                value.deepCopy()
            } else {
                value
            }
        }?.toTypedArray()
        return if (params == null) {
            dataClassCopyMethod.invoke(this)
        } else {
            dataClassCopyMethod.invoke(this, *params)
        } as T
    }
    return this
}
```