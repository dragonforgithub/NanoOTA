package com.lxl.nanosic.app.ble;

import android.app.Service;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothGatt;
import android.bluetooth.BluetoothGattCallback;
import android.bluetooth.BluetoothGattCharacteristic;
import android.bluetooth.BluetoothGattDescriptor;
import android.bluetooth.BluetoothGattService;
import android.bluetooth.BluetoothManager;
import android.bluetooth.BluetoothProfile;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Binder;
import android.os.Handler;
import android.os.IBinder;
import android.os.Looper;
import android.os.Message;
import android.util.Log;
import android.view.Gravity;
import android.widget.Toast;

import com.lxl.nanosic.app.L;
import com.lxl.nanosic.app.R;
import com.lxl.nanosic.app.UpgradeActivity;
import com.lxl.nanosic.app.Utils;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.Arrays;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.Timer;
import java.util.TimerTask;
import java.util.UUID;

public class BluetoothLeService extends Service {
    private static BluetoothLeService mThis = null;
    //HID协议
    private static String BUZZER_SERVICE = "00001802-0000-1000-8000-00805f9b34fb";
    private static String BUZZER_CHARACTER = "00002a06-0000-1000-8000-00805f9b34fb";
    private final static UUID UUID_BUZZER_SERVICE = UUID.fromString(BUZZER_SERVICE);
    private final static UUID UUID_BUZZER_CHARACTER = UUID.fromString(BUZZER_CHARACTER);
    private static String HID_SERVICE = "00001812-0000-1000-8000-00805f9b34fb";
    private final static UUID UUID_HID_SERVICE = UUID.fromString(HID_SERVICE);

    //GATT协议
    /*
    00001800-0000-1000-8000-00805f9b34fb
    00001801-0000-1000-8000-00805f9b34fb
    0000180a-0000-1000-8000-00805f9b34fb
    ab5e0001-5a21-4f05-bc7d-af01f617b664
    0000180f-0000-1000-8000-00805f9b34fb
    00001812-0000-1000-8000-00805f9b34fb
    ab5effd1-5a21-4f05-bc7d-af01f617b664

    gattCharacteristic --->:ab5effd3-5a21-4f05-bc7d-af01f617b664
    gattCharacteristic --->:ab5effd2-5a21-4f05-bc7d-af01f617b664
    */
    private static String GATT_SERVICE      = "ab5effd1-5a21-4f05-bc7d-af01f617b664";
    private static String GATT_SERVICE_IN   = "ab5effd2-5a21-4f05-bc7d-af01f617b664";
    private static String GATT_SERVICE_OUT  = "ab5effd3-5a21-4f05-bc7d-af01f617b664";
    private final static UUID UUID_GATT_SERVICE     = UUID.fromString(GATT_SERVICE);
    private final static UUID UUID_GATT_SERVICE_IN  = UUID.fromString(GATT_SERVICE_IN);
    private final static UUID UUID_GATT_SERVICE_OUT = UUID.fromString(GATT_SERVICE_OUT);

    public IntentFilter mFilter;
    // OtaFile RemoteOta ;
    // 标记BLE设备是否忙
    private boolean BleIsBusy;
    // BLE 接收到数据缓存
    private byte[] BleRecBuff ;
    // BLE接收到一个包
    private boolean fBleRecVendorPacketOk;
    // BLE发送包成功
    private boolean fBleSendVendorPacketOk;

    // 升级线程
    private Thread mUpgradeThread = null;
    private boolean isUpgradeWorking = false;

    private Thread mNotificationThread = null;

    private byte RemoteVid ,RemotePid ;      // 遥控器VID,PID
    private byte RemoteVerH , RemoteVerL ;   // 遥控器版本号高低字节
    private int RemoteBattery;               // 遥控器电量
    private boolean fEnableNotifyThreadIsRun  = false;  // 线程是否运行
    private boolean fRemoteUpgradeThreadIsRun = false;  // 线程是否运行
    private boolean fRemoteReadVerThreadIsRun = false;  // 线程是否运行

    UpgradeFile  mUpgradeFile;
    private int haveSendBinPacketNum,needSendBinPacketNum;

    SpeedControl mSpeedControl;
    Encryption mEncryption;

    private boolean enableDisplayToast = false;

    private int checkBleCount;       // 检测次数
    private boolean fCanAutoUpgrade; // 检测是否可以升级
    private boolean hasRcvUpgradeBroadcast; // 是否收到平台OTA广播包
    private boolean isUiStarted;     // UI是否已经启动

    public BluetoothLeService() {
        mThis = this;
    }

    private final IBinder binder = new LocalBinder();
    public class LocalBinder extends Binder {
        public BluetoothLeService getService() {
            return BluetoothLeService.this;
        }
    }

    @Override
    public void onCreate() {
        L.i("Bluetooth Le Service onCreate.");
        BroadcastAction.sendBroadcast(mThis,BroadcastAction.BROADCAST_SERVICE_SEND_ACTION_SERVICE,
        BroadcastAction.BROADCAST_CONTENT_SERVICE_ON_CREATED);

        BleIsBusy = false;
        fBleRecVendorPacketOk = false;
        fBleSendVendorPacketOk = false;
        BleRecBuff = new byte[22];
        fEnableNotifyThreadIsRun  = false;
        fRemoteUpgradeThreadIsRun = false;
        fRemoteReadVerThreadIsRun = false;
        fCanAutoUpgrade = false;
        haveSendBinPacketNum = 0x00;
        needSendBinPacketNum = 0x00;

        checkBleCount  = 0;
        hasRcvUpgradeBroadcast = false;
        isUiStarted=false;

        RegisterBroadcastReceiver();
        mUpgradeFile = new UpgradeFile(this,null);
        mSpeedControl = new SpeedControl();
        mEncryption = new Encryption();

        if(mEncryption.getEncryptionEnable()){
            L.i("Encrypted data transmission");
        }else{
            L.i("Unencrypted data transmission");
        }
    }

    @Override
    public IBinder onBind(Intent intent) {
        L.i("Bluetooth Le Service onBind");

        return binder;
    }

    @Override
    public boolean onUnbind(Intent intent) {
        // After using a given device, you should make sure that
        // BluetoothGatt.close() is called
//        close();
//        BluetoothLeBroadcast(BLUETOOTH_SERVICE_BROADCAST, BROADCAST_DESCRIBE_SERVICES_STOP, "UnBind");
        return super.onUnbind(intent);
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        // L.i(TAG, "Received start id " + startId + ": " + intent);
        // We want this service to continue running until it is explicitly
        // stopped, so return sticky.
        return START_STICKY;
    }

    @Override
    public void onDestroy() {
        // this service is no longer used and is being destroyed
        L.i("------Service onDestroy------");
        StopServiceTimer();
        unRegisterBroadcastReceiver();
        close();
        super.onDestroy();
    }

    //==============================================================================================
    // Bluetooth
    //==============================================================================================
    private BluetoothManager mBluetoothManager = null;
    private BluetoothAdapter mBluetoothAdapter = null;
    private BluetoothGatt mBluetoothGatt = null;
    private BluetoothDevice mBluetoothDevice = null ;
    public BluetoothGattCharacteristic VendorOut_Characteristic = null;
    public BluetoothGattCharacteristic VendorIn_Characteristic = null;
    public BluetoothGattCharacteristic Buzzer_Characteristic = null;
    public BluetoothGattCharacteristic OtaIn_Characteristic = null;

    // 重置蓝牙设备状态
    private void ResetBluetoothValue() {
        mBluetoothManager = null;
        mBluetoothAdapter = null;
        mBluetoothGatt = null;
        mBluetoothDevice = null;
        VendorOut_Characteristic = null;
        VendorIn_Characteristic = null;
        OtaIn_Characteristic = null;
        Buzzer_Characteristic = null;
        BleIsBusy = false;
        fCanAutoUpgrade = false;
    }

