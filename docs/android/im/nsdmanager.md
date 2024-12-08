### 自动获取ip
```kotlin
object NsdExtManager {
    private const val TAG = "NdsExtManager"
    private const val SERVICE_TYPE = "_http._tcp"
    private var nsdManager: NsdManager? = null
    private var registrationListener: RegistrationListener? = null
    private var discoveryListener: DiscoveryListener? = null
    private var resolveListener: NsdManager.ResolveListener? = null
    private var serviceInfoCallback: ServiceInfoCallback? = null
    private var serverIp: String? = null
    private var serverPort: Int = 0
    private var callback: ((ip: String, port: Int) -> Unit)? = null

    fun init(context: Context) {
        nsdManager = context.getSystemService(Context.NSD_SERVICE) as NsdManager
    }

    fun stopServer() {
        registrationListener?.let {
            nsdManager?.unregisterService(it)
        }
    }

    fun startClient(block: (ip: String, port: Int) -> Unit) {
        callback = block
        discoveryListener = object : DiscoveryListener {
            override fun onDiscoveryStarted(regType: String) {
                Log.d(TAG, "Service discovery started")
            }

            override fun onServiceFound(serviceInfo: NsdServiceInfo) {
                Log.d(TAG, "Service found: ${serviceInfo.serviceType}")
                if (serviceInfo.serviceType == "$SERVICE_TYPE.") {
                    serviceFound(serviceInfo)
                }
            }

            override fun onServiceLost(serviceInfo: NsdServiceInfo) {
                Log.d(TAG, "Service lost: " + serviceInfo.serviceName)
            }

            override fun onDiscoveryStopped(serviceType: String) {
                Log.d(TAG, "Discovery stopped: $serviceType")
            }

            override fun onStartDiscoveryFailed(serviceType: String, errorCode: Int) {
                Log.e(TAG, "Discovery failed: Error code:$errorCode")
                nsdManager!!.stopServiceDiscovery(this)
            }

            override fun onStopDiscoveryFailed(serviceType: String, errorCode: Int) {
                Log.e(TAG, "Discovery failed: Error code:$errorCode")
                nsdManager!!.stopServiceDiscovery(this)
            }
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
            serviceInfoCallback = object : ServiceInfoCallback {
                override fun onServiceInfoCallbackRegistrationFailed(errorCode: Int) {
                    Log.d(
                        TAG,
                        "onServiceInfoCallbackRegistrationFailed() called with: errorCode = $errorCode"
                    )
                }

                override fun onServiceUpdated(serviceInfo: NsdServiceInfo) {
                    Log.d(TAG, "onServiceUpdated() called with: serviceInfo = $serviceInfo")
                    getServerIp(serviceInfo)
                }

                override fun onServiceLost() {
                    Log.d(TAG, "onServiceLost() called")
                }

                override fun onServiceInfoCallbackUnregistered() {
                    Log.d(TAG, "onServiceInfoCallbackUnregistered() called")
                }
            }
        }

        resolveListener = object : NsdManager.ResolveListener {
            override fun onResolveFailed(serviceInfo: NsdServiceInfo, errorCode: Int) {
                Log.e(TAG, "Resolve failed: $errorCode")
            }

            override fun onServiceResolved(serviceInfo: NsdServiceInfo) {
                Log.d(TAG, "Service resolved: $serviceInfo")
                getServerIp(serviceInfo)
            }
        }

        nsdManager!!.discoverServices(SERVICE_TYPE, NsdManager.PROTOCOL_DNS_SD, discoveryListener)
    }

    fun stopClient() {
        discoveryListener?.let {
            nsdManager?.stopServiceDiscovery(it)
        }
    }

    private fun serviceFound(serviceInfo: NsdServiceInfo) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
            serviceInfoCallback?.let {
                try {
                    nsdManager?.registerServiceInfoCallback(
                        serviceInfo, Executors.newSingleThreadExecutor(),
                        it
                    )
                } catch (e: Exception) {
                    Log.e(TAG, "registerServiceInfoCallback error: ${e.message}")
                }
            }
        } else {
            try {
                nsdManager?.resolveService(serviceInfo, resolveListener)
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    private fun getServerIp(serviceInfo: NsdServiceInfo) {
        val host = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
            serviceInfo.hostAddresses.filterIsInstance<Inet4Address>()[0]
        } else {
            serviceInfo.host
        }
        serverIp = host.hostAddress
        serverPort = serviceInfo.port
        Log.d(TAG, "IP Address: $serverIp, Port: $serverPort")
        serverIp?.let {
            callback?.invoke(it, serverPort)
        }
    }

    fun startServer(port: Int, block: (isRegistered: Boolean, msg: String) -> Unit) {
        val serviceInfo = NsdServiceInfo()
        serviceInfo.serviceName = "${Build.BRAND}-Service"
        serviceInfo.serviceType = SERVICE_TYPE
        serviceInfo.port = port
        registrationListener = object : RegistrationListener {
            override fun onServiceRegistered(nsdServiceInfo: NsdServiceInfo) {
                Log.d(TAG, "Service registered: " + nsdServiceInfo.serviceName)
                block(true, nsdServiceInfo.serviceName)
            }

            override fun onRegistrationFailed(serviceInfo: NsdServiceInfo, errorCode: Int) {
                Log.e(TAG, "Registration failed: $errorCode")
                block(false, "${serviceInfo.serviceName} error: $errorCode")
            }

            override fun onServiceUnregistered(serviceInfo: NsdServiceInfo) {
                Log.d(TAG, "Service unregistered: " + serviceInfo.serviceName)
                block(false, serviceInfo.serviceName)
            }

            override fun onUnregistrationFailed(serviceInfo: NsdServiceInfo, errorCode: Int) {
                Log.e(TAG, "Unregistration failed: $errorCode")
            }
        }
        nsdManager!!.registerService(serviceInfo, NsdManager.PROTOCOL_DNS_SD, registrationListener)
    }
}
```
### 使用
Server端启动
```kotlin
NsdExtManager.startServer(1222) { isRegistered: Boolean, msg: String ->
    if (isRegistered) {
        
    } else {
        
    }
}
```
Server端关闭
```kotlin
NsdExtManager.stopServer()
```
Client端启动
```kotlin
NsdExtManager.startClient { ip, port ->
}
```
Client端关闭
```kotlin
NsdExtManager.stopClient()
```