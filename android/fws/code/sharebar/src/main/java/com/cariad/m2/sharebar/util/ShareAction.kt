package com.cariad.m2.sharebar.util

import android.content.ActivityNotFoundException
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.drawable.Drawable
import android.net.Uri
import android.os.Build
import android.text.TextUtils
import android.util.Log
import android.widget.Toast
import com.cariad.m2.sharebar.R
import com.cariad.m2.sharebar.core.ShareBarService
import com.cariad.m2.sharebar.map.provideAmapRetrofit
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.net.ConnectException

const val ALL_TYPE = "*/*"

/**
 * 显示任务岛
 */
const val ACTION_SHARE_BAR_SHOW = "action.sharebar.show"

/**
 * 隐藏任务岛
 */
const val ACTION_SHARE_BAR_HIDE = "action.sharebar.hide"

/**
 * 显示手机端发送的消息卡片
 */
const val ACTION_SHARE_BAR_CARD_SHOW = "action.sharebar.card.show"

/**
 * 处理自定义设置允许接收拖拽的应用
 */
const val ACTION_SHARE_BAR_CUSTOM_PKG_DRAG = "action.sharebar.custom.pkg_drag"

/**
 * 高德地图包名
 */
const val PKG_GAODE = "com.autonavi.minimap"

/**
 * 高德地图搜索界面
 */
const val GAODE_SEARCH_CLASS = "com.autonavi.map.activity.NewMapActivity"

/**
 * 美图秀秀包名
 */
const val PKG_MEITU = "com.mt.mtxx.mtxx"

/**
 * 美图秀秀编辑图片界面
 */
const val MEITU_SHARE_CLASS = "com.meitu.mtxx.img.IMGMainActivity"

/**
 * 微信包名
 */
const val PKG_WEIXIN = "com.tencent.mm"

/**
 * 自定义任务岛显示分享应用的包名(后续替换成vivo提供)
 */
val ALLOW_PKG_SHARE_LIST = listOf(PKG_MEITU, PKG_WEIXIN, PKG_GAODE)

const val TAG = "ShareAction"

/**
 * 启动分享应用
 *
 * @param context 上下文，启动Intent使用
 * @param mimeType 分享类型：text、image等，如text传text/plain，不可以使用*
 * @param packageName 启动应用包名
 * @param className 启动应用Activity
 * @param text 分享文本内容，可不传
 * @param uri 分享uri地址(图片地址)，可不传
 *
 */
fun shareApp(
    context: Context,
    mimeType: String,
    packageName: String,
    className: String,
    text: CharSequence?,
    uri: Uri?
) {
    val shareIntent = Intent(Intent.ACTION_SEND)
    shareIntent.type = mimeType
    shareIntent.component = ComponentName(packageName, className)
    text?.let {
        shareIntent.putExtra(Intent.EXTRA_TEXT, it.toString())
    }
    uri?.let {
        shareIntent.putExtra(Intent.EXTRA_STREAM, it)
    }
    shareIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
    try {
        context.startActivity(shareIntent)
    } catch (e: Exception) {
        Toast.makeText(context, "分享失败：${e.message}", Toast.LENGTH_SHORT).show()
    }
}

/**
 * https://lbs.amap.com/api/amap-mobile/gettingstarted/
 * 跳转到高德地图导航
 *
 * @param context 上下文，启动高德地图使用
 * @param poiName 终点地点名
 * @param lat 终点经度
 * @param lon 终点纬度
 *
 */
fun navigateInMap(context: Context, poiName: String, lat: String, lon: String) {
    val intent = Intent(
        Intent.ACTION_VIEW,
        Uri.parse("androidamap://navi?sourceApplication=m2&poiname=${poiName}&lat=${lat}&lon=${lon}&dev=0")
    )
    intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
    try {
        context.startActivity(intent)
    } catch (e: ActivityNotFoundException) {
        Toast.makeText(context, "未安装高德地图", Toast.LENGTH_SHORT).show()
    }
}

