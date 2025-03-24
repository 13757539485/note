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

#### <a id="cache_rv_pool">mCachedViews和RecycledViewPool相关</a>
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
|mChangedScrap/<br>mAttachedScrap|ArrayList|无，一般为屏幕内总的可见列表项数|临时存放仍在当前屏幕可见、但被标记为「移除」或「重用」的列表项|否|是/否
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
#### <a id="mChangedScrap">第一级缓存mChangedScrap</a>
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
通过position来获取缓存，满足条件还会通过id来获取(解决数据错乱，原因：重新从网络获取数据时同一个position数据变了，此缓存会重新触发onBindViewHolder)

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
