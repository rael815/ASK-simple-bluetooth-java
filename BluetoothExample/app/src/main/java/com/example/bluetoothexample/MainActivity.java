package com.example.bluetoothexample;

import android.Manifest;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothSocket;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.os.Handler;
import android.os.SystemClock;
import android.util.Log;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.UnsupportedEncodingException;
import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.Set;
import java.util.UUID;

public class MainActivity extends AppCompatActivity {

    public static final int ControlArduino = 1001; /*다른 액티비티를 띄우기 위한 요청코드(상수)*/
    private static final int REQUEST_ENABLE_BT = 0;
    private static final int REQUEST_DISCOVERABLE_BT = 1;
    final static int BT_MESSAGE_READ = 2;
    final static int BT_CONNECTING_STATUS = 3;
    public static final int PERMISSION_REQUEST_CODE = 4;

    private TextView mStatusBlueTv;
    private ListView mPairedLv, mNewLv;
    private ImageView mBlueIv;
    private Button mOnBtn, mOffBtn, mDiscoverBtn, mPairedBtn;
    private BluetoothAdapter mBlueAdapter = null;
    private ArrayAdapter<String> BTArrayAdapter;
    final static UUID BT_UUID = UUID.fromString("00001101-0000-1000-8000-00805F9B34FB");

    private MyBluetoothHandler handler = new MyBluetoothHandler(this);
    static ConnectingThread mThreadConnectedBluetooth;




