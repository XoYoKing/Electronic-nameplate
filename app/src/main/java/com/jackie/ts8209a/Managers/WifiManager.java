package com.jackie.ts8209a.Managers;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.net.NetworkInfo;
import android.net.wifi.ScanResult;
import android.net.wifi.WifiConfiguration;
import android.os.Message;
import android.os.Parcelable;
import android.util.Log;
import android.view.View;

import com.jackie.ts8209a.Application.App;
import com.jackie.ts8209a.CustomView.Dialog.CustomDialog;
import com.jackie.ts8209a.CustomView.Dialog.WifiPassword;
import com.jackie.ts8209a.CustomView.View.WifiSelection;

import java.util.ArrayList;
import java.util.List;
import java.util.Timer;
import java.util.TimerTask;

/**
 * Created by kuangyt on 2018/8/23.
 */

public class WifiManager {

    private static final String TAG = "WifiManager";

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
    private static WifiInfo wifiInfo = new WifiInfo();
    //Wifi信息监听器
    private static OnWifiInfoListener wifiInfoListeners = null;
    //获取wifi信息定时器
    private static Timer wifiInfoUpdate = null;
    //设备未连接wifi时自动连接定时器
    private static Timer wifiAutoConnect = null;

    /*  系统WIFI管理相关对象  */
    //系统WifiManager
    private static android.net.wifi.WifiManager osWifiManager = null;
    private static android.net.wifi.WifiInfo osWifiInfo;

    // 扫描出的网络连接列表
    private static List<ScanResult> wifiList;
    // 网络连接列表
    private static List<WifiConfiguration> wifiConfiguration;
    // 定义一个WifiLock
    private android.net.wifi.WifiManager.WifiLock wifiLock;

    //Application环境
    private static App app = null;

    //Wifi设置相关变量
    private static CustomDialog wifiSelector = null;
    private static WifiPassword wifiPassword = null;
    private static String connectWifiSsid = "";
    private static String connectWifiPwd = "";
    private static int connectWifiType = 0;

    private WifiManager(){
    }

    public static WifiManager getWifiManager() throws Exception{
        if(app == null || osWifiManager == null){
            throw (new Exception("WifiManager not initialized,please invoke \"initWifiManager()\""));
        }
        return wifiManager;
    }

    //wifi管理器初始化
    public static WifiManager initWifiManager(App application) throws Exception {
        app = application;
        osWifiManager = (android.net.wifi.WifiManager) app.getApplicationContext().getSystemService(Context.WIFI_SERVICE);
        osWifiInfo = osWifiManager.getConnectionInfo();

        IntentFilter filter = new IntentFilter();
        filter.addAction(android.net.wifi.WifiManager.NETWORK_STATE_CHANGED_ACTION);
        filter.addAction(android.net.wifi.WifiManager.WIFI_STATE_CHANGED_ACTION);

        app.registerReceiver(new wifiStatusReceiver(),filter);
        return getWifiManager();
    }

    // 打开WIFI
    public void startup() {
        if (!osWifiManager.isWifiEnabled()) {
            osWifiManager.setWifiEnabled(true);
        }
    }

    // 关闭WIFI
    public void shutdown() {
        if (osWifiManager.isWifiEnabled()) {
            osWifiManager.setWifiEnabled(false);
        }
    }

    // 添加一个网络并连接
    public boolean connect(String ssid, String pwd, int type) {
        WifiConfiguration wcg = createWifiInfo(ssid,pwd,type);
        int wcgID = osWifiManager.addNetwork(wcg);
        boolean success = osWifiManager.enableNetwork(wcgID, true);
        if(success) {
            osWifiManager.saveConfiguration();
            acquireWifiLock();
        }
        return success;
    }

    // 断开当前网络
    public void disconnect() {
        if (!osWifiManager.isWifiEnabled()) {
            osWifiManager.disconnect();
        }
    }

    // 检查当前WIFI状态
    public int getStatus() {
        return osWifiManager.getWifiState();
    }

