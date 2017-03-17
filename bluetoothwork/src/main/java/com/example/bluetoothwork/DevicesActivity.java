package com.example.bluetoothwork;

import android.Manifest;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.v7.app.AppCompatActivity;
import android.text.TextUtils;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.ListView;
import android.widget.Toast;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by Administrator on 2017/3/15.
 */

public class DevicesActivity extends AppCompatActivity implements AdapterView.OnItemClickListener {

    private static final int REQUEST_ENABLE = 1001;//请求打开蓝牙的请求码
    private static final int REQUEST_LOCATION = 1002;//请求扫描显示设备的请求码

    BluetoothAdapter bluetoothAdapter;//蓝牙管理器,蓝牙设备的管理类
    ArrayAdapter<String> adapter;//列表的适配器

    //列表的对象,BlueToothDevice蓝牙设备对象，里面包含蓝牙的数据
    List<BluetoothDevice> devices = new ArrayList();
    List<String> deviceNames = new ArrayList<>();//列表对象的数据

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        ListView listView=new ListView(this);
        setContentView(listView);//显示列表视图

        adapter=new ArrayAdapter<String>(this,android.R.layout.simple_list_item_1,deviceNames);
        //给列表设置适配器
        listView.setAdapter(adapter);
        requestPer();
        listView.setOnItemClickListener(this);
    }

    /**
     * 请求权限
     */
    private void requestPer() {

        if (Build.VERSION.SDK_INT >= 23) {
            int check = checkSelfPermission(Manifest.permission.ACCESS_COARSE_LOCATION);
            if (check != PackageManager.PERMISSION_GRANTED) {
                requestPermissions(new String[]{Manifest.permission.ACCESS_COARSE_LOCATION}, REQUEST_LOCATION);
            } else {
                //有了权限， 就扫描蓝牙设备
                checkBlue();
            }
        } else {
            //版本低于6.0
            checkBlue();
        }
    }
    /**
     * 请求权限后的回调方法
     */

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode==REQUEST_LOCATION && grantResults[0]==PackageManager.PERMISSION_GRANTED){
            //扫描蓝牙成功
            checkBlue();
        }else {
            setResult(RESULT_CANCELED);
            finish();
        }
    }

    /**
     *** 重写onResume方法， 进行广播接收者的注册
    */
    @Override
    protected void onResume() {
        super.onResume();
        registerReceiver(receiver, new IntentFilter(BluetoothDevice.ACTION_FOUND));
    }
    /*** 重写生命周期的onPause方法， 进行广播接收者的注销
    */
    @Override
    protected void onPause() {
        super.onPause();
        unregisterReceiver(receiver);
    }
    /*** 创建广播接收者
    *获取设备广播
     */
    private BroadcastReceiver receiver = new BroadcastReceiver() {
        //收到广播后的回调方法
        @Override
        public void onReceive(Context context, Intent intent) {
            //获取设备
            BluetoothDevice device = intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE);
            devices.add(device);//添加蓝牙设备对象
            //添加蓝牙设备名称
            deviceNames.add(TextUtils.isEmpty(device.getName()) ? "未命名" : device.getName());
            Toast.makeText(DevicesActivity.this,"",Toast.LENGTH_SHORT).show();
            adapter.notifyDataSetChanged();//刷新适配器
        }
    };
    /**
    *蓝牙设备的扫描操作， 并显示在视图列表中
     */
    private void checkBlue() {

        bluetoothAdapter=BluetoothAdapter.getDefaultAdapter();
        if (bluetoothAdapter.isEnabled()){//蓝牙已经就绪， 可以进行扫描
            deviceNames.clear();
            devices.clear();
            adapter.notifyDataSetChanged();
            //开始扫描设置， 扫描完成后会系统自动发送广播
            bluetoothAdapter.startDiscovery();
        }else {
            //蓝牙还没有就绪， 要先打开蓝牙
            openBlue();
        }
    }

    /**
     * 打来蓝牙设备
     */
    private void openBlue() {

        startActivityForResult(new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE),REQUEST_ENABLE);
    }
    /** 请求打开蓝牙后返回的回调方法方法
    * 进行扫描蓝牙设备操作
    */
    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        checkBlue();
    }

    /**
     * 点击列表视图的回调方法
     * 这里只能选择固定产品的设备， 选择其他的就提示重选
     */
    @Override
    public void onItemClick(AdapterView<?> parent, View view, int position, long id) {

        BluetoothDevice device = devices.get(position);
        //KQX
        if (device.getName().startsWith("KQX")) {
            //传递数据， 这个对象已经序列化过了
            setResult(RESULT_OK, getIntent().putExtra("device", device));
            finish();
        } else {
            Toast.makeText(this, "请选择卡丘熊蓝牙灯", Toast.LENGTH_SHORT).show();
        }
    }
}
