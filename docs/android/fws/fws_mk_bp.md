### 内置apk到系统
编写mk文件，将gradle编译出来的apk重新前面打包成系统apk
```mk
LOCAL_PATH:=$(call my-dir)
include $(CLEAR_VARS)
LOCAL_SRC_FILES := build/outputs/apk/release/myapp-release.apk
LOCAL_MODULE_CLASS := APPS
#可以为user、eng、tests、optional，optional代表在任何版本下都编译
LOCAL_MODULE_TAGS := optional
#编译模块的名称
LOCAL_MODULE := MyApp
#可以为testkey、platform、shared、media、PRESIGNED（使用原签名），platform代表为系统应用
LOCAL_CERTIFICATE := platform
#不设置或者设置为false，安装位置为system/app，如果设置为true，则安装位置为system/priv-app
LOCAL_PRIVILEGED_MODULE := false
#module的后缀，可不设置
LOCAL_MODULE_SUFFIX := $(COMMON_ANDROID_PACKAGE_SUFFIX)
# 关闭预编译，不会生成OAT文件
LOCAL_DEX_PREOPT := true
LOCAL_PRODUCT_MODULE := false
include $(BUILD_PREBUILT)
```
LOCAL_PRODUCT_MODULE为true产出目录在/product/，否则在/system/下

系统应用白名单，编译镜像时包含自己的apk，最终编译成system/app/Myapp

build/make/target/product/handheld_system.mk
```mk
PRODUCT_PACKAGES += \
    MyApp \

PRODUCT_SYSTEM_SERVER_APPS += \
    MyApp \
```

如果想编译到其他目录handheld_product.mk和handheld_system_ext.mk


参考：https://blog.csdn.net/etrospect/article/details/128235015

#### 输入法内置
参考：https://www.jianshu.com/p/e782897c6ab8

frameworks/base/services/core/java/com/android/server/InputMethodManagerService.java

buildInputMethodListLocked方法增加以下代码(待确认)
```java
// 内置搜狗输入法
String defaultIme = Settings.Secure.getString(mContext.getContentResolver(),
        Settings.Secure.DEFAULT_INPUT_METHOD);
if (defaultIme == null) {
    try {
        Settings.Secure.putString( mContext.getContentResolver(),
                Settings.Secure.DEFAULT_INPUT_METHOD, "com.sohu.inputmethod.sogou/.SogouIME");
    } catch (Exception e) {
    }
}
```

- 百度：com.baidu.input/.ImeService
- 讯飞：com.iflytek.inputmethod/.FlyIME
- 腾讯：com.tencent.qqpinyin/.QQPYInputMethodService
- 谷歌：com.google.android.inputmethod.pinyin/.PinyinIME
- 搜狗：com.sohu.inputmethod.sogou/.SogouIME
- 触宝：com.cootek.smartinput5/.TouchPalIM

针对LOCAL_PREBUILT_JNI_LIBS := \

可使用命令将当前文件夹下的so文件名到文本中，批量将#替换掉即可
```shell
find "$(pwd)" -type f -printf "%p#\n" > fullpaths.txt
```
百度输入法编译报错解决：

bp文件中添加
```
LOCAL_ENFORCE_USES_LIBRARIES := false
```
更多校验查看官方文档：https://source.android.google.cn/devices/tech/dalvik/art-class-loader-context?hl=zh-cn

### 开启Sta和Ap并发
wifi模组：正基AP6275系列 模组支持STA+AP并发

device/xxx/BoardConfig.mk中添加标志位
```mk
WIFI_HIDL_FEATURE_DUAL_INTERFACE:=true
```

kernel-5.10/drivers/net/wireless/rockchip_wlan/rkwifi/bcmdhd/Makefile开放
```mk
CONFIG_BCMDHD_STATIC_IF :=y

ifeq ($(CONFIG_BCMDHD_STATIC_IF),y)
    DHDCFLAGS += -DWL_STATIC_IF
endif
```

### 设备强制横屏
适用于Android11以上

https://blog.csdn.net/u011774634/article/details/125508483

frameworks/base/services/core/java/com/android/server/wm/DisplayContent.java
```java
int getOrientation() {
    if (true) {
        return SCREEN_ORIENTATION_LANDSCAPE;
    }
    //...
}
```

frameworks/base/services/core/java/com/android/server/wm/DisplayRotation.java
```java
boolean updateRotationUnchecked(boolean forceUpdate) {
    if (true) {
        return true;
    }
    //...
}
```

### 修改默认语言
源码路径：build/tools/buildinfo.sh
```sh
echo "persist.sys.language=zh"
echo "persist.sys.country=CN"
echo "persist.sys.localevar="
echo "persist.sys.timezone=Asia/Shanghai"
echo "ro.product.locale.language=zh"
echo "ro.product.locale.region=CN"
```

