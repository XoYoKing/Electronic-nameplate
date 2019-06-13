package com.itc.ts8209a.module.network;

import android.content.Intent;
import android.os.Message;
import android.util.Log;

import com.itc.ts8209a.app.MyApplication;
import com.itc.ts8209a.widget.Cmd;
import com.itc.ts8209a.widget.Debug;

import java.util.Locale;
import java.util.Timer;
import java.util.TimerTask;

import static com.itc.ts8209a.app.AppConfig.*;

/**
 * Created by kuangyt on 2018/12/7.
 */

public class EthernetManager extends NetDevManager{
    private static final String TAG = "EthernetManager";

    public static final String DEV_NAME = "eth0";

    private static EthernetManager ethernetManager = new EthernetManager();
//    private static NetDevInfo netDevInfo;

    private EthernetManager(){
    }

    protected static EthernetManager getEthernetManager(){
        return ethernetManager;
    }

    @Override
    protected void init(MyApplication myApplication) {
        super.init(myApplication);
        Debug.d(TAG,"Ethernet Initialization");
//        netDevInfo = new NetDevInfo();
        netDevInfo.name = DEV_NAME;
        netDevInfo.type = TYPE_WIER_NET;
        getState = new Timer();
        getState.schedule(new TimerTask() {
            @Override
            public void run() {
                getNetDevSta();
            }
        }, 5000, GET_NETWOR_STA_TIME);
    }

    @Override
    protected void startup() {
        String cmd = String.format(Locale.ENGLISH,"netcfg %s %s",DEV_NAME,"up");
        Cmd.execCmd(cmd);
        myApplication.sendBroadcast(new Intent("com.android.action.enable_ethernet"));
    }

    @Override
    protected void shutdown() {
        String cmd = String.format(Locale.ENGLISH,"netcfg %s %s",DEV_NAME,"down");
        Cmd.execCmd(cmd);
        myApplication.sendBroadcast(new Intent("com.android.action.disable_ethernet"));
    }

    @Override
    protected void setDhcpEn() {
        super.setDhcpEn();
        new Thread(){
            @Override
            public void run() {
                try {
                    shutdown();
                    Thread.sleep(1000);
                    startup();
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }
        }.start();
    }

    @Override
    public NetDevInfo getDevInfo() {
        return netDevInfo;
    }

    @Override
    protected String devName() {
        return DEV_NAME;
    }

    @Override
    protected void action(Message msg) {

    }
}
