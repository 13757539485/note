### 封装view
引用[glide](../github/glide.md#glide)

自定义属性
```xml
<declare-styleable name="GifImageView">
    <attr name="gif_src" format="reference"/>
</declare-styleable>
```
源码
```kotlin
class GifImageView(context: Context, attrs: AttributeSet?) : AppCompatImageView(context, attrs) {
    private val TAG = "GifImageView"

    init {
        val obtainStyledAttributes =
            context.obtainStyledAttributes(attrs, R.styleable.GifImageView)
        val drawable = obtainStyledAttributes.getResourceId(R.styleable.GifImageView_gif_src, 0)
        if (drawable != 0) {
            Glide.with(context).load(drawable).into(GifTarget(this, false))
        }
        obtainStyledAttributes.recycle()
    }

    fun loadGif(@DrawableRes resId: Int, isPlay: Boolean) {
        if (drawable is GifDrawable) {
            if (isPlay) {
                (drawable as GifDrawable).start()
                Log.d(TAG, "start")
            } else{
                (drawable as GifDrawable).stop()
                Log.d(TAG, "stop")
            }
        } else {
            Log.d(TAG, "load")
            Glide.with(context).load(resId).into(GifTarget(this, isPlay))
        }
    }

    fun pause() {
        if (drawable is GifDrawable) {
            (drawable as GifDrawable).let {
                if (it.isRunning) {
                    it.stop()
                    Log.d(TAG, "pause() called")
                } else {
                    Log.d(TAG, "not running")
                }
            }
        } else {
            Log.d(TAG, "can not to pause: drawable=$drawable")
        }
    }

    fun resume() {
        if (drawable is GifDrawable) {
            (drawable as GifDrawable).let {
                if (!it.isRunning) {
                    it.start()
                    Log.d(TAG, "resume() called")
                } else {
                    Log.d(TAG, "already run")
                }
            }
        } else {
            Log.d(TAG, "can not to resume: drawable=$drawable")
        }
    }

    private inner class GifTarget(
        private val view: AppCompatImageView,
        private val defaultPlay: Boolean = false
    ) : CustomTarget<Drawable>() {
        private val TAG = "GifTarget"
        override fun onResourceReady(
            resource: Drawable,
            transition: Transition<in Drawable>?
        ) {
            Log.d(
                TAG,
                "onResourceReady() called with: resource = $resource"
            )
            view.setImageDrawable(resource)
            if (resource is GifDrawable) {
                if (defaultPlay) {
                    resource.start()
                    Log.d(TAG, "onResourceReady start gif")
                } else {
                    resource.stop()
                    Log.d(TAG, "onResourceReady stop gif")
                }
            }
        }

        override fun onLoadCleared(placeholder: Drawable?) {
            Log.d(TAG, "onLoadCleared() called with: placeholder = $placeholder")
        }
    }
}
```
GifTarget：实现默认不自动播放

xml使用
```xml
<com.common.ui.GifImageView
    android:layout_width="@dimen/dp24"
    android:layout_height="@dimen/dp24"
    app:gif_src="@drawable/xxx" />
```
gif_src：gif资源图

代码中加载gif图：loadGif
