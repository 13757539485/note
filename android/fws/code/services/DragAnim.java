 private final Interpolator mInterpolator = new PathInterpolator(0.33f, 0.0f, 0.67f, 1.0f);

private SurfaceControl mAddLeash;
private GraphicBuffer mNoAccept;
private GraphicBuffer mAddIcon;
private ValueAnimator mShowAnimator;

private void visibleAddLeash(boolean isShow) {
    try (SurfaceControl.Transaction transaction =
                    mService.mTransactionFactory.get()) {
        if (mAddLeash != null) {
            if (isShow) {
                transaction.show(mAddLeash);
            } else {
                transaction.hide(mAddLeash);
            }
            transaction.apply();
        }
    }
}

//创建图标：不支持拖拽
private void createNoAcceptIcon() {
    if (mNoAccept == null) {
        BitmapFactory.Options options = new BitmapFactory.Options();
        options.inPreferredConfig = Bitmap.Config.HARDWARE;
        HardwareBuffer hardwareBuffer = BitmapFactory.decodeResource(mService.mContext.getResources(),
                R.drawable.presence_invisible, options).getHardwareBuffer();
        mNoAccept = GraphicBuffer.createFromHardwareBuffer(hardwareBuffer);
    }
}

//创建图标：支持拖拽
private void createAddIcon() {
    if (mAddIcon == null) {
        BitmapFactory.Options options = new BitmapFactory.Options();
        options.inPreferredConfig = Bitmap.Config.HARDWARE;
        HardwareBuffer hardwareBuffer = BitmapFactory.decodeResource(mService.mContext.getResources(),
                com.android.internal.R.drawable.ic_input_add, options).getHardwareBuffer();
        mAddIcon = GraphicBuffer.createFromHardwareBuffer(hardwareBuffer);
    }
}

//显示支持拖拽图标
private void showAddIcon() {
    createAddIcon();
    if (mAddIcon == null) {
        return;
    }
    createStatusLeash();
    try (SurfaceControl.Transaction transaction =
                    mService.mTransactionFactory.get()) {
        transaction.setBuffer(mAddLeash, mAddIcon);
        transaction.setPosition(mAddLeash,
                mSurfaceControl.getWidth() - mAddIcon.getWidth() / 2.0f, -mAddIcon.getHeight() / 2.0f);
        transaction.show(mAddLeash);
        transaction.apply();
    }
}

//显示不支持拖拽图标
private void showNoAcceptIcon() {
    createNoAcceptIcon();
    if (mNoAccept == null) {
        return;
    }
    createStatusLeash();
    try (SurfaceControl.Transaction transaction =
                    mService.mTransactionFactory.get()) {
        transaction.setBuffer(mAddLeash, mNoAccept);
        transaction.setPosition(mAddLeash,
                mSurfaceControl.getWidth() - mNoAccept.getWidth() / 2.0f, -mNoAccept.getHeight() / 2.0f);
        transaction.show(mAddLeash);
        transaction.apply();
    }
}

//创建视图用于显示是否接收拖拽的状态标志
private void createStatusLeash() {
    try (SurfaceControl.Transaction transaction =
                    mService.mTransactionFactory.get()) {
        if (mAddLeash != null) {
            return;
        }
        mAddLeash = new SurfaceControl.Builder()
                .setName("drag add layer")
                .setFormat(PixelFormat.TRANSLUCENT)
                .setParent(mSurfaceControl)
                .setCallsite("DragState.createShowAnimationLocked")
                .setBLASTLayer()
                .build();
        transaction.setColorSpace(mAddLeash, ColorSpace.get(ColorSpace.Named.SRGB)).apply();
    }
}

