package com.jackie.ts8209a.Managers;

import android.content.Context;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.util.Log;

import com.jackie.ts8209a.Application.App;

/**
 * Created by kuangyt on 2018/8/27.
 */

public class UserInfoManager {
    /************* 存取类型类型静态变量  **************/
    public static final int USER = 0;
    public static final int COMP = 1;
    public static final int POS = 2;

    /************* 用户信息保存键(key word)  **************/
    private final String USER_INFO_PATH = "com.jackie.userinfo";

    private final String[] keyNameStr = { "USERNAME", "COMPANY", "POSITION" };
    private final String[] keyFontColor = { "USNAMECOLOR", "COMPCOLOR",
            "POSCOLOR" };
    private final String[] keyFontStyle = { "USNAMEFONTSTYLE", "COMPFONTSTYLE",
            "POSFONTSTYLE" };
    private final String[] keyFontSize = { "USNAMEFONTSIZE", "COMPFONTSIZE",
            "POSFONTSIZE" };

    private final String[] keyServerIp = { "SERVERIP_1", "SERVERIP_2",
            "SERVERIP_3", "SERVERIP_4" };
    private final String[] keyLocalIp = { "LOCALIP_1", "LOCALIP_2",
            "LOCALIP_3", "LOCALIP_4" };

    private final String keyServerPort = "SERVERPORT";
    private final String keyNamePlateBGColor = "NAMEPLATEBGCOLOR";
    private final String keyDhcp = "DHCP";
    private final String keyLanguage = "LANGUAGE";
    private final String keyBrightness = "BRIGHTNESS";
    private final String keyWifiSsid = "WIFISSID";
    private final String keyWifiPwd = "WIFIPWD";
    private final String keyWifiType = "WIFITYPE";
    private final String keyDeviceID = "DEVICEID";

    /******************** 关键静态变量  **********************/
    //类唯一实现静态对象
    private static UserInfoManager UserInfo = new UserInfoManager();

    private static App app;
    private static SharedPreferences userInfo = null;
    private static SharedPreferences.Editor editor = null;
    private static boolean saving = false;

    /**************** 用户信息参数变量  *******************/
    private String[] strInfo = new String[3];
    private int[] fontColor = new int[3];
    private int[] fontStyle = new int[3];
    private int[] fontSize = new int[3];
    private int[] localIp = { 192, 168, 1, 1 };
    private int[] serverIp = { 192, 168, 1, 100 };
    private int serverPort = 8000;
    private boolean dhcp = false;
    private int NamePlateBGColor = Color.RED;
    private String language = "zh";
    private int brightness = 50;
    private String ssid = "";
    private String pwd = "";
    private int wifiType = 3;
    private int deviceID = 1;


    private UserInfoManager() {
    }


    public static UserInfoManager getUserInfoManager() {
        return UserInfo;
    }

    public void init(App app) {
        UserInfoManager.app = app;
        userInfo = UserInfoManager.app.getSharedPreferences(USER_INFO_PATH, Context.MODE_PRIVATE);
        editor = userInfo.edit();

        for (int i = 0; i < 3; i++) {
            strInfo[i] = userInfo.getString(keyNameStr[i], null);
            fontColor[i] = userInfo.getInt(keyFontColor[i], Color.YELLOW);
            fontStyle[i] = userInfo.getInt(keyFontStyle[i], 1);
            fontSize[i] = userInfo.getInt(keyFontSize[i], 50);
        }
        for (int i = 0; i < 4; i++) {
            serverIp[i] = userInfo.getInt(keyServerIp[i], serverIp[i]);
            localIp[i] = userInfo.getInt(keyLocalIp[i], localIp[i]);
        }
        serverPort = userInfo.getInt(keyServerPort, 8000);
        NamePlateBGColor = userInfo.getInt(keyNamePlateBGColor, Color.RED);
        dhcp = userInfo.getBoolean(keyDhcp, false);
        language = userInfo.getString(keyLanguage, "zh");
        brightness = userInfo.getInt(keyBrightness, 50);
        ssid = userInfo.getString(keyWifiSsid,"");
        pwd = userInfo.getString(keyWifiPwd,"");
        wifiType= userInfo.getInt(keyWifiType,3);
        deviceID = userInfo.getInt(keyDeviceID,1);


        editor.putString("TS8209A", "TS8209A");
        editor.commit();
    }

