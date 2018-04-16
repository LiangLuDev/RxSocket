package com.luliang.rxsocket;

import android.annotation.SuppressLint;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.text.TextUtils;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import io.reactivex.disposables.Disposable;

public class MainActivity extends AppCompatActivity {
    Button mBtnSend;
    Button mBtnConnect;
    Button mBtnHeartConnect;
    EditText mEtSendText;
    EditText mEtHost;
    EditText mEtPort;
    TextView mTvResponse;
    private Disposable mSubscribe;


    @SuppressLint("SetTextI18n")
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);


        mBtnSend = findViewById(R.id.btn_send);
        mBtnConnect = findViewById(R.id.btn_connect);
        mBtnHeartConnect = findViewById(R.id.btn_heart_connect);
        mEtSendText = findViewById(R.id.et_send_text);
        mEtHost = findViewById(R.id.et_host);
        mEtPort = findViewById(R.id.et_port);
        mTvResponse = findViewById(R.id.tv_response);


        //初始化
        RxSocket mRxSocket = RxSocket.getInstance();

        mBtnConnect.setOnClickListener(view -> {
            if (TextUtils.isEmpty(mEtHost.getText())) {
                Toast.makeText(MainActivity.this, "请输入服务器地址", Toast.LENGTH_SHORT).show();
                return;
            }
            if (TextUtils.isEmpty(mEtPort.getText())) {
                Toast.makeText(MainActivity.this, "请输入端口号", Toast.LENGTH_SHORT).show();
                return;
            }
            /**
             * 重连机制的订阅
             * 参数1：服务器地址
             * 参数2：端口号
             */
            mRxSocket.reconnection(mEtHost.getText().toString(), Integer.parseInt(mEtPort.getText().toString()))
                    .subscribe(s -> mTvResponse.setText("接收数据：" + s));
        });

        mBtnHeartConnect.setOnClickListener(view -> {
            if (TextUtils.isEmpty(mEtHost.getText())) {
                Toast.makeText(MainActivity.this, "请输入服务器地址", Toast.LENGTH_SHORT).show();
                return;
            }
            if (TextUtils.isEmpty(mEtPort.getText())) {
                Toast.makeText(MainActivity.this, "请输入端口号", Toast.LENGTH_SHORT).show();
                return;
            }
            /**
             * 心跳、重连机制的订阅
             * 参数1：服务器地址
             * 参数2：端口号
             * 参数3：心跳发送时间
             * 参数4：心跳发送信息
             */
            mRxSocket.reconnectionAndHeartBeat(mEtHost.getText().toString(), Integer.parseInt(mEtPort.getText().toString()), 5, "---Hello---")
                    .subscribe(s -> mTvResponse.setText("接收数据：" + s));
        });

        mBtnSend.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                mSubscribe = mRxSocket.send(mEtSendText.getText().toString()).subscribe();
            }
        });
    }


    @Override
    protected void onDestroy() {
        if (mSubscribe != null) {
            mSubscribe.dispose();
        }
        super.onDestroy();

    }
}
