package com.jackie.ts8209a.AppModule.Network;

import android.content.Context;
import android.content.Intent;
import android.net.NetworkInfo;
import android.net.wifi.ScanResult;
import android.net.wifi.WifiConfiguration;
import android.os.Message;
import android.os.Parcelable;
import android.util.Log;
import android.view.View;

import com.jackie.ts8209a.AppModule.APP;
import com.jackie.ts8209a.AppModule.Tools.Cmd;
import com.jackie.ts8209a.CustomView.Dialog.CustomDialog;
import com.jackie.ts8209a.CustomView.Dialog.WifiPassword;
import com.jackie.ts8209a.CustomView.View.WifiSelection;
import com.jackie.ts8209a.R;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Timer;
import java.util.TimerTask;

/**
 * Created by kuangyt on 2018/8/23.
 */

public class WifiManager extends NetDevManager{

    private static final String TAG = "WifiManager";

    public static final String DEV_NAME = "wlan0";

    public static final int NONE =1;
    public static final int WEP = 2;
    public static final int WPA = 3;
    public static final int EAP = 4;
    public static final int WPA2 = 5;
    public static final int WPA_WPA2 = 6;
    public static final int UNKNOW = 9;

    //对象单例
    private static WifiManager wifiManager = new WifiManager();
    //Wifi信息
//    private static NetDevInfo netDevInfo;

    //设备未连接wifi时自动连接定时器
    private Timer autoConnect;

    /*  系统WIFI管理相关对象  */
    //系统WifiManager
    private android.net.wifi.WifiManager osWifiManager = null;
    private android.net.wifi.WifiInfo osWifiInfo;

    // 扫描出的网络连接列表
    private List<ScanResult> wifiList;
    // 网络连接列表
    private List<WifiConfiguration> wifiConfiguration;
    // 定义一个WifiLock
    private android.net.wifi.WifiManager.WifiLock wifiLock;


    //Wifi设置相关变量
    private static CustomDialog selector = null;    //WIFI选择会话框对象
    private static WifiPassword password = null;    //密码输入会话框对象
    private static String selectedSsid = "";        //已选择WIFI的名称
    private static String selectedPwd = "";         //已选择WIFI的密码
    private static int selectedType = 0;            //已选择WIFI的类型

    private WifiManager(){
    }

    protected static WifiManager getWifiManager(){
        return wifiManager;
    }

    //初始化函数
    @Override
    protected void init(APP App) {
        super.init(App);
        Log.d(TAG,"Wifi Initialization");
        netDevInfo = new NetDevInfo();
        netDevInfo.name = DEV_NAME;
        netDevInfo.type = TYPE_WIRELESS;
        osWifiManager = (android.net.wifi.WifiManager) App.getApplicationContext().getSystemService(Context.WIFI_SERVICE);
        osWifiInfo = osWifiManager.getConnectionInfo();

        startGetState();
    }

    //系统广播接收过滤初始化
    @Override
    protected void setIntentFilter() {
        super.setIntentFilter();
        intentFilter.addAction(android.net.wifi.WifiManager.WIFI_STATE_CHANGED_ACTION);
    }

    //复写设备名称
    @Override
    protected String devName() {
        return DEV_NAME;
    }

    //获取当前wifi信息
//    public WifiInfo getWifiInfo(){
//        return netDevInfo;
//    }

    // 启动WIFI
    @Override
    protected void startup() {
        if (!osWifiManager.isWifiEnabled()) {
            osWifiManager.setWifiEnabled(true);
        }
    }

    @Override
    protected void setDhcpEn() {
        super.setDhcpEn();
        String cmd = String.format(Locale.ENGLISH,"netcfg %s %s",DEV_NAME,"dhcp");
        Cmd.execCmd(cmd);
    }

    // 关闭WIFI
    @Override
    protected void shutdown() {
        if (osWifiManager.isWifiEnabled()) {
            osWifiManager.setWifiEnabled(false);
        }
    }

    @Override
    public NetDevInfo getDevInfo() {
        return netDevInfo;
    }

    // 添加一个网络并连接
    protected boolean connect(String ssid, String pwd, int type) {
        WifiConfiguration wcg;
        boolean res;
        int wcgID;

         wcg = isExits(selectedSsid);

        if(wcg != null)
            wcgID = wcg.networkId;
        else {
            wcg = createWifiInfo(ssid,pwd,type);
            wcgID = osWifiManager.addNetwork(wcg);
        }

        res = osWifiManager.enableNetwork(wcgID, true);

        return res;
    }

