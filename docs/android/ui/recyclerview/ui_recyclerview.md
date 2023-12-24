## RecyclerView基本使用
### 定义adapter
1)先定义ViewHolder

xml布局：item.rv.xml
```kotlin
class MyViewHolder(val binding: ItemRvBinding) : RecyclerView.ViewHolder(binding.root)
```
2)再实现adapter
```kotlin
class MyAdapter(val list: MutableList<String>): RecyclerView.Adapter<MyViewHolder>() {
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): MyViewHolder = MyViewHolder(
        ItemRvBinding.inflate(LayoutInflater.from(parent.context), parent, false)

    override fun getItemCount(): Int = list.size

    override fun onBindViewHolder(holder: MyViewHolder, position: Int) {
        holder.binding.itemTv.text = list[position]
    }
}
```
3)添加监听
```kotlin
var onItemClickListener: ((pos: Int) -> Unit)? = null
var onItemLongClickListener: ((pos: Int) -> Unit)? = null
holder.itemView.setOnClickListener {
    onItemClickListener?.invoke(position)
}
holder.itemView.setOnLongClickListener {
    onItemLongClickListener?.invoke(position)
    true
}
```
### 基本使用
```kotlin
getBinding().rv.apply {
    layoutManager =
        LinearLayoutManager(this@RVActivity, LinearLayoutManager.VERTICAL, false)//使用ListView形式
    layoutManager =
        GridLayoutManager(this@RVActivity, 2,
GridLayoutManager.VERTICAL, false)////使用GridView形式
    layoutManager =
        StaggeredGridLayoutManager(3,
StaggeredGridLayoutManager.VERTICAL)//瀑布流
    val myAdapter = MyAdapter(mutableListOf("1", "2", "3"))
    adapter = myAdapter
    myAdapter.onItemClickListener = {
        
    }
    myAdapter.onItemLongClickListener = {
        
    }
    itemAnimator = DefaultItemAnimator() //其他动画需自定义

    (itemAnimator as SimpleItemAnimator).supportsChangeAnimations = false //关闭动画

    //给Item添加分割线
    addItemDecoration(DividerItemDecoration(this@RVActivity, DividerItemDecoration.VERTICAL))))//垂直方向
    addItemDecoration(DividerItemDecoration(this@RVActivity, DividerItemDecoration.HORIZONTAL))//水平方向
    scrollToPosition(0)//滚动到第0个
}
```
### 修改分割线样式
```xml
<item name="android:listDivider">@drawable/bg_recyclerview_divider</item>

<?xml version="1.0" encoding="utf-8"?>
<shape xmlns:android="http://schemas.android.com/apk/res/android"
       android:shape="rectangle">
 
    <gradient
        android:centerColor="#ff00ff00"
        android:endColor="#ff0000ff"
        android:startColor="#ffff0000"
        android:type="linear"/>
 
    <size
        android:width="10dp"
        android:height="10dp"/>
 
</shape>
```
### 自定义分割线
```kotlin
class MyDecoration: RecyclerView.ItemDecoration() {
    override fun getItemOffsets(
        outRect: Rect,
        view: View,
        parent: RecyclerView,
        state: RecyclerView.State
    ) {
        super.getItemOffsets(outRect, view, parent, state)
    }

    override fun onDraw(c: Canvas, parent: RecyclerView, state: RecyclerView.State) {
        super.onDraw(c, parent, state)
    }

    override fun onDrawOver(c: Canvas, parent: RecyclerView, state: RecyclerView.State) {
        super.onDrawOver(c, parent, state)
    }
}
```
GridLayoutManager设置分割线
```kotlin
class GridSpacingItemDecoration(//列数
    private val column: Int, private val leftSpace: Int, //左间距
    private val topSpace: Int, //顶部间距
    private val rightSpace: Int, //右间距
    private val bottomSpace: Int, //底部间距
    private val centerLeftSpace: Int, //item之间左间距
    private val centerTopSpace: Int //item之间顶部间距
) : RecyclerView.ItemDecoration() {

    fun getItemOffsets(
        outRect: Rect, view: View?,
        parent: RecyclerView, state: RecyclerView.State?
    ) {
        val pos = parent.getChildAdapterPosition(view!!)
        if (pos % column == 0) {
            outRect.left = leftSpace
        } else {
            outRect.left = centerLeftSpace
        }
        if (pos < column) {
            outRect.top = topSpace
        } else {
            outRect.top = centerTopSpace
        }
        if (pos % column == column - 1) {
            outRect.right = rightSpace
        } else {
            outRect.right = 0
        }
        outRect.bottom = bottomSpace
    }
}
```
#### 分割线绘制源码分析
```java
@Override
public void draw(Canvas c) {
    super.draw(c);

    final int count = mItemDecorations.size();
    for (int i = 0; i < count; i++) {
        mItemDecorations.get(i).onDrawOver(c, this, mState);
    }
    //...
}

@Override
public void onDraw(Canvas c) {
    super.onDraw(c);

    final int count = mItemDecorations.size();
    for (int i = 0; i < count; i++) {
        mItemDecorations.get(i).onDraw(c, this, mState);
    }
}
```
draw会调用父类View的draw，接着调用onDraw方法，即调用到分割线的onDraw，最后调用onDrawOver方法，所以onDrawOver可以用来绘制覆盖场景使用，列如吸顶

