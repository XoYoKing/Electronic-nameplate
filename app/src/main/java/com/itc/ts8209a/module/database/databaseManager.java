package com.itc.ts8209a.module.database;

import android.content.ContentValues;
import android.content.Context;
import android.content.ContextWrapper;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.util.Log;

import com.itc.ts8209a.app.MyApplication;
import com.itc.ts8209a.module.network.WifiManager;
import com.itc.ts8209a.widget.Cmd;
import com.itc.ts8209a.widget.Debug;
import com.itc.ts8209a.widget.General;

import java.io.File;
import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.HashMap;

import static com.itc.ts8209a.app.AppConfig.*;

/**
 * Created by kuangyt on 2018/8/27.
 *
 * Update by KT on 2019/01/16.
 * 1.修改保存key，增加铭牌条目数量（目前为静态，后续可改为动态变量），主要为了方便拓展自定义铭牌条目，不限制铭牌类型（入目前的姓名、公司、职位）；
 * 2.类名从UserInfoManager改为DatabaseManager；
 * 3.保存的SharedPreferences文件分类拓展为多个；
 *
 * Update by KT on 2019/04/26.
 * 1.增加“KEY_SCREEN_DIM_TIME”保存屏幕暗屏时间；
 */

public class DatabaseManager {
    private final String TAG = this.getClass().getSimpleName();

    private MyApplication app;
    /************* 用户信息保存键(key word)  **************/
    //保存参数文件名
    private final String NAMEPLATE_PREF = APP_NAME + ".nameplate";
    private final String DEVICE_PREF = APP_NAME + ".device";
    private final String NETWORK_PREF = APP_NAME + ".network";
    private final String MEETING_PREF = APP_NAME + ".meeting";

    //设备相关参数信息
    private final String KEY_DEVICE_ID = "dev_id";
    private final String KEY_LANGUAGE = "dev_language";
    private final String KEY_BRIGHTNESS = "dev_brightness";
    private final String KEY_SCREEN_DIM_TIME = "dev_screen_dim_time";


    //铭牌相关参数信息
    private final String KEY_ITEM_NUM = "nameplate_item_num";
    private final String KEY_STR_CONTENT = "nameplate_str_content";
    private final String KEY_STR_COLOR = "nameplate_str_color";
    private final String KEY_STR_STYLE = "nameplate_str_style";
    private final String KEY_STR_SIZE = "nameplate_str_size";
    private final String KEY_STR_POS_X = "nameplate_str_pos_x";
    private final String KEY_STR_POS_Y = "nameplate_str_pos_y";
    private final String KEY_NAMEPLATE_TYPE = "nameplate_type";
    private final String KEY_BACKGROUND_COLOR = "nameplate_background_color";
    private final String KEY_BACKGROUND_IMG = "nameplate_background_img_path";
    private final String KEY_NAMEPLATE_PIC = "nameplate_picture_path";

    //网络相关参数信息
    private final String KEY_SERVER_IP = "network_server_ip";
    private final String KEY_LOCAL_IP = "network_local_ip";
    private final String KEY_GATEWAY = "network_gateway";
    private final String KEY_NETMASK = "network_netmask";
    private final String KEY_SERVER_PORT = "network_server_port";
    private final String KEY_DHCP_EN = "network_dhcp_en";
    private final String KEY_WIFI_SSID = "network_wifi_ssid";
    private final String KEY_WIFI_PWD = "network_wifi_pwd";
    private final String KEY_WIFI_TYPE = "network_wifi_type";

    //会议相关参数信息
    private final String KEY_MEET_ID = "dev_meet_id";
    private final String KEY_MEET_NAME = "meet_name";
    private final String KEY_MEET_SLOGAN = "meet_slogan";
    private final String KEY_MEET_CONTENT = "meet_content";
    private final String KEY_MEET_START_TIME = "meet_start_time";
    private final String KEY_MEET_END_TIME = "meet_end_time";

    //消息数据库
    private final String DBKEY_MSG_CONTENT = "msg_content";
    private final String DBKEY_MSG_ADDRESSER = "msg_addresser";
    private final String DBKEY_USER_LIST_ID = "user_list_id";
    private final String DBKEY_USER_LIST_NAME = "user_list_name";