/**
 * 高德地图搜索POI2.0接口服务类
 */
private val amapApiService by lazy { provideAmapRetrofit() }

/**
 * 高德地图导航，通过输入地址搜索对应经纬度再去导航，经纬度默认取第1个
 *
 * @param context 上下文，启动高德地图使用
 * @param text 地址
 *
 */
fun navigate(context: Context, text: CharSequence) =
    CoroutineScope(Dispatchers.IO).launch {
        flow {
            val response = amapApiService.searchPlaces(
                keywords = text.toString()
            )
            if (response.isSuccessful) {
                emit(response.body()?.pois?.get(0))
            } else {
                throw java.lang.Exception("search fail")
            }
        }.flowOn(Dispatchers.IO)
            .catch { exception ->
                if (exception is ConnectException) {
                    withContext(Dispatchers.Main) {
                        Toast.makeText(
                            context,
                            "网络异常",
                            Toast.LENGTH_SHORT
                        ).show()
                    }
                }
            }
            .collect { data ->
                data?.let { pois ->
                    val loc = pois.location.split(",")
                    if (loc.isNotEmpty() && loc.size > 1) {
                        withContext(Dispatchers.Main) {
                            navigateInMap(
                                context,
                                pois.name,
                                loc[1],
                                loc[0]
                            )
                        }
                    }
                }
            }
    }

/**
 * 分享应用数据类
 */
data class ShareAppInfo(
    val icon: Drawable, // 应用图标
    val label: CharSequence, // 应用名称
    val packageName: String, // 应用包名
    val className: String, // 应用分享启动类
    val customUri: Boolean = false // 是否通过uri形式启动界面
)

/**
 * 系统查询接口
 *
 * @param type 通过type查询支持分享的应用集合，
 * "*\/\*"表示所有类型,
 * "text/\*"表示文本类型,
 * "image/\*"表示图片类型
 *
 * @return 分享应用集合
 */
fun queryAppInfo(
    context: Context,
    type: String,
    targetPackage: String? = null,
    cacheShareAppInfo: ((cache: ShareAppInfo)->Unit)? = null)
: List<ShareAppInfo> {
    Log.e(TAG, "queryAppInfo targetPackage: $targetPackage ,type: $type")
    val packageManager = context.packageManager
    val intent = Intent(Intent.ACTION_SEND, null)
    intent.addCategory(Intent.CATEGORY_DEFAULT)
    intent.setType(type)
    // 过滤需要的应用
    val list = mutableListOf<ShareAppInfo>()
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
        packageManager.queryIntentActivities(
            intent,
            PackageManager.ResolveInfoFlags.of(0L)
        )
    } else {
        packageManager.queryIntentActivities(
            intent, 0
        )
    }.filter {
        if (!TextUtils.isEmpty(targetPackage)) {
            it.activityInfo.packageName == targetPackage
        } else {
            ALLOW_PKG_SHARE_LIST.contains(it.activityInfo.packageName)
        }
    }.forEach {
        if (it.activityInfo.packageName == PKG_GAODE) {
            list.add(
                ShareAppInfo(
                    it.loadIcon(packageManager),
                    "${it.loadLabel(packageManager)}${context.resources.getString(R.string.amap_action_search)}",
                    it.activityInfo.packageName,
                    it.activityInfo.name
                )
            )
            list.add(
                ShareAppInfo(
                    it.loadIcon(packageManager),
                    "${it.loadLabel(packageManager)}${context.resources.getString(R.string.amap_action_navigate)}",
                    it.activityInfo.packageName,
                    it.activityInfo.name, true
                )
            )
        } else {
            list.add(
                ShareAppInfo(
                    it.loadIcon(packageManager),
                    it.loadLabel(packageManager),
                    it.activityInfo.packageName,
                    it.activityInfo.name
                )
            )
        }
    }
    if (!TextUtils.isEmpty(targetPackage) && list.size == 1) {
        cacheShareAppInfo?.invoke(list[0])
    }
    return list
}