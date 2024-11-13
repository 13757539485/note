// ICariadDrag.aidl
package com.cariad.m2;
import android.net.Uri;

interface ICariadDragListener {
    void callBack(in Uri uri);
}