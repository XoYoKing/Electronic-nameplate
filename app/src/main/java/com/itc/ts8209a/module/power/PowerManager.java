package com.itc.ts8209a.module.power;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.BatteryManager;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;

import com.itc.ts8209a.app.MyApplication;
import com.itc.ts8209a.module.network.EthernetManager;
import com.itc.ts8209a.widget.Cmd;
import com.itc.ts8209a.widget.Debug;

import java.util.Timer;
import java.util.TimerTask;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static com.itc.ts8209a.app.AppConfig.*;
import static com.itc.ts8209a.widget.Cmd.KEY_RES;

/**
 * Created by kuangyt on 2018/8/21.
 */

public class PowerManager {

    private static final String TAG = "PowerManager";

    public static final String BAT_LEVEL = "BAT_LEVEL";
    public static final String BAT_VOLTAGE = "BAT_VOLTAGE";
    public static final String BAT_STATE = "BAT_STATE";
    public static final String BAT_CHARGE = "BAT_CHARGE";
    public static final String POWER_MODE = "POWER_MODE";
    public static final String SAVE_POWER = "SAVE_POWER";
    public static final String CPU_FREQ = "CPU_FREQ";

    public static final int BATTERY_MODE = 1;
    public static final int POE_MODE = 2;

    private static PowerManager powerManager = new PowerManager();

    private static final int ELECTRIC_QUANTITY_MAX_VOLTAGE = 4000;
    private static final int ELECTRIC_QUANTITY_MIN_VOLTAGE = 3550;
    private static final int CHARGING_CORRECTION_VOLTAGE = 350;
    private static final int BAT_MAX_VOLTAGE = 4180;

    private int powMode;
    private int level;
    private int voltage;
    private int state;
    private boolean charge;
    private boolean savePower = false;
    private String cpuFreq = "";
    private int timeCount;
    private boolean isGetVoltage = false;

    private PowerManager() {
    }

    //返回单例
    public static PowerManager getPowerManager() {
        return powerManager;
    }

    //获取电量
    public int getLevel() {
        return level;
    }

    //获取电压值
    public int getVoltage() {
        return voltage;
    }

    //获取电池状态
    public int getStatus() {
        return state;
    }

    //获取充电状态
    public boolean getCharge() {
        return charge;
    }

    //获取供电方式
    public int getPowerMode() {
        return powMode;
    }


    //启动定时获取电池电量状态线程
    public void init(final MyApplication app) {

        //暂时没有方法用判断电源输入的方法来确定供电方式，因此只能用判断网络属性来确定供电方式
        Cmd.execCmd("netcfg", new Handler() {
            @Override
            public void handleMessage(Message msg) {
                Bundle bundle = (Bundle) msg.obj;
                String res = bundle.getString(KEY_RES);
                if (res.contains(EthernetManager.DEV_NAME)) {
                    powMode = POE_MODE;
                    level = 101;
                } else {
                    powMode = BATTERY_MODE;

                    IntentFilter intentFilter = new IntentFilter();
                    intentFilter.addAction(Intent.ACTION_BATTERY_CHANGED);
                    intentFilter.addAction(Intent.ACTION_POWER_CONNECTED);
                    intentFilter.addAction(Intent.ACTION_POWER_DISCONNECTED);
                    app.registerReceiver(new betteryStatusReceiver(), intentFilter);

                    (new Timer()).schedule(new getPowerStatus(), 1000, GET_BAT_INFO_TIME);
//                    (new Timer()).schedule(new savePowerTimer(), 5000, 1000);
                }

                broadcastPowerInfo();
            }

        });

//        String cmd = "echo \"performance\" > /sys/devices/system/cpu/cpu0/cpufreq/scaling_governor";
//        Cmd.execCmd(cmd);
    }

//    public void resetSavePowerTime() {
//        timeCount = 0;
//        if (savePower) {
//            normalPowerMode();
//            savePower = false;
//        }
//    }


    //定时获取设备状态
    private class getPowerStatus extends TimerTask implements Cmd.cmdResultListener {

        @Override
        public void run() {
            Cmd.execCmd("dumpsys battery", this);
//            Cmd.execCmd("cat /sys/devices/system/cpu/cpu0/cpufreq/scaling_governor", this);
//            Cmd.execCmd("cat /sys/devices/system/cpu/cpu0/cpufreq/cpuinfo_cur_freq", this);
        }

