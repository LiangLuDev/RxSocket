package com.luliang.rxsocket;

import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.text.TextUtils;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;

import io.reactivex.functions.Consumer;
import io.reactivex.schedulers.Schedulers;

public class MainActivity extends AppCompatActivity {
    private RxSocket rxSocket;
    Button mBtnSend;
    Button mBtnConnect;
    Button mBtnHeartConnect;
    EditText mEtSendText;
    private String HOST ="192.168.1.223";
    private int PORT=8080;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);


        mBtnSend = findViewById(R.id.btn_send);
        mBtnConnect = findViewById(R.id.btn_connect);
        mBtnHeartConnect = findViewById(R.id.btn_heart_connect);
        mEtSendText = findViewById(R.id.et_send_text);



         //初始化
        RxSocket rxSocket = RxSocket.getInstance();

        mBtnConnect.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                //重连机制的订阅
                rxSocket.reconnection(HOST, PORT)
                        .subscribe(s -> Log.d("server response data", s));
            }
        });

        mBtnHeartConnect.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                //心跳、重连机制的订阅
                rxSocket.reconnectionAndHeartBeat(HOST, PORT, 5, "---Hello---")
                        .subscribe(s -> Log.d("server response data", s));
            }
        });

        mBtnSend.setOnClickListener(view -> rxSocket.send(mEtSendText.getText().toString()).subscribeOn(Schedulers.io()).subscribe());
    }
}
