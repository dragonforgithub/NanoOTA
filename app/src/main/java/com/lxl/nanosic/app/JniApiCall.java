package com.lxl.nanosic.app;

public class JniApiCall {

    static {
        System.loadLibrary("nanoHAL");
        System.loadLibrary("appJNI");
    }

    public static void jni_NanoOpen() {
        NanoOpen();
    }

    public static void jni_NanoClose() {
        NanoClose();
    }

    public static void jni_NanoLogin(byte[] data,int dataLen) {
        NanoLogin(data, dataLen);
    }

    public static void jni_NanoRegister(byte[] data,int dataLen) {
        NanoRegister(data, dataLen);
    }

    public static void jni_NanoProcData(byte[] data,int dataLen) {
        NanoProcData(data, dataLen);
    }

    //Nanosic JNI接口
    private static native int  NanoOpen();
    private static native int  NanoClose();
    private static native int  NanoLogin(byte[] data,int dataLen);
    private static native int  NanoRegister(byte[] data,int dataLen);
    private static native void NanoProcData(byte[] data,int dataLen);
}
