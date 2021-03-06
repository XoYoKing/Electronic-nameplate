package com.itc.ts8209a.server;

import android.app.Service;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.*;
import android.os.Handler;
import android.util.Log;

import com.itc.ts8209a.widget.*;
import com.itc.ts8209a.widget.Debug;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedOutputStream;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.UnsupportedEncodingException;
import java.lang.ref.SoftReference;
import java.net.HttpURLConnection;
import java.net.Socket;
import java.net.SocketException;
import java.net.SocketTimeoutException;
import java.net.URL;
import java.util.Arrays;
import java.util.Deque;
import java.util.Timer;
import java.util.TimerTask;
import java.util.concurrent.LinkedBlockingDeque;

import static com.itc.ts8209a.app.AppConfig.*;
import static com.itc.ts8209a.module.network.NetworkManager.*;

/**
 * Created by kuangyt on 2018/8/30.
 */

public class Network extends Service {
    private static final String TAG = "Network";

    //***************** 网络状态字段 *********************/
    public static final int SOC_STA_CONNECTING = 1;
    public static final int SOC_STA_CONNECTED = 2;
    public static final int SOC_STA_DISCONNECTED = 3;
    public static final int SOC_STA_CONNECT_FAIL = 4;

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
    public static final int CMD_REFRESH_NETWORK = 25;

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
    private SocketHandler socketHandler;
    //图片服务器下载
    private HttpHandler httpHandler;
    //网络状态监控
    private NetworkMonitor networkMonitor;
    //服务器连接标志
    private boolean servConnected = false;
    //设备注册标志
    private boolean devRegistered = false;
    //会议信息获取标志
    private boolean getUserList = false;
    //进程间通讯
    private Messenger replyMessenger;

    //***************** 设备参数变量 *********************/
    private int wifiRssi = 0;
    private String netMac = "";
    private boolean netDevEn = false;
    private boolean networkEn = false;
    private int batLevel = 0;
    private int devId = 0;
    private int devBrightness = 0;
    private int[] servIp = {0, 0, 0, 0};
    private int[] localIp = {0, 0, 0, 0};
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
    public int onStartCommand(Intent intent, int flags, int startId) {
        return START_STICKY;
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
    }

    private Handler recMessengerHandler = new Handler() {
        @Override
        public void handleMessage(final Message msg) {
            if (msg == null)
                return;
            Bundle bundle = msg.getData();
//            Log.d(TAG,"what = "+msg.what);
            switch (msg.what) {
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
                    getUserList = false;
                    restartNetwork();
                    break;
                case CMD_SET_REPLY:
                    if (msg.replyTo != null)
                        replyMessenger = msg.replyTo;
                    break;
                case CMD_TRANSMIT_DATA:
                    Message transMsg = Message.obtain();
                    transMsg.what = bundle.getInt("iCmdEnum");
                    transMsg.setData(bundle);
                    socketTransHandler.sendMessage(transMsg);
                    break;
                case CMD_HTTP_DOWNLOAD:
                    String url = bundle.getString(DOWNLOAD_URL);
                    String path = bundle.getString(DOWNLOAD_PATH);

                    httpHandler.donwloadImg(url,path);
                    break;
            }
        }
    };

    private Messenger recMessenger = new Messenger(recMessengerHandler);


    private void creatNetwork() {
        /* 启动网络监控线程 */
        networkMonitor = new NetworkMonitor();
        (new Thread(networkMonitor)).start();

        /* 初始化Socket控制句柄 */
        socketHandler = new SocketHandler();
        socketHandler.setReceiveHandler(socketRecHandler);
        socketHandler.setSocketStaHandler(socketStateHandler);

        /* 初始化HTTP下载句柄 */
        httpHandler = new HttpHandler();
        httpHandler.setResultHandler(new Handler() {
            @Override
            public void handleMessage(Message msg) {
                try {
                    Message httpResMsg = Message.obtain();
                    Bundle bundle = new Bundle();
                    bundle.putBoolean("httpDownLoadRes", msg.what == 1);
                    bundle.putString("filePath", (String) msg.obj);
//                    bundle.putString("type", type);
                    httpResMsg.setData(bundle);
                    httpResMsg.what = CMD_HTTP_DOWNLOAD_RES;
                    replyMessenger.send(httpResMsg);
                } catch (RemoteException e) {
                    e.printStackTrace();
                }

            }
        });

        String ip = servIp[0] + "." + servIp[1] + "." + servIp[2] + "." + servIp[3];
        socketHandler.setIp(ip).setPort(servPort);
        socketHandler.connect();
    }

