### 自定义图标宽高
自定义属性，控制图标的宽高
```xml
<declare-styleable name="ImageTextView">
    <attr name="drawableWidth_left" format="dimension" />
    <attr name="drawableHeight_left" format="dimension" />
    <attr name="drawableWidth_top" format="dimension" />
    <attr name="drawableHeight_top" format="dimension" />
    <attr name="drawableWidth_right" format="dimension" />
    <attr name="drawableHeight_right" format="dimension" />
    <attr name="drawableWidth_bottom" format="dimension" />
    <attr name="drawableHeight_bottom" format="dimension" />
</declare-styleable>
```
源码
```kotlin
class ImageTextView(context: Context, attrs: AttributeSet?
): AppCompatTextView(context, attrs) {
    private val POSITION_LEFT: Int = 0
    private val POSITION_TOP: Int = 1
    private val POSITION_RIGHT: Int = 2
    private val POSITION_BOTTOM: Int = 3

    private var leftDrawableWidth = 0
    private var leftDrawableHeight = 0
    private var topDrawableWidth = 0
    private var topDrawableHeight = 0
    private var rightDrawableWidth = 0
    private var rightDrawableHeight = 0
    private var bottomDrawableWidth = 0
    private var bottomDrawableHeight = 0
    private var left: Drawable? = null
    private var top: Drawable? = null
    private var right: Drawable? = null
    private var bottom: Drawable? = null

    init {
        getAttributes(context, attrs, 0)
        includeFontPadding = false
    }

    private fun getAttributes(context: Context, attrs: AttributeSet?, defStyleAttr: Int) {
        /**
         * 获得我们所定义的自定义样式属性
         */
        val a =
            context.theme.obtainStyledAttributes(attrs, R.styleable.ImageTextView, defStyleAttr, 0)
        val n = a.indexCount
        for (i in 0 until n) {
            when (val attr = a.getIndex(i)) {
                R.styleable.ImageTextView_drawableWidth_left -> leftDrawableWidth =
                    a.getDimensionPixelSize(attr, 0)

                R.styleable.ImageTextView_drawableHeight_left -> leftDrawableHeight =
                    a.getDimensionPixelSize(attr, 0)

                R.styleable.ImageTextView_drawableWidth_top -> topDrawableWidth =
                    a.getDimensionPixelSize(attr, 0)

                R.styleable.ImageTextView_drawableHeight_top -> topDrawableHeight =
                    a.getDimensionPixelSize(attr, 0)

                R.styleable.ImageTextView_drawableWidth_right -> rightDrawableWidth =
                    a.getDimensionPixelSize(attr, 0)

                R.styleable.ImageTextView_drawableHeight_right -> rightDrawableHeight =
                    a.getDimensionPixelSize(attr, 0)

                R.styleable.ImageTextView_drawableWidth_bottom -> bottomDrawableWidth =
                    a.getDimensionPixelSize(attr, 0)

                R.styleable.ImageTextView_drawableHeight_bottom -> bottomDrawableHeight =
                    a.getDimensionPixelSize(attr, 0)
            }
        }
        a.recycle()

        /*
         * setCompoundDrawablesWithIntrinsicBounds方法会首先在父类的构造方法中执行，
         * 彼时执行时drawable的大小还都没有开始获取，都是0,
         * 这里获取完自定义的宽高属性后再次调用这个方法，插入drawable的大小
         * */
        setCompoundDrawablesWithIntrinsicBounds(
            left, top, right, bottom
        )
    }


    /**
     * Sets the Drawables (if any) to appear to the left of, above, to the
     * right of, and below the text. Use `null` if you do not want a
     * Drawable there. The Drawables' bounds will be set to their intrinsic
     * bounds.
     *
     *
     * Calling this method will overwrite any Drawables previously set using
     * [.setCompoundDrawablesRelative] or related methods.
     * 这里重写这个方法，来设置上下左右的drawable的大小
     *
     * @attr ref android.R.styleable#TextView_drawableLeft
     * @attr ref android.R.styleable#TextView_drawableTop
     * @attr ref android.R.styleable#TextView_drawableRight
     * @attr ref android.R.styleable#TextView_drawableBottom
     */
    override fun setCompoundDrawablesWithIntrinsicBounds(
        @Nullable left: Drawable?,
        @Nullable top: Drawable?,
        @Nullable right: Drawable?,
        @Nullable bottom: Drawable?
    ) {
        this.left = left
        this.top = top
        this.right = right
        this.bottom = bottom

        left?.setBounds(0, 0, leftDrawableWidth, leftDrawableHeight)
        right?.setBounds(0, 0, rightDrawableWidth, rightDrawableHeight)
        top?.setBounds(0, 0, topDrawableWidth, topDrawableHeight)
        bottom?.setBounds(0, 0, bottomDrawableWidth, bottomDrawableHeight)

        setCompoundDrawables(left, top, right, bottom)
    }

    /*
     * 代码中动态设置drawable的宽高度
     * */
    fun setDrawableSize(width: Int, height: Int, position: Int) {
        if (position == POSITION_LEFT) {
            leftDrawableWidth = width
            leftDrawableHeight = height
        }
        if (position == POSITION_TOP) {
            topDrawableWidth = width
            topDrawableHeight = height
        }
        if (position == POSITION_RIGHT) {
            rightDrawableWidth = width
            rightDrawableHeight = height
        }
        if (position == POSITION_BOTTOM) {
            bottomDrawableWidth = width
            bottomDrawableHeight = height
        }

        setCompoundDrawablesWithIntrinsicBounds(
            left, top, right, bottom
        )
    }
}
```

### 斜体修复
```kotlin
class FixTextView(
    context: Context, 
    attrs: AttributeSet?
) : AppCompatTextView(context, attrs) {
    private val minRect by lazy { Rect() }

    override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
        super.onMeasure(widthMeasureSpec, heightMeasureSpec)
        paint.getTextBounds(text.toString(), 0, text.length, minRect)
        val width = minRect.width()
        setMeasuredDimension(width, measuredHeight)
    }

    override fun onDraw(canvas: Canvas) {
        val text = text.toString()
        val left = minRect.left
        val top = measuredHeight / 2 - minRect.top / 2
        paint.color = currentTextColor
        canvas.drawText(text, -left.toFloat(), top.toFloat(), paint)
    }
}

android:textStyle="italic"

val typeface =
    Typeface.createFromAsset(requireActivity().assets, "Helvetica-BoldOblique.ttf")
binding.xxx.setTypeface(typeface, Typeface.ITALIC)
```