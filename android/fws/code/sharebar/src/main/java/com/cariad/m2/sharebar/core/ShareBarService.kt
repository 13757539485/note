package com.cariad.m2.sharebar.core

import android.app.Service
import android.content.ClipData
import android.content.Intent
import android.graphics.BitmapFactory
import android.os.ParcelFileDescriptor
import android.util.Log
import android.view.LayoutInflater
import androidx.core.view.isVisible
import androidx.recyclerview.widget.LinearLayoutManager
import com.cariad.m2.ICariadDrag
import com.cariad.m2.ICariadDragListener
import com.cariad.m2.sharebar.databinding.WindowShareBarBinding
import com.cariad.m2.sharebar.databinding.WindowShareBarCardBinding
import com.cariad.m2.sharebar.ui.ShareBarAdapter
import com.cariad.m2.sharebar.ui.ShareBarWindowManager
import com.cariad.m2.sharebar.util.ACTION_SHARE_BAR_CARD_SHOW
import com.cariad.m2.sharebar.util.ACTION_SHARE_BAR_CUSTOM_PKG_DRAG
import com.cariad.m2.sharebar.util.ACTION_SHARE_BAR_HIDE
import com.cariad.m2.sharebar.util.ACTION_SHARE_BAR_SHOW
import com.cariad.m2.sharebar.util.ALL_TYPE
import com.cariad.m2.sharebar.util.GAODE_SEARCH_CLASS
import com.cariad.m2.sharebar.util.MEITU_SHARE_CLASS
import com.cariad.m2.sharebar.util.MediaUtil
import com.cariad.m2.sharebar.util.PKG_GAODE
import com.cariad.m2.sharebar.util.PKG_MEITU
import com.cariad.m2.sharebar.util.ShareAppInfo
import com.cariad.m2.sharebar.util.dp2px
import com.cariad.m2.sharebar.util.navigate
import com.cariad.m2.sharebar.util.px2dp
import com.cariad.m2.sharebar.util.queryAppInfo
import com.cariad.m2.sharebar.util.shareApp
import com.google.android.flexbox.FlexDirection
import com.google.android.flexbox.FlexWrap
import com.google.android.flexbox.FlexboxLayoutManager
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.MainScope
import kotlinx.coroutines.launch


/**
 * 用来管理任务岛的服务类
 */
class ShareBarService : Service() {
    companion object {
        private const val TAG = "ShareBarService"
    }

    private val windowBing by lazy { WindowShareBarBinding.inflate(LayoutInflater.from(this)) }
    private val cardBing by lazy { WindowShareBarCardBinding.inflate(LayoutInflater.from(this)) }
    private val shareBarAdapter by lazy { ShareBarAdapter() }
    private val coroutineScope = CoroutineScope(Dispatchers.IO)
    private val mainScope = MainScope()
    private var tempShareAppInfo: ShareAppInfo? = null

    override fun onBind(p0: Intent?) = object : ICariadDrag.Stub() {
        override fun searchShareIntent(targetPackage: String, type: String): Int {
            return if ((targetPackage == PKG_GAODE && type.startsWith("text"))
                || targetPackage == PKG_MEITU
            ) 1
            else queryAppInfo(this@ShareBarService, type, targetPackage) {
                tempShareAppInfo = it
            }.size
        }

        override fun saveBitmap(pfd: ParcelFileDescriptor?, listener: ICariadDragListener?) {
            pfd?.let {
                coroutineScope.launch {
                    val bitmap = BitmapFactory.decodeFileDescriptor(it.fileDescriptor)

                    MediaUtil.saveBitmap(bitmap, this@ShareBarService, listener)
                }
            }
        }

        override fun shareBarShowOrHide(action: String, targetPackage: String, data: ClipData?) {
            Log.d(
                TAG,
                "shareBarShowOrHide() called with: action = $action, targetPackage = $targetPackage, data = $data"
            )
            mainScope.launch {
                when (action) {
                    ACTION_SHARE_BAR_SHOW -> {
                        data?.let { clipData ->
                            showShareBarWin(clipData)
                        }
                    }

                    ACTION_SHARE_BAR_HIDE -> {
                        ShareBarWindowManager.dismiss()
                    }

                    ACTION_SHARE_BAR_CUSTOM_PKG_DRAG -> {
                        customDrag(targetPackage, data)
                    }

                    else -> {}
                }
            }
        }
    }

    private fun showShareBarWin(data: ClipData) {
        val type =
            if (data.description == null) {
                ALL_TYPE
            } else {
                val mimeTypeCount = data.description.mimeTypeCount
                if (mimeTypeCount > 0) {
                    data.description.getMimeType(0)
                } else {
                    ALL_TYPE
                }
            } ?: ALL_TYPE
        val appInfos = queryAppInfo(this@ShareBarService, type)
        Log.e(TAG, "onStartCommand show bar type: $type ${appInfos.size}")
        if (appInfos.isNotEmpty()) {
            shareBarAdapter.setNewData(appInfos)
            val baseHeight = ((appInfos.size + 7) / 8).coerceAtMost(3) * dp2px(this@ShareBarService, 124F)
            val listHeight = baseHeight + dp2px(this@ShareBarService, 25F)
            windowBing.shareBarList.layoutParams.height = listHeight
            ShareBarWindowManager.create(
                windowBing.root,
                windowWidth = (appInfos.size.coerceAtMost(8)
                        * dp2px(this@ShareBarService, 96F))
                        + dp2px(this@ShareBarService, 85F),
                windowHeight = listHeight + dp2px(this@ShareBarService, 51F)
            )
        }
    }

