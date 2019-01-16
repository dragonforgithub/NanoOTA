package com.lxl.nanosic.app;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Build;
import android.os.Environment;
import android.support.v4.content.FileProvider;
import android.view.Gravity;
import android.widget.Toast;

import com.lxl.nanosic.app.ble.BroadcastAction;
import com.lxl.nanosic.app.okhttp.CallBackUtil;
import com.lxl.nanosic.app.okhttp.OkhttpUtil;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.File;
import okhttp3.Call;

public class MainBroadcastReceiver extends BroadcastReceiver {

    private Context mContext;
    private final String nanoOtaPath = Environment.getExternalStorageDirectory().getAbsolutePath()+"/nanosic_ota/";
    private String mNewestApkPath=null;

    @Override
    public void onReceive(Context context, Intent intent) {

        mContext = context;

        // 解析数据类型
        int ibroad_value = 0x00;
        String sbroad_value = null, sbroad_aux_val = null;

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

        String action = intent.getAction();
        L.i("MainBroadcastReceiver Action:" + action +
                 "\nsbroad_value:"+sbroad_value+
                 "\nsbroad_aux_val:"+sbroad_aux_val+
                 "\nibroad_value:"+ibroad_value);

        /** 获取服务器最新apk版本号 */
        if(action.equals(BroadcastAction.MAIN_UPDATE_APK_CHECK)){
            // 获取当前应用版本信息
            final int curVerCode = Utils.getAppVersionCode(mContext); //得到当前应用版本号
            // 服务器地址
            final String serverURL   = "https://47.98.206.54/app/LookUpVersList.ashx";
            final String projectName = "OtaApk";

            try {
                JSONObject jsonObject = new JSONObject();
                jsonObject.put("Mark", projectName);
                // 回调获取执行结果
                OkhttpUtil.okHttpPostJson(serverURL, jsonObject.toString(1), new CallBackUtil.CallBackString() {

                    @Override
                    public void onFailure(Call call, Exception e) {
                        L.e( "check error:" + e.getMessage());

                        // 联网异常,扫描本地安装包比当前版本更高则提示升级
                        if(curVerCode < GetLocalApkNewestVerCode(mContext, nanoOtaPath)){
                            L.i( "Install local apk");
                            openApkFile(mContext, new File(mNewestApkPath));
                        }
                    }

                    @Override
                    public void onResponse(String response) {
                        L.d("Post ---> " + response);
                        try {
                            int apkVerCur,apkVerCode=0;
                            String newestVersionStr=null;

                            JSONObject jsObj = new JSONObject(response); //转换成json对象
                            JSONArray jsArray = jsObj.getJSONArray("list"); //取出版本列表

                            // 遍历列表获得最高的版本号
                            for (int i = 0; i < jsArray.length(); i++) {
                                JSONObject verList = (JSONObject) jsArray.get(i);
                                String versionStr = verList.getString("version");

                                if(Utils.isNumeric(versionStr)){
                                    L.d("Server mVersionStr : "+versionStr);
                                    apkVerCur = Integer.parseInt(versionStr);
                                    if(apkVerCode < apkVerCur){
                                        apkVerCode = apkVerCur;
                                        newestVersionStr = versionStr;
                                    }
                                }
                            }

                            // 按规则最后一项是最新版本号，但实际不一样
                            // Post ---> { "ret": 0, "list": [ { "version": "001" }, { "version": "004" }, { "version": "002" }, { "version": "003" } ] }
                            /*
                            JSONObject verList = (JSONObject) jsArray.get(jsArray.length()-1);
                            String versionStr = verList.getString("version");
                            apkVerCode = Integer.parseInt(versionStr);
                            L.d("Server newest apkVerCode : "+apkVerCode);
                            */

                            L.w("Server newest VerCode:"+apkVerCode+",cur app VerCode:"+curVerCode);

                            // 服务器上最新版本高于当前安装的应用
                            if(apkVerCode > curVerCode){
                                // 本地没有最新安装包则发送下载广播
                                if(apkVerCode > GetLocalApkNewestVerCode(mContext, nanoOtaPath)){
                                    L.d( "===Send apk select broadcast");
                                    BroadcastAction.sendBroadcast(mContext, BroadcastAction.MAIN_UPDATE_APK_SELECT,
                                            projectName, newestVersionStr);

                                }else{ // 已下载安装包则提示安装
                                    L.i( "Install newest local apk");
                                    openApkFile(mContext, new File(mNewestApkPath));
                                }
                            }
                        } catch (JSONException e) {
                            L.e("Get apk list error: " + e.getMessage());
                        }
                    }
                });
            } catch (JSONException e) {
                e.printStackTrace();
            }
        }
        /** 下载安装包 */
        else if(action.equals(BroadcastAction.MAIN_UPDATE_APK_DOWNLOAD)){
            if(sbroad_value.equals("OtaApk")){ // 从服务器获得新版号，进行下载
                downLoadApkAndInstall(sbroad_value, sbroad_aux_val); // 下载安装包
            }else if(sbroad_value.equals("LocalApk")){ // 无法从服务器获得新版本号，打开本地最新版本
                File localApk = new File(sbroad_aux_val);
                openApkFile(mContext, localApk); //这里是代表本地安装包路径
            }else{
                L.e("Invalid broadcast!");
            }
        }
    }