    // 断开当前网络
    protected void disconnect() {
        if (!osWifiManager.isWifiEnabled()) {
            osWifiManager.disconnect();
        }
    }

    // 断开指定ID的网络
    protected void removeWifi(String ssid) {
        WifiConfiguration wcg = isExits(ssid);
        if(wcg != null) {
            osWifiManager.disableNetwork(wcg.networkId);
            osWifiManager.disconnect();
            osWifiManager.removeNetwork(wcg.networkId);
            Log.d(TAG,"disconnect wifi \""+ssid+"\"");
        }
    }

    // 得到配置好的网络
    protected List<WifiConfiguration> getConfiguration() {
        return wifiConfiguration;
    }

    // 获取wifi列表
    protected List<ScanResult> getWifiList() {
        return wifiList;
    }

    @Override
    protected void broadcastProcessor(Context context, Intent intent) {
        super.broadcastProcessor(context, intent);

        String action = intent.getAction();
//            Log.d(TAG,action);
        if (android.net.wifi.WifiManager.WIFI_STATE_CHANGED_ACTION.equals(action)) {// 这个监听wifi的打开与关闭，与wifi的连接无关
            int wifiState = intent.getIntExtra(android.net.wifi.WifiManager.EXTRA_WIFI_STATE, 0);
            switch (wifiState) {
                case android.net.wifi.WifiManager.WIFI_STATE_DISABLED:
                case android.net.wifi.WifiManager.WIFI_STATE_DISABLING:
//                    Log.d(TAG,DEV_NAME+" wifi disable");
                    netDevInfo.devEn = false;
                    stopGetState();
                    stopAutoConnect();
                    break;
                case android.net.wifi.WifiManager.WIFI_STATE_ENABLED:
//                    Log.d(TAG,DEV_NAME+" wifi enable");
                    netDevInfo.devEn = true;
                    break;
                case android.net.wifi.WifiManager.WIFI_STATE_ENABLING:
//                        Log.d(TAG,"WIFI_STATE_ENABLING");
                    break;
            }
        }else if (android.net.wifi.WifiManager.NETWORK_STATE_CHANGED_ACTION.equals(action)) {   //监听网络连接
            Parcelable parcelableExtra = intent.getParcelableExtra(android.net.wifi.WifiManager.EXTRA_NETWORK_INFO);
            if (null != parcelableExtra) {
                NetworkInfo networkInfo = (NetworkInfo) parcelableExtra;
                NetworkInfo.State state = networkInfo.getState();
                if (state == NetworkInfo.State.CONNECTED) {
//                    Log.d(TAG,DEV_NAME+" network connected");
                    osWifiManager.saveConfiguration();
                    netDevInfo.netEn = true;
//                    startGetState();//启动wifi信息更新定时器   `
                    stopAutoConnect(); //关闭wifi自动连接定时器
                } else if(state == NetworkInfo.State.DISCONNECTED) {
//                    Log.d(TAG,DEV_NAME+" network disconnected");
                    netDevInfo.netEn = false;
                    netDevInfo.reset();
//                    stopGetState();
                    startAutoConnect();
//                    updateWifiCfg();
                }
            }
        }
    }

    // 检查当前WIFI状态
    private int getWifiState() {
        return osWifiManager.getWifiState();
    }

    // 锁定WifiLock
    private void acquireWifiLock() {
        wifiLock = osWifiManager.createWifiLock(DEV_NAME);
//        wifiLock.acquire();
    }

    // 解锁WifiLock
    private void releaseWifiLock() {
        // 判断时候锁定
        if (wifiLock.isHeld()) {
            wifiLock.acquire();
        }
    }

    //扫描wifi列表
    private void scan() {
        osWifiManager.startScan();
        //得到扫描结果
        List<ScanResult> results = osWifiManager.getScanResults();
        // 得到配置好的网络连接
        wifiConfiguration = osWifiManager.getConfiguredNetworks();

        if (results != null) {
            wifiList = new ArrayList();
            for (ScanResult result : results) {
                if (result.SSID == null || result.SSID.length() == 0 || result.capabilities.contains("[IBSS]")) {
                    continue;
                }
                boolean found = false;
                for (ScanResult item : wifiList) {
                    if (item.SSID.equals(result.SSID) && item.capabilities.equals(result.capabilities)) {
                        found = true;
                        break;
                    }
                }
                if (!found) {
                    wifiList.add(result);
                }
            }
        }
    }

