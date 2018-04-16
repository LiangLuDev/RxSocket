# Socket连接-RxSocket
#### 功能简介
> - 服务器断开、网络错误等各种方式导致连接失败都会自动一直重连上服务器。
> - 心跳反馈，设置一个时间，每隔一个时间向服务器发送数据，保持在线。

### 使用方式（Android端）
> Android端扫码下载体验

![RxSocket.png](https://upload-images.jianshu.io/upload_images/2635045-a02398bfe2bf384d.png?imageMogr2/auto-orient/strip%7CimageView2/2/w/200)
#### 1.初始化RxSocket
> PS.此项目使用Rx2
```java
//初始化
RxSocket rxSocket = RxSocket.getInstance();
```
#### 2.重连机制连接
```java
/**
* 重连机制的订阅
* 参数1：服务器地址
* 参数2：端口号
*/
rxSocket.reconnection(HOST, PORT)
        .subscribe(s -> Log.d("server response data", s));
```
#### 3.心跳重连机制连接（不可动态改变心跳数据）
```java
/**
* 心跳、重连机制的订阅
* 参数1：服务器地址
* 参数2：端口号
* 参数3：心跳发送时间
* 参数4：心跳发送信息
*/
rxSocket.reconnectionAndHeartBeat(HOST, PORT, 5, "---Hello---")
        .subscribe(s -> Log.d("server response data", s));
```

#### 4.心跳重连机制连接（可动态改变心跳数据）
> 动态改变心跳数据主要针对于，比如电量cpu内存温度等情况需要动态设置心跳数据。
```java
/**
* 心跳、重连机制的订阅(心跳数据动态改变)
* 参数1：服务器地址
* 参数2：端口号
* 参数3：心跳发送时间
*/
rxSocket.reconnectionAndHeartBeat(HOST, PORT, 5)
		.flatMap(aLong -> mRxSocket.send(mEtHeartText.getText().toString()))
        .compose(mRxSocket.<String>heartBeatChange())
        .subscribe(s -> Log.d("server response data", s));
```

#### 5.发送数据
``` java
mSubscribe = rxSocket.send("hello").subscribe()
```
#### 6.应用退出或者不需要socket取消订阅
``` java
//取消订阅
mSubscribe.dispose();
```
### 使用方式（服务端）
> 使用此软件就不用自己写服务器，先模拟自己测试完毕再跟服务器联调。
> [服务端模拟软件下载](https://github.com/LiangLuDev/RxSocket/blob/167699bdca5a44308affb8d97036e309500adcff/NetAssist.exe)(仅支持Windows系统)
> 按照图片标注设置就行了。测试是否接收到数据能否发送数据就行了。

![网络调试助手.png](https://upload-images.jianshu.io/upload_images/2635045-f1f82da32fc39bed.png?imageMogr2/auto-orient/strip%7CimageView2/2/w/800)
### 意见反馈
如果遇到问题或者好的建议，请反馈到：issue、927195249@qq.com 或者LiangLuDev@gmail.com

如果觉得对你有用的话，赞一下吧!


