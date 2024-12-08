// ICariadDrag.aidl
package com.cariad.m2;
import android.content.ClipData;
import com.cariad.m2.ICariadDragListener;

interface ICariadDrag {
    int searchShareIntent(String targetPackage, String type);
    oneway void saveBitmap(in ParcelFileDescriptor pfd, in ICariadDragListener listener);
    void shareBarShowOrHide(String action, String targetPackage, in ClipData data);
}