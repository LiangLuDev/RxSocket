# RxSocket
### Rx封装Socket连接
#### 功能简介
> - 服务器断开、网络错误等各种方式导致连接失败都会自动一直重连上服务器。
> - 心跳反馈，设置一个时间，每隔一个时间向服务器发送数据，保持在线。

### 使用方式（Android端）
> Android端下载体验 [RxSocket.apk]()
#### 1.初始化RxSocket
```java
//服务器地址
private String HOST ="192.168.1.223";
//服务器端口号
private int PORT=8080;
//初始化
RxSocket rxSocket = RxSocket.getInstance();
```
#### 2.重连机制连接
```java
//重连机制的订阅
rxSocket.reconnection(HOST, PORT)
        .subscribe(s -> Log.d("server response data", s));
```
#### 3.心跳重连机制连接
```java
//心跳、重连机制的订阅
rxSocket.reconnectionAndHeartBeat(HOST, PORT, 5, "---Hello---")
        .subscribe(s -> Log.d("server response data", s));
```
#### 4.发送数据
``` java
rxSocket.send("hello").subscribeOn(Schedulers.io()).subscribe()
```
### 使用方式（服务端）
> 使用此软件就不用自己写服务器，先模拟自己测试完毕再跟服务器联调。
> [服务端模拟软件下载]()
> 按照图片标注设置就行了。测试是否接收到数据能否发送数据就行了。
![Alt text](./网络调试助手.png)

