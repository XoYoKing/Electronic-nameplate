package com.itc.ts8209a.app;

import android.graphics.Color;

import com.itc.ts8209a.module.font.FontManager;
import com.itc.ts8209a.module.nameplate.NameplateManager;

/**
 * Created by kuangyt on 2019/1/14.
 */

public class AppConfig {

    /***************** 设备及应用参数 ******************/
    //应用名称
    public static final String APP_NAME = "com.itc.ts8209a";

    //默认设备ID
    public static final int DEVICE_ID_DEF = 1;

    //设备ID最大值
    public static final int DEVICE_ID_MAX = 999;

    //默认设备语言
    public static final String DEVICE_LANGUAGE_DEF = "zh";

    //默认设备亮度
    public static final int DEVICE_BRIGHTNESS_DEF = 75;

    //默认设备暗屏时间
    public static final int DEVICE_SCREEN_DIM_TIME_DEF = 60;

    //暗屏时间数组15秒、60秒、5分钟、30分钟、1小时、永不
    public static final int[] DIM_SCREEN_TIME = {15,60,300,1800,3600,Integer.MAX_VALUE};

    //设备数据保存根目录
    public static final String DEVICE_ROOT = "/storage/sdcard0/ts8209a/";

    //管理员密码
    public static final String ADMIN_PASSWORD = "10241024";

    //数据库保存路径
    public static final String DATABASE_ROOT = DEVICE_ROOT + "database/";
    /******************** 会议相关参数 ******************/
    //会议消息记录数据库文件名
    public static final String MEETING_MSG_DB_NAME = "meetingMsg.db";

    //管理员消息保存数据表名
    public static final String ADMIN_MSG_TABLE_NAME= "adminMsg";

    //短消息保存数数据表名
    public static final String SMS_MSG_TABLE_NAME = "smsMsg";

    //用户列表保存数数据表名
    public static final String USER_LIST_TABLE_NAME = "userList";

    /***************** 电子铭牌相关参数 ******************/
    //电子铭牌根目录
    public static final String NAMEPLATE_ROOT = DEVICE_ROOT + "nameplate/";

    //图片铭牌保存目录
    public static final String NAMEPLATE_IMG_PATH = NAMEPLATE_ROOT + "img/";

    //铭牌背景图片保存目录
    public static final String NAMEPLATE_BACKGROUND_PATH = NAMEPLATE_ROOT + "background/";

    //生成铭牌二进制数据保存目录
    public static final String NAMEPLATE_BIN_FILE_PATH = NAMEPLATE_ROOT + "bin/";

    //生成铭牌二进制数据保存文件名
    public static final String NAMEPLATE_BIN_FILE_NAME = "nameplate.bin";

    //电子铭牌预设图片和电子铭牌背景图片最大保存数量
    public static final int NAMEPLATE_FILE_NUM_MAX = 10;

    //铭牌条目最大数量
    public static final int NAMEPLATE_ITEM_NUM_MAX = 5;

    //铭牌条目默认数量
    public static final int NAMEPLATE_ITEM_NUM_DEF = 3;

    //铭牌默认类型
    public static final int NAMEPLATE_STYLE_DEF = NameplateManager.TYPE_CUSTOM_COLOR;

    //铭牌文字内容默认颜色
    public static final int NAMEPLATE_BACKGROUND_COLOR_DEF = Color.RED;

    //铭牌文字内容默认颜色
    public static final int NAMEPLATE_STR_COLOR_DEF = Color.YELLOW;

    //铭牌文字内容默认字体
    public static final int NAMEPLATE_STR_STYLE_DEF = FontManager.YAHEI;

    //铭牌文字内容默认字体大小
    public static final int NAMEPLATE_STR_SIZE_DEF = 36;

    /***************** 电池电量相关参数 ******************/
    //设置定时获取电池状态时间(单位:秒)
    public static final int GET_BAT_INFO_TIME = 60;

    //进入低功耗模式无操作时间(单位:秒)    10分钟进入低功耗模式
    public static final int ENTER_SAVE_POWER_TIME = 10 * 60;

    //退出低功耗模式时间(单位:秒)
    //进入低功耗后50分钟退出（进入低功耗后设备性能可能无法处理系统或JVM等后台任务，长时间处于低功耗模式会导致设备报错或严重卡顿）
    public static final int EXIT_SAVE_POWER_TIME = 50 * 60;

    /***************** 网络相关参数 ******************/
    //获取网络状态信息时间(单位:毫秒)
    public static final int GET_NETWOR_STA_TIME = 15 * 1000;//10S

    //协议头
    public static final String NET_PROTOCOL_HEAD = "ITCL";

    //服务器文字编码
    public static final String SERV_ENCODING = "GBK";

    //默认本地IP地址
    public static final String LOCAL_IP_DEF = "192.168.1.2";

    //默认服务器IP地址
    public static final String SERVER_IP_DEF = "192.168.1.100";

    //默认服务器端口
    public static final int SERVER_PORT_DEF = 2340;

    //默认网关
    public static final String GATEWAY_DEF = "192.168.1.1";

    //默认掩码
    public static final String NETMASK_DEF = "255.255.255.0";

    //默认是否使能DHCP
    public static final boolean DHCP_EN_DEF = true;

    //Socket重连时间（单位S）
    public static final int RESTART_NET_TIME = 5; //(Second)

    //HTTP请求时间（单位S）
    public static final int HTTP_REQUEST_INTERVAL = 15;

    //HTTP读取时间（单位S）
    public static final int HTTP_READ_TIMEOUT = 30;

    //HTTP最大请求次数
    public static final int HTTP_REQUEST_TIMES = 10;

    //发送心跳间隔（单位S）
    public static final int SEND_HARTBEAT_TIME = 15;

    //心跳超时
    public static final int REC_HARTBEAT_TIMEOUT = SEND_HARTBEAT_TIME * 3;

    //设备重复注册时间（单位S）
    public static final int DEV_REGISTE_TIME = SEND_HARTBEAT_TIME; //(Second)

    /***************** 打印信息相关参数 ******************/
    //Debug功能是否使用Log打印功能
    public static final boolean DEBUG_LOG_EN = true;

    //Debug功能是否使用串口打印功能
    public static final boolean DEBUG_UART_EN = true;

    //软件Log信息保存目录
    public static final String LOG_CRASH_FILE_PATH = DEVICE_ROOT + "log/crash/";

}