    /******************** 内部对象  **********************/
    //单一实例
    private static DatabaseManager databaseManager = new DatabaseManager();

    private SharedPreferences spfNameplate = null;
    private SharedPreferences spfDevice = null;
    private SharedPreferences spfNetwork = null;
    private SharedPreferences spfMeeting = null;

    private SQLiteDatabase msgDatabase;
    /**************** 用户信息参数变量  *******************/
    //设备配置相关参数
    private String language;
    private int brightness;
    private int deviceID;
    private int screenDimTime;

    //电子铭牌相关参数
    private int itemNum;
    private int nameplateType;
    private int backgroundColor;
    private String backgroundImg;
    private String nameplatePic;
    private String[] strContent;
    private int[] strColor;
    private int[] strStyle;
    private int[] strSize;
    private float[] strPosX;
    private float[] strPosY;

    //网络配置相关参数
    private int[] localIp;
    private int[] serverIp;
    private int[] gateway;
    private int[] mask;
    private int serverPort;
    private boolean dhcp;
    private String ssid;
    private String pwd;
    private int wifiType;

    //会议信息相关参数;
    private int meetingId = 0;
    private String meetName = "";
    private String meetSlogan = "";
    private String meetContent = "";
    private String meetStartTime = "";
    private String meetEndTime = "";

    //消息相关参数
    protected static ArrayList<String> adminMsg;
    /* 短消息：用户用户列表 */
    protected static HashMap<Integer,String> userList = null;
    /*  短消息：短消息内容，新旧消息列表 */
    protected static ArrayList<String[]> smsMsg;

    private DatabaseManager() {
    }


    public static DatabaseManager getDatabaseManager() {
        return databaseManager;
    }

    public void init(MyApplication app) {
        this.app = app;

        sharedPreferencesInit();
        sharedPreferencesLoad();

        databaseInit();
        databaseLoad();
    }
    /*********************** 初始化数据库  **************************/
    private void databaseInit() {
        try {
            File file = new File(DATABASE_PATH);

            if (!file.exists()) {
                file.mkdirs();
            }

            msgDatabase = SQLiteDatabase.openOrCreateDatabase(DATABASE_PATH + MEETING_MSG_DB_NAME, null);
        }catch (Exception e){
            Log.e(TAG,e+"");
        }
    }

    private void databaseLoad(){
        (new Thread(){
            @Override
            public void run() {
                synchronized ("DATABASELOAD"){
                    try {
                        String sql = String.format(" create table if not exists %s_%d(id integer primary key,  %s varchar(255))",ADMIN_MSG_TABLE_NAME,deviceID,DBKEY_MSG_CONTENT);
                        msgDatabase.execSQL(sql);

                        sql = String.format(" create table if not exists %s_%d(id integer primary key,  %s varchar(30), %s varchar(255))",SMS_MSG_TABLE_NAME,deviceID,DBKEY_MSG_ADDRESSER,DBKEY_MSG_CONTENT);
                        msgDatabase.execSQL(sql);

//            sql = String.format(" create table if not exists %s_%d(%s integer(30), %s varchar(30))",USER_LIST_TABLE_NAME,deviceID,DBKEY_USER_LIST_ID,DBKEY_USER_LIST_NAME);
//            msgDatabase.execSQL(sql);
                    } catch (RuntimeException e) {
                        Log.e(TAG, e + "");
                    }

                    String sql = String.format("%s_%d",ADMIN_MSG_TABLE_NAME,deviceID);
                    Cursor cursor = msgDatabase.query(sql, null, null, null, null, null, null);

                    if(adminMsg == null)
                        adminMsg = new ArrayList<String>();
                    else
                        adminMsg.clear();

                    if (cursor.getCount() > 0) {
                        while (cursor.moveToNext()) {
                            adminMsg.add(cursor.getString(1));
                            Log.d(TAG,cursor.getString(1));
                        }
                    }

                    sql = String.format("%s_%d",SMS_MSG_TABLE_NAME,deviceID);
                    cursor = msgDatabase.query(sql, null, null, null, null, null, null);

                    if(smsMsg == null)
                        smsMsg = new ArrayList<String[]>();
                    else
                        smsMsg.clear();

                    if (cursor.getCount() > 0) {
                        while (cursor.moveToNext()) {
                            String[] sms = new String[2];
                            sms[0] = cursor.getString(1);
                            sms[1] = cursor.getString(2);
                            smsMsg.add(sms);
                        }
                    }

                }
            }
        }).start();
    }
    /******************************** 清空信息 ********************************/
    public void delMeetInfo(){
        defMeetInfo();
        delMsg();
        Cmd.execCmd("rm -rf " + DATABASE_PATH + NAMEPLATE_PREF + "*");
        Cmd.execCmd("rm -rf " + DATABASE_PATH + MEETING_PREF + "*");
    }

