官方中文网：https://docs.flutter.cn/community/china/

组件库：https://pub.dev

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
