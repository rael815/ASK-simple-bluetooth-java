package com.example.bluetoothexample;

import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;

public class ControlArduinoActivity extends AppCompatActivity {

    TextView mTvSendData;
    static TextView mTvReceiveData;
    Button mBtnSendData;
    final static int BT_MESSAGE_READ = 2;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_control_arduino);

        mTvReceiveData = (TextView)findViewById(R.id.tvReceiveData);
        mTvSendData =  (EditText) findViewById(R.id.tvSendData);
        mBtnSendData = (Button)findViewById(R.id.btnSendData);

        mBtnSendData.setOnClickListener(new Button.OnClickListener() {
            @Override
            public void onClick(View view) {
                if(MainActivity.mThreadConnectedBluetooth != null) {
                    MainActivity.mThreadConnectedBluetooth.write(mTvSendData.getText().toString());
                    mTvSendData.setText("");
                }
            }
        });

    }

}