    public void delMsg(){
        if(adminMsg != null)
            adminMsg.clear();
        if(smsMsg != null)
            smsMsg.clear();

        (new Thread(){
            @Override
            public void run() {
                Cursor cursor = msgDatabase.rawQuery("select name from sqlite_master where type='table' order by name",null);
                while(cursor.moveToNext()){
                    String name = cursor.getString(0);
                    msgDatabase.delete(name,null,null);
                }
            }
        }).start();
    }

    /********************************** 读取参数  ********************************/
    public void defMeetInfo() {
        meetName = "";
        meetSlogan = "";
        meetContent = "";
        meetStartTime = "";
        meetEndTime = "";

        nameplateType = NAMEPLATE_STYLE_DEF;
        backgroundColor = NAMEPLATE_BACKGROUND_COLOR_DEF;
        backgroundImg = "";
        nameplatePic = "";
        for (int i = 0; i < itemNum; i++) {
            strContent[i] = "";
            strColor[i] = NAMEPLATE_STR_COLOR_DEF;
            strStyle[i] = NAMEPLATE_STR_STYLE_DEF;
            strSize[i] = NAMEPLATE_STR_SIZE_DEF;
            strPosX[i] = 0;
            strPosY[i] = i * 150;
        }
    }

    private void sharedPreferencesInit(){
        try {
            Field field = ContextWrapper.class.getDeclaredField("mBase");
            field.setAccessible(true);

            Object obj = field.get(app);
            field = obj.getClass().getDeclaredField("mPreferencesDir");
            field.setAccessible(true);

            File file = new File(DATABASE_PATH);
            field.set(obj,file);
        } catch (NoSuchFieldException e) {
            e.printStackTrace();
        } catch (IllegalAccessException e) {
            e.printStackTrace();
        }
    }

    private void sharedPreferencesLoad(){
        sharedPreferencesLoad(DEVICE_PREF);
        sharedPreferencesLoad(NAMEPLATE_PREF);
        sharedPreferencesLoad(NETWORK_PREF);
        sharedPreferencesLoad(MEETING_PREF);
    }

