
### 封装媒体管理类
```kotlin
const val ACTION_VOLUME_CHANGED = "android.media.VOLUME_CHANGED_ACTION"
const val EXTRA_VOLUME_STREAM_TYPE = "android.media.EXTRA_VOLUME_STREAM_TYPE"

object MediaManager {
    private const val TAG = "MediaManager"
    private var volumeReceiver: VolumeReceiver? = null
    private lateinit var audioManager: AudioManager

    private lateinit var applicationContext: Context
    private var audioPlaybackCallback: AudioPlaybackCallback? = null

    fun init(context: Context) : MediaManager{
        if (MediaManager::applicationContext.isInitialized) {
            Log.i(TAG, "already init")
            return this
        }
        applicationContext = context.applicationContext
        audioManager = applicationContext.getSystemService(Context.AUDIO_SERVICE) as AudioManager
        return this
    }

    private fun check() {
        if (!MediaManager::applicationContext.isInitialized) {
            throw Exception("Please call init() one time")
        }
    }

    @SuppressLint("UnspecifiedRegisterReceiverFlag")
    fun registerVolumeReceiver(block: ((volume: Int) -> Unit)?) {
        val intentFilter = IntentFilter()
        intentFilter.addAction(ACTION_VOLUME_CHANGED)
        intentFilter.addAction(Intent.ACTION_MEDIA_BUTTON)
        volumeReceiver = VolumeReceiver(block)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            applicationContext.registerReceiver(
                volumeReceiver, intentFilter,
                Context.RECEIVER_NOT_EXPORTED
            )
        } else {
            applicationContext.registerReceiver(volumeReceiver, intentFilter)
        }
    }

    fun getCurrentMusicVolume(): Int {
        check()
        return audioManager.getStreamVolume(AudioManager.STREAM_MUSIC)
    }

    fun getCurrentMusicMaxVolume(): Int {
        check()
        return audioManager.getStreamMaxVolume(AudioManager.STREAM_MUSIC)
    }

    @RequiresApi(Build.VERSION_CODES.P)
    fun getCurrentMusicMinVolume(): Int {
        check()
        return audioManager.getStreamMinVolume(AudioManager.STREAM_MUSIC)
    }

    fun setMusicVolume(volume: Int) {
        check()
        audioManager.setStreamVolume(
            AudioManager.STREAM_MUSIC,
            volume,
            AudioManager.FLAG_REMOVE_SOUND_AND_VIBRATE
        )
    }

    fun playMusic() {
        check()
        val time = SystemClock.uptimeMillis()
        audioManager.dispatchMediaKeyEvent(
            KeyEvent(
                time, time,
                KeyEvent.ACTION_DOWN, KeyEvent.KEYCODE_MEDIA_PLAY, 0
            )
        )
    }

    fun pauseMusic() {
        check()
        val time = SystemClock.uptimeMillis()
        audioManager.dispatchMediaKeyEvent(
            KeyEvent(
                time, time,
                KeyEvent.ACTION_DOWN, KeyEvent.KEYCODE_MEDIA_PAUSE, 0
            )
        )
    }

    fun nextMusic() {
        check()
        val time = SystemClock.uptimeMillis()
        audioManager.dispatchMediaKeyEvent(
            KeyEvent(
                time, time,
                KeyEvent.ACTION_DOWN, KeyEvent.KEYCODE_MEDIA_NEXT, 0
            )
        )
    }

    fun previousMusic() {
        check()
        val time = SystemClock.uptimeMillis()
        audioManager.dispatchMediaKeyEvent(
            KeyEvent(
                time, time,
                KeyEvent.ACTION_DOWN, KeyEvent.KEYCODE_MEDIA_PREVIOUS, 0
            )
        )
    }

    fun isPlayingMusic(): Boolean {
        check()
        return audioManager.isMusicActive
    }

    @RequiresApi(Build.VERSION_CODES.O)
    fun registerMusicStatus(block: ((configs: List<AudioPlaybackConfiguration>) -> Unit)?) {
        check()
        audioPlaybackCallback = object : AudioPlaybackCallback() {
            override fun onPlaybackConfigChanged(configs: List<AudioPlaybackConfiguration>) {
                block?.invoke(configs)
            }
        }
        audioManager.registerAudioPlaybackCallback(audioPlaybackCallback!!, null)
    }

    @RequiresApi(Build.VERSION_CODES.O)
    fun unRegisterMusicStatus() {
        audioPlaybackCallback?.let {
            audioManager.unregisterAudioPlaybackCallback(it)
            audioPlaybackCallback = null
        }
    }

    fun destroy() {
        check()
        if (volumeReceiver != null) applicationContext.unregisterReceiver(volumeReceiver)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            unRegisterMusicStatus()
        }
    }
}
```
音量广播
```kotlin
class VolumeReceiver(private val block: ((volume: Int)->Unit)?) :
    BroadcastReceiver() {

    @RequiresApi(Build.VERSION_CODES.O)
    override fun onReceive(context: Context?, intent: Intent) {
        Log.d("VolumeReceiver", "onReceive() called with: intent = $intent")
        if (isReceiveVolumeChange(intent)) {
            block?.let { it(MediaManager.getCurrentMusicVolume()) }
        }
    }

    private fun isReceiveVolumeChange(intent: Intent): Boolean {
        return ACTION_VOLUME_CHANGED == intent.action && intent.getIntExtra(
            EXTRA_VOLUME_STREAM_TYPE,
            -1
        ) == AudioManager.STREAM_MUSIC
    }
}
```
### 使用
初始化并监听系统音量变化
```kotlin
private var isUserSetVolume: Boolean = false

MediaManager.init(this).registerVolumeReceiver { volume: Int ->
       注：设置音量时setMusicVolume监听会回调，技巧就是
    if (isUserSetVolume) {
        isUserSetVolume = false
        return@registerVolumeReceiver
    } 
}
主动修改时isUserSetVolume = true
```
销毁
```kotlin
MediaManager.destroy()
```

