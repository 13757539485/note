### <a id="vendor">vendor(厂商源码)定制</a>
以oriole手机为例：

aosp中：device/google/raviole/aosp_oriole.mk

lineage中：device/google/raviole/lineage_oriole.mk

添加引入
```mk
$(call inherit-product-if-exists, vendor/hfc/hfc.mk)
```
新建vendor/hfc/hfc.mk
添加引入
```mk
$(call inherit-product-if-exists, vendor/hfc/apps/apps.mk)
```
新建apps文件夹，新建apps.mk
```mk
PRODUCT_PACKAGES += \
	QQInput \
	SystemUIPlugin
```

### <a id="vendor_app">内置apk到系统</a>
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

### <a id="mk_bp">mk转bp</a>
mk语法：https://blog.csdn.net/u012514113/article/details/124384430

使用androidmk工具

路径：out/host/linux-x86/bin/androidmk

没有则执行
```
m -j blueprint_tools
```

mk转bp命令
```
androidmk /xxx/Android.mk > /xxx/Andoroid.bp
```

预装应用可卸载(待测试)
```
LOCAL_PATH := $(call my-dir)

include $(CLEAR_VARS)

LOCAL_MODULE := Test
LOCAL_SRC_FILES := Test.apk
LOCAL_MODULE_CLASS := APPS
LOCAL_MODULE_TAGS := optional
LOCAL_CERTIFICATE := platform
LOCAL_MODULE_SUFFIX := $(COMMON_ANDROID_PACKAGE_SUFFIX)
LOCAL_MODULE_PATH := $(TARGET_OUT_DATA_APPS)//重点这一句

include $(BUILD_PREBUILT)
```

### <a id="system_sign">生成系统签名文件</a>
aosp源码路径：build/target/product/security/

platform.pk8

platform.x509.pem
```shell
openssl pkcs8 -in platform.pk8 -inform DER -outform PEM -out platform.pem -nocrypt
```

```shell
openssl pkcs12 -export -in platform.x509.pem -inkey platform.pem -out platform.pk12 -name dev
```

```shell
keytool -importkeystore -deststorepass 123456 -destkeystore platform.jks -srckeystore platform.pk12 -srcstoretype PKCS12 -srcstorepass 123456
```

build.gradle中添加
```gradle
signingConfigs {
    config {
        storeFile file("platform.jks")
        storePassword '123456'
        keyAlias 'dev'
        keyPassword '123456'
    }
}

buildTypes {
    release {
        proguardFiles getDefaultProguardFile('proguard-android.txt'), 'proguard-rules.pro'
        signingConfig signingConfigs.config
    }
}
```
在Androidmanifest.xml添加
```xml
android:sharedUserId="android.uid.system"
```
