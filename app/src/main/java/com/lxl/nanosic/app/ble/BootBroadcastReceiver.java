package com.lxl.nanosic.app.ble;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;

import com.lxl.nanosic.app.L;
import com.lxl.nanosic.app.Utils;


public class BootBroadcastReceiver extends BroadcastReceiver {

    static final String action_boot  = "android.intent.action.BOOT_COMPLETED";
    static final String action_start = "rtk.ota.broadcast";
    static final String serviceName  = "com.lxl.nanosic.app.ble.BluetoothLeService";
    private static boolean isSTRunning=false;
    private Context myContext;

    @Override
    public void onReceive(Context context, Intent intent) {

        String action = intent.getAction();
        byte   bValue = intent.getByteExtra(action, (byte) 1);
        L.i("Receive broadcast.Action:" + action + ", value type:"+ bValue);
        L.i("Component:" + intent.getComponent().toString());
        myContext = context;

        if (action.equals(action_boot)) {
            // Create thread, bind BLE Service
            new Thread(new Runnable() {
                @Override
                public void run() {
                    try {
                        if(!isSTRunning){ //如果还没有线程启动BLE service
                            isSTRunning=true;
                            while (!Utils.isServiceRunning(myContext,serviceName)) {
                                L.i("===bThread start service...");
                                Intent BluetoothIntent=new Intent(myContext,BluetoothLeService.class);
                                myContext.startService(BluetoothIntent);
                                Thread.currentThread().sleep(500);
                            }
                            L.i("===bThread start service success!");
                            isSTRunning=false;
                        }
                        else{ //等待另一个线程启动完service再发广播包
                            while(isSTRunning){
                                L.i("===Wait for rThread done...");
                                Thread.currentThread().sleep(500);
                            }
                        }
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }

                    //发送广播包给BLE service
                    L.i("===Send boot self start broadcast");
                    BroadcastAction.sendBroadcast(myContext, BroadcastAction.BROADCAST_SERVICE_REC_ACTION_REMOTE_UPGRADE,
                            BroadcastAction.BROADCAST_CONTENT_UPGRADE_SELF_STAR);

                    L.i("End boot broadcast thread.");
                }
            }).start();
        }
        else if (action.equals(action_start) && bValue==1) {
            //启动服务，监听平台定制OTA广播包
            new Thread(new Runnable() {
                @Override
                public void run() {
                    try {
                        if(!isSTRunning){ //如果还没有线程启动BLE service
                            isSTRunning=true;
                            while (!Utils.isServiceRunning(myContext,serviceName)) {
                                L.i("===rThread start service...");
                                Intent BluetoothIntent=new Intent(myContext,BluetoothLeService.class);
                                myContext.startService(BluetoothIntent);
                                Thread.currentThread().sleep(500);
                            }
                            L.i("===rThread start service success!");
                            isSTRunning=false;
                        }
                        else{ //等待另一个线程启动完service再发广播包
                            while(isSTRunning){
                                L.i("===Wait for bThread done...");
                                Thread.currentThread().sleep(500);
                            }
                        }
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }

                    //发送广播包给BLE service
                    L.i("===Send OTA broadcast");
                    BroadcastAction.sendBroadcast(myContext, BroadcastAction.BROADCAST_SERVICE_REC_ACTION_REMOTE_UPGRADE,
                            BroadcastAction.BROADCAST_CONTENT_UPGRADE_GUIDE_STAR);

                    L.i("End OTA broadcast thread.");
                }
            }).start();
        }
    }
}