    private void sharedPreferencesLoad(String opt){
        //加载设备信息
        if (opt.equals(DEVICE_PREF)) {
            spfDevice = app.getSharedPreferences(DEVICE_PREF, Context.MODE_PRIVATE);

            deviceID = spfDevice.getInt(KEY_DEVICE_ID, DEVICE_ID_DEF);
            brightness = spfDevice.getInt(KEY_BRIGHTNESS, DEVICE_BRIGHTNESS_DEF);
            language = spfDevice.getString(KEY_LANGUAGE, DEVICE_LANGUAGE_DEF);
            screenDimTime = spfDevice.getInt(KEY_SCREEN_DIM_TIME,DEVICE_SCREEN_DIM_TIME_DEF);

        }

        //加载铭牌信息
        else if (opt.equals(NAMEPLATE_PREF)) {
            String nameplatePrefName = NAMEPLATE_PREF + "_id_" + deviceID;
            spfNameplate = app.getSharedPreferences(nameplatePrefName, Context.MODE_PRIVATE);

            int temp = spfNameplate.getInt(KEY_ITEM_NUM, NAMEPLATE_ITEM_NUM_DEF);
            itemNum = (temp > 0) && (temp <= NAMEPLATE_ITEM_NUM_MAX) ? temp : NAMEPLATE_ITEM_NUM_DEF;

            strContent = new String[itemNum];
            strColor = new int[itemNum];
            strStyle = new int[itemNum];
            strSize = new int[itemNum];
            strPosX = new float[itemNum];
            strPosY = new float[itemNum];

            for (int i = 0; i < itemNum; i++) {
                strContent[i] = spfNameplate.getString(KEY_STR_CONTENT + i, "");
                strColor[i] = spfNameplate.getInt(KEY_STR_COLOR + i, NAMEPLATE_STR_COLOR_DEF);
                strStyle[i] = spfNameplate.getInt(KEY_STR_STYLE + i, NAMEPLATE_STR_STYLE_DEF);
                strSize[i] = spfNameplate.getInt(KEY_STR_SIZE + i, NAMEPLATE_STR_SIZE_DEF);
                strPosX[i] = spfNameplate.getFloat(KEY_STR_POS_X + i, 0);
                strPosY[i] = spfNameplate.getFloat(KEY_STR_POS_Y + i, i * 150);
            }

            nameplateType = spfNameplate.getInt(KEY_NAMEPLATE_TYPE, NAMEPLATE_STYLE_DEF);
            backgroundColor = spfNameplate.getInt(KEY_BACKGROUND_COLOR, NAMEPLATE_BACKGROUND_COLOR_DEF);
            backgroundImg = spfNameplate.getString(KEY_BACKGROUND_IMG, "");
            nameplatePic = spfNameplate.getString(KEY_NAMEPLATE_PIC, "");
        }

        //加载网络信息
        else if (opt.equals(NETWORK_PREF)) {
            spfNetwork = app.getSharedPreferences(NETWORK_PREF, Context.MODE_PRIVATE);

            localIp = new int[4];
            serverIp = new int[4];
            gateway = new int[4];
            mask = new int[4];

            localIp = General.addrStrToInt(spfNetwork.getString(KEY_LOCAL_IP, LOCAL_IP_DEF));
            serverIp = General.addrStrToInt(spfNetwork.getString(KEY_SERVER_IP, SERVER_IP_DEF));
            gateway = General.addrStrToInt(spfNetwork.getString(KEY_GATEWAY, GATEWAY_DEF));
            mask = General.addrStrToInt(spfNetwork.getString(KEY_NETMASK, NETMASK_DEF));

            serverPort = spfNetwork.getInt(KEY_SERVER_PORT, SERVER_PORT_DEF);
            dhcp = spfNetwork.getBoolean(KEY_DHCP_EN, DHCP_EN_DEF);
            ssid = spfNetwork.getString(KEY_WIFI_SSID, "");
            pwd = spfNetwork.getString(KEY_WIFI_PWD, "");
            wifiType = spfNetwork.getInt(KEY_WIFI_TYPE, WifiManager.UNKNOW);
        }

        //加载会议信息
        else if(opt.equals(MEETING_PREF)){
            spfMeeting = app.getSharedPreferences(MEETING_PREF, Context.MODE_PRIVATE);

            meetingId = spfMeeting.getInt(KEY_MEET_ID, 1);
            meetName = spfMeeting.getString(KEY_MEET_NAME, "");
            meetContent = spfMeeting.getString(KEY_MEET_CONTENT, "");
            meetSlogan = spfMeeting.getString(KEY_MEET_SLOGAN, "");
            meetStartTime = spfMeeting.getString(KEY_MEET_START_TIME, "");
            meetEndTime = spfMeeting.getString(KEY_MEET_END_TIME, "");
        }
    }

