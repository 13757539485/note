TCP/IP模型

应用层：HTTP、FTP、DNS、Telnet、SNMP、TFTP(术语：消息)

传输层：TCP、UDP(术语：段)，操作系统实现

网络层：IP、ICMP、RIP、OSPF、BGP(术语：片)

网络接口层：3G、4G、5G所在(术语：帧)，硬件接口

全能术语：包(一般都使用数据包不去细分上述术语)

分别对应OSI模型

应用层(为应用提供服务)、表示层(数据格式转化加密)、会话层(建立、管理和维护会话)

传输层(建立、管理和维护对端的连接)

网络层(IP选址、路由选择)

链路层(提供介质访问和链路管理)、物理层(物理传输)

### 网络数据传输原理

封包：

应用层(微信A用户)->通过Http请求发送信息(hello)->传输层(封装包：TCP首部+消息数据)->网络层(封装包：TCP首部+消息数据+IP包首部)->链路层(封装包：TCP首部+消息数据+IP包首部+以太网包首部)->物理层(以太网电缆)

拆包：

链路层(解析首部拆包：TCP首部+消息数据+IP包首部)->网络层(解析首部拆包：TCP首部+消息数据)->传输层(解析首部拆包：消息数据)->应用层(微信B用户)

### TCP vs UDP
||TCP|UDP|
|--|--|--|
|连接|面向连接|面向无连接|
|可靠|可靠传输，使用流量、拥塞控制|不可靠，不使用流量、拥塞控制|
|连接对象个数|一对一|一对一、一对多、多对一、多对多|
|传输方式|面向字节流|面向报文|
|首部开销|最小20字节，最大60字节|8个字节|
|适用场景|可靠传输应用(文件传输)|实时应用(IP电话、视频会议、直播、在线视频)|

UDP既然不可靠如何实现可靠，用UDT解决

UDT：基于UDP，在应用层实现连接、重传，能优化带宽不稳定时传输算法，光纤、海量数据场景使用

### MAC vs IP
MAC地址：和硬件绑定，唯一，48位即6个字节，处于链路层，查看pc mac命令：ipconfig /all

IP地址：32位即4个字节，可修改

## TCP

### 基本特性
面向连接、可靠性、RTT(往返时延)和RTO(重传超时)、数据排序、流量控制、全双工

||流量控制|拥塞控制|
|--|--|--|
|关联|接收方缓存|网络拥堵情况|
|控制原理|滑动窗口控制流量，窗口大小在TCP头部的Window字段，大小为0时发送方停止发送数据并启动持续计时器(间隔询问是否可继续发送数据)，当接收方处理完数据后会发送通知报文|通过慢启动、拥塞避免、快重传和快恢复


### 三次握手
SYN：建立连接的标志位

seq：序列号，用于标识TCP报文段的顺序

ACK：确认消息的标志位

ack：确认消息的通用术语，用于确认已接收到的数据字节的序列号

其他4个标识位：PSH(传送)、FIN(结束)、RST(重置)、URG(紧急)

客户端------------服务端

->SYN=1,seq=X,客户端处于SYN_SENT

ACK=1,ack=X+1,SYN=1,seq=J<-，服务端处于SYN_RECV

->ACK=1,ack=J+1，客户端、服务器处于ESTABLISHED

第一次握手：客户端请求建立连接

第二次握手：服务器应答客户端，并请求建立连接

第三次握手：客户端针对服务器请求确认应答

#### 为何需要三次握手
TCP是全双工的，双方都能发送和接收能力，三次握手能确认双方的发送/接收能力，两次只能证明客户端的发送和服务端的接收能力，四次属于浪费

#### TCP握手漏洞
客户端伪造IP发送请求(第一次握手)，服务端响应(第二次握手)后等待客户端的第三次握手(永远不会有，导致一致等待浪费资源)

解决方案：无效连接监控释放、延缓TCB分配方法、防火墙

### 四次挥手
客户端------------服务端

->FIN=1,seq=X,客户端处于FIN_WAIT_1

ACK=1,ack=X+1<-，服务器处于CLOSE_WAIT，客户端依然能接收数据，收到后处于FIN_WAIT_2

FIN=1,seq=j<-，服务器处于CLOSE，客户端收到后处于TIME_WAIT

->ACK=1,ack=J+1，服务器收到后处于CLOSED，2*MSL后客户端处于CLOSED