    private void SendRemoteTotalInfo(){
        if((mBluetoothGatt != null) && (mBluetoothDevice != null)
                && (VendorOut_Characteristic != null)) {

            L.d("===Send BLE name and address broadcast");
            BroadcastAction.sendBroadcast(mThis, BroadcastAction.BROADCAST_SERVICE_SEND_ACTION_BLUETOOTH,
                    BroadcastAction.ROADCAST_CONTENT_BLUETOOTH_GATT_CONNECTED,
                    mBluetoothDevice.getAddress());

            BroadcastAction.sendBroadcast(mThis, BroadcastAction.BROADCAST_SERVICE_SEND_ACTION_BLUETOOTH,
                    BroadcastAction.ROADCAST_CONTENT_BLUETOOTH_GATT_DISCOVERED,
                    mBluetoothDevice.getName());
        }

        if(mUpgradeFile != null) {
            mUpgradeFile.SendFileTotalInfo();
        }else{
            L.e("No upgrade file!");
        }
    }
    /**
     * Initializes a reference to the local Bluetooth adapter.
     *
     * @return Return true if the initialization is successful.
     */
    public boolean initialize(String name, String addr) {
        L.d("initialize ble,name:" + name + " addr:" + addr);
        if (mBluetoothManager != null && name==null && addr==null) {
            L.w("The BluetoothManager is not null!");
            SendRemoteTotalInfo();
            return true;
        }

        // 重置蓝牙相关变量
        ResetBluetoothValue();

        if (mBluetoothManager == null) {
            mBluetoothManager = (BluetoothManager) getSystemService(Context.BLUETOOTH_SERVICE);
            if (mBluetoothManager == null) {
                L.e("Unable to initialize BluetoothManager.");
                return false;
            }
        }

        mBluetoothAdapter = mBluetoothManager.getAdapter();
        if (mBluetoothAdapter == null) {
            L.e("Unable to obtain a BluetoothAdapter.");
            return false;
        }

        Set<BluetoothDevice> devices = mBluetoothAdapter.getBondedDevices();
        L.i("bonded device size:" + devices.size());

        boolean findDev=false;
        if(devices.size()>0){
            for(Iterator<BluetoothDevice> it = devices.iterator(); it.hasNext();) {
                BluetoothDevice device = it.next();
                String bluetoothDeviceName = device.getName();
                String bluetoothDeviceAddress = device.getAddress();
                L.i("Find Remote:" + bluetoothDeviceName);
                if(((name == null) && (addr == null))
                        || (bluetoothDeviceName.equals(name) && (addr == null))
                        || (bluetoothDeviceAddress.equals(addr) && (name == null)))
                {
                    // 对当前绑定过的设备遍历进行GATT连接
                    BluetoothGatt devGatt = device.connectGatt(this, false, mGattCallbacks);
                    L.i("connectGatt : " + devGatt.getDevice().getAddress());
                    findDev = true;
                }
            }
        }
        return findDev;
    }

    /**
     * After using a given BLE device, the app must call this method to ensure
     * resources are released properly.
     */
    public void close() {
        if (mBluetoothGatt != null) {
            mBluetoothGatt.close();
            ResetBluetoothValue();
        }
    }
    //==============================================================================================
    /**
     *  GATT client callbacks
     *  mBluetoothGatt = device.connectGatt(this, true, mGattCallback);
     */
    private BluetoothGattCallback mGattCallbacks = new BluetoothGattCallback() {
        @Override
        public void onConnectionStateChange(BluetoothGatt gatt, int status, int newState) {
            try {
                switch (newState) {
                    case BluetoothProfile.STATE_CONNECTING:
                        L.d("connection state: CONNECTING...");
                        break;

                    case BluetoothProfile.STATE_CONNECTED:
                        if (gatt != null) {
                            // TODO:只记录检测到的第一个连接设备，尝试过设备列表切换，但是重新通过mac地址连接无回调
                            if(mBluetoothGatt == null || mBluetoothDevice == null){
                                mBluetoothGatt = gatt;
                                mBluetoothDevice = gatt.getDevice();
                                mBluetoothGatt.discoverServices();

                                // 发送连接广播
                                BroadcastAction.sendBroadcast(mThis,BroadcastAction.BROADCAST_SERVICE_SEND_ACTION_BLUETOOTH,
                                        BroadcastAction.ROADCAST_CONTENT_BLUETOOTH_GATT_CONNECTED,
                                        gatt.getDevice().getAddress());

                                // 发广播给主UI更新设备名
                                BroadcastAction.sendBroadcast(mThis,BroadcastAction.BROADCAST_CONTENT_DEV_INFO,
                                        gatt.getDevice().getName(), gatt.getDevice().getAddress());

                            }
                            L.w("onConnectionStateChange (" + gatt.getDevice().getAddress() + ") "
                                    + newState + " status: " + status);
                        } else{
                            L.e("device connected,but gatt is empty.");
                        }
                        break;

                    case BluetoothProfile.STATE_DISCONNECTING:
                        L.d("connection state: DISCONNECTING...");
                        break;

                    case BluetoothProfile.STATE_DISCONNECTED:

                        String devAddress = gatt.getDevice().getAddress();
                        if(devAddress.equals(mBluetoothDevice.getAddress())){
                            // 重置蓝牙状态
                            ResetBluetoothValue();
                            // 发送断开广播
                            BroadcastAction.sendBroadcast(mThis,BroadcastAction.BROADCAST_SERVICE_SEND_ACTION_BLUETOOTH,
                                    BroadcastAction.ROADCAST_CONTENT_BLUETOOTH_GATT_DISCONNECTED,
                                    gatt.getDevice().getAddress());

                            // 发广播给主UI更新设备名
                            /*
                            BroadcastAction.sendBroadcast(mThis,BroadcastAction.BROADCAST_CONTENT_DEV_INFO,
                                    getResources().getString(R.string.upgrade_RC_disconnected));
                            */

                        }

                        L.w("Ble disconnected : " + devAddress);
                        gatt.close();
                        break;

                    default:
                        L.w( "New state not processed: " + newState);
                        break;
                }
            } catch (NullPointerException e) {
                e.printStackTrace();
            }
        }

        @Override
        public void onServicesDiscovered(BluetoothGatt gatt, int status) {
            if (status == gatt.GATT_SUCCESS) {

                String curProtocol = null;
                // 判断当前服务的协议类型
                List<BluetoothGattService> list = gatt.getServices();
                for (BluetoothGattService bluetoothGattService:list){
                    String serviceUUID_Str = bluetoothGattService.getUuid().toString();
                    L.d("serviceUUID ---> " + serviceUUID_Str);

                    if(serviceUUID_Str.equalsIgnoreCase(HID_SERVICE)){
                        curProtocol = "hid";
                    }else if(serviceUUID_Str.equalsIgnoreCase(GATT_SERVICE)){
                        curProtocol = "gatt";
                    }
                }

                /** HID 协议 */
                if (curProtocol.equals("hid"))
                {
                    BluetoothGattService HidService = gatt.getService(UUID_HID_SERVICE);
                    L.i("Discovered hid service:" + HidService);
                    if (HidService != null) {
                        int CharacterSize = HidService.getCharacteristics().size();
                        L.i("Characteristic size:" + CharacterSize);
                        if(HidService.getCharacteristics().size() >=  6)
                        {
                            BroadcastAction.sendBroadcast(mThis,BroadcastAction.BROADCAST_SERVICE_SEND_ACTION_BLUETOOTH,
                                    BroadcastAction.ROADCAST_CONTENT_BLUETOOTH_GATT_DISCOVERED,
                                    mBluetoothDevice.getName());
                            List<BluetoothGattCharacteristic> HidCharList = HidService.getCharacteristics();

                            // default hid characteristic
                            OtaIn_Characteristic = HidCharList.get(CharacterSize - 1);
                            VendorOut_Characteristic = HidCharList.get(CharacterSize - 2);
                            VendorIn_Characteristic = HidCharList.get(CharacterSize - 3);

                            L.i("Ota In Characteristic:" + OtaIn_Characteristic.getUuid().toString());
                            L.i("Ota In Descriptors:" + OtaIn_Characteristic.getDescriptors().size());

                            L.i("Vendor Out Characteristic:" + VendorOut_Characteristic.getUuid().toString());
                            L.i("Vendor Out Descriptors:" + VendorOut_Characteristic.getDescriptors().size());

                            L.i("Vendor In Characteristic:" + VendorIn_Characteristic.getUuid().toString());
                            L.i("Vendor In Descriptors:" + VendorIn_Characteristic.getDescriptors().size());

                            //设备连接时读取版本和电量
                            EnableAllNotificationThread();

                            int write_type,err_num;
                            err_num = 0;
                            write_type = VendorOut_Characteristic.getWriteType();
                            if(write_type !=  1){
                                err_num ++;
                                L.e("Err: VendorOut_Characteristic write type Err.");
                            }
                            write_type = VendorIn_Characteristic.getWriteType();
                            if(write_type !=  2){
                                err_num ++;
                                L.e("Err: VendorIn_Characteristic write type Err.");
                            }
                            write_type = OtaIn_Characteristic.getWriteType();
                            if(write_type !=  2){
                                err_num ++;
                                L.e("Err: OtaIn_Characteristic write type Err.");
                            }
                            if(err_num > 0){
                                L.e("Not input characteristic.");
                            }
                        }else{
                            L.i("The number of Characteristic is too small.");
                        }
                    } else {
                        L.i("BluetoothGattService is null");
                    }

                    // TODO:蜂鸣器服务？要不要？
                    BluetoothGattService BuzzerService = gatt.getService(UUID_BUZZER_SERVICE);
                    if (BuzzerService != null) {
                        Buzzer_Characteristic = BuzzerService.getCharacteristic(UUID_BUZZER_CHARACTER);
                    }
                }
                /** GATT 协议 */
                else if (curProtocol.equals("gatt"))
                {
                    BluetoothGattService GattService = gatt.getService(UUID_GATT_SERVICE);
                    L.i("Discovered gatt service:" + GattService.getUuid().toString());
                    if (GattService != null) {
                        int CharacterSize = GattService.getCharacteristics().size();
                        L.i("Characteristic size:" + CharacterSize);
                        if(CharacterSize >= 2)
                        {
                            BroadcastAction.sendBroadcast(mThis,BroadcastAction.BROADCAST_SERVICE_SEND_ACTION_BLUETOOTH,
                                    BroadcastAction.ROADCAST_CONTENT_BLUETOOTH_GATT_DISCOVERED,
                                    mBluetoothDevice.getName());

                            List<BluetoothGattCharacteristic> GattCharList = GattService.getCharacteristics();
                            for (BluetoothGattCharacteristic gattCharacteristic : GattCharList) {
                                L.i("gattCharacteristic --->:" + gattCharacteristic.getUuid().toString());
                            }

                            // default gatt characteristic
                            VendorIn_Characteristic = GattService.getCharacteristic(UUID_GATT_SERVICE_IN);
                            VendorOut_Characteristic =  GattService.getCharacteristic(UUID_GATT_SERVICE_OUT);//GattCharList.get();

                            L.i("Vendor In Characteristic:" + VendorIn_Characteristic.getUuid().toString());
                            L.i("Vendor In Descriptors:" + VendorIn_Characteristic.getDescriptors().size());

                            L.i("Vendor Out Characteristic:" + VendorOut_Characteristic.getUuid().toString());
                            L.i("Vendor Out Descriptors:" + VendorOut_Characteristic.getDescriptors().size());

                            //设备连接时读取版本和电量
                            EnableAllNotificationThread();

                            int write_type,err_num;
                            err_num = 0;
                            write_type = VendorOut_Characteristic.getWriteType();
                            if(write_type !=  1){
                                err_num ++;
                                L.e("Err: VendorOut_Characteristic write type Err -> "+write_type);
                            }
                            write_type = VendorIn_Characteristic.getWriteType();
                            if(write_type !=  2){
                                err_num ++;
                                L.e("Err: VendorIn_Characteristic write type Err -> "+write_type);
                            }
                            if(err_num > 0){
                                L.e("Not input characteristic.");
                            }
                        }else{
                            L.i("The number of Characteristic is too small.");
                        }
                    } else {
                        L.i("BluetoothGattService is null");
                    }
                }
                else{
                    L.e("Invalid device!" );
                    String mainText = getResources().getString(R.string.Text_view_error_code_title);
                    String subText = getResources().getString(R.string.Toast_view_not_support_ota);
                    Utils.ToastShow(mThis, Toast.LENGTH_LONG, Gravity.TOP, mainText, subText);
                }
            } else {
                L.i("onServicesDiscovered received: " + status);
            }
        }

        @Override
        public void onCharacteristicChanged(BluetoothGatt gatt,
                                            BluetoothGattCharacteristic characteristic) {

            if(characteristic.getUuid().toString().equals("00002a4d-0000-1000-8000-00805f9b34fb") //HID
                    || characteristic.getUuid().toString().equals("ab5effd2-5a21-4f05-bc7d-af01f617b664")){ //GATT
                if(characteristic.getValue().length == 8){  // key input
                    KeyReceivedCallback(characteristic.getValue());
                } else {  // vendor input
                    if(characteristic.getValue().length <= BleRecBuff.length) {
                        // 获取遥控器返回的数据
                        System.arraycopy(characteristic.getValue(), 0, BleRecBuff, 0, characteristic.getValue().length);
                        fBleRecVendorPacketOk = true;
                    }else{
                        L.e("Reception length error.");
                    }
                }
                L.i("R:" + GetByteString(characteristic.getValue(),5));
            }
            super.onCharacteristicChanged(gatt, characteristic);
        }

        @Override
        public void onCharacteristicRead(BluetoothGatt gatt,
                                         BluetoothGattCharacteristic characteristic, int status) {
            super.onCharacteristicRead(gatt, characteristic,status);

            if(characteristic.getUuid().toString().equals("00002a19-0000-1000-8000-00805f9b34fb")){
            }
        }

        @Override
        public void onCharacteristicWrite(BluetoothGatt gatt,
                                          BluetoothGattCharacteristic characteristic, int status) {
            super.onCharacteristicWrite(gatt, characteristic, status);
            if(characteristic.getUuid().toString().equals("00002a4d-0000-1000-8000-00805f9b34fb") //HID
                    || characteristic.getUuid().toString().equals("ab5effd3-5a21-4f05-bc7d-af01f617b664")){ //GATT
                fBleSendVendorPacketOk = true;
                L.i("--");
            }
        }

        @Override
        public void onDescriptorRead(BluetoothGatt gatt,
                                     BluetoothGattDescriptor descriptor, int status) {
            BleIsBusy = false;
//            TmpDesBuf = descriptor.getValue();
        }

        @Override
        public void onDescriptorWrite(BluetoothGatt gatt,
                                      BluetoothGattDescriptor descriptor, int status) {
            L.i("onDescriptorWrite: " + descriptor.getUuid().toString());
            BleIsBusy = false;
        }

        @Override
        public void onReadRemoteRssi(BluetoothGatt gatt, int rssi, int status) {
            super.onReadRemoteRssi(gatt,rssi,status);
        }
    };


