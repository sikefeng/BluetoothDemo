package com.example.bluetoothdemo;

import androidx.appcompat.app.AppCompatActivity;

import android.app.Activity;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.util.Log;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.ListView;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import java.util.Set;

public class MainActivity extends AppCompatActivity implements View.OnClickListener {

    public static final int MESSAGE_STATE_CHANGE = 1;
    public static final int MESSAGE_READ = 2;
    public static final int MESSAGE_WRITE = 3;
    public static final int MESSAGE_DEVICE_NAME = 4;
    public static final int MESSAGE_TOAST = 5;
    private static final int REQUEST_ENABLE_CODE = 1;
    public static final String DEVICE_NAME = "device_name";
    public static final String TOAST = "toast";

    private BluetoothAdapter mBluetoothAdapter;
    private TextView tvState;
    private ProgressBar processBar;
    private ArrayAdapter<String> mPairedDevicesArrayAdapter;
    private ArrayAdapter<String> mNewDevicesArrayAdapter;
    private ChatClient mChatService = null;
    private String mConnectedDeviceName = null;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        initView();
        initBluetooth();
        initScanBlueReceiver();
        mChatService = new ChatClient(mHandler);

    }

    private final Handler mHandler = new Handler() {
        @Override
        public void handleMessage(Message msg) {
            switch (msg.what) {
                case MESSAGE_STATE_CHANGE:
                    switch (msg.arg1) {
                        case ChatClient.STATE_CONNECTED:
//                            mConversationArrayAdapter.clear();
                            break;
                        case ChatClient.STATE_CONNECTING:
                            break;
                        case ChatClient.STATE_LISTEN:
                        case ChatClient.STATE_NONE:
                            break;
                    }
                    break;
                case MESSAGE_WRITE:
                    byte[] writeBuf = (byte[]) msg.obj;
                    String writeMessage = new String(writeBuf);
//                    mConversationArrayAdapter.add("我： " + writeMessage);
                    showToast("1."+writeMessage);
                    break;
                case MESSAGE_READ:
                    byte[] readBuf = (byte[]) msg.obj;
                    String readMessage = new String(readBuf, 0, msg.arg1);
//                    mConversationArrayAdapter.add(mConnectedDeviceName + ": "+ readMessage);
                    showToast("2."+readMessage);
                    break;
                case MESSAGE_DEVICE_NAME:
                    mConnectedDeviceName = msg.getData().getString(DEVICE_NAME);
                    Toast.makeText(getApplicationContext(), "链接到" + mConnectedDeviceName, Toast.LENGTH_SHORT).show();
                    break;
                case MESSAGE_TOAST:
                    Toast.makeText(getApplicationContext(), msg.getData().getString(TOAST), Toast.LENGTH_SHORT).show();
                    break;
            }
        }
    };


    private void initScanBlueReceiver() {
        IntentFilter filter = new IntentFilter(android.bluetooth.BluetoothAdapter.ACTION_DISCOVERY_STARTED);
        filter.addAction(android.bluetooth.BluetoothAdapter.ACTION_DISCOVERY_FINISHED);
        filter.addAction(BluetoothDevice.ACTION_FOUND);

        registerReceiver(scanBlueReceiver, filter);

    }

    private ScanBlueReceiver scanBlueReceiver = new ScanBlueReceiver(new ScanBlueCallBack() {
        @Override
        public void onScanStarted() {
            processBar.setVisibility(View.VISIBLE);
        }

        @Override
        public void onScanFinished() {
            processBar.setVisibility(View.GONE);
        }

        @Override
        public void onScanning(BluetoothDevice device) {
            if (device.getBondState() != BluetoothDevice.BOND_BONDED) {
                mNewDevicesArrayAdapter.add(device.getName() + "\n" + device.getAddress());
            }
        }
    });

    @Override
    protected void onDestroy() {
        super.onDestroy();
        cancelScanBuletooth();
        this.unregisterReceiver(scanBlueReceiver);
        if (mChatService != null){
            mChatService.stop();
        }
    }

    private void initBluetooth() {
        mBluetoothAdapter = BluetoothAdapter.getDefaultAdapter();//获取BluetoothAdapter对象
        if (mBluetoothAdapter == null) {
            showToast("该设备不支持蓝牙使用！");
            return;
        }
        mPairedDevicesArrayAdapter = new ArrayAdapter<String>(this, R.layout.list_item);
        mNewDevicesArrayAdapter = new ArrayAdapter<String>(this, R.layout.list_item);

        ListView pairedListView = (ListView) findViewById(R.id.paired_devices);
        pairedListView.setAdapter(mPairedDevicesArrayAdapter);
        pairedListView.setOnItemClickListener(mDeviceClickListen);

        ListView newDeviceListView = (ListView) findViewById(R.id.new_devices);
        newDeviceListView.setAdapter(mNewDevicesArrayAdapter);
        newDeviceListView.setOnItemClickListener(mDeviceClickListen);


        Set<BluetoothDevice> pairedDevices = mBluetoothAdapter.getBondedDevices();
        if (pairedDevices.size() > 0) {
            for (BluetoothDevice device : pairedDevices) {
                mPairedDevicesArrayAdapter.add(device.getName() + "\n" + device.getAddress());
            }
        } else {
            String noDevices = getResources().getText(R.string.none_paired).toString();
            mPairedDevicesArrayAdapter.add(noDevices);
        }
    }

    private AdapterView.OnItemClickListener mDeviceClickListen = new AdapterView.OnItemClickListener() {
        @Override
        public void onItemClick(AdapterView<?> adapterView, View view, int i, long l) {
            cancelScanBuletooth();
            String info = ((TextView) view).getText().toString();
            String address = info.substring(info.length() - 17);
            BluetoothDevice device = mBluetoothAdapter.getRemoteDevice(address);
            mChatService.connect(device);
        }
    };

    /**
     * 自动打开蓝牙（同步）
     * 这个方法打开蓝牙会弹出提示
     * 需要在onActivityResult 方法中判断resultCode == RESULT_OK  true为成功
     */
    public void openBluetooth(Activity activity, int requestCode) {
        Intent intent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
        activity.startActivityForResult(intent, requestCode);
    }

    /**
     * 自动打开蓝牙（异步：蓝牙不会立刻就处于开启状态）
     * 这个方法打开蓝牙不会弹出提示
     */
