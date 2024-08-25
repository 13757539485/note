package com.hfc.localsocket;

import android.net.LocalSocket;

public interface MsgListener {
    void onMsgReceive(String msg, LocalSocket client);

    void onConnect(LocalSocket client);

    void onClose(LocalSocket client);
}