    //==============================================================================================
    private boolean checkGatt() {
        if (mBluetoothAdapter == null) {
            return false;
        }
        if (mBluetoothGatt == null) {
            return false;
        }
        if (BleIsBusy) {
            L.e("LeService busy");
            return false;
        }
        return true;
    }
    /**
     * Enables or disables notification on a give characteristic.
     *
     * @param characteristic
     *          Characteristic to act on.
     * @param
     *           enable
     *          If true, enable notification. False otherwise.
     */
    private boolean setCharacteristicNotification(
            BluetoothGattCharacteristic characteristic,
            BluetoothGattDescriptor clientConfig, boolean enable) {
        if (!checkGatt())
            return false;
        boolean ok = false;
        L.i("set Characteristic Notification:" + characteristic.getUuid().toString());
        if (mBluetoothGatt.setCharacteristicNotification(characteristic, enable)) {
            if (clientConfig != null) {
                if (enable) {
                    ok = clientConfig
                            .setValue(BluetoothGattDescriptor.ENABLE_NOTIFICATION_VALUE);
                } else {
                    ok = clientConfig
                            .setValue(BluetoothGattDescriptor.DISABLE_NOTIFICATION_VALUE);
                }
                if (ok) {
                    BleIsBusy = true;
                    ok = mBluetoothGatt.writeDescriptor(clientConfig);
                }
            }
        }
        return ok;
    }

    private boolean writeCharacteristic(BluetoothGattCharacteristic characteristic) {
        if (!checkGatt())
            return false;

        BleIsBusy = true;
        return mBluetoothGatt.writeCharacteristic(characteristic);
    }

    private boolean WriteVendorOutCharacter(byte buf[]){
        BleIsBusy = false;
        if(VendorOut_Characteristic != null) {
            try {
                VendorOut_Characteristic.setValue(buf);
                writeCharacteristic(VendorOut_Characteristic);
            }catch (Exception e){
                L.e("Err:write vendor err.");
            }
            return true;
        }else return false;
    }

    public String GetByteString(byte buf[], int len){
        String tmp = "";
        if(len > buf.length){
            len = buf.length;
        }
        for (int i = 0; i < len; i++) {
            tmp += String.format("%02x,", buf[i]);
        }
        return tmp;
    }

    private boolean OperationWait(int ms) {
        try{
            Thread.sleep(ms);
        }catch(InterruptedException e){
            e.printStackTrace();
        }
        return true;
    }

    //==============================================================================================
    private void RegisterBroadcastReceiver(){
        //--------------------------------------------------------------------------
        // Register the BroadcastReceiver
        mFilter = new IntentFilter(BroadcastAction.BROADCAST_SERVICE_REC_ACTION_GENERAL);
        mFilter.addAction(BroadcastAction.BROADCAST_SERVICE_REC_ACTION_SERVICE);
        mFilter.addAction(BroadcastAction.BROADCAST_SERVICE_REC_ACTION_REMOTE_UPGRADE);
        mFilter.addAction(BroadcastAction.BROADCAST_SERVICE_REC_ACTION_BLUETOOTH);
        mFilter.addAction(BroadcastAction.BROADCAST_SERVICE_REC_ACTION_FILE_OPERATION);
        mFilter.addAction(BroadcastAction.ROADCAST_CONTENT_BLUETOOTH_DEV_LIST);
        registerReceiver(BluetoothLeReceiver, mFilter);
    }

