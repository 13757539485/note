#### 官网
https://www.deepin.org/index/zh

#### 安装教程
https://www.deepin.org/zh/installation/

#### 博通无线芯片bcm4352解决无线断开搜索不到信号
1.卸载
```
sudo apt autopurge bcmwl-kernel-source
sudo apt autoremove bcmwl-kernel-source
```
在https://packages.debian.org/search中搜索broadcom-sta

buster-backports版本中下载对应的三个软件包：

broadcom-sta-common、broadcom-sta-dkms、broadcom-sta-source进行安装

可能遇到依赖冲突
```
sudo aptitude install -f
```
选择某个解决方案后重新安装上面的软件