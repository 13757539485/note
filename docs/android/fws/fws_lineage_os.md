## LineageOS
基于aosp定制并持续跟进版本，源自CyanogenMod项目，由全球开发者社区共同维护
## Pixel6刷LineageOS
## 刷机体验
1. 进入fastboot模式
```shell
adb -d reboot bootloader
```
或者按Volume Down + Power

2. 查看设备
```shell
fastboot devices
```
3. 解锁设备
```shell
fastboot flashing unlock
```
4. 下载[镜像包](./fws_lineage_os.md#pixel6镜像包)
- boot.img
- dtbo.img
- vendor_boot.img

分别刷入
```shell
fastboot flash boot boot.img
fastboot flash dtbo dtbo.img
fastboot flash vendor_boot vendor_boot.img
```
提示OKAY [  0.000s]字样即可

其中vendor_boot.img为Lineage Recovery

5. 进入Recovery

在fastboot模式中使用Volume键进行切换，Recovery Mode

6. 清空数据

选择Factory Reset然后Format data / factory reset

7. 下载刷机rom

lineage-xxx-nightly-oriole-signed.zip

选择Apply Update然后Apply from ADB

电脑端执行
```shell
adb -d sideload lineage-xxx-nightly-oriole-signed.zip
```
### Pixel6镜像包
https://download.lineageos.org/devices/oriole/builds

### 构建源码
https://wiki.lineageos.org/devices/oriole/build/

1. 初始化仓库
```shell
repo init -u https://mirrors4.tuna.tsinghua.edu.cn/git/lineageOS/LineageOS/android.git -b lineage-21.0 --git-lfs
```
2. 同步源码
```shell
repo sync -j20
```
3. 初始化环境
```shell
source build/envsetup.sh
breakfast oriole eng
```
4. 安装pixel6相关配置

连接手机执行
```shell
cd device/google/oriole
./extract-files.sh
```
如果手机当前系统版本和源码版本不一致需要单独下载固件提取
```shell
./extract-files.sh vendor.img
```
官方没有20版本，20版本：TQ3A.230901.001

https://pan.baidu.com/s/1Uc2p3HWTfBzvjRAtUvD7Ew 提取码: 8888

解压到vendor/google/oriole/

5. 编译源码
```shell
croot
brunch oriole eng
```
lunch没有选项
```
build_build_var_cache
```

对于20版本：lunch lineage_oriole-userdebug

对于21版本：lunch lineage_oriole-ap2a-userdebug

6. breakfast & brunch

源码路径：

vendor/lineage/build/envsetup.sh

默认是运行userdebug版本