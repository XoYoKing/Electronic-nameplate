package com.jackie.ts8209a.AppModule.Network;

import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.os.Looper;
import android.os.Message;
import android.os.Messenger;
import android.os.RemoteException;
import android.util.Log;

import com.jackie.ts8209a.Activity.AppActivity;
import com.jackie.ts8209a.Activity.MainActivity;
import com.jackie.ts8209a.Activity.MeetingInfoActivity;
import com.jackie.ts8209a.AppModule.APP;
import com.jackie.ts8209a.AppModule.Tools.Cmd;
import com.jackie.ts8209a.AppModule.Basics.BatteryManager;
import com.jackie.ts8209a.AppModule.Basics.UserInfoManager;
import com.jackie.ts8209a.RemoteServer.Network;

import org.json.JSONException;
import org.json.JSONObject;

import java.util.Deque;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Timer;
import java.util.TimerTask;
import java.util.concurrent.LinkedBlockingDeque;

import static android.content.Context.BIND_AUTO_CREATE;
import static com.jackie.ts8209a.Activity.AppActivity.SERVIE_ACK;
import static com.jackie.ts8209a.Activity.AppActivity.SMS_MSG;
import static com.jackie.ts8209a.Activity.AppActivity.SYS_SMS_MSG;
import static com.jackie.ts8209a.Activity.AppActivity.USER_LIST;
import static com.jackie.ts8209a.Activity.MeetingInfoActivity.MEETING_INFO;
import static com.jackie.ts8209a.AppModule.Tools.Cmd.KEY_RES;
import static com.jackie.ts8209a.RemoteServer.Network.EVT_TS_CENTERCONTROL;
import static com.jackie.ts8209a.RemoteServer.Network.EVT_TS_MEETINGINFO;
import static com.jackie.ts8209a.RemoteServer.Network.EVT_TS_REQSERVICE_ACK;
import static com.jackie.ts8209a.RemoteServer.Network.EVT_TS_SENDMSG;
import static com.jackie.ts8209a.RemoteServer.Network.RSP_TS_GET_USERINFO;
import static com.jackie.ts8209a.RemoteServer.Network.RSP_TS_GET_USERLIST;

/**
 * Created by kuangyt on 2018/9/12.
 */

public class NetworkManager implements NetDevManager.NetDevInfoUpdatedListener{
    private static final String TAG = "NetworkManager";

    public static final String DEV_NAME = "DEV_NAME";
    public static final String DEV_EN = "DEV_EN";
    public static final String NETWORK_EN = "NETWORK_EN";
    public static final String DHCP_EN = "DHCP_EN";
    public static final String IPADDR = "IPADDR";
    public static final String NETMASK = "NETMASK";
    public static final String GATEWAY = "GATEWAY";
    public static final String MAC = "MAC";
    public static final String SSID = "SSID";
    public static final String RSSI = "RSSI";


    private NetDevManager netDevManager;
    private NetDevManager.NetDevInfo netDevInfo;
    private UserInfoManager userInfoManager;
    private BatteryManager batteryManager;
    private static APP App;

    private static NetworkManager networkManager = new NetworkManager();
    private static Messenger networkMessenger;
    private static Deque<Bundle> sendQueue = new LinkedBlockingDeque<Bundle>();

    private Timer sendDevInfoTimer;
    private OnNetworkStatusListener staListener;
    private int networkStatus = Network.STA_DISCONNECTED;

    private NetworkManager() {
    }

    public void init(APP app){
        App = app;

        batteryManager = BatteryManager.getBatteryManager();
        userInfoManager = UserInfoManager.getUserInfoManager();

        Cmd.execCmd("netcfg",new Handler(){
            @Override
            public void handleMessage(Message msg) {
                Bundle bundle = (Bundle)msg.obj;
                String res = bundle.getString(KEY_RES);
                if(res.contains(EthernetManager.DEV_NAME)){
                    netDevManager = EthernetManager.getEthernetManager();
//                    Log.d(TAG,"Ethernet init");
                }else{
                    netDevManager = WifiManager.getWifiManager();
//                    Log.d(TAG,"WIFI init");
                }
                netDevManager.init(App);
                netDevInfo = netDevManager.getDevInfo();
                netDevManager.setNetDevInfoUpdatedListener(NetworkManager.this);

                if(!userInfoManager.getDhcp()){
                    netDevManager.setNetDevInfo(userInfoManager.getLocalIp(),userInfoManager.getMask(),userInfoManager.getGateway());
                }else{
                    netDevManager.setDhcpEn();
                }
            }
        });

        App.startService(new Intent(App, Network.class));
        App.bindService(new Intent(App, Network.class), new ServiceConnection() {
            @Override
            public void onServiceConnected(ComponentName name, IBinder service) {
                Log.d(TAG,"set Network Messenger");
                setNetworkMessenger(new Messenger(service));
            }

            @Override
            public void onServiceDisconnected(ComponentName name) {

            }
        }, BIND_AUTO_CREATE);


//        wifiInfo = wifiManager.getWifiInfo();
//        ethInfo = ethernetManager.getEthInfo();
    }

