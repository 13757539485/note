## 编译镜像命令
|命令|镜像文件|
|--|--|
|make snod|编译System.img |
|make vnod|编译vendor.img |
|make pnod|编译product.img|
|make senod|编译system_ext.img|
|make onod|编译odm.img|

## 清除命令
|命令|作用|
|--|--|
|m clobber|清除所有编译缓存|
|m clean|清除编译缓存，等价于rm -rf out/|
|m installclean|清除所有二进制文件|

## 查找命令
|命令|查找范围|
|--|--|
|jgrep|在所有java文件中查找|
|cgrep|在所有c/c++文件中查找|
|resgrep|在所有res/*.xml文件中查找|
|ggrep|在所有Gradle文件中查找|
|mangrep|在所有AndroidManfest.xml文件中查找|
|mgrep|在所有Makefiles和*.bp文件中查找|

## 其他命令
|命令|作用|
|--|--|
|printconfig|打印当前配置信息|
|allmod|显示aosp所有module|
|pathmod [module name]|显示module所在路径|
|refreshmod|刷新module列表|
|gomod [module name]|定位到指定module目录|
|croot|回到aosp根目录|

模块编译：make [module name]
## 常见模块编译
|模块名|make方式命令|mmm方式命令|
|--|--|--|
|init|make init|mmm system/core/init|
|zygote|make app_process|mmm frameworks/base/cmds/app_process|
|system_server|make services|mmm frameworks/base/services|
|framework-res|make framework-res|mmm frameworks/base/core/res|
|framework-jni|make libandroid_runtime|mmm frameworks/base/core/jni|
|framework|framework-minus-apex|mmm frameworks/base/core/java|
|services|make services|mmm frameworks/base/core/services|
|libandroid_servers|make libandroid_servers|mmm frameworks/base/core/jni|
|libsensorservice|make libsensorservice|mmm frameworks/native/services/sensorservice|
|sensorservice|make sensorservice|mmm frameworks/native/services/sensorservice|
|binder|make libbinder|mmm frameworks/native/libs/binder|

其中编译framework

make framework(Android12之后使用make framework-minus-apex)
或者直接mmm frameworks/base

## 生成系统签名文件
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