    private void unRegisterBroadcastReceiver(){
        if(BluetoothLeReceiver != null) {
            try {
                unregisterReceiver(BluetoothLeReceiver);
                L.i("bluetoothLeService unregisterReceiver");
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }

    private BroadcastReceiver BluetoothLeReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            int ibroad_value = 0x00;
            String sbroad_value = null, sbroad_aux_val = null,stmpdis;
            final String action = intent.getAction();
            String value_type = intent.getStringExtra(BroadcastAction.BROADCAST_VALUE_TYPE);
            if (value_type.equals(BroadcastAction.BROADCAST_VALUE_STRING)) {
                sbroad_value = intent.getStringExtra(BroadcastAction.BROADCAST_VALUE_CONTENT_STRING);
            } else if (value_type.equals(BroadcastAction.BROADCAST_VALUE_INT)) {
                ibroad_value = intent.getIntExtra(BroadcastAction.BROADCAST_VALUE_CONTENT_INT, 0);
            } else if (value_type.equals(BroadcastAction.BROADCAST_VALUE_STRING_INT)) {
                sbroad_value = intent.getStringExtra(BroadcastAction.BROADCAST_VALUE_CONTENT_STRING);
                ibroad_value = intent.getIntExtra(BroadcastAction.BROADCAST_VALUE_CONTENT_INT, 0);
            } else if (value_type.equals(BroadcastAction.BROADCAST_VALUE_STRING_STRING)) {
                sbroad_value = intent.getStringExtra(BroadcastAction.BROADCAST_VALUE_CONTENT_STRING);
                sbroad_aux_val = intent.getStringExtra(BroadcastAction.BROADCAST_VALUE_CONTENT_STRING_AUX);
            }

            if (BroadcastAction.BROADCAST_SERVICE_REC_ACTION_GENERAL.equals(action)) {
                // 普通的广播
            }else if (BroadcastAction.BROADCAST_SERVICE_REC_ACTION_SERVICE.equals(action)) {

            }else if (BroadcastAction.BROADCAST_SERVICE_REC_ACTION_REMOTE_UPGRADE.equals(action)) {
                if (sbroad_value.equals(BroadcastAction.BROADCAST_CONTENT_UPGRADE_INFO)) {
                    // 升级过程中需要显示的信息
                    L.i("===Received Upgrade information:"+sbroad_aux_val);
                    if(sbroad_aux_val.equals("ui started")){
                        isUiStarted = true;
                    }else if(sbroad_aux_val.equals("ui stopped")){
                        isUiStarted = false;
                    }
                }
                else if (sbroad_value.equals(BroadcastAction.BROADCAST_CONTENT_UPGRADE_FILE_PATH)) {
                    // 收到升级文件的路径
                    L.i("===Received file path : " + sbroad_aux_val);
                    mUpgradeFile = new UpgradeFile(getApplicationContext(), sbroad_aux_val);
                    StartServiceTimer(0, 1000); // 创建定时器，立即启动，每1s执行一次
                }
                else if (sbroad_value.equals(BroadcastAction.BROADCAST_CONTENT_UPGRADE_GUIDE_STAR)) {
                    // 收到平台定制的广播包，检查完毕后可进行UI提示升级
                    L.i("===Received OTA broadcast");
                    hasRcvUpgradeBroadcast=true;
                }
                else if (sbroad_value.equals(BroadcastAction.BROADCAST_CONTENT_UPGRADE_STAR)) {
                    // 启动升级
                    L.i("BluetoothLeService receive a broadcast,start remote upgrade.");

                    if(fRemoteUpgradeThreadIsRun){
                        BroadcastAction.sendBroadcast(mThis,
                                BroadcastAction.BROADCAST_SERVICE_SEND_ACTION_REMOTE_UPGRADE,
                                BroadcastAction.BROADCAST_CONTENT_UPGRADE_INFO,
                                "In the process of upgrading");
                    } else if(fRemoteReadVerThreadIsRun){
                        BroadcastAction.sendBroadcast(mThis,
                                BroadcastAction.BROADCAST_SERVICE_SEND_ACTION_REMOTE_UPGRADE,
                                BroadcastAction.BROADCAST_CONTENT_UPGRADE_INFO,
                                "In the process of read remote version");
                    } else if(fEnableNotifyThreadIsRun){
                        BroadcastAction.sendBroadcast(mThis,
                                BroadcastAction.BROADCAST_SERVICE_SEND_ACTION_REMOTE_UPGRADE,
                                BroadcastAction.BROADCAST_CONTENT_UPGRADE_INFO,
                                "Connection initialization is not completed");
                    }else{
                        RemoteUpgradeThread();
                    }

                } else if (sbroad_value.equals(BroadcastAction.BROADCAST_CONTENT_UPGRADE_STOP)) {
                    // 停止升级
                    L.w("Upgrade thread interrupt!");
                    isUpgradeWorking=false;

                } else if (sbroad_value.equals(BroadcastAction.BROADCAST_CONTENT_REMOTE_INFO)) {
                    // 读取遥控器信息
                    L.i("BluetoothLeService receive a broadcast,read remote info.");

                    if(fRemoteUpgradeThreadIsRun){
                        BroadcastAction.sendBroadcast(mThis,
                                BroadcastAction.BROADCAST_SERVICE_SEND_ACTION_REMOTE_UPGRADE,
                                BroadcastAction.BROADCAST_CONTENT_UPGRADE_INFO,
                                "In the process of upgrading");
                    } else if(fRemoteReadVerThreadIsRun){
                        BroadcastAction.sendBroadcast(mThis,
                                BroadcastAction.BROADCAST_SERVICE_SEND_ACTION_REMOTE_UPGRADE,
                                BroadcastAction.BROADCAST_CONTENT_UPGRADE_INFO,
                                "In the process of read remote version");
                    } else if(fEnableNotifyThreadIsRun){
                        BroadcastAction.sendBroadcast(mThis,
                                BroadcastAction.BROADCAST_SERVICE_SEND_ACTION_REMOTE_UPGRADE,
                                BroadcastAction.BROADCAST_CONTENT_UPGRADE_INFO,
                                "Connection initialization is not completed");
                    } else {
                        ReadRemoteVerPowerThread();
                    }
                }else if (sbroad_value.equals(BroadcastAction.BROADCAST_CONTENT_UPGRADE_SELF_STAR)) {
                    L.i("BluetoothLeService receive a broadcast,enable self star service init.");
                    //setServiceUpgradeMode(Config.curUpgradeMode);
                }

            }else if (BroadcastAction.BROADCAST_SERVICE_REC_ACTION_BLUETOOTH.equals(action)) {
                if(sbroad_value.equals(BroadcastAction.ROADCAST_CONTENT_BLUETOOTH_INFO)) {

                } else  if(sbroad_value.equals(BroadcastAction.ROADCAST_CONTENT_BLUETOOTH_GATT_INIT)) {
                    L.i("BluetoothLeService receive a broadcast, initialize ble.");
                    if (initialize(null, null)) {
                        L.d("Broadcast Init : find device.");
                    } else {
                        L.e("Init Err:no device.");
                    }
                }
            } else if (BroadcastAction.BROADCAST_SERVICE_REC_ACTION_FILE_OPERATION.equals(action)) {

            }
        }
    };

    //==============================================================================================
    void  KeyReceivedCallback(byte keys[]){
        L.i("===KeyReceivedCallback:"+keys.toString());
    }

    //==============================================================================================
    private void EnableAllNotificationThread(){

        if (mNotificationThread != null && mNotificationThread.isAlive()) {
            L.i("Notification thread is alive!");
        } else {
            L.i("Start enable all notification thread...");
            mNotificationThread = new Thread(new Runnable() {
                @Override
                public void run() {
                    fEnableNotifyThreadIsRun = true;
                    OperationWait(500);
                    if((mBluetoothGatt != null) && (VendorIn_Characteristic != null)) {
                        L.i("Enable Vendor In Notification.");
                        try {
                            setCharacteristicNotification(VendorIn_Characteristic, null, true);
                        }catch (Exception e){
                            L.e("Err:vendor In Notification err.");
                        }
                    }
                    OperationWait(20);
                    if((mBluetoothGatt != null) && (OtaIn_Characteristic != null)) {
                        L.i("Enable Ota In Notification.");
                        try {
                            setCharacteristicNotification(OtaIn_Characteristic, null, true);
                        }catch (Exception e){
                            L.e("Err:ota In Notification err.");
                        }
                    }
                    RemoteVid = 0x00;
                    RemotePid = 0x00;
                    RemoteVerH = 0x00;
                    RemoteVerL = 0x00;
                    RemoteBattery = 0x00;
                    OperationWait(20);
                    ReadRemoteSfVersion();
                    OperationWait(20);
                    ReadRemotePower();
                    L.i("End enable all notification thread.");

                    if(!fBleRecVendorPacketOk || !fBleSendVendorPacketOk){
                        //启动UI
                        //StartFloatActivity();
                        // 初始化信息时蓝牙读写异常
                        BroadcastAction.sendBroadcast(mThis,
                                BroadcastAction.BROADCAST_SERVICE_SEND_ACTION_REMOTE_UPGRADE,
                                BroadcastAction.BROADCAST_CONTENT_UPGRADE_ERR_INFO,
                                "Initial fail");
                    }

                    fEnableNotifyThreadIsRun = false;
                }
            });

            // 执行线程
            mNotificationThread.start();
        }
    }

