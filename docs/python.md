# 安装
进入[官网](https://www.python.org/downloads/)下载安装即可

# 环境变量
## windows

1. 新建python_home，添加安装路径，如图

![python_1](img/python_1.png)

2. path中添加变量，如图

![python_2](img/python_2.png)

3. 查看python版本，检测环境是否正常

```cmd
python -V
```

4. 查看pip版本

```cmd
pip -V
```

## linux
python一般默认自带，生成一下软链接即可
```
sudo ln -s /usr/bin/python3.10 /usr/bin/python
```
- 其中/usr/bin/python3.10可通过which python3按Tab键查看

安装pip
```
sudo apt install pip
```