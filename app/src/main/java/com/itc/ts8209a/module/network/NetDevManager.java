package com.itc.ts8209a.module.network;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.Handler;
import android.os.Message;
import android.util.Log;

import com.itc.ts8209a.app.MyApplication;
import com.itc.ts8209a.widget.Cmd;
import com.itc.ts8209a.widget.Debug;
import com.itc.ts8209a.widget.General;

import java.net.Inet4Address;
import java.net.InetAddress;
import java.net.NetworkInterface;
import java.net.SocketException;
import java.util.Arrays;
import java.util.Enumeration;
import java.util.Locale;
import java.util.Timer;

import static com.itc.ts8209a.widget.General.addrStrToIntArr;

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
    protected static final int ACTION_RECONNECT_WIFI = 4;

    protected final String IFCONFIG = "ifconfig ";
    protected final String NETCFG = "netcfg";
    protected final String GETPROP = "getprop|grep ";

    //获取网络信息定时器
    protected Timer getState;
    //Application环境
    protected MyApplication myApplication;
    //网络参数
    protected static NetDevInfo netDevInfo;

    //网络数据更新监听器
    protected NetDevInfoUpdatedListener infoListener;

    //系统广播接收过滤初始化
    protected IntentFilter intentFilter = null;

    //用于子类实现并返回设备名称
    protected abstract String devName();

    //初始化
    protected void init(MyApplication myApplication) {
        this.myApplication = myApplication;

        setIntentFilter();
        this.myApplication.registerReceiver(new networkBroadcastReceiver(), intentFilter);

        netDevInfo = new NetDevInfo();
    }

    protected abstract void startup();

    protected abstract void shutdown();

    protected abstract NetDevInfo getDevInfo();

    protected abstract void refreshNet();

    //设置网络参数
    protected void setNetDevInfo(final int[] ip, final int[] mask, final int[] gw) {
        netDevInfo.ip = ip;
        netDevInfo.mask = mask;
        netDevInfo.gw = gw;
        netDevInfo.dhcp = false;
        new Thread() {
            @Override
            public void run() {
                String cmd = String.format(Locale.ENGLISH, "ifconfig %s %d.%d.%d.%d netmask %d.%d.%d.%d", devName(), ip[0], ip[1], ip[2], ip[3], mask[0], mask[1], mask[2], mask[3]);
                Cmd.execCmd(cmd);
                cmd = String.format(Locale.ENGLISH, "route add default gw %d.%d.%d.%d dev %s", gw[0], gw[1], gw[2], gw[3], devName());
                Cmd.execCmd(cmd);
            }
        }.start();
    }

    //设置网络IP地址自动分配
    protected void setDhcpEn() {
        netDevInfo.dhcp = true;
    }

    //设置广播接受过滤
    protected void setIntentFilter() {
        intentFilter = new IntentFilter();
        intentFilter.addAction(android.net.wifi.WifiManager.NETWORK_STATE_CHANGED_ACTION);
    }

    //网络参数
    public static class NetDevInfo {
        protected int type = 0;
        protected String name = "";
        protected String mac = "00:00:00:00:00:00";
        protected int[] ip = {0, 0, 0, 0};
        protected int[] gw = {0, 0, 0, 0};
        protected int[] mask = {0, 0, 0, 0};
        protected String ssid = "";
        protected int rssi = -200;
        protected boolean netEn = false;
        protected boolean dhcp = true;
        protected boolean devEn = false;

        protected void reset() {
            for (int i = 0; i < 4; i++) {
                ip[i] = 0;
                gw[i] = 0;
                mask[i] = 0;
            }
            mac = "";
            ssid = "";
            rssi = -200;
        }

        public int getNetType() {
            return type;
        }

        public String getDevName() {
            return name;
        }

        public String getMac() {
            return mac;
        }

        public int[] getIp() {
            return ip;
        }

        public int[] getGw() {
            return gw;
        }

        public int[] getMask() {
            return mask;
        }

        public String getSsid() {
            return ssid;
        }

        public int getRssi() {
            return rssi;
        }

        public boolean getNetEn() {
            return netEn;
        }

        public boolean getDevEn() {
            return devEn;
        }

        public boolean getDhcp() {
            return dhcp;
        }
    }

    public interface NetDevInfoUpdatedListener {
        void infoUpdate(String devName);
    }

    protected void setNetDevInfoUpdatedListener(NetDevInfoUpdatedListener listener) {
        infoListener = listener;
    }

    //通过命令行获取以太网信息
    protected void getNetDevSta() {
//        Cmd.execCmd(IFCONFIG + devName(), this);
//        Cmd.execCmd(GETPROP + devName(), this);
//        Cmd.execCmd(NETCFG, this);

//        Log.d(TAG,"ip : " + getIpAddressString());
//        isConnected();
    }

    //广播处理方法
    protected void broadcastProcessor(Context context, Intent intent) {

    }

    //广播接收类
    private class networkBroadcastReceiver extends BroadcastReceiver {

        @Override
        public void onReceive(Context context, Intent intent) {
            broadcastProcessor(context, intent);
        }
    }

    protected Handler actionHandler = new Handler() {
        @Override
        public void handleMessage(Message msg) {
            action(msg);
        }
    };

    protected abstract void action(Message msg);

    protected boolean isConnected() {
        ConnectivityManager connectivityManager = (ConnectivityManager) myApplication.getSystemService(Context.CONNECTIVITY_SERVICE);
        NetworkInfo networkInfo = connectivityManager.getActiveNetworkInfo();

        return networkInfo != null && networkInfo.isAvailable();

    }

    protected String getIpAddressString() {
        try {
            for (Enumeration<NetworkInterface> enNetI = NetworkInterface
                    .getNetworkInterfaces(); enNetI.hasMoreElements(); ) {
                NetworkInterface netI = enNetI.nextElement();
                for (Enumeration<InetAddress> enumIpAddr = netI.getInetAddresses(); enumIpAddr.hasMoreElements(); ) {
                    InetAddress inetAddress = enumIpAddr.nextElement();
                    if (inetAddress instanceof Inet4Address && !inetAddress.isLoopbackAddress()) {
                        return inetAddress.getHostAddress();
                    }
                }
            }
        } catch (SocketException e) {
            e.printStackTrace();
        }
        return "0.0.0.0";
    }


    @Override
    public void onResult(String cmd, String res, int value) {
        final String con_result = "dhcp." + devName() + ".result";
        final String con_gw = "dhcp." + devName() + ".gateway";
        final String index_gw = "[dhcp." + devName() + ".gateway]: [";
        final String index_ip = "][dhcp." + devName() + ".ipaddress]";
        final String index_result = "[dhcp." + devName() + ".result]: [";
        final String index_serv = "][dhcp." + devName() + ".server]";

        try {
            if (cmd.contains(IFCONFIG)) {
                int[] ip, mask = new int[4];

                if (res.contains("ip") && res.contains("mask")) {
//                    ip = addrStrToIntArr(res.substring(res.indexOf("ip ") + 3, res.indexOf(" mask")));
                    mask = addrStrToIntArr(res.substring(res.indexOf("mask ") + 5, res.indexOf(" flags")));
//                    Log.d(TAG, "ip : " + ip[0] + ip[1] + ip[2] + ip[3] + "  mask : " + mask[0] + mask[1] + mask[2] + mask[3]);
                }

//                else {
//                    Arrays.fill(ip, 0);
//                    Arrays.fill(mask, 0);
//                    infoListener.infoUpdate(devName());
//                    return;
//                }

                ip = addrStrToIntArr(getIpAddressString());

                if ((infoListener != null) && (!General.ContrastArray(ip, netDevInfo.ip) || !General.ContrastArray(mask, netDevInfo.mask))) {
                    netDevInfo.ip = ip;
                    netDevInfo.mask = mask;
                    infoListener.infoUpdate(devName());
                }
            } else if (cmd.contains(GETPROP)) {
                int gw[] = new int[4];
                boolean netEn = false;

                if (res.contains(con_gw)) {
//                    gw = netDevInfo.dhcp ? addrStrToIntArr(res.substring(res.indexOf(index_gw) + index_gw.length(), res.indexOf(index_ip))) : netDevInfo.gw;
                    gw = addrStrToIntArr(res.substring(res.indexOf(index_gw) + index_gw.length(), res.indexOf(index_ip)));
                } else
                    Arrays.fill(gw, 0);

                if (res.contains(index_result) && res.contains(index_serv)) {
                    netEn = (netDevInfo.type == TYPE_WIER_NET) || ((res.substring(res.indexOf(index_result) + index_result.length(), res.indexOf(index_serv))).equals("ok"));
                }
//                netEn = res.contains(con_result) && ((res.substring(res.indexOf(index_result) + index_result.length(), res.indexOf(index_serv))).equals("ok"));

//                Log.d(TAG,"gw : "+gw[0]+gw[1]+gw[2]+gw[3]+"  netEn : "+netEn);
                if ((infoListener != null) && (!General.ContrastArray(gw, netDevInfo.gw) || netDevInfo.netEn != netEn)) {
                    netDevInfo.netEn = netEn;
                    netDevInfo.gw = gw;
                    infoListener.infoUpdate(devName());
                }
            } else if (cmd.contains(NETCFG)) {
                boolean devEn = res.contains(devName()) && res.substring(res.indexOf(devName()), res.indexOf(devName()) + 30).contains("UP");
                String devNetcfg = res.substring(res.indexOf(devName()), res.length());
                String mac = devNetcfg.substring(devNetcfg.indexOf(':') - 2, devNetcfg.indexOf(':') + 15);
//                Log.d(TAG,"mac : "+mac+"  "+devEn );
                if (infoListener != null && (devEn != netDevInfo.devEn || netDevInfo.mac == null || !mac.equals(netDevInfo.mac))) {
                    netDevInfo.devEn = devEn;
                    netDevInfo.mac = mac;
                    infoListener.infoUpdate(devName());
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
