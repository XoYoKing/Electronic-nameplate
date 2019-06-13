package com.itc.ts8209a.server;

import android.app.Service;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.*;
import android.util.Log;

import com.itc.ts8209a.activity.AppActivity;
import com.itc.ts8209a.widget.*;
import com.itc.ts8209a.widget.Debug;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedOutputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.UnsupportedEncodingException;
import java.net.HttpURLConnection;
import java.net.Socket;
import java.net.URL;
import java.util.Arrays;
import java.util.Timer;
import java.util.TimerTask;

import static com.itc.ts8209a.app.AppConfig.*;
import static com.itc.ts8209a.module.network.NetworkManager.*;

/**
 * Created by kuangyt on 2018/8/30.
 */

public class Network extends Service {
    private static final String TAG = "Network ";

    //***************** 网络状态字段 *********************/
    public static final int STA_CONNECTING = 1;
    public static final int STA_CONNECTED = 2;
    public static final int STA_DISCONNECTED = 3;

    //************** 进程通讯命令字段 ******************/
    //主进程->网络进程
    public static final int CMD_RESET_NETWORK = 11;
    public static final int CMD_UPDATA_DEV_INFO = 12;
    public static final int CMD_SET_REPLY = 13;
    public static final int CMD_TRANSMIT_DATA = 14;
    public static final int CMD_HTTP_DOWNLOAD = 15;
    //网络进程->主进程
    public static final int CMD_NETWORK_STATUS = 21;
    public static final int CMD_RECEIVE_DATA = 22;
    public static final int CMD_HTTP_DOWNLOAD_RES = 23;
    public static final int CMD_ID_REPEAT = 24;

    //******************* 设备信息字段 ***********************/
//    public static final String DEV_RSSI = "DEV_WIFI_RSSI";
//    public static final String DEV_MAC = "DEV_MAC_ADDR";
//    public static final String DEV_NETDEV_EN = "NETWORK_DEVICE_ENABLE";
//    public static final String DEV_BATLEV = "BAT_LEVEL";
//    public static final String DEV_ID = "DEVICE_ID";
//    public static final String DEV_BRIGHT = "DEV_BRIGHTNESS";
//    public static final String DEV_SERVIP = "SERVER_IP";
//    public static final String DEV_SERVPO = "SERVER_PORT";
//    public static final String DEV_NETWORK_EN = "ETHERNET_ENABLE";

    //***************** 服务器指令字段 *********************/
    //指令字段 Device -> Server
    public static final int REQ_HEARTBEAT = 101;
    public static final int REQ_TS_DEVICE_REG = 201;
    public static final int REQ_TS_GET_USERINFO = 207;
    public static final int REQ_TS_GET_USERLIST = 209;
    public static final int EVT_TS_REQSERVICE = 211;
    //指令字段 Server -> Device
    public static final int RSP_HEARTBEAT = 102;
    public static final int RSP_TS_DEVICE_REG = 202;
    public static final int EVT_TS_MEETINGINFO = 203;
    public static final int EVT_TS_MEETING_BEGIN = 204;
    public static final int EVT_TS_MEETING_END = 205;
    public static final int EVT_TS_QUIT_MEETING = 206;
    public static final int RSP_TS_GET_USERINFO = 208;
    public static final int RSP_TS_GET_USERLIST = 210;
    public static final int EVT_TS_REQSERVICE_ACK = 212;
    public static final int EVT_TS_CENTERCONTROL = 214;
    public static final int EVT_TS_USERSTATUS = 215;
    public static final int EVT_TS_NAMEPLATE_UPDATE = 216;
    public static final int EVT_TS_MEETING_ID = 217;
    //指令字段 Device -> Server -> Device
    public static final int EVT_TS_SENDMSG = 213;


    //***************** 网络相关成员变量 *********************/
    //命令通讯服务器socket
    private socketHandler commandSocket;
    //图片服务器下载
    private networkMonitor monitor;
    //服务器连接标志
    private boolean servConnected = false;
    //设备注册标志
    private boolean devRegistered = false;
    //会议信息获取标志
    private boolean getMeetInfo = false;

