package com.lxl.nanosic.app.ble;

import android.content.Context;
import android.content.Intent;

public class BroadcastAction {
    private static boolean isEnable = true;         // 打开或关闭发送广播
    //==================================================================================================
    //  广播第一项，后面的字符串表明第一项传输的数据类型
    public final static String BROADCAST_VALUE_TYPE = "com.nanosic.wnf1x0upgrade.BROADCAST_VALUE_TYPE";
    //==================================================================================================
    //  广播第一项传输的数据类型，是一个string还是一个int
    public final static String BROADCAST_VALUE_INT = "com.nanosic.wnf1x0upgrade.BROADCAST_VALUE_INT";
    public final static String BROADCAST_VALUE_STRING = "com.nanosic.wnf1x0upgrade.BROADCAST_VALUE_STRING";
    public final static String BROADCAST_VALUE_STRING_INT = "com.nanosic.wnf1x0upgrade.BROADCAST_VALUE_STRING_INT";
    public final static String BROADCAST_VALUE_STRING_STRING = "com.nanosic.wnf1x0upgrade.BROADCAST_VALUE_STRING_STRING";
    //==================================================================================================
    //  广播第二项，后面表明广播传播的内容，具体是String还是int由第一项决定
    public final static String BROADCAST_VALUE_CONTENT_STRING = "com.nanosic.wnf1x0upgrade.BROADCAST_VALUE_CONTENT_STRING";
    public final static String BROADCAST_VALUE_CONTENT_INT = "com.nanosic.wnf1x0upgrade.BROADCAST_VALUE_CONTENT_INT";
    public final static String BROADCAST_VALUE_CONTENT_STRING_AUX = "com.nanosic.wnf1x0upgrade.BROADCAST_VALUE_CONTENT_STRING_AUX";
    //==================================================================================================
    //  广播Action，表明有几类广播，接收广播时需要注册这几类广播
    // 通用项操作相关广播,未实际定义的都放在这个里面
    // 所有的SEND和REC都是针对BluetoothLeService的。
    public final static String BROADCAST_SERVICE_SEND_ACTION_GENERAL = "com.nanosic.wnf1x0upgrade.BROADCAST_SERVICE_SEND_ACTION_GENERAL";
    public final static String BROADCAST_SERVICE_REC_ACTION_GENERAL = "com.nanosic.wnf1x0upgrade.BROADCAST_SERVICE_REC_ACTION_GENERAL";

    // service相关广播，表明service的启动、关闭、状态等
    public final static String BROADCAST_SERVICE_SEND_ACTION_SERVICE = "com.nanosic.wnf1x0upgrade.BROADCAST_SERVICE_SEND_ACTION_SERVICE";
    public final static String BROADCAST_SERVICE_REC_ACTION_SERVICE = "com.nanosic.wnf1x0upgrade.BROADCAST_SERVICE_REC_ACTION_SERVICE";

    // Update相关广播，表明启动升级、进级进程、升级成功、失败等
    public final static String BROADCAST_SERVICE_SEND_ACTION_REMOTE_UPGRADE = "com.nanosic.wnf1x0upgrade.BROADCAST_SERVICE_SEND_ACTION_REMOTE_UPGRADE";
    public final static String BROADCAST_SERVICE_REC_ACTION_REMOTE_UPGRADE = "com.nanosic.wnf1x0upgrade.BROADCAST_SERVICE_REC_ACTION_REMOTE_UPGRADE";
    public final static String BROADCAST_SERVICE_SEND_ACTION_DONGLE_UPGRADE = "com.nanosic.wnf1x0upgrade.BROADCAST_SERVICE_SEND_ACTION_DONGLE_UPGRADE";
    public final static String BROADCAST_SERVICE_REC_ACTION_DONGLE_UPGRADE = "com.nanosic.wnf1x0upgrade.BROADCAST_SERVICE_REC_ACTION_DONGLE_UPGRADE";

    // USB操作相关广播，表明USB找到设备，关闭设备等
    public final static String BROADCAST_SERVICE_SEND_ACTION_USB = "com.nanosic.wnf1x0upgrade.BROADCAST_SERVICE_SEND_ACTION_USB";
    public final static String BROADCAST_SERVICE_REC_ACTION_USB = "com.nanosic.wnf1x0upgrade.BROADCAST_SERVICE_REC_ACTION_USB";

