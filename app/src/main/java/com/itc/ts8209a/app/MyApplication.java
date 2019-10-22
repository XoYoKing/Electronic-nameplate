package com.itc.ts8209a.app;

import android.app.Activity;
import android.app.ActivityManager;
import android.app.Application;
import android.content.Context;
import android.content.Intent;
import android.content.pm.ActivityInfo;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.support.v4.content.LocalBroadcastManager;
import android.text.TextUtils;
import android.text.format.Time;
import android.view.WindowManager;
import android.widget.Toast;

import com.itc.ts8209a.activity.MainActivity;
import com.itc.ts8209a.module.database.DatabaseManager;
import com.itc.ts8209a.module.nameplate.NameplateManager;
import com.itc.ts8209a.module.power.PowerManager;
import com.itc.ts8209a.module.font.FontManager;
import com.itc.ts8209a.module.network.NetworkManager;
import com.itc.ts8209a.widget.Cmd;
import com.itc.ts8209a.widget.CrashHandler;
import com.itc.ts8209a.widget.Debug;

import java.io.File;
import java.util.List;
import java.util.Timer;
import java.util.TimerTask;

import static com.itc.ts8209a.app.AppConfig.*;

/**
 * Created by kuangyt on 2018/8/20.
 */

public class MyApplication extends Application {

    private static final String TAG = "MyApplication";
    private static final String PROCESS_NAME = APP_NAME;

    //软件本地广播标志 Broadcast static value(BSV)
    public static final String ACTION_MAIN = APP_NAME + ".localbroadcast";
    public static final String ACTION_STAR_ACTIVITY = ACTION_MAIN + ".activity.start";
    public static final String ACTION_FINISH_ACTIVITY = ACTION_MAIN + ".activity.finish";
    public static final String ACTION_REFRESH_ACTIVITY = ACTION_MAIN + ".activity.refresh";
    public static final String ACTION_REFRESH_STABAR = ACTION_MAIN + ".stabar.refresh";
    public static final String ACTION_NETWORK_INFO_UPDATE = ACTION_MAIN + ".network.infoupdate";
    public static final String ACTION_POWER_INFO_UPDATE = ACTION_MAIN + ".power.infoupdate";
    public static final String ACTION_NAMEPLATE_UPDATE = ACTION_MAIN + ".nameplate.update";
    public static final String ACTION_HARDFAULT_REBOOT = ACTION_MAIN + ".reboot";

    public static Activity currentActivity;

    @Override
    public void onCreate() {
        super.onCreate();

        if (isAppMainProcess()) {
            // 完全隐藏系统状态栏以及底部导航键
            sendBroadcast(new Intent("com.android.action.hide_statusbar"));
            sendBroadcast(new Intent("com.android.action.hide_navigationbar"));

            //初始化APP本地广播功能
            LocalBroadcast.init(this);

            //初始化设备用户信息
            databaseInit();

            //初始化电源管理模块
            PowerManager.getPowerManager().init(this);

            //初始化网络管理模块
            NetworkManager.getNetworkManager().init(this);

            //初始化字体管理模块
            FontManager.getFontManager().init();

            //初始化电子铭牌管理模块
            NameplateManager.getNameplateManager().init(this);
            nameplateParaInit();

            //屏幕背光初始化
            screenLightInit();

            //应用文件初始化
            folderInit();

            //自动重启
//            rebootAlarm();

            //页面监控
            Monitor.pageTableMonitor();
            Monitor.procMonitor();
        }

        //初始化串口debug
        Debug.init();

        CrashHandler crashHandler = CrashHandler.getInstance();
        crashHandler.init(getApplicationContext());
    }

    public static void setCurrentActivity(Activity activity){
        currentActivity = activity;
    }

    private void rebootAlarm(){
        Timer alarm = new Timer();
        alarm.schedule(new TimerTask() {
            Time time = new Time();
            @Override
            public void run() {
                time.setToNow();
                if(time.hour == 3 && time.minute == 0 && time.year > 2000)
                    Cmd.execCmd("reboot");
            }
        }, 60 * 1000,60 * 1000);
    }