    /********************************** 保存参数到系统  ********************************/
    private void save(String opt) {
        if (opt.equals(DEVICE_PREF)) {
            (new Thread(){
                @Override
                public void run() {
                    synchronized (DEVICE_PREF) {
                        SharedPreferences.Editor editor = spfDevice.edit();

                        editor.putInt(KEY_DEVICE_ID, deviceID);
                        editor.putInt(KEY_BRIGHTNESS, brightness);
                        editor.putString(KEY_LANGUAGE, language);
                        editor.putInt(KEY_SCREEN_DIM_TIME, screenDimTime);
                        editor.apply();

                        Debug.d(TAG, "Device preferences save finish!!");
                    }
                }
            }).start();

        } else if (opt.equals(NAMEPLATE_PREF)) {
            (new Thread() {
                @Override
                public void run() {
                    synchronized (NAMEPLATE_PREF) {
                        SharedPreferences.Editor editor = spfNameplate.edit();

                        editor.putInt(KEY_ITEM_NUM, itemNum);
                        try {
                            for (int i = 0; i < itemNum; i++) {
                                editor.putString(KEY_STR_CONTENT + i, strContent[i]);
                                editor.putInt(KEY_STR_COLOR + i, strColor[i]);
                                editor.putInt(KEY_STR_STYLE + i, strStyle[i]);
                                editor.putInt(KEY_STR_SIZE + i, strSize[i]);
                                editor.putFloat(KEY_STR_POS_X + i, strPosX[i]);
                                editor.putFloat(KEY_STR_POS_Y + i, strPosY[i]);
                            }
                        } catch (NullPointerException e) {
                            Debug.d(TAG, "Nameplate preferences save err: " + e);
                        }
                        editor.putInt(KEY_NAMEPLATE_TYPE, nameplateType);
                        editor.putInt(KEY_BACKGROUND_COLOR, backgroundColor);
                        editor.putString(KEY_BACKGROUND_IMG, backgroundImg);
                        editor.putString(KEY_NAMEPLATE_PIC, nameplatePic);
                        editor.apply();

                        Debug.d(TAG, "Nameplate preferences save finish");
                    }
                }
            }).start();

        } else if (opt.equals(NETWORK_PREF)) {
            (new Thread(){
                @Override
                public void run() {
                    synchronized (NETWORK_PREF){
                        SharedPreferences.Editor editor = spfNetwork.edit();

                        editor.putString(KEY_LOCAL_IP, General.addrIntToStr(localIp));
                        editor.putString(KEY_SERVER_IP, General.addrIntToStr(serverIp));
                        editor.putString(KEY_GATEWAY, General.addrIntToStr(gateway));
                        editor.putString(KEY_NETMASK, General.addrIntToStr(mask));
                        editor.putInt(KEY_SERVER_PORT, serverPort);
                        editor.putBoolean(KEY_DHCP_EN, dhcp);
                        editor.putString(KEY_WIFI_SSID, ssid);
                        editor.putString(KEY_WIFI_PWD, pwd);
                        editor.putInt(KEY_WIFI_TYPE, wifiType);

                        editor.apply();
                    }
                }
            }).start();
        }

        else if (opt.equals(MEETING_PREF)) {
            (new Thread(){
                @Override
                public void run() {
                    synchronized (MEETING_PREF){
                        SharedPreferences.Editor editor = spfMeeting.edit();

                        editor.putInt(KEY_MEET_ID, meetingId);
                        editor.putString(KEY_MEET_NAME, meetName);
                        editor.putString(KEY_MEET_CONTENT, meetContent);
                        editor.putString(KEY_MEET_SLOGAN, meetSlogan);
                        editor.putString(KEY_MEET_START_TIME, meetStartTime);
                        editor.putString(KEY_MEET_END_TIME, meetEndTime);

                        editor.apply();
                    }
                }
            }).start();
        }
    }

    public void save() {
        save(NAMEPLATE_PREF);
        save(DEVICE_PREF);
        save(NETWORK_PREF);
        save(MEETING_PREF);
    }

    /********************************** 设置参数  ********************************/
    //======================= 设备参数设置  =======================//
    //设备ID
    public DatabaseManager setDevId(int id) {
        if (id > 0 && id <= DEVICE_ID_MAX)
            deviceID = id;
        else
            deviceID = DEVICE_ID_DEF;
        //save(DEVICE_PREF);

        //根据deviceID保存铭牌信息，因此ID改变后需要重新加载铭牌信息；
//        sharedPreferencesLoad(NAMEPLATE_PREF);
        return this;
    }

    //设置系统语言
    public DatabaseManager setLanguage(String language) {
        this.language = language;
        //save(DEVICE_PREF);
        return this;
    }

