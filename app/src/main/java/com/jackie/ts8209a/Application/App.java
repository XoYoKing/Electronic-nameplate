package com.jackie.ts8209a.Application;

import android.app.ActivityManager;
import android.app.Application;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.os.Handler;
import android.os.IBinder;
import android.os.Message;
import android.os.Messenger;
import android.os.SystemClock;
import android.text.TextUtils;

import com.jackie.ts8209a.Managers.BatteryManager;
import com.jackie.ts8209a.Managers.NetworkManager;
import com.jackie.ts8209a.RemoteServer.Network;
import com.jackie.ts8209a.Managers.UserInfoManager;
import com.jackie.ts8209a.Managers.WifiManager;

import java.io.DataOutputStream;
import java.io.File;
import java.io.IOException;
import java.util.Calendar;
import java.util.HashMap;
import java.util.Map;

/**
 * Created by kuangyt on 2018/8/20.
 */

public class App extends Application {

    private static final String TAG = "App";
    private static final String PROCESS_NAME = "com.jackie.ts8209a";

    //
    private BatteryManager batteryManager;
    private WifiManager wifiManager;
    private WifiManager.WifiInfo wifiInfo;
    private UserInfoManager userInfoManager;
    private NetworkManager networkManager;

    @Override
    public void onCreate() {
        super.onCreate();

        if (isAppMainProcess()) {
            // 完全隐藏系统状态栏以及底部导航键
            sendBroadcast(new Intent("com.android.action.hide_statusbar"));
            sendBroadcast(new Intent("com.android.action.hide_navigationbar"));

            userInfoInit();

            powerInit();

            networkInit();
        }
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

    private void powerInit() {
        batteryManager = BatteryManager.getBatteryManager();
        batteryManager.startGetBatteryStaRegular();
    }

    private void networkInit() {
        try {
            wifiManager = WifiManager.initWifiManager(this);
            wifiInfo = wifiManager.getWifiInfo();
            startService(new Intent(this, Network.class));
            bindService(new Intent(this, Network.class), new ServiceConnection() {
                @Override
                public void onServiceConnected(ComponentName name, IBinder service) {
                    networkManager = NetworkManager.initManager(new Messenger(service));
                }

                @Override
                public void onServiceDisconnected(ComponentName name) {

                }
            }, BIND_AUTO_CREATE);

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

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

    private static String getAppNameByPID(Context context, int pid) {
        ActivityManager manager = (ActivityManager) context.getSystemService(Context.ACTIVITY_SERVICE);
        for (android.app.ActivityManager.RunningAppProcessInfo processInfo : manager.getRunningAppProcesses()) {
            if (processInfo.pid == pid) {
                return processInfo.processName;
            }
        }
        return "";
    }

    private void userInfoInit() {
        userInfoManager = UserInfoManager.getUserInfoManager();
        userInfoManager.init(this);
    }


}