    private void restartNetwork() {


        String ip = servIp[0] + "." + servIp[1] + "." + servIp[2] + "." + servIp[3];

        socketHandler.setIp(ip).setPort(servPort);
        socketHandler.reconnect();
    }

    private Handler socketStateHandler = new Handler() {

        private void sendStatus(int sta) {
            if (replyMessenger != null) {
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


        @Override
        public void handleMessage(Message msg) {
            switch (msg.what) {
                case SOC_STA_CONNECTED: {
                    Log.d(TAG, "socket is connected");
                    sendStatus(msg.what);
//                    socketTransHandler.sendEmptyMessage(REQ_TS_DEVICE_REG);
                }
                break;

                case SOC_STA_DISCONNECTED:
                default: {
                    Log.d(TAG, "socket is disconnect");
                    sendStatus(msg.what);
                }
                break;
            }
        }
    };

    private Handler socketTransHandler = new Handler() {
        private byte[] head = {0x49, 0x54, 0x43, 0x4C, 0x01, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00};

        @Override
        public void handleMessage(Message msg) {
            try {
                JSONObject json = new JSONObject();
                Bundle bundle = msg.getData();
                json.put("iCmdEnum", msg.what);
                json.put("iDeviceID", devId);
                switch (msg.what) {
                    case REQ_HEARTBEAT:
                        json.put("iElectric", batLevel);
                        json.put("iLuminance", devBrightness);
                        json.put("iWifi", wifiRssi);
                        json.put("strIp", localIp[0] + "." + localIp[1] + "." + localIp[2] + "." + localIp[3]);
                        break;
                    case REQ_TS_DEVICE_REG:
                        json.put("strMac", netMac);
                        json.put("strIp", localIp[0] + "." + localIp[1] + "." + localIp[2] + "." + localIp[3]);
                        break;
                    case REQ_TS_GET_USERINFO:
                        break;
                    case REQ_TS_GET_USERLIST:
                        break;
                    case EVT_TS_REQSERVICE:
                        json.put("iServiceID", bundle.getInt("iServiceID"));
                        json.put("strContent", bundle.getString("strContent"));
                        break;
                    case EVT_TS_SENDMSG:
                        json.put("strContent", bundle.getString("strContent"));
                        json.put("iReceiverID", bundle.getInt("iReceiverID"));
                        json.put("iMsgType", 1);
                        json.put("iContentType", 0);
                        json.put("lstRecvDeviceID", 0);
                        break;
                    default://对于非定义的字段，直接将json对象置null
                        Log.d(TAG, "undefine value:" + msg.what);
                        json = null;
                        break;
                }

                if (json != null) {
//                    Debug.d(TAG,"send json:"+json.toString());
                    byte[] jsonByte = json.toString().getBytes(SERV_ENCODING);
                    byte[] sendContent = new byte[head.length + jsonByte.length];
                    System.arraycopy(head, 0, sendContent, 0, head.length);
                    System.arraycopy(jsonByte, 0, sendContent, head.length, jsonByte.length);
                    sendContent[8] = (byte) sendContent.length;
                    sendContent[9] = (byte) (sendContent.length / 256);
                    socketHandler.sendMsg(sendContent);
//                    networkMonitor.sendHartbeatTimeReset();
                }
            } catch (JSONException e) {
                e.printStackTrace();
            } catch (UnsupportedEncodingException e) {
                e.printStackTrace();
            }
        }
    };

    private Handler socketRecHandler = new Handler() {
        private JSONObject json;

        @Override
        public void handleMessage(Message msg) {
            byte[] recvByte = (msg.getData()).getByteArray("RECV_BYTE");
            byte[] jsonByte = new byte[recvByte.length - 24];

            System.arraycopy(recvByte, 24, jsonByte, 0, jsonByte.length);

            json = byteToJson(jsonByte);
//            Debug.d(TAG,"receive json:"+json.toString());
            if (json != null) {
                try {
                    Bundle bundle = new Bundle();
                    Message recMsg = Message.obtain();
                    recMsg.what = CMD_RECEIVE_DATA;

                    int iCmdEnum = json.getInt("iCmdEnum");
                    Debug.d(TAG, "command receiver : " + "iCmdEnum:" + iCmdEnum);
                    bundle.putInt("iCmdEnum", iCmdEnum);
                    switch (iCmdEnum) {
                        case RSP_HEARTBEAT: // 102
                            if (json.getInt("iResult") == 400)
                                devRegistered = false;
                            break;
                        case RSP_TS_DEVICE_REG:
                            int iRes = json.getInt("iResult");
                            bundle.putInt("iResult", iRes);
                            switch (iRes) {
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
                            bundle.putString("strName", json.getString("strName"));
                            bundle.putString("strSlogan", json.getString("strSlogan"));
                            bundle.putString("strContent", json.getString("strContent"));
                            bundle.putString("strStartTime", json.getString("strStartTime"));
                            bundle.putString("strEndTime", json.getString("strEndTime"));
                            break;
                        case EVT_TS_MEETING_BEGIN:
                            break;
                        case EVT_TS_MEETING_END:
                            devRegistered = false;
                            getUserList = false;
                            break;
                        case EVT_TS_QUIT_MEETING:
                            break;
                        case RSP_TS_GET_USERINFO:
                            Log.d(TAG, json.toString());
                            bundle.putString("strUserName", json.getString("strUserName"));
                            bundle.putString("strCompany", json.getString("strCompany"));
                            bundle.putString("strPosition", json.getString("strPosition"));
                            if (!json.isNull("strNameplateUrl"))
                                bundle.putString("strNameplateUrl", json.getString("strNameplateUrl"));
                            if (!json.isNull("strNameplateBGUrl"))
                                bundle.putString("strNameplateBGUrl", json.getString("strNameplateBGUrl"));

                            break;
                        case RSP_TS_GET_USERLIST:
                            bundle.putString("lstDevice", json.getJSONObject("lstDevice").toString());
                            getUserList = true;
                            break;
                        case EVT_TS_REQSERVICE_ACK:
                            bundle.putString("strContent", json.getString("strContent"));
                            break;
                        case EVT_TS_CENTERCONTROL:
                            bundle.putInt("iDeviceID", json.getInt("iDeviceID"));
                            bundle.putInt("iControlType", json.getInt("iControlType"));
                            break;
                        case EVT_TS_USERSTATUS:
                            break;
                        case EVT_TS_NAMEPLATE_UPDATE:
                            socketTransHandler.sendEmptyMessage(REQ_TS_GET_USERINFO);
                            break;
                        case EVT_TS_SENDMSG:
                            bundle.putString("strContent", json.getString("strContent"));
                            bundle.putInt("iReceiverID", json.getInt("iReceiverID"));
                            bundle.putInt("iMsgType", json.getInt("iMsgType"));
                            break;
                        case EVT_TS_MEETING_ID:
                            int meetingId;
                            if (json.has("iMeetID"))
                                meetingId = json.getInt("iMeetID");
                            else
                                meetingId = 0;

                            bundle.putInt("iMeetID", meetingId);
                            break;
                    }
                    recMsg.setData(bundle);
                    if (replyMessenger != null)
                        replyMessenger.send(recMsg);
                    servConnected = true;
                    networkMonitor.recHartbeatTimeReset();
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

        private JSONObject byteToJson(byte[] msgByte) {
            JSONObject jsonObject = null;
            String msgStr = null;
            try {
                msgStr = (new String(Arrays.copyOf(msgByte, msgByte.length), SERV_ENCODING));

                if (msgStr.indexOf('{') != -1 && msgStr.lastIndexOf('}') != -1) {
                    try {
//                    String jsonStr = new String(msgStr.substring(msgStr.indexOf('{'), msgStr.lastIndexOf('}') + 1));
                        String jsonStr = msgStr.substring(msgStr.indexOf('{'), msgStr.lastIndexOf('}') + 1);
                        jsonObject = new JSONObject(jsonStr);
                    } catch (JSONException e) {
                        Log.e(TAG, "byte to json err:" + e);
                    }
                }
            } catch (UnsupportedEncodingException e) {
                e.printStackTrace();
            }

            return jsonObject;
        }
    };

    private class NetworkMonitor implements Runnable {

        private int sendHartbeatTime = 0;
        private int recHartbeatTime = 0;
        private int restartNetworkTime = 0;
        private int devRegisteTime = DEV_REGISTE_TIME;

        private void sendHartbeatTimeReset() {
            sendHartbeatTime = 0;
        }

        private void recHartbeatTimeReset() {
            recHartbeatTime = 0;
        }


        @Override
        public void run() {
//            if(running)
//                return;
            synchronized (this) {
                while (true) {
                    try {
                        if (!socketHandler.isSocketConnected) {
                            if (restartNetworkTime++ >= RESTART_NET_TIME) {
                                if (networkEn) {
                                    if(!socketHandler.isSocketCreating) {
                                        Log.d(TAG, "NetworkMonitor->Restart to connect network");
                                        restartNetwork();
                                    }
                                } else {
                                    Log.d(TAG, "NetworkMonitor->Network conditions not available!");
                                }
                                restartNetworkTime = 0;
                            }
                        } else {
                            if (!devRegistered) {
                                if (!netMac.equals("00:00:00:00:00:00") && !(localIp[0] == 0 && localIp[1] == 0 && localIp[2] == 0 && localIp[3] == 0)) {
                                    if (devRegisteTime++ >= DEV_REGISTE_TIME) {
                                        socketTransHandler.sendEmptyMessage(REQ_TS_DEVICE_REG);
                                        devRegisteTime = 0;
                                    }
//                                    Thread.sleep(30 * 1000);
                                }
                            } else {
                                if (sendHartbeatTime++ >= SEND_HARTBEAT_TIME) {
                                    socketTransHandler.sendEmptyMessage(REQ_HEARTBEAT);
                                    if (!getUserList)
                                        socketTransHandler.sendEmptyMessage(REQ_TS_GET_USERLIST);
                                    sendHartbeatTimeReset();
                                }
                            }

                            if (recHartbeatTime++ >= REC_HARTBEAT_TIMEOUT) {
                                if(!httpHandler.isDownloading) {
                                    Log.d(TAG, "HartbeatTime OVER .. ");
                                    restartNetwork();
                                }
                                recHartbeatTimeReset();
                            }
                        }

                        Thread.sleep(1000);
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                }
            }
        }
    }

    private class HttpHandler {
        public boolean isDownloading = false;

        private String url;
        private String filePath;
        private Bitmap bitmap;
        private Handler resultHandler = null;
        private Thread download = null;


        public HttpHandler() {
        }

//        public HttpHandler(String filePath) {
//            this.filePath = filePath;
//        }

        public void donwloadImg(String url,String filePath) {
            this.url = url;
            this.filePath = filePath;

            if(download != null){
                download.interrupt();
                download = null;
            }

            download = new Thread(downloadRunnable);
            download.start();
        }

        public void setResultHandler(Handler handler) {
            resultHandler = handler;
        }

        private Runnable downloadRunnable = new Runnable() {
            private int retryTime;

            @Override
            public void run() {
                Message msg = Message.obtain();

                msg.what = 0;

                isDownloading = true;
                synchronized ("ImageDownload") {
                    retryTime = HTTP_REQUEST_TIMES;
                    while (retryTime-- > 0) {
                        try {
                            Log.d(TAG,"Image download try time = " + (HTTP_REQUEST_TIMES - retryTime));
                            byte[] data = getImage(url);
                            if (data != null) {
//                                getBitmapHWfromByte(data);
//                                bitmap = byteToBitmap(data);
//                                Log.d(TAG,"bitmap height = "+bitmap.getHeight()+"  bitmap width = "+bitmap.getWidth());
                                String fileName = String.format("%s_", System.currentTimeMillis()) + url.substring(url.lastIndexOf('/') + 1, url.length());
                                saveFile(data, fileName);
                                msg.what = 1;
                                msg.obj = filePath + fileName;
                                break;
                            }
                            Thread.sleep(3000);

                        } catch (IOException e) {
                            e.printStackTrace();
                        } catch (Exception e) {
                            e.printStackTrace();
                        }
                    }
                    if (resultHandler != null)
                        resultHandler.sendMessage(msg);
                }
                isDownloading = false;
            }
        };

        private int[] getBitmapHWfromByte(byte[] imgByte) {
            int[] hw = new int[2];
            BitmapFactory.Options options = new BitmapFactory.Options();
            options.inJustDecodeBounds = true;
            BitmapFactory.decodeByteArray(imgByte, 0, imgByte.length, options);

            hw[0] = options.outHeight;
            hw[1] = options.outWidth;
            return hw;
        }

        private Bitmap byteToBitmap(byte[] imgByte) {
            InputStream input;
            Bitmap bitmap;
            BitmapFactory.Options options = new BitmapFactory.Options();

            int[] hw = getBitmapHWfromByte(imgByte);
            if (hw[0] > 2048 && hw[1] > 1024) {
                options.inSampleSize = 2;
                Log.d(TAG,"inSampleSize = 2 ");
            }
            else {
                options.inSampleSize = 1;
                Log.d(TAG,"inSampleSize = 1 ");
            }
            input = new ByteArrayInputStream(imgByte);
            SoftReference softRef = new SoftReference(BitmapFactory.decodeStream(input, null, options));
            bitmap = (Bitmap) softRef.get();
            if (imgByte != null) {
                imgByte = null;
            }

            try {
                if (input != null) {
                    input.close();
                }
            } catch (IOException e) {
                // TODO Auto-generated catch block
                e.printStackTrace();
            }

            return bitmap;
        }

        private byte[] getImage(String path) throws Exception {
            if (path == null)
                return null;

            URL url = new URL(path);
            Log.d(TAG,"Image url : " + url);
            HttpURLConnection conn = (HttpURLConnection) url.openConnection();
            conn.setConnectTimeout(HTTP_REQUEST_INTERVAL * 1000);
            conn.setReadTimeout(HTTP_READ_TIMEOUT * 1000);
            conn.setRequestMethod("GET");
            conn.setRequestProperty("Connection","close");
            conn.setRequestProperty("Content-Type","image/jpeg");
            InputStream inStream = conn.getInputStream();
            if (conn.getResponseCode() == HttpURLConnection.HTTP_OK) {
                return readStream(inStream);
            }
            return null;
        }

        private byte[] readStream(InputStream inStream) throws Exception {
            ByteArrayOutputStream outStream = new ByteArrayOutputStream();
            byte[] buffer = new byte[4096];
            int len;
            while ((len = inStream.read(buffer)) != -1) {
                outStream.write(buffer, 0, len);
            }
            outStream.close();
            inStream.close();
            return outStream.toByteArray();
        }

        private void saveFile(Bitmap bm, String fileName) throws IOException {
//            fileName = "abc.jpg";
            File jia = new File(filePath);
            if (!jia.exists()) {   //判断文件夹是否存在，不存在则创建
                jia.mkdirs();
            }
            File myCaptureFile = new File(jia + "/" + fileName);
            BufferedOutputStream bos = new BufferedOutputStream(new FileOutputStream(myCaptureFile));
            bm.compress(Bitmap.CompressFormat.JPEG, 100, bos);
            bos.flush();
            bos.close();
        }

        private void saveFile(byte[] by, String fileName) throws IOException {
//            fileName = "abc.jpg";
            File jia = new File(filePath);
            if (!jia.exists()) {   //判断文件夹是否存在，不存在则创建
                jia.mkdirs();
            }
            Log.d(TAG,"data byte size = "+ by.length);
            File myCaptureFile = new File(jia + "/" + fileName);
            FileOutputStream stream = new FileOutputStream(myCaptureFile);
            stream.write(by,0,by.length);
            stream.close();
        }
    }

    private class SocketHandler {

        private boolean isSocketConnected = false;
        private boolean isSocketCreating = false;

        private Socket socket;
        private android.os.Handler receiveHandler = null;
        private android.os.Handler socketStaHandler = null;
        private int bufSize = 40 * 1024;
        private String ip = "";
        private int port = 0;

        //Thread
        private CreatSocketThread creatSocket;
        private receiveThread receiver;
        private sendThread sender;
        private Timer socMonitor;
        private socMonitorTask monitorTask;

        private SocketHandler() {
        }

        private SocketHandler setIp(String ip) {
            this.ip = ip;
            return this;
        }

        private SocketHandler setPort(int port) {
            this.port = port;
            return this;
        }

        private SocketHandler setReceiveHandler(android.os.Handler handler) {
            receiveHandler = handler;
            return this;
        }

        private SocketHandler setSocketStaHandler(android.os.Handler handler) {
            socketStaHandler = handler;
            return this;
        }

        private void connect() {
            if (isSocketCreating)
                return;
            creatSocket = new CreatSocketThread();
            creatSocket.start();
        }

        private void close() {
            try {
                if (receiver != null) {
                    receiver.interrupt();
                    receiver = null;
                }
                if (creatSocket != null) {
                    creatSocket.interrupt();
                    creatSocket = null;
                }
                if (sender != null) {
                    sender.interrupt();
                    sender = null;
                }

                if (monitorTask != null){
                    monitorTask.cancel();
                    monitorTask = null;
                }
                if (socMonitor != null){
                    socMonitor.cancel();
                    socMonitor.purge();
                    socMonitor = null;
                }

                if (null != socket) {
                    socket.close();
                    socket = null;
                }

                if (socketStaHandler != null) {
                    socketStaHandler.sendEmptyMessage(SOC_STA_DISCONNECTED);
                }
                isSocketConnected = false;
            } catch (IOException e) {
                e.printStackTrace();
            }
        }

        private void reconnect() {
            if (isSocketCreating)
                return;
            close();
            connect();
        }

        private void sendMsg(byte[] msg) {
            synchronized ("sendMsg") {
                if (sender != null) {
                    sender.send(msg);
                }
            }
        }

        public void sendMsg(String msg) {
            sendMsg(msg.getBytes());
        }


        private class CreatSocketThread extends Thread {
            @Override
            public void run() {
                isSocketCreating = true;
//                while (true) {
                try {
//                        while (!(netDevEn && networkEn)) {
//                            sleep(1000);
//                        }
                    socket = new Socket(ip, port);
                    if (socket.isConnected()) {
                        /* 开启接收线程 */
                        receiver = new receiveThread();
                        receiver.start();

                        /* 开启发送线程 */
                        sender = new sendThread();
                        sender.start();

                        /* socket监控定时器 */
//                        socMonitor = new Timer();
//                        monitorTask = new socMonitorTask();
//                        socMonitor.schedule(monitorTask, 1000, 2000);

                        if (socketStaHandler != null) {
                            socketStaHandler.sendEmptyMessage(SOC_STA_CONNECTED);
                        }
                        isSocketConnected = true;
                        isSocketCreating = false;
                        return;
//                            break;
                    }
//                        Thread.sleep(3000);
                } catch (IOException e) {
                    e.printStackTrace();
                    socket = null;
                }
//                    catch (InterruptedException e) {
//                        break;
//                    }
//                }
                isSocketCreating = false;
            }
        }

        private class receiveThread extends Thread {

            private final byte[] netHead = NET_PROTOCOL_HEAD.getBytes();

            @Override
            public void run() {
                synchronized (this) {
                    byte[] buffer = new byte[bufSize];
                    int length;

                    if (socket == null) {
                        close();
                        return;
                    }
                    try {
                        InputStream stream = socket.getInputStream();
//                        Log.d(TAG,socket.isClosed()+" "+socket.isInputShutdown()+" "+stream.read(buffer)+" "+socketConnected);
                        while (!socket.isClosed() && !socket.isInputShutdown()) {
                            if ((length = stream.read(buffer)) != -1 && length > 0) {
                                if (receiveHandler != null && length > netHead.length) {
                                    for (int i = 0; i < (length - netHead.length); i++) {
                                        if (buffer[i] == netHead[0] && buffer[i + 1] == netHead[1] && buffer[i + 2] == netHead[2] && buffer[i + 3] == netHead[3]) {
                                            int packLen = (buffer[i + 8] & 0xFF) + (buffer[i + 9] & 0xFF) * 256;
                                            if (packLen <= 24)
                                                continue;

                                            byte[] buf = new byte[packLen];
                                            Bundle bundle = new Bundle();

                                            System.arraycopy(buffer, i, buf, 0, packLen);
                                            bundle.putByteArray("RECV_BYTE", buf);

                                            Log.d(TAG, "packLen = " + packLen + " content:" + General.Byte2Str(buf, packLen > 50 ? 50 : packLen) + "..  end :" + buf[packLen - 2] + " " + buf[packLen - 1]);

                                            Message msg = Message.obtain();
                                            msg.setData(bundle);
                                            receiveHandler.sendMessage(msg);
                                        }
                                    }
                                    Arrays.fill(buffer, (byte) 0x00);
                                }
                            }
                            Thread.sleep(500);
                        }
                    } catch (SocketException e) {
                        e.printStackTrace();
                    } catch (SocketTimeoutException e) {
                        e.printStackTrace();
                    } catch (IOException e) {
                        e.printStackTrace();
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                    close();
                }
            }
        }

        private class sendThread extends Thread {
            private Deque<byte[]> queue = new LinkedBlockingDeque<byte[]>();
            private OutputStream stream;

            public void send(byte[] msg) {
                if (msg != null) {
                    queue.add(msg);
                }
            }

            @Override
            public void run() {
                synchronized (this) {
                    if (socket == null) {
                        close();
                        return;
                    }
                    try {
                        stream = socket.getOutputStream();
                        while (!socket.isClosed() && !socket.isOutputShutdown()) {
                            if (queue.size() > 0) {
                                byte[] temp = queue.poll();
                                Log.d(TAG, "network send : " + (new String(Arrays.copyOf(temp, temp.length), SERV_ENCODING)));
                                stream.write(temp);
                                stream.flush();
//                                Thread.sleep(300);
                            }
                            Thread.sleep(300);
                        }
                    } catch (IOException e) {
                        e.printStackTrace();
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                    close();

                }
            }
        }

//        private TimerTask socMonitorTask = new TimerTask() {
//            private byte[] msg = {(byte) 0xFF};
//
//            @Override
//            public void run() {
//                Log.d(TAG, "Socket monitor task..");
//                sendMsg(msg);
//            }
//        };

        private class socMonitorTask extends TimerTask{
            private byte[] msg = {(byte) 0xFF};

            @Override
            public void run() {
                Log.d(TAG, "Socket monitor task..");
                sendMsg(msg);
            }
        }
    }

}
