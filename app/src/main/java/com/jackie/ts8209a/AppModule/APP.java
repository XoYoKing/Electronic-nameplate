package com.jackie.ts8209a.AppModule;

import android.app.ActivityManager;
import android.app.Application;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.os.SystemClock;
import android.support.v4.content.LocalBroadcastManager;
import android.text.TextUtils;

import com.jackie.ts8209a.AppModule.Basics.NameplateManager;
import com.jackie.ts8209a.AppModule.Basics.PowerManager;
import com.jackie.ts8209a.AppModule.Basics.FontManager;
import com.jackie.ts8209a.AppModule.Network.NetworkManager;
import com.jackie.ts8209a.AppModule.Basics.UserInfoManager;
import com.jackie.ts8209a.AppModule.Tools.Printf;

import java.io.DataOutputStream;
import java.io.File;
import java.io.IOException;
import java.util.Calendar;

/**
 * Created by kuangyt on 2018/8/20.
 */

public class APP extends Application {

    private static final String TAG = "App";
    private static final String PROCESS_NAME = "com.jackie.ts8209a";

    //软件本地广播标志 Broadcast static value(BSV)
    public static final String ACTION_MAIN = PROCESS_NAME + ".localbroadcast";
    public static final String ACTION_STAR_ACTIVITY = ACTION_MAIN + ".activity.start";
    public static final String ACTION_FINISH_ACTIVITY = ACTION_MAIN + ".activity.finish";
    public static final String ACTION_REFRESH_ACTIVITY = ACTION_MAIN + ".activity.refresh";
    public static final String ACTION_NETWORK_INFO_UPDATE = ACTION_MAIN + ".network.infoupdate";
    public static final String ACTION_POWER_INFO_UPDATE = ACTION_MAIN + ".power.infoupdate";

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
            UserInfoManager.getUserInfoManager().init(this);

            //初始化电源管理模块
            PowerManager.getPowerManager().init(this);

            //初始化网络管理模块
            NetworkManager.getNetworkManager().init(this);

            //初始化字体管理模块
            FontManager.getFontManager().init();

            //初始化电子铭牌管理模块
            NameplateManager.getNameplateManager().init(this);
            nameplateParaInit();

            //初始化串口debug
            Printf.init();
        }
    }

    private void nameplateParaInit(){
        NameplateManager nameplateManager = NameplateManager.getNameplateManager();
        UserInfoManager userInfoManager = UserInfoManager.getUserInfoManager();

        String str[] = userInfoManager.getStr();
        int color[] = userInfoManager.getColor();
        int style[] = userInfoManager.getStyle();
        int size[] = userInfoManager.getSize();
        float x[] = userInfoManager.getPosX();
        float y[] = userInfoManager.getPosY();
        for(int i=0;i<3;i++)
            nameplateManager.para.setPara(i,str[i],color[i],size[i],style[i],x[i],y[i]);

        nameplateManager.para.setNpType(userInfoManager.getNamePlateType());
        nameplateManager.para.setBgColor(userInfoManager.getNamePlateBGColor());
        nameplateManager.para.setBgImg(userInfoManager.getNamePlateBGImg());
        nameplateManager.para.setNpImg(userInfoManager.getNamePlateImage());
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

    public static class SystemDateTime {

        static final String TAG = "SystemDateTime";

        public static void setDateTime(int year, int month, int day, int hour, int minute) throws IOException, InterruptedException {

            requestPermission();

            Calendar c = Calendar.getInstance();

            c.set(Calendar.YEAR, year);
            c.set(Calendar.MONTH, month - 1);
            c.set(Calendar.DAY_OF_MONTH, day);
            c.set(Calendar.HOUR_OF_DAY, hour);
            c.set(Calendar.MINUTE, minute);


            long when = c.getTimeInMillis();

            if (when / 1000 < Integer.MAX_VALUE) {
                SystemClock.setCurrentTimeMillis(when);
            }

            long now = Calendar.getInstance().getTimeInMillis();
            //Log.d(TAG, "set tm="+when + ", now tm="+now);

            if (now - when > 1000)
                throw new IOException("failed to set Date.");

        }

        public static void setDateTimeMillis(long millis) throws IOException, InterruptedException {
            millis *= 1000;
            requestPermission();

            if (millis / 1000 < Integer.MAX_VALUE) {
                SystemClock.setCurrentTimeMillis(millis);
            }

            long now = Calendar.getInstance().getTimeInMillis();
            //Log.d(TAG, "set tm="+when + ", now tm="+now);

            if (now - millis > 1000)
                throw new IOException("failed to set Date.");

        }

        static void requestPermission() throws InterruptedException, IOException {
            createSuProcess("chmod 666 /dev/alarm").waitFor();
        }

        static Process createSuProcess() throws IOException {
            File rootUser = new File("/system/xbin/ru");
            if (rootUser.exists()) {
                return Runtime.getRuntime().exec(rootUser.getAbsolutePath());
            } else {
                return Runtime.getRuntime().exec("su");
            }
        }

        static Process createSuProcess(String cmd) throws IOException {

            DataOutputStream os = null;
            Process process = createSuProcess();

            try {
                os = new DataOutputStream(process.getOutputStream());
                os.writeBytes(cmd + "\n");
                os.writeBytes("exit $?\n");
            } finally {
                if (os != null) {
                    try {
                        os.close();
                    } catch (IOException e) {
                    }
                }
            }

            return process;
        }
    }
}
