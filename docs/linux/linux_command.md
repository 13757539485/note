#### 查看本地ip
```
ifconfig
```
#### 查看ubuntu版本信息
```
lsb_release -a
```
#### 文件夹去除小锁
```
sudo chmod -R 777 路径(文件夹或文件)
```
#### 显示隐藏文件
```
ctrl+H
```
#### 查看内核版本
```
uname -a
可以区分是x68,x86_64,arm64等
```
#### 卸载旧版本内核
```
sudo apt-get remove linux-image- 接着按两下tab键
```
卸载完后执行
```
sudo update-grub
```
#### 查看分区占用情况
```
df -h
```
#### 杀死进程

以文件管理器nautilus为例
方式一：
```
killall nautilus
```
方式二：
```
ps -A | grep nautilus得到pid
sudo kill pid
```
#### as未响应
```
pkill java
```
#### vi使用
```
插入 按insert
保存并退出 先按esc 再按 : 输入wq
查找：/内容 按n搜索下一个 N上一个
编辑：a
显示行号：set nu 不显示set nonu
```
#### 安装filezilla用于管理ftp
```
sudo apt-get install filezilla
```
#### 命令安装
1)apt-get

sudo apt-get install package 安装包  
sudo apt-get install package - - reinstall 重新安装包  
sudo apt-get -f install 修复安装"-f = ——fix-missing"  
sudo apt-get remove package 删除包  
sudo apt-get remove package - - purge 删除包，包括删除配置文件等  
sudo apt-get autoremove package  删除包及其依赖的软件包  
sudo apt-get update 更新源  
sudo apt-get upgrade 更新已安装的包  
sudo apt-get dist-upgrade 升级系统  
sudo apt-get dselect-upgrade 使用 dselect 升级  
sudo apt-get build-dep package 安装相关的编译环境  
sudo apt-get source package 下载该包的源代码  
sudo apt-get clean && sudo apt-get autoclean 清理无用的包  
sudo apt-get check 检查是否有损坏的依赖

2)dpkg

dpkg –l | grep package 查询deb包的详细信息，没有指定包则显示全部已安装包,其中-l等价于--list
dpkg -s package 查看已经安装的指定软件包的详细信息，其中-s等价于--status  
dpkg -L package 列出一个包安装的所有文件清单，其中-L等价于--listfiles  
dpkg -S file 查看系统中的某个文件属于哪个软件包  
dpkg -i 所有deb文件的安装,其中-i等价于--install  
dpkg -r 所有deb文件的卸载，其中-r等价于--remove  
dpkg -P 彻底的卸载，包括软件的配置文件  
dpkg -c 查询deb包文件中所包含的文件,其中-c等价于--contents  
dpkg -L 查看系统中安装包的的详细清单，同时执行 -c 
sudo dpkg -I iptux.deb#查看iptux.deb软件包的详细信息，包括软件名称、版本以及大小等（其中-I等价于--info）
注：dpkg命令无法自动解决依赖关系。如果安装的deb包存在依赖包，则应避免使用此命令，或者按照依赖关系顺序安装依赖包。

#### 更新相关
```
sudo apt-get update
sudo apt-get autoremove
sudo apt-get upgrade
```

#### 搜索命令

grep：
```
grep 内容 ./ -R
```
find:
```
find . -name "xxx"在当前目录及子目录下查询可以用or连接
find . -name "xxx" or -name "xxx"
find . -regex "xxx"使用正则表达式
find . -iregex "xxx"忽略大小写
find . -name "*" -type f | xargs grep "xxx"
```

#### 配置启动挂载

位置：/etc/fstab

查看uuid：
```
sudo blkid
```

#### 查看文件夹大小
```
du -h --max-depth=1 目录 --max-depth=1限制目录层数
```

#### 终端清屏
```
clear或reset
```
#### 查看pid

除了使用ps，还可以使用lsof

如无法删除.fuse_hiddenxxx
```
lsof .fuse_hiddenxxx
```
查看进程所有者的PID
```
kill -9 pid或者直接进任务管理器杀死进程
```

#### 计算文件夹大小
```
du -sh 文件夹
```

#### 测试ssd速度
```
sudo apt-get install hdparm
sudo hdparm -Tt /dev/sda
```
#### <a id="apt_update">apt update报错</a>
在ubuntu22.10上

E: The repository 'https://mirrors.tuna.tsinghua.edu.cn/ubuntu kinetic Release' does not have a Release file.

解决：

编译/etc/apt/sources.list

复制内容
```
# 默认注释了源码镜像以提高 apt update 速度，如有需要可自行取消注释
deb http://mirrors.ustc.edu.cn/ubuntu-old-releases/ubuntu kinetic main restricted universe multiverse
#deb-src https://mirrors.tuna.tsinghua.edu.cn/ubuntu/ kinetic main restricted universe multiverse
deb http://mirrors.ustc.edu.cn/ubuntu-old-releases/ubuntu kinetic-updates main restricted universe multiverse
#deb-src https://mirrors.tuna.tsinghua.edu.cn/ubuntu/ kinetic-updates main restricted universe multiverse
deb http://mirrors.ustc.edu.cn/ubuntu-old-releases/ubuntu kinetic-backports main restricted universe multiverse
#deb-src https://mirrors.tuna.tsinghua.edu.cn/ubuntu/ kinetic-backports main restricted universe multiverse

deb http://mirrors.ustc.edu.cn/ubuntu-old-releases/ubuntu kinetic-security main restricted universe multiverse
#deb-src http://security.ubuntu.com/ubuntu/ kinetic-security main restricted universe multiverse

# 预发布软件源，不建议启用
# deb https://mirrors.tuna.tsinghua.edu.cn/ubuntu/ kinetic-proposed main restricted universe multiverse
# deb-src https://mirrors.tuna.tsinghua.edu.cn/ubuntu/ kinetic-proposed main restricted universe mul
```

https://mirrors.ustc.edu.cn/help/ubuntu-old-releases.html

### rar解压
```shell
sudo apt-get install unrar

unrar x -p密码 文件
```