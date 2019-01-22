package com.lxl.nanosic.app;

import android.Manifest;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.ServiceConnection;
import android.content.pm.PackageManager;

import android.net.Uri;
import android.os.Build;
import android.os.Handler;
import android.os.IBinder;
import android.provider.Settings;
import android.support.annotation.NonNull;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v4.widget.SwipeRefreshLayout;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;

import com.lxl.nanosic.app.ble.BluetoothLeService;
import com.lxl.nanosic.app.ble.BroadcastAction;
import com.lxl.nanosic.app.ble.Config;
import com.lxl.nanosic.app.ui.DrawableSwitch;
import com.lxl.nanosic.app.ui.SelectDialogFragment;
import com.lxl.nanosic.app.ui.UpgradeDevInfoFragment;
import com.lxl.nanosic.app.ui.UpgradeLocalFragment;
import com.lxl.nanosic.app.ui.UpgradeOnlineFragment;


public class MainActivity extends AppCompatActivity implements View.OnClickListener {

    private SwipeRefreshLayout mSwipeRefresh;
    private TextView mSwipeRefreshText;
    private Button mBtnLocal;
    private Button mBtnOnline;
    private Button mBtnDevInfo;
    private DrawableSwitch mDrawableSwitch;
    private TextView mTvCopyrightVer;
    private TextView mDevInfo;

    private final int OTA_PERMISSION_REQUEST_CODE = 10000;
    private final String[] strPermissions  = new String[] {
            //Manifest.permission.CAMERA,
            //Manifest.permission.RECORD_AUDIO,
            Manifest.permission.READ_EXTERNAL_STORAGE,
            Manifest.permission.WRITE_EXTERNAL_STORAGE
    };
    
    private UpgradeLocalFragment fragment_Local;
    private UpgradeOnlineFragment fragment_Online;
    private UpgradeDevInfoFragment fragment_ShowOtaInfo;

    private boolean isDownloading = false;

    //private SSLClient sslClient;

    private static MainActivity mApplication = null;

    public static MainActivity getInstance(){
        return mApplication;
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        mApplication = this;
        setContentView(R.layout.activity_main);

        /** 判断SDK版本，确认是否动态申请权限 **/
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            /** 第 1 步: 检查是否有相应的权限 **/
            if(checkPermissionAllGranted(strPermissions)==false) {
                /** 第 2 步: 请求权限,一次请求多个权限, 如果其他有权限是已经授予的将会自动忽略掉 **/
                ActivityCompat.requestPermissions(
                        this,
                        strPermissions,
                        OTA_PERMISSION_REQUEST_CODE
                );
            } else {
                // TODO:3s等待,检测是否已经在下载新版的安装包，没有则发送版本检测广播包
                new Handler().postDelayed(new Runnable(){
                    public void run() {
                        if(!isDownloading){
                            L.d("===sendBroadcast MAIN_UPDATE_APK_CHECK");
                            BroadcastAction.sendBroadcast(getApplicationContext(),
                                    BroadcastAction.MAIN_UPDATE_APK_CHECK,
                                    "check version");
                        }
                    }
                }, 3000);
            }

            /** 第 3 步: 判断权限申请结果，如用户未同意则引导至设置界面打开权限 **/
            int[] grantResults={0};
            onRequestPermissionsResult(OTA_PERMISSION_REQUEST_CODE,strPermissions,grantResults);

        }else{  //小于Android6.0的版本直接检测版本（无需手动授权sdcard读写权限）
            // TODO:3s等待,检测是否已经在下载新版的安装包，没有则发送版本检测广播包
            new Handler().postDelayed(new Runnable(){
                public void run() {
                    if(!isDownloading){
                        L.d("===sendBroadcast MAIN_UPDATE_APK_CHECK");
                        BroadcastAction.sendBroadcast(getApplicationContext(),
                                BroadcastAction.MAIN_UPDATE_APK_CHECK,
                                "check version");
                    }
                }
            }, 3000);
        }

        //TODO : 初始化C层进程，可移除
        //JniApiCall.jni_NanoOpen();