    //设置屏幕亮度
    public DatabaseManager setBrightness(int brightness) {
        if (brightness >= 0 && brightness <= 100) {
            this.brightness = brightness;
            //save(DEVICE_PREF);
        }
        return this;
    }

    //设置暗屏时间
    public DatabaseManager setScreenDimTime(int time){
        if(time != 0){
            screenDimTime = time;
            //save(DEVICE_PREF);
        }
        return this;
    }

    //======================= 电子铭牌参数设置  =======================//
    //设置铭牌条目数量
    public DatabaseManager setItemNum(int num) {
        itemNum = num;
        itemRenew();
        //save(NAMEPLATE_PREF);
        return this;
    }

    // 设置铭牌内容
    public DatabaseManager setStr(int type, String str) {
        if (type >= 0 && type < itemNum) {
            itemRenew();
            strContent[type] = str;
            //save(NAMEPLATE_PREF);
        }
        return this;
    }

    public DatabaseManager setStr(String[] str) {
        if (str.length == itemNum) {
            System.arraycopy(str, 0, strContent, 0, itemNum);
            //save(NAMEPLATE_PREF);
        }
        return this;
    }

    //设置字体颜色
    public DatabaseManager setColor(int type, int color) {
        if (type >= 0 && type < itemNum) {
            itemRenew();
            strColor[type] = color;
            //save(NAMEPLATE_PREF);
        }
        return this;
    }

    //设置字体颜色
    public DatabaseManager setColor(int[] color) {
        if (color.length == itemNum) {
            System.arraycopy(color, 0, strColor, 0, itemNum);
            //save(NAMEPLATE_PREF);
        }
        return this;
    }

    //设置字体风格
    public DatabaseManager setStyle(int type, int style) {
        if (type >= 0 && type < itemNum) {
            itemRenew();
            strStyle[type] = style;
            //save(NAMEPLATE_PREF);
        }
        return this;
    }

    //设置字体风格
    public DatabaseManager setStyle(int[] style) {
        if (style.length == itemNum) {
            System.arraycopy(style, 0, strStyle, 0, itemNum);
            //save(NAMEPLATE_PREF);
        }
        return this;
    }

    //设置字体大小
    public DatabaseManager setSize(int type, int size) {
        if (type >= 0 && type <= itemNum) {
            itemRenew();
            strSize[type] = size;
            //save(NAMEPLATE_PREF);
        }
        return this;
    }

    public DatabaseManager setSize(int[] size) {
        if (size.length == itemNum) {
            System.arraycopy(size, 0, strSize, 0, itemNum);
            //save(NAMEPLATE_PREF);
        }
        return this;
    }

    //设置字体位置坐标
    public DatabaseManager setstrPos(int type, float posX, float posY) {
        if (type >= 0 && type <= itemNum) {
            itemRenew();
            strPosX[type] = posX;
            strPosY[type] = posY;
            //save(NAMEPLATE_PREF);
        }
        return this;
    }

    //设置字体位置坐标
    public DatabaseManager setstrPos(float[] posX, float[] posY) {
        if (posX.length == itemNum && posY.length == itemNum) {
            System.arraycopy(posX, 0, strPosX, 0, itemNum);
            System.arraycopy(posY, 0, strPosY, 0, itemNum);
            //save(NAMEPLATE_PREF);
        }
        return this;
    }

    //设置电子铭牌背景色
    public DatabaseManager setNamePlateBGColor(int color) {
        backgroundColor = color;
        ////save();
        return this;
    }

    // 设置电子铭牌类型
    public DatabaseManager setNamePlateType(int type) {
        nameplateType = type;
        //save(NAMEPLATE_PREF);
        return this;
    }

    //设置电子铭牌背景图片路径
    public DatabaseManager setNamePlateBGImg(String path) {
        backgroundImg = path;
        //save(NAMEPLATE_PREF);
        return this;
    }

    //设置电子铭牌图片铭牌路径
    public DatabaseManager setNamePlateImgPath(String path) {
        nameplatePic = path;
        //save(NAMEPLATE_PREF);
        return this;
    }

