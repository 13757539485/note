package com.hfc.localsocket;

import android.net.LocalSocket;
import android.net.LocalSocketAddress;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.PrintWriter;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
public class LocalSocketClientHandler {
    private static final String TAG = "LocalSocketClient";
    private static final String TAG_MSG = "ServerReceive";
    private final ExecutorService cacheExecutor = Executors.newCachedThreadPool();

    private volatile boolean isRunning = false;

    private LocalSocket clientSocket;

    private OutputStream outputStream;

    private MsgListener msgListener;

    public final Handler mHandler;

    public LocalSocketClientHandler() {
        mHandler = new Handler(Looper.getMainLooper());
    }

    public void setMsgListener(MsgListener msgListener) {
        this.msgListener = msgListener;
    }

    public boolean isRunning() {
        return isRunning;
    }

    public void start(String socketName) {
        if (isRunning) {
            Log.d(TAG, "Client already is running");
            return;
        }
        cacheExecutor.execute(() -> {
            try {
                // 创建本地客户端套接字
                clientSocket = new LocalSocket();
                clientSocket.connect(
                        new LocalSocketAddress(
                                socketName,
                                LocalSocketAddress.Namespace.ABSTRACT
                        )
                );
                if (msgListener != null) {
                    msgListener.onConnect(clientSocket);
                }
                isRunning = true;
                outputStream = clientSocket.getOutputStream();
                Log.e(TAG, "Client starting");
                // 开启接收数据的线程
                startReceive(clientSocket);
            } catch (IOException e) {
                String msg = e.getMessage();
                if ("Connection refused".equals(msg)) {
                    Log.e(TAG, "Server is not running", e);
                } else {
                    Log.e(TAG, "Failed to connect to the server", e);
                }
            }
        });
    }

    public void closeClient() {
        if (!isRunning) return;
        if (clientSocket == null) return;
        isRunning = false;
        cacheExecutor.execute(() -> {
            try {
                clientSocket.shutdownInput();
                clientSocket.shutdownOutput();
                clientSocket.close();
                if (msgListener != null) {
                    msgListener.onClose(clientSocket);
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
            Log.i(TAG, "Client closed");
        });
    }

    public void send(String msg) {
        if (!isRunning) {
            Log.d(TAG, "Client is not running");
            return;
        }
        cacheExecutor.execute(() -> {
            try {
                PrintWriter writer = new PrintWriter(outputStream, true);
                writer.println(msg);
            } catch (Exception e) {
                Log.e(TAG, "Failed to send message to client", e);
            }
        });
    }

    private void startReceive(LocalSocket clientSocket) {
        try {
            BufferedReader reader = new BufferedReader(new InputStreamReader(clientSocket.getInputStream()));
            while (isRunning) {
                String response = reader.readLine();
                if (response == null) {
                    break;
                }

                if ("server_close".equals(response)) {
                    closeClient();
                }
                if (msgListener != null) {
                    msgListener.onMsgReceive(response, clientSocket);
                }
                Log.d(TAG_MSG, "Received from server: " + response);
            }
            reader.close();
        } catch (IOException e) {
            Log.e(TAG, "Failed to read from server", e);
        }
    }
}