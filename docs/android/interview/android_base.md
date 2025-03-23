### Activity的启动模式有哪些以及应用场景
任务栈就是Task，使用adb shell dumpsys activity activities或者adb shell dumpsys window可查看，过滤Task display areas in top down Z order

**standard​**

每次启动Activity时，无论该Activity是否已经存在于任务栈中，都会在当前任务栈中创建一个新的Activity实例。Activity的默认启动模式

应用场景：

独立操作：当每次启动都需要一个新的Activity实例来处理独立的操作时，如打开一个新的详情页面

**​singleTop**

如果目标Activity已经位于任务栈的顶部，则不会创建新的实例，而是复用栈顶的实例，并调用其onNewIntent()方法。
如果目标Activity不在栈顶，则会创建新的实例并压入栈中

应用场景：

1. ​通知点击：当应用接收到通知，用户点击通知后打开某个Activity，如果该Activity已经在栈顶（如正在运行的聊天界面），则复用该实例，避免重复创建。
2. ​返回栈优化：在某些需要优化返回栈行为的场景，避免用户多次点击返回按钮时重复出现相同的Activity

**​singleTask​**

在整个任务栈中，如果存在目标Activity的实例，则复用该实例，并将其上面的所有Activity出栈（即清除其之上的所有Activity）。如果不存在，则创建新的实例并置于新的任务栈中。该模式下的Activity只会存在于一个任务栈中

应用场景：

1. ​主界面Activity：通常用于应用的主界面，确保无论从哪里启动，都只有一个主界面实例，并且清空之前的任务栈。
2. ​需要独立任务栈的Activity：如浏览器的主界面，确保只有一个实例，并且管理其任务栈。

可以通过配置taskAffinity来实现类似​singleInstance​单独任务栈效果，但​singleInstance模式下只有自身Activity，而​singleTask​模式再启动会和它放同一个任务栈中，如ActivityA启动ActivityB，由于ActivityA是在taskAffinity为com.xx中所以会新建一个任务栈，但ActivityB再启动ActivityC就会在同一个任务栈

Task
  -ActivityRecord(ActivityC:standard)
  -ActivityRecord(ActivityB:singleTask)

Task
  -ActivityRecord(ActivityA:standard, taskAffinity=com.xx)

**​singleInstance​**

该模式下的Activity始终单独存在于一个独立的任务栈中，不允许其他Activity与其共享任务栈。
无论从哪个任务栈启动该Activity，都会将其置于一个新的任务栈中，并且该任务栈中只有这一个Activity。当再次启动该Activity时，会复用已存在的实例，并将其任务栈带到前台。

应用场景：

1. ​需要独立运行的Activity：如全屏的视频播放器、电话拨号界面等，需要独占一个任务栈，避免被其他Activity干扰。
2. ​单例模式的Activity：确保系统中只有一个实例存在，适用于需要全局唯一访问点的Activity。

**singleInstancePerTask**

Android 12 引入的一种新的 Activity 启动模式，它在传统的 standard、singleTop、singleTask 和 singleInstance 基础上增加了更灵活的任务栈管理方式

与​singleInstance​区别是可以通过设置flag：FLAG_ACTIVITY_MULTIPLE_TASK 或 FLAG_ACTIVITY_NEW_DOCUMENT来达到多个任务栈分别创建实例

应用场景：

适用于需要独立任务栈的 Activity，例如：

文档编辑应用中的文档查看界面。

多窗口模式下需要独立运行的 Activity

例如，浏览器应用中每个标签页可以作为一个独立的任务栈运行

### 说下onSaveInstanceState()方法的作用以及何时会被调用

作用:保存用户界面状态数据等：例如，保存用户在文本框中输入的内容、选中的列表项、滚动位置等

- ​配置更改：如屏幕旋转、语言更改等，导致Activity被销毁并重新创建。
- ​系统资源不足：当系统内存不足时，可能会销毁后台的Activity以释放资源。
- ​用户操作：例如用户按下Home键，Activity进入后台，系统可能会销毁Activity以回收内存

