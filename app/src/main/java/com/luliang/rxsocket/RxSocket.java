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
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread());
    }

    /**
     * 心跳、重连机制的订阅
     *
     * @param host
     * @param port
     * @return
     */
    public Observable<String> reconnectionAndHeartBeat(String host, int port, int period, String data) {
        return connect(host, port)
                .compose(this.<Boolean>heartBeat(period, data))
                .compose(this.<Boolean>read())
                .retry()
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread());
    }


    /**
     * 开始连接，这一步如果失败会一直重连，周期为30秒
     *
     * @param host
     * @param port
     * @return
     */
    private Observable<Boolean> connect(final String host, final int port) {
        return Observable.create(new ObservableOnSubscribe<Boolean>() {
            @Override
            public void subscribe(final ObservableEmitter<Boolean> emitter) throws Exception {
                //TODO 加上是否有可用网络做判断

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
     * @param <T>
     * @return
     */
    public <T> ObservableTransformer<T, Boolean> heartBeat(final int period, final String data) {
        return new ObservableTransformer<T, Boolean>() {
            @Override
            public ObservableSource<Boolean> apply(Observable<T> upstream) {
                return upstream.flatMap(new Function<T, ObservableSource<? extends Boolean>>() {
                    @Override
                    public ObservableSource<? extends Boolean> apply(T t) throws Exception {
                        return Observable.interval(period, TimeUnit.SECONDS)
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
     * 发送数据，超过五秒发送失败（服务端发送数据必须以\r\n结尾才算发送成功）
     *
     * @param data
     * @return
     */
    public Observable<Boolean> send(String data) {
        return Observable.just(data)
                .flatMap(new Function<String, ObservableSource<? extends Boolean>>() {
                    @Override
                    public ObservableSource<? extends Boolean> apply(String s) {
                        try {
                            if (connect && null != bufferedWriter) {
                                bufferedWriter.write(s);
                                bufferedWriter.write("\r\n");
                                bufferedWriter.flush();

                                return Observable.just(true);
                            }
                        } catch (Exception e) {
                            e.printStackTrace();
                        }
                        return Observable.just(false);
                    }
                })
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
                            public void subscribe(final ObservableEmitter<String> emitter) throws Exception {
                                if (!connect || threadStart) return;

                                Log.d("socket","读取线程开启");
                                new ReadThread(new SocketCallBack() {
                                    @Override
                                    public void onReceive(String result) {
                                        //如果服务器断开，这里会返回null,而rxjava2不允许发射一个null值，固会抛出空指针，利用其重新订阅服务
                                        emitter.onNext(result);
                                    }
                                }).start();
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
                e.printStackTrace();
            }
        }
    }


    private interface SocketCallBack {

        void onReceive(String result);
    }
}