源码路径：device/系统品牌/系统型号/device.mk
```mk
PRODUCT_LOCALES := zh_CN
```
源码路径：build/make/target/product/languages_default.mk将中文(zh_CN)放到最前面
```mk
PRODUCT_LOCALES := \
        zh_CN \
```
### framework源码添加接口

#### 问题
以View源码中添加属性为例
```java
public class View implements xxx {
    // ...
    // 添加属性
    boolean mEnablePressedAnim;
    // ...
}
```
默认情况下三方应用通过反射添加会报
```
Accessing hidden field Landroid/view/View;->mEnablePressedAnim:Z (blocked, reflection, denied)
```
反射代码
```kotlin
private fun fieldChange(view: View, isEnable: Boolean) {
    try {
        val field = View::class.java.getDeclaredField("mEnablePressedAnim")
        field.isAccessible = true
        field.setBoolean(view, isEnable)
    } catch (e: Exception) {
        e.printStackTrace()
    }
}
```
#### 原因和作用

是google对非SDK的api进行名单限制

https://developer.android.google.cn/guide/app-compatibility/restrictions-non-sdk-interfaces?hl=bg

除了到处csv查看api名单标记，还可以反编译得到smali文件查看
```smali
.field blacklist mEnablePressedAnim:Z
```
#### 解决方案
为了允许三方应用调用或反射调用

##### 临时方案
```shell
adb shell settings put global hidden_api_policy 1
```

##### 永久方案之aosp12及以上
源码文件：build/soong/apex/platform_bootclasspath_test.go

搜索TestPlatformBootclasspath_Fragments
platform_bootclasspath下的
```go
hidden_api: {
	// ...
	unsupported_packages: [
		"unsupported-packages.txt",
	],
	//添加以下
	sdk: [
		"sdk.txt",
	],
},
```
bootclasspath_fragment下的
```go
hidden_api: {
	// ...
	unsupported_packages: [
		"bar-unsupported-packages.txt",
	],
	//添加以下
	sdk: [
		"bar-sdk.txt",
	],
},
```
源码文件：build/soong/java/hidenapi_modular.go

搜索type HiddenAPIFlagFileProperties struct
```go
Unsupported_packages []string `android:"path"`
//添加以下
Sdk []string `android:"path"`
```
搜索[]*hiddenAPIFlagFileCategory
```go
{
	PropertyName: "unsupported_packages",
	propertyValueReader: func(properties *HiddenAPIFlagFileProperties) []string {
		return properties.Unsupported_packages
	},
	commandMutator: func(command *android.RuleBuilderCommand, path android.Path) {
		command.FlagWithInput("--unsupported ", path).Flag("--packages ")
	},
},
//添加以下
{
	PropertyName: "sdk",
	propertyValueReader: func(properties *HiddenAPIFlagFileProperties) []string {
		return properties.Sdk
	},
	commandMutator: func(command *android.RuleBuilderCommand, path android.Path) {
		command.FlagWithInput("--sdk ", path)
	},
},
```
源码路径: framework/base/boot/hiddenapi/

添加文件hiddenapi-sdk.txt，添加需要暴露的接口属性等，如View中添加属性boolean mEnablePressedAnim则对应添加
```text
Landroid/view/View;->mEnablePressedAnim:Z
```
源码文件：framework/base/boot/Android.bp
```mk
unsupported_packages: [
    "hiddenapi/hiddenapi-unsupported-packages.txt",
],
//添加以下
sdk: [
    "hiddenapi/hiddenapi-sdk.txt",
],
```
执行命令：
```shell
m api-stubs-docs-non-updatable-update-current-api
m -j20
```

##### 永久方案之aosp11及以下
源码文件：
直接添加包名过滤

framework/base/config/hiddenapi-greylist-packages.txt

通过添加具体属性或方法过滤

framework/base/config/hiddenapi-greylist.txt

或者
源码文件：/build/soong/java/hiddenapi_singleton.go
```go
FlagWithInput("--greylist-packages ",
 			android.PathForSource(ctx, "frameworks/base/config/hiddenapi-greylist-packages.txt")).
//添加以下
FlagWithInput("--whitelist ",
            android.PathForSource(ctx, "frameworks/base/config/hiddenapi-sdk.txt")).
```
对应路径添加文件hiddenapi-sdk.txt

##### 窗口模糊
对于Android12开启系统窗口模式

/device/设备/型号/xxx.mk
PRODUCT_PROPERTY_OVERRIDES += \
       ro.surface_flinger.supports_background_blur=1

### 导入jar或aar步骤
Android.bp中
```bp
java_import {
    name: "my_module_jar",
    jars: [
        "libs/my_module.jar", 
        "libs/kotlinx-coroutines-core-jvm-1.6.0.jar",
    ],
}
android_library_import {
    name:"my_module_aar",
    aars: [
        "libs/my_module-debug.aar", 
    ],
    sdk_version: "current",
}
```
如在framework/base/Android.bp调用
```
java_library {
    name: "framework-minus-apex",
    //...
    static_libs: [
       "my_module_jar",
       "my_module_aar",
    ],
}
```