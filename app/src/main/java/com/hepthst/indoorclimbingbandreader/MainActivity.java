package com.hepthst.indoorclimbingbandreader;

import android.Manifest;
import android.bluetooth.BluetoothDevice;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.ServiceConnection;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.os.IBinder;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import com.hepthst.indoorclimbingbandreader.model.UserInfo;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.SocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.FileChannel;
import java.nio.channels.SocketChannel;

public class MainActivity extends AppCompatActivity {

    private String TAG = "MainActivity";

    private TextView mDisplayStateTV;
    private TextView mStepTV;
    private TextView mDeviceInfoTV;
    private TextView mBatteryInfoTV;
    private TextView mDataText;
    private TextView mHeartRateText;
    private Button mScanBtn;
    private Button mBondBtn;
    private Button mConnectBtn;
    private Button mStartHeartBeatsScanBtn;
    private Intent serviceIntent;
    private static final int PERMISSION_REQUEST_COARSE_LOCATION = 1;
    private static final String Address = "192.168.56.1";
    private static final String FILENAME = "Data.txt";
        BroadcastReceiver mReceiver = new BroadcastReceiver() {
@Override
    public void onReceive(Context context, Intent intent) {
            Log.d(TAG, "BroadcastReceiver onReceive");
            if (intent.getAction().equals("state"))
            {
            //get Intent name used for next operation of value
                if (intent.getStringExtra("state").equals("0")) {//断开连接
                //get value of intent
                mDisplayStateTV.append("断开连接\n");
                updateConnectionStateUI(false);
                }
                if (intent.getStringExtra("state").equals("1")) {//连接成功
                mDisplayStateTV.append("连接到目标设备\n");
                updateConnectionStateUI(true);
                } else if (intent.getStringExtra("state").equals("3")) {//扫描超时
                mDisplayStateTV.append("扫描超时，重新扫描\n");
                } else if (intent.getStringExtra("state").equals("4")) {//开启实时计步通知
                mDisplayStateTV.append("开始计步\n");
                } else if (intent.getStringExtra("state").equals("6")) {//开启已配对通知
                    mDisplayStateTV.append("目标设备已配对\n");
                } else if (intent.getStringExtra("state").equals("8")) {//开启已配对通知
                    mDisplayStateTV.append("开始心率扫描...\n");
                }  else if (intent.getStringExtra("state").equals("9")) {//开启已配对通知
                        mDisplayStateTV.append("心率扫描结束...\n");
                } else if (intent.getStringExtra("state").equals("10")) {//开启已配对通知
                    mDisplayStateTV.append("用户信息确认结束...\n");
                    mDisplayStateTV.append("请开启心率监听...\n");
                } else if (intent.getStringExtra("state").equals("11")) {//开启已配对通知
                    mDisplayStateTV.append("心率监听已开启...\n");
                } else if (intent.getStringExtra("state").equals("7")) {//开启Notify后使用scan
                    mDisplayStateTV.append("心率扫描前，请录入用户信息\n");
                    mBondBtn.setEnabled(false);
                    mConnectBtn.setEnabled(false);
                }
                else {
                    String deviceAddress = intent.getStringExtra("state");
                    mDisplayStateTV.append("扫描到目标设备： " + deviceAddress + "\n");
                    mBondBtn.setEnabled(true);
                    mScanBtn.setEnabled(false);
                }
            }
            else if (intent.getAction().equals("step")) {
            mStepTV.setText(intent.getStringExtra("step"));
            }
            else if (intent.getAction().equals("battery")) {
                mBatteryInfoTV.setText(intent.getStringExtra("battery"));
            }
            else if (intent.getAction().equals("heartbeats")) {
                mHeartRateText.setText(intent.getStringExtra("heartbeats"));
            } else if (intent.getAction().equals(BluetoothDevice.ACTION_BOND_STATE_CHANGED)) {
            if (intent.getIntExtra(BluetoothDevice.EXTRA_BOND_STATE, -1 ) == BluetoothDevice.BOND_BONDED){
            mDisplayStateTV.append("绑定目标设备 " + "\n");
            mConnectBtn.setEnabled(true);
            mBondBtn.setEnabled(false);
            }
            }
            }
        };
            //service connection
            LeService.LocalBinder mService;
            ServiceConnection mServiceConnection = new ServiceConnection() {
    @Override
    public void onServiceConnected(ComponentName name, IBinder service) {
            Log.d(TAG, "onServiceConnected");
            mService = (LeService.LocalBinder) service;
            if (mService != null) {
                initBluetooth();
            }
    }

    @Override
    public void onServiceDisconnected(ComponentName name) {}
            };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
            PermisionUtils.verifyStoragePermissions(this);
            super.onCreate(savedInstanceState);
            setContentView(R.layout.activity_main);

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            // Android M Permission check
            if (this.checkSelfPermission(Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            requestPermissions(new String[]{Manifest.permission.ACCESS_COARSE_LOCATION}, PERMISSION_REQUEST_COARSE_LOCATION);
            }
            }