android Honeycomb(11)之前是onPause之前调用

android p(28)之前onStop之前调用，否测onStop之后调用

onRestoreInstanceState是在onStart之后onPostCreate之前调用

### Activity的生命周期

启动

标准版：onCreate->onStart->onResume

onAttachBaseContext->onCreate->onStart->onPostCreate->onResume->onPostResume->onAttachedToWindow->onWindowFocusChanged

销毁

标准版：onPause->onStop->onDestroy

onPause->onWindowFocusChanged->onStop->onDestroy->onDetachedFromWindow

拓展

ActivityA启动另一个ActivityB会调用哪些方法？如果B是透明主题的或DialogActivity

ActivityA的onPause->ActivityB的onCreate->onStart->onResume->ActivityA的onStop
如果B是透明主题的又或则是个DialogActivity时：则不会回调A的onStop

onPasue侧重点是否可操作

onStop侧重点是否可见

ActivityB返回ActivityA会调用哪些方法

ActivityB的onPause->ActivityA的onRestart->onCreate->onStart->onResume->ActivityB的onStop->onDestroy

### Activity横竖屏切换的生命周期

onPause->onStop->onSaveInstanceState->onDestroy->onCreate->onStart->onRestoreInstanceState->onResume

### Fragment的生命周期

onAttach->onCreate->onCreateView->onViewCreated->onStart->onResume

onPause->onStop->onDestroyView->onDestroy->onDetach

结合Activity

onCreate(A)->onAttach(F)->onCreate(F)->onCreateView(F)->onViewCreated(F)->onStart(F)->onStart(A)->onResume(A)->onReume(F)

onPause(F)->onPause(A)->onStop(F)->onStop(A)->onDestroyView(F)->onDestroy(F)->onDetach(F)->onDestroy(A)

### 管理Fragment中的add、replace、show和hide的区别

- add()
​用途：将一个新的Fragment添加到容器中。

​行为：新的Fragment会被添加到容器中，但不会移除或销毁现有的Fragment。

​生命周期：新的Fragment会经历onAttach、onCreate、onCreateView、onViewCreated、onStart和onResume等生命周期方法。

​适用场景：当你希望在同一个容器中保留多个Fragment，并且可能需要频繁切换它们时，使用add方法可以提高性能，因为不需要每次都重新创建Fragment。

- replace()

​用途：用一个新的Fragment替换容器中的现有Fragment。

​行为：当前的Fragment会被移除，新的Fragment会被添加到容器中。

​生命周期：当前的Fragment会经历onPause、onStop、onDestroyView、onDestroy和onDetach等生命周期方法，新的Fragment会经历onAttach、onCreate、onCreateView、onViewCreated、onStart和onResume等生命周期方法。

​适用场景：当你希望完全替换当前的Fragment，并且不需要保留其状态时，使用replace方法。

- show()

​用途：显示一个已经添加到容器中的Fragment。

​行为：将指定的Fragment的视图设置为可见。

​生命周期：onHiddenChanged回调。

​适用场景：当你希望在多个Fragment之间切换，并且希望保留它们的状态时，使用show和hide方法可以提高性能。

- hide()

​用途：隐藏一个已经添加到容器中的Fragment。

​行为：将指定的Fragment的视图设置为不可见。

​生命周期：onHiddenChanged回调。

​适用场景：当你希望在多个Fragment之间切换，并且希望保留它们的状态时，使用show和hide方法可以提高性能

onHiddenChanged是在onStart之后onResume之前

### Fragment横竖屏切换的生命周期

onPause->onStop->onSaveInstanceState->onDestroyView->onDestroy->onDetach->onAttach->onCreate->onCreateView->onViewCreated->onViewStateRestored->onStart->onRestoreInstanceState->onResume

重建生命周期Fragment优先于Activity，如onAttach->onCreate(F)->onCreate(A)

### 如何处理重建导致Fragment重叠
1. 配置清单文件，如屏幕旋转android:configChanges="screenSize|orientation"

