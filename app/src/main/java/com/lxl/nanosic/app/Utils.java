package com.lxl.nanosic.app;

import android.app.Activity;
import android.app.ActivityManager;
import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.content.Context;
import android.content.SharedPreferences;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.os.Build;
import android.text.TextUtils;
import android.util.Log;
import android.widget.Toast;

import java.io.IOException;
import java.io.InputStream;
import java.security.KeyStore;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.security.UnrecoverableKeyException;
import java.security.cert.CertificateException;
import java.security.cert.CertificateFactory;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.net.ssl.KeyManager;
import javax.net.ssl.KeyManagerFactory;
import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLSocketFactory;
import javax.net.ssl.TrustManagerFactory;

import static android.content.Context.NOTIFICATION_SERVICE;


public class Utils {

    static final String TAG = "Utils";

    /**
     * 判断service是否已经运行
     * 必须判断uid,因为可能有重名的Service,所以要找自己程序的Service
     *
     * @param className Service的全名,例如PushService.class.getName() *
     * @return true:Service已运行 false:Service未运行
     */
    public static boolean isServiceRunning(Context context, String className) {
        Log.i(TAG, "===check service state...");
        ActivityManager am = (ActivityManager) context.getSystemService(Context.ACTIVITY_SERVICE);
        List<ActivityManager.RunningServiceInfo> serviceList = am.getRunningServices(Integer.MAX_VALUE);
        int myUid = android.os.Process.myUid();
        for (ActivityManager.RunningServiceInfo runningServiceInfo : serviceList) {
            if (runningServiceInfo.uid == myUid && runningServiceInfo.service.getClassName().equals(className)) {
                return true;
            }
        }
        return false;
    }

    /**
     * Toast显示即时信息：
     * 每次把上一条取消以打达到显示最新一条消息
     */
    private static Toast mToast = null;

    public static void ToastShow(final Context context, int length, final int pos, final String mainText, final String subText) {
        //拼接主信息和次信息
        String toastText = mainText + (TextUtils.isEmpty(subText) ? "" : ("\t" + subText));

        //如果上一条还没显示完也立即取消
        if (mToast != null)
            mToast.cancel();

        mToast = Toast.makeText(context, toastText, length);
        mToast.setGravity(pos, 0, 0);
        mToast.show();
    }

    /**
     * 获得当前APP的版本号：
     */
    public static int getAppVersionCode(Context mContext) {
        PackageManager manager = mContext.getPackageManager();
        int code = 0;
        try {
            PackageInfo info = manager.getPackageInfo(mContext.getPackageName(), 0);
            code = info.versionCode;
        } catch (PackageManager.NameNotFoundException e) {
            e.printStackTrace();
        } return code;
    }

    /**
     * 获得当前APP的版本名：
     */
    public static String getAppVersionName(Context mContext) {
        PackageManager manager = mContext.getPackageManager();
        String name=null;
        try {
            PackageInfo info = manager.getPackageInfo(mContext.getPackageName(), 0);
            name = info.versionName;
        } catch (PackageManager.NameNotFoundException e) {
            e.printStackTrace();
        } return name;
    }

    /**
     * 获得当前APP的包名：
     */
    public static String getAppPktName(Context mContext) {
        PackageManager manager = mContext.getPackageManager();
        String pktName = null;
        try {
            PackageInfo info = manager.getPackageInfo(mContext.getPackageName(), 0);
            pktName = info.packageName;
        } catch (PackageManager.NameNotFoundException e) {
            e.printStackTrace();
        }
        return pktName;
    }

    /**
     * 进度条工具类
     */
    private static NotificationManager manager;
    private static NotificationManager getManager() {
        if (manager == null) {
            manager = (NotificationManager) MainActivity.getInstance().getSystemService(NOTIFICATION_SERVICE);
        }
        return manager;
    }

    //@RequiresApi(api = Build.VERSION_CODES.O)
    private static Notification.Builder getNotificationBuilder(String title, String content, String channelId) {

        Notification.Builder nBuilder = new Notification.Builder(MainActivity.getInstance());
        //大于8.0
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            //id随便指定
            NotificationChannel channel = new NotificationChannel(channelId, MainActivity.getInstance().getPackageName(), NotificationManager.IMPORTANCE_DEFAULT);
            //channel.canBypassDnd();//可否绕过，请勿打扰模式
            //channel.enableLights(true);//闪光
            //channel.setLockscreenVisibility(VISIBILITY_SECRET);//锁屏显示通知
            //channel.setLightColor(Color.RED);//指定闪光是的灯光颜色
            channel.canShowBadge();//桌面launcher消息角标
            channel.enableVibration(false);//是否允许震动
            channel.setVibrationPattern(new long[]{100, 100, 200});//震动的模式，震3次，第一次100，第二次100，第三次200毫秒
            //channel.setSound(null, null);
            //channel.getAudioAttributes();//获取系统通知响铃声音配置
            channel.getGroup();//获取通知渠道组
            channel.setBypassDnd(true);//设置可以绕过，请勿打扰模式
            //channel.shouldShowLights();//是否会闪光
            //通知管理者创建的渠道
            getManager().createNotificationChannel(channel);

            nBuilder.setChannelId(channelId);
        }

        nBuilder.setAutoCancel(true); //左右划除
        nBuilder.setContentTitle(title);
        nBuilder.setContentText(content);
        nBuilder.setSmallIcon(R.mipmap.ota_launcher);

