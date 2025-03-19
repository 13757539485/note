### AppCompatSeekBar
### 常见属性
```xml
android:duplicateParentState="true"
```
用来控制按下效果显示隐藏，true为隐藏

```xml
android:splitTrack="false"
```
用来解决thumb背景透明问题，false为不透明，默认true
#### 竖向SeekBar
自定义属性：
```xml
<declare-styleable name="VerticalSeekBar">
    <attr name="barRotation" format="enum" >
        <enum name="top" value="90" />
        <enum name="bottom" value="-90" />
    </attr>
</declare-styleable>
```
源码：
```kotlin
class VerticalSeekBar(context: Context, attrs: AttributeSet?) : AppCompatSeekBar(
    context, attrs
), OnSeekBarChangeListener {
    private var barRotation: Int = 90
    private var startAndStopListener: StartAndStopListener? = null
    private var touchListener: ((isTouch: Boolean)->Unit)? = null

    init {
        val obtainStyledAttributes =
            context.obtainStyledAttributes(attrs, R.styleable.VerticalSeekBar)
        barRotation = obtainStyledAttributes.getInt(R.styleable.VerticalSeekBar_barRotation, 90)
        obtainStyledAttributes.recycle()
        setOnSeekBarChangeListener(this)
    }

    override fun onSizeChanged(w: Int, h: Int, oldW: Int, oldH: Int) {
        super.onSizeChanged(h, w, oldW, oldH)
    }

    @Synchronized
    override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
        super.onMeasure(heightMeasureSpec, widthMeasureSpec)
        setMeasuredDimension(measuredHeight, measuredWidth)
    }

    override fun onDraw(c: Canvas) {
        when (barRotation) {
            90 -> {
                c.rotate(90f)
                c.translate(0f, -width.toFloat())
            }

            -90 -> {
                c.rotate(-90f)
                c.translate(-height.toFloat(), 0f)
            }
        }
        super.onDraw(c)
    }

    @Synchronized
    override fun setProgress(progress: Int) {
        super.setProgress(progress)
        onSizeChanged(width, height, 0, 0)
    }

    private fun calculateRotationProgress(y: Float) {
        if (barRotation == -90) {
            progress = max - (max * y / height).toInt()
        } else if (barRotation == 90) {
            progress = (max * y / height).toInt()
        }
    }

    override fun onTouchEvent(event: MotionEvent): Boolean {
        when (event.action) {
            MotionEvent.ACTION_DOWN -> {
                startAndStopListener?.startChange(this)
                calculateRotationProgress(event.y)
                this.touchListener?.invoke(true)
            }

            MotionEvent.ACTION_MOVE -> {
                calculateRotationProgress(event.y)
            }

            MotionEvent.ACTION_UP, MotionEvent.ACTION_CANCEL -> {
                startAndStopListener?.stopChange(this, progress)
                this.touchListener?.invoke(false)
            }

            else -> return super.onTouchEvent(event)
        }
        return true
    }

    fun setStartAndStopListener(startAndStopListener: StartAndStopListener?) {
        this.startAndStopListener = startAndStopListener
    }

    fun setTouchListener(block: (isTouch: Boolean) -> Unit) {
        this.touchListener = block
    }

    interface StartAndStopListener {
        fun startChange(seekBar: VerticalSeekBar)
        fun onChange(seekBar: VerticalSeekBar, progress: Int, fromUser: Boolean)
        fun stopChange(seekBar: VerticalSeekBar, progress: Int)
    }

    override fun onProgressChanged(seekBar: SeekBar?, progress: Int, fromUser: Boolean) {
        startAndStopListener?.onChange(this@VerticalSeekBar, progress, fromUser)
    }

    override fun onStartTrackingTouch(seekBar: SeekBar?) {
    }

    override fun onStopTrackingTouch(seekBar: SeekBar?) {
    }
}
```