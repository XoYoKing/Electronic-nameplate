package com.jackie.ts8209a.Managers;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.os.Message;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Created by kuangyt on 2018/8/21.
 */

public class BatteryManager{

    private static String TAG = "BatteryManager";

    private static BatteryManager batteryManager = new BatteryManager();
    private static OnBatteryStatusListener batteryStatusListener = null;

    private static int level;
    private static int voltage;
    private static int status;
    private static boolean charge;

    private BatteryManager(){
    }

    //返回单例
    public static BatteryManager getBatteryManager(){
        return batteryManager;
    }

    //获取电量
    public int getLevel(){
        return level;
    }

    //获取电压值
    public int getVoltage(){
        return voltage;
    }

    //获取电池状态
    public int getStatus(){
        return status;
    }

    //获取充电状态
    public boolean getCharge(){
        return charge;
    }

    //启动定时获取电池电量状态线程
    public void startGetBatteryStaRegular(){
        (new Thread(getBatteryStatus)).start();
    }

    //接收系统广播（静态注册），充电状态
    public static class betteryStatusReceiver extends BroadcastReceiver{

        String TAG = "betteryStatusReceiver";

        @Override
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();

            if (action.equals(Intent.ACTION_POWER_CONNECTED)) {
                charge = true;
            } else if (action.equals(Intent.ACTION_POWER_DISCONNECTED)) {
                charge = false;
            }

            invokListener.sendEmptyMessage(0);
        }
    }

    //设置监听接口函数
    public static void setOnBatteryStatusListener(OnBatteryStatusListener listener){
        batteryStatusListener = listener;
    }

    //电池状态监听接口
    public interface OnBatteryStatusListener{
        abstract public void OnBatteryStatus(boolean charge,int level,int sta,int vol);
    }

    //使用cmd命令获取当前电池状态
    private void cmdGetBatteryStatus(){
        synchronized ("cmdGetBatteryStatus") {
            String dumpStr = CmdManager.execCmd("dumpsys battery");

            String level = dumpStr.substring(dumpStr.indexOf("level:"));
            String voltage = dumpStr.substring(dumpStr.indexOf("voltage:"));
            String status = dumpStr.substring(dumpStr.indexOf("status:"));
            String charge = dumpStr.substring(dumpStr.indexOf("USB powered:"));

            level = level.substring(0, level.indexOf("scale"));
            voltage = voltage.substring(0, voltage.indexOf("current now"));
            status = status.substring(0, status.indexOf("health"));
            charge = charge.substring(0, charge.indexOf("Wireless powered"));

            Matcher m1 = Pattern.compile("\\d+").matcher(level);
            m1.find();
            level = m1.group();

            Matcher m2 = Pattern.compile("\\d+").matcher(voltage);
            m2.find();
            voltage = m2.group();

            Matcher m3 = Pattern.compile("\\d+").matcher(status);
            m3.find();
            status = m3.group();



            this.level = Integer.parseInt(level);
            this.voltage = Integer.parseInt(voltage);
            this.status = Integer.parseInt(status);
            this.charge = (charge.indexOf("true") != -1);

            //Log.d(TAG, "cmdGetBatteryStatus" + ":level = " + level + " status = " + status + " voltage = " + voltage);
        }
    }

    //定时获取设备电池状态
    private Runnable getBatteryStatus = new Runnable() {
        private static final int TIME = 60*1000;
//        private static final String ACTION = Intent.ACTION_BATTERY_CHANGED;

        @Override
        public void run() {
            while(true){
                try {
                    cmdGetBatteryStatus();
//                    Log.d(TAG, "getBatteryStatus -> level = " + level + " status = " + status + " voltage = " + voltage + " charge = "+charge);
//                    Message msg = new Message();
//                    msg.obj = ACTION;
                    invokListener.sendEmptyMessage(0);
                    Thread.sleep(TIME);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }
        }
    };

    //发送给监听器，保证在主线程发送
    private static android.os.Handler invokListener = new android.os.Handler() {
        @Override
        public void handleMessage(Message msg) {
            if (batteryStatusListener != null) {
                batteryStatusListener.OnBatteryStatus(charge, level, status, voltage);
            }
        }
    };



}