2. 创建Fragmnet使用tag配合add、show、hide方式
```
val bt = supportFragmentManager.beginTransaction()
val fragment = supportFragmentManager.findFragmentByTag("blankFragment")
if (fragment == null) {
    bt.add(R.id.fragment, blankFragment, "blankFragment")
        .show(blankFragment)
        .commitNowAllowingStateLoss()
} else {
    supportFragmentManager.beginTransaction()
        .show(blankFragment)
        .commitNowAllowingStateLoss()
}
```

### FragmentPagerAdapter与FragmentStatePagerAdapter的区别

二者都继承PagerAdapter，结合ViewPager使用

FragmentPagerAdapter

- 不会销毁 Fragment，仅调用 onPause() 和 onStop()，视图会被销毁但实例保留在 FragmentManager 中

- 适用于少量、静态的页面，例如一组固定的标签页。由于它保留所有 Fragment 实例，内存占用较高，但切换页面时性能较好

​FragmentStatePagerAdapte

- 会销毁不再显示的 Fragment，调用 onDestroyView() 和 onDestroy()，但通过 onSaveInstanceState() 保存状态，以便恢复

- 适用于大量、动态的页面，例如新闻列表或图片浏览。它会销毁不再需要的 Fragment，仅保留状态信息，从而节省内存，但切换页面时可能会有稍微的性能开销

可以使用ViewPager2代替ViewPager+FragmentStatePagerAdapter

### Service的生命周期以及启动销毁方式

**​startService()**

首次启动：onCreate() -> onStartCommand() 

多次启动：onStartCommand() 

**stopService()/stopSelf()**

 onDestroy()

**​bindService()**

onCreate() -> onBind()

**unbindService()**

onDestroy()

### onStartCommand返回值有啥作用

START_STICKY(默认)：终止服务，系统会尝试重新创建服务并调用onStartCommand，但不会重新传递最后一个Intent，而是传递一个空的Intent

适用场景：适用于需要持续运行但不依赖于特定Intent的服务，例如后台音乐播放器

START_REDELIVER_INTENT：终止服务，系统会尝试重新创建服务并调用onStartCommand，同时重新传递最后一个Intent

​适用场景：适用于需要确保每次启动命令都能被执行的服务，例如文件下载或数据同步

START_NOT_STICKY：终止服务，且没有新的启动命令（Intent）需要传递，系统不会自动重启该服务。

适用场景：适用于一次性任务或对实时性要求不高的任务

START_STICKY_COMPATIBILITY: START_STICKY的兼容版本

### Activity 与 Fragment 之间常见的几种通信方式

静态变量、全局应用类(单例)、数据库、​接口回调、​广播、ViewModel和LiveData

### Android的事件分发机制

见[事件分发](../android_ui.md#view_dispatch)

### Binder的原理

### Intent传递数据有大小限制吗

由于Intent传输数据底层是基于binder的，而binder数据传输大小限制约1M，具体看源码

/frameworks/native/libs/binder/ProcessState.cpp
```
#define BINDER_VM_SIZE ((1 * 1024 * 1024) - sysconf(_SC_PAGE_SIZE) * 2)
```

android页面大小为4k，所以最终数值为1M-8k

通过adb shell getconf PAGESIZE查看设备页面大小

### Serialzable和Parcelable的区别
|特性|Serializable|Parcelable|
|--|--|--|
|实现复杂度|简单，只需实现接口|复杂，需手动实现序列化和反序列化方法|
|性能|较低，基于反射，速度慢|高效，专为Android优化，速度快|
|内存消耗|较高，反射机制占用更多内存|较低，直接操作Parcel对象|
|使用场景|对象持久化、网络传输|Android组件间高效数据传递|
|代码维护|易于维护|维护成本较高，类结构变化需手动更新|
|跨平台能力|支持跨Java平台|仅限Android平台|
|安全性|反射可能带来安全隐患|更好地控制序列化过程，安全性较高|