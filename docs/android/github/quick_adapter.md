<a id="BaseRecyclerViewAdapterHelper">BaseRecyclerViewAdapterHelper</a>：https://github.com/CymChad/BaseRecyclerViewAdapterHelper
```kotlin
implementation（"io.github.cymchad:BaseRecyclerViewAdapterHelper4:4.1.2")
```
### 基本使用(官方案例)
```kotlin
class TestAdapter : BaseQuickAdapter<Status, TestAdapter.VH>() {

    // 自定义ViewHolder类
    class VH(
        parent: ViewGroup,
        val binding: LayoutAnimationBinding = LayoutAnimationBinding.inflate(
            LayoutInflater.from(parent.context), parent, false
        ),
    ) : RecyclerView.ViewHolder(binding.root)

    override fun onCreateViewHolder(context: Context, parent: ViewGroup, viewType: Int): VH {
        // 返回一个 ViewHolder
        return VH(parent)
    }

    override fun onBindViewHolder(holder: VH, position: Int, item: Status?) {
        // 设置item数据
    }
}
```
注：添加数据集合内部是直接赋值的形式，因此是引用，如果需要保证初始数据，可new一个集合
```kotlin
rvAdapter.submitList(mutableListOf<Media>().also { it.addAll(imgUrls) })
```

### 上拉加载更多
```kotlin
class CustomLoadMoreAdapter(callback: OnTrailingListener) : TrailingLoadStateAdapter<CustomLoadMoreAdapter.CustomVH>() {

    init {
        setOnLoadMoreListener(callback)
    }

    override fun onCreateViewHolder(parent: ViewGroup, loadState: LoadState): CustomVH {
        // 创建你自己的 UI 布局
        val viewBinding =
            XxxBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return CustomVH(viewBinding).apply {
            viewBinding.loadMoreLoadFailView.setOnClickListener {
                // 失败重试点击事件
                invokeFailRetry()
            }
            viewBinding.loadMoreLoadCompleteView.setOnClickListener {
                // 加载更多，手动点击事件
                invokeLoadMore()
            }
        }
    }

    override fun onBindViewHolder(holder: CustomVH, loadState: LoadState) {
        // 根据加载状态，来自定义你的 UI 界面
        when (loadState) {
            is LoadState.NotLoading -> {
                if (loadState.endOfPaginationReached) {
                    holder.viewBinding.loadMoreLoadCompleteView.visibility = View.GONE
                    holder.viewBinding.loadMoreLoadingView.visibility = View.GONE
                    holder.viewBinding.loadMoreLoadFailView.visibility = View.GONE
                    holder.viewBinding.loadMoreLoadEndView.visibility = View.VISIBLE
                } else {
                    holder.viewBinding.loadMoreLoadCompleteView.visibility = View.VISIBLE
                    holder.viewBinding.loadMoreLoadingView.visibility = View.GONE
                    holder.viewBinding.loadMoreLoadFailView.visibility = View.GONE
                    holder.viewBinding.loadMoreLoadEndView.visibility = View.GONE
                }
            }

            is LoadState.Loading -> {
                holder.viewBinding.loadMoreLoadCompleteView.visibility = View.GONE
                holder.viewBinding.loadMoreLoadingView.visibility = View.VISIBLE
                holder.viewBinding.loadMoreLoadFailView.visibility = View.GONE
                holder.viewBinding.loadMoreLoadEndView.visibility = View.GONE
            }

            is LoadState.Error -> {
                holder.viewBinding.loadMoreLoadCompleteView.visibility = View.GONE
                holder.viewBinding.loadMoreLoadingView.visibility = View.GONE
                holder.viewBinding.loadMoreLoadFailView.visibility = View.VISIBLE
                holder.viewBinding.loadMoreLoadEndView.visibility = View.GONE
            }

            is LoadState.None -> {
                holder.viewBinding.loadMoreLoadCompleteView.visibility = View.GONE
                holder.viewBinding.loadMoreLoadingView.visibility = View.GONE
                holder.viewBinding.loadMoreLoadFailView.visibility = View.GONE
                holder.viewBinding.loadMoreLoadEndView.visibility = View.GONE
            }
        }
    }


    class CustomVH(val viewBinding: XxxBinding) : RecyclerView.ViewHolder(viewBinding.root)
}
```
activity/fragment中调用
```
private val loadMoreAdapter by lazy {
    CustomLoadMoreAdapter(object : TrailingLoadStateAdapter.OnTrailingListener {
        override fun onLoad() {
            // 执行加载更多的操作，通常都是网络请求
            loadMoreData()
        }

        override fun onFailRetry() {
            // 加载失败后，点击重试的操作，通常都是网络请求
            loadMoreData()
        }

        override fun isAllowLoading(): Boolean {
            // 是否允许触发“加载更多”，通常情况下，下拉刷新的时候不允许进行加载更多
            return !binding.refreshLayout.isRefreshing
        }
    })
}
private val helper by lazy {
    QuickAdapterHelper.Builder(xxAdapter)//实际数据adapter
        .setTrailingLoadStateAdapter(loadMoreAdapter).build()
}

binding.rv.adapter = helper.adapter
```
配置加载状态
```kotlin
//初始化
helper.trailingLoadState = LoadState.None
//没有数据加载
helper.trailingLoadState = LoadState.NotLoading(true)

//加载中
LoadState.Loading
//加载出错
LoadState.Error
```