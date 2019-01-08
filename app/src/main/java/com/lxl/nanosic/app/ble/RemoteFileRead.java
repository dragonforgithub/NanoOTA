package com.lxl.nanosic.app.ble;

import android.content.Context;
import android.content.res.AssetManager;

import com.lxl.nanosic.app.L;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;

/**
 * Created by Hor on 2018/7/6.
 */

public class RemoteFileRead {

    public RemoteFileRead(){

    }

    private boolean isFileExists(Context context, String filename) {
        AssetManager assetManager = context.getAssets();
        try {
            String[] names = assetManager.list("");
            for (int i = 0; i < names.length; i++) {
                if (names[i].equals(filename.trim())) {
                    L.i(filename + " exists");
                    return true;
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
            L.i(filename + " not exists");
            return false;
        }
        L.i(filename + " not exists");
        return false;
    }

    public byte[] ReadSdCardUpgradeFile(Context _context, String nameFile)
    {
        byte [] fileBuf = null;
        int fileLen;
        FileInputStream in = null;

        try
        {
            in = new FileInputStream(nameFile); //读取升级文件
            fileLen = in.available();  // 返回流中尚未读取的字节的数量
            L.i("File length:" + fileLen);
            int length = -1;
            fileBuf = new byte[fileLen];

            while ((length = in.read(fileBuf)) != -1)
            {
                L.i("Actual reading file length:" + length);
            }
        }catch (Exception e){
            e.printStackTrace();
        }finally{
            if (in != null)
            {
                try {
                    in.close();
                } catch (IOException e1) {
                    e1.printStackTrace();
                }
            }
        }

        return fileBuf;
    }

    public byte[] ReadAssetUpgradeFile(Context _context, String nameFile)
    {
        byte [] fileBuf = null;
        int fileLen;
        InputStream in = null;

        try
        {
            in = _context.getAssets().open(nameFile); // 从assets目录下复制
            fileLen = in.available();  // 返回流中尚未读取的字节的数量
            L.i("File length:" + fileLen);
            int length = -1;
            fileBuf = new byte[fileLen];

            while ((length = in.read(fileBuf)) != -1)
            {
                L.i("Actual reading file length:" + length);
            }
        }catch (Exception e){
            e.printStackTrace();
        }finally{
            if (in != null)
            {
                try {
                    in.close();
                } catch (IOException e1) {
                    e1.printStackTrace();
                }
            }
        }

        return fileBuf;
    }

    private void CopyAssetFileToData(Context _context, String sour, String Dest)
    {

        int fileLen;
        InputStream in = null;
        FileOutputStream out = null;

        File del_file =  new File(Dest);
        if (del_file.isFile() && del_file.exists()) {
            // 文件存在，先删除
            L.i("find assets file in data direction.");
            try {
                del_file.delete();
            }catch (Exception e){
                e.printStackTrace();
            }
        }

        try
        {
            File file = new File(Dest);
            in = _context.getAssets().open(sour); // 从assets目录下复制
            fileLen = in.available();  // 返回流中尚未读取的字节的数量
            out = new FileOutputStream(file);
            int length = -1;
            byte[] buf = new byte[fileLen + 100];
            L.i("write asset file");
            while ((length = in.read(buf)) != -1)
            {
                out.write(buf, 0, length);
            }
            out.flush();
        }catch (Exception e){
            e.printStackTrace();
        }finally{
            if (in != null)
            {
                try {
                    in.close();
                } catch (IOException e1) {
                    e1.printStackTrace();
                }
            }
            if (out != null)
            {
                try {
                    out.close();
                } catch (IOException e1) {
                    e1.printStackTrace();
                }
            }
        }
    }


    public boolean CopyOtaFileToData(Context _context){
        String SourcePath,DestinationPath,DirPath;
        File file;

        //--------------------------------------------------------------------
        DirPath = _context.getCacheDir() + File.separator + "UsbUpdate";

        file = new File(DirPath);
        if (!file.exists()) {
            file.mkdir();
            L.i("mkdir DirPath");
        }

        DestinationPath = _context.getCacheDir() + File.separator + "UsbUpdate"
                + File.separator + "Upgrade.bin";

        SourcePath = "UsbUpdate" + File.separator + "usbupdate.bin";

        CopyAssetFileToData(_context, SourcePath, DestinationPath);
        L.i("DestinationPath:" + DestinationPath);




//        if (isFileExists(_context, "BleRemoteUpgrade")){
//            if (!isFileExists(_context, "remote.bin")) {
//                CopyAssetFileToData(_context, SourcePath, DestinationPath);
//                Log.e(TAG, "DestinationPath:" + DestinationPath);
//                return true;
//            }else{
//                Log.e(TAG, "update have not file");
//            }
//        }else{
//            Log.e(TAG, "assets have not file");
//        }
        return false;
    }

}
