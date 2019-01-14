package com.jackie.ts8209a.AppModule.Basics;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Bundle;
import android.util.Log;

import com.jackie.ts8209a.AppModule.APP;
import com.jackie.ts8209a.AppModule.Tools.Cmd;
import com.jackie.ts8209a.AppModule.Tools.Printf;

import java.util.Timer;
import java.util.TimerTask;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Created by kuangyt on 2018/8/21.
 */

public class PowerManager{

    private static final String TAG = "PowerManager";

    public static final String BAT_LEVEL = "BAT_LEVEL";
    public static final String BAT_VOLTAGE = "BAT_VOLTAGE";
    public static final String BAT_STATE = "BAT_STATE";
    public static final String BAT_CHARGE = "BAT_CHARGE";
    public static final String POWER_MODE = "POWER_MODE";
    public static final String SAVE_POWER = "SAVE_POWER";
    public static final String CPU_FREQ = "CPU_FREQ";

    //设置定时获取电池状态时间
    private final int GET_BAT_INFO_TIME = 30*1000;
    //进入低功耗模式无操作时间
    private final int SAVE_POWER_TIME = 5*60;  //5分钟进入低功耗模式

    private static PowerManager powerManager = new PowerManager();
    private APP App;

    private int powMode;
    private int level;
    private int voltage;
    private int state;
    private boolean charge;
    private boolean savePower = false;
    private String cpuFreq = "";
    private int timeCount;

    private PowerManager(){
    }

    //返回单例
    public static PowerManager getPowerManager(){
        return powerManager;
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

        (new Timer()).schedule(new getBatteryStatus(),2000, GET_BAT_INFO_TIME);
        (new Timer()).schedule(new savePowerTimer(),5000, 1000);
    }

    public void resetSavePowerTime(){
        timeCount = 0;
        if(savePower){
            normalPowerMode();
            savePower = false;
        }
    }


    //定时获取设备电池状态
    private class getBatteryStatus extends TimerTask implements Cmd.cmdResultListener {

        @Override
        public void run() {
            Cmd.execCmd("dumpsys battery", this);
            Cmd.execCmd("cat /sys/devices/system/cpu/cpu0/cpufreq/scaling_governor", this);
            Cmd.execCmd("cat /sys/devices/system/cpu/cpu0/cpufreq/cpuinfo_cur_freq", this);
        }

        @Override
        public void onResult(String cmd, String res, int value) {
            if (cmd.contains("battery")) {
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


                int l = Integer.parseInt(strLevel);
                int v = Integer.parseInt(strVoltage);
                int s = Integer.parseInt(strState);
//        boolean charge = (strCharge.contains("true"));

                if (level != l || voltage != v || state != s /* || this.charge != charge*/) {
                    level = l;
                    voltage = v;
                    state = s;
//            this.charge = charge;
                    broadcastBatteryInfo();
                }
            } else if (cmd.contains("scaling_governor") && res.equals("conservative") != savePower) {
                savePower = res.equals("conservative");
                broadcastBatteryInfo();
            } else if (cmd.contains("cpuinfo_cur_freq") && !cpuFreq.equals(res)) {
                cpuFreq = res;
                broadcastBatteryInfo();
            }

//        Log.d(TAG, "cmdGetBatteryStatus" + ":level = " + level + " state = " + state + " voltage = " + voltage + " charge = " + charge);

        }
    }

    private class savePowerTimer extends TimerTask{

        @Override
        public void run() {
            if(!savePower){
                if(timeCount++ >= SAVE_POWER_TIME) {
                    savePowerMode();
                    savePower = true;
                }
            }
        }
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
            broadcastBatteryInfo();
        }
    }

    private void broadcastBatteryInfo(){
        Bundle bundle = new Bundle();
        bundle.putInt(BAT_LEVEL,this.level);
        bundle.putInt(BAT_VOLTAGE,this.voltage);
        bundle.putInt(BAT_STATE,this.state);
        bundle.putBoolean(BAT_CHARGE,this.charge);
        bundle.putBoolean(SAVE_POWER,savePower);
        bundle.putString(CPU_FREQ,cpuFreq);
        APP.LocalBroadcast.send(APP.ACTION_POWER_INFO_UPDATE,bundle);

        Printf.d(TAG, "Battery: level = " + level + "  voltage = " + voltage + "  cpu_freq = " + cpuFreq);
    }

    private void savePowerMode(){
        String cmd = "echo \"conservative\" > /sys/devices/system/cpu/cpu0/cpufreq/scaling_governor";
        Cmd.execCmd(cmd);
//        Log.d(TAG,"savepower");
        Printf.d(TAG, "savepower mode");
    }

    private void normalPowerMode(){
        String cmd = "echo \"performance\" > /sys/devices/system/cpu/cpu0/cpufreq/scaling_governor";
        Cmd.execCmd(cmd);
//        Log.d(TAG,"normalPowerMode");
        Printf.d(TAG, "normalpower mode");
    }


//    private class getCpuFreqInfo extends TimerTask implements Cmd.cmdResultListener{
//
//        @Override
//        public void run() {
//            String cmd = "cat /sys/devices/system/cpu/cpu0/cpufreq/scaling_governor";
//            Cmd.execCmd(cmd,this);
//
//            cmd = "cat /sys/devices/system/cpu/cpu0/cpufreq/cpuinfo_cur_freq";
//            Cmd.execCmd(cmd,this);
//
//            Bundle bundle = new Bundle();
//            bundle.putString("GOVERNOR",cpuGovernor);
//            bundle.putString("FREQ",cpuFreq);
//            APP.LocalBroadcast.send(APP.ACTION_CPU_INFO_UPDATE,bundle);
//
////            Log.d(TAG,"getCpuFreqInfo");
//        }
//
//        @Override
//        public void onResult(String cmd, String res, int value) {
//            if(cmd.contains("scaling_governor")){
//                cpuGovernor = res;
//            }else if(cmd.contains("cpuinfo_cur_freq")){
//                cpuFreq = res;
//            }
//        }
//    }

}