    // 蓝牙相关操作
    public final static String BROADCAST_SERVICE_SEND_ACTION_BLUETOOTH = "com.nanosic.wnf1x0upgrade.BROADCAST_SERVICE_SEND_ACTION_BLUETOOTH";
    public final static String BROADCAST_SERVICE_REC_ACTION_BLUETOOTH = "com.nanosic.wnf1x0upgrade.BROADCAST_SERVICE_REC_ACTION_BLUETOOTH";

    //  File操作，控制service动作
    public final static String BROADCAST_SERVICE_SEND_ACTION_FILE_OPERATION = "com.nanosic.wnf1x0upgrade.BROADCAST_SERVICE_SEND_ACTION_FILE_OPERATION";
    public final static String BROADCAST_SERVICE_REC_ACTION_FILE_OPERATION = "com.nanosic.wnf1x0upgrade.BROADCAST_SERVICE_REC_ACTION_FILE_OPERATION";
    //==================================================================================================
    //  BROADCAST_ACTION_SERVICE相关广播项
    public final static String BROADCAST_CONTENT_SERVICE_ON_CREATED = "com.nanosic.wnf1x0upgrade.BROADCAST_CONTENT_SERVICE_ON_CREATED";
    public final static String BROADCAST_CONTENT_SERVICE_ON_BIND = "com.nanosic.wnf1x0upgrade.BROADCAST_CONTENT_SERVICE_ON_BIND";
    public final static String BROADCAST_CONTENT_SERVICE_ON_DESTROY = "com.nanosic.wnf1x0upgrade.BROADCAST_CONTENT_SERVICE_ON_DESTROY";
    public final static String BROADCAST_CONTENT_SERVICE_UNBIND = "com.nanosic.wnf1x0upgrade.BROADCAST_CONTENT_SERVICE_UNBIND";


    //==================================================================================================
    //  BROADCAST_SEND_ACTION_OTA相关广播项
    public final static String BROADCAST_CONTENT_UPGRADE_INFO = "com.nanosic.wnf1x0upgrade.BROADCAST_CONTENT_UPGRADE_INFO";
    public final static String BROADCAST_CONTENT_UPGRADE_STAR = "com.nanosic.wnf1x0upgrade.BROADCAST_CONTENT_UPGRADE_STAR";
    public final static String BROADCAST_CONTENT_UPGRADE_STOP = "com.nanosic.wnf1x0upgrade.BROADCAST_CONTENT_UPGRADE_STOP";
    public final static String BROADCAST_CONTENT_REMOTE_VERSION = "com.nanosic.wnf1x0upgrade.BROADCAST_CONTENT_REMOTE_VERSION";
    public final static String BROADCAST_CONTENT_REMOTE_INFO = "com.nanosic.wnf1x0upgrade.BROADCAST_CONTENT_REMOTE_INFO";
    public final static String BROADCAST_CONTENT_REMOTE_POWER = "com.nanosic.wnf1x0upgrade.BROADCAST_CONTENT_REMOTE_POWER";
    public final static String BROADCAST_CONTENT_UPGRADE_PROCESS = "com.nanosic.wnf1x0upgrade.BROADCAST_CONTENT_UPGRADE_PROCESS";
    public final static String BROADCAST_CONTENT_UPGRADE_COMPLETE = "com.nanosic.wnf1x0upgrade.BROADCAST_CONTENT_UPGRADE_COMPLETE";
    public final static String BROADCAST_CONTENT_UPGRADE_ERR_INFO = "com.nanosic.wnf1x0upgrade.BROADCAST_CONTENT_UPGRADE_ERR_INFO";
    public final static String BROADCAST_CONTENT_UPGRADE_ERR_CODE = "com.nanosic.wnf1x0upgrade.BROADCAST_CONTENT_UPGRADE_ERR_CODE";
    public final static String BROADCAST_CONTENT_UPGRADE_SELF_STAR = "com.nanosic.wnf1x0upgrade.BROADCAST_CONTENT_UPGRADE_SELF_STAR";
    public final static String BROADCAST_CONTENT_UPGRADE_GUIDE_STAR = "com.nanosic.wnf1x0upgrade.BROADCAST_CONTENT_UPGRADE_GUIDE_STAR";
    public final static String BROADCAST_CONTENT_UPGRADE_FILE_PATH = "com.nanosic.wnf1x0upgrade.BROADCAST_CONTENT_UPGRADE_GUIDE_STAR";