    private void ReadRemoteVerPowerThread(){
        L.i("Start read remote version and power thread...");
        new Thread(new Runnable() {
            @Override
            public void run() {
                for(int k=0;k<200;k++){
                    OperationWait(20);
                    if(fEnableNotifyThreadIsRun == false){
                        break;
                    }
                }
                if(fRemoteUpgradeThreadIsRun == false) {
                    fRemoteReadVerThreadIsRun = true;
                    RemoteVid = 0x00;
                    RemotePid = 0x00;
                    RemoteVerH = 0x00;
                    RemoteVerL = 0x00;
                    RemoteBattery = 0x00;
                    L.i("Read remote version and power");
                    OperationWait(20);
                    ReadRemoteSfVersion();
                    OperationWait(20);
                    ReadRemotePower();

                    fRemoteReadVerThreadIsRun = false;
                }else{
                    L.i("In the process of upgrading");
                }
                L.i("End read remote version and power thread.");
            }
        }).start();
    }

    private void ReadRemoteEncryVersionPower(){
        byte[] BufVersion = {0x5c,(byte)0x70,0x00,0x00,0x00,0x00,0x00,0x00,0x00,0x00,
                0x00,0x00,0x00,0x00,0x00,0x00,0x00,0x00,0x00,0x00};
        byte [] EncRandomBuf = null;

        EncRandomBuf = mEncryption.getRandomData();
        for(int t=0;t<7;t++){
            BufVersion[2 + t] = EncRandomBuf[t];
        }

        for(int i=0;i<3;i++) {
            if ((mBluetoothGatt != null) && (VendorOut_Characteristic != null)) {
                Arrays.fill(BleRecBuff,(byte)0);
                fBleRecVendorPacketOk = false;
                fBleSendVendorPacketOk = false;
                WriteVendorOutCharacter(BufVersion);
                L.i("Read remote version and power(S):" + GetByteString(BufVersion, 5));
            }
            for(int j=0;j<150;j++) {
                OperationWait(20);
                if(fBleRecVendorPacketOk && fBleSendVendorPacketOk) {
                    L.i("Successful receiving version and power package.");
                    break;
                }
            }

            L.w("Send:"+ fBleSendVendorPacketOk + ", Rcv:" + fBleRecVendorPacketOk);

            if(fBleRecVendorPacketOk) {
                if (BleRecBuff[0] == (byte) 0x5c){
                    if(BleRecBuff[1] == (byte) 0x70) {
                        // 收到电池电量数据包
                        boolean flagEncryOk = true;

                        if(mEncryption.getEncryptionEnable()){
                            flagEncryOk = mEncryption.RemoteInfoVerify(BleRecBuff);
                        }else{
                            flagEncryOk = true;
                        }

                        if(flagEncryOk){
                            RemoteVid = BleRecBuff[2];     // 遥控器VID
                            RemotePid = BleRecBuff[3];     // 遥控器PID
                            RemoteVerL = BleRecBuff[4];    // 遥控器版本号低字节
                            RemoteVerH = BleRecBuff[5];    // 遥控器版本号高字节
                            L.i("Remote version:" + String.format("%02x%02x,", RemoteVerH, RemoteVerL));
                            L.i("Remote VID PID:" + String.format("%02x%02x,", RemoteVid, RemotePid));
                            int itmp = (int) RemoteVerH;
                            itmp &= 0xff;
                            itmp *= 0x100;
                            itmp += ((int) RemoteVerL & 0xff);
                            BroadcastAction.sendBroadcast(mThis, BroadcastAction.BROADCAST_SERVICE_SEND_ACTION_REMOTE_UPGRADE,
                                    BroadcastAction.BROADCAST_CONTENT_REMOTE_VERSION,
                                    itmp);

                            RemoteBattery = (((int) BleRecBuff[7] << 8) & 0x00ff00);  // 电池电量
                            RemoteBattery += ((int) BleRecBuff[6] & 0x00ff);
                            L.i("Read remote battery power:" + String.format("%d", RemoteBattery));
                            BroadcastAction.sendBroadcast(mThis, BroadcastAction.BROADCAST_SERVICE_SEND_ACTION_REMOTE_UPGRADE,
                                    BroadcastAction.BROADCAST_CONTENT_REMOTE_POWER,
                                    RemoteBattery);

                        }else{
                            RemoteVid = 0x00;
                            RemotePid = 0x00;
                            RemoteVerL = 0x00;
                            RemoteVerH = 0x00;
                            RemoteBattery = 0x00;
                            BroadcastAction.sendBroadcast(mThis, BroadcastAction.BROADCAST_SERVICE_SEND_ACTION_REMOTE_UPGRADE,
                                    BroadcastAction.BROADCAST_CONTENT_REMOTE_VERSION,
                                    0x00);
                            BroadcastAction.sendBroadcast(mThis, BroadcastAction.BROADCAST_SERVICE_SEND_ACTION_REMOTE_UPGRADE,
                                    BroadcastAction.BROADCAST_CONTENT_REMOTE_POWER,
                                    0x00);
                            BroadcastAction.sendBroadcast(mThis,
                                    BroadcastAction.BROADCAST_SERVICE_SEND_ACTION_REMOTE_UPGRADE,
                                    BroadcastAction.BROADCAST_CONTENT_UPGRADE_INFO,
                                    "Encryption verification failure");

                            L.i("Remote controller encryption verification failure.");
                        }
                        break;
                    }else{
                        // 各种错误、异常处理
                        SendUpgradeErrCode(BleRecBuff[1]);
                    }
                }
            }
        }
    }

    private void ReadRemoteSfVersion(){
        byte[] BufVersion = {0x5c,(byte)0x82,0x00,0x00,0x00,0x00,0x00,0x00,0x00,
                0x00,0x00,0x00,0x00,0x00,0x00,0x00,0x00,0x00,0x00,0x00};

        byte [] EncRandomBuf = mEncryption.getRandomData();
        for(int t=0;t<7;t++){
            BufVersion[2 + t] = EncRandomBuf[t];
        }

        for(int i=0;i<3;i++) {
            if ((mBluetoothGatt != null) && (VendorOut_Characteristic != null)) {
                Arrays.fill(BleRecBuff,(byte)0);
                fBleRecVendorPacketOk = false;
                fBleSendVendorPacketOk = false;
                WriteVendorOutCharacter(BufVersion);
                L.i("Read remote version,S(read ver):" + GetByteString(BufVersion, 5));
            }else{
                L.e("mBluetoothGatt or VendorOut_Characteristic is null!");
            }

            for(int j=0;j<150;j++) {
                OperationWait(20);
                if(fBleRecVendorPacketOk && fBleSendVendorPacketOk) {
                    L.i("Successful receiving version package");
                    break;
                }
            }

            L.w("Send:"+ fBleSendVendorPacketOk + ", Rcv:" + fBleRecVendorPacketOk);
            if(fBleRecVendorPacketOk) {
                if (BleRecBuff[0] == (byte) 0x5c){
                    if(BleRecBuff[1] == (byte) 0xf9) {
                        RemoteVid = BleRecBuff[2];     // 遥控器VID
                        RemotePid = BleRecBuff[3];     // 遥控器PID
                        RemoteVerL = BleRecBuff[4];    // 遥控器版本号低字节
                        RemoteVerH = BleRecBuff[5];    // 遥控器版本号高字节
                        L.i("Remote version:" + String.format("%02x%02x,", RemoteVerH, RemoteVerL));
                        L.i("Remote VID PID:" + String.format("%02x%02x,", RemoteVid, RemotePid));
                        int itmp = (int) RemoteVerH;
                        itmp &= 0xff;
                        itmp *= 0x100;
                        itmp += ((int) RemoteVerL & 0xff);
                        BroadcastAction.sendBroadcast(mThis, BroadcastAction.BROADCAST_SERVICE_SEND_ACTION_REMOTE_UPGRADE,
                                BroadcastAction.BROADCAST_CONTENT_REMOTE_VERSION,
                                itmp);
                        break;

                    }else{
                        // 各种错误、异常处理
                        SendUpgradeErrCode(BleRecBuff[1]);
                    }
                }
            }
        }
    }

