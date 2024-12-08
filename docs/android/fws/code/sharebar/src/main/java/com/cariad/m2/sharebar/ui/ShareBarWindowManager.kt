package com.cariad.m2.sharebar.ui

import android.app.Service
import android.content.Context
import android.graphics.PixelFormat
import android.util.Log
import android.view.Gravity
import android.view.View
import android.view.ViewGroup
import android.view.WindowManager
import android.view.WindowManager.LayoutParams
import com.cariad.m2.sharebar.R
import com.cariad.m2.sharebar.util.getStatusBarHeight

/**
 * 悬浮窗辅助类，用于快速创建悬浮窗
 */
object ShareBarWindowManager {
    const val TAG = "ShareBarWindowManager"
    const val TAG_SHARE_BAR = "share_bar"
    const val TAG_SHARE_BAR_CARD = "share_bar_card"
    private val tagToView by lazy { HashMap<String, View>() }
    private lateinit var windowManager: WindowManager

    /**
     * 创建并显示悬浮窗
     *
     * @param windowView 窗口显示的view
     * @param tag 用来标记窗口，方便管理不同悬浮窗
     * @param params 浮窗参数配置
     *
     */
    fun create(
        windowView: View,
        tag: String = TAG_SHARE_BAR,
        windowWidth: Int = LayoutParams.WRAP_CONTENT,
        windowHeight: Int = LayoutParams.WRAP_CONTENT,
        params: LayoutParams = defaultParams(windowView.context, windowWidth, windowHeight)
    ) {
        if (tagToView[tag] != null) {
            Log.e(TAG, "window already create")
            return
        }
        tagToView[tag] = windowView
        if (!::windowManager.isInitialized) {
            windowManager = windowView.context.getSystemService(Service.WINDOW_SERVICE) as WindowManager
        }
//        BlurWindowHelper(windowView.context).initBlur(windowView, windowManager)
        addView(tag, params)
    }

    private fun defaultParams(context: Context, windowWidth: Int, windowHeight: Int) = LayoutParams().apply {
        type = LayoutParams.TYPE_APPLICATION_OVERLAY - 20
        format = PixelFormat.TRANSPARENT
        gravity = Gravity.TOP or Gravity.CENTER_HORIZONTAL
        flags =
            LayoutParams.FLAG_NOT_TOUCH_MODAL or LayoutParams.FLAG_NOT_FOCUSABLE or
                    LayoutParams.FLAG_LAYOUT_NO_LIMITS
        width = windowWidth
        height = windowHeight
        this.y = getStatusBarHeight(context)
        windowAnimations = R.style.ShareBarWindowAnimation
    }

    fun cardParams() = LayoutParams().apply {
        type = LayoutParams.TYPE_APPLICATION_OVERLAY
        format = PixelFormat.RGBA_8888
        gravity = Gravity.TOP or Gravity.END
        flags = LayoutParams.FLAG_NOT_TOUCH_MODAL or LayoutParams.FLAG_NOT_FOCUSABLE
        width = LayoutParams.WRAP_CONTENT
        height = LayoutParams.WRAP_CONTENT
        windowAnimations = R.style.ShareBarCardWindowAnimation
    }

    /**
     * 关闭浮窗
     *
     * @param tag 通过tag关闭对应浮窗
     */
    fun dismiss(tag: String = TAG_SHARE_BAR) {
        remove(tag)
    }

    fun isShowing(tag: String = TAG_SHARE_BAR) = tagToView[tag] != null

    private fun addView(tag: String = TAG_SHARE_BAR, params: LayoutParams) = try {
        val view = tagToView[tag] ?: throw Exception("view is null")
        if (view.parent != null) {
            (view.parent as ViewGroup).removeView(view)
        }
        windowManager.addView(view, params)
    } catch (e: Exception) {
        Log.e(TAG, "浮窗添加出现异常：$e")
    }

    private fun remove(tag: String = TAG_SHARE_BAR) = try {
        tagToView[tag]?.let {
            windowManager.removeView(it)
            tagToView.remove(tag)
        }
    } catch (e: Exception) {
        Log.e(TAG, "浮窗关闭出现异常：$e")
    }


}