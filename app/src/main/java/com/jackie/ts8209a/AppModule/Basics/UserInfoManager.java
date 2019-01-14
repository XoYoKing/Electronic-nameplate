package com.jackie.ts8209a.AppModule.Basics;

import android.content.Context;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.util.Log;

import com.jackie.ts8209a.AppModule.APP;
import com.jackie.ts8209a.AppModule.Tools.Printf;

/**
 * Created by kuangyt on 2018/8/27.
 */

public class UserInfoManager {
    private final String TAG = this.getClass().getSimpleName();
    /************* 存取类型类型静态变量  **************/
    public static final int USER = 0;
    public static final int COMP = 1;
    public static final int POS = 2;

    /************* 用户信息保存键(key word)  **************/
    private final String USER_INFO_PATH = "com.jackie.userinfo";

    private final String[] keyNameStr = {"USERNAME", "COMPANY", "POSITION"};
    private final String[] keyFontColor = {"USNAMECOLOR", "COMPCOLOR","POSCOLOR"};
    private final String[] keyFontStyle = {"USNAMEFONTSTYLE", "COMPFONTSTYLE","POSFONTSTYLE"};
    private final String[] keyFontSize = {"USNAMEFONTSIZE", "COMPFONTSIZE","POSFONTSIZE"};
    private final String[] keyServerIp = {"SERVERIP_1", "SERVERIP_2","SERVERIP_3", "SERVERIP_4"};
    private final String[] keyLocalIp = {"GATEWAY_1", "GATEWAY_2","GATEWAY_3", "GATEWAY_4"};
    private final String[] keyGateway = {"LOCALIP_1", "LOCALIP_2","LOCALIP_3", "LOCALIP_4"};
    private final String[] keyMask = {"MASK_1", "MASK_2","MASK_3", "MASK_4"};
    private final String[] keyFontPosX = {"USNAMEFONTPOSX", "COMPFONTPOSX", "POSFONTPOSX"};
    private final String[] keyFontPosY = {"USNAMEFONTPOSY", "COMPFONTPOSY", "POSFONTPOSY"};
    private final String keyServerPort = "SERVERPORT";
    private final String keyNamePlateBGColor = "NAMEPLATEBGCOLOR";
    private final String keyNamePlateType = "NAMEPLATETYPE";
    private final String keyNamePlateBGImg = "NAMEPLATEBGIMG";
    private final String keyNamePlateImage = "NAMEPLATEIMAGE";
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

    private static APP App;
    private static SharedPreferences userInfo = null;
    private static SharedPreferences.Editor editor = null;
    private static boolean saving = false;

    /**************** 用户信息参数变量  *******************/
    //用户及电子铭牌相关参数
    private String[] strInfo = new String[3];
    private int[] fontColor = new int[3];
    private int[] fontStyle = new int[3];
    private int[] fontSize = new int[3];
    private float[] fontPosX = new float[3];
    private float[] fontPosY = new float[3];
    private int NamePlateBGColor = Color.RED;
    private int NamePlateType = 0;
    private String NamePlateBGImgPath = "";
    private String NamePlateImagePath = "";

    //网络配置相关参数
    private int[] localIp = {192,168,1,100};
    private int[] serverIp = {192,168,1,120};
    private int[] gateway = {192,168,1,1};
    private int[] mask = {255,255,255,0};
    private int serverPort = 8000;
    private boolean dhcp = false;
    private String ssid = "";
    private String pwd = "";
    private int wifiType = 3;

    //设备配置相关参数
    private String language = "zh";
    private int brightness = 50;
    private int deviceID = 1;


    private UserInfoManager() {
    }


    public static UserInfoManager getUserInfoManager() {
        return UserInfo;
    }

