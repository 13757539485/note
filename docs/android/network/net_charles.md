## 抓包工具charles
下载地址：https://www.charlesproxy.com/download/

注册：https://www.zzzmode.com/mytools/charles/

### 代理配置

#### window

Help->SSL Proxying->Install Charles Root Certificate

![cert](../../img/net/win_cert.png)

选择安装证书(I)...

![cert1](../../img/net/win_cert1.png)

选择本地计算机

![cert2](../../img/net/win_cert2.png)

选择受信任的根证书颁发机构

![cert3](../../img/net/win_cert3.png)
### 基本使用

## Wireshark

下载地址：https://www.wireshark.org/

BPF语法：https://www.wireshark.org/docs/dfref/

### 抓包百度案例

TCP三次握手分析

使用过滤器过滤ip和tcp

![ws](../../img/net/win_ws.png)

![ws1](../../img/net/win_ws1.png)

Frame：物理层

Ethernet：链路层

Internet：网络层

Transmission：传输层，TCP所在

第一次握手：客户端发起请求，SYN=1，Seq=1319028629
![ws2](../../img/net/win_ws2.png)
第二次握手：服务端应答，并请求建立连接，ACK=1,ack=1319028629+1,SYN=1,seq=4292058564
![ws3](../../img/net/win_ws3.png)
第三次握手：客户端应答，ACK=1,ack=4292058564+1
![ws4](../../img/net/win_ws4.png)