        @Override
        public void onResult(String cmd, String res, int value) {

            //从命令返回信息中提取电池信息
            if (cmd.contains("battery")) {
                String strLevel = res.substring(res.indexOf("level:"));
                String strVoltage = res.substring(res.indexOf("voltage:"));
                String strState = res.substring(res.indexOf("state:"));
                String strAcPow = res.substring(res.indexOf("AC powered:"));
                String strUsbPow = res.substring(res.indexOf("USB powered:"));


                strLevel = strLevel.substring(0, strLevel.indexOf("scale"));
                strVoltage = strVoltage.substring(0, strVoltage.indexOf("current now"));
                strState = strState.substring(0, strState.indexOf("health"));
                strAcPow = strAcPow.substring(0, strAcPow.indexOf("USB powered"));
                strUsbPow = strUsbPow.substring(0, strUsbPow.indexOf("Wireless powered"));


                Matcher m1 = Pattern.compile("\\d+").matcher(strLevel);
                m1.find();
                strLevel = m1.group();

                Matcher m2 = Pattern.compile("\\d+").matcher(strVoltage);
                m2.find();
                strVoltage = m2.group();

                Matcher m3 = Pattern.compile("\\d+").matcher(strState);
                m3.find();
                strState = m3.group();


                int vol = Integer.parseInt(strVoltage);
                int sta = Integer.parseInt(strState);
                boolean cha = (strUsbPow.contains("true") || strAcPow.contains("true"));
                int lev = vol == 0 ? 0 : voltageToLevel(vol);

                Debug.d(TAG, "onResult: vol = " + vol + "  lev = " + lev + " sta = " + sta + "  cha = " + cha);

//                if (voltage != vol || state != sta || charge != cha) {
//                    level = charge ? (level >= l ? level : level + 1) : (level > l ? level - 1 : level);
                    if (vol != 0) {
                        if (!isGetVoltage) {
                            isGetVoltage = true;
                            level = lev;
                        } else {
                            if (charge)
                                level = lev > level ? level + 1 : level;
                            else
                                level = level > lev ? level - 1 : level;
                        }
                    }
                    voltage = vol;
                    state = sta;
                    charge = cha;
                    broadcastPowerInfo();
//                }
            }

            //从命令返回信息中提取cpu运行状态信息
            else if (cmd.contains("scaling_governor") && res.equals("conservative") != savePower) {
                savePower = res.equals("conservative");
                broadcastPowerInfo();
            }

            //从命令返回信息中提取cpu频率信息
            else if (cmd.contains("cpuinfo_cur_freq") && !cpuFreq.equals(res)) {
                cpuFreq = res;
                broadcastPowerInfo();
            }

        }
    }

//    private class savePowerTimer extends TimerTask {
//
//        @Override
//        public void run() {
//            //非充电下
//            if (!charge) {
//                if (!savePower) {
//                    if (timeCount++ >= ENTER_SAVE_POWER_TIME) {
//                        savePowerMode();
//                        savePower = true;
//                        timeCount = 0;
//                    }
//                } else {
//                    if (timeCount++ >= EXIT_SAVE_POWER_TIME) {
//                        normalPowerMode();
//                        savePower = false;
//                        timeCount = 0;
//                    }
//                }
//            }
//
//            //当设备处于充电状态
//            else {
//                if (savePower) {
//                    normalPowerMode();
//                    savePower = false;
//                    timeCount = 0;
//                }
//            }
//        }
//    }

    //接收系统广播（静态注册），充电状态
    public class betteryStatusReceiver extends BroadcastReceiver {

        @Override
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();

            if (action.equals(Intent.ACTION_POWER_CONNECTED)) {
                charge = true;
            } else if (action.equals(Intent.ACTION_POWER_DISCONNECTED)) {
                charge = false;
            } else if (action.equals(Intent.ACTION_BATTERY_CHANGED)) {
//                level = intent.getIntExtra(android.os.BatteryManager.EXTRA_LEVEL,0);
//                int v = intent.getIntExtra(BatteryManager.EXTRA_VOLTAGE, -1);
                if (!isGetVoltage) {
                    level = intent.getIntExtra(android.os.BatteryManager.EXTRA_LEVEL, 0);
                }
            }
            broadcastPowerInfo();
        }
    }

    private void broadcastPowerInfo() {
        Bundle bundle = new Bundle();
        bundle.putInt(POWER_MODE, powMode);
        bundle.putInt(BAT_LEVEL, this.level);
        bundle.putInt(BAT_VOLTAGE, this.voltage);
        bundle.putInt(BAT_STATE, this.state);
        bundle.putBoolean(BAT_CHARGE, this.charge);
        bundle.putBoolean(SAVE_POWER, savePower);
        bundle.putString(CPU_FREQ, cpuFreq);
        MyApplication.LocalBroadcast.send(MyApplication.ACTION_POWER_INFO_UPDATE, bundle);

        Debug.d(TAG, "PowerInfo Battery: level = " + level + "  voltage = " + voltage + " charge = " + charge + "  cpu_freq = " + cpuFreq);
    }

//    private void broadcastPowerMode(){
//        Bundle bundle = new Bundle();
//        bundle.putInt(POWER_MODE,powMode);
//        MyApplication.LocalBroadcast.send(MyApplication.ACTION_POWER_MODE_UPDATE,bundle);
//    }

//    private void savePowerMode() {
//        String cmd = "echo \"conservative\" > /sys/devices/system/cpu/cpu0/cpufreq/scaling_governor";
//        Cmd.execCmd(cmd);
//        Debug.d(TAG, "savepower mode");
//    }

//    private void normalPowerMode() {
//        String cmd = "echo \"performance\" > /sys/devices/system/cpu/cpu0/cpufreq/scaling_governor";
//        Cmd.execCmd(cmd);
//        Debug.d(TAG, "normalpower mode");
//    }

    private int voltageToLevel(int v) {
        int level = 0;
        int correction, max_voltage, min_voltage;

        correction = charge ? CHARGING_CORRECTION_VOLTAGE : 0;
        max_voltage = ELECTRIC_QUANTITY_MAX_VOLTAGE + correction > BAT_MAX_VOLTAGE ? BAT_MAX_VOLTAGE : ELECTRIC_QUANTITY_MAX_VOLTAGE + correction;
        min_voltage = ELECTRIC_QUANTITY_MIN_VOLTAGE + correction;

        if (v <= max_voltage && v >= min_voltage) {
            level = ((v - min_voltage) * 100) / (max_voltage - min_voltage);
        } else if (v > max_voltage)
            level = 100;

        else if (v < min_voltage)
            level = 0;

        return level;
    }
}
