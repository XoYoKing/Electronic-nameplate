package com.jackie.ts8209a.Activity;

import android.app.Activity;
import android.app.ActivityManager;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.graphics.Color;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.os.PowerManager;
import android.support.v4.content.LocalBroadcastManager;
import android.util.Log;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.Animation;
import android.view.animation.TranslateAnimation;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.jackie.ts8209a.AppModule.APP;
import com.jackie.ts8209a.AppModule.Basics.BatteryManager;
import com.jackie.ts8209a.AppModule.Network.EthernetManager;
import com.jackie.ts8209a.AppModule.Network.NetDevManager;
import com.jackie.ts8209a.AppModule.Network.NetworkManager;
import com.jackie.ts8209a.RemoteServer.Network;
import com.jackie.ts8209a.AppModule.Network.WifiManager;
import com.jackie.ts8209a.R;

import java.util.ArrayList;
import java.util.HashMap;

import static com.jackie.ts8209a.AppModule.Network.NetworkManager.*;

/**
 * Created by kuangyt on 2018/8/21.
 */

public class AppActivity extends Activity {

    public static final int MEETING_INFO = 1;
    public static final int USER_LIST = 2;
    public static final int SERVIE_ACK = 3;
    public static final int SMS_MSG = 4;
    public static final int SYS_SMS_MSG = 5;

    //MeetingInfo
    protected static String infoName = "";
    protected static String infoSlogan = "";
    protected static String infoContent = "";
    protected static String infoStartTime = "";
    protected static String infoEndTime = "";

    //MeetingInfo->Msg from Administrator
    protected static ArrayList<String> adminMsg = new ArrayList<String>();

    //SMS->UserList
    protected static HashMap<Integer,String> userList = null;
    //SMS->sms infoContent
    protected static ArrayList<String[]> newSMS = new ArrayList<String[]>();
    protected static ArrayList<String[]> oldSMS = new ArrayList<String[]>();

    protected String TAG = getClass().getSimpleName();
    protected boolean returnEnable = true;
    protected boolean stateBarEnable = true;
    protected APP App;

    protected PowerManager.WakeLock wakeLock;
    protected stateBarHandler stateBar;
    protected Button btnReturn;

    protected BatteryManager batteryManager;
    protected WifiManager wifiManager;
    protected NetworkManager networkManager;

    protected LocalBroadcastManager localBroadcast = null;
    protected IntentFilter intentFilter = null;
//    protected BroadcastReceiver broadcastReceiver = null;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
//        ActionBar actionbar = getActionBar();
//        actionbar.hide();
        View view = getWindow().getDecorView();
        view.setSystemUiVisibility(View.SYSTEM_UI_FLAG_LAYOUT_STABLE
                | View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION
                | View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
                | View.SYSTEM_UI_FLAG_HIDE_NAVIGATION // hide nav bar
                | View.SYSTEM_UI_FLAG_FULLSCREEN // hide state bar
                | View.SYSTEM_UI_FLAG_IMMERSIVE
                | View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY);
        overridePendingTransition(R.anim.fadein, R.anim.fadeout);

//        actContext = this;
        try {
            App = (APP) getApplication();
            batteryManager = BatteryManager.getBatteryManager();
            networkManager = NetworkManager.getNetworkManager();
        } catch (Exception e) {
        }

        keepWake();

        if(stateBarEnable)
            stateBar = new stateBarHandler(this);