    private void ReadRemotePower(){
        byte[] BufVersion = {0x5c,(byte)0x83,0x00,0x00,0x00,0x00,0x00,0x00,0x00,0x00,
                0x00,0x00,0x00,0x00,0x00,0x00,0x00,0x00,0x00,0x00};

        for(int i=0;i<3;i++) {
            if ((mBluetoothGatt != null) && (VendorOut_Characteristic != null)) {
                Arrays.fill(BleRecBuff,(byte)0);
                fBleRecVendorPacketOk = false;
                fBleSendVendorPacketOk = false;
                WriteVendorOutCharacter(BufVersion);
                L.i("Read remote power,S(read ver):" + GetByteString(BufVersion, 5));
            }
            for(int j=0;j<150;j++) {
                OperationWait(20);
                if(fBleRecVendorPacketOk && fBleSendVendorPacketOk) {
                    L.i("Successful receiving power package.");
                    break;
                }
            }

            L.w("Send:"+ fBleSendVendorPacketOk + ", Rcv:" + fBleRecVendorPacketOk);

            if(fBleRecVendorPacketOk) {
                if (BleRecBuff[0] == (byte) 0x5c){
                    if(BleRecBuff[1] == (byte) 0xf7) {
                        // 收到电池电量数据包
                        RemoteBattery = (((int) BleRecBuff[3] << 8) & 0x00ff00);  // 电池电量
                        RemoteBattery += ((int) BleRecBuff[2] & 0x00ff);
                        L.i("Read remote battery power:" + String.format("%d", RemoteBattery));
                        BroadcastAction.sendBroadcast(mThis, BroadcastAction.BROADCAST_SERVICE_SEND_ACTION_REMOTE_UPGRADE,
                                BroadcastAction.BROADCAST_CONTENT_REMOTE_POWER,
                                RemoteBattery);
                        break;
                    }else{
                        // 各种错误、异常处理
                        SendUpgradeErrCode(BleRecBuff[1]);
                    }
                }
            }
        }
    }

    //==============================================================================================
    // remote upgrade
    private boolean SendAndRecPacket(byte [] sendBuf,byte recCmd,int timerOutMs){
        boolean  flag = false;
        for(int i=0;i<3;i++) {
            if ((mBluetoothGatt != null) && (VendorOut_Characteristic != null)) {
                Arrays.fill(BleRecBuff,(byte)0);
                fBleRecVendorPacketOk = false;
                fBleSendVendorPacketOk = false;
                WriteVendorOutCharacter(sendBuf);
                L.i("S:" + GetByteString(sendBuf, 6));
            }
            OperationWait(8);
            for(int j=0;j<(timerOutMs/2);j++) {
                OperationWait(2);
                if(fBleRecVendorPacketOk && fBleSendVendorPacketOk) {
                    L.i("Successful receiving package");
                    break;
                }
            }
            if(fBleRecVendorPacketOk) {
                if (BleRecBuff[0] == (byte) 0x5c){
                    if(BleRecBuff[1] == recCmd){
                        if (BleRecBuff.length > 10) {
                            L.i("Request package," + recCmd);
                            flag = true;
                            break;
                        }
                    }else{
                        // 各种错误、异常处理
                        SendUpgradeErrCode(BleRecBuff[1]);
                    }
                }
            }
        }
        return flag;
    }

    private boolean SendAndRecBinPacket(byte [] sendBuf){
        boolean  flag = false;

        // TODO: 多包发送，里面不阻塞，连续发几包再同步一下收包bit map \
        // sendBuf 可以临时存贮到数组，根据返回的 bit map重发丢的包

        for(int i=0;i<3;i++) {
            if ((mBluetoothGatt != null) && (VendorOut_Characteristic != null)) {
                Arrays.fill(BleRecBuff,(byte)0);
                fBleRecVendorPacketOk = false;
                fBleSendVendorPacketOk = false;
                WriteVendorOutCharacter(sendBuf);
                L.i("S:" + GetByteString(sendBuf, 5));
            }
            OperationWait(10);
            for(int j=0;j<800;j++) {
                if(fBleRecVendorPacketOk && fBleSendVendorPacketOk) {
                    break;
                }
                if(j>=5) {
                    if ((j % 5) == 0) {
                        OperationWait(3);
                    }
                }
                mSpeedControl.WaitUsDelay(1000);
            }
            if(fBleRecVendorPacketOk) {
                if (BleRecBuff[0] == (byte) 0x5c) {
                    if (BleRecBuff[1] == (byte)0xf0) {
                        // 表示遥控器接收到一个正确地数据包，请求下一个数据包
                        if (BleRecBuff.length > 10) {
                            int itmp = (int)BleRecBuff[2];
                            needSendBinPacketNum = itmp & 0xff;
                            itmp = (int)BleRecBuff[3];
                            needSendBinPacketNum += ((itmp & 0xff)<<8);
                            L.i("Request bin package:" + needSendBinPacketNum);
                            flag = true;
                            break;
                        }
                    }else if(BleRecBuff[1] == (byte)0xfa){
                        //  遥控器升级成功

                    }else{
                        // 各种错误、异常处理
                        SendUpgradeErrCode(BleRecBuff[1]);
                    }
                }
            }
        }
        return flag;
    }

    private boolean SendUpgradeBinFile(){
        byte[] sendBinBuff ;
        byte[] tempBuf;
        byte[] backBuf = new byte [20];
        int errNum;
        int curSendPercent,backSendPercent;
        boolean flag = false;
        haveSendBinPacketNum = 0x00;
        needSendBinPacketNum = -1 ;
        errNum = 0;
        curSendPercent = 0;
        backSendPercent = 0;
        while(isUpgradeWorking) {
            sendBinBuff = mUpgradeFile.GetDataPacketBuf(haveSendBinPacketNum);

            if((mEncryption.getEncryptionEnable()) &&(sendBinBuff != null)){
                for (int n = 0; n < 16; n++) {
                    backBuf[2 + n] = sendBinBuff[4 + n];
                }
                backBuf[0] = 0x01;
                backBuf[1] = sendBinBuff[2];
                tempBuf = mEncryption.encryptData(backBuf);
                for (int n = 0; n < 16; n++) {
                    sendBinBuff[4 + n] = tempBuf[1 + n];
                }
            }

            if(sendBinBuff != null) {
                // 发送升级包
                if (SendAndRecBinPacket(sendBinBuff)) {
                    curSendPercent = mUpgradeFile.GetSendPercent(haveSendBinPacketNum);
                    if(curSendPercent > 99){
                        curSendPercent = 99;
                    }
                    if(curSendPercent != backSendPercent){
                        backSendPercent = curSendPercent;
                        L.i("Send Percent:" + curSendPercent);
                        BroadcastAction.sendBroadcast(mThis, BroadcastAction.BROADCAST_SERVICE_SEND_ACTION_REMOTE_UPGRADE,
                                BroadcastAction.BROADCAST_CONTENT_UPGRADE_PROCESS,
                                curSendPercent);
                    }

                    if (haveSendBinPacketNum == needSendBinPacketNum) {
                        haveSendBinPacketNum++;
                        errNum = 0;

                    }
                }else{
                    L.e("A packet sending failure");
                    errNum ++;
                    if(errNum >= 3){
                        break;
                    }
                }
            }else{
                if(curSendPercent > 97){
                    flag = true;
                }
                break;
            }
        }
        return flag;
    }

    private boolean SendUpgradeEndFileHead(){
        byte[] sendHeaderBuff ;
        byte[] backBuf = new byte[20] ;
        byte[] tempBuf;
        boolean flag = true;

        for(int k=0;k<4;k++) {
            sendHeaderBuff = mUpgradeFile.GetAllHeadPacketBuf(k);
            L.i("Send all Image header:" + k);

            if((mEncryption.getEncryptionEnable()) &&(sendHeaderBuff != null)){
                for (int n = 0; n < 16; n++) {
                    backBuf[2 + n] = sendHeaderBuff[4 + n];
                }
                backBuf[0] = 0x01;
                backBuf[1] = sendHeaderBuff[2];
                tempBuf = mEncryption.encryptData(backBuf);
                for (int n = 0; n < 16; n++) {
                    sendHeaderBuff[4 + n] = tempBuf[1 + n];
                }
            }

            if(!SendAndRecPacket(sendHeaderBuff,(byte)0xf8,2000)) {
                L.e("Three transmission failures.");
                flag = false;
                break;
            }
        }
        return flag;
    }

    private boolean SendUpgradeEndCmd(){
        byte[] sendEndCmdBuff ;
        boolean flag = true;

        sendEndCmdBuff = mUpgradeFile.GetEntryUpgradePacketBuf();
        L.i("Send Upgrade end command");
        if(!SendAndRecPacket(sendEndCmdBuff,(byte)0xfA,2000)) {
            L.e("Three transmission failures.");
            flag = false;
        }

        return flag;
    }

