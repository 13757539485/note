### 虚拟机形式
[虚拟机安装](pdf/虚拟机装mac.pdf)
### 黑苹果形式
#### 1.安装
见[黑苹果安装](pdf/最新10.14黑屏果安装教程.pdf)
#### 2.驱动修复
##### 1.修复亮度之核显教程

**1.删除万能驱动**
/EFI/CLOVER/kexts/other/IntelGraphicsFixup.kext，往里添加FakePCIID_Intel_HD_Graphics.kext，AppleBacklightFixup.kext

**2.使用Clover Configurator**

打开/EFI/CLOVER/config.plist
1）勾选Acpi项中的AddDTGP和AddPNLF，Patches中添加AZAL to HDEF或HDAS to HDEF

2）勾选Devices项中的SetIntelBacklight和SetIntelMacBacklight；System Paramenters可设置开机默认亮度Backlight Level最亮0xFFFF,Inject Kexts选择Detect

**3.查看CPU型号，选择合适的platform-id**
Intel HD Graphics 4400可使用0x0a160000，0x0c160000，0x0a260006

Intel核显platform ID整理及smbios速查表

https://blog.daliansky.net/Intel-core-display-platformID-finishing.html

**4.在Graphics项中填写ig-platform-id**

**5.保存后重启**

##### 2.修复声音之仿冒声卡教程
**1.删除万能驱动**

/EFI/CLOVER/kexts/other/VoodooHDA.kext，往里添加AppleALC.kext和Lilu.kext

**2.使用Clover Configurator**

打开/EFI/CLOVER/config.plist,勾选Acpi项中的FIXHPET

**3.查看声卡型号**

win下可使用aida软件查看，Mac直接使用Hackintool查看

Realtek AppleALC283可使用layout-id如下：
1, 3, 11, 66(本机11)

其他AppleALC支持的Codecs列表

https://blog.daliansky.net/AppleALC-Supported-codecs.html 

笔记本，从大数字改到小数字尝试

4.在Devices项中Audio Inject填写layout-id

5.保存后重启

##### 3.ddst修改教程
1.Command+shift+G，前往/usr/bin目录，并将iasl编译器拖入到该目录

2.aml转dsl：
```
iasl -da -dl *.aml
```
3.使用MaciASL打开dsl进行修改或者打补丁

4.dsl转aml：
```
iasl *.dsl
```
5.复制到EFI/Clover/ACPI/patched/

#### 3.优化
##### 1.隐藏相关启动盘

Clover Configurator中选择Gui项去除Scan中的Legacy，Hide Volume中添加Preboot，Recovery，macOS Install

##### 2.设置主题

Gui中Theme修改名称，名称为EFI/CLOVER/themes中的文件夹名称

##### 3.设置默认启动盘

Boot中Default Boot Volume填写盘名称