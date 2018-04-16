package com.luliang.rxsocket;


import android.util.Log;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.net.Socket;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;

import io.reactivex.Observable;
import io.reactivex.ObservableEmitter;
import io.reactivex.ObservableOnSubscribe;
import io.reactivex.ObservableSource;
import io.reactivex.ObservableTransformer;
import io.reactivex.android.schedulers.AndroidSchedulers;
import io.reactivex.functions.Function;
import io.reactivex.schedulers.Schedulers;


/**
 * @author LuLiang
 * @github https://github.com/LiangLuDev
 * RxSocket 连接处理
 */


public class RxSocket {


    private static final AtomicReference<RxSocket> INSTANCE = new AtomicReference<>();

    private Socket socket;

    private boolean connect = false;
    private boolean threadStart = false;

    private BufferedReader bufferedReader;
    private BufferedWriter bufferedWriter;

    public static RxSocket getInstance() {
        for (; ; ) {
            RxSocket current = INSTANCE.get();
            if (null != current) {
                return current;
            }
            current = new RxSocket();
            if (INSTANCE.compareAndSet(null, current)) {
                return current;
            }
        }
    }

    /**
     * 重连机制的订阅
     *
     * @param host
     * @param port
     * @return
     */
    public Observable<String> reconnection(String host, int port) {
        return connect(host, port)
                .compose(this.<Boolean>read())
                .retry()
                .compose(io_main());
    }

    /**
     * 心跳、重连机制的订阅(心跳数据初始化后不再变化，适用于心跳包数据不改变的情况)
     *
     * @param host
     * @param port
     * @param period 心跳频率
     * @param data   心跳数据
     * @return
     */
    public Observable<String> reconnectionAndHeartBeat(String host, int port, int period, String data) {
        return connect(host, port)
                .compose(this.<Boolean>heartBeat(period, data))
                .compose(this.<Boolean>read())
                .retry()
                .compose(io_main());
    }

    /**
     * 心跳、重连机制的订阅(心跳数据可以动态改变，适用于心跳包数据动态变化的情况)
     *
     * @param host
     * @param port
     * @param period 心跳频率
     * @return
     */
    public Observable<Long> reconnectionAndHeartBeat(String host, int port, final int period) {
        return connect(host, port)
                .flatMap(new Function<Boolean, ObservableSource<? extends Long>>() {
                    @Override
                    public ObservableSource<? extends Long> apply(Boolean aBoolean) throws Exception {
                        return interval(period);
                    }
                });
    }

    /**
     * 心跳包数据动态改变后记得调用该变换方式
     *
     * @return
     */
    public ObservableTransformer<Boolean, String> heartBeatChange() {
        return new ObservableTransformer<Boolean, String>() {
            @Override
            public ObservableSource<String> apply(Observable<Boolean> upstream) {
                return upstream.flatMap(new Function<Boolean, ObservableSource<? extends String>>() {
                    @Override
                    public ObservableSource<? extends String> apply(Boolean aBoolean) throws Exception {
                        return Observable.create(new ObservableOnSubscribe<String>() {
                            @Override
                            public void subscribe(ObservableEmitter<String> emitter) throws Exception {
                                startThread(emitter);
                            }
                        });
                    }
                })
                        .retry()
                        .subscribeOn(Schedulers.io())
                        .observeOn(AndroidSchedulers.mainThread());
            }
        };
    }

    /**
     * 开始连接，这一步如果失败会一直重连，周期为30秒(30秒的意思不是说30秒重连一次，而是30秒内都在重连，30秒内没连接上会重新订阅重新连接，等于一直在重连)
     *
     * @param host
     * @param port
     * @return
     */
    private Observable<Boolean> connect(final String host, final int port) {
        return Observable.create(new ObservableOnSubscribe<Boolean>() {
            @Override
            public void subscribe(final ObservableEmitter<Boolean> emitter) throws Exception {
                //TODO 根据项目需求加上是否有可用网络做判断

                close();

                socket = new Socket(host, port);

                emitter.onNext(connect = true);
                emitter.onComplete();
            }
        })
                .retry();
    }

