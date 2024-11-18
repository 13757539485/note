定时，倒计时，后台任务
### Timer & TimerTask
```kotlin
class MinuteTimerTask(private val period: Long = 60000L, private val callback: () -> Unit) {
    private var timer: Timer? = null
    private var timerTask: TimerTask? = null

    fun start() {
        // 如果 timer 已经存在，先取消它
        stop()

        // 创建新的 Timer 和 TimerTask
        timer = Timer()
        timerTask = object : TimerTask() {
            override fun run() {
                callback()
            }
        }

        // 调度任务，每分钟执行一次
        timer?.schedule(timerTask, period, period)
        //timer?.scheduleAtFixedRate(timerTask, period, period)
    }

    fun stop() {
        // 取消并清除 Timer 和 TimerTask
        timer?.cancel()
        timer = null
        timerTask = null
    }
}
```
注意：callback回调如果涉及UI操作需要切换成主线程
### delay
```kotlin
class MinuteTimerKt(private val period: Long = 60000L, private val callback: () -> Unit) {
    private val mainScope = MainScope()
    private var loop = AtomicBoolean(false)
    private var job: Job? = null

    fun start() {
        if (loop.get()) {
            return
        }
        loop.set(true)
        job = mainScope.launch {
            while (loop.get()) {
                delay(period)
                callback()
            }
        }
    }

    fun stop() {
        loop.set(false)
        job?.cancel()
        job = null
    }
}
```
### CountDownTimer
jdk中的api
```kotlin
private val countDown by lazy { object : CountDownTimer(2000L, 2000L){
        override fun onTick(millisUntilFinished: Long) {
            
        }

        override fun onFinish() {
            
        }
    } 
}
countDown.start()
countDown.cancel()
```
### HandlerThread
精确到1毫秒级别
```kotlin
class HmCountDownTimer {
    private val TAG = "HmCountDownTimer"
    private val handlerThread: HandlerThread = HandlerThread("CountDownTimerThread")
    private var handler: Handler
    init {
        handlerThread.start()
        handler = Handler(handlerThread.looper)
    }
    @Volatile
    private var isStart: Boolean = false

    // 主线程的 Handler，用于确保 block() 在主线程中执行
    private val mainHandler = Handler(Looper.getMainLooper())

    fun startTimer(allTime: Long, countTime: Long = 3L, block: () -> Unit) {
        if (isStart) {
            Log.d(
                TAG,
                "already startTimer() called with: allTime = $allTime, countTime = $countTime"
            )
            return // 防止重复启动
        }
        isStart = true
        Log.d(
            TAG,
            "startTimer() called with: allTime = $allTime, countTime = $countTime"
        )
        val startTime = System.currentTimeMillis()
        val runnable = object : Runnable {
            override fun run() {
                if (!isStart) return

                val elapsedTime = System.currentTimeMillis() - startTime
                val remainingTime = allTime - elapsedTime

                if (remainingTime <= countTime * 1000L) {
                    // 在主线程中执行 block()
                    mainHandler.post { block() }
                    stopTimer()
                } else {
                    handler.postDelayed(this, 1L) // 每 1 毫秒检查一次
                }
            }
        }

        handler.post(runnable)
    }

    fun stopTimer() {
        Log.d(
            TAG,
            "stopTimer() called with: isStart = $isStart"
        )
        if (isStart) {
            isStart = false
            handler.removeCallbacksAndMessages(null)
            handlerThread.quitSafely()
        }
    }
}
```