    private void nameplateParaInit(){
        NameplateManager nameplateManager = NameplateManager.getNameplateManager();
        DatabaseManager databaseManager = DatabaseManager.getDatabaseManager();

        String str[] = databaseManager.getStr();
        int color[] = databaseManager.getColor();
        int style[] = databaseManager.getStyle();
        int size[] = databaseManager.getSize();
        float x[] = databaseManager.getPosX();
        float y[] = databaseManager.getPosY();
        for(int i=0;i<3;i++)
            nameplateManager.para.setPara(i,str[i],color[i],size[i],style[i],x[i],y[i]);

        nameplateManager.para.setNpType(databaseManager.getNamePlateType());
        nameplateManager.para.setBgColor(databaseManager.getNamePlateBGColor());
        nameplateManager.para.setBgImg(databaseManager.getNamePlateBGImg());
        nameplateManager.para.setNpImg(databaseManager.getNamePlateImage());
    }

    private void databaseInit(){
        DatabaseManager.getDatabaseManager().init(this);

        (new Timer()).schedule(new TimerTask() {
            @Override
            public void run() {
                LocalBroadcast.send(MyApplication.ACTION_REFRESH_ACTIVITY, MainActivity.class);
            }
        },3000);
    }

    private void folderInit(){
        File folder = new File(DEVICE_ROOT);
        if(!(new File(DEVICE_ROOT)).exists())
            folder.mkdirs();

        folder = new File(NAMEPLATE_ROOT);
        if(!folder.exists())
            folder.mkdirs();

        folder = new File(NAMEPLATE_IMG_PATH);
        if(!folder.exists())
            folder.mkdirs();

        folder = new File(NAMEPLATE_BACKGROUND_PATH);
        if(!folder.exists())
            folder.mkdirs();

        folder = new File(NAMEPLATE_BIN_FILE_PATH);
        if(!folder.exists())
            folder.mkdirs();

        folder = new File(DATABASE_ROOT);
        if(!folder.exists())
            folder.mkdirs();

    }

    private void screenLightInit(){
        DatabaseManager manager = DatabaseManager.getDatabaseManager();
        Cmd.execCmd("settings put system screen_brightness " + manager.getBrightness());
        int time = manager.getScreenDimTime();
        Cmd.execCmd("settings put system screen_off_timeout " +(time==Integer.MAX_VALUE ? time : time * 1000));
    }

    //判断是否为APP主进程
    private boolean isAppMainProcess() {
        try {
            int pid = android.os.Process.myPid();
            String process = getAppNameByPID(getApplicationContext(), pid);
            if (TextUtils.isEmpty(process)) {
                return true;
            } else if ((PROCESS_NAME).equalsIgnoreCase(process)) {
                return true;
            } else {
                return false;
            }
        } catch (Exception e) {
            e.printStackTrace();
            return true;
        }
    }

    public static class LocalBroadcast {
        private static LocalBroadcastManager broadcastManager;

        private static void init(Context context) {
            broadcastManager = LocalBroadcastManager.getInstance(context);
        }

        public static void send(String action, Bundle value) {
            Intent intent = new Intent(action);
            intent.putExtra("BUNDLE", value);
            broadcastManager.sendBroadcast(intent);
        }

        public static void send(String action, Class<?> value) {
            Intent intent = new Intent(action);
            intent.putExtra("CLASS", value);
            broadcastManager.sendBroadcast(intent);
        }

        public static void send(String action, String value) {
            Intent intent = new Intent(action);
            intent.putExtra("STRING", value);
            broadcastManager.sendBroadcast(intent);
        }

        public static void send(String action, int value) {
            Intent intent = new Intent(action);
            intent.putExtra("INTEGER", value);
            broadcastManager.sendBroadcast(intent);
        }

        public static void send(String action) {
            Intent intent = new Intent(action);
            broadcastManager.sendBroadcast(intent);
        }

    }

    private static String getAppNameByPID(Context context, int pid) {
        ActivityManager manager = (ActivityManager) context.getSystemService(Context.ACTIVITY_SERVICE);
        for (android.app.ActivityManager.RunningAppProcessInfo processInfo : manager.getRunningAppProcesses()) {
            if (processInfo.pid == pid) {
                return processInfo.processName;
            }
        }
        return "";
    }


