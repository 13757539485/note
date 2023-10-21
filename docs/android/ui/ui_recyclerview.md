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
StaggeredGridLayoutManager.VERTICAL)//使用ListView形式
    val myAdapter = MyAdapter(mutableListOf("1", "2", "3"))
    adapter = myAdapter
    myAdapter.onItemClickListener = {
        
    }
    myAdapter.onItemLongClickListener = {
        
    }
    itemAnimator = DefaultItemAnimator()//其他动画需自定义
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
### 注意点

getChildCount()实际返回的是整数的可见的item数量

getChildAt(int index)获取是可见的第index个位置的item

所以当item只显示一项时以下获取的效果一致(案例抖音)
```kotlin
val itemView: View? = layoutmanager.findViewByPosition(0)
val itemView: View? = getChildAt(0)
//关闭动画
(itemAnimator as SimpleItemAnimator).supportsChangeAnimations = false

//GridLayoutManager设置分割线
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

## 四级缓存源码分析
onTouchEvent会触发缓存和复用流程
```java
@Override
public boolean onTouchEvent(MotionEvent e) {
    //...
    if (scrollByInternal(
            canScrollHorizontally ? dx : 0,
            canScrollVertically ? dy : 0,
            vtev)) {
        getParent().requestDisallowInterceptTouchEvent(true);
    }
    //...
}

boolean scrollByInternal(int x, int y, MotionEvent ev) {
    //...
    if (x != 0) {
        consumedX = mLayout.scrollHorizontallyBy(x, mRecycler, mState);
    }
    if (y != 0) {
        consumedY = mLayout.scrollVerticallyBy(y, mRecycler, mState);
    }
    //...
}
```
以LinearLayoutManager为例
```java
@Override
public int scrollVerticallyBy(int dy, RecyclerView.Recycler recycler,
        RecyclerView.State state) {
    if (mOrientation == HORIZONTAL) {
        return 0;
    }
    return scrollBy(dy, recycler, state);
}
```
scrollBy主要看fill方法
```java
recycleByLayoutState(recycler, layoutState);
layoutChunk(recycler, state, layoutState, layoutChunkResult);
```
### 缓存
recycleByLayoutState主要是缓存相关

缓存最终调用recycleChildren方法，接着调用recycleView
```java
public void removeAndRecycleViewAt(int index, Recycler recycler) {
    final View view = getChildAt(index);
    removeViewAt(index);
    recycler.recycleView(view);
}
```
recycleView主要看recycleViewHolderInternal方法

#### mCachedViews和RecycledViewPool相关
```java
final ArrayList<ViewHolder> mCachedViews = new ArrayList<ViewHolder>();
int mViewCacheMax = DEFAULT_CACHE_SIZE;
static final int DEFAULT_CACHE_SIZE = 2;
void recycleViewHolderInternal(ViewHolder holder) {
    //...
    if (xxx)) {
        int cachedViewSize = mCachedViews.size();
        if (cachedViewSize >= mViewCacheMax && cachedViewSize > 0) {
            recycleCachedViewAt(0); // 超过2个后会移除第0个
            cachedViewSize--;
        }
        int targetCacheIndex = cachedViewSize;
        //...
        mCachedViews.add(targetCacheIndex, holder);//添加到缓存中
        cached = true;
    }
    //...
    if (!cached) {
        addViewHolderToRecycledViewPool(holder, true);
        recycled = true;
    }
    //...
}
mCachedViews默认最大只有2个缓存大小，超过后会将第0个移除
```java
void recycleCachedViewAt(int cachedViewIndex) {
    ViewHolder viewHolder = mCachedViews.get(cachedViewIndex);
    addViewHolderToRecycledViewPool(viewHolder, true);
    mCachedViews.remove(cachedViewIndex);
}
```
缓存到缓存池中
```java
void addViewHolderToRecycledViewPool(ViewHolder holder, boolean dispatchRecycled) {
    //...
    getRecycledViewPool().putRecycledView(holder);
}
private static final int DEFAULT_MAX_SCRAP = 5;
int mMaxScrap = DEFAULT_MAX_SCRAP;
SparseArray<ScrapData> mScrap = new SparseArray<>();//key是itemViewType
public void putRecycledView(ViewHolder scrap) {
    final int viewType = scrap.getItemViewType();//根据不同tpye类型缓存ViewHolder
    final ArrayList scrapHeap = getScrapDataForType(viewType).mScrapHeap;
    if (mScrap.get(viewType).mMaxScrap <= scrapHeap.size()) {
        return;
    }
    //...
    scrap.resetInternal();//清空ViewHolder数据
    scrapHeap.add(scrap);
}
private ScrapData getScrapDataForType(int viewType) {
    ScrapData scrapData = mScrap.get(viewType);
    if (scrapData == null) {
        scrapData = new ScrapData();
        mScrap.put(viewType, scrapData);
    }
    return scrapData;
}
```
缓存池中每个itemViewType最多缓存5个，且数据会清空

