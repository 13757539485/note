## 项目简介
自定义服务，用来传输车机和手机之间数据

### 项目结构

#### localsocket

模块(library)类型，封装LocalSocket的使用，当前使用在proxy项目以及framework中

注：framework并不直接依赖此项目，而是复制了LocalSocketClientHandle.java文件到frameworks/base/core/java/android/hardware/目录

使用方式

服务端

创建服务端对象
```kotlin
private val localSocketServerHandler by lazy { LocalSocketServerHandler() }
```
启动服务端
```kotlin
localSocketServerHandler.start("xxx")
```
发送消息
```kotlin
localSocketServerHandler.send("xxx")
```
设置监听
```kotlin
localSocketServerHandler.setMsgListener(object : MsgListener {
    override fun onMsgReceive(msg: String, client: LocalSocket) {
        //TODO：处理接收到消息
    }

    override fun onConnect(client: LocalSocket) {
        //TODO：有客户端连接
    }

    override fun onClose(client: LocalSocket) {
        //TODO：有客户端关闭
    }
})
```
关闭服务端
```kotlin
localSocketServerHandler.closeServer()
```
客户端

创建客户端对象
```kotlin
private val clientHandler by lazy { LocalSocketClientHandler() }
```
启动客户端
```kotlin
clientHandler.start("xxx")//和服务端保持相同名字
```
发送消息
```kotlin
clientHandler.send("xxx")
```
设置监听
```kotlin
localSocketServerHandler.setMsgListener(object : MsgListener {
    override fun onMsgReceive(msg: String, client: LocalSocket) {
        //TODO：处理接收到消息
    }

    override fun onConnect(client: LocalSocket) {
        //TODO：连接成功
    }

    override fun onClose(client: LocalSocket) {
        //TODO：客户端关闭
    }
})
```
关闭客户端
```kotlin
clientHandler.closeClient()
```

#### netty
模块(library)类型，封装Netty的通用类，具体使用见proxy项目

#### netty_client和netty_server
应用(application)类型，用来测试localsocket和netty的基本使用

#### proxy
模块、应用类型，将build.gradle_d.kts为模块，build.gradle.kts为应用(默认)，暂定方案是改文件名的形式切换

此模块正式使用会编译成系统app，具体见Android.mk文件

对netty的扩展使用SensorClientHandler和SensorServerHandler

~~## 传感器实现方案1(普通应用生效，游戏无效，废除)~~
### 源码
RK3588车机端

framework层修改点

frameworks/base/core/java/android/hardware/SystemSensorManager.java

主要修改对传感器监听数据和反监听数据接口进行拦截处理，作为开启和关闭手机端传感器的入口

frameworks/base/core/java/android/hardware/LocalSocketClientHandle.java

主要是封装LocalSocket的api作为client端使用，具体调用在SystemSensorManager中

系统app层：proxy项目(CariadProxy)，此项目没有界面，是一个服务，作为连通车机framework和手机端数据桥梁

手机端

app层: netty_client项目，主要是通过netty将传感器数据传递到proxy项目中

### 整体流程
1. RK开机启动proxy中LocalSocket服务端以及Netty服务端
2. 手机端启动Netty客户端并连接
3. RK三方app监听传感器
4. RK fw(SystemSensorManager)被创建
5. RK SystemSensorManager创建LocalSocket客户端并连接
6. RK三方app调用registerListenerImpl
7. RK LocalSocket客户端发送enable消息，携带传感器类型
8. 手机端收到消息开启对应传感器监听并将数据通过Netty客户端发送到RK
9. RK端Netty服务端收到消息，通过LocalSocket服务端分发消息
10. RK三方app收到数据

SystemSensorManager每个app进程都会创建一个，即有多个LocalSocket客户端

流程图

![sensor_app](./img/sensor_app.png)

## 传感器实现方案2
### 源码
#### RK3588车机端