    // 指定配置好的网络进行连接
    private void connectConfiguration(int index) {
        // 索引大于配置好的网络索引返回
        if (index >= wifiConfiguration.size()) {
            return;
        }
        // 连接配置好的指定ID的网络
        osWifiManager.enableNetwork(wifiConfiguration.get(index).networkId,true);
    }

    //启动定时获取wifi状态
    private void startGetState(){
        if(getState == null)
            getState = new Timer();
        getState.schedule(new TimerTask() {
            @Override
            public void run() {
                getNetDevSta();
                osWifiInfo = osWifiManager.getConnectionInfo();
                updateWifiCfg();
//                Log.d(TAG,ssid+" "+mac+" "+rssi);
            }
        }, 5000, getNetDevStaTime);
    }

    private void updateWifiCfg(){
        String ssid,mac;
        int rssi;
        ssid = osWifiInfo.getSSID().equals("0x") ? "" : osWifiInfo.getSSID();
        rssi = osWifiInfo.getRssi();
        mac = osWifiInfo.getMacAddress();
        if((infoListener != null) && (!ssid.equals(netDevInfo.ssid) || !mac.equals(netDevInfo.mac) || rssi != netDevInfo.rssi)) {
            netDevInfo.ssid = ssid;
            netDevInfo.rssi = rssi;
            netDevInfo.mac = mac;
            infoListener.infoUpdate(DEV_NAME);
        }
    }

    //停止定时获取wifi状态
    private void stopGetState(){
        if(getState != null){
            getState.cancel();
            getState = null;
        }
    }

    //启动自动WIFI连接
    private void startAutoConnect(){
        scan();
        if(autoConnect == null)
            autoConnect = new Timer();
        autoConnect.schedule(new TimerTask() {
            int index = 0;

            @Override
            public void run() {
                connectConfiguration(index++);
                if(index > wifiConfiguration.size())
                    index = 0;
            }
        }, 5000, 10 * 1000);
    }

    //关闭自动WIFI连接
    private void stopAutoConnect(){
        if(autoConnect != null){
            autoConnect.cancel();
            autoConnect = null;
        }
    }

    @Override
    protected void action(Message msg) {
        switch (msg.what){
            case ACTION_WIFI_SELECT:
                creatWifiSelector((Context) msg.obj);
                break;
            case ACTION_REMOVE_WIFI:
                break;
            case ACTION_REMOVE_ALL_WIFI:
                removeAllWifiCfg();
                break;
        }
    }

