```kotlin
MediaScannerConnection.scanFile(
    ProxyApplication.getINSTANCE(),
    arrayOf(imageMsg),
    arrayOf("image/jpeg")
) { path: String?, uri: Uri? ->
    // 图片扫描完毕，用来保证图片写入完整后继续其他操作
}
```
### 添加依赖
```kts
dependencies {
    api ("io.netty:netty-all:5.0.0.Alpha2")
}
```

### 服务端
```kotlin
object ServerHandler {
    private var channel: Channel? = null
    private var workerGroup: EventLoopGroup? = null
    private var listener: ServerListener? = null
    private var channelHandlerContext: ChannelHandlerContext? = null

    fun registerListener(listener: ServerListener) {
        ServerHandler.listener = listener
    }

    fun unregisterListener() {
        listener = null
    }

    fun startServerJava(port: Int = 1234) {
        MainScope().launch {
            startServer(port)
        }
    }

    suspend fun startServer(port: Int = 1234) {
        if (channel != null && channel!!.isActive) {
            println("server already start")
            return
        }
        withContext(Dispatchers.IO) {
            workerGroup = NioEventLoopGroup()
            val bootstrap = ServerBootstrap()
            bootstrap.group(workerGroup)
                .channel(NioServerSocketChannel::class.java)
                .option(ChannelOption.SO_REUSEADDR, true)
                .option(ChannelOption.SO_BACKLOG, 1024)
                .option(ChannelOption.SO_KEEPALIVE, true)
                .option(ChannelOption.CONNECT_TIMEOUT_MILLIS, 30000)
                .childOption(ChannelOption.SO_KEEPALIVE, true)
                .childOption(ChannelOption.SO_RCVBUF, 1024 * 1024)
                .childOption(ChannelOption.SO_SNDBUF, 1024 * 1024)
                .childHandler(object : ChannelInitializer<SocketChannel>() {
                    @Throws(Exception::class)
                    override fun initChannel(socketChannel: SocketChannel) {
                        socketChannel.pipeline().addLast(ByteArrayMessageEncoder())
                        socketChannel.pipeline().addLast(ByteArrayMessageDecoder())
                        socketChannel.pipeline()
                            .addLast(object : SimpleChannelInboundHandler<ByteArrayMessage>() {
                                override fun messageReceived(
                                    ctx: ChannelHandlerContext,
                                    msg: ByteArrayMessage
                                ) {
                                    println("msg $msg ${Thread.currentThread().name}")
                                    when (msg.type) {
                                        0 -> {
                                            listener?.messageReceived(
                                                BitmapFactory.decodeByteArray(
                                                    msg.data,
                                                    0,
                                                    msg.length
                                                )
                                            )
                                        }

                                        1 -> {
                                            listener?.messageReceived(String(msg.data))
                                        }
                                    }
                                }

                                override fun channelActive(ctx: ChannelHandlerContext?) {
                                    super.channelActive(ctx)
                                    channelHandlerContext = ctx
                                    listener?.channelActive()
                                }

                                override fun channelInactive(ctx: ChannelHandlerContext?) {
                                    super.channelInactive(ctx)
                                    println("server channelInactive")
                                }
                            })
                    }
                })
            try {
                val future = bootstrap.bind(port).sync()
                channel = future.channel()
                println("server running")
                channel!!.closeFuture().sync()
            } catch (e: InterruptedException) {
                e.printStackTrace()
            }
        }
    }

    fun sendMsgJava(msg: ByteArrayMessage) {
        MainScope().launch {
            sendMsg(msg)
        }
    }

    suspend fun sendMsg(msg: ByteArrayMessage) {
        if (channelHandlerContext == null) {
            println("channel is null, please check")
            return
        }
        val channel = channelHandlerContext!!
        withContext(Dispatchers.IO) {
            channel.writeAndFlush(msg)
        }
    }

    fun sendStrJava(msg: String) {
        MainScope().launch {
            sendStr(msg)
        }
    }

    suspend fun sendStr(msg: String) {
        if (channelHandlerContext == null) {
            println("channel is null, please check")
            return
        }
        val channel = channelHandlerContext!!
        withContext(Dispatchers.IO) {
            val bytes = msg.toByteArray()
            channel.writeAndFlush(ByteArrayMessage(bytes, bytes.size, 1))
        }
    }

    fun closeServer() {
        MainScope().launch {
            withContext(Dispatchers.IO) {
                workerGroup?.shutdownGracefully()
                channel?.closeFuture()?.syncUninterruptibly()
                channel = null
                channelHandlerContext?.close()?.sync()
                channelHandlerContext = null
            }
            println("server close")
        }
    }
}

interface ServerListener {
    fun channelActive()

    fun messageReceived(bitmap: Bitmap)

    fun messageReceived(msg: String)
}
```

