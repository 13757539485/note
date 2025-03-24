### 设计模式六大原则
#### 1.单一职责原则
一个类应该只负责一项职责
#### 2.接口隔离原则
建立单一接口，不要建立臃肿庞大的接口。也就是说，接口尽量细化，同时接口中的方法尽量少
#### 3.里氏替换原则
子类可以扩展父类的功能，但是不能改变父类原有的功能(不能覆盖父类的非抽象方法)
#### 4.依赖倒置原则
模块间依赖通过抽象发生，实现类之间不发生直接依赖关系，如类中的成员属性应该抽象化(接口)
#### 5.迪米特法则
核心是尽量降低类之间的耦合
#### 6.开闭原则
强调对修改关闭，对扩展开放，遵循该原则能够使代码更加灵活、易于维护和扩展

### 23种设计模式
1. 创建型模式，共五种：⼯⼚⽅法模式、抽象⼯⼚模式、单例模式、建造者模式、原型模式

2. 结构型模式，共七种：适配器模式、装饰器模式、代理模式、外观模式、桥接模式、组合模式、享元模式

3. ⾏为型模式，共⼗⼀种：策略模式、模板⽅法模式、观察者模式、迭代⼦模式、责任链模式、命令模式、备忘录模式、状态模式、访问者模式、中介者模式、解释器模式

#### ⼯⼚⽅法模式

MediaPlayer的创建

#### 抽象⼯⼚模式


#### 单例模式

[单例模式](./design/design_single.md)

#### 建造者模式

AlertDialog，Notification

#### 原型模式

TemplateManager

#### 适配器模式

ListView的Adapter或者RecyclerView的Adapter

#### 装饰器模式

ContextThemeWrapper类及其派生类Activity和Service使用了装饰器模式，在Context的基础上添加了对主题的支持


#### 代理模式

WindowManagerGlobal

#### 外观模式

Context类及其子类ContextWrapper使用了外观模式，提供了一个统一的接口来访问系统的各种服务，如ActivityManager、InputManager等

#### ​桥接模式

View类及其子类（如Button、TextView）与绘制相关的类（如Canvas、HardwareLayer）之间使用了桥接模式，将视图的结构与其绘制实现分离

#### 组合模式

Android中的ViewGroup类及其子类（如LinearLayout、RelativeLayout）使用了组合模式，允许将多个视图组合成一个树形结构，统一管理

#### 享元模式

[享元模式](./design/design_enyuan.md)

#### 策略模式

[策略模式](./design/design_strategy.md)

#### 模板方法模式
AsyncTask(onPreExecute、doInBackground、onPostExecute)、Activity(生命周期)

#### 观察者模式

[观察者模式](./design/design_observer.md)

#### 迭代子模式

ndroid中的BaseAdapter类实现了Iterator接口，提供了遍历数据集的方法

#### <a id="chain">责任链模式</>
Android事件分发机制

优点

1. 降低对象间耦合度
2. 增强系统可扩展性，满足开闭原则
3. 增强对象指派责任的灵活性
4. 简化对象之间的连接，避免if else语句
5. 责任分担，每个类只需要处理自己的工作，符合单一职责原则

缺点

1. 不能保证事件一定被处理
2. 责任链太长时，系统性能受影响
3. 责任链建立需要合理处理才能保证，复杂性增加

#### 命令模式

Android中的Runnable接口及其在Handler中的使用可以看作命令模式的实现，将操作封装为对象，便于传递和执行。

#### ​备忘录模式

Android中的Activity类的onSaveInstanceState和onRestoreInstanceState方法使用了备忘录模式，允许在Activity生命周期变化时保存和恢复状态。

#### ​状态模式

Android中的MediaPlayer类使用了状态模式，根据当前播放状态（如初始化、准备、播放、暂停等）调用相应的方法。

#### ​访问者模式

Android中的LayoutInflater类及其在解析XML布局文件时的使用可以看作访问者模式的实现，允许对XML节点进行不同的处理。

#### ​中介者模式

Android中的EventBus库使用了中介者模式，通过一个中央事件总线来管理组件之间的通信，减少耦合。

#### ​解释器模式

Android中的PackageParser类使用了解释器模式，解析AndroidManifest.xml文件中的标签，将其转换为对应的Java对象