### 三方音乐信息获取
```kotlin
class NativeExt(context: Context) {
    private val TAG = "NativeExt"
    private val handler = Handler(Looper.getMainLooper())
    private val mainScope = MainScope()
    var currentMusicPkg: String = ""

    init {
        MediaManager.init(context)
    }
    private val mediaSessionManager: MediaSessionManager = context.getSystemService(Context.MEDIA_SESSION_SERVICE) as MediaSessionManager

    fun startListening() {
        MediaManager.registerMusicStatus {
            handler.postDelayed({
                mediaSessionManager.getActiveSessions(
                    null
                ).also { cs ->
                    if (cs.isNotEmpty()) {
                        cs.filter { controller ->
                            Log.i(
                                TAG,
                                "filter: ${controller.packageName}-${controller.playbackState?.state}"
                            )
                            controller.playbackState?.state == PlaybackState.STATE_PLAYING
                        }

                        cs[0].let { mc ->
                            if (mc.playbackState?.state == PlaybackState.STATE_PLAYING || mc.playbackState?.state == PlaybackState.STATE_BUFFERING) {
                                if (currentMusicPkg == mc.packageName) {
                                    Log.i(
                                        TAG,
                                        "repeat-------------------"
                                    )
                                    return@also
                                }
                                currentMusicPkg = mc.packageName
                                Log.e(
                                    TAG,
                                    "current play Pkg: ${mc.packageName}/$currentMusicPkg"
                                )
                                mc.metadata?.let { data ->
                                    FlowBus.with<MediaMetadata>(EVENT_MEDIA_METADATA_UPDATE).post(mainScope, data)
                                }
                            } else {
                                currentMusicPkg = ""
                                Log.e(
                                    TAG,
                                    "current pause Pkg: ${mc.packageName}/$currentMusicPkg"
                                )
                            }
                        }
                    }
                }
            }, 100)
        }
    }

    fun stopListening() {
        MediaManager.unRegisterMusicStatus()
    }
}
```