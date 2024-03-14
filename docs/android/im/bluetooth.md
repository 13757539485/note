## 经典蓝牙
### 添加权限
```xml
<uses-permission android:name="android.permission.BLUETOOTH"/>
<uses-permission android:name="android.permission.BLUETOOTH_SCAN"/>
<uses-permission android:name="android.permission.BLUETOOTH_ADMIN"/>
<uses-permission android:name="android.permission.BLUETOOTH_CONNECT"/>
<uses-permission android:name="android.permission.BLUETOOTH_PRIVILEGED"
    tools:ignore="ProtectedPermissions" />
<uses-permission android:name="android.permission.BLUETOOTH_ADVERTISE"/>
<uses-permission android:name="com.google.android.things.permission.MANAGE_BLUETOOTH" />
<uses-permission android:name="android.permission.ACCESS_FINE_LOCATION"/>
```
### 动态申请权限

### 使用三方权限库[XXPermissions](../android_github.md#xx_p)
```kotlin
XXPermissions.with(this)
    .permission(Permission.ACCESS_FINE_LOCATION)
    .permission(Permission.Group.BLUETOOTH)
    .request(object : OnPermissionCallback {
        override fun onGranted(permissions: MutableList<String>, allGranted: Boolean) {
            if (!allGranted) {
                toast("获取部分权限成功，但部分权限未正常授予")
                return
            }
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                val lm: LocationManager = getSystemService(Context.LOCATION_SERVICE) as LocationManager
                if (!lm.isProviderEnabled(LocationManager.GPS_PROVIDER)){
                    Toast.makeText(this@MainActivity, "请您先开启gps,否则蓝牙不可用", Toast.LENGTH_SHORT).show()
                        // 创建指向系统定位服务设置的Intent
                        val intent = Intent(Settings.ACTION_LOCATION_SOURCE_SETTINGS)
                        // 启动该Intent，用户将被带到系统设置中的定位服务页面
                        startActivity(intent)
                } else {
                    if (!bluetoothAdapter.isEnabled) {
                        startActivity(Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE))
                    } else {
                        if (Build.MODEL != SERVER_MODEL) {
                            registerReceiver(receiver, BluetoothReceiver.markFilter())
                            scanBluetooth()
                        }
                    }
                }
            }
        }

        override fun onDenied(permissions: MutableList<String>, doNotAskAgain: Boolean) {
            if (doNotAskAgain) {
                toast("被永久拒绝授权，请手动授予定位附近权限")
                // 如果是被永久拒绝就跳转到应用权限系统设置页面
                XXPermissions.startPermissionActivity(this@MainActivity, permissions)
            } else {
                toast("获取权限失败")
            }
        }
    })
```

### 注册蓝牙广播监听
```kotlin
registerReceiver(receiver, BluetoothReceiver.markFilter())

@SuppressLint("MissingPermission")
class BluetoothReceiver(private val block: (device: BluetoothDevice?) -> Unit) : BroadcastReceiver() {
    private val TAG = "BlueToothReceiver"
    override fun onReceive(context: Context, intent: Intent) {
        when (intent.action) {
            BluetoothDevice.ACTION_ACL_CONNECTED -> Log.d(TAG, "ACTION_ACL_CONNECTED")
            BluetoothDevice.ACTION_ACL_DISCONNECTED -> Log.d(
                TAG,
                "ACTION_ACL_DISCONNECTED"
            ) // 连接断开，停止通信
            BluetoothDevice.ACTION_FOUND -> {
                // 找到设备
                val device =
                    intent.getParcelableExtra<BluetoothDevice>(BluetoothDevice.EXTRA_DEVICE)
                block(device)
            }

            BluetoothDevice.ACTION_BOND_STATE_CHANGED -> {
                // 远程设备的绑定状态发生变化
                Log.d(TAG, "ACTION_BOND_STATE_CHANGED")
                when ((intent.getParcelableExtra<Parcelable>(BluetoothDevice.EXTRA_DEVICE) as BluetoothDevice?)!!.bondState) {
                    BluetoothDevice.BOND_NONE -> {}
                    BluetoothDevice.BOND_BONDING -> {}
                    BluetoothDevice.BOND_BONDED -> {}
                }
            }
        }
    }

    companion object {
        /**
         * 设置Intent过滤器
         *
         * @return
         */
        fun markFilter(): IntentFilter {
            val filter = IntentFilter()
            filter.addAction(BluetoothDevice.ACTION_FOUND)
            filter.addAction(BluetoothDevice.ACTION_BOND_STATE_CHANGED)
            filter.addAction(BluetoothDevice.ACTION_ACL_CONNECTED)
            filter.addAction(BluetoothDevice.ACTION_ACL_DISCONNECTED)
            return filter
        }
    }
}
```

