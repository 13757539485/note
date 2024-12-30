官方中文网：https://docs.flutter.cn/community/china/

组件库：

https://pub.dev

https://pub-web.flutter-io.cn/


### 配置java环境
```shell
sudo apt install openjdk-17-jdk
```
### 配置flutter环境
```shell
export FLUTTER_SDK="/xxx/FlutterSDK/flutter_linux_3.10.3-stable/flutter/bin"
export PATH=$PATH:$FLUTTER_SDK
export PUB_HOSTED_URL="https://pub.flutter-io.cn"
export FLUTTER_STORAGE_BASE_URL="https://storage.flutter-io.cn"
```
### 配置Android环境
```shell
export ANDROID_HOME=/xxx/AndroidSDK/
export IDE_HOME=/opt/android-studio-for-platform
export PATH=$PATH:$ANDROID_HOME/bin:$ANDROID_HOME/tools:$ANDROID_HOME/platform-tools:$ANDROID_HOME/cmdline-tools/latest/bin
```
flutter会修改androidsdk成/user/lib/android-sdk
```shell
flutter config --android-sdk /xxx/AndroidSDK/
```
### 切换sdk版本
安装 fvm
```shell
dart pub global activate fvm
```
卸载 fvm
```shell
dart pub global deactivate fvm
```
安装最新稳定版本
```shell
fvm install stable
```
项目使用某个版本
```shell
fvm use stable
```
全局使用
```shell
fvm global stable
```
### 常见问题
#### fvm安装sdk报错

git error: RPC failed； curl 92 HTTP/2 stream 0 was not closed cleanly: PROTOCOL_ERROR (err 1)
```
git config --global HTTP/1.1
```

#### 运行web无法联网
/fluttersdk/packages\flutter_tools\lib\src\web的chrome.dart文件，添加 ‘–disable-web-security’
```
'--disable-translate',
'--disable-web-security',
if (headless)
```
删除/fluttersdk/bin/cache/下的文件

flutter_tools.snapshot

flutter_tools.stamp