#### mAttachedScrap和mChangedScrap相关
```java
onMeasure
dispatchLayoutStep1();//PreLayout 动画前布局 动画的准备工作，存储一下现有item的信息
dispatchLayoutStep2();//真正测量和布局

onLayout
dispatchLayoutStep1();
dispatchLayoutStep2();
dispatchLayoutStep3();//PostLayout 动画后布局 执行动画，清除一些数据
```
只会执行一次

dispatchLayoutStep2->onLayoutChildren->detachAndScrapAttachedViews
```java
public void detachAndScrapAttachedViews(Recycler recycler) {
    final int childCount = getChildCount();
    for (int i = childCount - 1; i >= 0; i--) {
        final View v = getChildAt(i);
        scrapOrRecycleView(recycler, i, v);
    }
}
private void scrapOrRecycleView(Recycler recycler, int index, View view) {
    final ViewHolder viewHolder = getChildViewHolderInt(view);
    //...
    if (viewHolder.isInvalid() && !viewHolder.isRemoved()
            && !mRecyclerView.mAdapter.hasStableIds()) {
        removeViewAt(index);
        recycler.recycleViewHolderInternal(viewHolder);
    } else {
        detachViewAt(index);
        recycler.scrapView(view);
        mRecyclerView.mViewInfoStore.onViewDetached(viewHolder);
    }
}
```
recycleViewHolderInternal就是上面的mCachedViews和RecycledViewPool缓存逻辑

scrapView就是mAttachedScrap和mChangedScrap缓存逻辑，不限制容量满足条件直接缓存
```java
void scrapView(View view) {
    final ViewHolder holder = getChildViewHolderInt(view);
    if (holder.hasAnyOfTheFlags(ViewHolder.FLAG_REMOVED | ViewHolder.FLAG_INVALID)
            || !holder.isUpdated() || canReuseUpdatedViewHolder(holder)) {
        //...
        holder.setScrapContainer(this, false);
        mAttachedScrap.add(holder);
    } else {
        if (mChangedScrap == null) {
            mChangedScrap = new ArrayList<ViewHolder>();
        }
        holder.setScrapContainer(this, true);
        mChangedScrap.add(holder);
    }
}
```
onLayoutChildren->fill走onTouchEvent分析的流程

### 总结
|缓存结构|容器类型|容量限制|缓存用途|是否回调createView|是否回调bindView
|--|--|--|--|--|--|
|mChangedScrap/<br>mAttachedScrap|ArrayList|无，一般为屏幕内总的可见列表项数|临时存放仍在当前屏幕可见、但被标记为「移除」或「重用」的列表项|否|否
|mCachedViews|ArrayList|默认为2|存放已被移出屏幕、但有可能很快重新进入屏幕的列表项|否|否
|mViewCacheExtension|开发者自己定义|无|提供额外的可由开发人员自由控制的缓存层级|否|否
|RecycledViewPool(mScrap)|SparseArray|每种itemType默认为5|按不同的itemType分别存放超出mCachedViews限制的、被移出屏幕的列表项|否|是

1. mChangedScrap 表示数据已经改变的ViewHolder列表，需要重新绑定数据（调用onBindViewHolder）
    - 开启了列表项动画(itemAnimator)，并且列表项动画的canReuseUpdatedViewHolder(ViewHolder viewHolder)方法返回false的前提下；
    - 调用了notifyItemChanged、notifyItemRangeChanged这一类方法，通知列表项数据发生变化；
2. mAttachedScrap 应对的则是剩下的绝大部分场景
    - 像notifyItemMoved、notifyItemRemoved这种列表项发生移动，但列表项数据本身没有发生变化的场景。
    - 关闭了列表项动画，或者列表项动画的canReuseUpdatedViewHolder方法返回true，即允许重用原先的ViewHolder对象的场景
3. 回调bindView通过mFlags决定，从RecycledViewPool取出的缓存会调用resetInternal方法使得mFlags变成0

