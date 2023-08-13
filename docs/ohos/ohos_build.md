#### WSL2配置
[wsl2环境配置](../window/window_wsl2.md)

1.打开Microsoft store搜索ubuntu，推荐使用ubuntu-20.04，下载后如果出现乱码则安装update_x64.msi(wsl2中)

2.设置root密码
```
sudo passwd root
```

3.Ubuntu Shell环境修改为bash

执行sudo dpkg-reconfigure dash，选择No

4.切换python版本
```
sudo update-alternatives --install /usr/bin/python python /usr/bin/python3 150
```

5.安装pip3
```
sudo apt -y update
sudo apt upgrade
sudo apt install python3-pip
```

6.配置git相关
```
sudo apt install git-all
su
curl -s https://packagecloud.io/install/repositories/github/git-lfs/script.deb.sh | sudo bash
apt install git-lfs
git lfs install
exit
git config --global user.name "yuli"
git config --global user.email "1875287386@qq.com"
git config --global credential.helper store
```

7.配置repo
```
curl https://gitee.com/oschina/repo/raw/fork_flow/repo-py3 > /usr/local/bin/repo
chmod a+x /usr/local/bin/repo
pip3 install -i https://pypi.tuna.tsinghua.edu.cn/simple requests
```

8.配置交换swap内存
```
sudo fallocate -l 10G /swapfile
sudo chmod 600 /swapfile
sudo mkswap /swapfile
sudo swapon /swapfile
echo '/swapfile swap swap defaults 0 0' | sudo tee -a /etc/fstab
free -h：可查看
```

9.按照wsl迁移
#### 官网相关

源码文档： https://gitee.com/openharmony/docs

开发者文档： http://docs.openharmony.cn/pages/v4.0/zh-cn/OpenHarmony-Overview_zh.md/

#### 源码下载
注册gitee以及生成ssh
```
ssh-keygen -t ed25519 -C "ubuntu"
```
下载
```
repo init -u git@gitee.com:openharmony/manifest.git -b master --no-repo-verify
repo sync -c
repo forall -c 'git lfs pull'
```
#### 编译配置

配置环境：
```
./build/build_scripts/env_setup.sh
sudo apt install libxinerama-dev libxcursor-dev libxrandr-dev libxi-dev
```
预编译工具下载：
```
./build/prebuilts_download.sh
```
64位：
```
./build.sh --product-name rk3568 --target-cpu arm64 --ccache
```
32位：
```
./build.sh --product-name rk3568  --ccache
```

编译完成镜像路径：\out\rk3568\packages\phone\images\

#### 参考教程
https://blog.csdn.net/nanzhanfei/article/details/121951919

#### 其他使用网站
daily构建版本

http://ci.openharmony.cn/dailys/dailybuilds