    override fun onCreate() {
        super.onCreate()
        windowBing.shareBarList.layoutManager =
            FlexboxLayoutManager(this, FlexDirection.ROW, FlexWrap.WRAP)
        windowBing.shareBarList.adapter = shareBarAdapter
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        intent?.let {
            if (it.action == ACTION_SHARE_BAR_CARD_SHOW) {
                cardShow(it)
            }
            if (it.action == "test_open") {
                val appInfos = queryAppInfo(this@ShareBarService, ALL_TYPE)
                if (appInfos.isNotEmpty()) {
                    shareBarAdapter.setNewData(appInfos)
                    Log.e(TAG, "onStartCommand: ${appInfos.size}")
                    ShareBarWindowManager.create(
                        windowBing.root,
                        windowWidth = (appInfos.size * dp2px(this@ShareBarService, 96F))
                                + dp2px(this@ShareBarService, 35F)
                    )
                }
            } else if (it.action == "test_close") {
                ShareBarWindowManager.dismiss()
            }
        }
        return super.onStartCommand(intent, flags, startId)
    }

    /**
     * 接收由手机端直接发送的信息，只支持调用高德地图处理
     */
    private fun cardShow(intent: Intent) {
        ShareBarWindowManager.create(
            cardBing.root,
            tag = ShareBarWindowManager.TAG_SHARE_BAR_CARD,
            params = ShareBarWindowManager.cardParams()
        )
        cardBing.cardImg.setImageBitmap(null)
        if (intent.data == null) {
            cardBing.firstText.isVisible = true
            cardBing.secondText.isVisible = true
            cardBing.cardImg.layoutParams.apply {
                width = dp2px(this@ShareBarService, 400f)
                height = dp2px(this@ShareBarService, 180f)
            }
        } else {
            cardBing.firstText.isVisible = false
            cardBing.secondText.isVisible = false
            contentResolver.openInputStream(intent.data!!)?.use {
                val bitmap = BitmapFactory.decodeStream(it)
                cardBing.cardImg.layoutParams.apply {
                    width = px2dp(this@ShareBarService, bitmap.width.toFloat())
                    height = px2dp(this@ShareBarService, bitmap.height.toFloat())
                }
                cardBing.cardImg.setImageBitmap(bitmap)
            }
        }
        intent.getStringExtra("text")?.let { text ->
            val content = text.split("\r?\n".toRegex())
            if (content.size > 1) {
                cardBing.firstText.text = content[0]
                cardBing.secondText.text = content[1]
            }
            cardBing.root.setOnClickListener {
                shareApp(
                    this@ShareBarService,
                    "text/plain",
                    PKG_GAODE,
                    GAODE_SEARCH_CLASS,
                    if (intent.data == null) content[1] else content[0],
                    null
                )
                ShareBarWindowManager.dismiss(ShareBarWindowManager.TAG_SHARE_BAR_CARD)
            }
        }
    }

    /**
     * 自定义应用窗口支持拖拽功能，支持列表由framework中{@link CariadDragHelper#handleDragEvent}决定
     */
    private fun customDrag(pkg: String, clipData: ClipData?) {
        // 如果是高德或者美图执行分享功能
        clipData?.let {
            if (PKG_GAODE == pkg && it.itemCount > 0) {
                val itemAt = it.getItemAt(0)
                itemAt.text?.let { text ->
                    navigate(this@ShareBarService, text)
                }
            } else if (PKG_MEITU == pkg && it.itemCount > 0) {
                val itemAt = it.getItemAt(0)
                itemAt.uri?.let { uri ->
                    shareApp(
                        this@ShareBarService,
                        "image/*",
                        PKG_MEITU,
                        MEITU_SHARE_CLASS,
                        null, uri
                    )
                }
            } else {
                Log.e(TAG, "hide pkg: $pkg")
                tempShareAppInfo?.let { info ->
                    val type = it.description?.let { desc ->
                        if (desc.mimeTypeCount > 0) desc.getMimeType(0)
                        else ""
                    } ?: ""
                    if (type.isNotEmpty() && it.itemCount > 0 && pkg == info.packageName) {
                        val itemAt = it.getItemAt(0)
                        shareApp(
                            this@ShareBarService,
                            type, pkg, info.className,
                            itemAt.text, itemAt.uri
                        )
                    }
                }

                it.description?.let { desc ->
                    for (i in 0 until desc.mimeTypeCount) {
                        val itemAt = desc.getMimeType(i)
                        Log.e(TAG, "desc: $itemAt")
                    }
                }

                for (i in 0 until it.itemCount) {
                    val itemAt = it.getItemAt(i)
                    Log.e(
                        TAG, "clip: ${itemAt.text} " +
                                "${itemAt.uri} "
                    )
                }
            }
        }
    }
}