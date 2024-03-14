## 系统拖拽API
startDragAndDrop(ClipData data, DragShadowBuilder shadowBuilder, Object myLocalState, int flags)

ClipData：用于存储传送的数据

DragShadowBuilder：拖动时展示的视图或阴影

myLocalState：这个参数可以用作Activity内部一种轻量级的数据传输机制。监听方通过DragEvent#getLocalState()方法来获取数据。它不能跨Activity，如果在其他Activity调用getLocalState()方法会返回null

flags：设置为0表示不设置flag

1. DRAG_FLAG_GLOBAL表示可以跨window拖拽，典型的是分屏状态下的拖拽
2. DRAG_FLAG_GLOBAL_PERSISTABLE_URI_PERMISSION
3. DRAG_FLAG_GLOBAL_PREFIX_URI_PERMISSION
4. DRAG_FLAG_GLOBAL_URI_READ
5. DRAG_FLAG_GLOBAL_URI_WRITE
6. DRAG_FLAG_OPAQUE
7. DRAG_FLAG_ACCESSIBILITY_ACTION

拖拽简单文本
```kotlin
val shadow = LayoutInflater.from(this).inflate(R.layout.xxx, null)

val builder = MyDragShadowBuilder(shadow)
view.setOnLongClickListener(object : OnLongClickListener {
    //长按事件触发拖拽
    override fun onLongClick(v: View?): Boolean {
        val data = ClipData.newPlainText("drag text", content)
        imageView.startDragAndDrop(
            data,
            builder, // 如果不是自定义阴影直接new View.DragShadowBuilder(view)
            null,
            0 or View.DRAG_FLAG_GLOBAL or View.DRAG_FLAG_OPAQUE
        )

        return true
    }
})

class MyDragShadowBuilder(private var mShadow: View) :
    View.DragShadowBuilder() {
    private val width: Int = 400
    private val height: Int = 400

    override fun onProvideShadowMetrics(outShadowSize: Point?, outShadowTouchPoint: Point?) {
        //拖动图像的宽和高
        outShadowSize?.set(width, height)

        //手指在拖动图像的位置 中点
        outShadowTouchPoint?.set(width / 2, height / 2)
    }

    override fun onDrawShadow(canvas: Canvas) {
        mShadow.measure(width, height)
        mShadow.layout(0, 0, width, height)
        mShadow.draw(canvas)
    }
}
```

拖拽处理通过setOnDragListener
```kotlin
binding.root.setOnDragListener { v, event ->
    if (event.action == DragEvent.ACTION_DROP) {
        event.clipData?.let { clipData ->
            if (clipData.description == null) return@let
            val mimeType: String = clipData.description.getMimeType(0)
            clipData.getItemAt(0)?.let {
                if (mimeType.startsWith("image/")) {
                    contentResolver.openInputStream(it.uri)?.use {input->
                        BitmapFactory.decodeStream(input)?.let { bitmap->
                            // 处理图片
                        }
                    }
                } else if (mimeType.startsWith("text/")) {
                    // 处理文本
                } else {
                    // 处理其他内容
                }
            }
        }
    }
    true
}
```

## 源码分析

## 实战案例
[案例定制](./fws_drag_case.md)

应用横屏
https://blog.csdn.net/u012932409/article/details/117379528