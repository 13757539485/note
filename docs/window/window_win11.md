### 1.右键菜单恢复win10

1.win+r打开运行窗口，输入regedit，按下回车键确认即可打开注册表

2.在路径中输入：HKEY_CURRENT_USER\SOFTWARE\CLASSES\CLSID

![win11_clsid](../img/win11_clsid.png)

3.右键点击CLSID项，点击新建一个项，命名为{86ca1aa0-34aa-4e8b-a509-50c905bae2a2}
![win11_clsid_create](../img/win11_clsid_create.png)

4.右键点击新建的项，然后再新建一个项，命名为InprocServer32
![win11_clsid_inner_create](../img/win11_clsid_inner_create.png)

5.重启资源管理器
![win11_reboot_res](../img/win11_reboot_res.png)