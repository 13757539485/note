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
## <a id="name_mod">常见模块编译</a>
|模块名|make方式命令|mmm方式命令|
|--|--|--|
|init|make init|mmm system/core/init|
|zygote|make app_process|mmm frameworks/base/cmds/app_process|
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

## ninja
### 配置环境
```shell
ln -sf out/combined-***.ninja build.ninja
```
如oriole设备
```shell
ln -sf out/combined-aosp_oriole.ninja build.ninja
```
保证ninja命令存在
```shell
cp prebuilts/build-tools/linux-x86/bin/ninja out/host/linux-x86/bin/
```
### 编译
整编
```shell
ninja
```
单编和make一样，如
```shell
ninja services
```
### 常见问题
1. 缺少so库，如缺少libjemalloc5.so
```shell
sudo cp prebuilts/build-tools/linux-x86/lib64/libjemalloc5.so /usr/lib/
```
2. Android.bp修改
```shell
rm -rf out/soong/.temp
m soong
ninja -f out/build-***.ninja
```
