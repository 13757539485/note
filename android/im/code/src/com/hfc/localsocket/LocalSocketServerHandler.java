package com.hfc.localsocket;

import android.net.LocalServerSocket;
import android.net.LocalSocket;
import android.system.Os;
import android.system.OsConstants;
import android.util.Log;

import java.io.IOException;
import java.net.SocketException;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.SynchronousQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

public class LocalSocketServerHandler {
    private static final String TAG = "LocalSocketServer";

    private final ExecutorService cacheExecutor = new ThreadPoolExecutor(
            Runtime.getRuntime().availableProcessors(),
            100,
            60L, TimeUnit.SECONDS,
            new SynchronousQueue<>()
    );
    private volatile boolean isRunning = false;

    private LocalServerSocket serverSocket;

    private CopyOnWriteArrayList<Client> clients;

    public LocalSocketServerHandler() {
        clients = new CopyOnWriteArrayList<>();
    }

    private MsgListener msgListener;

    public void setMsgListener(MsgListener msgListener) {
        this.msgListener = msgListener;
    }

    public void start(String socketName) {
        if (isRunning) {
            Log.d(TAG, "Server already running");
            return;
        }
        try {
            // 创建本地服务器套接字
            serverSocket = new LocalServerSocket(socketName);
            isRunning = true;
            Log.e(TAG, "Server starting");
            // 启动监听线程
            startReceive();
        } catch (IOException e) {
            Log.e(TAG, "Failed to create server socket", e);
        }
    }

    public void closeServer() {
        if (serverSocket == null) {
            Log.e(TAG, "Server is no start");
            return;
        }
        send("server_close");
        isRunning = false;
        try {
            Os.shutdown(serverSocket.getFileDescriptor(), OsConstants.SHUT_RDWR);
            serverSocket.close();
            if (msgListener != null) {
                msgListener.onClose(null);
            }
        } catch (Exception e) {
            Log.d(TAG, "Server shutdown：", e);
        }

        Log.d(TAG, "Server closed");
    }

    public void send(String msg) {
        if (!isRunning) {
            Log.d(TAG, "Server is not running");
            return;
        }
        for (Client client : clients) {
            if (client.checkValid()) {
                client.send(msg);
            }
        }
    }

    public void sendAsync(String msg) {
        if (!isRunning) {
            Log.d(TAG, "Server is not running");
            return;
        }
        cacheExecutor.execute(() -> {
            for (Client client : clients) {
                if (client.checkValid()) {
                    client.send(msg);
                }
            }
        });
    }

    private void startReceive() {
        cacheExecutor.execute(() -> {
            while (isRunning) {
                try {
                    // 接收新的客户端连接
                    LocalSocket clientSocket = serverSocket.accept();
                    Client client = new Client(clientSocket, clients);
                    client.setMsgListener(msgListener);
                    clients.add(client);
                    Log.i(TAG, "has client connect: " + clientSocket);
                    if (msgListener != null) {
                        msgListener.onConnect(clientSocket);
                    }
                    // 创建一个处理客户端请求的线程
                    cacheExecutor.execute(client::handleClient);
                } catch (SocketException e) {
                    Log.e(TAG, "Error accepting connection", e);
                } catch (IOException e) {
                    Log.e(TAG, "IO error", e);
                }
            }
        });
    }
}