    //***************** 设备参数变量 *********************/
    private int wifiRssi = 0;
    private String netMac = "";
    private boolean netDevEn = false;
    private boolean networkEn = false;
    private int batLevel = 0;
    private int devId = 0;
    private int devBrightness = 0;
    private int[] servIp = {0,0,0,0};
    private int[] localIp = {0,0,0,0};
    private int servPort = 0;

    @Override
    public IBinder onBind(Intent intent) {
        return recMessenger.getBinder();
    }

    @Override
    public void onCreate() {
        super.onCreate();

        creatNetwork();

    }

    @Override
    public void onDestroy() {
        super.onDestroy();
    }

    //进程间通讯
    private Messenger replyMessenger;

    private Handler recMessengerHandler = new Handler(){
        @Override
        public void handleMessage(final Message msg) {
            if(msg == null)
                return;
            Bundle bundle = msg.getData();
//            Log.d(TAG,"what = "+msg.what);
            switch (msg.what){
                case CMD_UPDATA_DEV_INFO:
                    wifiRssi = bundle.getInt(WIFI_RSSI);
                    netMac = bundle.getString(NETWORK_MAC);
                    localIp = bundle.getIntArray(NETWORK_LOCAL_IP);
                    netDevEn = bundle.getBoolean(NET_DRIVE_EN);
                    networkEn = bundle.getBoolean(NETWORK_EN);
                    batLevel = bundle.getInt(DEV_BAT_LEVEL);
                    devId = bundle.getInt(DEV_ID);
                    devBrightness = bundle.getInt(DEV_BRIGHTNESS);
                    break;
                case CMD_RESET_NETWORK:
                    servIp = bundle.getIntArray(NETWORK_SERV_IP);
                    servPort = bundle.getInt(NETWORK_SERV_PORT);
                    devRegistered = false;
                    restartNetwork();
                    break;
                case CMD_SET_REPLY:
                    if(msg.replyTo != null)
                        replyMessenger = msg.replyTo;
                    break;
                case CMD_TRANSMIT_DATA:
                    Message transMsg = Message.obtain();
                    transMsg.what = bundle.getInt("iCmdEnum");
                    transMsg.setData(bundle);
                    socketTransHandler.sendMessage(transMsg);
                    break;
                case CMD_HTTP_DOWNLOAD:
                    final String url = bundle.getString(DOWNLOAD_URL);
                    final String path = bundle.getString(DOWNLOAD_PATH);
                    final String type = bundle.getString(DOWNLOAD_TYPE);

                    httpDownloadHandler httpDownload = new httpDownloadHandler(path);
                    httpDownload.setResultHandler(new Handler() {
                        @Override
                        public void handleMessage(Message msg) {
                            try {
                                Message httpResMsg = Message.obtain();
                                Bundle bundle = new Bundle();
                                bundle.putBoolean("httpDownLoadRes", msg.what == 1);
                                bundle.putString("filePath",(String)msg.obj);
                                bundle.putString("type",type);
                                httpResMsg.setData(bundle);
                                httpResMsg.what = CMD_HTTP_DOWNLOAD_RES;
                                replyMessenger.send(httpResMsg);
                            } catch (RemoteException e) {
                                e.printStackTrace();
                            }

                        }
                    });
                    httpDownload.donwloadImg(url);
                    break;
            }
        }
    };

    private Messenger recMessenger = new Messenger(recMessengerHandler);


    public void creatNetwork(){
        restartNetwork();
        monitor = new networkMonitor();
        (new Thread(monitor)).start();
    }

    public void restartNetwork(){
        String ip = servIp[0]+"."+servIp[1]+"."+servIp[2]+"."+servIp[3];
        if(commandSocket != null)
            commandSocket.closeSocket();
        initSocket(ip,servPort);
}

    public void initSocket(String ip, int port){
//        Log.i(TAG,"Socket init( "+ip+" : "+port+")");
        commandSocket = new socketHandler(ip,port);
        commandSocket.setReceiveHandler(socketRecHandler);
        commandSocket.createSocket();
    }

