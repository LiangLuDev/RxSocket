# RxSocket
### Rx封装Socket连接

> 目前简单的实现socket连接，后面会继续完善，尽量考虑到所有需求，

### 使用方式
#### 1.建立连接
```java
		 //初始化socket
        rxSocket = RxSocket.getInstance();
        //socket连接
        rxSocket.connect("192.168.137.1", 8080)
                .filter(aBoolean -> aBoolean)
                .compose(rxSocket.heartBeat(5, "keep heart"))
                .compose(rxSocket.read())
                .filter(s -> !TextUtils.isEmpty(s))
                .compose(RxSchedulers.io_main())
                .subscribe(s -> Log.d("server response data", s));
```

#### 2.发送数据
``` java
rxSocket.send("hello").subscribeOn(Schedulers.io()).subscribe()
```