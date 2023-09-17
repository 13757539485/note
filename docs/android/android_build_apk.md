## 多渠道打包

### 什么是多渠道包？
渠道包就是要在安装包中添加渠道信息，也就是channel，对应不同的渠道，例如：小米市场、ov市场、应用宝市场等
### 为什么要提供多渠道包？
在安装包中添加不同的标识，应用在请求网络的时候携带渠道信息，方便后台做运营统计

### 360打包
#### 1.原理
apk文件本质就是zip文件,利用zip文件“可以添加comment（摘要）”的数据结构特点，在文件的末尾写入任意数据，而不用重新解压zip文件，我们就可以将渠道信息写入摘要区
#### 2.方法
https://jiagu.360.cn/#/global/download
#### 3.优缺点
优点:
1、5M的apk，1秒种能打300个
2、在下载apk的同时，服务端可以写入一些信息，例如邀请码，分享信息等
缺点:
渠道信息也是很容易修改，虽然可以加密，只是提高了修改的门槛

### 友盟打包
#### 1.原理
一般来讲，这个渠道的标识会放在AndroidManifest.xml的Application的一个Metadata中。然后就可以在java中通过API获取对应的数据了。
#### 2.方法

第一种：友盟就提供了多渠道打包的方式，可用于渠道统计等。
现在Android的构建工具换成了gradle，通过gradle，简单配置后就可以实现自动打所有渠道包。
1.按照umeng的要求，manifest文件中需要有
```xml
<meta-data
 android:name="UMENG_CHANNEL"
 android:value="${UMENG_CHANNEL_VALUE}" />
```
这段配置，value那里就是wandoujia，360之类的渠道名称，但是我们在这里不会去写渠道名，写的是一个占位符，后面gradle编译的时候会动态的替换掉它。
2，在module（一般也就是app）的build.gradle的android{}中添加如下内容：
```groovy
productFlavors{
  wandoujia{}
  xiaomi{}
 }
 productFlavors.all { flavor ->
  flavor.manifestPlaceholders = [UMENG_CHANNEL_VALUE: name]
 }
```

重命名apk
```groovy
applicationVariants.all { variant ->    //批量修改Apk名字
        variant.outputs.all { output ->
            if (!variant.buildType.isDebuggable()) {
                def sourceFile = ".apk"
                def replaceFile = "-${variant.versionName}_${releaseTime()}.apk"
                outputFileName = output.outputFile.name.replace(sourceFile, replaceFile)
            }
        }
    }
def releaseTime() {
    return new Date().format("yyyy-MM-dd", TimeZone.getTimeZone("UTC"))
}
```

4.获取渠道
在代码中我们可以通过读取mate-data信息来获取渠道，然后添加到请求参数中，获取方法如下：
```java
private String getChannel() {
   try {
       PackageManager pm = getPackageManager();
       ApplicationInfo appInfo = pm.getApplicationInfo(getPackageName(), PackageManager.GET_META_DATA);
       return appInfo.metaData.getString("UMENG_CHANNEL");
   } catch (PackageManager.NameNotFoundException ignored) {
   }
   return "";
}
```
5.执行签名打包：
这时候你去app/build/outputs/apk中就能看到自动打好的渠道包了
### 3.优缺点
打包方式效率比较低下