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