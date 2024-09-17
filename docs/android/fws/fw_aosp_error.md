### Android源码下载常见问题
#### <a id="tar_error">使用镜像下载报错</a>
报错：
Repo command failed: RepoUnhandledExceptionError
	GitCommandError: 'reset --keep v2.44^0' on repo failed
stderr: error: Entry 'project.py' not uptodate. Cannot merge.
fatal: 不能重置索引文件至版本 'v2.44^0'。

解决：
```shell
cd .repo/repo/
git pull
```

报错：
error.GitError: manifests rev-list ('^1013d985f70641b2cc05943f57fab5824d9e2ff3', 'HEAD', '--'): fatal: bad revision 'HEAD'

解决：
```shell
cd .repo/manifests
git reset --hard 1013d985f70641b2cc05943f57fab5824d9e2ff3
git pull
```
报错：git config --global --add safe.directory /xxx/xxx

解决：
```shell
git config --global --add safe.directory "*"
```
文件权限被修改

解决：
```shell
repo forall -c git config --add core.filemode false
```

#### <a id="adb_error">adb和fastboot报错</a>
no permissions (missing udev rules? user is in the plugdev group)

解决：
```shell
lsusb
```
输出：Bus 001 Device 009: ID 18d1:4ee7 Google Inc. Nexus/Pixel Device (charging + debug)
```shell
cd /etc/udev/rules.d/
sudo vi 51-android.rules
添加内容：
SUBSYSTEM=="usb", ATTRS{idVendor}=="18d1", ATTRS{idProduct}=="4ee7",MODE="0666"
```
设置权限
```shell
sudo chmod a+x 51-android.rules
```
fastboot相同解决

Bus 001 Device 014: ID 18d1:4ee0 Google Inc. Nexus/Pixel Device (fastboot)
SUBSYSTEM=="usb", ATTRS{idVendor}=="18d1", ATTRS{idProduct}=="4ee0",MODE="0666"