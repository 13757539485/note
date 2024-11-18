### 主进程初始化SDK
```kotlin
override fun onCreate() {
    super.onCreate()
    // 根据进程名进行不同的初始化
    if (packageName == getProcessName(this)) {
        initMainProcess()// 主进程初始化
    }

     private fun initMainProcess() {
        //todo
    }

    private fun getProcessName(context: Context): String? {
        val am = context.getSystemService(ACTIVITY_SERVICE) as ActivityManager
        for (processInfo in am.runningAppProcesses) {
            if (processInfo.pid == Process.myPid()) {
                return processInfo.processName
            }
        }
        return null
    }
}
```
### 拦截sdk中的Activity
```kotlin
fun registerHook(application: Application) {
    application.registerActivityLifecycleCallbacks(object : ActivityLifecycleCallbacks {
        override fun onActivityCreated(activity: Activity, savedInstanceState: Bundle?) {
            if ("com.xxx.xxx.XXXActivity" == activity.javaClass.name) {
                Log.d(TAG, "activity create")
                activity.findViewById<ViewGroup>(android.R.id.content).also {
                    it.post {
                        it.addView(XXXView(activity))//添加view或者布局
                    }
                }
            }
        }

        override fun onActivityStarted(activity: Activity) {
        }

        override fun onActivityResumed(activity: Activity) {
        }

        override fun onActivityPaused(activity: Activity) {
        }

        override fun onActivityStopped(activity: Activity) {
        }

        override fun onActivitySaveInstanceState(activity: Activity, outState: Bundle) {
        }

        override fun onActivityDestroyed(activity: Activity) {
            if ("com.xxx.xxx.XXXActivity" == activity.javaClass.name) {
                Log.d(TAG, "activity destroy")
            }
        }
    })
}
```