    // 锁定WifiLock
    public void acquireWifiLock() {
        wifiLock = osWifiManager.createWifiLock("WIFI");
        wifiLock.acquire();
    }

    // 解锁WifiLock
    public void releaseWifiLock() {
        // 判断时候锁定
        if (wifiLock.isHeld()) {
            wifiLock.acquire();
        }
    }

    //扫描wifi列表
    public static void scan() {
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

    // 得到配置好的网络
    public List<WifiConfiguration> getConfiguration() {
        return wifiConfiguration;
    }

    // 获取wifi列表
    public List<ScanResult> getWifiList() {
        return wifiList;
    }

    //获取当前wifi信息
    public WifiInfo getWifiInfo(){
        return wifiInfo;
    }

    // 指定配置好的网络进行连接
    public static void connectConfiguration(int index) {
        // 索引大于配置好的网络索引返回
        if (index > wifiConfiguration.size()) {
            return;
        }
        // 连接配置好的指定ID的网络
        osWifiManager.enableNetwork(wifiConfiguration.get(index).networkId,true);
    }

    // 断开指定ID的网络
    public void disconnectWifi(String ssid) {
        WifiConfiguration wcg = isExits(ssid);
        osWifiManager.disableNetwork(wcg.networkId);
        osWifiManager.disconnect();
    }

    //系统广播接收器
    public static class wifiStatusReceiver extends BroadcastReceiver {

        String TAG = "wifiStatusReceiver";

        @Override
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();
//            Log.d(TAG,action);
            if (android.net.wifi.WifiManager.WIFI_STATE_CHANGED_ACTION.equals(action)) {// 这个监听wifi的打开与关闭，与wifi的连接无关
                int wifiState = intent.getIntExtra(android.net.wifi.WifiManager.EXTRA_WIFI_STATE, 0);
                switch (wifiState) {
                    case android.net.wifi.WifiManager.WIFI_STATE_DISABLED:
                    case android.net.wifi.WifiManager.WIFI_STATE_DISABLING:
//                        Log.d(TAG,"WIFI_STATE_DISENABLED");
                        wifiInfo.wifiEn = false;
                        if(wifiInfoUpdate != null){
                            wifiInfoUpdate.cancel();
                            wifiInfoUpdate = null;
                        }
                        if(wifiAutoConnect != null){
                            wifiAutoConnect.cancel();
                            wifiAutoConnect = null;
                        }
                        break;
                    case android.net.wifi.WifiManager.WIFI_STATE_ENABLED:
//                        Log.d(TAG,"WIFI_STATE_ENABLED");
                        wifiInfo.wifiEn = true;
                        break;
                    case android.net.wifi.WifiManager.WIFI_STATE_ENABLING:
//                        Log.d(TAG,"WIFI_STATE_ENABLING");
                        break;
                }
            }

            if (android.net.wifi.WifiManager.NETWORK_STATE_CHANGED_ACTION.equals(action)) {
                Parcelable parcelableExtra = intent.getParcelableExtra(android.net.wifi.WifiManager.EXTRA_NETWORK_INFO);
                if (null != parcelableExtra) {
                    NetworkInfo networkInfo = (NetworkInfo) parcelableExtra;
                    NetworkInfo.State state = networkInfo.getState();
                    if (state == NetworkInfo.State.CONNECTED) {
                        wifiInfo.netEn = true;
                        //启动wifi信息更新定时器
                        if(wifiInfoUpdate == null)
                            wifiInfoUpdate = new Timer(true);
                        wifiInfoUpdate.schedule(new wifiInfoUpdateTask(),500,10*1000);
                        //关闭wifi自动连接定时器
                        if(wifiAutoConnect != null){
                            wifiAutoConnect.cancel();
                            wifiAutoConnect = null;
                        }
                    } else if(state == NetworkInfo.State.DISCONNECTED) {
//                        Log.d(TAG,"NETWORK_NOT_CONNECTED");
                        wifiInfo.netEn = false;
                        wifiInfo.ssid = "";
                        wifiInfo.rssi = -200;
                        wifiInfo.localMac = "";
                        for(int i=0;i<wifiInfo.ip.length;i++)
                            wifiInfo.ip[i] = 0x00;

                        if(wifiInfoUpdate != null){
                            wifiInfoUpdate.cancel();
                            wifiInfoUpdate = null;
                        }
                        if(wifiAutoConnect == null)
                            wifiAutoConnect = new Timer(true);
                        wifiAutoConnect.schedule(new wifiAutoConnectTask(),10*1000,10*1000);
                        handler.sendEmptyMessage(0);
                    }
                }
            }
            handler.sendEmptyMessage(0);
        }
    }