    /********************************** 保存参数到系统方法 ********************************/
    private void save(){
        if(saving == false){
            saving = true;

            new Thread(){
                @Override
                public void run() {
                    if(editor == null) return;
                    try{
                        Thread.sleep(1000);
                        for (int i = 0; i < 3; i++) {
                            editor.putString(keyNameStr[i], strInfo[i]);
                            editor.putInt(keyFontColor[i], fontColor[i]);
                            editor.putInt(keyFontStyle[i], fontStyle[i]);
                            editor.putInt(keyFontSize[i], fontSize[i]);
                        }
                        for (int i = 0; i < 4; i++) {
                            editor.putInt(keyServerIp[i], serverIp[i]);
                            editor.putInt(keyLocalIp[i], localIp[i]);
                        }
                        editor.putInt(keyServerPort,serverPort);
                        editor.putInt(keyNamePlateBGColor,NamePlateBGColor);
                        editor.putBoolean(keyDhcp, dhcp);
                        editor.putString(keyLanguage, language);
                        editor.putInt(keyBrightness, brightness);
                        editor.putString(keyWifiSsid,ssid);
                        editor.putString(keyWifiPwd,pwd);
                        editor.putInt(keyWifiType,wifiType);
                        editor.putInt(keyDeviceID,deviceID);

                        editor.commit();

//						PromptBox.BuildPrompt("SAVE_SUCCESS").Text((String)app.getApplicationContext().getResources().getText(R.string.save_successfully)).Time(1).TimeOut(3000);
                    }catch(Exception e){}

                    saving = false;
                    Log.d("UserInfoManager","User info has been saved");
                }
            }.start();
        }
    }

    /********************************** 设置参数方法 ********************************/
    //设备ID
    public UserInfoManager setDeviceId(int id){
        if(id > 0 && id <= 1000 ) {
            deviceID = id;
            save();
        }
        return this;
    }

    // 设置用户姓名、公司名称、用户职位
    public UserInfoManager setStr(int type, String str) {
        if (type >= 0 && type <= 2) {
            strInfo[type] = str;
            save();
        }
        return UserInfo;
    }

    public UserInfoManager setStr(String[] str) {
        if (str.length == 3) {
            for (int i = 0; i < 3; i++) {
                strInfo[i] = str[i];
            }
            save();
        }
        return UserInfo;
    }

    // 设置字体颜色
    public UserInfoManager setColor(int type, int color) {
        if (type >= 0 && type <= 2) {
            fontColor[type] = color;
            save();
        }
        return UserInfo;
    }

    public UserInfoManager setColor(int[] color) {
        if (color.length == 3) {
            for (int i = 0; i < 3; i++) {
                fontColor[i] = color[i];
            }
            save();
        }
        return UserInfo;
    }

    //设置电子铭牌背景色
    public UserInfoManager setNamePlateBGColor(int color){
        NamePlateBGColor = color;
        save();
        return UserInfo;
    }

    // 设置字体风格
    public UserInfoManager setStyle(int type, int style) {
        if (type >= 0 && type <= 2) {
            fontStyle[type] = style;
            save();
        }
        return UserInfo;
    }

    public UserInfoManager setStyle(int[] style) {
        if (style.length == 3) {
            for (int i = 0; i < 3; i++) {
                fontStyle[i] = style[i];
            }
            save();
        }
        return UserInfo;
    }

    // 设置字体大小
    public UserInfoManager setSize(int type, int size) {
        if (type >= 0 && type <= 2) {
            fontSize[type] = size;
            save();
        }
        return UserInfo;
    }

    public UserInfoManager setSize(int[] size) {
        if (size.length == 3) {
            for (int i = 0; i < 3; i++) {
                fontSize[i] = size[i];
            }
            save();
        }
        return UserInfo;
    }