    //Wifi接入设置->Wifi选择列表
    private void creatWifiSelector(final Context context){
        scan();
        CustomDialog.Builder builder = new CustomDialog.Builder(context,CustomDialog.VERTICAL_LIST);
        ScanResult[] wifiListArr = wifiList.toArray(new ScanResult[wifiList.size()]);
        View.OnClickListener listener = new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                WifiSelection selection = (WifiSelection) view;
                selectedSsid = selection.getSsid();
                selectedType = selection.getEncryptType();

                if(selectedType == NONE || isExits(selectedSsid) != null) {
                    wifiManager.connect(selectedSsid, "", selectedType);
                    selector.dismiss();
                }else
                    creatWifiPassword(context,selection.getSsid(),selection.getRssi());
            }
        };
        for(int i=0;i<wifiListArr.length;i++){
            WifiSelection selection = new WifiSelection(context);
            selection.setRssi(wifiListArr[i].level).setSsid(wifiListArr[i].SSID).setEncryptType(getEncryptionMethod(wifiListArr[i].capabilities));
            selection.setOnClickListener(listener);
            builder.addListView(selection);
        }
        selector = builder.setTitle(true,"WIFI列表").creatDialog();
        selector.show();
    }

    //Wifi接入设置->Wifi密码输入
    private void creatWifiPassword(Context context,String ssid,int rssi){
        WifiPassword.Builder builder = new WifiPassword.Builder(context);
        password = builder.setRssi(rssi).setSsid(ssid).creatWifiPassword();
        password.setOnPasswordResultListener(new WifiPassword.OnPasswordResultListener() {
            @Override
            public void passwordResult(int result, String password) {
                if(result == WifiPassword.CONNECT){
                    selectedPwd = password;
                    wifiManager.connect(selectedSsid, selectedPwd, selectedType);
                }else if(result == WifiPassword.CANCEL){
                    selectedSsid = "";
                    selectedPwd = "";
                }
                selector.dismiss();
                WifiManager.password.dismiss();
            }
        });
        password.show();
    }

    //通过获取到的字段判断加密类型
    private int getEncryptionMethod(String field){
        int result;
        if(field.contains("WPA-PSK") || field.contains("WPA2-PSK")){
            result = WPA;
            if(field.contains("WPA-PSK") && field.contains("WPA2-PSK"))
                result = WPA_WPA2;
            else if(!field.contains("WPA-PSK") || field.contains("WPA2-PSK"))
                result = WPA2;
            else if(field.contains("WPA-PSK") || !field.contains("WPA2-PSK"))
                result = WPA;
        }
        else if(field.contains("WEP"))
            result = WEP;
        else if(field.contains("EAP"))
            result = EAP;
        else if(field.equals("[ESS]"))
            result = NONE;
        else
            result = UNKNOW;

        return result;
    }

    private WifiConfiguration createWifiInfo(String SSID, String Password,int Type) {
        WifiConfiguration config = new WifiConfiguration();
        config.allowedAuthAlgorithms.clear();
        config.allowedGroupCiphers.clear();
        config.allowedKeyManagement.clear();
        config.allowedPairwiseCiphers.clear();
        config.allowedProtocols.clear();

        config.SSID = "\"" + SSID + "\"";

        WifiConfiguration tempConfig = this.isExits(SSID);
        if (tempConfig != null) {
            osWifiManager.removeNetwork(tempConfig.networkId);
        }

        if (Type == NONE) // WIFICIPHER_NOPASS
        {
            config.allowedKeyManagement.set(WifiConfiguration.KeyMgmt.NONE);
        }else if (Type == WEP) // WIFICIPHER_WEP
        {
            config.hiddenSSID = true;
            config.wepKeys[0] = "\"" + Password + "\"";
            config.allowedAuthAlgorithms.set(WifiConfiguration.AuthAlgorithm.SHARED);
            config.allowedGroupCiphers.set(WifiConfiguration.GroupCipher.CCMP);
            config.allowedGroupCiphers.set(WifiConfiguration.GroupCipher.TKIP);
            config.allowedGroupCiphers.set(WifiConfiguration.GroupCipher.WEP40);
            config.allowedGroupCiphers.set(WifiConfiguration.GroupCipher.WEP104);
            config.allowedKeyManagement.set(WifiConfiguration.KeyMgmt.NONE);
            config.wepTxKeyIndex = 0;
        }else if (Type == WPA||Type == WPA2||Type == WPA_WPA2) // WIFICIPHER_WPA
        {
            config.preSharedKey = "\"" + Password + "\"";
            config.hiddenSSID = true;
            config.allowedAuthAlgorithms.set(WifiConfiguration.AuthAlgorithm.OPEN);
            config.allowedGroupCiphers.set(WifiConfiguration.GroupCipher.TKIP);
            config.allowedKeyManagement.set(WifiConfiguration.KeyMgmt.WPA_PSK);
            config.allowedPairwiseCiphers.set(WifiConfiguration.PairwiseCipher.TKIP);
            config.allowedGroupCiphers.set(WifiConfiguration.GroupCipher.CCMP);
            config.allowedPairwiseCiphers.set(WifiConfiguration.PairwiseCipher.CCMP);
            config.status = WifiConfiguration.Status.ENABLED;
        }
        return config;
    }

    private WifiConfiguration isExits(String SSID) {
        List<WifiConfiguration> existingConfigs = osWifiManager.getConfiguredNetworks();
        for (WifiConfiguration existingConfig : existingConfigs) {
            if (existingConfig.SSID.equals("\"" + SSID + "\"")) {
                return existingConfig;
            }
        }
        return null;
    }

    private void removeAllWifiCfg(){
        List<WifiConfiguration> existingConfigs = osWifiManager.getConfiguredNetworks();
        for (WifiConfiguration existingConfig : existingConfigs) {
            removeWifi(existingConfig.SSID.replace("\"",""));
            Log.d(TAG,"remove wifi "+existingConfig.SSID);
        }
    }


}