#### 分割线事件问题
分割线是无法触发任何事件，需要手动添加手势解决
```kotlin
val gestureDetector =
    GestureDetector(parent.context, object : GestureDetector.SimpleOnGestureListener() {
        override fun onSingleTapUp(e: MotionEvent): Boolean {
            val pointX = e.x
            val pointY = e.y
            //判断边距再处理事件
            return super.onSingleTapUp(e)
        }
    })
parent.addOnItemTouchListener(object : RecyclerView.OnItemTouchListener {
    override fun onInterceptTouchEvent(rv: RecyclerView, e: MotionEvent): Boolean =
        gestureDetector.onTouchEvent(e)

    override fun onTouchEvent(rv: RecyclerView, e: MotionEvent) {
        gestureDetector.onTouchEvent(e)
    }

    override fun onRequestDisallowInterceptTouchEvent(disallowIntercept: Boolean) {
        
    }
})
```
### 实用方法

#### getChildCount()
实际返回的是整数的可见的item数量

#### getChildAt(int index)
获取是可见的第index个位置的item

所以当item只显示一项时以下获取的效果一致(案例抖音)
```kotlin
val itemView: View? = layoutmanager.findViewByPosition(0)
val itemView: View? = getChildAt(0)
```
#### 关闭动画
主要是用来解决刷新出现闪烁问题
```kotlin
(getBinding().rv.itemAnimator as SimpleItemAnimator).supportsChangeAnimations = false
//或者
getBinding().rv.itemAnimator = null
```

#### setHastFixedSize
```kotlin
rv.setHastFixedSize(true)
```
item改变时不会影响rv的高度时使用

#### setHasStableIds
```kotlin
adapter.setHasStableIds(true)
```
并需要重写getItemId(int position)方法，固定id可以减少刷新，可以解决数据错乱见源码分析[一级缓存复用机制](./ui_source_code.md#mChangedScrap)

#### 修改缓存池大小
默认是5个，见源码分析[RecyclerViewPool缓存](./ui_source_code.md#cache_rv_pool)的DEFAULT_MAX_SCRAP值
```kotlin
rv.recycledViewPool.setMaxRecycledViews(0, 5)
```
#### 复用(共享)缓存池
```kotlin
rv.setRecycledViewPool(recycledViewPool)
```

#### 修改cache缓存大小
默认是2个，见源码分析[Cache缓存](./ui_source_code.md#cache_rv_pool)的DEFAULT_CACHE_SIZE值
```kotlin
rv.setItemViewCacheSize(3)
```