    // 设置服务器IP
    public UserInfoManager setServIp(int[] ip) {
        if (ip.length == 4) {
            for (int i = 0; i < 4; i++) {
                serverIp[i] = ip[i];
            }
            save();
        }
        return UserInfo;
    }

    // 设置本地IP
    public UserInfoManager setLocalIp(int[] ip) {
        if (ip.length == 4) {
            for (int i = 0; i < 4; i++) {
                localIp[i] = ip[i];
            }
            save();
        }
        return UserInfo;
    }

    //设置服务器端口
    public UserInfoManager setServPort(int port){
        if(port >= 1 && port <= 65535){
            serverPort = port;
            save();
        }
        return UserInfo;
    }

    //设置DHCP是否启动
    public UserInfoManager setDhcp(boolean dhcp){
        this.dhcp = dhcp;
        save();
        return UserInfo;
    }

    //设置系统语言
    public UserInfoManager setLanguage(String language){
        this.language = language;
        save();
        return UserInfo;
    }

    public UserInfoManager setBrightness(int brightness){
        this.brightness = brightness;
        save();
        return UserInfo;
    }

    public UserInfoManager setWifiSsid(String ssid){
        this.ssid = ssid;
        save();
        return UserInfo;
    }

    public UserInfoManager setWifiPwd(String pwd){
        this.pwd = pwd;
        save();
        return UserInfo;
    }

    public UserInfoManager setWifiType(int type){
        wifiType = type;
        save();
        return UserInfo;
    }
    /********************************** 读取参数方法 ********************************/
    //设备ID
    public int getDeviceID(){
        return deviceID;
    }
    //获取用户姓名、公司名称、用户职位
    public String getStr(int type) {
        if (type >= 0 && type <= 2) {
            return strInfo[type];
        } else
            return "";
    }

    public String[] getStr() {
        String[] str = new String[3];
        for(int i=0;i<3;i++){
            str[i] = strInfo[i];
        }
        return str;
    }

    // 获取字体颜色
    public int getColor(int type) {
        if (type >= 0 && type <= 2) {
            return fontColor[type];
        } else
            return Color.WHITE;
    }

    public int[] getColor() {
        int[] color = new int[3];
        for(int i=0;i<3;i++){
            color[i] = fontColor[i];
        }
        return color;
    }

    // 获取电子铭牌背景色
    public int getNamePlateBGColor() {
        return NamePlateBGColor;
    }

    // 获取字体风格
    public int getStyle(int type) {
        if (type >= 0 && type <= 2) {
            return fontStyle[type];
        } else
            return 1;
    }

    public int[] getStyle() {
        int[] style = new int[3];
        for(int i=0;i<3;i++){
            style[i] = fontStyle[i];
        }
        return style;
    }

    // 获取字体大小
    public int getSize(int type) {
        if (type >= 0 && type <= 2) {
            return fontSize[type];
        } else
            return 50;
    }

    public int[] getSize() {
        int[] size = new int[3];
        for(int i=0;i<3;i++){
            size[i] = fontSize[i];
        }
        return size;
    }

    // 获取服务器IP
    public int[] getServIp() {
        int[] ip = new int[4];
        for(int i=0;i<4;i++){
            ip[i] = serverIp[i];
        }
        return ip;
    }

    // 获取本地IP
    public int[] getLocalIp() {
        int[] ip = new int[4];
        for(int i=0;i<4;i++){
            ip[i] = localIp[i];
        }
        return ip;
    }

    //获取服务器端口
    public int getServPort(){
        return serverPort;
    }

    //获取DHCP是否启动
    public boolean getDhcp(){
        return dhcp;
    }

    //获取系统语言
    public String getLanguage() {
        return language;
    }

    public int getBrightness(){
        return brightness;
    }

    public String getWifiSsid(){
        return ssid;
    }

    public String getWifiPwd(){
        return pwd;
    }

    public int getWifiType(){
        return wifiType;
    }
}
