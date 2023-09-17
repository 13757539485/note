### field和property区别
field：字段
property：属性
property = field + get/set方法
```java
B.class.getDeclaredFields(); // field获取
Introspector.getBeanInfo(B.class); // property获取
```