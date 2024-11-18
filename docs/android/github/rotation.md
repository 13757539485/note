#### <a id="anim_rotation">旋转动画</a>
每隔4s旋转360，无限循环
```kotlin
class AlbumImageView(context: Context, attrs: AttributeSet?) : AppCompatImageView(context, attrs) {
    private val TAG = "AlbumImageView"
    private var rotationAnimator: ObjectAnimator =
        ObjectAnimator.ofFloat(this, "rotation", 0f, 360f)

    init {
        rotationAnimator.setDuration(4000)
        rotationAnimator.interpolator = LinearInterpolator()
        rotationAnimator.repeatCount = ObjectAnimator.INFINITE
        rotationAnimator.repeatMode = ObjectAnimator.RESTART
    }

    fun startAnim() {
        if (!rotationAnimator.isStarted) {
            rotationAnimator.start()
            Log.d(TAG, "startAnim() called")
        }
    }

    fun stopAnim() {
        if (rotationAnimator.isRunning) {
            rotationAnimator.cancel()
            Log.d(TAG, "stopAnim() called")
        }
    }
    fun pauseAnim() {
        if (rotationAnimator.isRunning) {
            rotationAnimator.pause()
            Log.d(TAG, "pauseAnim() called")
        }
    }
    fun resumeAnim() {
        if (rotationAnimator.isPaused) {
            rotationAnimator.resume()
            Log.d(TAG, "resumeAnim() called")
        }
    }
}
```