    //==================================================================================================
    // BROADCAST_SERVICE_SEND_ACTION_BLUETOOTH
    public final static String BROADCAST_CONTENT_BLUETOOTH_INFO = "com.nanosic.wnf1x0upgrade.BROADCAST_CONTENT_BLUETOOTH_INFO";
    public final static String BROADCAST_CONTENT_BLUETOOTH_GATT_CONNECTED = "com.nanosic.wnf1x0upgrade.BROADCAST_CONTENT_BLUETOOTH_GATT_CONNECTED";
    public final static String BROADCAST_CONTENT_BLUETOOTH_GATT_DISCONNECTED = "com.nanosic.wnf1x0upgrade.BROADCAST_CONTENT_BLUETOOTH_GATT_DISCONNECTED";
    public final static String BROADCAST_CONTENT_BLUETOOTH_GATT_DISCOVERED = "com.nanosic.wnf1x0upgrade.BROADCAST_CONTENT_BLUETOOTH_GATT_DISCOVERED";
    public final static String BROADCAST_CONTENT_BLUETOOTH_GATT_INIT = "com.nanosic.wnf1x0upgrade.BROADCAST_CONTENT_BLUETOOTH_GATT_INIT";
    public final static String BROADCAST_CONTENT_BLUETOOTH_DEV_VIDPID = "com.nanosic.wnf1x0upgrade.BROADCAST_CONTENT_BLUETOOTH_DEV_VIDPID";
    public final static String BROADCAST_CONTENT_BLUETOOTH_DEV_PROTOCOL = "com.nanosic.wnf1x0upgrade.BROADCAST_CONTENT_BLUETOOTH_DEV_PROTOCOL";

    //==================================================================================================
    //  BROADCAST_SERVICE_SEND_ACTION_FILE_OPERATION 相关广播项
    public final static String ROADCAST_CONTENT_REMOTE_FILE_INFO = "com.nanosic.wnf1x0upgrade.ROADCAST_CONTENT_REMOTE_FILE_INFO";
    public final static String ROADCAST_CONTENT_REMOTE_FILE_VERSION = "com.nanosic.wnf1x0upgrade.ROADCAST_CONTENT_REMOTE_FILE_VERSION";
    public final static String ROADCAST_CONTENT_DONGLE_FILE_VERSION = "com.nanosic.wnf1x0upgrade.ROADCAST_CONTENT_DONGLE_FILE_VERSION";
    public final static String ROADCAST_CONTENT_DONGLE_FILE_PATH = "com.nanosic.wnf1x0upgrade.ROADCAST_CONTENT_DONGLE_FILE_PATH";
    public final static String ROADCAST_CONTENT_REMOTE_FILE_PATH = "com.nanosic.wnf1x0upgrade.ROADCAST_CONTENT_REMOTE_FILE_PATH";
    public final static String BROADCAST_CONTENT_UPGRADE_DOWNLOAD_FILE = "com.nanosic.www.wnf1x0.remoteupgrade.BROADCAST_CONTENT_UPGRADE_DOWNLOAD_FILE";
    public final static String ROADCAST_CONTENT_REMOTE_BATTERY = "com.nanosic.usbupdate.ROADCAST_CONTENT_REMOTE_BATTERY";
    public final static String ROADCAST_CONTENT_DONGLE_VERSION = "com.nanosic.usbupdate.ROADCAST_CONTENT_DONGLE_VERSION";


    //==================================================================================================
    //  USB 相关广播项
    public final static String BROADCAST_CONTENT_USB_OPEN = "com.nanosic.usbupdate.BROADCAST_CONTENT_USB_OPEN";
    public final static String BROADCAST_CONTENT_USB_CLOSE = "com.nanosic.usbupdate.BROADCAST_CONTENT_USB_CLOSE";
    public final static String BROADCAST_CONTENT_USB_SUCCESS = "com.nanosic.usbupdate.BROADCAST_CONTENT_USB_SUCCESS";
    public final static String BROADCAST_CONTENT_USB_ERR = "com.nanosic.usbupdate.BROADCAST_CONTENT_USB_ERR";

