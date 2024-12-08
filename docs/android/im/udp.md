
### 服务端
```kotlin
object UdpTask {
    private val timer by lazy { Timer() }
    private val timerTask by lazy {
        object : TimerTask() {
            override fun run() {
                Log.d("yuli", "schedule to send ip")
                try {
                    udpSocket.send(sendPacket)
                } catch (e: Exception) {
                    Log.e("yuli", "run: ${e.message}")
                }
            }
        }
    }
    private val udpSocket by lazy {
        DatagramSocket().apply { broadcast = true }
    }
    private val helloMsg by lazy { "hello".toByteArray() }

    private val sendPacket by lazy {
        DatagramPacket(
            helloMsg,
            helloMsg.size,
            InetAddress.getByName("255.255.255.255"),
            12344
        )
    }

    fun sayHelloForJava() {
        MainScope().launch {
            sayHello()
        }
    }

    suspend fun sayHello(){
        try {
            sayHelloInner()
        } catch (e: Exception) {
            e.printStackTrace()
            Log.e("yuli", "run: ${e.message}")
        } finally {
            udpSocket.close()
            Log.d("yuli", "close socket")
        }
    }

    private suspend fun sayHelloInner() {
        withContext(Dispatchers.IO) {
            udpSocket.send(sendPacket)
            timer.scheduleAtFixedRate(timerTask, 1000, 1000)
        }

        withContext(Dispatchers.IO) {
            Log.d("yuli", "ready while to receive ok")
            while (true) {
                val receiveData = ByteArray(1024)
                val receivePacket = DatagramPacket(receiveData, receiveData.size)

                udpSocket.receive(receivePacket)
                val responseMessage = String(receivePacket.data, 0, receivePacket.length)
                Log.d("yuli", "receive: $responseMessage")
                if ("ok" == responseMessage) {
                    timer.cancel()
                    break
                }
            }
        }
    }

    fun stop(){
        timer.cancel()
        try {
            udpSocket.close()
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }
}
```
### 客户端
```kotlin
object UdpTask {
    private val udpSocket by lazy {
        DatagramSocket(12344).apply { broadcast = true }
    }

    fun receiveHelloForJava(callback: (String) -> Unit) {
        MainScope().launch {
            receiveHello(callback)
        }
    }

    suspend fun receiveHello(callback: (String) -> Unit) {
        try {
            receiveHelloInner(callback)
        } catch (e: Exception) {
            e.printStackTrace()
        } finally {
            udpSocket.close()
            Log.d("yuli", "close socket")
        }
    }

    private suspend fun receiveHelloInner(callback: (String) -> Unit) {
        withContext(Dispatchers.IO) {
            while (true) {
                val receiveData = ByteArray(1024)
                val receivePacket = DatagramPacket(receiveData, receiveData.size)

                udpSocket.receive(receivePacket)
                val ip = receivePacket.address.hostAddress ?: ""
                Log.e("yuli", "recvIp: $ip")
                if (ip.isNotEmpty()) {
                    callback(ip)
                    val confirm = "ok".toByteArray()
                    val packet = DatagramPacket(confirm, confirm.size,
                        receivePacket.address,
                        receivePacket.port)
                    udpSocket.send(packet)
                    Log.e("yuli", "send ok")
                    break
                }
            }
        }
    }
}
```

### 自动获取ip功能
```kotlin
object UdpExtManager {
    private const val TAG = "UdpExtManager"
    private val isClientRunning: AtomicBoolean = AtomicBoolean(false)
    private val isServerRunning: AtomicBoolean = AtomicBoolean(false)
    private val ioScope = CoroutineScope(Dispatchers.IO)
    private var clientScope: Job? = null
    private var serverScope: Job? = null

    fun startClient() {
        clientScope = ioScope.launch {
            try {
                isClientRunning.set(true)
                val group = InetAddress.getByName("239.255.255.250")
                val socket = MulticastSocket(1998)
                socket.joinGroup(group)
                val buffer = ByteArray(1024)
                val packet = DatagramPacket(buffer, buffer.size)
                Log.d(TAG, "Received SSDP message start: ")
                while (isClientRunning.get()) {
                    socket.receive(packet)
                    val received =
                        String(packet.data, 0, packet.length)
                    Log.d(TAG, "Received SSDP message: $received")
                }
                socket.close()
            } catch (e: IOException) {
                e.printStackTrace()
                isClientRunning.set(false)
            }
        }
    }

    fun stopClient() {
        isClientRunning.set(false)
        clientScope?.cancel()
    }

    fun startServer(ipAddress: String?) {
        Log.d(TAG, "get IP address $ipAddress")
        if (ipAddress == null) {
            return
        }
        val message: String = ipAddress

        // 使用UDP多播发送消息
        serverScope = ioScope.launch {
            isServerRunning.set(true)
            try {
                val group =
                    InetAddress.getByName(replaceLastWith255(ipAddress))
                val socket = DatagramSocket()
                socket.broadcast = true
                val msgBytes = message.toByteArray()
                val packet =
                    DatagramPacket(msgBytes, msgBytes.size, group, 1998)
                while (isServerRunning.get()) {
                    socket.send(packet)
                    delay(10000L)
                }
                Log.d(TAG, "SSDP broadcast sent.")
                socket.close()
            } catch (e: Exception) {
                e.printStackTrace()
                Log.d(TAG, "Failed to send SSDP broadcast.")
                isServerRunning.set(false)
            }
        }
    }

    fun stopServer() {
        isServerRunning.set(false)
        serverScope?.cancel()
    }

    private fun replaceLastWith255(ipAddress: String): String? {
        val segments = ipAddress.split("\\.".toRegex()).dropLastWhile { it.isEmpty() }
            .toTypedArray()
        if (segments.size != 4) {
            Log.d(TAG, "Invalid IP address format: $ipAddress")
        }
        segments[3] = "255"
        return java.lang.String.join(".", *segments)
    }
}
```