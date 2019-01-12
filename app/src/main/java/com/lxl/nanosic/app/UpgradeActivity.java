package com.lxl.nanosic.app;

import android.Manifest;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.ServiceConnection;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;
import android.os.IBinder;
import android.support.v7.app.AppCompatActivity;
import android.text.TextUtils;
import android.view.Gravity;
import android.view.KeyEvent;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import com.lxl.nanosic.app.ble.BluetoothLeService;
import com.lxl.nanosic.app.ble.BroadcastAction;
import com.lxl.nanosic.app.ble.Config;
import com.lxl.nanosic.app.ui.MultipleProgressBar;

public class UpgradeActivity extends AppCompatActivity {

    private static UpgradeActivity mThis = null;

    //----------------------------------------------------------------------------------------------
    private TextView TextView_MainDisplay = null;   // 主要状态显示
    private TextView TextView_GuideDisplay = null;  // 向导提示
    private ImageView iv_fail    = null;            // 升级失败图标
    private ImageView iv_success = null;            // 升级成功图标
    private MultipleProgressBar multipleProgressBar = null; //多样进度条(圆形)

    //----------------------------------------------------------------------------------------------
    public IntentFilter mainRecFilter;
    private String RemoteMac,RemoteName;
    private String UpgradeFileVersion,UpgradeFilePath;
    private int RemoteSfVer,RemotePower;