    // Service相关的广播描述
    public final static String BLUETOOTH_SERVICE_BROADCAST = "nanoic.bluetoothle.service.BLUETOOTH_SERVICE_BROADCAST";
    // Bluetooth相关的广播描述
    public final static String BLUETOOTH_OPERATION_BROADCAST = "nanoic.bluetoothle.service.BLUETOOTH_OPERATION_BROADCAST";
    // Remote OAD相关的广播描述
    public final static String REMOTE_OAD_OPERATION_BROADCAST = "nanoic.bluetoothle.service.REMOTE_OAD_OPERATION_BROADCAST";
    // 未定义相关的广播描述
    public final static String NO_DEFINE_OPERATION_BROADCAST = "nanoic.bluetoothle.service.NO_DEFINE_OPERATION_BROADCAST";
    //---------------------------------------------------
    // 广播项
    public final static String SERVICE_BROADCAST_DESCRIBE = "nanoic.bluetoothle.service.SERVICE_BROADCAST_DESCRIBE";
    public final static String SERVICE_BROADCAST_INT_VALUE = "nanoic.bluetoothle.service.SERVICE_BROADCAST_INT_VALUE";
    public final static String SERVICE_BROADCAST_STRING_VALUE = "nanoic.bluetoothle.service.SERVICE_BROADCAST_STRING_VALUE";
    //--------------------------------------------------
    public final static String BROADCAST_DESCRIBE_SERVICE_STAR = "nanoic.bluetoothle.service.BROADCAST_DESCRIBE_SERVICE_STAR";
    public final static String BROADCAST_DESCRIBE_SERVICES_STOP = "nanoic.bluetoothle.service.BROADCAST_DESCRIBE_SERVICES_STOP";
    public final static String BROADCAST_DESCRIBE_GATT_CONNECTED = "nanoic.bluetoothle.service.BROADCAST_DESCRIBE_GATT_CONNECTED";
    public final static String BROADCAST_DESCRIBE_GATT_DISCONNECTED = "nanoic.bluetoothle.service.BROADCAST_DESCRIBE_GATT_DISCONNECTED";
    public final static String BROADCAST_DESCRIBE_GATT_DISCOVERED = "nanoic.bluetoothle.service.BROADCAST_DESCRIBE_GATT_DISCOVERED";
    public final static String BROADCAST_DESCRIBE_REMOTE_OAD_FAILURE = "nanoic.bluetoothle.service.BROADCAST_DESCRIBE_REMOTE_OAD_FAILURE";
    public final static String BROADCAST_DESCRIBE_REMOTE_OAD_SUCCESS = "nanoic.bluetoothle.service.BROADCAST_DESCRIBE_REMOTE_OAD_SUCCESS";
    public final static String BROADCAST_DESCRIBE_REMOTE_OAD_PROGRESS = "nanoic.bluetoothle.service.BROADCAST_DESCRIBE_REMOTE_OAD_PROGRESS";
    public final static String BROADCAST_DESCRIBE_REMOTE_OAD_START = "nanoic.bluetoothle.service.BROADCAST_DESCRIBE_REMOTE_OAD_START";

    public final static String BROADCAST_DESCRIBE_REMOTE_OTA_FAILED = "nanoic.bluetoothle.service.BROADCAST_DESCRIBE_REMOTE_OTA_FAILED";

    public final static String BROADCAST_DESCRIBE_REMOTE_OAD_VERSION = "nanoic.bluetoothle.service.BROADCAST_DESCRIBE_REMOTE_OAD_VERSION";
    public final static String BROADCAST_DESCRIBE_REMOTE_OAD_FILE_VERSION = "nanoic.bluetoothle.service.BROADCAST_DESCRIBE_REMOTE_OAD_FILE_VERSION";
    public final static String BROADCAST_DESCRIBE_REMOTE_OAD_FILE_PATH = "nanoic.bluetoothle.service.BROADCAST_DESCRIBE_REMOTE_OAD_FILE_PATH";
    public final static String BROADCAST_DESCRIBE_REMOTE_OAD_POWER = "nanoic.bluetoothle.service.BROADCAST_DESCRIBE_REMOTE_OAD_POWER";

