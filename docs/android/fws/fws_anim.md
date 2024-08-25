### 添加系统圆角动画
一般系统动画apply的地方(转场动画除外)：

frameworks/base/services/core/java/com/android/server/wm/WindowAnimationSpec.java
```java
@Override
public void apply(Transaction t, SurfaceControl leash, long currentPlayTime) {
    //...
    if (mAnimation.hasRoundedCorners()) {
        t.setCornerRadius(leash, tmp.transformation.getRadius());
    }
} 
```

#### 圆角动画类
[RoundCornerAnimation](./code/fw/RoundCornerAnimation.java)

frameworks/base/core/java/android/view/animation/Transformation.java
```java
private float mRadius; 
    
public void clear() {
   //...
   mRadius = 0.0f;
}

public void set(Transformation t) {
   //...
   mRadius = t.getRadius();
   //...
}

public void compose(Transformation t) {
    //...
    mRadius += t.getRadius();
    //...
}  
public void postCompose(Transformation t) {
    //...
    mRadius += t.getRadius();
    //...
}  
     
/**
 * Sets the current Transform's radius
 * @hide
 */
public void setRadius(float radius) {
    mRadius = radius;
}

/**
 * Returns the current Transform's radius
 * @hide
 */
public float getRadius() {
    return mRadius;
}
```

#### 开启ProtoLog日志
wm logging enable-text XXX