    //WIfi管理器handler
    public static android.os.Handler handler = new android.os.Handler(){
        @Override
        public void handleMessage(Message msg) {
            switch (msg.what) {
                case 0:
                    if (wifiInfoListeners != null)
                        wifiInfoListeners.OnWifiInfo(wifiInfo);
                    break;
                case 1:
                    creatWifiSelector((Context) msg.obj);
                    break;
            }
        }
    };

    //添加wifi信息监听接口函数
    public static void setOnWifiInfoListener(OnWifiInfoListener listener){
        wifiInfoListeners = listener;
    }

    //wifi信息监听接口
    public interface OnWifiInfoListener{
        void OnWifiInfo(WifiInfo wifiInfo);
    }

    //Wifi信息对象
    public static class WifiInfo{
        private boolean wifiEn = false;
        private boolean netEn = false;

        private String ssid = "";
        private int rssi = -200;
        private int[] ip = new int[4];
        private String localMac = "";

        public String SSID(){
            return ssid;
        }

        public int RSSI(){
            return rssi;
        }

        public int[] IP(){
            return ip;
        }

        public String MAC(){
            return localMac;
        }

        public boolean wifiEnable(){
            return wifiEn;
        }

        public boolean networkEnable(){
            return netEn;
        }
    }

    //Wifi接入设置->Wifi选择列表
    private static void creatWifiSelector(final Context context){
        scan();
        CustomDialog.Builder builder = new CustomDialog.Builder(context,CustomDialog.VERTICAL_LIST);
        ScanResult[] wifiListArr = wifiList.toArray(new ScanResult[wifiList.size()]);
        View.OnClickListener listener = new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                WifiSelection selection = (WifiSelection) view;
                connectWifiSsid = selection.getSsid();
                connectWifiType = selection.getEncryptType();
                if(connectWifiType == NONE) {
                    wifiManager.connect(connectWifiSsid, "", connectWifiType);
                    wifiSelector.dismiss();
                }
                else if(isExits(connectWifiSsid) != null){
                    osWifiManager.enableNetwork(isExits(connectWifiSsid).networkId,true);
                    wifiSelector.dismiss();
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
        wifiSelector = builder.setTitle(true,"WIFI列表").creatDialog();
        wifiSelector.show();
    }

    //Wifi接入设置->Wifi密码输入
    private static void creatWifiPassword(Context context,String ssid,int rssi){
        WifiPassword.Builder builder = new WifiPassword.Builder(context);
        wifiPassword = builder.setRssi(rssi).setSsid(ssid).creatWifiPassword();
        wifiPassword.setOnPasswordResultListener(new WifiPassword.OnPasswordResultListener() {
            @Override
            public void passwordResult(int result, String password) {
                if(result == WifiPassword.CONNECT){
                    connectWifiPwd = password;
                    wifiManager.connect(connectWifiSsid,connectWifiPwd,connectWifiType);
                }else if(result == WifiPassword.CANCEL){
                    connectWifiSsid = "";
                    connectWifiPwd = "";
                }
                wifiSelector.dismiss();
                wifiPassword.dismiss();
            }
        });
        wifiPassword.show();
    }

    //通过获取到的字段判断加密类型
    private static int getEncryptionMethod(String field){
        int result;
        if(field.indexOf("WPA-PSK") != -1|| field.indexOf("WPA2-PSK") != -1){
            result = WPA;
            if(field.indexOf("WPA-PSK") != -1&& field.indexOf("WPA2-PSK") != -1)
                result = WPA_WPA2;
            else if(field.indexOf("WPA-PSK") == -1|| field.indexOf("WPA2-PSK") != -1)
                result = WPA2;
            else if(field.indexOf("WPA-PSK") != -1|| field.indexOf("WPA2-PSK") == -1)
                result = WPA;
        }
        else if(field.indexOf("WEP") != -1)
            result = WEP;
        else if(field.indexOf("EAP") != -1)
            result = EAP;
        else if(field.equals("[ESS]"))
            result = NONE;
        else
            result = UNKNOW;

        return result;
    }

    //定时器任务：获取wifi信息
    private static class wifiInfoUpdateTask extends TimerTask{
        @Override
        public void run() {
//                Log.d(TAG,"updata wifiInfo!");
            osWifiInfo = osWifiManager.getConnectionInfo();
            wifiInfo.ssid = osWifiInfo.getSSID();
            wifiInfo.rssi = osWifiInfo.getRssi();
            wifiInfo.ip = ipIntToArr(osWifiInfo.getIpAddress());
            wifiInfo.localMac = osWifiInfo.getMacAddress();

            handler.sendEmptyMessage(0);
        }
    }

    private static class wifiAutoConnectTask extends  TimerTask{
        private int index = 0;
        @Override
        public void run() {
            Log.d(TAG,"wifiAutoConnectTask");
            connectConfiguration(index++);
            if(index > wifiConfiguration.size())
                index = 0;
        }
    }

    // 将int类型的IP转换成字符串形式的IP
    private static int[] ipIntToArr(int ip) {
        int[] arr = new int[4];
        arr[0] = (int) (0xff & ip);
        arr[1] = (int) ((0xff00 & ip) >> 8);
        arr[2] = (int) ((0xff0000 & ip) >> 16);
        arr[3] = (int) ((0xff000000 & ip) >> 24);
        return arr;
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
        }
        if (Type == WEP) // WIFICIPHER_WEP
        {
            config.hiddenSSID = true;
            config.wepKeys[0] = "\"" + Password + "\"";
            config.allowedAuthAlgorithms
                    .set(WifiConfiguration.AuthAlgorithm.SHARED);
            config.allowedGroupCiphers.set(WifiConfiguration.GroupCipher.CCMP);
            config.allowedGroupCiphers.set(WifiConfiguration.GroupCipher.TKIP);
            config.allowedGroupCiphers.set(WifiConfiguration.GroupCipher.WEP40);
            config.allowedGroupCiphers
                    .set(WifiConfiguration.GroupCipher.WEP104);
            config.allowedKeyManagement.set(WifiConfiguration.KeyMgmt.NONE);
            config.wepTxKeyIndex = 0;
        }
        if (Type == WPA||Type == WPA2||Type == WPA_WPA2) // WIFICIPHER_WPA
        {
            config.preSharedKey = "\"" + Password + "\"";
            config.hiddenSSID = true;
            config.allowedAuthAlgorithms.set(WifiConfiguration.AuthAlgorithm.OPEN);
            config.allowedGroupCiphers.set(WifiConfiguration.GroupCipher.TKIP);
            config.allowedKeyManagement.set(WifiConfiguration.KeyMgmt.WPA_PSK);
            config.allowedPairwiseCiphers.set(WifiConfiguration.PairwiseCipher.TKIP);
            config.allowedGroupCiphers.set(WifiConfiguration.GroupCipher.CCMP);
            config.allowedPairwiseCiphers.set(WifiConfiguration.PairwiseCipher.CCMP);

//            config.allowedProtocols.set(WifiConfiguration.Protocol.WPA);

            config.status = WifiConfiguration.Status.ENABLED;
        }
        return config;
    }

    private static WifiConfiguration isExits(String SSID) {
        List<WifiConfiguration> existingConfigs = osWifiManager.getConfiguredNetworks();
        for (WifiConfiguration existingConfig : existingConfigs) {
            if (existingConfig.SSID.equals("\"" + SSID + "\"")) {
                return existingConfig;
            }
        }
        return null;
    }

}
