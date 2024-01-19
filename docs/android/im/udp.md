
### 服务端

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

### 客户端

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