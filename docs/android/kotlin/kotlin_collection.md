## list集合
listOf：生成不可变集合
```kotlin
val list = listOf("a", "b", "c")
//println(list.get(3))//报下标越界
//println(list[3]))//报下标越界
println(list.getOrElse(3) { "Nav" })
println(list.getOrNull(3) ?: "Nav")
```
<font color="#dd0000">注：[]是运算符重载</font>

getOrElse：如果下标越界返回lambda里自定义内容能够

getOrNull：如果下标越界返回null，可结合空合并使用

mutableListOf：生成可变集合
```kotlin
val mutableList = mutableListOf("a", "b", "c")
mutableList.add("d")
mutableList.remove("b")
println(mutableList)
```
相互转化
```kotlin
val mutableChangeToList: List<String> = mutableList.toList()
val listChangeMutable: List<String> = list.toMutableList()
```
此处toList()和toMutableList()可省略

操作符重载运用
```kotlin
val list = MutableList(5) { "$it A" }
list.add(1, "a")
list += "c"
list -= "d"
list.removeIf { it.contains("A") }
println(list)
```
MutableList(5)控制初始化个数，且赋默认值

+=其实就是add("c")

removeIf：移除满足条件的项
### 遍历
```kotlin
for (s in list) {
    print(s)
}
list.forEach {
    print(it)
}
list.forEachIndexed { index, item ->
    print("$index,$item ")
}
```
### 解构体
```kotlin
val list = listOf("a", "b", "c")
val (_, v2, v3) = list
println("v2: $v2, v3: #v3")
```
其中_表示不需要第一个元素

## set集合
```kotlin
val set = setOf("a", "b", "3")
set.elementAt(2)
set.elementAtOrElse(2) { "" }
set.elementAtOrNull(2)
```
和list使用类似，但没有[]运算符重载
```kotlin
val set = mutableSetOf("a", "a", "b", "c")
set += "d"
set -= "f"
```
list集合去重
```kotlin
var list = listOf("a", "a", "b", "c")
list = list.toSet().toList()
list = list.distinct()
```
distinct源码就是：toMutableSet().toList()

## map集合
```kotlin
var map = mapOf(1 to "a", 2 to "b", 3 to("c"), Pair(4, "d"))
获取值
println(map[3])
println(map.get(3))
//println(map.getValue(3))会崩溃
println(map.getOrDefault(5, "e"))
println(map.getOrElse(4) { "d" })
```
### 遍历
```kotlin
val map = mapOf(1 to "a", 2 to "b", 3 to("c"))
for (entry in map) {
    println("${entry.key} , ${entry.value}")
}
map.forEach { (key, value) ->
    println("$key , $value")
}
map.forEach {
    println("${it.key} , ${it.value}")
}
```
### 赋值
```kotlin
val map = mutableMapOf(1 to "a", 2 to "b", 3 to ("c"))
map += 4 to "d"
map[5] = "e"
map.put(5, "e")//和上面一种一样，推荐上面
map.getOrPut(5) { "w" }//如果存在则获取原来的值e
map.getOrPut(6) { "f" }//如果不存在则添加并返回结果f
```