### 客户端
```kotlin
object AlbumClientHandler {
    private var workerGroup: EventLoopGroup? = null
    private var channel: Channel? = null
    private var listener: AlbumClientListener? = null
    private var channelHandlerContext: ChannelHandlerContext? = null

    fun registerListener(listener: AlbumClientListener) {
        this.listener = listener
    }

    fun unregisterListener() {
        listener = null
    }

    fun startAlbumClientJava(ip: String, port: Int = 1234) {
        MainScope().launch {
            startAlbumClient(ip, port)
        }
    }

    suspend fun startAlbumClient(ip: String, port: Int = 1234) {
        if (channel != null && channel!!.isActive) {
            println("client already start")
            return
        }
        withContext(Dispatchers.IO) {
            workerGroup = NioEventLoopGroup()
            val bootstrap = Bootstrap()
            bootstrap.group(workerGroup)
                .channel(NioSocketChannel::class.java)
                .handler(object : ChannelInitializer<SocketChannel>() {
                    @Throws(Exception::class)
                    override fun initChannel(socketChannel: SocketChannel) {
                        socketChannel.pipeline().addLast(ByteArrayMessageEncoder())
                        socketChannel.pipeline().addLast(ByteArrayMessageDecoder())
                        socketChannel.pipeline()
                            .addLast(object : SimpleChannelInboundHandler<ByteArrayMessage>() {
                                override fun messageReceived(
                                    ctx: ChannelHandlerContext?,
                                    msg: ByteArrayMessage
                                ) {
                                    println("msg $msg")
                                    when (msg.type) {
                                        0 -> {
                                            listener?.messageReceived(
                                                BitmapFactory.decodeByteArray(
                                                    msg.data,
                                                    0,
                                                    msg.length
                                                )
                                            )
                                        }

                                        1 -> {
                                            listener?.messageReceived(String(msg.data))
                                        }
                                    }
                                }

                                override fun channelActive(ctx: ChannelHandlerContext?) {
                                    super.channelActive(ctx)
                                    channelHandlerContext = ctx
                                    listener?.channelActive()
                                }

                                override fun channelInactive(ctx: ChannelHandlerContext?) {
                                    super.channelInactive(ctx)
                                    listener?.channelInActive("")
                                }
                            })
                    }
                })
            try {
                val future = bootstrap.connect(ip, port).sync()
                channel = future.channel()
                println("client running")
                future.channel().closeFuture().sync()
            } catch (e: Exception) {
                if (e is ConnectException || e is NoRouteToHostException) {
                    println("server is not running: ${e.message}")
                    listener?.channelInActive("ConnectException")
                } else {
                    e.printStackTrace()
                }
            }
        }
    }

    fun sendMsgJava(msg: List<ByteArrayMessage>) {
        MainScope().launch {
            sendMsg(msg)
        }
    }

    suspend fun sendMsg(msg: List<ByteArrayMessage>) {
        if (channelHandlerContext == null) {
            println("channel is null, please check")
            return
        }
        val channel = channelHandlerContext!!
        withContext(Dispatchers.IO) {
            msg.forEach {
                channel.writeAndFlush(it)
            }
        }
    }

    fun sendStrJava(msg: String) {
        MainScope().launch {
            sendStr(msg)
        }
    }

    suspend fun sendStr(msg: String) {
        if (channelHandlerContext == null) {
            println("channel is null, please check")
            return
        }
        val channel = channelHandlerContext!!
        withContext(Dispatchers.IO) {
            val bytes = msg.toByteArray()
            channel.writeAndFlush(ByteArrayMessage(bytes, bytes.size, 1))
        }
    }

    fun closeClient() {
        MainScope().launch {
            withContext(Dispatchers.IO) {
                workerGroup?.shutdownGracefully()
                channel?.closeFuture()?.sync()
                channel = null
                channelHandlerContext?.close()?.sync()
                channelHandlerContext = null
                println("close client")
            }
        }
    }
}

interface ClientListener {
    fun channelActive()

    fun channelInActive(msg: String)

    fun messageReceived(bitmap: Bitmap)

    fun messageReceived(msg: String)
}
```

### 解码器
```kotlin
class ByteArrayMessageDecoder: ByteToMessageDecoder() {
    override fun decode(ctx: ChannelHandlerContext?, byteBuf: ByteBuf, out: MutableList<Any>) {
        println("start decode")
        if (byteBuf.readableBytes() < 4) {
            return
        }
        byteBuf.markReaderIndex()
        val length = byteBuf.readInt()
        if (byteBuf.readableBytes() < length) {
            byteBuf.resetReaderIndex()
            return
        }
        val data = ByteArray(length)
        byteBuf.readBytes(data);
        val messageType = byteBuf.readByte().toInt()
        out.add(ByteArrayMessage(data, length, messageType))
        println("end decode")
    }
}
```

### 编码器
```kotlin
class ByteArrayMessageEncoder: MessageToByteEncoder<ByteArrayMessage>() {
    override fun encode(ctx: ChannelHandlerContext?, msg: ByteArrayMessage, out: ByteBuf) {
        println("start encode")
        out.writeInt(msg.length)
        out.writeBytes(msg.data)
        out.writeByte(msg.type)
        println("end encode")
    }
}
```

### 数据包
```kotlin
data class ByteArrayMessage(var data: ByteArray, var length: Int, var type: Int = 0) {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as ByteArrayMessage

        if (!data.contentEquals(other.data)) return false
        if (length != other.length) return false

        return true
    }

    override fun hashCode(): Int {
        var result = data.contentHashCode()
        result = 31 * result + length
        return result
    }
}
```