### 复用
回到fill方法中layoutChunk方法主要是复用相关
```java
void layoutChunk(RecyclerView.Recycler recycler, RecyclerView.State state,LayoutState layoutState, LayoutChunkResult result) {
    View view = layoutState.next(recycler);
    //...
}
View next(RecyclerView.Recycler recycler) {
    //...
    final View view = recycler.getViewForPosition(mCurrentPosition);
    //...
    return view;
}
```
最终调到tryGetViewHolderForPositionByDeadline方法
```java
ViewHolder tryGetViewHolderForPositionByDeadline(int position,
                boolean dryRun, long deadlineNs) {
    ViewHolder holder = null;
    // 0) If there is a changed scrap, try to find from there
    if (mState.isPreLayout()) {
        holder = getChangedScrapViewForPosition(position);
        fromScrapOrHiddenOrCache = holder != null;
    }
    // 1) Find by position from scrap/hidden list/cache
    if (holder == null) {
        holder = getScrapOrHiddenOrCachedHolderForPosition(position, dryRun);
        if (holder != null) {
            if (!validateViewHolderForOffsetPosition(holder)) {
                if (!dryRun) {
                    //...
                    recycleViewHolderInternal(holder);
                }
                holder = null;
            } else {
                fromScrapOrHiddenOrCache = true;
            }
        }
    }
    if (holder == null) {
        final int offsetPosition = mAdapterHelper.findPositionOffset(position);
        //...

        final int type = mAdapter.getItemViewType(offsetPosition);
        // 2) Find from scrap/cache via stable ids, if exists
        if (mAdapter.hasStableIds()) {
            holder = getScrapOrCachedViewForId(mAdapter.getItemId(offsetPosition),type, dryRun);
            //...
        }
        if (holder == null && mViewCacheExtension != null) {
            final View view = mViewCacheExtension
                    .getViewForPositionAndType(this, position, type);
            if (view != null) {
                holder = getChildViewHolder(view);
                //...
            }
        }
        if (holder == null) { // fallback to pool
            //...
            holder = getRecycledViewPool().getRecycledView(type);
            //...
        }
        if (holder == null) {
            //...
            holder = mAdapter.createViewHolder(RecyclerView.this, type);
            //...
        }
    }
    boolean bound = false;
    if (xxx) {
    } else if (xxx)) {
        //...
        bound = tryBindViewHolderByDeadline(holder, offsetPosition, position, deadlineNs);
    }
    //...
}
```
#### 第一级缓存mChangedScrap
getChangedScrapViewForPosition(position)
```java
ArrayList<ViewHolder> mChangedScrap = null;
ViewHolder getChangedScrapViewForPosition(int position) {
    //...
    // find by position
    for (int i = 0; i < changedScrapSize; i++) {
        final ViewHolder holder = mChangedScrap.get(i);
        if (!holder.wasReturnedFromScrap() && holder.getLayoutPosition() == position) {
            //...
            return holder;
        }
    }
    // find by id
    if (mAdapter.hasStableIds()) {
        //...
        if (xxx) {
            final long id = mAdapter.getItemId(offsetPosition);
            for (int i = 0; i < changedScrapSize; i++) {
                final ViewHolder holder = mChangedScrap.get(i);
                if (xxx && holder.getItemId() == id) {
                    //...
                    return holder;
                }
            }
        }
    }
    return null;
}
```
通过position来获取缓存，满足条件还会通过id来获取(解决数据错乱，原因：重新从网络获取数据时同一个position数据变了，此缓存不会重新触发onBindViewHolder)

##### 开启通过id获取缓存
```kotlin
myAdapter.setHasStableIds(true)
//重写Adapter中的getItemId方法，指定不同的id
override fun getItemId(position: Int): Long {
    return super.getItemId(position)
}
```

#### 第二级缓存mAttachedScrap和mCachedViews
getScrapOrHiddenOrCachedHolderForPosition
```java
final ArrayList<ViewHolder> mAttachedScrap = new ArrayList<>();
final ArrayList<ViewHolder> mCachedViews = new ArrayList<ViewHolder>();
ViewHolder getScrapOrHiddenOrCachedHolderForPosition(int position, boolean dryRun) {
    final int scrapCount = mAttachedScrap.size();

    // Try first for an exact, non-invalid match from scrap.
    for (int i = 0; i < scrapCount; i++) {
        final ViewHolder holder = mAttachedScrap.get(i);
        if (xxx) {
            //...
            return holder;
        }
    }

    //....

    // Search in our first-level recycled view cache.
    final int cacheSize = mCachedViews.size();
    for (int i = 0; i < cacheSize; i++) {
        final ViewHolder holder = mCachedViews.get(i);
        //...
        if (xxx) {
            if (!dryRun) {
                mCachedViews.remove(i);
            }
            //...
            return holder;
        }
    }
    return null;
}
```
getScrapOrCachedViewForId也是从mAttachedScrap获取缓存
##### 修改cache缓存大小
```kotlin
setItemViewCacheSize(3)
```
#### 第三级缓存mViewCacheExtension
自定义缓存
```kotlin
setViewCacheExtension(object : RecyclerView.ViewCacheExtension(){
    override fun getViewForPositionAndType(
        recycler: RecyclerView.Recycler,
        position: Int,
        type: Int
    ): View? {
        return null
    }
})
```
#### 第四级缓存RecycledViewPool
getRecycledViewPool().getRecycledView(type)通过缓存池获取
```java
SparseArray<ScrapData> mScrap = new SparseArray<>()
public ViewHolder getRecycledView(int viewType) {
    final ScrapData scrapData = mScrap.get(viewType);
    if (scrapData != null && !scrapData.mScrapHeap.isEmpty()) {
        final ArrayList<ViewHolder> scrapHeap = scrapData.mScrapHeap;
        return scrapHeap.remove(scrapHeap.size() - 1);
    }
    return null;
}
```
##### 修改缓存池大小
```kotlin
rv.recycledViewPool.setMaxRecycledViews(0, 5)
```
##### 复用(共享)缓存池
```kotlin
rv.setRecycledViewPool(recycledViewPool)
```