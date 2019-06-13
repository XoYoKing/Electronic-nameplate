package com.itc.ts8209a.module.network;

import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.os.Message;
import android.os.Messenger;
import android.os.RemoteException;
import android.util.Log;

import com.itc.ts8209a.activity.AppActivity;
import com.itc.ts8209a.activity.EditUserInfoActivity;
import com.itc.ts8209a.activity.MainActivity;
import com.itc.ts8209a.activity.MeetingInfoActivity;
import com.itc.ts8209a.activity.ShowNameActivity;
import com.itc.ts8209a.app.MyApplication;
import com.itc.ts8209a.module.database.DatabaseManager;
import com.itc.ts8209a.module.nameplate.NameplateManager;
import com.itc.ts8209a.module.power.PowerManager;
import com.itc.ts8209a.widget.Cmd;
import com.itc.ts8209a.server.Network;
import com.itc.ts8209a.widget.Debug;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.File;
import java.util.Deque;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Timer;
import java.util.TimerTask;
import java.util.concurrent.LinkedBlockingDeque;

import static android.content.Context.BIND_AUTO_CREATE;
import static com.itc.ts8209a.activity.AppActivity.*;
import static com.itc.ts8209a.activity.MeetingInfoActivity.MEETING_INFO;
import static com.itc.ts8209a.app.AppConfig.*;
import static com.itc.ts8209a.app.MyApplication.*;
import static com.itc.ts8209a.widget.Cmd.KEY_RES;
import static com.itc.ts8209a.server.Network.*;
import static com.itc.ts8209a.widget.FilePath.listFileSortByModifyTime;

/**
 * Created by kuangyt on 2018/9/12.
 */

public class NetworkManager implements NetDevManager.NetDevInfoUpdatedListener {
    private static final String TAG = "NetworkManager";

    public static final String NET_DRIVE_NAME = "NET_DRIVE_NAME";
    public static final String NET_DRIVE_EN = "NET_DRIVE_EN";
    public static final String NETWORK_EN = "NETWORK_EN";
    public static final String NETWORK_DHCP_EN = "NETWORK_DHCP_EN";
    public static final String NETWORK_LOCAL_IP = "NETWORK_LOCAL_IP";
    public static final String NETWORK_MASK = "NETWORK_MASK";
    public static final String NETWORK_GATEWAY = "NETWORK_GATEWAY";
    public static final String NETWORK_MAC = "NETWORK_MAC";
    public static final String NETWORK_SERV_IP = "NETWORK_SERV_IP";
    public static final String NETWORK_SERV_PORT = "NETWORK_SERV_PORT";
    public static final String WIFI_SSID = "WIFI_SSID";
    public static final String WIFI_RSSI = "WIFI_RSSI";
    public static final String DEV_ID = "DEV_ID";
    public static final String DEV_BAT_LEVEL = "DEV_BAT_LEVEL";
    public static final String DEV_BRIGHTNESS = "DEV_BRIGHTNESS";
    public static final String DOWNLOAD_URL = "DOWNLOAD_URL";
    public static final String DOWNLOAD_PATH = "DOWNLOAD_PATH";
    public static final String DOWNLOAD_TYPE = "DOWNLOAD_TYPE";


    private NetDevManager netDevManager;
    private NetDevManager.NetDevInfo netDevInfo;
    private DatabaseManager databaseManager;
    private PowerManager powerManager;
    private MyApplication app;

    private static NetworkManager networkManager = new NetworkManager();
    private static Messenger networkMessenger;
    private static Deque<Bundle> sendQueue = new LinkedBlockingDeque<Bundle>();

    private Timer sendDevInfoTimer;
    private OnNetworkStatusListener staListener;
    private int networkStatus = Network.STA_DISCONNECTED;
    private int errCount = 0;

    private NetworkManager() {
    }

    public void init(final MyApplication app) {
        this.app = app;

        powerManager = PowerManager.getPowerManager();
        databaseManager = DatabaseManager.getDatabaseManager();

        Cmd.execCmd("netcfg", new Handler() {
            @Override
            public void handleMessage(Message msg) {
                Bundle bundle = (Bundle) msg.obj;
                String res = bundle.getString(KEY_RES);
                if (res.contains(EthernetManager.DEV_NAME)) {
                    netDevManager = EthernetManager.getEthernetManager();
                } else {
                    netDevManager = WifiManager.getWifiManager();
                }
                netDevManager.init(app);
                netDevInfo = netDevManager.getDevInfo();
                netDevManager.setNetDevInfoUpdatedListener(NetworkManager.this);

                if (!databaseManager.getDhcp()) {
                    netDevManager.setNetDevInfo(databaseManager.getLocalIp(), databaseManager.getMask(), databaseManager.getGateway());
                } else {
                    netDevManager.setDhcpEn();
                }
            }
        });
        app.startService(new Intent(app, Network.class));
        app.bindService(new Intent(app, Network.class), new ServiceConnection() {
            @Override
            public void onServiceConnected(ComponentName name, IBinder service) {
                setNetworkMessenger(new Messenger(service));
            }

            @Override
            public void onServiceDisconnected(ComponentName name) {
            }
        }, BIND_AUTO_CREATE);
    }