    /**
     * 心跳
     *
     * @param period 心跳频率
     * @param data   心跳数据
     * @return
     */
    private ObservableTransformer<Boolean, Boolean> heartBeat(final int period, final String data) {
        return new ObservableTransformer<Boolean, Boolean>() {
            @Override
            public ObservableSource<Boolean> apply(Observable<Boolean> upstream) {
                return upstream.flatMap(new Function<Boolean, ObservableSource<? extends Boolean>>() {
                    @Override
                    public ObservableSource<? extends Boolean> apply(Boolean aBoolean) throws Exception {
                        return interval(period)
                                .flatMap(new Function<Long, ObservableSource<? extends Boolean>>() {
                                    @Override
                                    public ObservableSource<? extends Boolean> apply(Long aLong) throws Exception {
                                        return send(data);
                                    }
                                });
                    }
                });
            }
        };
    }

    /**
     * 心跳频率
     *
     * @param period
     * @return
     */
    private Observable<Long> interval(int period) {
        //从0开始是因为这个心跳服务可以立刻开始传递下去，那么socket基本可以实现2秒内重新连接上（不加心跳的话，基本实现秒连）
        return Observable.interval(0, period, TimeUnit.SECONDS);
    }

    /**
     * 读取线程开启
     *
     * @param emitter
     */
    private void startThread(final ObservableEmitter<String> emitter) {
        if (!connect || threadStart) return;

        Log.d("socket", "读取线程开启");
        new ReadThread(new SocketCallBack() {
            @Override
            public void onReceive(String result) {
                //如果服务器断开，这里会返回null,而rxjava2不允许发射一个null值，固会抛出空指针，利用其重新订阅服务
                emitter.onNext(result);
            }
        }).start();
    }

    /**
     * 发送数据，超过五秒发送失败(指定在io线程发送数据)
     *
     * @param data
     * @return
     */
    public Observable<Boolean> send(String data) {
        return Observable.just(data)
                .flatMap(new Function<String, ObservableSource<? extends Boolean>>() {
                    @Override
                    public ObservableSource<? extends Boolean> apply(String s) throws Exception {
                        if (connect && null != bufferedWriter) {
                            bufferedWriter.write(s.concat("\r\n"));
                            bufferedWriter.flush();

                            return Observable.just(true);
                        }
                        return Observable.just(false);
                    }
                })
                .subscribeOn(Schedulers.io())
                .timeout(5, TimeUnit.SECONDS, Observable.just(false));
    }

    /**
     * 读取数据
     *
     * @param <T>
     * @return
     */
    private <T> ObservableTransformer<T, String> read() {
        return new ObservableTransformer<T, String>() {
            @Override
            public ObservableSource<String> apply(Observable<T> upstream) {
                return upstream.flatMap(new Function<T, ObservableSource<? extends String>>() {
                    @Override
                    public ObservableSource<? extends String> apply(T t) throws Exception {
                        return Observable.create(new ObservableOnSubscribe<String>() {
                            @Override
                            public void subscribe(ObservableEmitter<String> emitter) throws Exception {
                                startThread(emitter);
                            }
                        });
                    }
                });
            }
        };
    }

    /**
     * 关闭socket
     *
     * @throws IOException
     */
    private void close() throws IOException {
        Log.d("socket", "初始化Socket");

        connect = false;
        threadStart = false;

        if (null != socket) {
            socket.close();
            socket = null;
        }

        if (null != bufferedReader) {
            bufferedReader.close();
            bufferedReader = null;
        }

        if (null != bufferedWriter) {
            bufferedWriter.close();
            bufferedWriter = null;
        }
    }

    /**
     * 读取数据线程
     */
    private class ReadThread extends Thread {

        private SocketCallBack callBack;

        private ReadThread(SocketCallBack callBack) {
            this.callBack = callBack;
        }

        @Override
        public void run() {
            try {
                threadStart = true;

                bufferedReader = new BufferedReader(new InputStreamReader(socket.getInputStream()));
                bufferedWriter = new BufferedWriter(new OutputStreamWriter(socket.getOutputStream()));

                while (connect) {
                    callBack.onReceive(bufferedReader.readLine());
                }
            } catch (Exception e) {
//                e.printStackTrace();
            }
        }
    }

    /**
     * 线程切换
     *
     * @param <T>
     * @return
     */
    public static <T> ObservableTransformer<T, T> io_main() {
        return upstream -> upstream.subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread());
    }

    private interface SocketCallBack {

        void onReceive(String result);
    }
}