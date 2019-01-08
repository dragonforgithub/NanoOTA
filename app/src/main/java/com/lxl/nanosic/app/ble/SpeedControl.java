package com.lxl.nanosic.app.ble;

import com.lxl.nanosic.app.L;

/**
 * Created by Hor on 2018/7/11.
 */

public class SpeedControl {

    private volatile long startTime ;
    private volatile long estimatedTime;

    public SpeedControl(){
        startTime = System.nanoTime();
    }

    public boolean  WaitUsDelay(long usTime){
        long curTime;

        startTime = System.nanoTime();
        while (true){
            curTime = System.nanoTime();
            if(curTime >= startTime){
                estimatedTime = curTime - startTime;
            }else{
                startTime = System.nanoTime();
                estimatedTime = 0x00;
                L.i("Speed control,timer err.");
            }
            if(estimatedTime > (usTime*1000)){
                break;
            }
        }

        return true;
    }

}
