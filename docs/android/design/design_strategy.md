### 简介
将算法或者策略抽象出来提供统一接口，允许外部动态替换算法或策略
### 使用场景
1. 针对同一类问题的多种处理方式，仅具体行为不同时
2. 需要安全封装多种同一类操作时
3. 出现同一抽象类有多个子类，且需要使用if else或when来选择具体子类时
if else或when(switch)不遵循开闭原则
### 案例
```kotlin
class PriceCalculator {// 计算类
    private fun busPrice(){}
    private fun subWayPrice(){}
    fun calPrice(traffic: Traffic) {
        if (traffic == Traffic.BUS) {busPrice()}
        else if (traffic == Traffic.SUBWAY){subWayPrice()}
    }
}
enum class Traffic{// 出行类型
    BUS,SUBWAY
}

fun main() {
    PriceCalculator().calPrice(Traffic.BUS)
    PriceCalculator().calPrice(Traffic.SUBWAY)
}
```
**问题和缺陷**

1. PriceCalculator类违反单一职责，需要计算不同类型的出行价格
2. 违反开闭原则出现if else臃肿，拓展差，添加一种出现类型需要添加else分支以及价格计算方式

**策略模式解决** 

抽象统一接口和具体实现
```kotlin
interface CalculateStrategy {
    fun calPrice()
}
class BusStrategy : CalculateStrategy {
    override fun calPrice() {}
}

class SubWayStrategy : CalculateStrategy {
    override fun calPrice() {}
}
```
调用
```kotlin
class PriceCalculator(var strategy: CalculateStrategy) {
    fun calPrice() {
        strategy.calPrice()
    }
}
fun main() {
    PriceCalculator(BusStrategy()).calPrice()
    PriceCalculator(SubWayStrategy()).calPrice()
}
```
### Android源码中的策略模式
动画插值器

### 优缺点
优点：

1. 结构清晰，使用简单直观
2. 耦合度较低，扩展方便
3. 操作封装比较彻底，数据更安全
缺点：

1. 随着策略增加，子类变得繁多