### RenderThread
渲染线程，Android5.0之后将Open GL线程独立出来的线程

硬件加速：通过GPU渲染，Android4.0默认开启，

GPU：硬件，由GPU厂商按照Open GL规范实现的驱动

Open GL线程：处理硬件加速绘制的线程

### Android绘制流程
软件绘制

CPU主导，每个窗口关联一个Surface，lockCanvas方法获取一个Canvas，SurfaceFlinger，GraphicBuffer

硬件绘制

GPU主导

### 转场动画
https://blog.csdn.net/chennai1101/article/details/81984104

z轴修改问题：只能循环修改ViewGroup的clipChildren属性
```
 private fun clipAllViewGroup(v: View, clipChild: Boolean) {
    if (v.parent == null || v.parent !is ViewGroup) return
    (v.parent as ViewGroup).clipChildren = clipChild
    clipAllViewGroup(v.parent as ViewGroup, clipChild)
}
```
### 属性动画
[旋转动画](../github/rotation.md#anim_rotation)