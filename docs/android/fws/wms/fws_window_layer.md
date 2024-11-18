### 窗口层级数
0-36共37层

https://juejin.cn/post/7301955975338131495

https://blog.csdn.net/q1165328963/article/details/127746382

https://blog.csdn.net/CJohn1994/article/details/132794066

https://blog.csdn.net/learnframework/article/details/129132667

### 命令分析层级
```shell
adb shell dumpsys activity containers > wms.txt
```
### 源码分析
#### 添加主要层级
frameworks/base/services/core/java/com/android/server/wm/DisplayContent.java
```java
DisplayContent(Display display, RootWindowContainer root,
            @NonNull DeviceStateController deviceStateController) {
    //...
    configureSurfaces(pendingTransaction);
    //...            
}

private void configureSurfaces(Transaction transaction) {
    //...  
    mDisplayAreaPolicy = mWmService.getDisplayAreaPolicyProvider().instantiate(
                    mWmService, this /* content */, this /* root */,
                    mImeWindowsContainer);
    //... 
}
```
frameworks/base/services/core/java/com/android/server/wm/DisplayAreaPolicy.java
```java
public DisplayAreaPolicy instantiate(WindowManagerService wmService,
                DisplayContent content, RootDisplayArea root,
                DisplayArea.Tokens imeContainer) {
    //... 
    configureTrustedHierarchyBuilder(rootHierarchy, wmService, content);
    //... 
}
private void configureTrustedHierarchyBuilder(HierarchyBuilder rootHierarchy,WindowManagerService wmService, DisplayContent content) {
rootHierarchy.addFeature(new Feature.Builder(wmService.mPolicy, "WindowedMagnification",
    FEATURE_WINDOWED_MAGNIFICATION)
    .upTo(TYPE_ACCESSIBILITY_MAGNIFICATION_OVERLAY)
    .except(TYPE_ACCESSIBILITY_MAGNIFICATION_OVERLAY)
    .setNewDisplayAreaSupplier(DisplayArea.Dimmable::new)
    .build());
if (content.isDefaultDisplay) {
rootHierarchy.addFeature(new Feature.Builder(wmService.mPolicy, "HideDisplayCutout",
    FEATURE_HIDE_DISPLAY_CUTOUT)
    .all()
    .except(TYPE_NAVIGATION_BAR, TYPE_NAVIGATION_BAR_PANEL, TYPE_STATUS_BAR,
            TYPE_NOTIFICATION_SHADE)
    .build())
    .addFeature(new Feature.Builder(wmService.mPolicy, "OneHanded",FEATURE_ONE_HANDED)
    .all()
    .except(TYPE_NAVIGATION_BAR, TYPE_NAVIGATION_BAR_PANEL,TYPE_SECURE_SYSTEM_OVERLAY)
    .build());
    }
rootHierarchy
.addFeature(new Feature.Builder(wmService.mPolicy, "FullscreenMagnification",
FEATURE_FULLSCREEN_MAGNIFICATION)
    .all()
    .except(TYPE_ACCESSIBILITY_MAGNIFICATION_OVERLAY, TYPE_INPUT_METHOD,
            TYPE_INPUT_METHOD_DIALOG, TYPE_MAGNIFICATION_OVERLAY,
            TYPE_NAVIGATION_BAR, TYPE_NAVIGATION_BAR_PANEL)
    .build())
.addFeature(new Feature.Builder(wmService.mPolicy, "ImePlaceholder",
    FEATURE_IME_PLACEHOLDER)
    .and(TYPE_INPUT_METHOD, TYPE_INPUT_METHOD_DIALOG)
    .build());
    }
}
```
upTo：用来设置图层层级

except：用来排除层级

如"WindowedMagnification", 窗口类型FEATURE_WINDOWED_MAGNIFICATION

upTo(TYPE_ACCESSIBILITY_MAGNIFICATION_OVERLAY)，TYPE_ACCESSIBILITY_MAGNIFICATION_OVERLAY对应层级是32

对应查询源码位置：

frameworks/base/services/core/java/com/android/server/policy/WindowManagerPolicy.java

即0-32

except(TYPE_ACCESSIBILITY_MAGNIFICATION_OVERLAY),即排除32，则为0-31