        fragment_Local = UpgradeLocalFragment.newInstance("Local", null,null); //本地升级界面
        fragment_Online = new UpgradeOnlineFragment(); //在线升级界面
        fragment_ShowOtaInfo = UpgradeDevInfoFragment.newInstance(); //设备信息界面

        /** 创建和服务器的SSL连接 */
        //sslClient = new SSLClient(getApplicationContext());

        /** 绑定服务 */
        BinderService();

        /** 注册广播 */
        RegisterBroadcastReceiver();

        /** 添加监听 */
        mBtnLocal = findViewById(R.id.otaLocal);
        mBtnLocal.setOnClickListener(this);

        mBtnOnline = findViewById(R.id.otaOnline);
        mBtnOnline.setOnClickListener(this);

        mBtnDevInfo = findViewById(R.id.devInfo);
        mBtnDevInfo.setOnClickListener(this);

        mDevInfo = findViewById(R.id.DeviceInfo);

        /** 设置加密开关 */
        mDrawableSwitch = findViewById(R.id.drawableSwitch);
        mDrawableSwitch.setListener(new DrawableSwitch.MySwitchStateChangeListener() {
            @Override
            public void mySwitchStateChanged(boolean isSwitchOn)
            {
                Config.SetEncryptState(isSwitchOn);
            }
        });

        /** 下拉刷新控件 */
        mSwipeRefreshText = findViewById(R.id.swipeRefreshText);
        mSwipeRefresh = findViewById(R.id.swipeRefreshLayoutV4);
        mSwipeRefresh.setColorSchemeResources(android.R.color.holo_blue_light,
                android.R.color.holo_green_light, android.R.color.darker_gray);
        //给SwipeRefreshLayout绑定刷新监听
        mSwipeRefresh.setOnRefreshListener(new SwipeRefreshLayout.OnRefreshListener() {
            @Override
            public void onRefresh() {
                mSwipeRefreshText.setVisibility(View.VISIBLE);
                L.d("===sendBroadcast BROADCAST_CONTENT_BLUETOOTH_GATT_INIT");
                // 发送初始化广播
                BroadcastAction.sendBroadcast(getApplicationContext(), BroadcastAction.BROADCAST_SERVICE_REC_ACTION_BLUETOOTH,
                        BroadcastAction.BROADCAST_CONTENT_BLUETOOTH_GATT_INIT,
                        "FORCE");

                /** 5s超时停止刷新 */
                new Handler().postDelayed(new Runnable(){
                    public void run() {
                        mSwipeRefreshText.setVisibility(View.GONE);
                        mSwipeRefresh.setRefreshing(false);
                    }
                }, 5000);
            }
        });

        /** 文本显示版权和应用版本信息 */
        String copyRightStr = getResources().getString(R.string.copyright_info);
        String versionText = getResources().getString(R.string.app_version);
        String appVersion = Utils.getAppVersionName(getApplicationContext());
        mTvCopyrightVer = findViewById(R.id.copyrightVer);
        mTvCopyrightVer.setText(copyRightStr+"\n"+versionText + appVersion);