    public static NetworkManager getNetworkManager(){
        return networkManager;
    }

    private void setNetworkMessenger(Messenger messenger) {
        try {

//            Log.d(TAG,"init");
            networkMessenger = messenger;
            Message msg = Message.obtain();
            msg.what = Network.CMD_SET_REPLY;
            msg.replyTo = networkManager.recMessenger;
            networkMessenger.send(msg);
            if (networkManager.sendDevInfoTimer == null) {
//                Log.d(TAG, "networkManager.sendDevInfoTimer == null");
                networkManager.sendDevInfoTimer = new Timer();
                networkManager.sendDevInfoTimer.schedule(new TimerTask() {
                    @Override
                    public void run() {
                        networkManager.sendDevInfo();
                    }
                }, 3000, 5000);
            }
            networkManager.resetNetwork();

            new Thread(getSendQueue).start();
        } catch (RemoteException e) {
            e.printStackTrace();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private Messenger recMessenger = new Messenger(new Handler() {
        @Override
        public void handleMessage(Message msg) {
            switch (msg.what) {
                case Network.CMD_NETWORK_STATUS:
                    networkStatus = msg.arg1;
                    if (staListener != null)
                        staListener.OnNetworkStatus(networkStatus);
                    break;
                case Network.CMD_RECEIVE_DATA:
                    Bundle bundle = msg.getData();
                    Message dataMsg = Message.obtain();
                    switch (bundle.getInt("iCmdEnum")) {
                        case EVT_TS_MEETINGINFO:
                            dataMsg.what = MEETING_INFO;
                            dataMsg.setData(msg.getData());
                            AppActivity.handler.sendMessage(dataMsg);
                            APP.LocalBroadcast.send(APP.ACTION_REFRESH_ACTIVITY, MeetingInfoActivity.class);
                            APP.LocalBroadcast.send(APP.ACTION_REFRESH_ACTIVITY, MainActivity.class);
                            break;
                        case RSP_TS_GET_USERINFO:
                            userInfoManager.setStr(UserInfoManager.USER, bundle.getString("strUserName"));
                            userInfoManager.setStr(UserInfoManager.POS, bundle.getString("strCompany"));
                            userInfoManager.setStr(UserInfoManager.COMP, bundle.getString("strPosition"));
                            break;
                        case RSP_TS_GET_USERLIST:
                            try {
                                JSONObject json = new JSONObject(bundle.getString("lstDevice"));
                                HashMap<Integer, String> map = new HashMap<Integer, String>();
                                Iterator keys = json.keys();
                                for (int i = 0; i < json.length(); i++) {
                                    String keyStr = (String) keys.next();
                                    map.put(Integer.valueOf(keyStr), json.getString(keyStr));
                                }
                                dataMsg.what = USER_LIST;
                                dataMsg.obj = map;
                                AppActivity.handler.sendMessage(dataMsg);
                            } catch (JSONException e) {
                                e.printStackTrace();
                            }
                            break;
                        case EVT_TS_REQSERVICE_ACK:
                            dataMsg.what = SERVIE_ACK;
                            dataMsg.obj = bundle.getString("strContent");
                            AppActivity.handler.sendMessage(dataMsg);
                            break;
                        case EVT_TS_CENTERCONTROL:
                            if (bundle.getInt("iDeviceID") != userInfoManager.getDeviceID())
                                break;
                            Log.d(TAG, "ControyType : " + bundle.getInt("iControlType"));
                            break;
                        case EVT_TS_SENDMSG:
                            dataMsg.setData(bundle);
                            dataMsg.what = bundle.getInt("iReceiverID") == 0 ? SYS_SMS_MSG : SMS_MSG;
//                            Log.d(TAG,"what: "+dataMsg.what+"  content:"+dataMsg.getData().getString("strContent"));
                            AppActivity.handler.sendMessage(dataMsg);
                            break;
                    }
                    break;
            }
        }
    });

    private void sendToNetwork(int what,Bundle bundle){
        bundle.putInt("what",what);
        sendQueue.add(bundle);
    }

    private static Runnable getSendQueue = new Runnable() {
        @Override
        public void run() {
            synchronized ("getSendQueue") {
                while (true) {
                        try {
                            if(sendQueue.size() > 0) {
                                Bundle bundle = sendQueue.poll();
                                Message msg = Message.obtain();
                                msg.what = bundle.getInt("what");
                                msg.setData(bundle);
                                networkMessenger.send(msg);
                            }
                            Thread.sleep(300);
                        } catch (RemoteException e) {
                            e.printStackTrace();
                        } catch (InterruptedException e) {
                            e.printStackTrace();
                        }

                }
            }
        }
    };

    public NetDevManager.NetDevInfo getNetDevInfo(){
        return netDevInfo;
    }

    public void setNetworkInfo(int[] ip,int[] mask,int[] gw){
        netDevManager.setNetDevInfo(ip,mask,gw);
    }

    public void creatWifiSelection(Context context){
        Message msg = Message.obtain();
        msg.what = NetDevManager.ACTION_WIFI_SELECT;
        msg.obj = context;
        netDevManager.action(msg);
    }

    public void removeAllWifi(){
        Message msg = Message.obtain();
        msg.what = NetDevManager.ACTION_REMOVE_ALL_WIFI;
        netDevManager.action(msg);
    }

    public void setDhcpEn(){
        netDevManager.setDhcpEn();
    }

    public void sendDevInfo() {
        Bundle bundle = new Bundle();
        bundle.putInt(Network.DEV_ID, userInfoManager.getDeviceID());
        bundle.putInt(Network.DEV_BRIGHT, userInfoManager.getBrightness());
        bundle.putInt(Network.DEV_BATLEV, batteryManager.getLevel());
        bundle.putInt(Network.DEV_RSSI, netDevInfo.rssi);
        bundle.putString(Network.DEV_MAC, netDevInfo.mac);
        bundle.putBoolean(Network.DEV_NETDEV_EN, netDevInfo.devEn);
        bundle.putBoolean(Network.DEV_NETWORK_EN,netDevInfo.netEn);

        sendToNetwork(Network.CMD_UPDATA_DEV_INFO, bundle);
    }

    public void resetNetwork() {
        Bundle bundle = new Bundle();
        bundle.putIntArray(Network.DEV_SERVIP, userInfoManager.getServIp());
        bundle.putInt(Network.DEV_SERVPO, userInfoManager.getServPort());

        sendToNetwork(Network.CMD_RESET_NETWORK, bundle);
}

    public void callService(String content){
        Bundle bundle = new Bundle();

        bundle.putInt("iCmdEnum",Network.EVT_TS_REQSERVICE);
        bundle.putString("strContent",content);
        bundle.putInt("iServiceID",0);
        sendToNetwork(Network.CMD_TRANSMIT_DATA, bundle);
    }

    public void sendSms(int id,String content){
        Bundle bundle = new Bundle();

        bundle.putInt("iCmdEnum", EVT_TS_SENDMSG);
        bundle.putString("strContent",content);
        bundle.putInt("iReceiverID",id);
        sendToNetwork(Network.CMD_TRANSMIT_DATA, bundle);
    }

    public int getNetworkStatus(){
        return networkStatus;
    }

    public void setOnNetworkStatusListener(OnNetworkStatusListener listener){
        staListener = listener;
    }

    @Override
    public void infoUpdate(String devName) {
        Bundle bundle = new Bundle();
//        Log.d(TAG,devName+" : "+netDevInfo.devEn);

        bundle.putString(DEV_NAME,devName);
        bundle.putBoolean(DEV_EN,netDevInfo.devEn);
        bundle.putBoolean(NETWORK_EN,netDevInfo.netEn);
        bundle.putBoolean(DHCP_EN,netDevInfo.dhcp);
        bundle.putIntArray(IPADDR,netDevInfo.ip);
        bundle.putIntArray(NETMASK,netDevInfo.mask);
        bundle.putIntArray(GATEWAY,netDevInfo.gw);
        bundle.putString(MAC,netDevInfo.mac);
        bundle.putString(SSID,netDevInfo.ssid);
        bundle.putInt(RSSI,netDevInfo.rssi);

        APP.LocalBroadcast.send(APP.ACTION_NETWORK_INFO_UPDATE,bundle);
    }

    public interface OnNetworkStatusListener{
        void OnNetworkStatus(int sta);
    }

}