build的时候默认会排除36
```java
private boolean mExcludeRoundedCorner = true;
Feature build() {
    if (mExcludeRoundedCorner) {
        mLayers[mPolicy.getMaxWindowLayer()] = false;
    }
    //...
}
```
同理其他图层

"HideDisplayCutout": all()即0-36

except(TYPE_NAVIGATION_BAR(24), TYPE_NAVIGATION_BAR_PANEL(25), TYPE_STATUS_BAR(15),TYPE_NOTIFICATION_SHADE(17))

0-14,16,18-23,26-35(build排除36)

"OneHanded": all()即0-36

except(TYPE_NAVIGATION_BAR(24), TYPE_NAVIGATION_BAR_PANEL(25),TYPE_SECURE_SYSTEM_OVERLAY(33))

0-23,26-32,34-35(build排除36)

"FullscreenMagnification": all()即0-36

except(TYPE_ACCESSIBILITY_MAGNIFICATION_OVERLAY(32), TYPE_INPUT_METHOD(13),TYPE_INPUT_METHOD_DIALOG(14), TYPE_MAGNIFICATION_OVERLAY(28),TYPE_NAVIGATION_BAR(24), TYPE_NAVIGATION_BAR_PANEL(25))

0-12,15-23,26-27,29-31,33-35(build排除36)

"ImePlaceholder": 0

and(TYPE_INPUT_METHOD(13), TYPE_INPUT_METHOD_DIALOG(14))

13-14

#### 开始构建层级
```java
public DisplayAreaPolicy instantiate(WindowManagerService wmService,
                DisplayContent content, RootDisplayArea root,
                DisplayArea.Tokens imeContainer) {
    //... 
    return new DisplayAreaPolicyBuilder().setRootHierarchy(rootHierarchy).build(wmService);
}
Result build(WindowManagerService wmService) {
    //... 
    mRootHierarchyBuilder.build(mDisplayAreaGroupHierarchyBuilders);
    //... 
}

private void build(@Nullable List<HierarchyBuilder> displayAreaGroupHierarchyBuilders) {
    //... 
    //根据算法建立数结构
    final int size = mFeatures.size();
    for (int i = 0; i < size; i++) {
        if (featureArea == null || featureArea.mParent != areaForLayer[layer]) {
            featureArea = new PendingArea(feature, layer, areaForLayer[layer]);
            areaForLayer[layer].mChildren.add(featureArea);
        }
        areaForLayer[layer] = featureArea;
        } else {
            featureArea = null;
        }
    }
    //... 
}
```
WindowedMagnification: 0-31，直接是在Display下挂载

HideDisplayCutout：0-14,16,18-23,26-35

![HideDisplayCutout](./img/HideDisplayCutout.png)

- 其中32:35是由于parent只能到31所以需要在Display下挂32:35

OneHanded：0-23,26-32,34-35

![OneHanded](./img/OneHanded.png)

FullscreenMagnification：0-12,15-23,26-27,29-31,33-35

![FullscreenMagnification](./img/FullscreenMagnification.png)

ImePlaceholder：13-14

![ImePlaceholder](./img/ImePlaceholder.png)

图层显示总结

Feature名称|层级|窗口类型|
:-|:-:|:-:|
WindowedMagnification|0-31|FEATURE_WINDOWED_MAGNIFICATION
HideDisplayCutout|0-14,16-16,18-23,26-31,32-35|FEATURE_HIDE_DISPLAY_CUTOUT
OneHanded|0-14,15-15,16-16,17-17,18-23,26-31,32-32,34-35|FEATURE_ONE_HANDED
FullscreenMagnification|0-12,15-15,16-16,17-17,18-23,26-27,29-31,33-33,34-35|FEATURE_FULLSCREEN_MAGNIFICATION
ImePlaceholder|13-14|FEATURE_IME_PLACEHOLDER

