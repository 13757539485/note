package com.hfc.localsocket;

import android.net.LocalSocket;
import android.util.Log;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

public class Client {
    private static final String TAG = "Client";

    private static final String TAG_MSG = "ClientReceive";

    private volatile boolean startRead;
    private OutputStream outputStream;
    private final LocalSocket clientSocket;
    private MsgListener msgListener;
    private final CopyOnWriteArrayList<Client> clients;

    public void setMsgListener(MsgListener msgListener) {
        this.msgListener = msgListener;
    }

    public Client(LocalSocket clientSocket, CopyOnWriteArrayList<Client> clients) throws IOException {
        startRead = true;
        this.clientSocket = clientSocket;
        this.clients = clients;
        outputStream = clientSocket.getOutputStream();
    }

    public void handleClient() {
        try {
            BufferedReader reader = new BufferedReader(new InputStreamReader(clientSocket.getInputStream()));
            PrintWriter writer =
                    new PrintWriter(new BufferedWriter(new OutputStreamWriter(outputStream)), true);
            while (startRead) {
                String message = reader.readLine();

                // 客户端关闭
                if (message == null) {
                    close();
                    clients.remove(this);
                    if (msgListener != null) {
                        msgListener.onClose(clientSocket);
                    }
                    break;
                }

                if (msgListener != null) {
                    msgListener.onMsgReceive(message, clientSocket);
                }
                Log.i(TAG_MSG, "Receive from " + clientSocket +  " message: " + message);
            }
            reader.close();
            writer.close();
            Log.i(TAG, "Server not continue read: " + clientSocket);
        } catch (IOException e) {
            Log.i(TAG, "HandleClient exception：" + e.getMessage());
        }
    }

    public boolean checkValid() {
        return outputStream != null;
    }

    public void send(String msg) {
        if (outputStream == null) {
            Log.d(TAG, "OutputStream is null: " + clientSocket);
            return;
        }
        PrintWriter writer = new PrintWriter(outputStream, true);
        writer.println(msg);
    }

    public void close() {
        outputStream = null;
        startRead = false;
        Log.i(TAG, "Client is closed: " + clientSocket);
    }
}
