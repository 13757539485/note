LocalSocket是安卓特有，剔除里Socket网络部分

app中一般无法使用，java层使用

### 封装好的工具
[LocalSocketClientHandler](./code/LocalSocketClientHandler.java)

[Client](./code/Client.java)

[LocalSocketServerHandler](./code/LocalSocketServerHandler.java)

[MsgListener](./code/MsgListener.java)

### client端

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

### server端
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