#### 构建Leaf层级
```java
//... 
PendingArea leafArea = null;
int leafType = LEAF_TYPE_TOKENS;
for (int layer = 0; layer < maxWindowLayerCount; layer++) {
    int type = typeOfLayer(policy, layer);
    if (leafArea == null || leafArea.mParent != areaForLayer[layer]
            || type != leafType) {
        leafArea = new PendingArea(null /* feature */, layer, areaForLayer[layer]);
        areaForLayer[layer].mChildren.add(leafArea);
        leafType = type;
        if (leafType == LEAF_TYPE_TASK_CONTAINERS) {
            addTaskDisplayAreasToApplicationLayer(areaForLayer[layer]);
            addDisplayAreaGroupsToApplicationLayer(areaForLayer[layer],
                    displayAreaGroupHierarchyBuilders);
            leafArea.mSkipTokens = true;
        } else if (leafType == LEAF_TYPE_IME_CONTAINERS) {
            leafArea.mExisting = mImeContainer;
            leafArea.mSkipTokens = true;
        }
    }
    leafArea.mMaxLayer = layer;
}
root.computeMaxLayer();
root.instantiateChildren(mRoot, displayAreaForLayer, 0, featureAreas);
mRoot.onHierarchyBuilt(mFeatures, displayAreaForLayer, featureAreas);
```
其中对两种类型进行特殊处理，在typeOfLayer中
```java
private static int typeOfLayer(WindowManagerPolicy policy, int layer) {
    if (layer == APPLICATION_LAYER) {
        return LEAF_TYPE_TASK_CONTAINERS;
    } else if (layer == policy.getWindowLayerFromTypeLw(TYPE_INPUT_METHOD)
            || layer == policy.getWindowLayerFromTypeLw(TYPE_INPUT_METHOD_DIALOG)) {
        return LEAF_TYPE_IME_CONTAINERS;
    } else {
        return LEAF_TYPE_TOKENS;
    }
}
```
APPLICATION_LAYER为2, TYPE_INPUT_METHOD为13, TYPE_INPUT_METHOD_DIALOG为14，其他都用PendingArea处理，这两种分别使用APPLICATION_LAYER是
```java
leafArea.mExisting = mTaskDisplayAreas.get(i);
leafArea.mSkipTokens = true;
```
INPUT是
```java
leafArea.mExisting = mImeContainer;
leafArea.mSkipTokens = true;
```
PendingArea在instantiateChildren中转成DisplayArea
```java
void instantiateChildren(DisplayArea<DisplayArea> parent, DisplayArea.Tokens[] areaForLayer,
        int level, Map<Feature, List<DisplayArea<WindowContainer>>> areas) {
    //... 
    for (int i = 0; i < mChildren.size(); i++) {
        final PendingArea child = mChildren.get(i);
        final DisplayArea area = child.createArea(parent, areaForLayer);
        if (area == null) {
            continue;
        }
        //... 
    }
}
private DisplayArea createArea(DisplayArea<DisplayArea> parent,
        DisplayArea.Tokens[] areaForLayer) {
    //... 
    if (mFeature == null) {
        final DisplayArea.Tokens leaf = new DisplayArea.Tokens(parent.mWmService, type, "Leaf:" + mMinLayer + ":" + mMaxLayer);//Leaf名称
        fillAreaForLayers(leaf, areaForLayer);
        return leaf;
    } else {
        return mFeature.mNewDisplayAreaSupplier.create(parent.mWmService, type, mFeature.mName + ":" + mMinLayer + ":" + mMaxLayer, mFeature.mId);
    }
}
```
除了APPLICATION和INPUT都是Leaf:x:x

APPLICATION名字为DefaultTaskDisplayArea

INPUT名字为ImeContainer

相关代码如下
```java
mImeWindowsContainer = ImeContainer(WindowManagerService wms) {
    super(wms, Type.ABOVE_TASKS, "ImeContainer", FEATURE_IME);
}
//mImeWindowsContainer传递给imeContainer
final TaskDisplayArea defaultTaskDisplayArea = new TaskDisplayArea(content, wmService, "DefaultTaskDisplayArea", FEATURE_DEFAULT_TASK_CONTAINER);
tdaList.add(defaultTaskDisplayArea);
rootHierarchy.setImeContainer(imeContainer).setTaskDisplayAreas(tdaList);
```
![Leaf](./img/Leaf.png)

#### 构建WindowToken/Task/ActivityRecord/WindowState

https://juejin.cn/post/7229587772217491511

![wms_layer](./img/wms_layer.drawio.png)

### SurfaceFlinger
命令

```shell
adb shell dumpsys SurfaceFlinger > surface.txt
```
### winscope