MSL：最长报文段寿命(存活的最长时间 RFC中定义为2分钟，一般操作系统中为30s)

TIME_WAIT：持续1-4分钟

第一次挥手：客户端发送关闭请求

第二次挥手：服务器响应客户端关闭请求

第三次挥手：服务端发送关闭请求

第四次挥手：客户端发送关闭请求确认

<font color="#dd0000">客户端和服务端都可以发起关闭请求</font>

#### 为何需要四次挥手
和三次握手相同，如果服务器没有数据发送时第二和第三次挥手会合并

#### TIME_WAIT作用
1. 防止连接关闭时第四次挥手中的ack丢失，如果没有此状态，客户端直接进入CLOSED，一旦客户端最后一次ACK丢失，服务器会重传此时无人处理将长时间等待直到重传次数达上限
2. 防止新连接收到旧链接的TCP报文

## Socket
Socket是应用层与TCP/IP协议通信的中间软件抽象层，是一组接口

对于应用来说网络底层都是socket，将传输层、网络层、链路层包裹

网络通讯：建立连接(客户端、服务端)、读网络数据、写网络数据

BIO：B-Blocking，阻塞式网络通讯也是传统的网络通讯，面向流，如Socket通讯

- 一个客户端需要服务端开一个线程服务，大量客户端就需要大量线程
- 客户端发送请求并一直阻塞等待，服务器处理并响应，客户端收到响应处理信息

服务器ServerSocket：bind、accept、线程(socket)
```kotlin
fun main() {
    val executor = Executors.newFixedThreadPool(Runtime.getRuntime().availableProcessors() * 2)
    val serverSocket = ServerSocket()//服务端必备
    serverSocket.bind(InetSocketAddress(10001))//监听端口
    println("start server........")
    while (true) {
        executor.execute {
            serverSocket.accept().let { s ->
                val bis = ObjectInputStream(s.getInputStream())
                val userName = bis.readUTF()
                println("accept client msg: $userName")
                val bos = ObjectOutputStream(s.getOutputStream())
                bos.writeUTF("Hello, $userName")
                bos.flush()
            }
        }
    }
}
```
客户端socket
```kotlin
fun main() {
    val serverAddress = InetSocketAddress("127.0.0.1", 10001)
    val socket = Socket()
    socket.connect(serverAddress)
    val bos = ObjectOutputStream(socket.getOutputStream())
    bos.writeUTF("yuli")
    bos.flush()
    val bis = ObjectInputStream(socket.getInputStream())
    println(bis.readUTF())
}
```
NIO：IO多路复用，同步非阻塞，优化BIO，面向缓冲区

效率：BIO比NIO高

Selector、Channel、Buffer

Buffer：一般使用ByteBuffer,capacity、position、limit

AIO：异步IO，真正实现的只有Windows

直接内存比堆内存快

堆内存：由于jvm存在垃圾回收机制，需要先拷贝到应用进程缓冲区再拷贝到套接字发送缓冲区

### 零拷贝
指计算机执行操作时，CPU不需要先将数据从某处内存复制到另一个特定区域

尽可能减少CPU拷贝次数或没有CPU拷贝即为零拷贝

使用mmap、sendfile、slice函数可以减少cpu拷贝

#### DMA
直接内存访问技术，用来代替CPU进行数据搬运工作

#### 传统数据传送
4次拷贝：

用户进程-----------------------------------内核

[磁盘->DMA拷贝->文件读取缓冲区]->CPU拷贝->应用进程缓冲区->CPU拷贝

->[套接字发送缓冲区->DMA拷贝->网络设备缓冲区]

4次上下文切换：

用户态->开始read->内核态->read完成->用户态->开始send->内核态->send完成->用户态

#### MMAP内存映射
3次拷贝：

[磁盘->DMA拷贝]->应用进程缓冲区->CPU拷贝

->[套接字发送缓冲区->DMA拷贝->网络设备缓冲区]

上下文切换还是4次

#### sendfile
[磁盘->DMA拷贝->文件读取缓冲区]->CPU拷贝->[套接字发送缓冲区->DMA拷贝->网络设备缓冲区]

硬件支持情况下，CPU拷贝可省略，2次拷贝，2次上下文切换

#### slice
2次拷贝，2次上下文切换：

[磁盘->DMA拷贝->文件读取缓冲区]->PIPE(管道)->[套接字发送缓冲区->DMA拷贝->网络设备缓冲区]

select、poll、epoll