        /** 发送广播,获得遥控器名字和MAC地址 */
        new Handler().postDelayed(new Runnable(){
                public void run() {
                if(!isDownloading){
                    L.d("===sendBroadcast BROADCAST_CONTENT_BLUETOOTH_GATT_INIT");
                    BroadcastAction.sendBroadcast(getApplicationContext(), BroadcastAction.BROADCAST_SERVICE_REC_ACTION_BLUETOOTH,
                            BroadcastAction.BROADCAST_CONTENT_BLUETOOTH_GATT_INIT);
                }
            }
        }, 100);
    }

    @Override
    protected void onDestroy() {
        //sslClient.SSL_Close();
        UnRegisterBroadcastReceiver();
        UnBinderService();
        super.onDestroy();
    }

    /**
     * 检查是否拥有指定的所有权限
     */
    private boolean checkPermissionAllGranted(String[] permissions) {
        for (String permission : permissions) {
            if (ContextCompat.checkSelfPermission(this, permission) != PackageManager.PERMISSION_GRANTED) {
                // 只要有一个权限没有被授予, 则直接返回 false
                return false;
            }
        }
        return true;
    }

    /**
     * 打开 APP 的详情设置
     */
    private void openAppDetails() {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setMessage(getResources().getString(R.string.permission_guide));
        builder.setPositiveButton(getResources().getString(R.string.permission_manual), new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                Intent intent = new Intent();
                intent.setAction(Settings.ACTION_APPLICATION_DETAILS_SETTINGS);
                intent.addCategory(Intent.CATEGORY_DEFAULT);
                intent.setData(Uri.parse("package:" + getPackageName()));
                intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                intent.addFlags(Intent.FLAG_ACTIVITY_NO_HISTORY);
                intent.addFlags(Intent.FLAG_ACTIVITY_EXCLUDE_FROM_RECENTS);
                startActivity(intent);
            }
        });
        builder.setNegativeButton(getResources().getString(R.string.permission_cancel), null);
        builder.show();
    }

    /**
     * 申请权限结果返回处理
     */
    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);

        if (requestCode == OTA_PERMISSION_REQUEST_CODE) {
            boolean isAllGranted = true;

            // 判断是否所有的权限都已经授予了
            for (int grant : grantResults) {
                if (grant != PackageManager.PERMISSION_GRANTED) {
                    isAllGranted = false;
                    break;
                }
            }

            if (!isAllGranted) {
                // 弹出对话框告诉用户需要权限的原因, 并引导用户去应用权限管理中手动打开权限按钮
                openAppDetails();
            }
        }
    }

    // 按键监听
    @Override
    public void onClick(View view) {

        // 防止短时间连续点击产生多个DialogFragment
        if(Utils.isFastDoubleClick()){
            L.w("Click too fast!!!");
            return;
        }

        switch (view.getId()){
            case R.id.otaLocal:

                // 显示本地升级界面
                fragment_Local.show(getFragmentManager(), "local"); //本地升级界面

                //TODO : SSL调试部分可移除
                /*
                try {
                    // c层测试服务器接口，可移除
                    byte[] testData = "[sheldon]:test server!".getBytes();
                    //JniApiCall.jni_NanoLogin(testData,testData.length);

                    // 创建json数据包
                    JSONObject jsonObject = new JSONObject();
                    jsonObject.put("Model", "KK309");
                    jsonObject.put("Vid", "0x1234");
                    jsonObject.put("Pid", "0x5678");
                    jsonObject.put("Version", 99);

                    // 发送数据
                    //sslClient.sendMessageToServer(jsonObject.toString(1));
                } catch (JSONException e) {
                    e.printStackTrace();
                }
                */

                break;

            case R.id.otaOnline:
                // 显示在线升级界面
                fragment_Online.show(getFragmentManager(), "online"); //在线升级界面
                break;

            case R.id.devInfo:
                // 显示升级信息
                fragment_ShowOtaInfo.show(getFragmentManager(), "otaInfo"); // 升级信息界面
                break;
        }
    }


    //==============================================================================================
    // 启动BLE操作service
    //==============================================================================================
    BluetoothLeService localService = null;
    private ServiceConnection localServiceConnection = null;

    //用bindService方法启动服务
    private void BinderService() {
        localServiceConnection = new ServiceConnection() {
            @Override
            public void onServiceConnected(ComponentName componentName, IBinder binder) {
                //调用bindService方法启动服务时候，如果服务需要与activity交互，
                //则通过onBind方法返回IBinder并返回当前本地服务
                localService = ((BluetoothLeService.LocalBinder) binder).getService();
            }
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

    /**
     * 自定义广播接收器
     */
    private MainBroadcastReceiver MainBR = new MainBroadcastReceiver();
    private void RegisterBroadcastReceiver(){
        IntentFilter mFilter = new IntentFilter(BroadcastAction.MAIN_UPDATE_APK_CHECK);
        mFilter.addAction(BroadcastAction.MAIN_UPDATE_APK_DOWNLOAD);
        registerReceiver(MainBR, mFilter);
        L.i("Register MainBroadcastReceiver");

        // Register the BroadcastReceiver
        IntentFilter activityFilter = new IntentFilter(BroadcastAction.MAIN_UPDATE_APK_SELECT);
        activityFilter.addAction(BroadcastAction.MAIN_UPDATE_APK_DOWNLOADING);
        activityFilter.addAction(BroadcastAction.MAIN_UPDATE_APK_DOWNLOADFAILED);
        activityFilter.addAction(BroadcastAction.BROADCAST_SERVICE_SEND_ACTION_BLUETOOTH);
        registerReceiver(MainActivityReceiver, activityFilter);
        L.i("Register MainActivityReceiver");
    }

    private void UnRegisterBroadcastReceiver(){
        try {
            if(MainBR != null) {
                unregisterReceiver(MainBR);
                L.i("Unregister MainBroadcastReceiver");
            }
            if(MainActivityReceiver != null) {
                unregisterReceiver(MainActivityReceiver);
                L.i("Unregister MainActivityReceiver");
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    /** 主界面收到的广播 */
    private BroadcastReceiver MainActivityReceiver = new BroadcastReceiver() {
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

            // 服务器检测到新版本，提示用户升级
            if(action.equals(BroadcastAction.MAIN_UPDATE_APK_SELECT)){
                // 获得项目名和版本号参数
                String projectName = sbroad_value;
                String versionStr = sbroad_aux_val;

                // TODO:本来想把升级选择界面放到MainBroadcastReceiver中处理，但是无法getFragmentManager()
                // 根据服务器版本检测结果，进行下载(网络正常)还是本地扫描(网络异常)
                SelectDialogFragment fragment_Select = SelectDialogFragment.newInstance(projectName, versionStr);
                fragment_Select.show(getFragmentManager(),"select");
            }
            else if(action.equals(BroadcastAction.MAIN_UPDATE_APK_DOWNLOADING)){
                String state = sbroad_value;
                int curProgress = ibroad_value;
                isDownloading = state.equals("true"); // 正在下载升级文件标志位

                // 通知栏显示下载进度,内部已封装兼容低版本
                /*
                if(curProgress > 0){
                    Utils.showNotification(getResources().getString(R.string.app_update),
                            getResources().getString(R.string.app_download_progress),
                            0x3,"0x1",curProgress,100);
                    L.d("Download progress:"+curProgress+"%");
                }
                */
            }
            else if(action.equals(BroadcastAction.MAIN_UPDATE_APK_DOWNLOADFAILED)){
                /*
                Utils.showNotification(getResources().getString(R.string.app_update),
                        getResources().getString(R.string.app_download_failed),
                        0x3,"0x1",-1,100);
                */
                L.e("安装包下载失败！");
                isDownloading = false;
            }else if(action.equals(BroadcastAction.BROADCAST_SERVICE_SEND_ACTION_BLUETOOTH)){
                L.w("Update BLE device info : "+sbroad_value+"---"+sbroad_aux_val);
                if (sbroad_value.equals(BroadcastAction.BROADCAST_CONTENT_BLUETOOTH_GATT_CONNECTED)) {

                } else if (sbroad_value.equals(BroadcastAction.BROADCAST_CONTENT_BLUETOOTH_GATT_DISCOVERED)) {
                    mSwipeRefreshText.setVisibility(View.GONE);
                    mSwipeRefresh.setRefreshing(false);
                    mDevInfo.setText(sbroad_aux_val);
                } else if (sbroad_value.equals(BroadcastAction.BROADCAST_CONTENT_BLUETOOTH_GATT_DISCONNECTED)) {
                    mSwipeRefreshText.setVisibility(View.GONE);
                    mSwipeRefresh.setRefreshing(false);
                    mDevInfo.setText("");
                } else if (sbroad_value.equals(BroadcastAction.BROADCAST_CONTENT_BLUETOOTH_DEV_PROTOCOL)) {

                } else if (sbroad_value.equals(BroadcastAction.BROADCAST_CONTENT_BLUETOOTH_DEV_VIDPID)) {

                }
            }
        }
    };
}
