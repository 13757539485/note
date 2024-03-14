#include <netinet/in.h>
#include <map>
#include <iostream>
#include <unistd.h>
#include <cerrno>
#include <sys/socket.h>

#include "ThreadPool.cpp"

int client_sockfd = -1; // 这里只处理一个client
//std::map<int, int> clients; // 多个client处理方式
int server_sockfd = -1;
std::atomic<bool> shouldAccept{true};
std::atomic<bool> shouldRead{true};
ThreadPool pool(4);

void closeServer() {
    if (server_sockfd != -1) {
        shouldAccept.store(false);
        shouldRead.store(false);
        shutdown(server_sockfd, 0);
        close(server_sockfd);
        server_sockfd = -1;
        shutdown(client_sockfd, 0);
        close(client_sockfd);
        client_sockfd = -1;
        // for (auto &client_sockfd: clients) {
        //     shutdown(client_sockfd.first, 0);
        //     close(client_sockfd.first);
        // }
        // clients.clear(); // 清空客户端连接映射表
        ALOGD("server close");
    }
}

void analysisResponse(char buffer[]){
    
}

void receiveClient() {
    struct sockaddr_in cli_addr;
    while (server_sockfd != -1 && shouldAccept.load()) {
        socklen_t clilen = sizeof(cli_addr);
        // 接受客户端连接
        client_sockfd = accept(server_sockfd, (struct sockaddr *) &cli_addr, &clilen);
        if (client_sockfd < 0) {
            // 错误处理
            ALOGD("socket accept error %d", client_sockfd);
            continue;
        }
        // else {
        //     clients[client_sockfd] = client_sockfd;
        // }
        ALOGD("socket accept %d", client_sockfd);
//         循环读取并处理客户端发来的数据
        while (shouldRead.load()) {
            char buffer[1024] = {0};
            ssize_t n = read(client_sockfd, buffer, sizeof(buffer) - 1);
            if (n <= 0) {
                // 客户端断开连接或读取错误，关闭连接并退出循环
                client_sockfd = -1;
                //clients.erase(client_sockfd);
                break;
            }
            buffer[n] = '\0';
            // 处理接收到的数据，例如打印或转发
            analysisResponse(buffer);
        }
    }

//     关闭服务端socket
    closeServer();
}

void startServer(int port) {
    struct sockaddr_in serv_addr;

    // 创建socket
    server_sockfd = socket(AF_INET, SOCK_STREAM, 0);
    if (server_sockfd < 0) {
        // 错误处理
        ALOGD("socket create error %d", server_sockfd);
        return;
    }
    shouldAccept.store(true);
    shouldRead.store(true);
    ALOGD("socket created %d", server_sockfd);
    // 设置socket为SO_REUSEADDR选项，允许快速重用端口
    int reuseaddr = 1;
    if (setsockopt(server_sockfd, SOL_SOCKET, SO_REUSEADDR, &reuseaddr, sizeof(reuseaddr)) <
        0) {
        // 错误处理
        closeServer();
        ALOGD("setsockopt error");
        return;
    }

    // 绑定端口
    bzero((char *) &serv_addr, sizeof(serv_addr));
    serv_addr.sin_family = AF_INET;
    serv_addr.sin_addr.s_addr = INADDR_ANY;
    serv_addr.sin_port = htons(port);
    if (bind(server_sockfd, (struct sockaddr *) &serv_addr, sizeof(serv_addr)) < 0) {
        // 错误处理
        closeServer();
        return;
    }
    ALOGD("socket bind %d", port);
    // 监听端口
    listen(server_sockfd, 5); // 参数5代表最大挂起连接数

    pool.enqueue([&] {
        receiveClient();
    });
}

void sendMsg(const std::string& message) {
    if (!shouldAccept.load()) {
        ALOGD("server is not running");
        return;
    }
    
    if (client_sockfd == -1) {
        ALOGD("clients is empty");
        return;
    }
    // if (clients.empty()) {
    //         ALOGD("clients is empty");
    //         return;
    //     }
    // for (auto &entry: clients) {
    //     const char* send_buffer = message.c_str();
    //     if (send(entry.first, send_buffer, message.length(), 0) < 0) {
    //         ALOGD("sending data error");
    //     }
    // }

    const char* send_buffer = message.c_str();
    if (send(client_sockfd, send_buffer, message.length(), 0) < 0) {
        ALOGD("sending data error");
    }
    ALOGD("sending msg %s", message.c_str());
}