    public void init(APP App) {
        UserInfoManager.App = App;
        userInfo = UserInfoManager.App.getSharedPreferences(USER_INFO_PATH, Context.MODE_PRIVATE);
        editor = userInfo.edit();

        for (int i = 0; i < 3; i++) {
            strInfo[i] = userInfo.getString(keyNameStr[i], null);
            fontColor[i] = userInfo.getInt(keyFontColor[i], Color.YELLOW);
            fontStyle[i] = userInfo.getInt(keyFontStyle[i], 1);
            fontSize[i] = userInfo.getInt(keyFontSize[i], 50);
            fontPosX[i] = userInfo.getFloat(keyFontPosX[i], 0);
            fontPosY[i] = userInfo.getFloat(keyFontPosY[i], 0);
        }
        for (int i = 0; i < 4; i++) {
            serverIp[i] = userInfo.getInt(keyServerIp[i], serverIp[i]);
            localIp[i] = userInfo.getInt(keyLocalIp[i], localIp[i]);
            gateway[i] = userInfo.getInt(keyGateway[i], gateway[i]);
            mask[i] = userInfo.getInt(keyMask[i], mask[i]);
        }
        serverPort = userInfo.getInt(keyServerPort, 8000);
        NamePlateBGColor = userInfo.getInt(keyNamePlateBGColor, Color.RED);
        dhcp = userInfo.getBoolean(keyDhcp, true);
        language = userInfo.getString(keyLanguage, "zh");
        brightness = userInfo.getInt(keyBrightness, 50);
        ssid = userInfo.getString(keyWifiSsid, "");
        pwd = userInfo.getString(keyWifiPwd, "");
        wifiType = userInfo.getInt(keyWifiType, 3);
        deviceID = userInfo.getInt(keyDeviceID, 1);
        NamePlateType = userInfo.getInt(keyNamePlateType, 0);
        NamePlateBGImgPath = userInfo.getString(keyNamePlateBGImg, "");
        NamePlateImagePath = userInfo.getString(keyNamePlateImage, "");


        editor.putString("TS8209A", "TS8209A");
        editor.commit();
    }

    /********************************** 保存参数到系统方法 ********************************/
    private void save() {
        if (!saving) {
            saving = true;

            new Thread() {
                @Override
                public void run() {
                    synchronized (this) {
                        if (editor == null) return;
                        try {
                            Thread.sleep(1000);
                            for (int i = 0; i < 3; i++) {
                                editor.putString(keyNameStr[i], strInfo[i]);
                                editor.putInt(keyFontColor[i], fontColor[i]);
                                editor.putInt(keyFontStyle[i], fontStyle[i]);
                                editor.putInt(keyFontSize[i], fontSize[i]);
                                editor.putFloat(keyFontPosX[i], fontPosX[i]);
                                editor.putFloat(keyFontPosY[i], fontPosY[i]);
                            }
                            for (int i = 0; i < 4; i++) {
                                editor.putInt(keyServerIp[i], serverIp[i]);
                                editor.putInt(keyLocalIp[i], localIp[i]);
                                editor.putInt(keyGateway[i], gateway[i]);
                                editor.putInt(keyMask[i], mask[i]);
                            }
                            editor.putInt(keyServerPort, serverPort);
                            editor.putInt(keyNamePlateBGColor, NamePlateBGColor);
                            editor.putBoolean(keyDhcp, dhcp);
                            editor.putString(keyLanguage, language);
                            editor.putInt(keyBrightness, brightness);
                            editor.putString(keyWifiSsid, ssid);
                            editor.putString(keyWifiPwd, pwd);
                            editor.putInt(keyWifiType, wifiType);
                            editor.putInt(keyDeviceID, deviceID);
                            editor.putInt(keyNamePlateType, NamePlateType);
                            editor.putString(keyNamePlateBGImg, NamePlateBGImgPath);
                            editor.putString(keyNamePlateImage, NamePlateImagePath);

                            editor.commit();

//						PromptBox.BuildPrompt("SAVE_SUCCESS").Text((String)App.getApplicationContext().getResources().getText(R.string.save_successfully)).Time(1).TimeOut(3000);
                        } catch (Exception e) {
                            e.printStackTrace();
                        }

                        saving = false;
//                        Log.d(TAG, "User info has been saved");
                        Printf.d(TAG,"User info has been saved");
                    }
                }
            }.start();
        }
    }