    private boolean isUpgrading     = false;
    private boolean isUpgradeDone   = false;
    private boolean isRcvUpgradeKey = false;
    private boolean isUIOnTheTop    = true;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_upgrade);
        mThis = this;
        ComponentInitialization();
        RegisterBroadcastReceiver();
        checkBluetoothAndLocationPermission(this);

        //如果没有收到正在升级的广播包则提示用户升级
        if(!isUpgrading){
            TextView_MainDisplay.setText("请连接蓝牙遥控器：");
            TextView_GuideDisplay.setText("{按任意键进入升级}\n{按返回键退出升级}");
        }

        // 绑定 BluetoothLeService
        if (localService == null) {
            BinderService();
        }

        // 发送UI启动广播告知service
        BroadcastActivityState("ui started");

        L.i("onCreate");
    }

    @Override
    public void onDestroy() {
        StopUpgrade(); //停止升级
        BroadcastActivityState("ui stopped");// 发送UI停止广播告知service
        unRegisterBroadcastReceiver();
        UnBinderService();
        super.onDestroy();
        L.i("onDestroy");
    }

    @Override
    protected void onResume() {
        super.onResume();
        isUIOnTheTop=true;
        L.i("onResume");
    }

    @Override
    protected void onPause() {
        BroadcastActivityState("ui stopped");// 发送UI停止广播告知service
        //按HOME键退出的情况也进入升级
        if(isUpgradeDone){
            ExitUpgrade();
        }
        else if(!isUpgrading) {
            isRcvUpgradeKey = true;
            TextView_MainDisplay.setText("正在初始化信息...");
            TextView_GuideDisplay.setVisibility(View.GONE);
            iv_fail.setVisibility(View.GONE);
            iv_success.setVisibility(View.GONE);
        }

        super.onPause();
        isUIOnTheTop=false;
        L.i("onPause");
    }

    /**
     * 重写返回键事件，防止应用退出
     * */
    /*
    @Override
    public void onBackPressed() {
        L.i("onBackPressed : do nothing." );
    }
    */

    //==============================================================================================
    // 确认可升级时检测是否有任意按键事件
    public boolean onKeyDown(int keyCode, KeyEvent event) { //重写的键盘按下监听
        L.i("Receive key code :" + keyCode);


        if (event.getKeyCode() == KeyEvent.KEYCODE_BACK
                && event.getAction() == KeyEvent.ACTION_DOWN
                && event.getRepeatCount() == 0) {
            //具体的操作代码
        }

        /** 不要改变判断顺序，有优先级！*/
        if(isUpgradeDone){
            ExitUpgrade();
        }else if(!isUpgrading){
            isRcvUpgradeKey=true;
            TextView_MainDisplay.setText("正在初始化信息...");
            TextView_GuideDisplay.setVisibility(View.GONE);
            iv_fail.setVisibility(View.GONE);
            iv_success.setVisibility(View.GONE);
        }
        return super.onKeyDown(keyCode, event);
    }

    /*  组件初始化 */
    private void  ComponentInitialization(){
        String sTempString;

        TextView_MainDisplay = (TextView) findViewById(R.id.textView_Info_Display); //主要状态显示
        TextView_GuideDisplay = (TextView) findViewById(R.id.textView_Guide_Info);
        multipleProgressBar = (MultipleProgressBar) findViewById(R.id.circleBar); // 进度条

        //升级结果图标
        iv_fail    = (ImageView) findViewById(R.id.imageView_failed);
        iv_success = (ImageView) findViewById(R.id.imageView_success);

        RemoteMac = null;
        RemoteName = null;
        RemoteSfVer = 0x00;
        RemotePower = 0x00;
        UpgradeFileVersion = null;
        UpgradeFilePath = null;
        //==========================================================================================
        // 初始化所有文本的显示
        sTempString = "AG:RemoteUpgrade,";
        if(Config.GetEncryptState()){
            sTempString += "Encrypted";
        }else{
            sTempString += "Unencrypted";
        }
        L.w(sTempString);

        multipleProgressBar.setProgress(0);
        multipleProgressBar.setEnabled(true);
    }

    //==============================================================================================
    // 启动BLE操作service
    //==============================================================================================
    BluetoothLeService localService = null;
    private ServiceConnection localServiceConnection = null;

    //用bindService方法启动服务
    private void BinderService() {
        localServiceConnection = new ServiceConnection() {
            /*
            * 只有在MyService中的onBind方法中返回一个IBinder实例才会在Bind的时候
            * 调用onServiceConnection回调方法
            * 第二个参数service就是MyService中onBind方法return的那个IBinder实例，可以利用这个来传递数据
            */
            @Override
            public void onServiceConnected(ComponentName componentName, IBinder binder) {
                //调用bindService方法启动服务时候，如果服务需要与activity交互，
                //则通过onBind方法返回IBinder并返回当前本地服务
                localService = ((BluetoothLeService.LocalBinder) binder).getService();

                RemoteMac = null;
                RemoteName = null;
                RemoteSfVer = 0x00;
                RemotePower = 0x00;
                localService.initialize(null,null);
            }

            /* SDK上是这么说的：
            * This is called when the connection with the service has been unexpectedly disconnected
            * that is, its process crashed. Because it is running in our same process, we should never see this happen.
            * 所以说，只有在service因异常而断开连接的时候，这个方法才会用到
            * */
            @Override
            public void onServiceDisconnected(ComponentName componentName) {
                localService = null;
                L.i("service disconnect the connection because of the exception." );
            }
        };
        Intent intent = new Intent(this, BluetoothLeService.class);
        bindService(intent,localServiceConnection , Context.BIND_AUTO_CREATE);
    }

    private void UnBinderService(){
        Intent intent = new Intent(this, BluetoothLeService.class);
        stopService(intent);
        if( localService != null) {            ;
            unbindService(localServiceConnection);
            localService = null;
        }
    }

    //==============================================================================================
    // 接收广播信息
    private void RegisterBroadcastReceiver(){
        mainRecFilter = new IntentFilter(BroadcastAction.BROADCAST_SERVICE_SEND_ACTION_GENERAL);
        mainRecFilter.addAction(BroadcastAction.BROADCAST_SERVICE_SEND_ACTION_SERVICE);
        mainRecFilter.addAction(BroadcastAction.BROADCAST_SERVICE_SEND_ACTION_BLUETOOTH);
        mainRecFilter.addAction(BroadcastAction.BROADCAST_SERVICE_SEND_ACTION_REMOTE_UPGRADE);
        mainRecFilter.addAction(BroadcastAction.BROADCAST_SERVICE_SEND_ACTION_FILE_OPERATION);
        registerReceiver(mainReceiver, mainRecFilter);
    }

    private void unRegisterBroadcastReceiver(){
        if(mainReceiver != null) {
            try {
                unregisterReceiver(mainReceiver);
                L.i("Main activity unregisterReceiver");
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }

    //==============================================================================================
    // 处理BLE service 的广播事件
    private BroadcastReceiver mainReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            int ibroad_value = 0x00;
            String sbroad_value = null, sbroad_aux_val = null;
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

            if (BroadcastAction.BROADCAST_SERVICE_SEND_ACTION_GENERAL.equals(action)) {
                // 普通的广播

            }else if (BroadcastAction.BROADCAST_SERVICE_SEND_ACTION_SERVICE.equals(action)) {
                // 和BluetoothLe Service相关的广播
                if (sbroad_value.equals(BroadcastAction.BROADCAST_CONTENT_SERVICE_ON_CREATED)) {

                } else if (sbroad_value.equals(BroadcastAction.BROADCAST_CONTENT_SERVICE_ON_BIND)) {

                } else if (sbroad_value.equals(BroadcastAction.BROADCAST_CONTENT_SERVICE_UNBIND)) {

                } else if (sbroad_value.equals(BroadcastAction.BROADCAST_CONTENT_SERVICE_ON_DESTROY)) {

                }

            }else if (BroadcastAction.BROADCAST_SERVICE_SEND_ACTION_BLUETOOTH.equals(action)) {
                if (sbroad_value.equals(BroadcastAction.ROADCAST_CONTENT_BLUETOOTH_INFO)) {

                } else  if (sbroad_value.equals(BroadcastAction.ROADCAST_CONTENT_BLUETOOTH_GATT_CONNECTED)) {
                    // Gatt connected
                    RemoteMac = sbroad_aux_val;
                    L.i("Receive broadcast,Ble Address :" + RemoteMac);
                }else  if (sbroad_value.equals(BroadcastAction.ROADCAST_CONTENT_BLUETOOTH_GATT_DISCONNECTED)) {
                    // Gatt dis connected
                    String BleAddress = sbroad_aux_val;
                    L.i("Receive broadcast,Ble disconnected :" + BleAddress);
                    if(isUpgrading)
                        FinishUpgrade(false,"遥控器已断连！",null);

                }else  if (sbroad_value.equals(BroadcastAction.ROADCAST_CONTENT_BLUETOOTH_GATT_DISCOVERED)) {
                    // Gatt discovered
                    RemoteName = sbroad_aux_val;
                    L.i("Receive broadcast,the device is connected successfully.The name is:" + RemoteName);
                }

            }else if (BroadcastAction.BROADCAST_SERVICE_SEND_ACTION_REMOTE_UPGRADE.equals(action)) {
                // 和升级相关的广播
                if (sbroad_value.equals(BroadcastAction.BROADCAST_CONTENT_UPGRADE_INFO)) {
                    // 升级过程中需要显示的信息
                    L.i("Receive broadcast,upgrade info:" + sbroad_aux_val);
                    if(sbroad_aux_val.equals("ready to upgrade")){
                        L.i("isUpgrading:" + isUpgrading + ",isRcvUpgradeKey:"+isRcvUpgradeKey);
                        if(!isUpgrading && isRcvUpgradeKey){
                            StartUpgrade(); //开始升级线程
                        } else if(isUpgradeDone){
                            ExitUpgrade();  //升级完成后，自动退出UI
                        }
                    }
                }else if (sbroad_value.equals(BroadcastAction.BROADCAST_CONTENT_UPGRADE_STAR)) {
                    // 启动升级

                } else if (sbroad_value.equals(BroadcastAction.BROADCAST_CONTENT_UPGRADE_STOP)) {
                    // 停止升级

                } else if (sbroad_value.equals(BroadcastAction.BROADCAST_CONTENT_REMOTE_VERSION)) {
                    // 遥控器版本
                    RemoteSfVer = ibroad_value;
                    L.i("Receive broadcast,the device sftware version:" + String.format("0x%04X,", RemoteSfVer));

                }else if (sbroad_value.equals(BroadcastAction.BROADCAST_CONTENT_REMOTE_POWER)) {
                    // 遥控器电量
                    RemotePower = ibroad_value;
                    L.i("Receive broadcast,the device power:" + String.format("%d,", RemotePower));

                }else if (sbroad_value.equals(BroadcastAction.BROADCAST_CONTENT_UPGRADE_PROCESS)) {
                    // 升级进度
                    int iPercent = ibroad_value;
                    isUpgrading=true;
                    L.i("Receive broadcast,Upgrade Percent:" + String.format("%d,", iPercent));
                    multipleProgressBar.setProgress(iPercent);
                    multipleProgressBar.setVisibility(View.VISIBLE);
                    TextView_MainDisplay.setText("升级中，请不要断电！");
                    TextView_GuideDisplay.setVisibility(View.GONE);
                    iv_fail.setVisibility(View.GONE);
                    iv_success.setVisibility(View.GONE);

                    // 如果UI被转后台则通过toast提示升级信息
                    if(!isUIOnTheTop){
                        Utils.ToastShow(getApplicationContext(), Toast.LENGTH_LONG, Gravity.TOP,
                                "遥控器升级中...", + iPercent +"%");
                    }

                }else if (sbroad_value.equals(BroadcastAction.BROADCAST_CONTENT_UPGRADE_COMPLETE)) {
                    // 升级完成
                    L.i("Receive broadcast,upgrade complete:" + sbroad_aux_val);

                    if(sbroad_aux_val.equals("version is the same")){
                        FinishUpgrade(true,"已经是最新版本！",null);
                    }else{
                        FinishUpgrade(true,"升级成功！",null);
                    }

                }else if (sbroad_value.equals(BroadcastAction.BROADCAST_CONTENT_UPGRADE_ERR_INFO)) {
                    // 升级错误
                    String aux_val=null;
                    L.i("Receive broadcast,upgrade err info:" + sbroad_aux_val);
                    if(sbroad_aux_val.equals("low battery")){
                        aux_val = "遥控器电量过低，请更换电池！";
                    }else if(sbroad_aux_val.equals("VID or PID error")){
                        aux_val = "升级文件不匹配，请确认遥控器型号！";
                    }else if(sbroad_aux_val.equals("Send Image fail")
                            || sbroad_aux_val.equals("Send bin fail")
                            || sbroad_aux_val.equals("Send all header fail")
                            || sbroad_aux_val.equals("Send end command fail")
                            || sbroad_aux_val.equals("Initial fail")){
                        aux_val = "数据传输异常，请重试！";
                    }
                    FinishUpgrade(false,"升级失败！",aux_val);
                }
                else if (sbroad_value.equals(BroadcastAction.BROADCAST_CONTENT_UPGRADE_ERR_CODE)) {
                    L.i("Receive broadcast,upgrade err code:" + sbroad_aux_val);
                }
            }else if (BroadcastAction.BROADCAST_SERVICE_SEND_ACTION_FILE_OPERATION.equals(action)) {
                // 和文件操作相关的广播
                if (sbroad_value.equals(BroadcastAction.ROADCAST_CONTENT_REMOTE_FILE_INFO)) {
                    // 文件操作中需要显示的信息

                }else if (sbroad_value.equals(BroadcastAction.ROADCAST_CONTENT_REMOTE_FILE_VERSION)) {
                    // 文件版本
                    UpgradeFileVersion = sbroad_aux_val;
                    L.i("Receive broadcast,upgrade file version:" + UpgradeFileVersion);
                } else if (sbroad_value.equals(BroadcastAction.ROADCAST_CONTENT_REMOTE_FILE_PATH)) {
                    // 升级文件路径
                    UpgradeFilePath = sbroad_aux_val;
                    L.i("Receive broadcast,upgrade file path:" + UpgradeFilePath);
                }
            }
        }
    };

    //==============================================================================================
    private static final int REQUEST_PERMISSION_BLUETOOTH_LE_CODE = 0x345;
    private static final int REQUEST_PERMISSION_STORAGE = 0x346;
    private String[] BlePermissions = {
        Manifest.permission.BLUETOOTH,                      /*  允许程序连接配对过的蓝牙设备  */
        Manifest.permission.BLUETOOTH_ADMIN,                /*  允许程序进行发现和配对新的蓝牙设备  */
        Manifest.permission.BLUETOOTH_PRIVILEGED,           /*  允许应用程序配对蓝牙设备，而无需用户交互  */
    };
    private String[] StoragePermissions = {
            Manifest.permission.READ_EXTERNAL_STORAGE,
            Manifest.permission.WRITE_EXTERNAL_STORAGE,
    };


    // 动态检查蓝牙权限
    private void checkBluetoothAndLocationPermission(Context context){
        String sTempString;
        // 23:android 6.0(Build.VERSION_CODES.M = 23)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            if ((checkSelfPermission(BlePermissions[0]) != PackageManager.PERMISSION_GRANTED)
                    || (checkSelfPermission(BlePermissions[1]) != PackageManager.PERMISSION_GRANTED)
                    || (checkSelfPermission(BlePermissions[2]) != PackageManager.PERMISSION_GRANTED)
                    ) {
                L.e("BluetoothLe no authority, request it.");
                // 第一个参数是请求的权限集合，第二个参数是请求码，在回调监听中可以用来判断是哪个权限请求的结果
                requestPermissions(BlePermissions,REQUEST_PERMISSION_BLUETOOTH_LE_CODE);

            }else{
                L.e("BluetoothLe permissions check normal.");
            }

            if ((checkSelfPermission(StoragePermissions[0]) != PackageManager.PERMISSION_GRANTED)
                    || (checkSelfPermission(StoragePermissions[1]) != PackageManager.PERMISSION_GRANTED)
                    ) {
                L.e("Storage no authority, request it.");
                // 第一个参数是请求的权限集合，第二个参数是请求码，在回调监听中可以用来判断是哪个权限请求的结果
                requestPermissions(StoragePermissions,REQUEST_PERMISSION_STORAGE);

            }else{
                L.e("Storage permissions check normal.");
            }
        }else{
            sTempString = getResources().getString(R.string.Text_view_operation_permission);
            if(hasPermission(context,BlePermissions[0]) == true)
            {
                sTempString += "  BLUETOOTH";
            }else{
                L.e("No BLUETOOTH permission.");
            }
            if(hasPermission(context,BlePermissions[1]) == true)
            {
                sTempString += "  BLUETOOTH_PRIVILEGED";
            }else{
                L.e("No BLUETOOTH_PRIVILEGED permission.");
            }
            sTempString += "\n";
            if(hasPermission(context,BlePermissions[2]) == true)
            {
                sTempString += "  BLUETOOTH_ADMIN";
            }else{
                L.e("No BLUETOOTH_ADMIN permission.");
            }

            sTempString += "\n";
            if(hasPermission(context,StoragePermissions[0]) == true)
            {
                sTempString += "  READ_EXTERNAL_STORAGE";
            }else{
                L.e("No READ_EXTERNAL_STORAGE permission.");
            }
            if(hasPermission(context,StoragePermissions[1]) == true)
            {
                sTempString += "  WRITE_EXTERNAL_STORAGE";
            }else{
                L.e("No WRITE_EXTERNAL_STORAGE permission.");
            }
            L.i(sTempString);
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if(requestCode == REQUEST_PERMISSION_BLUETOOTH_LE_CODE){
            int grantResult = grantResults[0];
            boolean granted = grantResult == PackageManager.PERMISSION_GRANTED;
            L.i("Bluetooth request permissions result:" + granted);
            if(granted == false){
                // BlePermissionTextView.setText("No permission for BluetoothLe operation.");
            }
        }
        if(requestCode == REQUEST_PERMISSION_STORAGE){
            int grantResult = grantResults[0];
            boolean granted = grantResult == PackageManager.PERMISSION_GRANTED;
            L.i("Storage request permissions result:" + granted);
        }
    }

    public static boolean hasPermission(Context context, String permission){
        int perm = context.checkCallingOrSelfPermission(permission);
        return perm == PackageManager.PERMISSION_GRANTED;
    }

    // 广播通知service当前UI的状态
    private void BroadcastActivityState(final String state) {
        L.i("activity state : " + state);
        BroadcastAction.sendBroadcast(mThis, BroadcastAction.BROADCAST_SERVICE_REC_ACTION_REMOTE_UPGRADE,
                BroadcastAction.BROADCAST_CONTENT_UPGRADE_INFO, state);
    }

    // 检测遥控器是否连接
    private void CheckConnectState(){
        L.i("search and connect remote...");
        BroadcastAction.sendBroadcast(mThis, BroadcastAction.BROADCAST_SERVICE_REC_ACTION_BLUETOOTH,
                BroadcastAction.ROADCAST_CONTENT_BLUETOOTH_GATT_INIT);
    }

    // 读取版本、电量
    private void ReadVersion_Power(){
        L.i("Read remote version and power...");
        BroadcastAction.sendBroadcast(mThis, BroadcastAction.BROADCAST_SERVICE_REC_ACTION_REMOTE_UPGRADE,
                BroadcastAction.BROADCAST_CONTENT_REMOTE_INFO);
    }

    // 启动升级
    private void StartUpgrade(){
        L.i("start remote upgrade...");
        isUpgrading=true;
        BroadcastAction.sendBroadcast(mThis, BroadcastAction.BROADCAST_SERVICE_REC_ACTION_REMOTE_UPGRADE,
                BroadcastAction.BROADCAST_CONTENT_UPGRADE_STAR);
    }

    // 停止升级
    private void StopUpgrade(){
        L.i("stop remote upgrade...");
        isUpgrading=false;
        BroadcastAction.sendBroadcast(mThis, BroadcastAction.BROADCAST_SERVICE_REC_ACTION_REMOTE_UPGRADE,
                BroadcastAction.BROADCAST_CONTENT_UPGRADE_STOP);

        FinishUpgrade(false,"遥控器升级已终止！",null);
    }

    // 升级完成
    private void FinishUpgrade(boolean result, String mainInfo , String auxInfo){

        //下一次检测（间隔大概5s）,如果用户还没按键则自动退出
        if(isUpgradeDone){
            ExitUpgrade();
        }

        isUpgrading=false;
        isUpgradeDone=true;
        isRcvUpgradeKey=false;

        // 更新UI显示内容
        multipleProgressBar.setVisibility(View.GONE);
        TextView_MainDisplay.setText(mainInfo);
        if(TextUtils.isEmpty(auxInfo))
            TextView_GuideDisplay.setVisibility(View.GONE);
        else{
            TextView_GuideDisplay.setText(auxInfo);
            TextView_GuideDisplay.setVisibility(View.VISIBLE);
        }

        if(result) //升级成功图标
            iv_success.setVisibility(View.VISIBLE);
        else       //升级失败图标
            iv_fail.setVisibility(View.VISIBLE);

        // 如果UI被转后台则通过toast提示升级信息
        if(!isUIOnTheTop){
            Utils.ToastShow(getApplicationContext(), Toast.LENGTH_LONG, Gravity.TOP, mainInfo, auxInfo);
        }
        L.i("Upgrade finish : "+result);
    }

    // 退出
    private void ExitUpgrade(){
        L.i("Program exit.");
        BroadcastActivityState("ui stopped");// 发送UI停止广播告知service
        finish();
        System.exit(0);
    }
}
