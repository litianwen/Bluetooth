package com.example.bluetoothwork;

import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothGatt;
import android.bluetooth.BluetoothGattCallback;
import android.bluetooth.BluetoothGattCharacteristic;
import android.bluetooth.BluetoothGattDescriptor;
import android.bluetooth.BluetoothGattService;
import android.content.Intent;
import android.os.Handler;
import android.os.Message;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.View;

import java.util.List;

/**蓝牙4.0演示
 */
public class MainActivity extends AppCompatActivity {

    private static final int REQUEST_DEVICE = 1000;//设备选择页面的请求码
    BluetoothDevice device;//蓝牙设备对象
    BluetoothGatt gatt;//低耗能蓝牙设备对象
    BluetoothGattCharacteristic writer;//耗能设备的输入的特征值

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        startActivityForResult(new Intent(MainActivity.this, DevicesActivity.class), REQUEST_DEVICE);
    }

    /**
     * 测试
     */
    public void test(View v) {
        //发送数据
        // byte[] buf = getBuf((byte) 0x30);
        // byte[] buf = {0x55, (byte) 0xaa, 0x30, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0
        byte[] buf = new byte[17];
        buf[0] = 0x55;
        buf[1] = (byte) 0xaa;
        buf[2] = 0x30;
        writer.setValue(buf);
        gatt.writeCharacteristic(writer);
    }

    /**
     * 改变颜色
     */
    public void rgb(View v) {
        int r = (int) (Math.random() * 255);
        int g = (int) (Math.random() * 255);
        int b = (int) (Math.random() * 255);
        byte[] buf = getBuf(new byte[]{0x07, (byte) r, (byte) g, (byte) b, 0x00, (byte) 0x80});
        writer.setValue(buf);
        gatt.writeCharacteristic(writer);
    }

    /**
     * 循环改变颜色
     */
    public void loop(View v) {
        handler.sendEmptyMessageDelayed(1, 1000);
    }

    /**
     * 取消循环改变颜色
     */
    public void stopLoop(View v) {
        handler.removeMessages(1);
    }

    Handler handler = new Handler() {
        @Override
        public void handleMessage(Message msg) {
            super.handleMessage(msg);
            changeLoop();
            handler.sendEmptyMessageDelayed(1, 1000);
        }
    };

    private void changeLoop() {
        int r = (int) (Math.random() * 255);
        int g = (int) (Math.random() * 255);
        int b = (int) (Math.random() * 255);
        byte[] buf = getBuf(new byte[]{0x07, (byte) r, (byte) g, (byte) b, 0x00, (byte) 0x80});
        writer.setValue(buf);
        gatt.writeCharacteristic(writer);
    }

    /**
     * 蓝牙设备连接后的回调方法
     */
    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == REQUEST_DEVICE && resultCode == RESULT_OK) {
            device = data.getParcelableExtra("device");
            if (device != null)
                conn();//连接这个低耗能蓝牙设备
        } else {

            finish();
        }
    }

    /**
     * 连接低耗能蓝牙设备的方法
     */
    private void conn() {
        //BLE连接过程， 这里需要最低版本是API是18， 也就是android4.3以后的手机
        //第一个参数上下文
        //第二个参数是否自动连接
        //第三个参数连接后的回调方法
        gatt = device.connectGatt(this, false, mGattCallback);
        //连接
        gatt.connect();
    }

    /**
     * 连接蓝牙时， 连接的回调接口
     */
    BluetoothGattCallback mGattCallback = new BluetoothGattCallback() {
        //选择部分需要的回调方法
        @Override
        public void onConnectionStateChange(BluetoothGatt gatt, int status, int newState) {
            super.onConnectionStateChange(gatt, status, newState);
            Log.e("TAG", "-------------------->>第一步");
            Log.e("TAG", "----------->onConnectionStateChange");
            //获取服务 获取设备电量 获取设备名称
            gatt.discoverServices();
        }

        @Override
        public void onServicesDiscovered(BluetoothGatt gatt, int status) {
            super.onServicesDiscovered(gatt, status);
            //获取低耗能蓝牙设备对象
            List<BluetoothGattService> services = gatt.getServices();
            for (BluetoothGattService service : services) {
                Log.e("TAG", "---------------" + service.getUuid());
                for (BluetoothGattCharacteristic bluetoothGattCharacteristic : service.getCharacteristics()) {
                    Log.e("TAG", "------------->>>" + bluetoothGattCharacteristic.getUuid());
                }
            }
            Log.e("TAG", "----------->>onServicesDiscovered");
            //获得需要的数据
            //这里是设备对象的第三个服务的第一个特征值， 用来数据的设置
            writer = services.get(2).getCharacteristics().get(0);
            //这里是设备对象的第三个服务的第二个特征值， 用来数据的读取
            BluetoothGattCharacteristic reader = gatt.getServices().get(2).getCharacteristics().get(1);
            //打开读取开关
            Log.e("TAG", "----------->>获取到特征值");
            for (BluetoothGattDescriptor descriptor : reader.getDescriptors()) {
                descriptor.setValue(BluetoothGattDescriptor.ENABLE_NOTIFICATION_VALUE);
                gatt.writeDescriptor(descriptor);
            }
            gatt.setCharacteristicNotification(reader, true);
        }
    };

    /**
     * 设置， 获取协议数据
     */
    public byte[] getBuf(byte... buf) {
        byte[] bufs = new byte[17];
        //两位协议头
        bufs[0] = 0x55;
        bufs[1] = (byte) 0xaa;
        //协议命令共14个,不足补0
        if (buf != null && buf.length > 0) {
            for (int i = 0; i < buf.length; i++) {
                bufs[2 + i] = buf[i];//从第三个开始设置
            }
        }
        return bufs;
    }


}
