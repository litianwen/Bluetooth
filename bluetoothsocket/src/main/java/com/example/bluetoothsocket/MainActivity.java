package com.example.bluetoothsocket;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothServerSocket;
import android.bluetooth.BluetoothSocket;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Handler;
import android.os.Message;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.ListView;
import android.widget.Toast;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.UUID;

public class MainActivity extends AppCompatActivity implements AdapterView.OnItemClickListener {

    private ListView lvDevices;

    private BluetoothAdapter bluetoothAdapter;
    //蓝牙名称、地址
    private List<String > bluetoothDeviceList =new ArrayList<String>();
    //蓝牙适配列表
    private ArrayAdapter<String > arrayAdapter;
    //手动随机输入UUID
    private final UUID MY_UUID=UUID.fromString("8hfs44ie-s2s2-8989-89sd-a2s34f4g33f4");
    private final String NAME="Bluetooth_Socket";

    private BluetoothDevice device;
    private BluetoothSocket clientSocket;

    private AcceptThread acceptThread;
    private OutputStream outputStream;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        //初始化、获取蓝牙
        bluetoothAdapter=BluetoothAdapter.getDefaultAdapter();
        lvDevices= (ListView) findViewById(R.id.lvDevices);
        //配对蓝牙列表
        Set<BluetoothDevice> pairedDevices = bluetoothAdapter.getBondedDevices();
        if (pairedDevices.size() > 0) {
            for (BluetoothDevice device : pairedDevices) {
                bluetoothDeviceList.add(device.getName() + ":" + device.getAddress()+"\n");
            }
        }
        //显示适配器
        arrayAdapter=new ArrayAdapter<String>(this,android.R.layout.simple_list_item_1,android.R.id.text1,bluetoothDeviceList);
        lvDevices.setAdapter(arrayAdapter);
        lvDevices.setOnItemClickListener(this);

        acceptThread=new AcceptThread();
        acceptThread.start();

    }

    private final BroadcastReceiver broadcastReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {

            String action = intent.getAction();
            if (BluetoothDevice.ACTION_FOUND.equals(action)) {
                BluetoothDevice device = intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE);
                if (device.getBondState() != BluetoothDevice.BOND_BONDED) {
                    bluetoothDeviceList.add(device.getName() + ":" + device.getAddress()+"\n");
                    arrayAdapter.notifyDataSetChanged();
                }
            } else if (BluetoothAdapter.ACTION_DISCOVERY_FINISHED.equals(action)) {
                setProgressBarIndeterminateVisibility(false);
                setTitle("连接蓝牙设备");
            }
        }
    };

//    IntentFilter intentFilter = new IntentFilter(BluetoothDevice.ACTION_FOUND);
//    this.registerReceiver(broadcastReceiver, intentFilter);
//
//    intentFilter = new IntentFilter(BluetoothAdapter.ACTION_DISCOVERY_FINISHED);
//    this.registerReceiver(broadcastReceiver, intentFilter);

    @Override
    public void onItemClick(AdapterView<?> parent, View view, int position, long id) {

        String s = arrayAdapter.getItem(position);
        //解析蓝牙地址
        String address=s.substring(s.indexOf(":")+1).trim();

        try {
            if (bluetoothAdapter.isDiscovering()){
                bluetoothAdapter.cancelDiscovery();
            }
            try {
                if (device==null){
                    device=bluetoothAdapter.getRemoteDevice(address);
                }
                if (clientSocket==null){
                    clientSocket=device.createRfcommSocketToServiceRecord(MY_UUID);
                    clientSocket.connect();
                    outputStream=clientSocket.getOutputStream();
                }
            }catch (Exception e){

            }

            if (outputStream!=null){
                outputStream.write("发送信息到其他蓝牙设备".getBytes("utf-8"));
            }
        }catch (Exception e){

        }
    }

    public void onClickDevice(View view){
        setProgressBarIndeterminateVisibility(true);
        setTitle("正在扫描");
        if (bluetoothAdapter.isDiscovering()){
            bluetoothAdapter.cancelDiscovery();
        }
        bluetoothAdapter.startDiscovery();
    }

    private Handler handler=new Handler() {
        @Override
        public void handleMessage(Message msg) {
            Toast.makeText(MainActivity.this, String.valueOf(msg.obj),Toast.LENGTH_LONG).show();
            super.handleMessage(msg);
        }
    };

    private class AcceptThread extends Thread{
        private BluetoothServerSocket serverSocket;
        private BluetoothSocket socket;
        private InputStream inputStream;
        private OutputStream os;

        public AcceptThread() {
            try {
                serverSocket=bluetoothAdapter.listenUsingRfcommWithServiceRecord(NAME,MY_UUID);
            } catch (IOException e) {
                e.printStackTrace();
            }
        }

        @Override
        public void run() {
            try {
                socket=serverSocket.accept();
                inputStream=socket.getInputStream();
                os=socket.getOutputStream();
                while (true){
                    byte[] bytes=new byte[128];
                    int count =inputStream.read(bytes);
                    Message message=new Message();
                    message.obj=new String(bytes,0,count,"utf-8");
                    handler.handleMessage(message);
                }
            } catch (IOException e) {
                e.printStackTrace();
            }

        }
    }
}
