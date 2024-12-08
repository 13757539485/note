package com.cariad.m2.sharebar.util

import android.annotation.SuppressLint
import android.content.Context
import android.graphics.drawable.Drawable
import android.graphics.drawable.LayerDrawable
import android.os.Build
import android.view.View
import android.view.WindowManager
import androidx.annotation.RequiresApi
import com.cariad.m2.sharebar.R
import java.util.function.Consumer

class BlurWindowHelper(context: Context) {
    private var mWindowManager: WindowManager? = null

    //窗口背景高斯模糊程度
    private var mBackgroundBlurRadius = 0F
    private var mBackgroundCornersRadius = 0F

    // 根据窗口高斯模糊功能是否开启来为窗口设置不同的不透明度
    private val mWindowBackgroundAlphaWithBlur = 170
    private val mWindowBackgroundAlphaNoBlur = 255

    //使用一个矩形drawable文件作为窗口背景，这个矩形的轮廓和圆角确定了窗口高斯模糊的区域
    private val mContext: Context
    private var mView: View? = null
    private var mWindowBackgroundDrawable: Drawable? = null

    init {
        mContext = context
    }

    fun initBlur(view: View?, windowManager: WindowManager) {
        mView = view
        mWindowManager = windowManager
        mBackgroundBlurRadius = dp2px(mContext, 69f).toFloat()
        mBackgroundCornersRadius = dp2px(mContext, 42f).toFloat()
        mWindowBackgroundDrawable = mView!!.background ?: mContext.getDrawable(R.drawable.window_background)
        setupWindowBlurListener()
    }

    private fun setupWindowBlurListener() {
        val windowBlurEnabledListener: Consumer<Boolean> =
            Consumer<Boolean> { blursEnabled: Boolean ->
                updateWindowForBlurs(
                    blursEnabled
                )
            }
        mView!!.addOnAttachStateChangeListener(object : View.OnAttachStateChangeListener {
            override fun onViewAttachedToWindow(v: View) {
                mWindowManager!!.addCrossWindowBlurEnabledListener(windowBlurEnabledListener)
            }

            override fun onViewDetachedFromWindow(v: View) {
                mWindowManager!!.removeCrossWindowBlurEnabledListener(windowBlurEnabledListener)
            }
        })
    }

    private fun updateWindowForBlurs(blursEnabled: Boolean) {
        // 根据窗口高斯模糊功能是否开启来为窗口设置不同的不透明度
        mWindowBackgroundDrawable!!.alpha =
            if (blursEnabled) mWindowBackgroundAlphaWithBlur else mWindowBackgroundAlphaNoBlur //调整背景的透明度
        setBackgroundBlurRadius(mView) //设置背景模糊程度
    }

    /**
     * 为View设置高斯模糊背景
     *
     * @param view
     */
    private fun setBackgroundBlurRadius(view: View?) {
        if (view == null) {
            return
        }
        val target = view.parent
        val backgroundBlurDrawable = getBackgroundBlurDrawableByReflect(target)
        val originDrawable = view.background
        val destDrawable: Drawable = LayerDrawable(arrayOf(backgroundBlurDrawable, originDrawable))
        view.background = destDrawable
    }

    /**
     * 通过反射获取BackgroundBlurDrawable实例对象
     *
     * @param viewRootImpl
     * @return
     */
    @SuppressLint("PrivateApi")
    @RequiresApi(Build.VERSION_CODES.Q)
    private fun getBackgroundBlurDrawableByReflect(viewRootImpl: Any): Drawable? {
        var drawable: Drawable? = null
        try {
            //调用ViewRootImpl的createBackgroundBlurDrawable方法创建实例
            val method_createBackgroundBlurDrawable =
                viewRootImpl.javaClass.getDeclaredMethod("createBackgroundBlurDrawable")
            method_createBackgroundBlurDrawable.isAccessible = true
            drawable = method_createBackgroundBlurDrawable.invoke(viewRootImpl) as Drawable
            //调用BackgroundBlurDrawable的setBlurRadius方法
            val method_setBlurRadius = drawable.javaClass.getDeclaredMethod(
                "setBlurRadius",
                Int::class.javaPrimitiveType
            )
            method_setBlurRadius.isAccessible = true
            method_setBlurRadius.invoke(drawable, mBackgroundBlurRadius.toInt())
            //调用BackgroundBlurDrawable的setCornerRadius方法
            val method_setCornerRadius = drawable!!.javaClass.getDeclaredMethod(
                "setCornerRadius",
                Float::class.javaPrimitiveType
            )
            method_setCornerRadius.isAccessible = true
            method_setCornerRadius.invoke(drawable, mBackgroundCornersRadius)
        } catch (e: Exception) {
            e.printStackTrace()
        }
        return drawable
    }
}