    //重新检测铭牌条目数量与实际数组长度是否匹配
    private void itemRenew() {
        if (strContent.length != itemNum) {
            String[] s = new String[itemNum];
            System.arraycopy(strContent, 0, s, 0, (strContent.length > itemNum ? itemNum : strContent.length));
            strContent = s;
        }

        if (strColor.length != itemNum) {
            int[] i = new int[itemNum];
            System.arraycopy(strColor, 0, i, 0, (strColor.length > itemNum ? itemNum : strColor.length));
            strColor = i;
        }

        if (strStyle.length != itemNum) {
            int[] i = new int[itemNum];
            System.arraycopy(strStyle, 0, i, 0, (strStyle.length > itemNum ? itemNum : strStyle.length));
            strStyle = i;
        }

        if (strSize.length != itemNum) {
            int[] i = new int[itemNum];
            System.arraycopy(strSize, 0, i, 0, (strSize.length > itemNum ? itemNum : strSize.length));
            strSize = i;
        }

        if (strPosX.length != itemNum) {
            float[] f = new float[itemNum];
            System.arraycopy(strPosX, 0, f, 0, (strPosX.length > itemNum ? itemNum : strPosX.length));
            strPosX = f;
        }

        if (strPosY.length != itemNum) {
            float[] f = new float[itemNum];
            System.arraycopy(strPosY, 0, f, 0, (strPosY.length > itemNum ? itemNum : strPosY.length));
            strPosY = f;
        }
    }

    //======================= 网络参数设置 =======================//
    //设置服务器IP
    public DatabaseManager setServIp(int[] ip) {
        if (ip.length == 4) {
            System.arraycopy(ip, 0, serverIp, 0, 4);
            //save(NETWORK_PREF);
        }
        return this;
    }

    //设置本地IP
    public DatabaseManager setLocalIp(int[] ip) {
        if (ip.length == 4) {
            System.arraycopy(ip, 0, localIp, 0, 4);
            //save(NETWORK_PREF);
        }
        return this;
    }

    //设置服务器端口
    public DatabaseManager setServPort(int port) {
        if (port >= 1 && port < 65536) {
            serverPort = port;
            //save(NETWORK_PREF);
        }
        return this;
    }

    //设置网关
    public DatabaseManager setGateway(int[] gw) {
        if (gw.length == 4) {
            System.arraycopy(gw, 0, gateway, 0, 4);
            //save(NETWORK_PREF);
        }
        return this;
    }

    //设置掩码
    public DatabaseManager setMask(int[] mask) {
        if (mask.length == 4) {
            System.arraycopy(mask, 0, this.mask, 0, 4);
            //save(NETWORK_PREF);
        }
        return this;
    }

    //设置DHCP是否启动
    public DatabaseManager setDhcp(boolean dhcp) {
        this.dhcp = dhcp;
        //save(NETWORK_PREF);
        return this;
    }

    public DatabaseManager setWifiSsid(String ssid) {
        this.ssid = ssid;
        //save(NETWORK_PREF);
        return this;
    }

    public DatabaseManager setWifiPwd(String pwd) {
        this.pwd = pwd;
        //save(NETWORK_PREF);
        return this;
    }

    public DatabaseManager setWifiType(int type) {
        wifiType = type;
        //save(NETWORK_PREF);
        return this;
    }

    //======================= 会议数据设置 =======================//
    //会议ID
    public DatabaseManager setMeetId(int id){
        meetingId = id;
        //save(MEETING_PREF);
        return this;
    }

    //会议名称
    public DatabaseManager setMeetName(String name){
        meetName = name;
        //save(MEETING_PREF);
        return this;
    }

    //会议内容
    public DatabaseManager setMeetContent(String content){
        meetContent = content;
        //save(MEETING_PREF);
        return this;
    }

    //会议标语
    public DatabaseManager setMeetSlogan(String slogan){
        meetSlogan = slogan;
        //save(MEETING_PREF);
        return this;
    }

    //会议开始时间
    public DatabaseManager setMeetStartTime(String startTime){
        meetStartTime = startTime;
        //save(MEETING_PREF);
        return this;
    }

