### <a id="glide">Glide</a>
官方网址：https://github.com/bumptech/glide

Glide.with().load().into()

with：空白Fragment管理生命周期(ui线程才会创建、如果是application也不创建)，同步到RequestManager方便管理类似ImageViewTarget等类

load：构建出RequestBuilder对象

into：运行队列 等待队列 活动缓存 内存缓存 网络模型

源码分析：

https://blog.csdn.net/u013347784/article/details/125728996

默认是网络请求是使用HttpURLConnection

活动缓存、内存缓存、磁盘缓存

### 圆形
```kotlin
apply(RequestOptions.circleCropTransform())
```
### 自定义加载
[见gif加载案例](./gif.md)