    public final static String BROADCAST_DREMOTE_OAD_FAILURE_TIMEOUT = "nanoic.bluetoothle.service.BROADCAST_DREMOTE_OAD_FAILURE_TIMEOUT";
    //---------------------------------------------------
    // 和外界交互的广播
    // 启动OTA升级
    public final static String BLUETOOTH_LE_BROADCAST_START_OTA = "nanoic.bluetoothle.service.BLUETOOTH_LE_BROADCAST_START_OTA";
    public final static String BLUETOOTH_LE_BROADCAST_STOP_OTA = "nanoic.bluetoothle.service.BLUETOOTH_LE_BROADCAST_STOP_OTA";
    public final static String BLUETOOTH_LE_SERVICE_STOP = "nanoic.bluetoothle.service.BLUETOOTH_LE_SERVICE_STOP";
    public final static String BLUETOOTH_LE_SERVICE_RESET = "nanoic.bluetoothle.service.BLUETOOTH_LE_SERVICE_RESET";
    public final static String BLUETOOTH_LE_BROADCAST_READ_REMOTE_VER_POWER = "nanoic.bluetoothle.service.BLUETOOTH_LE_BROADCAST_READ_REMOTE_VER_POWER";
    public final static String BLUETOOTH_LE_BROADCAST_USB_REMOTE = "nanoic.bluetoothle.service.BLUETOOTH_LE_BROADCAST_USB_REMOTE";

    public final static String BLUETOOTH_LE_POWERON_BROADCAST_START_OTA = "nanoic.bluetoothle.service.BLUETOOTH_LE_POWER_ON_BROADCAST_START_OTA";

    //---------------------------------------------------
    public final static String ACTION_CONNECTION_STATE_CHANGED = "android.bluetooth.adapter.action.CONNECTION_STATE_CHANGED";
    public final static String ACL_CONNECTED = "android.bluetooth.device.action.ACL_CONNECTED";
    public final static String ACL_DISCONNECTED = "android.bluetooth.device.action.ACL_DISCONNECTED";

    // Service停止原因
    public final static String BLUETOOTH_LE_SERVICE_STOP_READ_VER_TOUT = "Read program version timeout";
    public final static String BLUETOOTH_LE_SERVICE_STOP_READ_BATTERY_TOUT = "Read battery timeout";
    public final static String BLUETOOTH_LE_SERVICE_STOP_READ_NO_NEED = "Do not need to upgrade or Battery low";


    // 主UI相关广播
    public final static String MAIN_UPDATE_APK_CHECK = "nanosic.ota.app.MAIN_UPDATE_APK_CHECK";
    public final static String MAIN_UPDATE_APK_SELECT = "nanosic.ota.app.MAIN_UPDATE_APK_SELECT";
    public final static String MAIN_UPDATE_APK_DOWNLOAD = "nanosic.ota.app.MAIN_UPDATE_APK_DOWNLOAD";
    public final static String MAIN_UPDATE_APK_DOWNLOADING = "nanosic.ota.app.MAIN_UPDATE_APK_DOWNLOADING";
    public final static String MAIN_UPDATE_APK_DOWNLOADFAILED = "nanosic.ota.app.MAIN_UPDATE_APK_DOWNLOADFAILED";

    public static void enableBroadcast(boolean loggable) {
        BroadcastAction.isEnable = loggable;
    }

    private static boolean isBroadcastEnable() {
        return isEnable;
    }
    //==================================================================================================

    public static void sendBroadcast(Context myContext, final String action, String sval) {
        final Intent intent = new Intent(action);
        intent.putExtra(BROADCAST_VALUE_TYPE, BROADCAST_VALUE_STRING);
        intent.putExtra(BROADCAST_VALUE_CONTENT_STRING, sval);
        myContext.sendBroadcast(intent);
    }

    public static void sendBroadcast(Context myContext, final String action, int val) {
        final Intent intent = new Intent(action);
        intent.putExtra(BROADCAST_VALUE_TYPE, BROADCAST_VALUE_INT);
        intent.putExtra(BROADCAST_VALUE_CONTENT_INT, val);
        myContext.sendBroadcast(intent);
    }

    public static void sendBroadcast(Context myContext, final String action, String sval, int val) {
        final Intent intent = new Intent(action);
        intent.putExtra(BROADCAST_VALUE_TYPE, BROADCAST_VALUE_STRING_INT);
        intent.putExtra(BROADCAST_VALUE_CONTENT_STRING, sval);
        intent.putExtra(BROADCAST_VALUE_CONTENT_INT, val);
        myContext.sendBroadcast(intent);
    }

    public static void sendBroadcast(Context myContext, final String action, String sval, String saux) {
        final Intent intent = new Intent(action);
        intent.putExtra(BROADCAST_VALUE_TYPE, BROADCAST_VALUE_STRING_STRING);
        intent.putExtra(BROADCAST_VALUE_CONTENT_STRING, sval);
        intent.putExtra(BROADCAST_VALUE_CONTENT_STRING_AUX, saux);
        myContext.sendBroadcast(intent);
    }
    //==============================================================================================


}
