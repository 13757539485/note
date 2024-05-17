### 理论
#### NFC标签
- 一种内置NFC芯片的小型电子标签
- 可以存储信息，如网址、文字、电话号码或特定指令
- 不需要电池
- NFC功能的设备（如智能手机）靠近时，可以无线方式激活并交换信息

##### 应用场景
包括但不限于产品防伪、信息查询、广告互动、智能家居控制、以及实现快速设置手机（如切换静音模式、开启Wi-Fi等）

工作原理基于ISO 14443A或ISO 15693协议，通常在13.56MHz的高频下运作，读写距离一般在1-10厘米之间

#### 手机nfc模块
- 近场通信模块，需要打通驱动如[pn7160](../fws/fws_rk.md#nfc)
- 结合RFID（射频识别）和无线通信技术

##### 应用场景

移动支付、数据传输、无线配对（如蓝牙音箱、耳机）以及模拟实体卡片（如公交卡、门禁卡）等功能

### 基本使用
#### 声明权限
```xml
<uses-permission android:name="android.permission.NFC" />

<uses-feature
    android:name="android.hardware.nfc"
    android:required="true" />
```
#### nfc适配器
必须依赖于activity，如果获取不到则设备不支持nfc
```kotlin
val nfcAdapter = NfcAdapter.getDefaultAdapter(activity)
if (nfcAdapter == null) {
    Toast.makeText(activity, "设备不支持NFC", Toast.LENGTH_SHORT).show()
}
```
#### 开启nfc
如果nfc没开启，可跳转到设置界面开启
```kotlin
if (!nfcAdapter.isEnabled) {
    Toast.makeText(activity, "NFC未开启，请开启NFC功能", Toast.LENGTH_SHORT).show()
    val intent = Intent(Settings.ACTION_NFC_SETTINGS)
    activity.startActivity(intent)
}
```

#### 检测nfc标签等设备
方式一：reader模式，允许在后台检测(也需要activity)
```kotlin
// 开启
val flags = NfcAdapter.FLAG_READER_NFC_A or
        NfcAdapter.FLAG_READER_NFC_B or
        NfcAdapter.FLAG_READER_NFC_F or
        NfcAdapter.FLAG_READER_NFC_V or
        NfcAdapter.FLAG_READER_NFC_BARCODE
nfcAdapter?.enableReaderMode(activity, { tag -> }, flags, null)

// 关闭
nfcAdapter?.disableReaderMode(activity)
```

方式二：分发模式，界面显示时首先拦截，否则走系统
```kotlin
// 开启
val intent = Intent(activity, activity::class.java).apply {
    addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP)
}
val pendingIntent = PendingIntent.getActivity(
    activity, 0, intent,
    PendingIntent.FLAG_MUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
)
nfcAdapter.enableForegroundDispatch(
    activity,
    pendingIntent,
    intentFiltersArray,
    techListsArray
)
// 关闭
nfcAdapter.disableForegroundDispatch(activity)
```
在activity中重写onNewIntent
```kotlin
override fun onNewIntent(intent: Intent) {
    super.onNewIntent(intent)
    handleIntent(intent)
}

private fun handleIntent(intent: Intent?) {
    intent?.let {
        if (NfcAdapter.ACTION_NDEF_DISCOVERED == it.action) {
            it.getParcelableArrayExtra(NfcAdapter.EXTRA_NDEF_MESSAGES)?.let { rawMessages ->
                
            }
        }
    }
}
```

#### Activity使用
一般在onResume和onPause中开启nfc检测
```
public override fun onResume() {
    super.onResume()
//        NfcManager.enableNfcDispatcher(this)
    NfcManager.enableNfcReaderMode(this) {
        it?.let { message ->
        }
    }
}

public override fun onPause() {
    super.onPause()
    NfcManager.disableNfcDispatcher(this)
//        NfcManager.disableNfcReaderMode(this)
}
```

#### 解析标签
以文本内容为例，其他还有：URL、电话、名片、蓝牙、WIFI、应用、位置、数据、收藏夹等
```kotlin
val ndef = Ndef.get(tag)
if (ndef != null) {
    ndef.connect()
    try {
        ndef.cachedNdefMessage.records.forEach { record ->
            if (record.tnf == NdefRecord.TNF_WELL_KNOWN &&
                record.type.contentEquals(NdefRecord.RTD_TEXT)
            ) {
                try {
                    val payload = record.payload
                    val textEncoding = if ((payload[0].toInt() and 0x80) == 0) "UTF-8" else "UTF-16"
                    val languageSize = payload[0].toInt() and 0x3F
                    String(
                        payload, languageSize + 1,
                        payload.size - languageSize - 1, Charset.forName(textEncoding)
                    )
                } catch (e: UnsupportedEncodingException) {
                    Log.e(TAG, "Unsupported Encoding", e)
                }
            } else {
                /* 可以在这里添加对其他类型NDEF记录的支持 */
                Log.e(TAG, "other type: ${record.type}" )
            }
        }
    } finally {
        ndef.close()
    }
}
```

### 工具类
[NfcManager](./code/src/com/nfc/NfcManager.kt)

### nfc写数据
购买nfc标签贴

工具：下载NFC标签助手
