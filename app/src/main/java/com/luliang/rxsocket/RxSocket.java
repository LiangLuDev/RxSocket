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
import io.reactivex.functions.Function;

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
     * 开始连接，连接超时为10秒，这一步如果失败会一直重连
     *
     * @param host
     * @param port
     * @return
     */
    public Observable<Boolean> connect(final String host, final int port) {
        return Observable.create(new ObservableOnSubscribe<Boolean>() {
            @Override
            public void subscribe(final ObservableEmitter<Boolean> emitter) throws Exception {
                close();

                socket = new Socket(host, port);

                emitter.onNext(connect = true);
                emitter.onComplete();
            }
        })
                .timeout(10, TimeUnit.SECONDS)
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
     * 发送数据，超过五秒发送失败
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
    public <T> ObservableTransformer<T, String> read() {
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

                                Log.d("socket","开启线程");
                                new ReadThread(new SocketCallBack() {
                                    @Override
                                    public void onReceive(String result) {
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
        Log.d("socket","初始化Socket");

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
                    String result = bufferedReader.readLine();
                    if (null == result) {
                        throw new NullPointerException("服务器主动断开");
                    }
                    callBack.onReceive(result);
                }
            } catch (Exception e) {
                e.printStackTrace();
            }

            try {
                close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    private interface SocketCallBack {

        void onReceive(String result);
    }
}