            mDisplayStateTV = (TextView) findViewById(R.id.display_state_tv);
            mStepTV = (TextView) findViewById(R.id.step_info_tv);
            mDeviceInfoTV = (TextView) findViewById(R.id.device_info_tv);
            mBatteryInfoTV = (TextView) findViewById(R.id.bettery_info_tv);
            mDataText = (TextView) findViewById(R.id.display_data_tv);
            mHeartRateText = (TextView) findViewById(R.id.display_heartbeats_tv);
            mScanBtn = (Button) findViewById(R.id.scan_btn);
            mBondBtn = (Button) findViewById(R.id.bond_btn);
            mStartHeartBeatsScanBtn = (Button) findViewById(R.id.HeartRateScan_btn);
            mBondBtn.setEnabled(false);
            mConnectBtn = (Button) findViewById(R.id.connect_btn);
            mConnectBtn.setEnabled(false);


            //开启蓝牙连接的服务
            serviceIntent = new Intent(MainActivity.this, LeService.class);
            bindService(serviceIntent, mServiceConnection, Context.BIND_AUTO_CREATE);

            }

    @Override
    protected void onPause() {
            super.onPause();
            unregisterReceiver(mReceiver);
            }

    @Override
    protected void onResume() {
            super.onResume();
            registerReceiver(mReceiver, makeGattUpdateIntentFilter());
            }

    @Override
    protected void onDestroy() {
            super.onDestroy();
            unbindService(mServiceConnection);
            }

    public void handleClickEvent(View view) {
        if (view.getId() == R.id.scan_btn) {
            mService.startLeScan();
        }
        if (view.getId() == R.id.bond_btn) {
            int result = mService.bondTarget();
            if (result == 1) {
                mDisplayStateTV.append("开始目标设备 " + "\n");
                mConnectBtn.setEnabled(true);
                mBondBtn.setEnabled(false);
            } else if (result == -1) {
                mDisplayStateTV.append("绑定失败 " + "\n");
            }
        }
        if (view.getId() == R.id.connect_btn) {
            mService.connectToGatt();
        }
        if (view.getId() == R.id.user_info_btn){
            UserInfo userInfo = new UserInfo(20271234, 1, 32, 160, 40, "1哈哈", 0);
            mService.setUserInfo(userInfo);
        }
        if (view.getId() == R.id.HeartRateScan_btn){
            mService.startHeartRateScan();
        }
        if (view.getId() == R.id.HeartRateNotify_btn){
            mService.setHeartRateScanListener();
        }
        if (view.getId() == R.id.Transfer_btn){
            new Thread(new Runnable() {
                @Override
                public void run() {
                    SocketChannel socketChannel = null;
                    try{
                        socketChannel = SocketChannel.open();
                        SocketAddress socketAddress = new InetSocketAddress(Address,1991);
                        socketChannel.connect(socketAddress);
                        sendData(socketChannel,"filename");
                        String string = "";
                        string = receiveData(socketChannel);
                        if(!string.isEmpty()){
                            socketChannel = SocketChannel.open();
                            socketChannel.connect(new InetSocketAddress(Address,1991));
                            sendData(socketChannel,string);
                            receiveFile(socketChannel,new File("sdcard/"+string));
                        }else
                            Log.d(TAG, "file object is empty.");
                    }catch (IOException e) {
                        e.printStackTrace();
                    }finally {
                        try{
                            socketChannel.close();
                        }catch (Exception e){}
                    }
                }
            }).start();
        }
        if (view.getId() == R.id.Read_btn){
            File file = new File(Environment.getExternalStorageDirectory(),FILENAME);
            if (Environment.getExternalStorageState().equals(Environment.MEDIA_MOUNTED)){
                try {
                    FileInputStream inputStream = null;
                    inputStream = new FileInputStream(file);
                    byte[] b = new byte[inputStream.available()]; // this what I need
                    inputStream.read(b);
                    mDataText.setText(new String(b));
                    Toast.makeText(MainActivity.this,"读取文件成功",Toast.LENGTH_LONG).show();
                } catch (FileNotFoundException e) {
                    e.printStackTrace();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }else{
                Toast.makeText(MainActivity.this,"SDcard 不存在/此时无法读写",Toast.LENGTH_LONG).show();
            }
        }

    }


    private void sendData(SocketChannel socketChannel, String stirng) throws IOException {
        byte[] bytes = stirng.getBytes();
        ByteBuffer buffer = ByteBuffer.wrap(bytes);
        socketChannel.write(buffer);
        socketChannel.socket().shutdownOutput();
    }

    private String receiveData(SocketChannel socketChannel) throws IOException {
        String string = null;
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        try {
            ByteBuffer buffer = ByteBuffer.allocateDirect(1024);
            byte[] bytes;
            int count = 0;
            while((count = socketChannel.read(buffer)) >= 0){
                buffer.flip();
                bytes = new byte[count];
                buffer.get(bytes);
                baos.write(bytes);
                buffer.clear();
            }
            bytes = baos.toByteArray();
            string = new String(bytes);
        }finally{
            try {
                baos.close();
            }catch (Exception e){}
        }
        return string;
    }

    private static void receiveFile(SocketChannel socketChannel, File file) throws IOException {
        FileOutputStream fos = null;
        FileChannel channel = null;

        try{
            fos = new FileOutputStream(file);
            channel = fos.getChannel();
            ByteBuffer buffer = ByteBuffer.allocateDirect(1024);

            int size = 0;
            while((size = socketChannel.read(buffer)) != -1){
                buffer.flip();
                if (size > 0 ){
                    buffer.limit(size);
                    channel.write(buffer);
                    buffer.clear();
                }
            }
        }finally {
            try{
                channel.close();
            }catch (Exception e){}
            try{
                fos.close();
            }catch (Exception e){}
        }
    }


    private void initBluetooth() {
        boolean bluetoothStatte = mService.initBluetooth();
        if (bluetoothStatte == false)
        {
            mDisplayStateTV.setText("您的设备不支持蓝牙！\n");
        } else {
            boolean leScannerState = mService.initLeScanner();
            if (leScannerState == true) {
                mDisplayStateTV.setText("蓝牙已就绪！\n");
            }
            }
    }

    private static IntentFilter makeGattUpdateIntentFilter() {
    final IntentFilter intentFilter = new IntentFilter();
            intentFilter.addAction("state");
            intentFilter.addAction("step");
            intentFilter.addAction("battery");
            intentFilter.addAction("heartbeats");
            intentFilter.addAction(BluetoothDevice.ACTION_BOND_STATE_CHANGED);
            return intentFilter;
            }

    private void updateConnectionStateUI(boolean enable) {

        String deviceName = enable ? ("MI1S") : ("未连接");
        mDeviceInfoTV.setText(deviceName);

        mBatteryInfoTV.setText("0|100");
        mStepTV.setText("0");
        mHeartRateText.setText("0");
        mDataText.setText("0");
        mScanBtn.setEnabled(!enable);
        mConnectBtn.setEnabled(!enable);
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, String permissions[], int[] grantResults) {
        switch (requestCode) {
            case PERMISSION_REQUEST_COARSE_LOCATION:
            if (grantResults[0] == PackageManager.PERMISSION_GRANTED) {
            // TODO request success
            }
            break;
            }
    }
}