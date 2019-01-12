package com.lxl.nanosic.app.ble;

import com.lxl.nanosic.app.L;

/**
 * Created by Hor on 2018/7/18.
 */

public class Config {

    // 是否加密传输数据，主要是针对新、旧款遥控器
    private static boolean isEncryptedSendBin = false;

    private Config() {
        L.i("Config,cannot be initialized");
    }

    public static void SetEncryptState(boolean state) {
        isEncryptedSendBin = state;
        L.i("Config encrypt state : " + isEncryptedSendBin);
    }

    public static boolean GetEncryptState() {
        return isEncryptedSendBin;
    }
}