    public static NetworkManager getNetworkManager() {
        return networkManager;
    }

    private void setNetworkMessenger(Messenger messenger) {
        try {
            networkMessenger = messenger;
            Message msg = Message.obtain();
            msg.what = Network.CMD_SET_REPLY;
            msg.replyTo = recMessenger;
            messenger.send(msg);
            if (sendDevInfoTimer == null) {
                sendDevInfoTimer = new Timer();
                sendDevInfoTimer.schedule(new TimerTask() {
                    @Override
                    public void run() {
                        sendDevInfo();
                    }
                }, 3000, 5000);
            }
            resetNetwork();
            new Thread(getSendQueue).start();
        } catch (RemoteException e) {
            e.printStackTrace();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private Handler recHandler = new Handler(){
        @Override
        public void handleMessage(Message msg) {
            Bundle bundle;

            switch (msg.what) {
                case Network.CMD_ID_REPEAT:
                    PromptBox.BuildPrompt("DEVICE_ID_REPEAT").Text("设备ID重复").Time(1).TimeOut(10000);
                    break;
                case Network.CMD_HTTP_DOWNLOAD_RES:
                    bundle = msg.getData();
                    boolean res = bundle.getBoolean("httpDownLoadRes");

                    if(res){
                        if(bundle.getString("type").equals("NP")) {
                            PromptBox.BuildPrompt("IMAGE_NAMEPLATE_UPTATA").Text("已更新图片铭牌").Time(1).TimeOut(3000);
                            final String filePath = bundle.getString("filePath");
                            (new Timer()).schedule(new TimerTask() {
                                @Override
                                public void run() {
                                    databaseManager.setNamePlateType(NameplateManager.TYPE_RDY_MADE_PIC)
                                            .setNamePlateImgPath(filePath)
                                            .save();
                                    MyApplication.LocalBroadcast.send(ACTION_NAMEPLATE_UPDATE, filePath);
                                    MyApplication.LocalBroadcast.send(ACTION_REFRESH_ACTIVITY, EditUserInfoActivity.class);
                                }
                            }, 500);

                            List<File> list = listFileSortByModifyTime(NAMEPLATE_IMG_PATH+"id_"+ databaseManager.getDeviceID()+"/");
                            if(list.size() > NAMEPLATE_FILE_NUM_MAX){
                                int delNum = list.size() - NAMEPLATE_FILE_NUM_MAX;
                                for(int i = 0;i < delNum;i++){
                                    Log.d(TAG,"delete nameplate image : "+list.get(i).getName());
                                    list.get(i).delete();
                                }
                            }
                        }

                        else if(bundle.getString("type").equals("BG")){
                            List<File> list = listFileSortByModifyTime(NAMEPLATE_BACKGROUND_PATH+"id_"+ databaseManager.getDeviceID()+"/");
                            if(list.size() > NAMEPLATE_FILE_NUM_MAX){
                                int delNum = list.size() - NAMEPLATE_FILE_NUM_MAX;
                                for(int i = 0;i < delNum;i++){
                                    Log.d(TAG,"delete nameplate background : "+list.get(i).getName());
                                    list.get(i).delete();
                                }
                            }
                        }
                    }
                    break;
                case Network.CMD_NETWORK_STATUS:
                    networkStatus = msg.arg1;
                    if (staListener != null)
                        staListener.OnNetworkStatus(networkStatus);
                    break;
                case Network.CMD_RECEIVE_DATA:
                    bundle = msg.getData();
                    Message appMsg = Message.obtain();
                    int iCmdEnum = bundle.getInt("iCmdEnum");
//                    Debug.d(TAG,"iCmdEnum:"+iCmdEnum);
                    switch (iCmdEnum) {
                        case RSP_TS_DEVICE_REG:

                            break;
                        case EVT_TS_MEETINGINFO:
                            Log.d(TAG,"EVT_TS_MEETINGINFO  "+bundle.getString("strName"));
                            databaseManager.setMeetName(bundle.getString("strName"))
                                    .setMeetSlogan(bundle.getString("strSlogan"))
                                    .setMeetContent(bundle.getString("strContent"))
                                    .setMeetStartTime(bundle.getString("strStartTime"))
                                    .setMeetEndTime(bundle.getString("strEndTime"))
                                    .save();

                            appMsg.what = MEETING_INFO;
                            AppActivity.handler.sendMessage(appMsg);
                            break;
                        case RSP_TS_GET_USERINFO:
                            databaseManager.setStr(NameplateManager.USER, bundle.getString("strUserName"))
                                    .setStr(NameplateManager.POS, bundle.getString("strCompany"))
                                    .setStr(NameplateManager.COMP, bundle.getString("strPosition"))
                                    .save();
                            appMsg.what = USER_INFO;
                            AppActivity.handler.sendMessage(appMsg);

                            String url = bundle.getString("strNameplateUrl");
                            if(url != null){
                                Bundle downloadBundle = new Bundle();
                                downloadBundle.putString(DOWNLOAD_PATH,NAMEPLATE_IMG_PATH+"id_"+ databaseManager.getDeviceID()+"/");
                                downloadBundle.putString(DOWNLOAD_URL,url);
                                downloadBundle.putString(DOWNLOAD_TYPE,"NP");
                                sendToNetwork(CMD_HTTP_DOWNLOAD,downloadBundle);
                            }

                            url = bundle.getString("strNameplateBGUrl");
                            if(url != null){
                                Bundle downloadBundle = new Bundle();
                                downloadBundle.putString(DOWNLOAD_PATH,NAMEPLATE_BACKGROUND_PATH+"id_"+ databaseManager.getDeviceID()+"/");
                                downloadBundle.putString(DOWNLOAD_URL,url);
                                downloadBundle.putString(DOWNLOAD_TYPE,"BG");
                                sendToNetwork(CMD_HTTP_DOWNLOAD,downloadBundle);
                            }
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
                                appMsg.what = USER_LIST;
                                appMsg.obj = map;
                                AppActivity.handler.sendMessage(appMsg);
                            } catch (JSONException e) {
                                e.printStackTrace();
                            }
                            break;
                        case EVT_TS_REQSERVICE_ACK:
                            appMsg.what = SERVIE_ACK;
                            appMsg.obj = bundle.getString("strContent");
                            AppActivity.handler.sendMessage(appMsg);
                            break;
                        case EVT_TS_CENTERCONTROL:
                            Debug.d(TAG, "Device id : " + bundle.getInt("iDeviceID") + " , ControlType : " + bundle.getInt("iControlType"));
                            switch (bundle.getInt("iControlType")) {
                                case 1:
                                    MyApplication.LocalBroadcast.send(ACTION_STAR_ACTIVITY, MainActivity.class);
                                    break;
                                case 2:
                                    MyApplication.LocalBroadcast.send(ACTION_STAR_ACTIVITY, MeetingInfoActivity.class);
                                    break;
                                case 3:
                                    MyApplication.LocalBroadcast.send(ACTION_STAR_ACTIVITY, ShowNameActivity.class);
                                    break;
                                case 10:
                                    Cmd.execCmd("reboot -p");
                                    break;
                            }
                            break;
                        case EVT_TS_MEETING_END:
                            appMsg.what = MEETING_END;
                            AppActivity.handler.sendMessage(appMsg);
                            Cmd.execCmd("rm -rf "+NAMEPLATE_IMG_PATH);
                            Cmd.execCmd("rm -rf "+NAMEPLATE_BACKGROUND_PATH);
                            databaseManager.delMeetInfo();
                            resetNetwork();
                            break;
                        case EVT_TS_SENDMSG:
                            appMsg.setData(bundle);
                            appMsg.what = bundle.getInt("iReceiverID") == 0 ? SYS_SMS_MSG : SMS_MSG;
                            AppActivity.handler.sendMessage(appMsg);
                            break;
                        case EVT_TS_MEETING_ID:
                            final int meetId = bundle.getInt("iMeetID");
//                            Log.d(TAG, "meeting id = " + meetId);
                            if(meetId != 0 && databaseManager.getMeetId() != meetId ){
                                Log.d(TAG,"save id = "+databaseManager.getMeetId()+" meetId = "+meetId);
                                databaseManager.setMeetId(meetId).save();
                                databaseManager.delMsg();
                            }

                            else if(meetId == 0){
                                Log.d(TAG,"meetId = "+meetId);
                                databaseManager.delMeetInfo();
                            }
                            LocalBroadcast.send(MyApplication.ACTION_REFRESH_ACTIVITY, MainActivity.class);
                            LocalBroadcast.send(MyApplication.ACTION_REFRESH_ACTIVITY, MeetingInfoActivity.class);
                            LocalBroadcast.send(MyApplication.ACTION_REFRESH_ACTIVITY, EditUserInfoActivity.class);
                            break;
                    }
                    break;
            }
        }
    };

    private Messenger recMessenger = new Messenger(recHandler);

    private void sendToNetwork(int what, Bundle bundle) {
        bundle.putInt("what", what);
        sendQueue.add(bundle);
    }

    private static Runnable getSendQueue = new Runnable() {
        @Override
        public void run() {
            synchronized ("getSendQueue") {
                while (true) {
                    try {
                        if (sendQueue.size() > 0) {
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

    public NetDevManager.NetDevInfo getNetDevInfo() {
        return netDevInfo;
    }

    public void setNetworkInfo(int[] ip, int[] mask, int[] gw) {
        netDevManager.setNetDevInfo(ip, mask, gw);
    }

    public void creatWifiSelection(Context context) {
        Message msg = Message.obtain();
        msg.what = NetDevManager.ACTION_WIFI_SELECT;
        msg.obj = context;
        netDevManager.action(msg);
    }

    public void removeAllWifi() {
        Message msg = Message.obtain();
        msg.what = NetDevManager.ACTION_REMOVE_ALL_WIFI;
        netDevManager.action(msg);
    }

    public void setDhcpEn() {
        netDevManager.setDhcpEn();
    }

    public void sendDevInfo() {
        try {
            Bundle bundle = new Bundle();
            bundle.putInt(DEV_ID, databaseManager.getDeviceID());
            bundle.putInt(DEV_BRIGHTNESS, databaseManager.getBrightness());
            bundle.putInt(DEV_BAT_LEVEL, powerManager.getLevel());
            bundle.putInt(WIFI_RSSI, netDevInfo.rssi);
            bundle.putString(NETWORK_MAC, netDevInfo.mac);
            bundle.putBoolean(NET_DRIVE_EN, netDevInfo.devEn);
            bundle.putBoolean(NETWORK_EN, netDevInfo.netEn);
            bundle.putIntArray(NETWORK_LOCAL_IP, netDevInfo.ip);
            sendToNetwork(Network.CMD_UPDATA_DEV_INFO, bundle);
        }catch (Exception e){
            e.printStackTrace();
            if(errCount++ > 10)
                LocalBroadcast.send(ACTION_HARDFAULT_REBOOT);
        }
    }

    public void resetNetwork() {
        sendDevInfo();

        Bundle bundle = new Bundle();
        bundle.putIntArray(NETWORK_SERV_IP, databaseManager.getServIp());
        bundle.putInt(NETWORK_SERV_PORT, databaseManager.getServPort());
        sendToNetwork(Network.CMD_RESET_NETWORK, bundle);
    }

    public void callService(String content) {
        Bundle bundle = new Bundle();

        bundle.putInt("iCmdEnum", Network.EVT_TS_REQSERVICE);
        bundle.putString("strContent", content);
        bundle.putInt("iServiceID", 0);
        sendToNetwork(Network.CMD_TRANSMIT_DATA, bundle);
    }

    public void sendSms(int id, String content) {
        Bundle bundle = new Bundle();

        bundle.putInt("iCmdEnum", EVT_TS_SENDMSG);
        bundle.putString("strContent", content);
        bundle.putInt("iReceiverID", id);
        sendToNetwork(Network.CMD_TRANSMIT_DATA, bundle);
    }

    public int getNetworkStatus() {
        return networkStatus;
    }

    public void setOnNetworkStatusListener(OnNetworkStatusListener listener) {
        staListener = listener;
    }

    @Override
    public void infoUpdate(String devName) {
        Bundle bundle = new Bundle();
        bundle.putString(NET_DRIVE_NAME, devName);
        bundle.putBoolean(NET_DRIVE_EN, netDevInfo.devEn);
        bundle.putBoolean(NETWORK_EN, netDevInfo.netEn);
        bundle.putBoolean(NETWORK_DHCP_EN, netDevInfo.dhcp);
        bundle.putIntArray(NETWORK_LOCAL_IP, netDevInfo.ip);
        bundle.putIntArray(NETWORK_MASK, netDevInfo.mask);
        bundle.putIntArray(NETWORK_GATEWAY, netDevInfo.gw);
        bundle.putString(NETWORK_MAC, netDevInfo.mac);
        bundle.putString(WIFI_SSID, netDevInfo.ssid);
        bundle.putInt(WIFI_RSSI, netDevInfo.rssi);

        MyApplication.LocalBroadcast.send(MyApplication.ACTION_NETWORK_INFO_UPDATE, bundle);
    }

    public interface OnNetworkStatusListener {
        void OnNetworkStatus(int sta);
    }

}
