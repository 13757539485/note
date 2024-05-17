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