        return nBuilder;
    }

    //@RequiresApi(api = Build.VERSION_CODES.O)
    public static void showNotification(String title, String content, int manageId, String channelId, int progress, int maxProgress) {
        final Notification.Builder builder = getNotificationBuilder(title,content,channelId);
       /* Intent intent = new Intent(this, SecondeActivity.class);
        PendingIntent pendingIntent = PendingIntent.getActivity(this, 0, intent, 0);
        builder.setContentIntent(pendingIntent);*/
        builder.setOnlyAlertOnce(true);
        builder.setDefaults(Notification.FLAG_ONLY_ALERT_ONCE);
        builder.setWhen(System.currentTimeMillis());
        builder.setProgress(maxProgress, progress, false);
        builder.setContentText(progress != -1 ? (content + progress + "%"):content);

        getManager().notify(manageId, builder.build());
    }

    //@RequiresApi(api = Build.VERSION_CODES.O)
    public static void cancleNotification(int manageId) {
        getManager().cancel(manageId);
    }


    /**
     * 保存输入数据，以便下次自动填入
     */
    public static void savePreferences(Context ctx, String entry, String value) {
        //实例化SharedPreferences对象（第一步）
        SharedPreferences mySharedPreferences= ctx.getSharedPreferences("test",
                Activity.MODE_PRIVATE);
        //实例化SharedPreferences.Editor对象（第二步）
        SharedPreferences.Editor editor = mySharedPreferences.edit();
        //用putString的方法保存数据
        editor.putString(entry, value);
        //提交当前数据
        editor.commit();
        L.d("savePreferences : <" + entry + "," + value + ">");
    }

    public static String getPreferences(Context ctx, String entry) {
        //同样，在读取SharedPreferences数据前要实例化出一个SharedPreferences对象
        SharedPreferences sharedPreferences= ctx.getSharedPreferences("test",
                Activity.MODE_PRIVATE);
        // 使用getString方法获得value，注意第2个参数是value的默认值
        return sharedPreferences.getString(entry,null);
    }

    /**
     * 利用正则表达式判断字符串是否是数字
     * @param str
     * @return
     */
    public static boolean isNumeric(String str){
        Pattern pattern = Pattern.compile("[0-9]*");
        Matcher isNum = pattern.matcher(str);
        if( !isNum.matches() ){
            return false;
        }
        return true;
    }

    /**
     * 判断是否快速双击
     * @return
     */
    private static long lastClickTime;
    public static boolean isFastDoubleClick() {
        long time = System.currentTimeMillis();
        long timeD = time - lastClickTime;
        //L.w(" Double click timeD = " + timeD);
        lastClickTime = time;
        if ( 0 < timeD && timeD < 800) {
            return true;
        }
        return false;
    }



    // 使用命令keytool -printcert -rfc -file xxx.cer 导出证书为字符串，然后将字符串转换为输入流，
    // 如果使用的是OkHttp可以直接使用new Buffer().writeUtf8(s).inputStream()

    /**
     * 返回SSLSocketFactory
     *
     * @param certificates 证书的输入流
     * @return SSLSocketFactory
     */
    public static SSLSocketFactory getSSLSocketFactory(InputStream... certificates) {
        return getSSLSocketFactory(null, certificates);
    }

    /**
     * 双向认证
     *
     * @param keyManagers  KeyManager[]
     * @param certificates 证书的输入流
     * @return SSLSocketFactory
     */
    public static SSLSocketFactory getSSLSocketFactory(KeyManager[] keyManagers, InputStream... certificates) {
        try {
            CertificateFactory certificateFactory = CertificateFactory.getInstance("X.509");
            KeyStore keyStore = KeyStore.getInstance(KeyStore.getDefaultType());
            keyStore.load(null);
            int index = 0;
            for (InputStream certificate : certificates) {
                String certificateAlias = Integer.toString(index++);
                keyStore.setCertificateEntry(certificateAlias, certificateFactory.generateCertificate(certificate));
                try {
                    if (certificate != null)
                        certificate.close();
                } catch (IOException e) {
                }
            }
            SSLContext sslContext = SSLContext.getInstance("TLS");
            TrustManagerFactory trustManagerFactory = TrustManagerFactory.getInstance(TrustManagerFactory.getDefaultAlgorithm());
            trustManagerFactory.init(keyStore);
            sslContext.init(keyManagers, trustManagerFactory.getTrustManagers(), new SecureRandom());
            SSLSocketFactory socketFactory = sslContext.getSocketFactory();
            return socketFactory;
        } catch (Exception e) {
            e.printStackTrace();
        }
        return null;
    }

    /**
     * 获得双向认证所需的参数
     *
     * @param bks          bks证书的输入流
     * @param keystorePass 秘钥
     * @return KeyManager[]对象
     */
    public static KeyManager[] getKeyManagers(InputStream bks, String keystorePass) {
        KeyStore clientKeyStore = null;
        try {
            clientKeyStore = KeyStore.getInstance("BKS");
            clientKeyStore.load(bks, keystorePass.toCharArray());
            KeyManagerFactory keyManagerFactory = KeyManagerFactory.getInstance(KeyManagerFactory.getDefaultAlgorithm());
            keyManagerFactory.init(clientKeyStore, keystorePass.toCharArray());
            KeyManager[] keyManagers = keyManagerFactory.getKeyManagers();
            return keyManagers;
        } catch (KeyStoreException e) {
            e.printStackTrace();
        } catch (UnrecoverableKeyException e) {
            e.printStackTrace();
        } catch (CertificateException e) {
            e.printStackTrace();
        } catch (NoSuchAlgorithmException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }
        return null;
    }
}

