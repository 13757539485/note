### 常见配置问题
网页不显示，说明没有开启JavaScript
```kotlin
webview.settings.javaScriptEnabled = true
```
网页加载后报错，说明设置了WebViewClient，需在AndroidManifest中添加usesCleartextTraffic
```xml
<application android:usesCleartextTraffic="true">
```

### Android调用H5

#### 方式1

```kotlin
webview.loadUrl("javascript:funName()")
```
不能获取返回值

#### 方式2
```kotlin
evaluateJavascript("funName()") { returnStr-> 
    
}
```

### H5调用Android

#### 方式1
```kotlin
addJavascriptInterface(JsInterface(), "android")

private inner class JsInterface {
    @JavascriptInterface
    fun jsCallback(content: String) {

    }
}
```

#### 方式2
WebViewClient中重写方法
```kotlin
override fun shouldOverrideUrlLoading(view: WebView?, request: WebResourceRequest?): Boolean {
        return super.shouldOverrideUrlLoading(view, request)
    }
```
一般用于拦截链接，返回true表示拦截

#### 方式3
WebViewChromeClient从重写方法onJsAlert、onJsConfirm、onJsPrompt

### <a id="webview_optimize">优化方案</a>
1. WebView 动态加载和销毁
2. 独立的web进程，与主进程隔开，解决任何webview内存泄漏之类的问题
3. 实现WebView复用
4. DNS解析优化（接口与网页主域名一致）
5. 离线预推，下发离线包，并增量更新
6. 网页按节点局部刷新
7. 自定义实现图片资源缓存
8. 加载本地网页

#### 独立进程思路

Manifest中申明WebViewActivity在其他进程，再声明一个Service
```xml
<activity
    android:name=".YWebViewActivity"
    android:exported="false"
    android:process=":ywebview" />
<service
    android:name=".mainprocess.MainProcessCommandService"
    android:exported="false"
    android:process=":ywebview" />
```
Application中绑定Service，相当于启动了ywebview进程
```kotlin
Looper.myQueue().addIdleHandler {
    WebViewCommandsDispatcher.prepareWebViewConfig()
    false
}
// 绑定Service
val intent = Intent(BaseApplication.instance, MainProcessCommandService::class.java)
        BaseApplication.instance.bindService(intent, this, Context.BIND_AUTO_CREATE)

class MainProcessCommandService: Service() {
    override fun onBind(intent: Intent): IBinder? {
        return MainProcessCommandsManager.asBinder()
    }
}
```
WebView进程预加载能优化启动时间100ms+，但在冷启动时直接启动WebViewActivity会很慢(webview进程还没有加载完成，还不如直接启动快)

#### WebView复用
实现思路是使用集合来保存WebView，可以最大保存几个，如使用Stack、List等，可随进程启动时创建WebView
```kotlin
private val cache: Stack<YWebView> = Stack()
fun prepareWebView() {
    if (cache.size < MAX_CACHE) {
        cache.push(createWebView())
    }
}
private fun createWebView(): YWebView =
    YWebView(MutableContextWrapper(BaseApplication.instance))
```
获取WebView时需要替换context，由于创建的时候采用applicationContext
```kotlin
cache.pop().also { yWebView ->
    (yWebView.context as MutableContextWrapper).baseContext = context
}
```
在Activity销毁时回收WebView(可能存在问题)
```kotlin
fun destroy(webView: YWebView, isCache: Boolean = true) {
    webView.stopLoading()
    webView.clearCache(true)
    webView.clearHistory()
    webView.clearFormData()
    webView.clearSslPreferences()
    if (webView.parent != null)
        (webView.parent as ViewGroup).removeView(webView)
    (webView.context as MutableContextWrapper).baseContext = webView.context.applicationContext
    if (isCache && cache.size < MAX_CACHE) {
        cache.push(webView)
    }
}
```
待解决问题：

WebView复用：存在问题界面goBack时混乱

goBack时会自动刷新界面

网页中打开应用未实现

### X5内核

https://x5.tencent.com/docs/index.html

### AgentWeb

https://github.com/Justson/AgentWeb

基于X5内核的WebView封装