    private Handler socketTransHandler = new Handler(){
        private byte[] head = {0x49,0x54,0x43,0x4C,0x01,0x00,0x00,0x00,0x00,0x00,0x00,0x00,0x00,0x00,0x00,0x00,0x00,0x00,0x00,0x00,0x00,0x00,0x00,0x00};

        @Override
        public void handleMessage(Message msg) {
            try {
                JSONObject json = new JSONObject();
                Bundle bundle = msg.getData();
                json.put("iCmdEnum", msg.what);
                json.put("iDeviceID",devId);
                switch(msg.what){
                    case REQ_HEARTBEAT:
                        json.put("iElectric",batLevel);
                        json.put("iLuminance",devBrightness);
                        json.put("iWifi",wifiRssi);
                        break;
                    case REQ_TS_DEVICE_REG:
                        json.put("strMac", netMac);
                        json.put("strIp", localIp[0]+"."+localIp[1]+"."+localIp[2]+"."+localIp[3]);
                        break;
                    case REQ_TS_GET_USERINFO:break;
                    case REQ_TS_GET_USERLIST:break;
                    case EVT_TS_REQSERVICE:
                        json.put("iServiceID", bundle.getInt("iServiceID"));
                        json.put("strContent", bundle.getString("strContent"));
                        break;
                    case EVT_TS_SENDMSG:
                        json.put("strContent",bundle.getString("strContent"));
                        json.put("iReceiverID",bundle.getInt("iReceiverID"));
                        json.put("iMsgType",1);
                        json.put("iContentType",0);
                        json.put("lstRecvDeviceID",0);
                        break;
                    default://对于非定义的字段，直接将json对象置null
                        Log.d(TAG,"undefine value:"+msg.what);
                        json = null;
                        break;
                }

                if (json != null) {
//                    Log.d(TAG,"send json:"+json.toString());
                    byte[] jsonByte = json.toString().getBytes(SERV_ENCODING);
                    byte[] sendContent = new byte[head.length + jsonByte.length];
                    System.arraycopy(head, 0, sendContent, 0, head.length);
                    System.arraycopy(jsonByte, 0, sendContent, head.length, jsonByte.length);
                    sendContent[8] = (byte) sendContent.length;
                    commandSocket.sendMsg(sendContent);
                    monitor.sendHartbeatTimeReset();
                }
            } catch (JSONException e) {
                e.printStackTrace();
            } catch (UnsupportedEncodingException e) {
                e.printStackTrace();
            }
        }
    };

