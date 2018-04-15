# RxSocket
### Rx封装Socket连接
#### 功能简介
> - 服务器断开、网络错误等各种方式导致连接失败都会自动一直重连上服务器。
> - 心跳反馈，设置一个时间，每隔一个时间向服务器发送数据，保持在线。

### 使用方式（Android端）
> Android端扫码下载体验

![RxSocket.png](https://upload-images.jianshu.io/upload_images/2635045-a02398bfe2bf384d.png?imageMogr2/auto-orient/strip%7CimageView2/2/w/200)
#### 1.初始化RxSocket
```java
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
> [服务端模拟软件下载]()(仅支持Windows系统)
> 按照图片标注设置就行了。测试是否接收到数据能否发送数据就行了。
![网络调试助手.png](https://upload-images.jianshu.io/upload_images/2635045-f1f82da32fc39bed.png?imageMogr2/auto-orient/strip%7CimageView2/2/w/800)
### 意见反馈
如果遇到问题或者好的建议，请反馈到：issue、927195249@qq.com 或者LiangLuDev@gmail.com

如果觉得对你有用的话，赞一下吧!