    /************************* Monitor ****************************/
    private static class Monitor{
        private static final String[] ACTIVITY_WHITE_LIST = {
                "com.itc.ts8209a.activity.EditUserInfoActivity",
                "com.itc.ts8209a.activity.MeetingInfoActivity",
                "com.itc.ts8209a.activity.CallServiceActivity",
                "com.itc.ts8209a.activity.SmsActivity",
                "com.itc.ts8209a.activity.ShowNameActivity",
                "com.itc.ts8209a.activity.SettingActivity",
                "com.itc.ts8209a.activity.MainActivity",
//                "cn.wps.moffice.pdf",
//                "cn.wps.moffice.writer",
//                "cn.wps.moffice.spreadsheet",
//                "cn.wps.moffice.presentation",
//                "cn.wps.moffice.documentmanager",
                "com.android.packageinstaller"};

        private static final String ALIVE_PROCESS_NAME = "com.itc.ts8209a:network";

        private static void pageTableMonitor() {
            Timer monitor = new Timer();
            monitor.schedule(new TimerTask() {

                @Override
                public void run() {
                    // TODO Auto-generated method stub
                    synchronized (this) {
                        try {
                            ActivityManager manager = (ActivityManager) currentActivity.getSystemService(Context.ACTIVITY_SERVICE);
                            String name = manager.getRunningTasks(1).get(0).topActivity.getClassName();
                            int angle = ((WindowManager) currentActivity.getSystemService(Context.WINDOW_SERVICE)).getDefaultDisplay().getRotation();

                            if (!checkActivityInWhiteList(name) || angle != 0) {
                                if (angle != 0) {
                                    monitorHandler.sendEmptyMessage(3);
                                }
                                monitorHandler.sendEmptyMessage(2);
                                Cmd.execCmd("adb shell input keyevent 4");
                                Thread.sleep(1000);
                            }
                        } catch (Exception e) {
                            e.printStackTrace();
                        }
                    }
                }
            }, 1000, 500);
        }

        private static Handler monitorHandler = new Handler() {
            private Toast toast = null;
            public void handleMessage(Message msg) {
                if(toast != null)
                    toast.cancel();
                switch (msg.what) {
                    case 1:
                        toast = Toast.makeText(currentActivity, (String) msg.obj, Toast.LENGTH_SHORT);
                        toast.show();
                        break;
                    case 2:
                        toast = Toast.makeText(currentActivity,"抱歉，该页面未开放使用", Toast.LENGTH_LONG);
                        toast.show();
                        break;
                    case 3:
                         currentActivity.setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE);
                        break;
//                    case 4:
//                        killProcess("cn.wps",false);
//                        break;
                }
            }
        };

        private static boolean checkActivityInWhiteList(String name){
            for (String anActivityWhiteList : ACTIVITY_WHITE_LIST) {
                if (name.contains(anActivityWhiteList) || name.equals(anActivityWhiteList))
                    return true;
            }
            return false;
        }

        private static void procMonitor(){

            Timer monitor = new Timer();
            monitor.schedule(new TimerTask() {
                @Override
                public void run() {
                    if(!getProcessAlive(currentActivity,ALIVE_PROCESS_NAME)) {
                        Debug.d(TAG, ALIVE_PROCESS_NAME + "is dead");
                        NetworkManager.getNetworkManager().startNetworkService();
                    }
                }
            },30*1000,15*1000);
        }

        private static boolean getProcessAlive(Context context,String procName) {
            // 获取活动管理器
            ActivityManager am = (ActivityManager) context.getSystemService(Context.ACTIVITY_SERVICE);

            // 获取正在运行的进程信息
            List<ActivityManager.RunningAppProcessInfo> runningAppProcesses = am.getRunningAppProcesses();
            // 遍历所有 进程
            for (ActivityManager.RunningAppProcessInfo runningAppProcessInfo : runningAppProcesses) {
                String processName = runningAppProcessInfo.processName;
                if(processName.equals(procName))
                    return true;
//                // 进程的id
//                int pid = runningAppProcessInfo.pid;
            }

            return false;
        }
    }
}