    private Handler socketRecHandler = new Handler(){
        private JSONObject json;

        @Override
        public void handleMessage(Message msg) {
            json = byteToJson((byte[])msg.obj);
            if (json != null) {
                try {
                    Bundle bundle = new Bundle();
                    Message recMsg = Message.obtain();
                    recMsg.what = CMD_RECEIVE_DATA;

                    int iCmdEnum = json.getInt("iCmdEnum");
                    Debug.d(TAG,"command receiver : " + "iCmdEnum:"+iCmdEnum);
                    bundle.putInt("iCmdEnum",iCmdEnum);
                    switch (iCmdEnum) {
                        case RSP_HEARTBEAT: // 102
                            break;
                        case RSP_TS_DEVICE_REG:
                            switch (json.getInt("iResult")) {
                                case 200:
                                    SysDateTime.setDateTimeMillis(Long.parseLong(json.getString("strErrorMsg")));
                                    devRegistered = true;
                                    break;
                                case 401:
                                    recMsg.what = CMD_ID_REPEAT;
                                    break;
                                default:
                                    devRegistered = false;
                                    break;
                            }
                            break;
                        case EVT_TS_MEETINGINFO:
                            bundle.putString("strName",json.getString("strName"));
                            bundle.putString("strSlogan",json.getString("strSlogan"));
                            bundle.putString("strContent",json.getString("strContent"));
                            bundle.putString("strStartTime",json.getString("strStartTime"));
                            bundle.putString("strEndTime",json.getString("strEndTime"));
                            getMeetInfo = true;
                            break;
                        case EVT_TS_MEETING_BEGIN:
                            break;
                        case EVT_TS_MEETING_END:
                            devRegistered = false;
                            getMeetInfo = false;
                            break;
                        case EVT_TS_QUIT_MEETING:
                            break;
                        case RSP_TS_GET_USERINFO:
//                            Log.d(TAG,json.toString());
                            bundle.putString("strUserName",json.getString("strUserName"));
                            bundle.putString("strCompany",json.getString("strCompany"));
                            bundle.putString("strPosition",json.getString("strPosition"));
                            if(!json.isNull("strNameplateUrl"))
                                bundle.putString("strNameplateUrl",json.getString("strNameplateUrl"));
                            if(!json.isNull("strNameplateBGUrl"))
                                bundle.putString("strNameplateBGUrl",json.getString("strNameplateBGUrl"));

                            socketTransHandler.sendEmptyMessage(REQ_TS_GET_USERLIST);
                            break;
                        case RSP_TS_GET_USERLIST:
                            bundle.putString("lstDevice",json.getJSONObject("lstDevice").toString());
                            break;
                        case EVT_TS_REQSERVICE_ACK:
                            bundle.putString("strContent",json.getString("strContent"));
                            break;
                        case EVT_TS_CENTERCONTROL:
                            bundle.putInt("iDeviceID",json.getInt("iDeviceID"));
                            bundle.putInt("iControlType",json.getInt("iControlType"));
                            break;
                        case EVT_TS_USERSTATUS:
                            break;
                        case EVT_TS_NAMEPLATE_UPDATE:
                            socketTransHandler.sendEmptyMessage(REQ_TS_GET_USERINFO);
                            break;
                        case EVT_TS_SENDMSG:
                            bundle.putString("strContent",json.getString("strContent"));
                            bundle.putInt("iReceiverID",json.getInt("iReceiverID"));
                            bundle.putInt("iMsgType",json.getInt("iMsgType"));
                            break;
                        case EVT_TS_MEETING_ID:
                            int meetingId;
                            if(json.has("iMeetID"))
                                meetingId = json.getInt("iMeetID");
                            else
                                meetingId = 1;

                            bundle.putInt("iMeetID",meetingId);
                            break;
                    }
                    recMsg.setData(bundle);
                    if(replyMessenger != null)
                        replyMessenger.send(recMsg);
                    servConnected = true;
                    monitor.recHartbeatTimeReset();
                } catch (JSONException e) {
                    e.printStackTrace();
                } catch (InterruptedException e) {
                    e.printStackTrace();
                } catch (IOException e) {
                    e.printStackTrace();
                } catch (RemoteException e) {
                    e.printStackTrace();
                }
            }
        }

        private JSONObject byteToJson(byte[] msgByte){
            JSONObject jsonObject = null;
            String msgStr = null;
            try {
                msgStr = (new String(Arrays.copyOf(msgByte,msgByte.length),SERV_ENCODING));

                if(msgStr.indexOf('{') != -1 && msgStr.lastIndexOf('}') != -1){
                    try {
//                    String jsonStr = new String(msgStr.substring(msgStr.indexOf('{'), msgStr.lastIndexOf('}') + 1));
                        String jsonStr = msgStr.substring(msgStr.indexOf('{'), msgStr.lastIndexOf('}') + 1);
                        jsonObject = new JSONObject(jsonStr);
                    } catch (JSONException e) {
                        Log.e(TAG,"byte to json err:"+e);
                    }
                }
            } catch (UnsupportedEncodingException e) {
                e.printStackTrace();
            }

            return jsonObject;
        }
    };

    private class networkMonitor implements Runnable{
        private static final int SEND_HARTBEAT_TIME = 15; //15S
        private static final int REC_HARTBEAT_TIMEOUT = 90; //90S

//        private boolean running = false;
        private int sendHartbeatTime = SEND_HARTBEAT_TIME;
        private int recHartbeatTime = 0;

        public void sendHartbeatTimeReset(){
            sendHartbeatTime = SEND_HARTBEAT_TIME;
        }

        public void recHartbeatTimeReset(){
            recHartbeatTime = 0;
        }

        @Override
        public void run() {
//            if(running)
//                return;
            synchronized ("networkMonitor") {
//                running = true;
                while (true) {
                    try {
                        while(!commandSocket.getConnected()) {
//                            restartNetwork();
                            Thread.sleep(1000);
                        }
//                        Log.d(TAG,"networkMonitor :"+sendHartbeatTime+"  "+recHartbeatTime);
                        if(sendHartbeatTime-- <= 0) {
                            socketTransHandler.sendEmptyMessage(REQ_HEARTBEAT);
                            sendHartbeatTimeReset();
                        }

                        if(!devRegistered){
                            socketTransHandler.sendEmptyMessage(REQ_TS_DEVICE_REG);
                            Thread.sleep(5000);
                        }

                        if(recHartbeatTime++ >= REC_HARTBEAT_TIMEOUT){
                            restartNetwork();
                            recHartbeatTimeReset();
                        }
                        Thread.sleep(1000);
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                }
            }
        }
    }