    private boolean SendUpgradeResetCmd(){
        byte[] sendResetCmdBuff ;
        boolean flag = true;

        sendResetCmdBuff = mUpgradeFile.GetUpgradeResetPacketBuf();
        L.i("Send Upgrade reset command");
        if(!SendAndRecPacket(sendResetCmdBuff,(byte)0xfb,2000)) {
            L.e("Three transmission failures.");
            flag = false;
        }
        return flag;
    }

    private boolean SendUpgradeErrCode(byte errCode){
        String strErrCode = null;
        switch (errCode){
            case (byte)0xF0:
                strErrCode = getResources().getString(R.string.Text_view_info_code_F0);
                break;
            case (byte)0xF1:
                strErrCode = getResources().getString(R.string.Text_view_info_code_F1);
                break;
            case (byte)0xF2:
                strErrCode = getResources().getString(R.string.Text_view_info_code_F2);
                break;
            case (byte)0xF3:
                strErrCode = getResources().getString(R.string.Text_view_info_code_F3);
                break;
            case (byte)0xF4:
                strErrCode = getResources().getString(R.string.Text_view_info_code_F4);
                break;
            case (byte)0xF5:
                strErrCode = getResources().getString(R.string.Text_view_info_code_F5);
                break;
            case (byte)0xF6:
                strErrCode = getResources().getString(R.string.Text_view_info_code_F6);
                break;
            case (byte)0xF7:
                strErrCode = getResources().getString(R.string.Text_view_info_code_F7);
                break;
            case (byte)0xF8:
                strErrCode = getResources().getString(R.string.Text_view_info_code_F8);
                break;
            case (byte)0xF9:
                strErrCode = getResources().getString(R.string.Text_view_info_code_F9);
                break;
            case (byte)0xFA:
                strErrCode = getResources().getString(R.string.Text_view_info_code_FA);
                break;
            case (byte)0xFB:
                strErrCode = getResources().getString(R.string.Text_view_info_code_FB);
                break;
            case (byte)0xFC:
                strErrCode = getResources().getString(R.string.Text_view_info_code_FC);
                break;
            case (byte)0xFD:
                strErrCode = getResources().getString(R.string.Text_view_info_code_FD);
                break;
            case (byte)0xFE:
                strErrCode = getResources().getString(R.string.Text_view_info_code_FE);
                break;
            case (byte)0xFF:
                strErrCode = getResources().getString(R.string.Text_view_info_code_FF);
                break;
        }

        BroadcastAction.sendBroadcast(mThis,
                BroadcastAction.BROADCAST_SERVICE_SEND_ACTION_REMOTE_UPGRADE,
                BroadcastAction.BROADCAST_CONTENT_UPGRADE_ERR_CODE,
                strErrCode);
        L.e("ErrCode:"+ String.format("0x%02X,", errCode));
        return true;
    }

    private void RemoteUpgradeThread(){

        // 重置循环标志
        isUpgradeWorking=true;

        if (mUpgradeThread != null && mUpgradeThread.isAlive()) {
            L.i("Remote upgrade thread is alive!");
        } else {
            L.i("Start remote upgrade thread...");
            mUpgradeThread = new Thread(new Runnable() {
                @Override
                public void run() {
                    byte[] BleSendBuff ;
                    boolean flagUpgrade;

                    byte[] tempBuf;
                    byte[] BackBleSendBuff=new byte[20];
                    int n;

                    // 线程正在运行标志
                    fRemoteUpgradeThreadIsRun = true;

                    //如果是手动开启app则先判断是否已经读取过版本和电量
                    if((RemoteVid == 0x00) && (RemotePid == 0x00)
                            && (RemoteVerH == 0x00)
                            && (RemoteVerL == 0x00)
                            && (RemoteBattery == 0x00)
                            ) {
                        OperationWait(20);

                        if(mEncryption.getEncryptionEnable()){
                            ReadRemoteEncryVersionPower();
                        }else {
                            ReadRemoteSfVersion();
                            OperationWait(20);
                            ReadRemotePower();
                        }
                        L.i("Checking VID PID and version.");
                    }

                    flagUpgrade = false;
                    if((RemoteVid == 0x00) && (RemotePid == 0x00)
                            && (RemoteVerH == 0x00)
                            && (RemoteVerL == 0x00)
                            && (RemoteBattery == 0x00)
                            ) {
                        L.i("No check on VID PID and version.");
                    }else{
                        if(RemoteBattery >= 2600){
                            if(mUpgradeFile.CheckUpgradePidVid(RemoteVid,RemotePid)) {
                                L.i("VID PID check is normal.");
                                if (!mUpgradeFile.CheckUpgradeVersion(RemoteVerH, RemoteVerL)) {
                                    L.i("Remote version check is normal.");
                                    flagUpgrade = true;
                                } else {
                                    // 发送广播显示结果
                                    BroadcastAction.sendBroadcast(mThis,
                                            BroadcastAction.BROADCAST_SERVICE_SEND_ACTION_REMOTE_UPGRADE,
                                            BroadcastAction.BROADCAST_CONTENT_UPGRADE_COMPLETE,
                                            "version is the same");
                                }
                            }else{
                                // 启动UI
                                //StartFloatActivity();
                                // 发送广播显示结果
                                L.e("VID or PID error, abort.");
                                BroadcastAction.sendBroadcast(mThis,
                                        BroadcastAction.BROADCAST_SERVICE_SEND_ACTION_REMOTE_UPGRADE,
                                        BroadcastAction.BROADCAST_CONTENT_UPGRADE_ERR_INFO,
                                        "VID or PID error");
                            }
                        }else{
                            // 启动UI
                            //StartFloatActivity();

                            L.i("The battery voltage is low, abort.");
                            // 发送广播显示结果
                            BroadcastAction.sendBroadcast(mThis,
                                    BroadcastAction.BROADCAST_SERVICE_SEND_ACTION_REMOTE_UPGRADE,
                                    BroadcastAction.BROADCAST_CONTENT_UPGRADE_ERR_INFO,
                                    "low battery");
                            if(enableDisplayToast){
                                SendToastHandler(getResources().getString(R.string.Toast_view_power_low));
                                L.i("Send toast,low battery.");
                            }
                        }
                    }

                    if(flagUpgrade){
                        if(enableDisplayToast){
                            L.i("Send toast,start upgrade.");
                            SendToastHandler(getResources().getString(R.string.Toast_view_star_upgrade));
                            L.i("Send toast,start upgrade.");
                        }

                        BleSendBuff = mUpgradeFile.GetHeadPacketBuf();

                        if(mEncryption.getEncryptionEnable()) {
                            for (n = 0; n < 20; n++) {
                                BackBleSendBuff[n] = BleSendBuff[n];
                            }
                            BackBleSendBuff[0] = 0x00;
                            BackBleSendBuff[1] = 0x01;
                            tempBuf = mEncryption.encryptData(BackBleSendBuff);
                            for (n = 0; n < 16; n++) {
                                BleSendBuff[2 + n] = tempBuf[1 + n];
                            }
                        }

                        // 发送升级Image头
                        L.i("Send Image header");
                        if(SendAndRecPacket(BleSendBuff,(byte)0xf8,2000)) {
                            // 下传bin文件
                            L.i("Send upgrade bin");
                            if(SendUpgradeBinFile()) {
                                // bin文件下传结结束后下传帧头
                                L.i("Send file header");
                                if(SendUpgradeEndFileHead()){
                                    // 下传升级完成命令
                                    L.i("Send end command");
                                    if(SendUpgradeEndCmd()){
                                        L.i("upgrade complete,send reset command.");
                                        if(SendUpgradeResetCmd()) {
                                            RemoteVid = 0x00;
                                            RemotePid = 0x00;
                                            RemoteVerL = 0x00;
                                            RemoteVerH = 0x00;
                                            RemoteBattery = 0x00;
                                            L.i("upgrade successful.");
                                        }else {
                                            L.i("Send reset command fail.");
                                        }

                                        //启动UI
                                        //StartFloatActivity();
                                        //发送广播给UI显示结果
                                        BroadcastAction.sendBroadcast(mThis,
                                                BroadcastAction.BROADCAST_SERVICE_SEND_ACTION_REMOTE_UPGRADE,
                                                BroadcastAction.BROADCAST_CONTENT_UPGRADE_PROCESS,
                                                100);
                                        BroadcastAction.sendBroadcast(mThis,
                                                BroadcastAction.BROADCAST_SERVICE_SEND_ACTION_REMOTE_UPGRADE,
                                                BroadcastAction.BROADCAST_CONTENT_UPGRADE_COMPLETE,
                                                "Upgrade successful.");
                                        if(enableDisplayToast){
                                            SendToastHandler(getResources().getString(R.string.Toast_view_upgrade_success));
                                            enableDisplayToast = false ;
                                            L.i("Send toast,upgrade successful.");
                                        }
                                    }else{
                                        // 启动UI
                                        //StartFloatActivity();
                                        // 下传升级完成命令,失败
                                        BroadcastAction.sendBroadcast(mThis,
                                                BroadcastAction.BROADCAST_SERVICE_SEND_ACTION_REMOTE_UPGRADE,
                                                BroadcastAction.BROADCAST_CONTENT_UPGRADE_ERR_INFO,
                                                "Send end command fail");
                                        L.i("Send end command fail, abort.");
                                    }
                                }else{
                                    // 启动UI
                                    //StartFloatActivity();
                                    // bin文件下传结结束后下传帧头,失败
                                    BroadcastAction.sendBroadcast(mThis,
                                            BroadcastAction.BROADCAST_SERVICE_SEND_ACTION_REMOTE_UPGRADE,
                                            BroadcastAction.BROADCAST_CONTENT_UPGRADE_ERR_INFO,
                                            "Send all header fail");
                                    L.i("Send Header fail, abort.");
                                }
                            }else{
                                // 启动UI
                                //StartFloatActivity();
                                // 下传bin文件,失败
                                BroadcastAction.sendBroadcast(mThis,
                                        BroadcastAction.BROADCAST_SERVICE_SEND_ACTION_REMOTE_UPGRADE,
                                        BroadcastAction.BROADCAST_CONTENT_UPGRADE_ERR_INFO,
                                        "Send bin fail");
                                L.i("Send bin fail, abort.");
                            }
                        }else{
                            // 启动UI
                            //StartFloatActivity();
                            // 发送升级Image头,失败
                            BroadcastAction.sendBroadcast(mThis,
                                    BroadcastAction.BROADCAST_SERVICE_SEND_ACTION_REMOTE_UPGRADE,
                                    BroadcastAction.BROADCAST_CONTENT_UPGRADE_ERR_INFO,
                                    "Send Image fail");
                            L.i("Send Image fail, abort.");
                        }
                    }

                    L.i("End remote upgrade thread.");
                    fRemoteUpgradeThreadIsRun = false;
                }
            });

            // 启动线程
            mUpgradeThread.start();
        }
    }

