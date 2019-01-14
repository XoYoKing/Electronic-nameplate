package com.jackie.ts8209a.AppModule.Network;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.Handler;
import android.os.Message;
import android.util.Log;

import com.jackie.ts8209a.AppModule.APP;
import com.jackie.ts8209a.AppModule.Tools.Cmd;
import com.jackie.ts8209a.AppModule.Tools.General;

import java.util.Locale;
import java.util.Timer;

import static com.jackie.ts8209a.AppModule.Tools.General.addrStrToInt;

/**
 * Created by kuangyt on 2018/12/12.
 */

public abstract class NetDevManager implements Cmd.cmdResultListener {
    private static final String TAG = "NetDevManager";

    public static final int TYPE_WIER_NET = 1;
    public static final int TYPE_WIRELESS = 2;

    protected static final int ACTION_WIFI_SELECT = 1;
    protected static final int ACTION_REMOVE_WIFI = 2;
    protected static final int ACTION_REMOVE_ALL_WIFI = 3;

    protected final String IFCONFIG = "ifconfig ";
    protected final String NETCFG = "netcfg";
    protected final String GETPROP = "getprop|grep ";

    protected final int getNetDevStaTime = 5 * 1000;//10S

    //获取网络信息定时器
    protected Timer getState;
    //Application环境
    protected APP App;
    //网络参数
    protected static NetDevInfo netDevInfo;

    //网络数据更新监听器
    protected NetDevInfoUpdatedListener infoListener;

    //系统广播接收过滤初始化
    protected IntentFilter intentFilter = null;

    //用于子类实现并返回设备名称
    protected abstract String devName();

    //初始化
    protected void init(APP app){
        App = app;

        setIntentFilter();
        App.registerReceiver(new networkBroadcastReceiver(),intentFilter);

    }

    protected abstract void startup();

    protected abstract void shutdown();

    protected abstract NetDevInfo getDevInfo();

    //设置网络参数
    protected void setNetDevInfo(final int[] ip,final int[] mask,final int[] gw){
        netDevInfo.ip = ip;
        netDevInfo.mask = mask;
        netDevInfo.gw = gw;
        netDevInfo.dhcp = false;
        new Thread(){
            @Override
            public void run() {
                String cmd = String.format(Locale.ENGLISH,"ifconfig %s %d.%d.%d.%d netmask %d.%d.%d.%d",devName(),ip[0], ip[1], ip[2], ip[3],mask[0], mask[1], mask[2], mask[3]);
                Cmd.execCmd(cmd);
                cmd = String.format(Locale.ENGLISH,"route add default gw %d.%d.%d.%d dev %s",gw[0], gw[1], gw[2], gw[3],devName());
                Cmd.execCmd(cmd);
            }
        }.start();
    }
    //设置网络IP地址自动分配
    protected void setDhcpEn(){
        netDevInfo.dhcp = true;
    }

    //设置广播接受过滤
    protected void setIntentFilter(){
        intentFilter = new IntentFilter();
        intentFilter.addAction(android.net.wifi.WifiManager.NETWORK_STATE_CHANGED_ACTION);
    }

    //网络参数
    public static class NetDevInfo {
        protected int type = 0;
        protected String name = "";
        protected String mac;
        protected int[] ip = {0,0,0,0};
        protected int[] gw = {0,0,0,0};
        protected int[] mask = {0,0,0,0};
        protected String ssid = "";
        protected int rssi = -200;
        protected boolean netEn = false;
        protected boolean dhcp = true;
        protected boolean devEn = false;

        protected void reset(){
            for(int i=0;i<4;i++){
                ip[i] = 0;
                gw[i] = 0;
                mask[i] = 0;
            }
            mac = "";
            ssid = "";
            rssi = -200;
        }

        public int getNetType(){
            return type;
        }

        public String getDevName(){
            return name;
        }

        public String getMac(){
            return mac;
        }

        public int[] getIp(){
            return ip;
        }

        public int[] getGw(){
            return gw;
        }

        public int[] getMask(){
            return mask;
        }

        public String getSsid(){
            return ssid;
        }

