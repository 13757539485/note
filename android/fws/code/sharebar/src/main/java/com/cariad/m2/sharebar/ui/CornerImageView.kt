package com.cariad.m2.sharebar.ui

import android.content.Context
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.Path
import android.graphics.PorterDuff
import android.graphics.PorterDuffXfermode
import android.graphics.RectF
import android.util.AttributeSet
import androidx.appcompat.widget.AppCompatImageView
import com.cariad.m2.sharebar.R

class CornerImageView : AppCompatImageView {
    private lateinit var mDrawRectF: RectF
    private var mLeftTopRadius: Float = 0.0f
    private var mRightTopRadius: Float = 0.0f
    private var mLeftBottomRadius: Float = 0.0f
    private var mRightBottomRadius: Float = 0.0f
    private lateinit var mRadius: FloatArray
    private var mAllRadius: Float = 0.0f
    private lateinit var mTopPath: Path
    private lateinit var mBottomPath: Path
    private lateinit var mXFerMode: PorterDuffXfermode
    private lateinit var mPaint: Paint

    constructor(context: Context) : this(context, null)
    constructor(context: Context, attrs: AttributeSet?) : this(context, attrs, 0)
    constructor(context: Context, attrs: AttributeSet?, defStyleAttr: Int) : super(
        context,
        attrs,
        defStyleAttr
    ) {
        init(context, attrs)
    }

    private fun init(context: Context, attrs: AttributeSet?) {
        val typedArray = context.obtainStyledAttributes(attrs, R.styleable.CornerImageView)
        mAllRadius = typedArray.getDimension(
            R.styleable.CornerImageView_radius,
            0.0f
        )
        if (mAllRadius == 0.0f) {
            mLeftTopRadius = typedArray.getDimension(
                R.styleable.CornerImageView_left_top_radius,
                0.0f
            )
            mRightTopRadius = typedArray.getDimension(
                R.styleable.CornerImageView_right_top_radius,
                0.0f
            )
            mLeftBottomRadius =
                typedArray.getDimension(
                    R.styleable.CornerImageView_left_bottom_radius,
                    0.0f
                )
            mRightBottomRadius =
                typedArray.getDimension(
                    R.styleable.CornerImageView_right_bottom_radius,
                    0.0f
                )
        }
        typedArray.recycle()
        if (mAllRadius == 0.0f) {
            mRadius = floatArrayOf(
                mLeftTopRadius, mLeftTopRadius,
                mRightTopRadius, mRightTopRadius,
                mRightBottomRadius, mRightBottomRadius,
                mLeftBottomRadius, mLeftBottomRadius
            )
        }
        mTopPath = Path()
        mBottomPath = Path()
        mXFerMode = PorterDuffXfermode(PorterDuff.Mode.DST_OUT)
        mPaint = Paint()
    }

    override fun onSizeChanged(w: Int, h: Int, oldw: Int, oldh: Int) {
        super.onSizeChanged(w, h, oldw, oldh)
        mDrawRectF = RectF(0.0f, 0.0f, w.toFloat(), h.toFloat())
    }

    override fun onDraw(canvas: Canvas) {
        canvas.saveLayer(mDrawRectF, null)
        super.onDraw(canvas)
        clip(canvas)
    }

    private fun clip(canvas: Canvas?) {
        mPaint.reset()
        mPaint.isAntiAlias = true
        mPaint.style = Paint.Style.FILL
        mPaint.xfermode = mXFerMode

        mTopPath.reset()
        if (mAllRadius == 0.0f) {
            mTopPath.addRoundRect(mDrawRectF, mRadius, Path.Direction.CW)
        } else {
            mTopPath.addRoundRect(mDrawRectF, mAllRadius, mAllRadius, Path.Direction.CW)
        }
        mBottomPath.reset()
        mBottomPath.addRect(mDrawRectF, Path.Direction.CW)
        mBottomPath.op(mTopPath, Path.Op.DIFFERENCE)

        canvas!!.drawPath(mBottomPath, mPaint)
        mPaint.xfermode = null
        canvas.restore()
    }
}