    //==============================================================================================
    // 定时器中断用于监测升级状态并发送广播
    private final static int BLUETOOTHLE_SERVICE_HANDLE_TIMER = 1;
    private Timer mServiceTimer = null; // 定时器，监测升级状态
    private TimerTask mServiceTimerTask = null; // 定时器线程

    // 创建定时器
    private void StartServiceTimer(long delayMs, long periodMs){
        //先停止当前存在的定时器
        StopServiceTimer();

        L.i("===Start service timer.");
        mServiceTimer = new Timer();
        mServiceTimerTask = new TimerTask() {
            @Override
            public void run() {
                Message VendorMessage = new Message();
                VendorMessage.what = BLUETOOTHLE_SERVICE_HANDLE_TIMER;
                mServiceHandler.sendMessage(VendorMessage);
            }
        };
        mServiceTimer.schedule(mServiceTimerTask, delayMs, periodMs);
    }

    // 销毁定时器
    private void StopServiceTimer(){
        L.i("Stop service timer.");
        try {
            if (mServiceTimer != null) {
                mServiceTimer.cancel();
                mServiceTimer = null;
            }

            if (mServiceHandler != null) {
                mServiceHandler.removeCallbacks(mServiceTimerTask);
                //mServiceHandler = null;
            }

            if (mServiceTimerTask != null) {
                mServiceTimerTask.cancel();
                mServiceTimerTask = null;
            }
        }catch (Exception e) {
            e.printStackTrace();
        }
    }

    // 消息处理线程
    private Handler mServiceHandler = new Handler(new Handler.Callback() {
        @Override
        public boolean handleMessage(Message msg) {
            int msgId = msg.what;
            switch (msgId) {
                case BLUETOOTHLE_SERVICE_HANDLE_TIMER:
                    // Timer定时器中断，发送的消息
                    checkBleCount++; //检测次数
                    L.i("===Check BluetoothLe devices count : " + checkBleCount);

                    // 通过开机广播包自启动的
                    if ((mBluetoothManager == null) || (mBluetoothAdapter == null)
                            || (mBluetoothGatt == null) || (VendorOut_Characteristic == null)) {
                        //重置蓝牙状态
                        ResetBluetoothValue();
                        //检测绑定的设备进行连接
                        if (initialize(null, null)) {
                            L.d("Init : find bonded devices.");
                        } else {
                            L.e("Init Err:no device.");
                        }
                    }else{
                        // 设备连接已完成，可以进行升级
                        fCanAutoUpgrade = true;
                        L.w("set fCanAutoUpgrade : " + fCanAutoUpgrade);
                    }
                    /* //修改定时器执行周期
                    else{
                        if(upgradeDelayMs>0){
                            upgradeDelayMs=0;
                            StartServiceTimer(1000,200);
                        }
                    }*/

                    // 开始升级后就停止定时器
                    if(fRemoteUpgradeThreadIsRun){
                        L.i("===Upgrade thread is running!");
                        StopServiceTimer();
                        break;
                    }

                    L.i("===VID PID:" + String.format("%02x%02x", RemoteVid, RemotePid)
                            +",RemoteBattery="+RemoteBattery+",fCanAutoUpgrade="+fCanAutoUpgrade);

                    // 各状态检测完毕可以开始自动升级
                    if ((RemoteVid != 0x00) && (RemotePid != 0x00)
                            && (RemoteBattery != 0) && (fCanAutoUpgrade)) {

                        L.i("Start upgrade detection...");
                        if (mUpgradeFile.CheckUpgradePidVid(RemoteVid, RemotePid)) {
                            L.i("Automatic upgrade,VID PID check is normal.");
                            if (!mUpgradeFile.CheckUpgradeVersion(RemoteVerH, RemoteVerL)) {
                                L.i("Automatic upgrade,SoftVersion check is normal.");

                                // 开始升级
                                L.i("Upgrade mode [Guide],OTA broadcast:"+hasRcvUpgradeBroadcast);
                                // UI提示按键升级
                                if(hasRcvUpgradeBroadcast && !isUiStarted){ // 广播启动则认为已经具备升级条件
                                    // 启动UI
                                    StartFloatActivity();
                                    hasRcvUpgradeBroadcast=false;
                                }else{
                                    L.i("===hasRcvUpgradeBroadcast:"+hasRcvUpgradeBroadcast
                                            +",isUiStarted="+isUiStarted);
                                }

                                // 发送广播告知UI满足升级条件
                                BroadcastAction.sendBroadcast(mThis,
                                        BroadcastAction.BROADCAST_SERVICE_SEND_ACTION_REMOTE_UPGRADE,
                                        BroadcastAction.BROADCAST_CONTENT_UPGRADE_INFO,
                                        "ready to upgrade");

                            } else { //-----版本相同-----
                                // 启动UI
                                // StartFloatActivity();

                                if(isUiStarted){
                                    StopServiceTimer();
                                    // 发送广播给UI显示结果
                                    BroadcastAction.sendBroadcast(mThis,
                                            BroadcastAction.BROADCAST_SERVICE_SEND_ACTION_REMOTE_UPGRADE,
                                            BroadcastAction.BROADCAST_CONTENT_UPGRADE_COMPLETE,
                                            "version is the same");
                                }
                                L.i("Automatic upgrade,SoftVersion check is same.");
                            }
                        } else { //-----VID PID 不匹配-----
                            // 启动UI
                            // StartFloatActivity();

                            if(isUiStarted){
                                StopServiceTimer();
                                // 发送广播给UI显示结果
                                BroadcastAction.sendBroadcast(mThis,
                                        BroadcastAction.BROADCAST_SERVICE_SEND_ACTION_REMOTE_UPGRADE,
                                        BroadcastAction.BROADCAST_CONTENT_UPGRADE_ERR_INFO,
                                        "VID or PID error");
                            }
                            L.e("Automatic upgrade,VID check error.");
                        }
                    }
                    break;
            }
            return false;
        }
    });

    // Toast显示调试信息
    private void SendToastHandler(final String strToast){
        Handler handler = new Handler(Looper.getMainLooper());
        handler.post(new Runnable() {
            public void run() {
                try {
                    Utils.ToastShow(getApplicationContext(), Toast.LENGTH_LONG, Gravity.TOP, strToast, null);
                }catch (Exception e){
                    L.e("send toast err.");
                }
            }
        });
    }

    // 启动UI显示
    private void StartFloatActivity(){
        if(!isUiStarted){
            Intent mainActivityIntent = new Intent(mThis, UpgradeActivity.class);  // 要启动的Activity
            mainActivityIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            mThis.startActivity(mainActivityIntent);
            L.i("StartFloatActivity...");
        }
    }
}



