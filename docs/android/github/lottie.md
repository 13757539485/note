### lottie
https://github.com/airbnb/lottie-android

#### 使用方式
```kts
implementation 'com.airbnb.android:lottie:6.6.0'
```

xml添加布局
```xml
<com.airbnb.lottie.LottieAnimationView
    app:lottie_autoPlay="true"
    android:scaleX="1.3"
    android:scaleY="1.3"
    app:lottie_loop="true"
    app:lottie_fileName="ktv_count.json"
    android:layout_width="match_parent"
    android:layout_height="match_parent" />
```
缩放使用scaleX和scaleY

lottie_autoPlay：自动播放

lottie_loop：是否循环播放

lottie_fileName：assets文件夹中的json文件

播放动画(前提是xml中已经引用了json)
```kotlin
binding.xxx.playAnimation()
```
取消动画
```kotlin
if (binding.xxx.isAnimating)
    binding.xxx.cancelAnimation()
```
暂停动画
```kotlin
binding.xxx.pauseAnimation()
```
恢复动画
```kotlin
binding.xxx.resumeAnimation()
```
代码中加载动画
```kotlin
LottieCompositionFactory.fromAsset(context, lottieJson, null).addListener {
    binding.xxx.setComposition(it)
}
```
监听动画
```kotlin
binding.xxx.addAnimatorListener(object : AnimatorListenerAdapter(){
    override fun onAnimationEnd(animation: Animator?) {
    }
})
```
- 待验证最新版本：
自定义view中不要使用findViewById获取LottieAnimationView，采用getChildAt(index),原因是activity重启时，dispatchrestoreInstanceState机制和LottieAnimationView恢复机制冲突导致