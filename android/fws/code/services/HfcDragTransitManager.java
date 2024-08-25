package com.xxx.xxx;

import android.content.ClipData;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.graphics.Bitmap;
import android.net.Uri;
import android.os.Bundle;
import android.os.IBinder;
import android.os.MemoryFile;
import android.os.ParcelFileDescriptor;
import android.os.RemoteCallback;
import android.os.UserHandle;
import android.util.Log;

import java.io.ByteArrayOutputStream;
import java.io.FileDescriptor;
import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;

public class HfcDragTransitManager {

    private static final HfcDragTransitManager instance = new HfcDragTransitManager();
    private static final String TAG = "DragTransit";

    private BindListener mBindListener;
    private RemoteCallback mUriListener;

    private IHfcDrag mHfcDrag;

    private Context mContext;

    private final IHfcDragListener mDragListener = new IHfcDragListener.Stub() {
        @Override
        public void callBack(Uri uri) {
            Bundle bundle = new Bundle();
            bundle.putParcelable("drop_uri", uri);
            mUriListener.sendResult(bundle);
        }
    };

    private final ServiceConnection mConnect = new ServiceConnection() {
        @Override
        public void onServiceConnected(ComponentName name, IBinder service) {
            mHfcDrag = IHfcDrag.Stub.asInterface(service);
            if (mHfcDrag != null && mBindListener != null) {
                mBindListener.onBindBack(true, mHfcDrag);
            }
            Log.e(TAG, "onServiceConnected: " + mHfcDrag);
        }

        @Override
        public void onServiceDisconnected(ComponentName name) {
            mHfcDrag = null;
            if (mBindListener != null) {
                mBindListener.onBindBack(false, null);
            }
            Log.e(TAG, "onServiceDisconnected" );
        }
    };

    public HfcDragTransitManager() {
    }

    public static HfcDragTransitManager getInstance() {
        return instance;
    }

    public void bindShareService(Context context, BindListener listener) {
        mContext = context;
        mBindListener = listener;
        if (mHfcDrag != null) {
            Log.e(TAG, "already bind share service.");
            mBindListener.onBindBack(true, mHfcDrag);
            return;
        }
        if (context != null) {
            Intent intent = new Intent("com.cariad.m2.action.SHARE")
                    .setPackage("com.cariad.m2.car_link_launcher");
            context.bindServiceAsUser(intent,
                    mConnect,
                    Context.BIND_AUTO_CREATE | Context.BIND_FOREGROUND_SERVICE_WHILE_AWAKE,
                    UserHandle.SYSTEM);
        }
    }

    public void unBindShareService() {
        if (mHfcDrag != null && mContext != null) {
            mContext.unbindService(mConnect);
            mContext = null;
            mBindListener = null;
            mUriListener = null;
        }
    }

    public void saveBitmap(android.os.ParcelFileDescriptor pfd, RemoteCallback listener) {
        mUriListener = listener;
        Log.e(TAG, "saveBitmap: " + mHfcDrag);
        if (mHfcDrag != null) {
            try {
                mHfcDrag.saveBitmap(pfd, mDragListener);
            } catch (Exception e) {
                Log.e(TAG, "saveBitmap: " + e.getMessage());
            }
        }
    }

    public int getShareActionSize(String pkg, String type) {
        Log.e(TAG, "getShareActionSize: " + mHfcDrag + ",pkg=" + pkg + ",type=" + type);
        if (mHfcDrag != null) {
            try {
                return mHfcDrag.searchShareIntent(pkg, type);
            } catch (Exception e) {
                Log.e(TAG, "getShareActionSize: " + e.getMessage());
            }
        }
        return 0;
    }

    public void shareBarShowOrHide(String action, String targetPackage, ClipData data) {
        Log.e(TAG, "shareBarShowOrHide: " + mHfcDrag + ",action:" + action + ",pkg=" + targetPackage + ",data=" + data);
        if (mHfcDrag != null) {
            try {
                mHfcDrag.shareBarShowOrHide(action, targetPackage, data);
            } catch (Exception e) {
                Log.e(TAG, "shareBarShowOrHide: " + e.getMessage());
            }
        }
    }

    public interface BindListener {
        void onBindBack(boolean isBind, IHfcDrag iBinder);
    }
}