    private class httpDownloadHandler {
        private String url;
//        private String fileName;
        private String filePath;
        private Bitmap bitmap;
        private Handler resultHandler = null;

        public httpDownloadHandler(String filePath){
            this.filePath = filePath;
        }

        public void donwloadImg(String url){
            this.url = url;
            new Thread(saveFileRunnable).start();
        }

        public void setResultHandler(Handler handler){
            resultHandler = handler;
        }

        private Runnable saveFileRunnable = new Runnable() {
            @Override
            public void run() {

                synchronized ("saveFileRunnable") {
                    try {
                        byte[] data = getImage(url);
                        Message msg = Message.obtain();
                        if (data != null) {
                            bitmap = BitmapFactory.decodeByteArray(data, 0, data.length);// bitmap
                            String fileName =  url.substring(url.lastIndexOf('/') + 1, url.length());
                            saveFile(bitmap, fileName);
                            msg.what = 1;
                            msg.obj = filePath+fileName;

                        } else {
                            msg.what = 0;
                            Log.e(TAG, "Image error!");
                        }

                        if (resultHandler != null)
                            resultHandler.sendMessage(msg);
                    } catch (IOException e) {
                        e.printStackTrace();
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                }
            }
        };

        private byte[] getImage(String path) throws Exception{
            URL url = new URL(path);
            HttpURLConnection conn = (HttpURLConnection) url.openConnection();
            conn.setConnectTimeout(5 * 1000);
            conn.setRequestMethod("GET");
            InputStream inStream = conn.getInputStream();
            if(conn.getResponseCode() == HttpURLConnection.HTTP_OK){
                return readStream(inStream);
            }
            return null;
        }

        private byte[] readStream(InputStream inStream) throws Exception{
            ByteArrayOutputStream outStream = new ByteArrayOutputStream();
            byte[] buffer = new byte[4096];
            int len;
            while( (len=inStream.read(buffer)) != -1){
                outStream.write(buffer, 0, len);
            }
            outStream.close();
            inStream.close();
            return outStream.toByteArray();
        }

        private void saveFile(Bitmap bm, String fileName) throws IOException {
//            fileName = "abc.jpg";
            File jia=new File(filePath);
            if(!jia.exists()){   //判断文件夹是否存在，不存在则创建
                jia.mkdirs();
            }
            File myCaptureFile = new File(jia +"/"+ fileName);
            BufferedOutputStream bos = new BufferedOutputStream(new FileOutputStream(myCaptureFile));
            bm.compress(Bitmap.CompressFormat.JPEG, 80, bos);
            bos.flush();
            bos.close();
        }
    }

    private class socketHandler{
        private Socket socket;
        private android.os.Handler receiveHandler = null;
        private int bufSize = 1500;
        private String ip = "";
        private int port = 0;
        private boolean connected = false;
        private boolean creating = false;
        private Timer socketWatchman = null;

        //Thread
        private recThread receiver;
        private creatSocThread creatSocket;
        private sendThread sender;

        public socketHandler(String ip,int port){
            this.ip = ip;
            this.port = port;
        }

        public void createSocket() {
            if(creating)
                return;
            creatSocket = new creatSocThread();
            creatSocket.start();
        }

        public void closeSocket() {
            try {
                if (null != socket) {
                    if (!socket.isClosed()) {
                        socket.close();
                    }
                    socket = null;
                }
                if(receiver != null) {
                    receiver.interrupt();
                    receiver = null;
                }
                if(creatSocket != null){
                    creatSocket.interrupt();
                    creatSocket = null;
                }
                if(sender != null){
                    sender.interrupt();
                    sender = null;
                }
                discreateSocketWatchman();
                connected = false;
                sendStatus(STA_DISCONNECTED);
            } catch (IOException e) {
                e.printStackTrace();
            }
        }

        public boolean getConnected(){
            return connected;
        }

        public void sendMsg(byte[] msg) {
            sender = new sendThread(msg);
            sender.start();
        }

        public void sendMsg(String msg) {
            sendMsg(msg.getBytes());
        }