### 启动/关闭蓝牙服务端

[BtServer](./code/src/com/bt/BtServer.kt)
```kotlin
btServer = BtServer(btServerListener)
btServer!!.startServer()
btServer?.close()
```
### 启动/关闭蓝牙客户端

[BtClinet](./code/src/com/bt/BtClinet.kt)
```kotlin
btClient = BtClient(blueDev, object : BluetoothListener{
    override fun onStart() {
        MainScope().launch {
            holder.binding.blueItemStatusTv.text = "正在连接..."
        }
    }
    override fun onMsgRecv(socket: BluetoothSocket?, msg: String) {
        MainScope().launch {
            block(true, msg)
        }
    }
    override fun onError(error: String) {
        MainScope().launch {
            block(false, error)
            holder.binding.blueItemStatusTv.text = "已配对"
        }
    }
    override fun onConnected(msg: String) {
        MainScope().launch {
            holder.binding.blueItemStatusTv.text = "已连接"
        }
    }
}).also {
    MainScope().launch {
        it.startClient()
    }
}
blueAdapter?.close()
```
其中监听回调
```kotlin
val BLUE_UUID = UUID.fromString("00001101-0000-1000-8000-00805F9B34FB")
interface BluetoothListener {
    fun onStart()
    fun onMsgRecv(socket: BluetoothSocket?, msg: String)
    fun onError(error: String)
    fun onConnected(msg: String)
}
```
客户端一般是列表形式展现

每个蓝牙设备类BluetoothDevice

地址: blueDev.address

名称: blueDev.name

已配对： blueDev.bondState == BluetoothDevice.BOND_BONDED

获取列表：注册广播后回调添加设备
```kotlin
private val receiver = BluetoothReceiver {
    it?.let { blueDev ->
        if (blueDev !in blueBeans && blueDev.name != null) {
            blueAdapter?.add(blueDev)
            blueAdapter?.notifyItemInserted(blueBeans.size)
        }
    }
}
```
启动蓝牙扫描后，广播才会回调
```kotlin
fun scanBluetooth() {
    blueBeans.clear()
    blueAdapter?.notifyDataSetChanged()
    val bondedDevices = bluetoothAdapter.bondedDevices // 获取以及配对的设备
    bondedDevices?.forEach { device ->
        blueBeans.add(device)
        blueAdapter?.notifyItemInserted(blueBeans.size)
    }
    if (bluetoothAdapter.isDiscovering) {
        bluetoothAdapter.cancelDiscovery()// 取消扫描
    }
    bluetoothAdapter.startDiscovery()// 开始扫描
}
```

### 其他

获取ip地址
```kotlin
fun getIPAddress(): String? {
    try {
        val interfaces = NetworkInterface.getNetworkInterfaces()
        while (interfaces.hasMoreElements()) {
            val networkInterface = interfaces.nextElement()
            if (networkInterface.name == sKeyInterfaceName) {
                val addresses = networkInterface.inetAddresses
                while (addresses.hasMoreElements()) {
                    val inetAddress = addresses.nextElement()
                    if (!inetAddress.isLoopbackAddress && !inetAddress.isLinkLocalAddress
                        && inetAddress.isSiteLocalAddress
                    ) {
                        return inetAddress.hostAddress
                    }
                }
            }
        }
    } catch (e: Exception) {
        e.printStackTrace()
    }
    return null
}
```
判断是否是IP地址
```kotlin
fun isIPv4Address(ipString: String): Boolean {
    // IPv4 地址正则表达式，匹配形如 "0.0.0.0" 至 "255.255.255.255" 的格式
    val ipv4Regex = "^((25[0-5]|2[0-4][0-9]|[01]?[0-9][0-9]?)\\.){3}(25[0-5]|2[0-4][0-9]|[01]?[0-9][0-9]?)$"
    val pattern = Pattern.compile(ipv4Regex)
    return pattern.matcher(ipString).matches()
}
```