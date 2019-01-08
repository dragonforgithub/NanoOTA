package com.lxl.nanosic.app.ble;

import com.lxl.nanosic.app.L;

/**
 * Created by Hor on 2018/7/13.
 */

public class Encryption {

    private byte RandomBuf[];
    private boolean flagEnableEncryption=false;

    /*-----------------------------------------------------------------------------
    Function Name:	Java_com_nanosic_www_wnf1x0_remoteupgrade_getRandom
    Input		:
    Output		:
    Return		:	返回给Java层获取到的随机数的6字节数组
    Describe		:	获取随机数，需要Java层传个数组大小len，len值不能小于6
    -------------------------------------------------------------------------------*/
    public static native byte[] getRandom(int len);

    /*-----------------------------------------------------------------------------
    Function Name:	Java_com_nanosic_www_wnf1x0_remoteupgrade_devInfoVerify
    Input		:
    Output		:
    Return		:	返回一个布尔值，TRUE表示验证通过，FALSE表示验证不通过。
    Describe    :	验证是否是我们的遥控。reportID不需要传进来
    -------------------------------------------------------------------------------*/
    public static native boolean devInfoVerify(byte[] version_packet);

    /*-----------------------------------------------------------------------------
    Function Name:	Java_com_nanosic_www_wnf1x0_remoteupgrade_encryptData
    Input		:
    Output		:
    Return		:	返回加密后的数组，第一个字节为包序号
    Describe		:	传入下发的数据包的数据，另外如果是16字节的文件头，第一个字节为0，
                        第二字节为包序号，如果是数据或者需要重传的64字节文件头，第一字节为1，
                        第二字节为包序号，后续字节为数据，
    -------------------------------------------------------------------------------*/
    public static native byte[] encryptData(byte[] data);


    static {
        try {
            //加载so库的时候，需要掐头去尾，去掉lib和.so
            System.loadLibrary("DataEncryption");
        }catch (Exception e){
            L.e("Loading so error.");
        }
    }

    //TODO:要更新jni库接口的包名，后面需加密方式再更新
    /*
    public Encryption(){

        RandomBuf = new byte[8];
        RandomBuf = getRandom(7);
        flagEnableEncryption = Config.isEncryptedSendBin;
    }

    public boolean setEncryptionEnable(boolean fEnable){
        flagEnableEncryption = fEnable;
        return true;
    }

    public boolean getEncryptionEnable(){
        return flagEnableEncryption;
    }

    public byte [] getRandomData(){
        Arrays.fill(RandomBuf,(byte)0);
        RandomBuf = getRandom(7);
        return RandomBuf;
    }

    public boolean RemoteInfoVerify(byte[] verPacket){

        return devInfoVerify(verPacket);
    }

    public byte [] encryptUpgradePacket(byte[] bufPacket){

        return encryptData(bufPacket);
    }
    */
}