        public int getRssi(){
            return rssi;
        }

        public boolean getNetEn(){
            return netEn;
        }

        public boolean getDevEn(){
            return devEn;
        }

        public boolean getDhcp(){
            return dhcp;
        }
    }

    public interface NetDevInfoUpdatedListener{
        void infoUpdate(String devName);
    }

    protected void setNetDevInfoUpdatedListener(NetDevInfoUpdatedListener listener){
        infoListener = listener;
    }

    //通过命令行获取以太网信息
    protected void getNetDevSta() {
        Cmd.execCmd(IFCONFIG+ devName(), this);
        Cmd.execCmd(GETPROP+ devName(), this);
        Cmd.execCmd(NETCFG, this);
    }

    //广播处理方法
    protected void broadcastProcessor(Context context,Intent intent){

    }

    //广播接收类
    private class networkBroadcastReceiver extends BroadcastReceiver{

        @Override
        public void onReceive(Context context, Intent intent) {
            broadcastProcessor(context,intent);
        }
    }

    protected Handler actionHandler = new Handler(){
        @Override
        public void handleMessage(Message msg) {
            action(msg);
        }
    };

    protected abstract void action(Message msg);

    private void connectManager(){
        ConnectivityManager connectivityManager = (ConnectivityManager) App.getSystemService(Context.CONNECTIVITY_SERVICE);
        NetworkInfo networkInfo = connectivityManager.getActiveNetworkInfo();
        if(networkInfo != null){
            int type = networkInfo.getType();

            Log.d(TAG,"activity network = "+ type);

            if(type == ConnectivityManager.TYPE_WIFI)
                Log.d(TAG,"activity network wifi");
            else if(type == ConnectivityManager.TYPE_ETHERNET)
                Log.d(TAG,"activity netork ethernet");
        }
    }

    @Override
    public void onResult(String cmd, String res, int value) {
        final String con_result = "dhcp."+devName()+".result";
        final String con_gw = "dhcp."+devName()+".gateway";
        final String index_gw = "[dhcp."+devName()+".gateway]: [";
        final String index_ip = "][dhcp."+devName()+".ipaddress]";
        final String index_result = "[dhcp."+devName()+".result]: [";
        final String index_serv = "][dhcp."+devName()+".server]";

        try {
//            Log.d(TAG,cmd+" : "+res);
            if (cmd.contains(IFCONFIG) && res.contains("ip") && res.contains("mask")) {
                int[] ip = addrStrToInt(res.substring(res.indexOf("ip ") + 3, res.indexOf(" mask")));
                int[] mask = addrStrToInt(res.substring(res.indexOf("mask ") + 5, res.indexOf(" flags")));
                if((infoListener != null) && (!General.ContrastArray(ip,netDevInfo.ip) || !General.ContrastArray(mask,netDevInfo.mask))){
                    netDevInfo.ip = ip;
                    netDevInfo.mask = mask;
                    infoListener.infoUpdate(devName());
                }
            } else if (cmd.contains(GETPROP) && res.contains(con_result) && res.contains(con_gw)) {
                int gw[] = netDevInfo.dhcp ? addrStrToInt(res.substring(res.indexOf(index_gw) + index_gw.length(), res.indexOf(index_ip))) : netDevInfo.gw;
                boolean netEn = ((res.substring(res.indexOf(index_result) + index_result.length(), res.indexOf(index_serv))).equals("ok"));
                if((infoListener != null) && (!General.ContrastArray(gw,netDevInfo.gw) || netDevInfo.netEn != netEn)){
                    netDevInfo.netEn = netEn;
                    netDevInfo.gw = gw;
                    infoListener.infoUpdate(devName());
                }
            } else if (cmd.contains(NETCFG)) {
                boolean devEn = res.contains(devName()) && res.substring(res.indexOf(devName()), res.indexOf(devName()) + 30).contains("UP");
                if(infoListener != null && devEn != netDevInfo.devEn) {
                    netDevInfo.devEn = devEn;
                    infoListener.infoUpdate(devName());
                }
            }
        }catch (Exception e){
            e.printStackTrace();
        }
    }
}
