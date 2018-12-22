package com.jackie.ts8209a.AppModule.Basics;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Bundle;
import android.os.Message;
import android.util.Log;

import com.jackie.ts8209a.AppModule.APP;
import com.jackie.ts8209a.AppModule.Network.NetDevManager;
import com.jackie.ts8209a.AppModule.Tools.Cmd;

import java.util.Timer;
import java.util.TimerTask;
import java.util.logging.Filter;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Created by kuangyt on 2018/8/21.
 */

public class BatteryManager implements Cmd.cmdResultListener{

    private static final String TAG = "BatteryManager";

    public static final String BAT_LEVEL = "BAT_LEVEL";
    public static final String BAT_VOLTAGE = "BAT_VOLTAGE";
    public static final String BAT_STATE = "BAT_STATE";
    public static final String BAT_CHARGE = "BAT_CHARGE";

    //设置定时获取电池状态时间
    private static final int GET_STA_TIME = 30*1000;

    private static BatteryManager batteryManager = new BatteryManager();
//    private OnBatteryStatusListener batteryStatusListener = null;
    private APP App;
    private Timer getBatteryStatus;

    private int level;
    private int voltage;
    private int state;
    private boolean charge;

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
        return state;
    }

    //获取充电状态
    public boolean getCharge(){
        return charge;
    }

    //启动定时获取电池电量状态线程
    public void init(APP app){
        this.App = app;

        IntentFilter intentFilter = new IntentFilter();
        intentFilter.addAction(Intent.ACTION_BATTERY_CHANGED);
        intentFilter.addAction(Intent.ACTION_POWER_CONNECTED);
        intentFilter.addAction(Intent.ACTION_POWER_DISCONNECTED);
        App.registerReceiver(new betteryStatusReceiver(),intentFilter);
        getBatteryStatus = new Timer();
        getBatteryStatus.schedule(new getBatteryStatus(),1000,GET_STA_TIME);
    }


    //定时获取设备电池状态
    private class getBatteryStatus extends TimerTask {

        @Override
        public void run() {
            cmdGetBatteryStatus();
//            invokListener.sendEmptyMessage(0);
        }
    }

    //使用cmd命令获取当前电池状态
    private void cmdGetBatteryStatus() {
        Cmd.execCmd("dumpsys battery",this);
    }

    @Override
    public void onResult(String cmd, String res, int value) {
        String strLevel = res.substring(res.indexOf("level:"));
        String strVoltage = res.substring(res.indexOf("voltage:"));
        String strState = res.substring(res.indexOf("state:"));
//        String strCharge = res.substring(res.indexOf("USB powered:"));

        strLevel = strLevel.substring(0, strLevel.indexOf("scale"));
        strVoltage = strVoltage.substring(0, strVoltage.indexOf("current now"));
        strState = strState.substring(0, strState.indexOf("health"));
//        strCharge = strCharge.substring(0, strCharge.indexOf("Wireless powered"));

        Matcher m1 = Pattern.compile("\\d+").matcher(strLevel);
        m1.find();
        strLevel = m1.group();

        Matcher m2 = Pattern.compile("\\d+").matcher(strVoltage);
        m2.find();
        strVoltage = m2.group();

        Matcher m3 = Pattern.compile("\\d+").matcher(strState);
        m3.find();
        strState = m3.group();

        int level = Integer.parseInt(strLevel);
        int voltage = Integer.parseInt(strVoltage);
        int state = Integer.parseInt(strState);
//        boolean charge = (strCharge.contains("true"));


        if(this.level != level || this.voltage != voltage || this.state != state /* || this.charge != charge*/){
            this.level = level;
            this.voltage = voltage;
            this.state = state;
//            this.charge = charge;
            sendBroadcast();
        }

//        Log.d(TAG, "cmdGetBatteryStatus" + ":level = " + level + " state = " + state + " voltage = " + voltage + " charge = " + charge);
    }

    //接收系统广播（静态注册），充电状态
    public class betteryStatusReceiver extends BroadcastReceiver{

        @Override
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();

//            Log.d(TAG,action);
            if (action.equals(Intent.ACTION_POWER_CONNECTED)) {
                charge = true;
            } else if (action.equals(Intent.ACTION_POWER_DISCONNECTED)) {
                charge = false;
            }else if(action.equals(Intent.ACTION_BATTERY_CHANGED)){
                level = intent.getIntExtra(android.os.BatteryManager.EXTRA_LEVEL,0);
            }
            sendBroadcast();
        }
    }

    private void sendBroadcast(){
        Bundle bundle = new Bundle();
        bundle.putInt(BAT_LEVEL,this.level);
        bundle.putInt(BAT_VOLTAGE,this.voltage);
        bundle.putInt(BAT_STATE,this.state);
        bundle.putBoolean(BAT_CHARGE,this.charge);
        APP.LocalBroadcast.send(APP.ACTION_BATTERY_INFO_UPDATE,bundle);
    }
//    //设置监听接口函数
//    public void setOnBatteryStatusListener(OnBatteryStatusListener listener){
//        batteryStatusListener = listener;
//    }
//
//    //电池状态监听接口
//    public interface OnBatteryStatusListener{
//        void OnBatteryStatus(boolean charge,int level,int sta,int vol);
//    }
//
//
//    //发送给监听器，保证在主线程发送
//    private android.os.Handler invokListener = new android.os.Handler() {
//        @Override
//        public void handleMessage(Message msg) {
//            if (batteryStatusListener != null) {
//                batteryStatusListener.OnBatteryStatus(charge, level, state, voltage);
//            }
//        }
//    };



}
