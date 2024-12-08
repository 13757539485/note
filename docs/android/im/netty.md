```kotlin
MediaScannerConnection.scanFile(
    ProxyApplication.getINSTANCE(),
    arrayOf(imageMsg),
    arrayOf("image/jpeg")
) { path: String?, uri: Uri? ->
    // 图片扫描完毕，用来保证图片写入完整后继续其他操作
}
```
netty可以与Socket混合使用，即client是netty编写，server是socket编写，反之亦可

### 添加依赖
```kts
dependencies {
    api ("io.netty:netty-all:5.0.0.Alpha2")
}
```

### 封装的工具
#### 服务端
[com/hfc/netty/StringServerHandler](./code/src/com/hfc/netty/StringServerHandler.kt)

[com/hfc/netty/StringServerListener](./code/src/com/hfc/netty/StringServerListener.kt)
#### 客户端
[com/hfc/netty/StringClientHandler](./code/src/com/hfc/netty/StringClientHandler.kt)

[com/hfc/netty/StringClientListener](./code/src/com/hfc/netty/StringClientListener.kt)

#### 基类
[com/hfc/netty/base/BaseHandler](./code/src/com/hfc/netty/base/BaseHandler.kt)

[com/hfc/netty/base/SimpleChannelHandler](./code/src/com/hfc/netty//base/SimpleChannelHandler.kt)

[com/hfc/netty/base/SimpleListener](./code/src/com/hfc/netty/base/SimpleListener.kt)

### 简单使用
StringServerHandler和StringClientHandler是基于String消息封装，消息解析是通过换行符

#### server端
启动服务，参数port不传默认1235
```kotlin
StringServerHandler.startServer()
```
关闭服务
```kotlin
StringServerHandler.close()
```
设置监听
```kotlin
StringServerHandler.registerListener(object : StringServerListener {
    override fun channelActive() {
        lifecycleScope.launch {
            // 建立连接完成
        }
    }

    override fun channelInActive(msg: String) {
        lifecycleScope.launch {
            // 断开连接
        }
    }

    override fun messageReceived(msg: String) {
        lifecycleScope.launch {
            // 收到消息
        }
    }
})
```
发送消息
```kotlin
StringServerHandler.sendMsg(msg)
```
#### client端
启动服务，参数ip地址，必传String类型，port不传默认1235
```kotlin
StringClientHandler.startClient(ipStr)
```
关闭服务
```kotlin
StringClientHandler.close()
```
设置监听
```kotlin
StringClientHandler.registerListener(object : StringClientHandler {
    override fun channelActive() {
        lifecycleScope.launch {
            // 建立连接完成
        }
    }

    override fun channelInActive(msg: String) {
        lifecycleScope.launch {
            when (msg) {
                "ConnectException" -> {
                    // 服务器端未启动
                }
                else -> {
                    // 服务器端关闭
                }
            }
}
    }

    override fun messageReceived(msg: String) {
        lifecycleScope.launch {
            // 收到消息
        }
    }
})
```
发送消息
```kotlin
StringClientHandler.sendMsg(msg)
```

### 自定义消息体
以传递bitmap和Stirng为例

1. Listener接口编写，继承SimpleListener
```kotlin
fun messageReceived(bitmap: Bitmap)
fun messageReceived(msg: String)
```

2. 编写Handler，继承BaseHandler<上面Listener>，编写启动函数和发送消息函数
```kotlin
fun startServer(port: Int = 1234) {
    buildTcpServer(port) { socketChannel ->
        socketChannel.pipeline().addLast(ByteArrayMessageEncoder())
        socketChannel.pipeline().addLast(ByteArrayMessageDecoder())
        socketChannel.pipeline()
            .addLast(object :
                SimpleChannelHandler<ByteArrayMessage, ByteArrayServerListener>(listener, this) {
                override fun messageReceived(
                    ctx: ChannelHandlerContext,
                    msg: ByteArrayMessage
                ) {
                    when (msg.type) {
                        0 -> {
                            //转成Bitmap
                        }

                        1 -> {
                            //转成String
                        }
                    }
                }
            })
        }
    }

    fun sendMsg(msg: ByteArrayMessage) {
        checkHandlerContext {
            mainScope.launch {
                withContext(Dispatchers.IO) {
                    it.writeAndFlush(msg)
                }
            }
        }
    }
```
具体见
#### 自定义解码器
[com/hfc/netty/decoder/ByteArrayMessageDecoder](./code/src/com/hfc/netty/decoder/ByteArrayMessageDecoder.kt)
#### 自定义编码器
[com/hfc/netty/encoder/ByteArrayMessageEncoder](./code/src/com/hfc/netty/encoder/ByteArrayMessageEncoder.kt)
#### 自定义数据包
[com/hfc/netty/pack/ByteArrayMessage](./code/src/com/hfc/netty/pack/ByteArrayMessage.kt)
#### 自定义客户端
[com/hfc/netty/ByteArrayClientHandler](./code/src/com/hfc/netty/ByteArrayClientHandler.kt)

[com/hfc/netty/ByteArrayClientListener](./code/src/com/hfc/netty/ByteArrayClientListener.kt)
#### 自定义服务端
[com/hfc/netty/ByteArrayServerHandler](./code/src/com/hfc/netty/ByteArrayServerHandler.kt)

[com/hfc/netty/StringServerHandler](./code/src/com/hfc/netty/ByteArrayServerListener.kt)

发送图片字节
```kotlin
private val imgs = listOf(R.mipmap.test1, R.mipmap.test2, R.mipmap.test3)
lifecycleScope.launch {
    val bitmapsFlow: List<ByteArrayMessage> = imgs.asFlow()
        .map { resourceId ->
            BitmapFactory.decodeResource(resources, resourceId)
        }.map { bitmap ->
            val baos = ByteArrayOutputStream().apply {
                bitmap.compress(Bitmap.CompressFormat.PNG, 100, this)
            }
            val byte = baos.toByteArray()
            ByteArrayMessage(byte, byte.size)
        }
        .flowOn(Dispatchers.IO)
        .toList()
    AlbumClientHandler.sendMsg(bitmapsFlow)
}
```

### 全新封装
#### 自定义解码器
[CustomMessageDecoder](./code/src/com/hfc/netty/decoder/CustomMessageDecoder.kt)
#### 自定义编码器
[CustomMessageEncoder](./code/src/com/hfc/netty/encoder/CustomMessageEncoder.kt)
#### 自定义数据包
[CustomMessage](./code/src/com/hfc/netty/pack/CustomMessage.kt)
#### 自定义客户端
[NettyClient](./code/src/com/hfc/netty/NettyClient.kt)

[ClientHandler](./code/src/com/hfc/netty/ClientHandler.kt)
#### 自定义服务端
[NettyServer](./code/src/com/hfc/netty/NettyServer.kt)

[ServerHandler](./code/src/com/hfc/netty/ServerHandler.kt)
#### 监听回调
[IHandlerCallback](./code/src/com/hfc/netty/IHandlerCallback.kt)

### 自动连接功能
自动获取ip：

[NsdManager](./nsdmanager.md)

Server端
```kotlin
class NsdServerActivity : AppCompatActivity() {
    private val nettyServer by lazy { NettyServer() }
    private val binding by lazy { ActivityNsdServerBinding.inflate(layoutInflater) }
    private val mainScope by lazy { MainScope() }
    private val devices by lazy { mutableListOf<String>() }

    private val nettyCallback by lazy {
        object : IHandlerCallback {
            override fun channelActive(deviceName: String) {
                mainScope.launch {
                    if (!devices.contains(deviceName)) {
                        devices.add(deviceName)
                    }
                    binding.txtDevice.text = "设备：$devices"
                }
            }

            override fun channelInActive(deviceName: String, extraMsg: String) {
                if (deviceName == "") {
                    Log.d(
                        "ServerHandler",
                        "ServerHandler connect error"
                    )
                    return
                }
                mainScope.launch {
                    if (devices.contains(deviceName)) {
                        devices.remove(deviceName)
                    }
                    binding.txtDevice.text = "设备：$devices"
                }
            }

            override fun onReceive(msg: CustomMessage) {
                mainScope.launch {
                    binding.txtMsg.text = "来自${msg.name}的消息${msg.content},类型为${msg.type}"
                    if (msg.bitmap != null) {
                        binding.imgShow.setImageBitmap(msg.bitmap)
                    }
                }
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(binding.root)
        NsdExtManager.init(this)
        binding.btnNsdConnect.setOnCheckedChangeListener { buttonView, isChecked ->
            if (isChecked) {
                NsdExtManager.startServer(1222) { isRegistered: Boolean, msg: String ->
                    if (isRegistered) {
                        nettyServer.start(1222, nettyCallback)
                    } else {
                        nettyServer.stop()
                    }
                }
            } else {
                NsdExtManager.stopServer()
                nettyServer.stop()
            }
        }
        binding.btnSendStr.setOnClickListener {
            nettyServer.sendMessageByName(CustomMessage("samsung", "music", "action:play", null))
        }
        binding.btnSendAll.setOnClickListener {
            nettyServer.sendMessageByAll(CustomMessage("Server","controller", "action:volume_up", null))
        }
        binding.btnSendBitmap.setOnClickListener {
            nettyServer.sendMessageByName(CustomMessage("samsung", "guess", "",
                BitmapFactory.decodeResource(resources, R.mipmap.test1)))
        }
    }
}
```
Client端
```kotlin
class NsdClientActivity : AppCompatActivity() {
    private val binding by lazy { ActivityNsdClientBinding.inflate(layoutInflater) }
    private val nsdClient by lazy { NettyClient() }
    private val mainScope by lazy { MainScope() }
    private val nettyCallback by lazy { object : IHandlerCallback {
        override fun channelActive(deviceName: String) {
            mainScope.launch {
                binding.txtMsg.text = "建立连接"
                binding.btnNsdConnect.isChecked = true
            }
        }

        override fun channelInActive(deviceName: String, extraMsg: String) {
            mainScope.launch {
                binding.txtMsg.text = "断开连接"
                binding.btnNsdConnect.isChecked = false
            }
        }

        override fun onReceive(msg: CustomMessage) {
            mainScope.launch {
                binding.txtMsg.text = "来自${msg.name}的消息${msg.content},类型为${msg.type}"
                if (msg.bitmap != null) {
                    binding.imgShow.setImageBitmap(msg.bitmap)
                }
            }
        }
    } }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(binding.root)
        NsdExtManager.init(this)
        binding.btnNsdConnect.setOnCheckedChangeListener { button, isChecked ->
            if (!button.isPressed) return@setOnCheckedChangeListener
            if (isChecked) {
                NsdExtManager.startClient { ip, port ->
                    nsdClient.start(ip, port, nettyCallback)
                }
            } else {
                NsdExtManager.stopClient()
                nsdClient.stop()
            }
        }
        binding.btnSendStr.setOnClickListener {
            nsdClient.sendMessage(CustomMessage(Build.BRAND, "music", "action:pause", null))
        }
        binding.btnSendBitmap.setOnClickListener {
            nsdClient.sendMessage(CustomMessage(Build.BRAND, "guess", "",
                BitmapFactory.decodeResource(resources, R.mipmap.test3)))
        }
    }
}
```