        public void setReceiverBufSize(int bufSize) {
            this.bufSize = bufSize;
        }

        public void setReceiveHandler(android.os.Handler handler) {
            receiveHandler = handler;
        }

        private class recThread extends Thread {

            @Override
            public void run() {
                synchronized ("receiver") {
                    byte[] buffer = new byte[bufSize];
                    int length;
                    if (socket == null) {
                        closeSocket();
                        return;
                    }
                    try {
                        InputStream stream = socket.getInputStream();
//                        Log.d(TAG,socket.isClosed()+" "+socket.isInputShutdown()+" "+stream.read(buffer)+" "+connected);
                        while (!socket.isClosed() && !socket.isInputShutdown() && ((length = stream.read(buffer)) != -1) && connected) {
                            Thread.sleep(1);
                            if (length > 0) {
//                                Log.d(TAG, socket.getInetAddress() + ":" + new String(Arrays.copyOf(buffer, length)).trim());
//                                Log.d(TAG, socket.getInetAddress() + ":" + Debug.Byte2Str(buffer));
                                if (receiveHandler != null) {
                                    Message msg = Message.obtain();
                                    msg.obj = buffer.clone();
                                    receiveHandler.sendMessage(msg);
                                    Arrays.fill(buffer, (byte) 0x00);
                                }
                            }
                        }
                    } catch (IOException e) {
                        Log.e(TAG, "socket receiver err:" + e);
                    } catch (InterruptedException e) {
                        Log.i(TAG, "socket receiver exit by interrupted!");
                        closeSocket();
                    }
                    Log.e(TAG, "socket receive exit");
//                    closeSocket();
                    connected = false;
                }
            }
        }

        private class creatSocThread extends Thread{
            @Override
            public void run() {
                int i = 0;
                creating = true;
                sendStatus(STA_CONNECTING);
                while (true) {
                    try {
                        while (!(netDevEn || networkEn))
                            sleep(1000);
                        Thread.sleep(i > 0 ? 2000 : 0);
                        socket = new Socket(ip, port);
                        if (socket.isConnected()) {
                            connected = true;
                            receiver = new recThread();
                            receiver.start();
                            createSocketWatchman();
                            sendStatus(STA_CONNECTED);
                            break;
                        }
                    } catch (IOException e) {
                        socket = null;
                        Log.e(TAG, "Creat socket err: " + e.getMessage() + " try times:" + (++i));
                    } catch (InterruptedException e) {
                        Log.i(TAG, "CreatSocket exit by interrupted!");
                        break;
                    }
                }
                creating = false;
            }
        }

        private class sendThread extends Thread{

            private byte[] msg;

            public sendThread(byte[] msg){
                this.msg = msg;
            }

            @Override
            public void run() {
                synchronized ("sendMsg") {
                    int waittime = 0;
                    try {
                        while(!connected){
                            Thread.sleep(1000);
                            if(waittime++ > 60)
                                return;
                        }
                        if (!socket.isClosed() && !socket.isOutputShutdown()) {
                            OutputStream stream = socket.getOutputStream();
                            stream.write(this.msg);
                            stream.flush();
                        }
                    } catch (IOException e) {
                        Log.e(TAG, "socket send msg err:" + e);
                    } catch (InterruptedException e) {
                        Log.i(TAG, "sender exit by interrupted");
                    }
                }
            }
        }

        private void createSocketWatchman(){
            if(socketWatchman == null){
                socketWatchman = new Timer();
                socketWatchman.schedule(new watchmanTask(),5000,5000);
            }
        }

        private void discreateSocketWatchman(){
            if(socketWatchman != null){
                socketWatchman.cancel();
                socketWatchman = null;
            }
        }

        private class watchmanTask extends TimerTask{

            @Override
            public void run() {
                if(!connected){
//                    Log.d(TAG,"watchmanTask");
                    closeSocket();
                    createSocket();
                }
            }
        }

        private void sendStatus(int sta){
            if(replyMessenger != null){
                try {
                    Message msg = Message.obtain();
                    msg.what = CMD_NETWORK_STATUS;
                    msg.arg1 = sta;
                    replyMessenger.send(msg);
                } catch (RemoteException e) {
                    e.printStackTrace();
                }
            }
        }
    }

}