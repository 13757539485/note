### mmkv
github: https://github.com/Tencent/MMKV/

写入快，小数据快，支持多进程，不会自动备份数据，初始化加载文件或大字符时可能会卡顿

诞生目的：解决高频主线程写数据

### datastore
诞生目的：代替SharedPreferences

支持多进程，会自动备份，大数据存储比mmkv快