//拖拽显示动画
private void createShowAnimationLocked() {
    int width = mSurfaceControl.getWidth();
    int height = mSurfaceControl.getHeight();

    Matrix matrix = new Matrix();
    float[] matrixValues = new float[9];
    mShowAnimator = ValueAnimator.ofPropertyValuesHolder(
            PropertyValuesHolder.ofFloat(ANIMATED_PROPERTY_SCALE, 1.2f, 1.0f),
            PropertyValuesHolder.ofFloat(
                    ANIMATED_PROPERTY_ALPHA, 1, mOriginalAlpha)
    );
    mShowAnimator.addUpdateListener(animation -> {
        if (mSurfaceControl == null && animation.isRunning()) {
            animation.cancel();
            return;
        }
        try (SurfaceControl.Transaction transaction =
                        mService.mTransactionFactory.get()) {
            transaction.setAlpha(
                    mSurfaceControl,
                    (float) animation.getAnimatedValue(ANIMATED_PROPERTY_ALPHA));
            float tmpScale = (float) animation.getAnimatedValue(ANIMATED_PROPERTY_SCALE);
            float scaleCenterX = mCurrentX;
            float scaleCenterY = mCurrentY;
            matrix.setScale(tmpScale, tmpScale);
            matrix.postTranslate(scaleCenterX - (width * tmpScale / 2.0f),
                    scaleCenterY - (height * tmpScale / 2.0f));

            transaction.setMatrix(mSurfaceControl, matrix, matrixValues);
            transaction.apply();
        }
    });
    mShowAnimator.setDuration(200);
    mShowAnimator.setInterpolator(mInterpolator);
    mShowAnimator.addListener(new Animator.AnimatorListener() {
        @Override
        public void onAnimationStart(Animator animator) {}

        @Override
        public void onAnimationCancel(Animator animator) {
            mAnimationCompleted = true;
            mDragDropController.sendHandlerMessage(MSG_ANIMATION_END, null);
        }

        @Override
        public void onAnimationRepeat(Animator animator) {}

        @Override
        public void onAnimationEnd(Animator animation) {}
    });

    mService.mAnimationHandler.post(() -> mShowAnimator.start());
}

//拖拽跟手
void updateDragSurfaceLocked(boolean keepHandling, float x, float y) {
    if (mAnimator != null || (mShowAnimator != null && mShowAnimator.isRunning())) {
        return;
    }
    //...
    float width = mSurfaceControl.getWidth() / 2.0f;
    float height = mSurfaceControl.getHeight() / 2.0f;
    mTransaction.setPosition(mSurfaceControl, x - width,y - height).apply();
    //...
}
 
private int mDragTag = -1; //角标不显示
private String mCurrentPkg;
private boolean mLastResult;
//处理拖拽不是显示图标逻辑
public boolean notifyDropStatus(boolean oriResult, boolean isOtherWindow,
    IWindow window, DragEvent event, String packageName) {
    String mimeType = event.getClipDescription().getMimeType(0);
    if (event.getAction() == DragEvent.ACTION_DROP) {
        int shareActionSize = HfcDragTransitManager.getInstance().getShareActionSize(packageName, mimeType);
        return oriResult || shareActionSize == 1;
    }

    boolean needQuery = false;
    if (mCurrentPkg == null) {
        // 拽起第一次进入应用
        Log.e("DragTransit", "packageName first");
        mCurrentPkg = packageName;
        if (isOtherWindow) {
            Log.e("DragTransit", "packageName first other");
            needQuery = true;
        }
    } else {
        if (!packageName.equals(mCurrentPkg)) {
            // 应用切换
            Log.e("DragTransit", "packageName change");
            mCurrentPkg = packageName;
            needQuery = true;
        } else {
            // 应用内
            if (oriResult != mLastResult) {
                mLastResult = oriResult;
                Log.e("DragTransit", "result change");
                needQuery = true;
            }
        }
    }
    if (needQuery) {
        Log.e("DragTransit", "needQuery start");
        int dragTag = -1; // hide
        if (isOtherWindow) {
            Log.e("DragTransit", "needQuery other");
            if (oriResult) {
                Log.e("DragTransit", "needQuery ori");
                dragTag = 0;
            } else {
                int shareActionSize = CariadDragTransitManager.getInstance().getShareActionSize(packageName, mimeType);
                Log.e("DragTransit", "shareActionSize: " + shareActionSize);
                if (shareActionSize == 1) {
                    // +
                    dragTag = 0;
                } else {
                    if (shareActionSize > 1) {
                        // share
                        dragTag = 1;
                    } else {
                        // no accept
                        dragTag = 2;
                    }
                }
            }
        }
        Log.e("DragTransit", "mDragTag=" + mDragTag + ",dragTag=" + dragTag);
        if (mDragTag != dragTag) {
            mDragTag = dragTag;
            switch (mDragTag) {
                case 0:
                    showAddIcon();
                    Log.e("DragTransit", "notifyDropStatus show add");
                    break;
                case 1:
                    // share
//                        break;
                case 2:
                    showNoAcceptIcon();
                    Log.e("DragTransit", "notifyDropStatus show no accept");
                    break;
                case -1:
                    visibleAddLeash(false);
                    Log.e("DragTransit", "notifyDropStatus hide");
                    break;
            }
        }
    }
    return mDragTag == 0;
}

void closeLocked() {
    //...
    mCurrentPkg = null;
    mDragTag = -1;
    mLastResult = false;
}