    //会议结束时间
    public DatabaseManager setMeetEndTime(String endTime){
        meetEndTime = endTime;
        //save(MEETING_PREF);
        return this;
    }
    /********************************** 读取参数  ********************************/
    //======================= 设备参数获取  =======================//
    //设备ID
    public int getDeviceID() {
        return deviceID;
    }

    //获取设备显示语言
    public String getLanguage() {
        return language;
    }

    //获取设备亮度
    public int getBrightness() {
        return brightness;
    }

    //获取暗屏时间
    public int getScreenDimTime(){return screenDimTime;}
    //======================= 电子铭牌参数获取  =======================//
    //获取铭牌内容
    public String[] getStr() {
        return strContent.clone();
    }

    // 获取字体颜色
    public int[] getColor() {
        return strColor.clone();
    }

    // 获取字体风格
    public int[] getStyle() {
        return strStyle.clone();
    }

    // 获取字体大小
    public int[] getSize() {
        return strSize.clone();
    }

    // 获取电子铭牌背景色
    public int getNamePlateBGColor() {
        return backgroundColor;
    }

    // 获取电子铭牌类型
    public int getNamePlateType() {
        return nameplateType;
    }

    //获取电子铭牌背景图片路径
    public String getNamePlateBGImg() {
        return backgroundImg;
    }

    //获取电子铭牌图片铭牌路径
    public String getNamePlateImage() {
        return nameplatePic;
    }

    //获取字体位置坐标
    public float[] getPosX() {
        return strPosX.clone();
    }

    //获取字体位置坐标
    public float[] getPosY() {
        return strPosY.clone();
    }

    //======================= 网络参数获取  =======================//
    // 获取服务器IP
    public int[] getServIp() {
        return serverIp.clone();
    }

    // 获取本地IP
    public int[] getLocalIp() {
        return localIp.clone();
    }

    // 获取网关
    public int[] getGateway() {
        return gateway.clone();
    }

    //获取掩码
    public int[] getMask() {
        return mask.clone();
    }

    //获取服务器端口
    public int getServPort() {
        return serverPort;
    }

    //获取DHCP是否启动
    public boolean getDhcp() {
        return dhcp;
    }

    public String getWifiSsid() {
        return ssid;
    }

    public String getWifiPwd() {
        return pwd;
    }

    public int getWifiType() {
        return wifiType;
    }

    //======================= 获取会议参数 =======================//
    //会议ID
    public int getMeetId(){
        return meetingId;
    }

    //会议名称
    public String getMeetName(){
        return meetName;
    }

    //会议内容
    public String getMeetContent(){
        return meetContent;
    }

    //会议标语
    public String getMeetSlogan(){
        return meetSlogan;
    }

    //会议开始时间
    public String getMeetStartTime(){
        return meetStartTime;
    }

    //会议结束时间
    public String getMeetEndTime(){
        return meetEndTime;
    }

    /**************************** 消息（Message）存取 ****************************/
    //======================= 保存消息  =======================//
    public DatabaseManager saveAdminMsg(final String msg){
        (new Thread(){
            @Override
            public void run() {
                ContentValues value = new ContentValues();
                value.put(DBKEY_MSG_CONTENT,msg);

                String sql = String.format("%s_%d",ADMIN_MSG_TABLE_NAME,deviceID);
                msgDatabase.insert(sql,null,value);
            }
        }).start();
        return this;
    }

    public DatabaseManager saveSmsMsg(String addresser,String msg){
        ContentValues value = new ContentValues();
        value.put(DBKEY_MSG_ADDRESSER,addresser);
        value.put(DBKEY_MSG_CONTENT,msg);

        String sql = String.format("%s_%d",SMS_MSG_TABLE_NAME,deviceID);
        msgDatabase.insert(sql,null,value);
        return this;
    }

    //======================= 获取消息  =======================//
    public ArrayList<String> getAdminMsg() {
        if(adminMsg == null)
            adminMsg = new ArrayList<String>();
        return adminMsg;
    }

    public ArrayList<String[]> getSmsMsg(){
        if(smsMsg == null)
            smsMsg = new ArrayList<String[]>();
        return smsMsg;
    }

}