//    public void openBlueAsyn() {
//        if (isSupportBlue()) {
//            mBluetoothAdapter.enable();
//        }
//    }


    //关闭蓝牙
    public void closeBluetooth() {
        if (mBluetoothAdapter.isEnabled()) {
            //关闭蓝牙
            mBluetoothAdapter.disable();
            showToast("蓝牙关闭成功");
        } else {
            showToast("蓝牙已关闭");
        }
        checkBluetoothState();
    }

    /**
     * 扫描的方法 返回true 扫描成功
     * 通过接收广播获取扫描到的设备
     * (扫描周围蓝牙设备（配对上的设备有可能扫描不出来）)
     *
     * @return
     */
    public boolean scanBluetooth() {
        if (!isBlueEnable()) {
            showToast("请打开蓝牙!");
            return false;
        }

        //当前是否在扫描，如果是就取消当前的扫描，重新扫描
        if (mBluetoothAdapter.isDiscovering()) {
            mBluetoothAdapter.cancelDiscovery();
        }

        //此方法是个异步操作，一般搜索12秒
        return mBluetoothAdapter.startDiscovery();
    }

    /**
     * 取消扫描蓝牙
     *
     * @return true 为取消成功
     */
    public boolean cancelScanBuletooth() {
        if (isSupportBlue()) {
            return mBluetoothAdapter.cancelDiscovery();
        }
        return true;
    }

    /**
     * 设备是否支持蓝牙  true为支持
     *
     * @return
     */
    public boolean isSupportBlue() {
        return mBluetoothAdapter != null;
    }

    /**
     * 蓝牙是否打开   true为打开
     *
     * @return
     */
    public boolean isBlueEnable() {
        return isSupportBlue() && mBluetoothAdapter.isEnabled();
    }

    private void checkBluetoothState() {
        if (!isSupportBlue()) return;
        if (mBluetoothAdapter.isEnabled()) {
            tvState.setText("蓝牙状态： 开");
        } else {
            tvState.setText("蓝牙状态： 关");
        }
    }


    private void initView() {
        tvState = findViewById(R.id.tvState);
        processBar = findViewById(R.id.processbar);
        findViewById(R.id.btnOpen).setOnClickListener(this);
        findViewById(R.id.btnClose).setOnClickListener(this);
        findViewById(R.id.btnCancalSearch).setOnClickListener(this);
        findViewById(R.id.btnSearch).setOnClickListener(this);
    }


    @Override
    public void onClick(View v) {

        switch (v.getId()) {
            case R.id.btnOpen:
                openBluetooth(this, REQUEST_ENABLE_CODE);
                break;
            case R.id.btnClose:
                closeBluetooth();
                break;
            case R.id.btnCancalSearch:
                cancelScanBuletooth();
                break;
            case R.id.btnSearch:
                scanBluetooth();
                break;
        }

    }


    private void showToast(String msg) {
        Toast.makeText(this, msg, Toast.LENGTH_SHORT).show();
    }


    @Override
    protected void onResume() {
        super.onResume();
        checkBluetoothState();
    }


    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if (requestCode == REQUEST_ENABLE_CODE) {
            if (resultCode == RESULT_OK) {
                checkBluetoothState();
            }
            if (resultCode == RESULT_CANCELED) {
                checkBluetoothState();
            }
        } else {
            checkBluetoothState();
        }

    }


    /**
     * 扫描广播接收类
     * Created by zqf on 2018/7/6.
     */

    public class ScanBlueReceiver extends BroadcastReceiver {
        private final String TAG = ScanBlueReceiver.class.getName();
        private ScanBlueCallBack callBack;

        public ScanBlueReceiver(ScanBlueCallBack callBack) {
            this.callBack = callBack;
        }

        //广播接收器，当远程蓝牙设备被发现时，回调函数onReceiver()会被执行
        @Override
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();
            Log.d(TAG, "action:" + action);
            BluetoothDevice device = intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE);
            switch (action) {
                case BluetoothAdapter.ACTION_DISCOVERY_STARTED:
                    Log.d(TAG, "开始扫描...");
                    callBack.onScanStarted();
                    break;
                case BluetoothAdapter.ACTION_DISCOVERY_FINISHED:
                    Log.d(TAG, "结束扫描...");
                    callBack.onScanFinished();
                    break;
                case BluetoothDevice.ACTION_FOUND:
                    Log.d(TAG, "发现设备...");
                    callBack.onScanning(device);
                    break;
            }
        }
    }

    public interface ScanBlueCallBack {

        void onScanStarted();

        void onScanFinished();

        void onScanning(BluetoothDevice device);
    }


}
