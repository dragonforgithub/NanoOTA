package com.lxl.nanosic.app.ble;

import android.content.Context;

import com.lxl.nanosic.app.L;

import java.io.File;

/**
 * Created by Hor on 2018/7/10.
 */

public class UpgradeFile {
    private Context backContext = null;

    RemoteFileRead mRemoteFileR ;
    private byte mFileBuf[];
    private byte mFileUpgradeHead[];
    private byte BackFileHead[] ;
    private byte OnePacketBuf[];
    private int  mFileLeng;
    private String pathFile;
    private int UpgradeAppLen,UpgradeAppStarAddr;
    String FileVersion;

    public UpgradeFile(Context bContex, String filePath){
        backContext = bContex;
        OnePacketBuf = new byte[20];
        mFileUpgradeHead = new byte[32];
        BackFileHead = new byte[64];
        UpgradeAppLen = 0x00;
        UpgradeAppStarAddr = 0x00;
        FileVersion = null;

        mRemoteFileR = new RemoteFileRead();

        if(filePath == null) { //未指定sdcard的路径则从asset目录加载升级文件
            pathFile = "RemoteUpgrade" + File.separator + "remote.bin";
            mFileBuf = mRemoteFileR.ReadAssetUpgradeFile(bContex,pathFile);
            if(mFileBuf != null) {
                if(mFileBuf.length > 0x6000) {
                    mFileLeng = mFileBuf.length;
                    GetUpgradeFileHead();
                    BroadcastAction.sendBroadcast(backContext, BroadcastAction.BROADCAST_SERVICE_SEND_ACTION_FILE_OPERATION,
                            BroadcastAction.ROADCAST_CONTENT_REMOTE_FILE_PATH,
                            "APK/assets/" + pathFile);
                }else{
                    mFileLeng = 0x00;
                    mFileBuf = null;
                }
            }else{
                mFileLeng = 0x00;
            }
        } else { //指定sdcard路径的升级文件

            mFileBuf = mRemoteFileR.ReadSdCardUpgradeFile(bContex,filePath);
            if(mFileBuf != null) {
                if(mFileBuf.length > 0x6000) {
                    mFileLeng = mFileBuf.length;
                    GetUpgradeFileHead();
                    BroadcastAction.sendBroadcast(backContext, BroadcastAction.BROADCAST_SERVICE_SEND_ACTION_FILE_OPERATION,
                            BroadcastAction.ROADCAST_CONTENT_REMOTE_FILE_PATH,
                            "sdcard/" + pathFile);
                }else{
                    mFileLeng = 0x00;
                    mFileBuf = null;
                }
            }else{
                mFileLeng = 0x00;
            }

        }
    }

    public boolean CheckUpgradePidVid(byte vid,byte pid){
        if((vid == mFileUpgradeHead[0]) && (pid == mFileUpgradeHead[1]))
        {
            return true;
        }else{
            L.i("Remote VID PID:" + String.format("%02x%02x,", vid, pid));
            L.i("File VID PID:" + String.format("%02x%02x,", mFileUpgradeHead[0], mFileUpgradeHead[1]));

            System.out.printf("vid(%02x)-(%02x),pid(%02x)-(%02x), ",
                    vid,pid,mFileUpgradeHead[0],mFileUpgradeHead[1]);

            return false;
        }
    }

    public boolean CheckUpgradeVersion(byte verh,byte verl){
        if((verh == mFileUpgradeHead[5]) && (verl == mFileUpgradeHead[4]))
        {
            return true;
        }else{
            return false;
        }
    }

    private void GetUpgradeFileHead(){

        if (mFileLeng > 64) {
            int mFileHeadBaseAddr = mFileLeng - 64;
            mFileUpgradeHead[0] = mFileBuf[mFileHeadBaseAddr + 0x14];        // 厂商ID
            mFileUpgradeHead[1] = mFileBuf[mFileHeadBaseAddr + 0x15];        // 产品ID
            mFileUpgradeHead[2] = mFileBuf[mFileHeadBaseAddr];               // CRC_SHADOW
            mFileUpgradeHead[3] = 0x00;
            mFileUpgradeHead[4] = mFileBuf[mFileHeadBaseAddr + 0x16];        // 版本号
            mFileUpgradeHead[5] = mFileBuf[mFileHeadBaseAddr + 0x17];
            int iTemp = (int)mFileBuf[mFileHeadBaseAddr + 0x09];
            iTemp &= 0x00ff;
            int binLength = iTemp;
            iTemp = (int)mFileBuf[mFileHeadBaseAddr + 0x0a]<<8;
            iTemp &= 0x00ff00;
            binLength += iTemp;
            iTemp = (int)mFileBuf[mFileHeadBaseAddr + 0x0b]<<16;
            iTemp &= 0x00ff0000;
            binLength += iTemp;
            iTemp = (int)mFileBuf[mFileHeadBaseAddr + 0x0c]<<24;
            iTemp &= 0xff000000;
            binLength += iTemp;
            UpgradeAppLen = binLength;
            binLength /= 128;
            mFileUpgradeHead[6] = (byte)(binLength & 0xff);          // bin文件长度
            mFileUpgradeHead[7] = (byte)((binLength>>8) & 0xff);
            mFileUpgradeHead[8] = mFileBuf[mFileHeadBaseAddr + 0x01];   // bootload 跳转地址
            mFileUpgradeHead[9] = mFileBuf[mFileHeadBaseAddr + 0x02];
            mFileUpgradeHead[10] = mFileBuf[mFileHeadBaseAddr + 0x03];
            mFileUpgradeHead[11] = mFileBuf[mFileHeadBaseAddr + 0x04];
            mFileUpgradeHead[12] = 0x04;                                // head 类型，固定为0x04
            mFileUpgradeHead[13] = mFileBuf[mFileHeadBaseAddr + 0x5];   // 保留字节
            mFileUpgradeHead[14] = mFileBuf[mFileHeadBaseAddr + 0x6];
            mFileUpgradeHead[15] = (byte)0xff;                          // 标志

            iTemp = (int)mFileUpgradeHead[13];
            iTemp &= 0xff;
            UpgradeAppStarAddr = iTemp;
            iTemp = ((int)mFileUpgradeHead[14]<<8);
            iTemp &= 0x00ff00;
            UpgradeAppStarAddr += iTemp;
            UpgradeAppStarAddr += 1;
            UpgradeAppStarAddr *= 128;
            for(int j=0;j<64;j++)
            {
                BackFileHead[j] = mFileBuf[mFileBuf.length - 64 + j];
            }
            FileVersion = String.format("0x%02X", mFileUpgradeHead[5])  + String.format("%02X", mFileUpgradeHead[4]);
            L.i("Upgrade File VID: " + String.format("0x%02X", mFileUpgradeHead[0]));
            L.i("Upgrade File PID: " + String.format("0x%02X", mFileUpgradeHead[1]));
            L.i("Upgrade File Version: " + FileVersion);

            BroadcastAction.sendBroadcast(backContext, BroadcastAction.BROADCAST_SERVICE_SEND_ACTION_FILE_OPERATION,
                    BroadcastAction.ROADCAST_CONTENT_REMOTE_FILE_VERSION,
                    FileVersion);
        }
    }