        initBroadcast();
    }

    @Override
    protected void onResume() {
        super.onResume();
        wakeLock.acquire(); //设置保持唤醒
        PromptBox.creatPromptBox(this);

        if(stateBarEnable) {
            stateBar.configStatusBar();
        }

        if(returnEnable){
            btnReturn = (Button)findViewById(R.id.return_btn);
            btnReturn.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    retBtnClick();
                }
            });
        }

    }

    @Override
    public void finish() {
        super.finish();
        overridePendingTransition(R.anim.fadein, R.anim.fadeout);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
//        batteryManager.unRegManagement();
        wakeLock.release();
    }

    //保持设备不锁屏
    protected void keepWake(){
        PowerManager pm = (PowerManager)getSystemService(POWER_SERVICE);
        wakeLock = pm.newWakeLock(PowerManager.SCREEN_DIM_WAKE_LOCK, "cn");
//        wakeLock.acquire(); //设置保持唤醒
    }

    protected class stateBarHandler {

//        private int batLevel;
//        private boolean batCharge;

        private TextView tvBatLevel;
        private ImageView ivBatLevel;
        private ImageView ivWifi;
        private ImageView ivEthNet;
        private ImageView ivNetwork;

        private Context context;

        public stateBarHandler(Context context){
            this.context = context;
        }

        public void configStatusBar(){

            Activity activity = (Activity)context;
            tvBatLevel = (TextView)activity.findViewById(R.id.stabar_batlevel_tv);
            ivBatLevel = (ImageView) activity.findViewById(R.id.stabar_batlevel_iv);
            ivWifi = (ImageView)activity.findViewById(R.id.stabar_wifi_iv);
            ivEthNet = (ImageView)activity.findViewById(R.id.stabar_ether_net_iv);
            ivNetwork = (ImageView)activity.findViewById(R.id.stabar_network_iv);

            networkManager.setOnNetworkStatusListener(new networkStatusListener());

            setBatLevelDisplay(batteryManager.getLevel(),batteryManager.getCharge());
            setNetworkStaDisplay(networkManager.getNetworkStatus() == Network.STA_CONNECTED);

            NetDevManager.NetDevInfo netDevInfo = networkManager.getNetDevInfo();
            if(netDevInfo != null){
//                Log.d(TAG,netDevInfo.getDevName() + " : " + netDevInfo.getDevEn());
                if(netDevInfo.getDevName().equals(EthernetManager.DEV_NAME)){
                    setWifiInfoDisplay(false,0);
                    setEthInfoDisplay(netDevInfo.getNetEn());
                }else if(netDevInfo.getDevName().equals(WifiManager.DEV_NAME)){
                    setEthInfoDisplay(false);
                    setWifiInfoDisplay(netDevInfo.getDevEn(),netDevInfo.getRssi());
                }
            }
        }

        private void setBatLevelDisplay(int level ,boolean charge){

            tvBatLevel.setText(level +"%");

            if(charge){
                ivBatLevel.setImageLevel(5);
            }else{
                if(level <= 5){
                    ivBatLevel.setImageLevel(4);
                }else if(level > 5 && level <= 20){
                    ivBatLevel.setImageLevel(3);
                }else if(level > 20 && level <= 50){
                    ivBatLevel.setImageLevel(2);
                }else if(level > 50 && level <= 80){
                    ivBatLevel.setImageLevel(1);
                }else if(level >80 && level <= 100){
                    ivBatLevel.setImageLevel(0);
                }
            }
        }

        private void setWifiInfoDisplay(boolean enable,int rssi){
            if(enable){
                ivWifi.setVisibility(View.VISIBLE);

                if (rssi > -50 && rssi < 0) {//最强
                    ivWifi.setImageLevel(0);
                } else if (rssi > -70 && rssi < -50) {//较强
                    ivWifi.setImageLevel(1);
                } else if (rssi > -80 && rssi < -70) {//较弱
                    ivWifi.setImageLevel(2);
                } else if (rssi < -80) {//微弱
                    ivWifi.setImageLevel(3);
                }
            }
            else
                ivWifi.setVisibility(View.GONE);
        }

        private void setEthInfoDisplay(boolean enable){
            if(enable)
                ivEthNet.setVisibility(View.VISIBLE);
            else
                ivEthNet.setVisibility(View.GONE);
        }

        private void setNetworkStaDisplay(boolean display){
            if(display)
                ivNetwork.setVisibility(View.VISIBLE);
            else
                ivNetwork.setVisibility(View.GONE);
        }

        private class networkStatusListener implements NetworkManager.OnNetworkStatusListener{
            @Override
            public void OnNetworkStatus(int sta) {
                if(sta == Network.STA_CONNECTED)
                    setNetworkStaDisplay(true);
                else
                    setNetworkStaDisplay(false);
            }
        }

    }

    //本地广播
    protected void initBroadcast(){
        localBroadcast = LocalBroadcastManager.getInstance(this);

        setIntentFilter();
        localBroadcast.registerReceiver(new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                broadcastProcessing(intent);
            }
        },intentFilter);

    }

    protected void setIntentFilter(){
        intentFilter = new IntentFilter();
        intentFilter.addAction(APP.ACTION_MAIN);
        intentFilter.addAction(APP.ACTION_STAR_ACTIVITY);
        intentFilter.addAction(APP.ACTION_FINISH_ACTIVITY);
        intentFilter.addAction(APP.ACTION_REFRESH_ACTIVITY);
        intentFilter.addAction(APP.ACTION_NETWORK_INFO_UPDATE);
        intentFilter.addAction(APP.ACTION_BATTERY_INFO_UPDATE);
    }

    protected void broadcastProcessing(Intent intent){
        if (intent.getAction().equals(APP.ACTION_STAR_ACTIVITY)
                && isActivityTop()) {

            Intent starActIntent = new Intent(this, (Class<?>) intent.getExtras().get("CLASS"));
            startActivity(starActIntent);

        } else if (intent.getAction().equals(APP.ACTION_FINISH_ACTIVITY)
                && isActivityTop()
                && getClass().equals(intent.getExtras().get("CLASS"))) {

            this.finish();

        } else if (intent.getAction().equals(APP.ACTION_REFRESH_ACTIVITY)
                && isActivityTop()
                && (getClass().equals(intent.getExtras().get("CLASS")) || intent.getExtras().get("CLASS") == null)) {

            onResume();
        }else if(intent.getAction().equals(APP.ACTION_NETWORK_INFO_UPDATE)){
            Bundle bundle = intent.getBundleExtra("BUNDLE");
            String devName = bundle.getString(DEV_NAME);

            if(devName.equals(WifiManager.DEV_NAME)) {
                stateBar.setEthInfoDisplay(false);
                stateBar.setWifiInfoDisplay(bundle.getBoolean(DEV_EN), bundle.getInt(RSSI));
            }
            else if(devName.equals(EthernetManager.DEV_NAME)) {
                stateBar.setWifiInfoDisplay(false,0);
                stateBar.setEthInfoDisplay(bundle.getBoolean(NETWORK_EN));
            }
        }else if(intent.getAction().equals(APP.ACTION_BATTERY_INFO_UPDATE)){
            Bundle bundle = intent.getBundleExtra("BUNDLE");
            stateBar.setBatLevelDisplay(bundle.getInt(BatteryManager.BAT_LEVEL),bundle.getBoolean(BatteryManager.BAT_CHARGE));
        }
    }

    protected boolean isActivityTop() {
        Class<? extends Context> cls = getClass();
        ActivityManager manager = (ActivityManager)getSystemService(Context.ACTIVITY_SERVICE);
        String name = manager.getRunningTasks(1).get(0).topActivity.getClassName();
        return name.equals(cls.getName());
    }

    protected void retBtnClick(){
        finish();
    }

    public static class PromptBox {

        public static final int MAX = 60000;
        public static final int DEF = 10000;// 多条提示之间切换时间:默认10S

        private static final int CLOSE = 0;
        private static final int UPDATA = 1;
        private static final int REDISPLAY = 2;
        private static final int DISPLAY = 3;
        private static final int REFLASH = 4;

        private static PromptBox promBox = new PromptBox();

        private static Context promContext = null;
        private static LinearLayout promLayout = null;

        private static ArrayList<promBoxContent> promContentList = new ArrayList<promBoxContent>();
        // private static boolean display = false;

        private static TranslateAnimation mShowAction;
        private static TranslateAnimation mHiddenAction;
        private static promptThread promThread = null;


        private PromptBox() {
            mShowAction = new TranslateAnimation(Animation.RELATIVE_TO_SELF,0.0f,
                    Animation.RELATIVE_TO_SELF, 0.0f,
                    Animation.RELATIVE_TO_SELF, -1.0f,
                    Animation.RELATIVE_TO_SELF, 0.0f);
            mShowAction.setDuration(500);
            mHiddenAction = new TranslateAnimation(	Animation.RELATIVE_TO_SELF,0.0f,
                    Animation.RELATIVE_TO_SELF, 0.0f,
                    Animation.RELATIVE_TO_SELF, 0.0f,
                    Animation.RELATIVE_TO_SELF, -1.0f);
            mHiddenAction.setDuration(500);
        }


        private static void creatPromptBox(Context context) {
            promContext = context;
            Activity activity = (Activity) context;

            try {
                promLayout = (LinearLayout) activity
                        .findViewById(R.id.prompt_box_lay);
                promLayout.setVisibility(View.GONE);

                promHandler.sendEmptyMessage(REDISPLAY);
            } catch (Exception e) {
                Log.e("creatPromptBox", "promLayout:" + e);
            }
        }

        // 提示框控制句柄
        private static final Handler promHandler = new Handler() {
            private promBoxContent promContent = null;

            // private LinearLayout layout = null;

            @Override
            public void handleMessage(Message msg) {
                switch (msg.what) {
                    case CLOSE:
                        promThread = null;
                        promContent = null;
                        promLayout.startAnimation(mHiddenAction);
                        promLayout.setVisibility(View.GONE);
                        break;
                    case UPDATA:
                        try {
                            promContent = (promBoxContent) msg.obj;

                            LinearLayout layout;

                            layout = promContent.getLayout(promContext);
                            layout.setVisibility(View.GONE);
                            layout.startAnimation(mShowAction);
                            promLayout.removeAllViews();
                            promLayout.addView(layout);
                            layout.setVisibility(View.VISIBLE);

                        } catch (Exception e) {
                            Log.e("promHandler", "UPDATA:" + e);
                        }
                        break;
                    case REDISPLAY:
                        if (promContent != null && promLayout != null) {
                            try {
                                promLayout.removeAllViews();
                                promLayout.addView(promContent
                                        .getLayout(promContext));
                                promLayout.setVisibility(View.VISIBLE);
                            } catch (Exception e) {
                                Log.e("promHandler", "REDISPLAY:" + e);
                            }
                        }
                        break;
                    case DISPLAY:
                        if (promLayout != null) {
                            if (promThread == null) {
                                promThread = new promptThread();
                                promThread.start();
                            }
                            promLayout.removeAllViews();
                            promLayout.startAnimation(mShowAction);
                            promLayout.setVisibility(View.VISIBLE);
                        }
                        break;
                    case REFLASH:
                        if (promThread != null) {
                            promThread.interrupt();
                        }
                        break;
                    default:
                        break;
                }
            }
        };

        // 提示框数据监测线程
        private static class promptThread extends Thread {
            @Override
            public void run() {
                int index = 0;

                while (promContentList.size() > 0) {
//					Log.i("promContentList", "size = " + promContentList.size());
                    if (index < 0)
                        index = promContentList.size() - 1;
                    try {
                        promBoxContent content = promContentList.get(index);

                        if (content.time <= 0) {
                            promContentList.remove(index);
                        } else {
                            // Log.i("infoContent", infoContent.infoName + ":"
                            // + infoContent.promText + "num=" + infoContent.time
                            // + "index=" + index);
                            content.time--;
                            promContentList.set(index, content);

                            Message msg = new Message();
                            msg.what = UPDATA;
                            msg.obj = content;
                            promHandler.sendMessage(msg);

                            Thread.sleep(content.timeout);
                        }
                        // promLayout.removeView(layout);
                    } catch (Exception e) {
//						Log.e("Prompt Box Thread ERR", "" + e);
                    }

                    index--;
                }
                promHandler.sendEmptyMessage(CLOSE);

            }
        };

        //增加提示内容
        public static Builder BuildPrompt(String name) {
            final Builder builder = new Builder();

            try {
                builder.content.name = name;
                int index = findContentByName(name);
                if(index < 0){
                    builder.content.promText = "";
                    builder.content.promSrcId = 0;
                    builder.content.promView = null;
                    builder.content.time = DEF;
                    builder.content.timeout = DEF;

                    new Thread() {
                        public void run() {
                            try {
                                Thread.sleep(150);
                            } catch (InterruptedException e) {
                            }
                            promContentList.add(builder.content);
                            if (promThread == null)
                                promHandler.sendEmptyMessage(DISPLAY);
                            else
                                promHandler.sendEmptyMessage(REFLASH);
                        };
                    }.start();
                }
            } catch (Exception e) {
                Log.e("BuildPrompt", "" + e);
            }
            return builder;
        }

        //提示内容操作类及方法
        public static class Builder {
            private promBoxContent content = new promBoxContent();

            private Builder() {
            }

            public Builder Text(String text) {
                content.promText = text;
                return this;
            }

            public Builder Image(int id) {
                content.promSrcId = id;
                return this;
            }

            public Builder View(View view) {
                content.promView = view;
                return this;
            }

            public Builder Time(int time) {
                content.time = time;
                return this;
            }

            public Builder TimeOut(int timeout) {
                content.timeout = timeout;
                return this;
            }
        }

        // 删除提示内容
        public static void removePrompt(String name) {
            int index = findContentByName(name);
//			Log.i("removePrompt", infoName);
            if (index >= 0) {
                promContentList.remove(index);
//				Log.i("removePrompt", "success:" + index);
            }
        }

        // 内部方法，根据name寻找到对应的索引号
        private static int findContentByName(String name) {
            int index;
            for (index = 0; index < promContentList.size(); index++) {
                if (promContentList.get(index).name.equals(name))
                    return index;
            }
            return -1;
        }

        private static class promBoxContent {
            // 信息内容参数
            public String name; // 提示信息的命名
            public int time; // 显示的次数
            public int timeout; // 单次显示的时间

            // 显示内容变量
            public String promText; // 主要提示信息文字
            public int promSrcId; // 图片
            public View promView; // 嵌入其他VIEW 如按钮

            public promBoxContent() {

                // promBtnText = "";
            }

            public LinearLayout getLayout(Context context)
                    throws Exception {
                LinearLayout layout = new LinearLayout(context);
                LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(
                        LinearLayout.LayoutParams.WRAP_CONTENT, LinearLayout.LayoutParams.WRAP_CONTENT);
                layout.setLayoutParams(params);

                if (promText.equals(""))
                    throw new Exception("promTextNUllException");

                if (promSrcId != 0) {
                    ImageView imgPromSrc = new ImageView(context);
                    imgPromSrc.setImageDrawable(context.getResources()
                            .getDrawable(promSrcId));
                    layout.addView(imgPromSrc);
                }

                if (promView != null) {
                    ViewGroup parent = (ViewGroup) promView.getParent();
                    if (parent != null) {
                        parent.removeAllViews();
                    }
                    layout.addView(promView);
                }

                TextView tvPromText = new TextView(context);
                tvPromText.setText(promText);
                tvPromText.setTextColor(Color.WHITE);
                tvPromText.setTextSize(24);
                layout.addView(tvPromText);

                return layout;
            }

//			public class PromBoxException extends Exception {
//
//				public PromBoxException(String detailMessage) {
//					super(detailMessage);
//				}
//			}
        }

    }

    public static Handler handler = new Handler() {
        @Override
        public void handleMessage(Message msg) {
            Bundle bundle = msg.getData();
            switch (msg.what) {
                case MEETING_INFO:
                    if (bundle == null)
                        break;
                    infoName = bundle.getString("strName");
                    infoSlogan = bundle.getString("strSlogan");
                    infoContent = bundle.getString("strContent");
                    infoStartTime = bundle.getString("strStartTime");
                    infoEndTime = bundle.getString("strEndTime");
                    break;
                case USER_LIST:
                    userList = (HashMap) msg.obj;
                    break;
                case SERVIE_ACK:
                    PromptBox.BuildPrompt("CALL_SERVICE_ACK").Text("已收到服务请求：" + msg.obj).Time(1).TimeOut(2500);
                    break;
                case SMS_MSG:
                    if (bundle == null)
                        break;
                    String[] smsStr = new String[2];
                    int userId = bundle.getInt("iReceiverID");
                    try {
                        smsStr[0] = userList.get(userId);
                        if (smsStr[0] == null)
                            smsStr[0] = "ID: " + userId;
                    } catch (Exception e) {
                        smsStr[0] = "ID: " + userId;
                    }
                    smsStr[1] = bundle.getString("strContent");
                    Log.i("sms receive", smsStr[0] + "-->" + smsStr[1]);
                    newSMS.add(smsStr);

                    PromptBox.BuildPrompt("YOU_HAVE_A_SMS")
                            .Text("你有一条来自：" + smsStr[0]
                                    + "的新消息").Time(1).TimeOut(8000);
                    break;
                case SYS_SMS_MSG:
                    adminMsg.add(bundle.getString("strContent"));
                    PromptBox.BuildPrompt("YOU_HAVE_A_SMS")
                            .Text("你有一条来自管理员的新消息").Time(1).TimeOut(8000);
                    APP.LocalBroadcast.send(APP.ACTION_REFRESH_ACTIVITY, MeetingInfoActivity.class);
                    break;
            }
        }
    };
}
