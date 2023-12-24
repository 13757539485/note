## 多选案例
### 使用场景
一般使用线性布局(购物车)或者格子布局(相册)
### 实现步骤
#### 布局选择
```kotlin
private val linear by lazy { LinearLayoutManager(this) }
private val grid by lazy { GridLayoutManager(this, 3) }
private fun changeManager(flag: Int) {
    when (flag) {
        0 -> {
            getBinding().adRv.layoutManager = linear
        }

        1 -> {
            getBinding().adRv.layoutManager = grid
        }
}
```
#### 绑定adapter
```kotlin
getBinding().adRv.apply {
    itemAnimator = null
    adapter = helper.adapter
}
```
#### 上拉加载更多
(使用[BaseRecyclerViewAdapterHelper](../../android_github.md#BaseRecyclerViewAdapterHelper))
```kotlin
val helperBuilder = QuickAdapterHelper.Builder(rvAdapter)
    .setTrailingLoadStateAdapter(object : TrailingLoadStateAdapter.OnTrailingListener {
        override fun onLoad() {
            rvAdapter.addAll(mutableListOf<Media>().also { it.addAll(loadMoreUrls) })
            _helper?.trailingLoadState = LoadState.NotLoading(true)
        }

        override fun onFailRetry() {
        }

        override fun isAllowLoading(): Boolean {
            return !getBinding().smartRefresh.isRefreshing
        }
    })
_helper = helperBuilder.build()
val helper = _helper!!
helper.trailingLoadState = LoadState.NotLoading(false)
```

#### 下拉刷新
(使用[SmartRefreshLayout](../../android_github.md#smart-fresh))
```kotlin
getBinding().smartRefresh.setOnRefreshListener {
    rvAdapter.submitList(mutableListOf<Media>().also { it.addAll(imgUrls) })
    getBinding().smartRefresh.finishRefresh()
    helper.trailingLoadState = LoadState.NotLoading(false)
}
```

#### 适配器实现
```kotlin
var isSelectMode: Boolean = false//是否进入多选模式
private val cacheSelect = LinkedHashMap<String, Media>()// 缓存选中的数据

```
onBindViewHolder
```kotlin
if (isSelectMode) { // 复选框状态设置
    holder.binding.btnCheck.visibility = View.VISIBLE
    holder.binding.btnCheck.isSelected = cacheSelect.containsKey(media.url)
} else {
    holder.binding.btnCheck.visibility = View.GONE
}
```
item点击处理复选框状态
```kotlin
holder.itemView.clickNormal {
    if (isSelectMode) {
        if (cacheSelect.containsKey(media.url)) {
            cacheSelect.remove(media.url)
            notifyItemChanged(position)
        } else {
            cacheSelect[media.url] = media
            notifyItemChanged(position)
        }
    } else {
        // 跳转等其他处理
    }
}
```
进入/退出多选模式
```kotlin
fun enterSelectMode() {
    isSelectMode = !isSelectMode
    cacheSelect.clear()
    notifyItemRangeChanged(0, itemCount)
}
```
获取选中项
```kotlin
fun getSelectItem() = cacheSelect.values
```

### 瀑布流相关
#### 常见问题
界面刷新或者添加视图时出现位置交换、顶部空白等问题

#### 解决方案
对布局属性进行调整
```kotlin
private val stagger by lazy {
    StaggeredGridLayoutManager(
        3,
        StaggeredGridLayoutManager.VERTICAL
    ).apply {
        gapStrategy = StaggeredGridLayoutManager.GAP_HANDLING_NONE//防止一些场景的位置交换
        isItemPrefetchEnabled = false//预加载的视图是不可见的不利于处理复选框逻辑
    }
}
```
关闭RecyclerView屏幕外缓存，影响复选框显示隐藏处理
```kotlin
getBinding().adRv.setItemViewCacheSize(0)
```
进入/退出多选模式时不要调用notifyItemRangeChanged刷新，手动处理复选框逻辑
```kotlin
val manager = getBinding().adRv.layoutManager as StaggeredGridLayoutManager
val startPos = manager.findFirstVisibleItemPositions(null).apply {
    Arrays.sort(this)//会出现类似[3,1,2]表示最上面可见的视图下标为1，2，3，需要排一下顺序
}
val endPos = manager.findLastVisibleItemPositions(null).apply {
    Arrays.sort(this)//最下面可见视图下标
}
for (i in startPos[0]..endPos[endPos.size - 1]) {//遍历可见视图将复选框显示/隐藏处理
    manager.findViewByPosition(i)?.let { itemView ->
        itemView.findViewById<AppCompatImageView>(R.id.btnCheck)?.let {
            it.isVisible = rvAdapter.isSelectMode
            it.isSelected = false
        }
    }
}
```
其他不可见的复选框会在onBindViewHolder时处理，前提是onBindViewHolder会调用，因此上面需要关闭预加载和屏幕外缓存

点击事件修改，不能调用notifyItemChanged去刷新，可能会导致位置交换
```kotlin
holder.itemView.clickNormal {
    if (isSelectMode) {
        if (cacheSelect.containsKey(media.url)) {
            cacheSelect.remove(media.url)
            holder.binding.btnCheck.isSelected = false
        } else {
            cacheSelect[media.url] = media
            holder.binding.btnCheck.isSelected = true
        }
    } else {
        // 跳转等其他处理
    }
}
```

itemView或者ImageView需要固定宽高，网络图片可让后台告诉图片宽高，本地图片可以通过MediaStore的WIDTH/HEIGHT字段获取，或者其他场景(如拍照)通过bitmap获取
```kotlin
holder.binding.imgContent.apply {//ImageView
    //按照图片宽高比例换算ImageView高度
    val imageHeight = (media.height * imageWidth).div(media.width)

    layoutParams.apply {
        width = imageWidth
        height = imageHeight
    }
}.load(media.url)
```
图片加载使用[coil库](../../android_github.md#coil)

scrollToPosition会触发位置计算，在列表中间时往顶部添加数据无影响，往底部添加数据存在问题
```kotlin
rvAdapter.add()
getBinding().adRv.layoutManager?.scrollToPosition(0)
getBinding().adRv.layoutManager?.scrollToPosition(rvAdapter.itemCount - 1) //会导致顶部空白
getBinding().adRv.scrollBy(0,2300) // 正常 但需要计算y值
```