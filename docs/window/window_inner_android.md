#### 1.开启windows功能(需要重启)
打开控制面板-程序-启动或关闭windows功能

勾选Hyper-V和虚拟机平台

**对于Win11家庭版**

新建文件名为：开启Hyper-V.cmd，内容如下，保持双击执行
````
pushd "%~dp0"
dir /b %SystemRoot%\servicing\Packages*Hyper-V*.mum >hyper-v.txt
for /f %%i in ('findstr /i . hyper-v.txt 2^>nul') do dism /online /norestart /add-package:"%SystemRoot%\servicing\Packages%%i"
del hyper-v.txt
Dism /online /enable-feature /featurename:Microsoft-Hyper-V-All /LimitAccess /ALL
````
#### 2.下载子系统
打开设置-时间和语言-语言和区域

将国家和地区修改成美国，下载以下软件

Windows Subsystem for Android下载链接：(浏览器打开点get in store app)
https://www.microsoft.com/store/productId/9P3395VX91NR
#### 3.连接设备
开始中打开适用于Android的Windows子系统设置软件，选择开发人员，开启开发人员模式，记住adb的ip连接比如
```
adb connect 127.0.0.1:58526
```
连接后使用
```
adb install xxx.apk
```