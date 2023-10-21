### field和property区别
field：字段
property：属性
property = field + get/set方法
```java
B.class.getDeclaredFields(); // field获取
Introspector.getBeanInfo(B.class); // property获取
```

### 静态属性和静态方法
可以被子类继承

静态方法不能被重写

两者都可以被覆盖
```java
class Parent {
    public static int a = 0;

    public static void findA() {
        System.out.println("parent");
    }
}

class Child extends Parent {
}

Child.findA();//parent
System.out.println(Child.a);//0
```
子类修改
```java
class Child extends Parent {
    public static int a = 1;

    public static void findA() {
        System.out.println("child");
    }
}
Child.findA();//child
System.out.println(Child.a);//1
```