    public void SendFileTotalInfo(){
        if(pathFile != null) {
            BroadcastAction.sendBroadcast(backContext, BroadcastAction.BROADCAST_SERVICE_SEND_ACTION_FILE_OPERATION,
                    BroadcastAction.ROADCAST_CONTENT_REMOTE_FILE_PATH,
                    "APK/assets/" + pathFile);
        }

        if(FileVersion != null) {
            BroadcastAction.sendBroadcast(backContext, BroadcastAction.BROADCAST_SERVICE_SEND_ACTION_FILE_OPERATION,
                    BroadcastAction.ROADCAST_CONTENT_REMOTE_FILE_VERSION,
                    FileVersion);
        }
    }



    protected byte[] GetHeadPacketBuf(){
        OnePacketBuf[0] = 0x5c;
        OnePacketBuf[1] = (byte)0xf0;

        for(int i=0;i<16;i++){
            OnePacketBuf[2 + i] = mFileUpgradeHead[i];
        }
        OnePacketBuf[18] = 0x00;
        OnePacketBuf[19] = 0x00;
        return OnePacketBuf;
    }

    protected int GetSendPercent(int packetIndex){
        if(UpgradeAppLen > 64) {
            return ((packetIndex * 1600) / UpgradeAppLen);
        }else {
            return 0;
        }
    }

    protected byte[] GetDataPacketBuf(int packetIndex){
        int fileToSendLen,newPacketIndex;
        if(mFileBuf.length < 100)
        {
            return null;
        }
        if(UpgradeAppLen > 64) {
            fileToSendLen = UpgradeAppLen ;
        }else {
            fileToSendLen = 0x00;
        }

        if((packetIndex*16) < fileToSendLen) {
            OnePacketBuf[0] = 0x5c;
            OnePacketBuf[1] = (byte) 0xf1;
            OnePacketBuf[2] = (byte) (packetIndex & 0xff);
            OnePacketBuf[3] = (byte) ((packetIndex >> 8) & 0xff);
            if ((packetIndex * 16) < (fileToSendLen - 16)) {
                for (int i = 0; i < 16; i++) {
                    OnePacketBuf[4 + i] = mFileBuf[UpgradeAppStarAddr + packetIndex * 16 + i];
                }
            } else {
                for (int i = 0; i < 16; i++) {
                    OnePacketBuf[4 + i] = 0x00;
                }
                for (int i = 0; i < (fileToSendLen - (packetIndex * 16)); i++) {
                    OnePacketBuf[4 + i] = mFileBuf[UpgradeAppStarAddr + packetIndex * 16 + i];
                }
            }
            return OnePacketBuf;
        }
        else {
            return null;
        }
    }

    protected byte[] GetAllHeadPacketBuf(int packetIndex){
        OnePacketBuf[0] = 0x5c;
        OnePacketBuf[1] = (byte) 0x85;
        OnePacketBuf[2] = (byte) (packetIndex & 0xff);
        OnePacketBuf[3] = (byte) ((packetIndex >> 8) & 0xff);

        if (packetIndex < 4) {
            for (int i = 0; i < 16; i++) {
                OnePacketBuf[4 + i] = BackFileHead[packetIndex * 16 + i];
            }
            return OnePacketBuf;
        }
        else {
            return null;
        }
    }

    protected byte[] GetEntryUpgradePacketBuf(){
        OnePacketBuf[0] = 0x5c;
        OnePacketBuf[1] = (byte) 0x84;   // 重新发送Image header
        for(int k=2;k<20;k++) {
            OnePacketBuf[k] = 0x00;
        }
        return OnePacketBuf;
    }

    protected byte[] GetUpgradeResetPacketBuf(){
        OnePacketBuf[0] = 0x5c;
        OnePacketBuf[1] = (byte) 0x86;   // 复位遥控器
        for(int k=2;k<20;k++) {
            OnePacketBuf[k] = 0x00;
        }
        return OnePacketBuf;
    }
}