    @Override
    protected void onCreate(Bundle savedInstanceState)  {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        mStatusBlueTv  = findViewById(R.id.statusBluetoothTv);
        mBlueIv         = findViewById(R.id.BluetoothIv);
        mOnBtn          = findViewById(R.id.OnBtn);
        mOffBtn         = findViewById(R.id.OffBtn);
        mDiscoverBtn    = findViewById(R.id.DiscoverableBtn);
        mPairedBtn      = findViewById(R.id.PairedBtn);
        mPairedLv = (ListView)findViewById(R.id.PairedLv);
        mNewLv =  (ListView)findViewById(R.id.NewDevLv);


        // Register for broadcasts when a device is discovered.
        IntentFilter filter = new IntentFilter();
        filter.addAction(BluetoothAdapter.ACTION_STATE_CHANGED); //BluetoothAdapter.ACTION_STATE_CHANGED : 블루투스 상태변화 액션
        filter.addAction(BluetoothAdapter.ACTION_CONNECTION_STATE_CHANGED);
        filter.addAction(BluetoothDevice.ACTION_ACL_CONNECTED); //연결 확인
        filter.addAction(BluetoothDevice.ACTION_ACL_DISCONNECTED); //연결 끊김 확인
        filter.addAction(BluetoothDevice.ACTION_BOND_STATE_CHANGED);
        filter.addAction(BluetoothDevice.ACTION_FOUND);    //기기 검색됨
        filter.addAction(BluetoothAdapter.ACTION_DISCOVERY_STARTED);   //기기 검색 시작
        filter.addAction(BluetoothAdapter.ACTION_DISCOVERY_FINISHED);  //기기 검색 종료
        filter.addAction(BluetoothDevice.ACTION_PAIRING_REQUEST);
        registerReceiver(bReceiver,filter);

        BTArrayAdapter = new ArrayAdapter<String>(this, android.R.layout.simple_list_item_1);
        mBlueAdapter = BluetoothAdapter.getDefaultAdapter();




        // Check if Bluetooth is available or not
        if(mBlueAdapter == null) {
            mStatusBlueTv.setText("Bluetooth is not available");
        }
        else {
            mStatusBlueTv.setText("Bluetooth is available");
        }

        // Set image according to the bluetooth status (on/off)
        if(mBlueAdapter.isEnabled()) {
            mBlueIv.setImageResource(R.drawable.ic_action_on);

        }
        else {
            mBlueIv.setImageResource(R.drawable.ic_action_off);
        }


        // on btn click
        mOnBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if(!mBlueAdapter.isEnabled()) {
                    showToast("turning on bluetooth...");
                    // intent to on bluetooth
                    Intent intent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
                    startActivityForResult(intent, REQUEST_ENABLE_BT);
                }
                else {
                    showToast("bluetooth is already on");
                    mBlueIv.setImageResource(R.drawable.ic_action_on);
                }

            }
        });

        // discover bluetooth btn click
        mDiscoverBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if(!mBlueAdapter.isDiscovering()) {
                    showToast("Making Your Device Discoverable");
                    Intent intent = new Intent(BluetoothAdapter.ACTION_REQUEST_DISCOVERABLE);
                    startActivityForResult(intent, REQUEST_DISCOVERABLE_BT);
                }
            }
        });

        // off btn click
        mOffBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if(mBlueAdapter.isEnabled()) {
                    mBlueAdapter.disable();
                    showToast("Turning Bluetooth off");
                    mBlueIv.setImageResource((R.drawable.ic_action_off));
                }
                else {
                    showToast("Bluetooth is already off");
                }

            }
        });


    // get paired devices btn click
        mPairedBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if (mBlueAdapter.isEnabled()) {
                    if (mBlueAdapter.isDiscovering()) {
                        // the button is pressed when it discovers, so cancel the discovery
                        mBlueAdapter.cancelDiscovery();
                    }
                    else {

                        BTArrayAdapter.clear();
                        /*
                        COARSE Permission is mandatory in the version of over Android 6.0.
                        */
                        if (ContextCompat.checkSelfPermission(MainActivity.this, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
                            ActivityCompat.requestPermissions(MainActivity.this, new String[]{Manifest.permission.ACCESS_COARSE_LOCATION}, PERMISSION_REQUEST_CODE);
                        }
                        mBlueAdapter.startDiscovery();
                    }
                    final ArrayList<String> list = new ArrayList<>();
                    Set<BluetoothDevice> PairedDevices = mBlueAdapter.getBondedDevices();
                    for(BluetoothDevice device: PairedDevices) {
                        list.add(device.getName() + "," + device);
                        Toast.makeText(getApplicationContext(), "Showing Paired and New Devices",Toast.LENGTH_SHORT).show();
                        final ArrayAdapter adapter;
                        // "this" error => "MainActivity.this"
                        adapter = new  ArrayAdapter<String>(MainActivity.this,android.R.layout.simple_list_item_1,list);
                        mPairedLv.setAdapter(adapter);

                        //onItemClickListener를 추가
                        mPairedLv.setOnItemClickListener(new AdapterView.OnItemClickListener() {
                            @Override
                            public void onItemClick(AdapterView<?> parent, View clickedView, int position, long id) {
                                mBlueAdapter.cancelDiscovery();
                                String  itemValue = (String) mPairedLv.getItemAtPosition(position);
                                String MAC = itemValue.substring(itemValue.length() - 17);
                                BluetoothDevice bluetoothDevice = mBlueAdapter.getRemoteDevice(MAC);
                                // Initiate a connection request in a separate thread
                                mThreadConnectedBluetooth = new ConnectingThread(bluetoothDevice);
                                mThreadConnectedBluetooth.start();

                            }
                        });
                    }

                    mNewLv.setAdapter(BTArrayAdapter);
                    //onItemClickListener를 추가
                    mNewLv.setOnItemClickListener(new AdapterView.OnItemClickListener() {
                        @Override
                        public void onItemClick(AdapterView<?> parent, View clickedView, int position, long id) {
                            mBlueAdapter.cancelDiscovery();
                            String  itemValue = (String) mNewLv.getItemAtPosition(position);
                            String MAC = itemValue.substring(itemValue.length() - 17);
                            BluetoothDevice bluetoothDevice = mBlueAdapter.getRemoteDevice(MAC);
                            // Initiate a connection request in a separate thread
                            mThreadConnectedBluetooth = new ConnectingThread(bluetoothDevice);
                            mThreadConnectedBluetooth.start();
                        }
                    });
                }
                else {
                    //bluetooth is off so can't get paired devices
                    showToast("Turn on bluetooth to get paired devices");
                }

            }
        });
    }

    private static final class MyBluetoothHandler extends Handler {
        private final WeakReference<MainActivity> ref;

        public MyBluetoothHandler(MainActivity act) {
            ref = new WeakReference<>(act);
        }

        @Override
        public void handleMessage(android.os.Message msg) {
            MainActivity act = ref.get();
            if (act != null) {
                // do work
                if(msg.what == BT_MESSAGE_READ){
                    String readMessage = null;
                    try {
                        readMessage = new String((byte[]) msg.obj, "UTF-8");
                    } catch (UnsupportedEncodingException e) {
                        e.printStackTrace();
                    }
                    ControlArduinoActivity.mTvReceiveData.setText(readMessage);
                }
            }

        }
    }


    BroadcastReceiver bReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            final String action = intent.getAction();   //입력된 action
            Toast.makeText(context, "받은 액션 : "+action , Toast.LENGTH_SHORT).show();
            Log.d("Bluetooth action", action);
            final BluetoothDevice device = intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE);

            //입력된 action에 따라서 함수를 처리한다
            switch (action){
                case BluetoothAdapter.ACTION_STATE_CHANGED: //블루투스의 연결 상태 변경
                    final int state = intent.getIntExtra(BluetoothAdapter.EXTRA_STATE, BluetoothAdapter.ERROR);
                    switch(state) {
                        case BluetoothAdapter.STATE_OFF:

                            break;
                        case BluetoothAdapter.STATE_TURNING_OFF:

                            break;
                        case BluetoothAdapter.STATE_ON:

                            break;
                        case BluetoothAdapter.STATE_TURNING_ON:

                            break;
                    }

                    break;
                case BluetoothDevice.ACTION_ACL_CONNECTED:  //블루투스 기기 연결

                    break;
                case BluetoothDevice.ACTION_BOND_STATE_CHANGED:

                    break;
                case BluetoothDevice.ACTION_ACL_DISCONNECTED:   //블루투스 기기 끊어짐

                    break;

                case BluetoothAdapter.ACTION_DISCOVERY_STARTED: //블루투스 기기 검색 시작

                    break;
                case BluetoothDevice.ACTION_FOUND:  //블루투스 기기 검색 됨, 블루투스 기기가 근처에서 검색될 때마다 수행됨
                    String deviceName = device.getName();
                    String deviceHardwareAddress = device.getAddress();
                    if (device.getBondState() != BluetoothDevice.BOND_BONDED) {
                        BTArrayAdapter.add(deviceName + "," + deviceHardwareAddress);
                        }
                    break;

                case BluetoothAdapter.ACTION_DISCOVERY_FINISHED:    //블루투스 기기 검색 종료
                    setProgressBarIndeterminateVisibility(false);
                    if (BTArrayAdapter.getCount() == 0) {
                        String noDevices = getResources().getText(R.string.none_found).toString();
                        BTArrayAdapter.add(noDevices);
                    }
                    break;
                case BluetoothDevice.ACTION_PAIRING_REQUEST:

                    break;
            }

        }
    };

    @Override
    protected void onDestroy() {
        // TODO Auto-generated method stub
        super.onDestroy();
        unregisterReceiver(bReceiver);
    }

    class ConnectingThread extends Thread {
        private final BluetoothSocket bluetoothSocket;
        private final BluetoothDevice bluetoothDevice;
        private final InputStream mmInStream;
        private final OutputStream mmOutStream;

        public ConnectingThread(BluetoothDevice device) {
            InputStream tmpIn = null;
            OutputStream tmpOut = null;
            BluetoothSocket temp = null;
            bluetoothDevice = device;

            // Get a BluetoothSocket to connect with the given BluetoothDevice
            try {
                temp = bluetoothDevice.createRfcommSocketToServiceRecord(BT_UUID);
                tmpIn = temp.getInputStream();
                tmpOut = temp.getOutputStream();
            } catch (IOException e) {
                e.printStackTrace();
            }
            bluetoothSocket = temp;
            mmInStream = tmpIn;
            mmOutStream = tmpOut;
        }


        public void run() {
            // Cancel any discovery as it will slow down the connection
            mBlueAdapter.cancelDiscovery();
            byte[] buffer = new byte[2048];
            int bytes;

            try {
                // This will block until it succeeds in connecting to the device
                // through the bluetoothSocket or throws an exception
                bluetoothSocket.connect();
                Intent intent = new Intent(getApplicationContext(),ControlArduinoActivity.class);
                startActivityForResult(intent,ControlArduino);//액티비티 띄우기

            } catch (IOException connectException) {
                connectException.printStackTrace();
                try {
                    bluetoothSocket.close();
                } catch (IOException closeException) {
                    closeException.printStackTrace();
                }
            }

            while (true) {
                try {
                    bytes = mmInStream.available();
                    if (bytes != 0) {
                        SystemClock.sleep(1000);
                        bytes = mmInStream.available();
                        bytes = mmInStream.read(buffer, 0, bytes);
                        handler.obtainMessage(BT_MESSAGE_READ, bytes, -1, buffer).sendToTarget();
                    }
                } catch (IOException e) {
                    break;
                }
            }

            // Code to manage the connection in a separate thread
        /*
            manageBluetoothConnection(bluetoothSocket);
        */
        }

        // Cancel an open connection and terminate the thread
        public void cancel() {
            try {
                bluetoothSocket.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }

        public void write(String toString) {
            byte[] bytes = toString.getBytes();
            try {
                mmOutStream.write(bytes);
            } catch (IOException e) {
                Toast.makeText(getApplicationContext(), "데이터 전송 중 오류가 발생했습니다.", Toast.LENGTH_LONG).show();
            }
        }
    }


    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        switch (requestCode) {
            case REQUEST_ENABLE_BT:
                if (resultCode == RESULT_OK) {
                    // bluetooth is on
                    mBlueIv.setImageResource(R.drawable.ic_action_on);
                    showToast("Bluetooth is on" );
                }
                else {
                    // user denied to turn bluetooth on
                    showToast("Could't on Bluetooth");
                }
                break;
        }
        super.onActivityResult(requestCode, resultCode, data);
    }

    // toast message function
    private void showToast(String msg) {
        Toast.makeText(this, msg, Toast.LENGTH_SHORT).show();
    }


}

/* Using BluetoothAdapter class we will do the following operations
 * 1-Check if Bluetooth is available or not.
 * 2-Turn On/Off Bluetooth.
 * 3-Make Bluetooth Discoverable.
 * 4-Display Paired/Bounded devices.
 * Note: The getBoundedDevices() method of BluetoothAdapter class provides a set containing list of all paired or bounded bluetooth devices.
 * Permissions Required: BLUETOOTH, BLUETOOTH_ADMIN */