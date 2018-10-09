package com.jackie.ts8209a.Managers;

import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.os.Messenger;
import android.os.RemoteException;
import android.util.Log;

import com.jackie.ts8209a.Activity.AppActivity;
import com.jackie.ts8209a.Application.App;
import com.jackie.ts8209a.RemoteServer.Network;

import org.json.JSONException;
import org.json.JSONObject;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Timer;
import java.util.TimerTask;

import static com.jackie.ts8209a.Activity.AppActivity.SERVIE_ACK;
import static com.jackie.ts8209a.Activity.AppActivity.SMS_MSG;
import static com.jackie.ts8209a.Activity.AppActivity.SYS_SMS_MSG;
import static com.jackie.ts8209a.Activity.AppActivity.USER_LIST;
import static com.jackie.ts8209a.Activity.MeetingInfoActivity.MEETING_INFO;
import static com.jackie.ts8209a.RemoteServer.Network.EVT_TS_CENTERCONTROL;
import static com.jackie.ts8209a.RemoteServer.Network.EVT_TS_MEETINGINFO;
import static com.jackie.ts8209a.RemoteServer.Network.EVT_TS_REQSERVICE_ACK;
import static com.jackie.ts8209a.RemoteServer.Network.EVT_TS_SENDMSG;
import static com.jackie.ts8209a.RemoteServer.Network.RSP_TS_GET_USERINFO;
import static com.jackie.ts8209a.RemoteServer.Network.RSP_TS_GET_USERLIST;

/**
 * Created by kuangyt on 2018/9/12.
 */

public class NetworkManager {

    private static final String TAG = "NetworkManager";

    private BatteryManager batteryManager;
    private WifiManager wifiManager;
    private WifiManager.WifiInfo wifiInfo;
    private UserInfoManager userInfoManager;
    private App app;

    private static NetworkManager networkManager = new NetworkManager();
    private static Messenger networkMessenger;

    private Timer sendDevInfoTimer;

    private OnNetworkStatusListener staListener;

    private int networkStatus = Network.STA_DISCONNECTED;


    private NetworkManager() {
        try {
            batteryManager = BatteryManager.getBatteryManager();
            userInfoManager = UserInfoManager.getUserInfoManager();
            wifiManager = WifiManager.getWifiManager();
            wifiInfo = wifiManager.getWifiInfo();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public static NetworkManager initManager(Messenger messenger){
        try {

            Log.d(TAG,"initManager");
            networkMessenger = messenger;
            Message msg = Message.obtain();
            msg.what = Network.CMD_SET_REPLY;
            msg.replyTo = networkManager.recMessenger;
            networkMessenger.send(msg);
            if(networkManager.sendDevInfoTimer == null) {
                Log.d(TAG,"networkManager.sendDevInfoTimer == null");
                networkManager.sendDevInfoTimer = new Timer();
                networkManager.sendDevInfoTimer.schedule(new TimerTask() {
                    @Override
                    public void run() {
                        networkManager.sendDevInfo();
                    }
                }, 3000, 5000);
            }
            networkManager.resetNetwork();
        } catch (RemoteException e) {
            e.printStackTrace();
        } catch (Exception e) {
            e.printStackTrace();
        }
        return getNetworkManager();
    }

    public static NetworkManager getNetworkManager(){
        return networkManager;
    }

    private Messenger recMessenger = new Messenger(new Handler(){
        @Override
        public void handleMessage(Message msg) {
            switch(msg.what){
                case Network.CMD_NETWORK_STATUS:
                    networkStatus = msg.arg1;
                    if(staListener != null)
                        staListener.OnNetworkStatus(networkStatus);
                    break;
                case Network.CMD_RECEIVE_DATA:
                    Bundle bundle = msg.getData();
                    Message dataMsg = Message.obtain();
                    switch(bundle.getInt("iCmdEnum")){
                        case EVT_TS_MEETINGINFO:
                            dataMsg.what = MEETING_INFO;
                            dataMsg.setData(msg.getData());
                            AppActivity.handler.sendMessage(dataMsg);
                            break;
                        case RSP_TS_GET_USERINFO:
                            userInfoManager.setStr(UserInfoManager.USER,bundle.getString("strUserName"));
                            userInfoManager.setStr(UserInfoManager.POS,bundle.getString("strCompany"));
                            userInfoManager.setStr(UserInfoManager.COMP,bundle.getString("strPosition"));
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
                            if(bundle.getInt("iDeviceID") != userInfoManager.getDeviceID())
                                break;
                            Log.d(TAG,"ControyType : "+bundle.getInt("iControlType"));
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
        try {
            if (networkMessenger == null)
                return;
            Message msg = Message.obtain();
            msg.what = what;
            msg.setData(bundle);
            networkMessenger.send(msg);
        } catch (RemoteException e) {
            e.printStackTrace();
        }
    }

    public void sendDevInfo() {
        Bundle bundle = new Bundle();
        bundle.putInt(Network.DEV_RSSI, wifiInfo.RSSI());
        bundle.putString(Network.DEV_MAC, wifiInfo.MAC());
        bundle.putBoolean(Network.DEV_WIFIEN, wifiInfo.wifiEnable());
        bundle.putInt(Network.DEV_BATLEV, batteryManager.getLevel());
        bundle.putInt(Network.DEV_ID, userInfoManager.getDeviceID());
        bundle.putInt(Network.DEV_BRIGHT, userInfoManager.getBrightness());

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

    public interface OnNetworkStatusListener{
        void OnNetworkStatus(int sta);
    }

}
