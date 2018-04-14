package com.luliang.rxsocket;

import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.text.TextUtils;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;

import io.reactivex.functions.Consumer;
import io.reactivex.functions.Predicate;
import io.reactivex.schedulers.Schedulers;

public class MainActivity extends AppCompatActivity {
    private RxSocket rxSocket;
    Button mBtnSend;
    Button mBtnConnect;
    EditText mEtSendText;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);


        mBtnSend = findViewById(R.id.btn_send);
        mBtnConnect = findViewById(R.id.btn_connect);
        mEtSendText = findViewById(R.id.et_send_text);

        mBtnConnect.setOnClickListener(view -> connectSocket());
        mBtnSend.setOnClickListener(view -> rxSocket.send(mEtSendText.getText().toString()).subscribeOn(Schedulers.io()).subscribe());
    }

    private void connectSocket() {
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
    }
}