    /********************************** 设置参数方法 ********************************/
    //设备ID
    public UserInfoManager setDeviceId(int id) {
        if (id > 0 && id <= 1000) {
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

    //设置字体颜色
    public UserInfoManager setColor(int type, int color) {
        if (type >= 0 && type <= 2) {
            fontColor[type] = color;
            save();
        }
        return UserInfo;
    }

    //设置字体颜色
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
    public UserInfoManager setNamePlateBGColor(int color) {
        NamePlateBGColor = color;
        save();
        return UserInfo;
    }

    // 设置电子铭牌类型
    public UserInfoManager setNamePlateType(int type) {
        NamePlateType = type;
        save();
        return UserInfo;
    }

    //设置电子铭牌背景图片路径
    public UserInfoManager setNamePlateBGImg(String path) {
        NamePlateBGImgPath = path;
        save();
        return UserInfo;
    }

    //设置电子铭牌图片铭牌路径
    public UserInfoManager setNamePlateImage(String path) {
        NamePlateImagePath = path;
        save();
        return UserInfo;
    }

    //设置字体风格
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

    //设置字体大小
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

    //设置字体位置坐标
    public UserInfoManager setFontPosition(int type, float posX, float posY) {
        if (type >= 0 && type <= 2) {
            fontPosX[type] = posX;
            fontPosY[type] = posY;
            save();
        }
        return UserInfo;
    }

    public UserInfoManager setFontPosition(float[] posX, float[] posY) {
        if (posX.length == 3 && posY.length == 3) {
            for (int i = 0; i < 3; i++) {
                fontPosX[i] = posX[i];
                fontPosY[i] = posY[i];
            }
            save();
        }
        return UserInfo;
    }

    //设置服务器IP
    public UserInfoManager setServIp(int[] ip) {
        if (ip.length == 4) {
            for (int i = 0; i < 4; i++) {
                serverIp[i] = ip[i];
            }
            save();
        }
        return UserInfo;
    }

    //设置本地IP
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
    public UserInfoManager setServPort(int port) {
        if (port >= 1 && port <= 65535) {
            serverPort = port;
            save();
        }
        return UserInfo;
    }

    //设置DHCP是否启动
    public UserInfoManager setDhcp(boolean dhcp) {
        this.dhcp = dhcp;
        save();
        return UserInfo;
    }

    //设置系统语言
    public UserInfoManager setLanguage(String language) {
        this.language = language;
        save();
        return UserInfo;
    }

    //设置屏幕亮度
    public UserInfoManager setBrightness(int brightness) {
        this.brightness = brightness;
        save();
        return UserInfo;
    }

    public UserInfoManager setWifiSsid(String ssid) {
        this.ssid = ssid;
        save();
        return UserInfo;
    }

    public UserInfoManager setWifiPwd(String pwd) {
        this.pwd = pwd;
        save();
        return UserInfo;
    }

    public UserInfoManager setWifiType(int type) {
        wifiType = type;
        save();
        return UserInfo;
    }


    public UserInfoManager setGateway(int[] gw){
        if (gw.length == 4) {
            for (int i = 0; i < 4; i++) {
                gateway[i] = gw[i];
            }
            save();
        }
        return UserInfo;
    }

    public UserInfoManager setMask(int[] mask){
        if (mask.length == 4) {
            for (int i = 0; i < 4; i++) {
                this.mask[i] = mask[i];
            }
            save();
        }
        return UserInfo;
    }

    /********************************** 读取参数方法 ********************************/
    //设备ID
    public int getDeviceID() {
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
        for (int i = 0; i < 3; i++) {
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
        for (int i = 0; i < 3; i++) {
            color[i] = fontColor[i];
        }
        return color;
    }

    // 获取电子铭牌背景色
    public int getNamePlateBGColor() {
        return NamePlateBGColor;
    }

    // 获取电子铭牌类型
    public int getNamePlateType() {
        return NamePlateType;
    }

    //获取电子铭牌背景图片路径
    public String getNamePlateBGImg() {
        return NamePlateBGImgPath;
    }

    //获取电子铭牌图片铭牌路径
    public String getNamePlateImage() {
        return NamePlateImagePath;
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
        for (int i = 0; i < 3; i++) {
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
        for (int i = 0; i < 3; i++) {
            size[i] = fontSize[i];
        }
        return size;
    }

    //获取字体位置坐标
    public float[] getPosX() {
        float[] po = new float[3];
        for (int i = 0; i < 3; i++) {
            po[i] = fontPosX[i];
        }
        return po;
    }

    public float[] getPosY() {
        float[] po = new float[3];
        for (int i = 0; i < 3; i++) {
            po[i] = fontPosY[i];
        }
        return po;
    }

    // 获取服务器IP
    public int[] getServIp() {
        int[] ip = new int[4];
        for (int i = 0; i < 4; i++) {
            ip[i] = serverIp[i];
        }
        return ip;
    }

    // 获取本地IP
    public int[] getLocalIp() {
        int[] ip = new int[4];
        for (int i = 0; i < 4; i++) {
            ip[i] = localIp[i];
        }
        return ip;
    }

    //获取服务器端口
    public int getServPort() {
        return serverPort;
    }

    //获取DHCP是否启动
    public boolean getDhcp() {
        return dhcp;
    }

    //获取系统语言
    public String getLanguage() {
        return language;
    }

    public int getBrightness() {
        return brightness;
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

    // 获取网关
    public int[] getGateway() {
        int[] gw = new int[4];
        for (int i = 0; i < 4; i++) {
            gw[i] = gateway[i];
        }
        return gw;
    }

    //获取掩码
    public int[] getMask() {
        int[] mask = new int[4];
        for (int i = 0; i < 4; i++) {
            mask[i] = this.mask[i];
        }
        return mask;
    }
}
