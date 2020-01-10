package com.itc.ts8209a.module.network;

import android.content.Context;
import android.content.Intent;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.Message;
import android.provider.Settings;
import android.util.Log;

import com.itc.ts8209a.app.MyApplication;
import com.itc.ts8209a.widget.Cmd;
import com.itc.ts8209a.widget.General;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.util.Arrays;
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

//    private getEthManager() {
//        try {
//            //获取ETHERNET_SERVICE参数
//            String ETHERNET_SERVICE = (String) Context.class.getField("ETHERNET_SERVICE").get(null);
//
//            Class<?> ethernetManagerClass = Class.forName("android.net.EthernetManager");
//
//            Class<?> ipConfigurationClass = Class.forName("android.net.IpConfiguration");
//
//            //获取ethernetManager服务对象
//            Object ethernetManager = myApplication.getSystemService(ETHERNET_SERVICE);
//
//            Object getConfiguration = ethernetManagerClass.getDeclaredMethod("getConfiguration").invoke(ethernetManager);
//
//            Log.e(TAG, "ETHERNET_SERVICE : " + ETHERNET_SERVICE);
//
//            //获取在EthernetManager中的抽象类mService成员变量
//            Field mService = ethernetManagerClass.getDeclaredField("mService");
//
//            //修改private权限
//            mService.setAccessible(true);
//
//            //获取抽象类的实例化对象
//            Object mServiceObject = mService.get(ethernetManager);
//
//            Class<?> iEthernetManagerClass = Class.forName("android.net.IEthernetManager");
//
//            Method[] methods = iEthernetManagerClass.getDeclaredMethods();
//
//            for (Method ms : methods) {
//
//                if (ms.getName().equals("setEthernetEnabled")) {
//
//                    ms.invoke(mServiceObject, true);
//
//                    Log.e(TAG, "mServiceObject : " + mServiceObject);
//
//                }
//
//            }
//            Class<?> staticIpConfig = Class.forName("android.net.StaticIpConfiguration");
//
//            Constructor<?> staticIpConfigConstructor = staticIpConfig.getDeclaredConstructor(staticIpConfig);
//
//            Object staticIpConfigInstance = staticIpConfig.newInstance();
//
//            //获取LinkAddress里面只有一个String类型的构造方法
//            Constructor<?> linkAddressConstructor = LinkAddress.class.getDeclaredConstructor(String.class);
//
//            //实例化带String类型的构造方法
////            LinkAddress linkAddress = (LinkAddress) linkAddressConstructor.newInstance("192.168.1.10/24");//192.168.1.1/24--子网掩码长度,24相当于255.255.255.0
//
//            Class<?> inetAddressClass = Class.forName("java.net.InetAddress");
//
//            //默认网关参数
//            byte[] bytes = new byte[]{(byte) 192, (byte) 168, 3, 1};
//
//            Constructor<?>[] inetAddressConstructors = inetAddressClass.getDeclaredConstructors();
//
//            for (Constructor inetc : inetAddressConstructors) {
//
//                //获取有三种参数类型的构造方法
//                if (inetc.getParameterTypes().length == 3) {
//
//                    //修改权限
//                    inetc.setAccessible(true);
//
//                    WifiManager wm = (WifiManager) getSystemService(WIFI_SERVICE);
//
//                    int ipAddressInt = wm.getConnectionInfo().getIpAddress();
//
//                    //hostName主机名
//                    String hostName = String.format(Locale.getDefault(), "%d.%d.%d.%d", (ipAddressInt & 0xff), (ipAddressInt >> 8 & 0xff), (ipAddressInt >> 16 & 0xff), (ipAddressInt >> 24 & 0xff));
//
//                    inetAddress = (InetAddress) inetc.newInstance(2, bytes, hostName);
//
//                }
//
//            }
//            //获取staticIpConfig中所有的成员变量
//            Field[] declaredFields = staticIpConfigInstance.getClass().getDeclaredFields();
//
//            for (Field f : declaredFields) {
//
//                //设置成员变量的值
//                if (f.getName().equals("ipAddress")) {
//
//                    //设置IP地址和子网掩码
//                    f.set(staticIpConfigInstance, linkAddress);
//
//                } else if (f.getName().equals("gateway")) {
//
//                    //设置默认网关
////                    f.set(staticIpConfigInstance, inetAddress);
//
//                } else if (f.getName().equals("domains")) {
//
//                    f.set(staticIpConfigInstance, "");
//
//                }/*else if (f.getName().equals("dnsServers")){
//
//                //设置DNS
//                    f.set(staticIpConfigInstance,new ArrayList<InetAddress>());
//
//                }*/
//
//            }
//            Object staticInstance = staticIpConfigConstructor.newInstance(staticIpConfigInstance);
//
//            //存放ipASSignment枚举类参数的集合
//            HashMap ipAssignmentMap = new HashMap();
//
//            //存放proxySettings枚举类参数的集合
//            HashMap proxySettingsMap = new HashMap();
//
//            Class<?>[] enumClass = ipConfigurationClass.getDeclaredClasses();
//
//            for (Class enumC : enumClass) {
//
//                //获取枚举数组
//                Object[] enumConstants = enumC.getEnumConstants();
//
//                if (enumC.getSimpleName().equals("ProxySettings")) {
//
//                    for (Object enu : enumConstants) {
//
//                        //设置代理设置集合 STATIC DHCP UNASSIGNED PAC
//                        proxySettingsMap.put(enu.toString(), enu);
//
//                    }
//
//                } else if (enumC.getSimpleName().equals("IpAssignment")) {
//
//                    for (Object enu : enumConstants) {
//
//                        //设置以太网连接模式设置集合 STATIC DHCP UNASSIGNED
//                        ipAssignmentMap.put(enu.toString(), enu);
//
//                    }
//
//                }
//
//            }
//            //获取ipConfiguration类的构造方法
//            Constructor<?>[] ipConfigConstructors = ipConfigurationClass.getDeclaredConstructors();
//
//            for (Constructor constru : ipConfigConstructors) {
//
//                //获取ipConfiguration类的4个参数的构造方法
//                if (constru.getParameterTypes().length == 4) {//设置以上四种类型
//
//                    //初始化ipConfiguration对象,设置参数
//                    ipConfigurationInstance = constru.newInstance(ipAssignmentMap.get("STATIC"), proxySettingsMap.get("NONE"), staticInstance, ProxyInfo.buildDirectProxy(null, 0));
//
//                }
//
//            }
//
//            Log.e(TAG, "ipCon : " + ipConfigurationInstance);
//
//            //获取ipConfiguration类中带有StaticIpConfiguration参数类型的名叫setStaticIpConfiguration的方法
//            Method setStaticIpConfiguration = ipConfigurationClass.getDeclaredMethod("setStaticIpConfiguration", staticIpConfig);
//
//            //修改private方法权限
//            setStaticIpConfiguration.setAccessible(true);
//
//            //在ipConfiguration对象中使用setStaticIpConfiguration方法,并传入参数
//            setStaticIpConfiguration.invoke(ipConfigurationInstance, staticInstance);
//
//            Object ethernetManagerInstance = ethernetManagerClass.getDeclaredConstructor(Context.class, iEthernetManagerClass).newInstance(this, mServiceObject);
//
//            ethernetManagerClass.getDeclaredMethod("setConfiguration", ipConfigurationClass).invoke(ethernetManagerInstance, ipConfigurationInstance);
//
//            Log.e(TAG, "getConfiguration : " + getConfiguration.toString());
//        } catch (ClassNotFoundException e) {
//            e.printStackTrace();
//        } catch (InstantiationException e) {
//            e.printStackTrace();
//        } catch (IllegalAccessException e) {
//            e.printStackTrace();
//        } catch (NoSuchMethodException e) {
//            e.printStackTrace();
//        } catch (InvocationTargetException e) {
//            e.printStackTrace();
//        } catch (NoSuchFieldException e) {
//            e.printStackTrace();
//        }
//    }

    protected static EthernetManager getEthernetManager(){
        return ethernetManager;
    }

    @Override
    protected void init(MyApplication myApplication) {
        super.init(myApplication);
//        Debug.d(TAG,"Ethernet Initialization");
//        netDevInfo = new NetDevInfo();
        netDevInfo.name = DEV_NAME;
        netDevInfo.type = TYPE_WIER_NET;
        netDevInfo.devEn = true;
        getState = new Timer();
        getState.schedule(new TimerTask() {
            @Override
            public void run() {
                getNetDevSta();
            }
        }, 5000, GET_NETWOR_STA_TIME);
    }



    protected void getNetDevSta() {
        String mac = "00:00:00:00:00:00";
        int ip[] = {0,0,0,0},gw[] = {0,0,0,0},mask[] = {0,0,0,0};
        boolean netEn;

        try {
            netEn = isConnected();
            mac = getPropInfo("cat /sys/class/net/eth0/address");
            if(netEn){
                ip = General.addrStrToIntArr(getIpAddressString());
                gw = General.addrStrToIntArr(getPropInfo("getprop dhcp."+DEV_NAME+".gateway"));
                mask = General.addrStrToIntArr(getPropInfo("getprop dhcp."+DEV_NAME+".mask"));
            }
            else{
                Arrays.fill(ip,0);
                Arrays.fill(gw,0);
                Arrays.fill(mask,0);
                rebootEthernet();
            }

            if ((infoListener != null) && (netDevInfo.netEn != netEn || !mac.equals(netDevInfo.mac) || !General.ContrastArray(netDevInfo.ip, ip) ||
                    !General.ContrastArray(netDevInfo.gw, gw) || !General.ContrastArray(netDevInfo.mask, mask))) {

                netDevInfo.mac = mac;
                netDevInfo.ip = ip;
                netDevInfo.gw = gw;
                netDevInfo.mask = mask;
                netDevInfo.netEn = netEn;
                infoListener.infoUpdate(DEV_NAME);
            }
        } catch (NullPointerException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private String getPropInfo(String name) throws IOException {
        String str = "",result = "";
        DataInputStream stream = new DataInputStream((Runtime.getRuntime().exec(name)).getInputStream());

        while ((str = stream.readLine()) != null) {
            result += str;
        }

        return result;
    }

    private void rebootEthernet() {
        try {
            Process p = Runtime.getRuntime().exec("sh");
            DataOutputStream stream = new DataOutputStream(p.getOutputStream());
            Log.d(TAG, "reboot ethernet");
            stream.writeBytes("netcfg eth0 down\n");
            Thread.sleep(2000);
            stream.writeBytes("netcfg eth0 up\n");
            Thread.sleep(500);
            stream.close();
            p.destroy();
            Log.d(TAG, "reboot finish");
        } catch (IOException e) {
            e.printStackTrace();
        } catch (InterruptedException e) {
            e.printStackTrace();
        }

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
    protected void refreshNet() {
        setDhcpEn();
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