    /** 获取本地APK最高版本号 */
    public int GetLocalApkNewestVerCode(Context ctx, String pathStr) {
        int newestCode=1;
        // 扫描apk安装包
        File dirPath = new File(pathStr);
        if(dirPath.exists()){
            File[] files = dirPath.listFiles();
            for (int i = files.length-1; i >= 0; i--) {
                if (files[i].isFile()) {
                    String filename = files[i].getName();
                    if(filename.endsWith(".apk")){
                        File filePath = new File(files[i].getAbsolutePath());
                        L.w("Local apk ---> "+filePath.toString());
                        // 获取安装包信息
                        PackageManager pm = ctx.getPackageManager();
                        PackageInfo info = pm.getPackageArchiveInfo(filePath.toString(),
                                PackageManager.GET_ACTIVITIES);

                        if(info != null){
                            ApplicationInfo appInfo = info.applicationInfo;
                            String apkName    = pm.getApplicationLabel(appInfo).toString();
                            String apkPktName = appInfo.packageName; //得到安装包名称
                            String apkVerName = info.versionName; //得到安装包版本名
                            int    apkVerCode = info.versionCode; //得到安装包版本号

                            L.d("appName:"+apkName+";packageName:"+apkPktName
                                    +";version:"+apkVerName+";verCode:"+apkVerCode
                                    +";newestCode:"+newestCode);

                            // 获取当前应用版本信息，进行比较
                            String curPktName = Utils.getAppPktName(ctx); //得到当前应用包名
                            if(apkPktName.equals(curPktName)){
                                if(newestCode < apkVerCode){
                                    // 记录更新版本的安装包路径
                                    newestCode = apkVerCode;
                                    mNewestApkPath = filePath.toString();
                                }else{
                                    // 删除相同/旧版安装包
                                    String oldApk = filePath.toString();
                                    File file = new File(oldApk);
                                    if(file.isFile()){
                                        file.delete();
                                        L.w("Delete : " + oldApk);
                                    }
                                }
                            }
                        }else{
                            L.e("Unrecognized apk！");
                        }
                    }
                }
            }
        }else{
            L.w("Local dir is not exist!");
        }
        L.w("Local APK newestCode : "+newestCode);
        return newestCode;
    }

    /** 下载新版apk并且安装 */
    private void downLoadApkAndInstall(String projectName, String versionStr)
    {
        // 从服务器下载最新版本，例如："https://47.98.206.54/OtaApk/002/Nanosic_OTA.apk"
        String downLoadURL = "https://47.98.206.54/app/"+projectName+"/"+versionStr+"/Nanosic_OTA.apk";
        //设置本地保存路径
        String fileName = "Nanosic_OTA_V"+versionStr+".apk";
        String sdCardPath = Environment.getExternalStorageDirectory().getAbsolutePath()+"/nanosic_ota/";

        // 回调获取执行结果
        L.d("Download : " + downLoadURL + " ---> " + sdCardPath + fileName);

        BroadcastAction.sendBroadcast(mContext, BroadcastAction.MAIN_UPDATE_APK_DOWNLOADING,
                "true");

        OkhttpUtil.okHttpDownloadFile(downLoadURL, new CallBackUtil.CallBackFile(mContext, sdCardPath, fileName) {
            @Override
            public void onFailure(Call call, Exception e) {
                L.e( "DownloadFile error:" + e);
                //Utils.ToastShow(mContext, Toast.LENGTH_SHORT, Gravity.CENTER_HORIZONTAL,"错误:","文件下载失败！");
                BroadcastAction.sendBroadcast(mContext, BroadcastAction.MAIN_UPDATE_APK_DOWNLOADFAILED,
                        null);
            }

            @Override
            public void onResponse(String response) {
                L.d("DownloadFile(String) ---> " + response);
                BroadcastAction.sendBroadcast(mContext, BroadcastAction.MAIN_UPDATE_APK_DOWNLOADING,
                        "false");
            }

            @Override
            public void onResponse(File response) {
                L.d("DownloadFile(File) ---> " + response);
                if(response != null){
                    openApkFile(mContext,response);
                    //Utils.ToastShow(mContext, Toast.LENGTH_SHORT, Gravity.TOP,"新版已下载，请立即安装！",null);
                    BroadcastAction.sendBroadcast(mContext, BroadcastAction.MAIN_UPDATE_APK_DOWNLOADING,
                            "false");
                } else{
                    //Utils.ToastShow(mContext, Toast.LENGTH_SHORT, Gravity.CENTER_HORIZONTAL,"错误:","文件下载失败！");
                    BroadcastAction.sendBroadcast(mContext, BroadcastAction.MAIN_UPDATE_APK_DOWNLOADFAILED,
                            null);
                }
            }
        });
    }

    /** 安装新版本 */
    public void openApkFile(Context ctx,File file) {

        L.d(file.getAbsolutePath() + " is installing...");
        Intent installInte = new Intent();
        installInte.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        installInte.setAction(Intent.ACTION_VIEW);
        if(Build.VERSION.SDK_INT>= Build.VERSION_CODES.N){
            Uri uriForFile = FileProvider.getUriForFile(mContext, BuildConfig.APPLICATION_ID+".provider", file);
            installInte.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
            installInte.setDataAndType(uriForFile,  "application/vnd.android.package-archive");
        }else{
            installInte.setDataAndType(Uri.fromFile(file), "application/vnd.android.package-archive");
        }

        try {
            ctx.startActivity(installInte);
        } catch (Exception var5) {
            var5.printStackTrace();
            String mainText = mContext.getResources().getString(R.string.Text_view_error_code_title);
            String subText = mContext.getResources().getString(R.string.Toast_view_unrecognized_file);
            Utils.ToastShow(mContext, Toast.LENGTH_LONG, Gravity.TOP, mainText, subText);
        }
    }
}
