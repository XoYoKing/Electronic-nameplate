package com.itc.ts8209a.activity;

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
import android.support.v4.content.LocalBroadcastManager;
import android.util.Log;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.Animation;
import android.view.animation.TranslateAnimation;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;
import android.widget.TextClock;
import android.widget.TextView;

import com.bumptech.glide.Glide;
import com.itc.ts8209a.app.MyApplication;
import com.itc.ts8209a.module.nameplate.NameplateManager;
import com.itc.ts8209a.module.power.PowerManager;
import com.itc.ts8209a.module.database.DatabaseManager;
import com.itc.ts8209a.module.network.EthernetManager;
import com.itc.ts8209a.module.network.NetDevManager;
import com.itc.ts8209a.module.network.NetworkManager;
import com.itc.ts8209a.server.Network;
import com.itc.ts8209a.module.network.WifiManager;
import com.itc.ts8209a.R;
import com.itc.ts8209a.widget.Cmd;

import java.util.ArrayList;
import java.util.HashMap;
import static com.itc.ts8209a.app.AppConfig.*;
import static com.itc.ts8209a.app.MyApplication.setCurrentActivity;
import static com.itc.ts8209a.module.network.NetworkManager.*;
import static com.itc.ts8209a.module.power.PowerManager.BATTERY_MODE;
import static com.itc.ts8209a.module.power.PowerManager.POE_MODE;

/**
 * Created by kuangyt on 2018/8/21.
 */

public class AppActivity extends Activity implements View.OnClickListener {

    public static final int MEETING_INFO = 1;
    public static final int USER_LIST = 2;
    public static final int SERVIE_ACK = 3;
    public static final int SMS_MSG = 4;
    public static final int SYS_SMS_MSG = 5;
    public static final int MEETING_END = 6;
    public static final int USER_INFO = 7;
//    public static final int MSG_INIT = 8;

    /***************** 静态变量字段  ******************/
    //会议信息字段
    protected static String meetName = "";
    protected static String meetSlogan = "";
    protected static String meetContent = "";
    protected static String meetStartTime = "";
    protected static String meetEndTime = "";

    /* 会议信息，管理员消息 */
    protected static ArrayList<String> adminMsg;
    protected static int newAdminMsgCount;
    /* 短消息：用户用户列表 */
    protected static HashMap<Integer,String> userList = null;
    /*  短消息：短消息内容，新旧消息列表 */
    protected static ArrayList<String[]> newSMS = new ArrayList<String[]>();
    protected static ArrayList<String[]> oldSMS;


    protected String TAG = getClass().getSimpleName();
    protected MyApplication myApplication;

    protected android.os.PowerManager.WakeLock wakeLock;
    protected stateBarHandler stateBar;
    protected Button btnReturn;

    protected PowerManager powerManager;
    protected NetworkManager networkManager;
    protected NetDevManager.NetDevInfo netDevInfo;
    protected DatabaseManager databaseManager;
    protected NameplateManager nameplateManager;

    protected LocalBroadcastManager localBroadcast = null;
    protected IntentFilter intentFilter = null;

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

        keepWake();

        myApplication = (MyApplication) getApplication();

        stateBar = new stateBarHandler(this);

        initBroadcast();
    }

    @Override
    protected void onResume() {
        super.onResume();

        wakeLock.acquire(); //设置保持唤醒

        networkManager = NetworkManager.getNetworkManager();
        netDevInfo = networkManager.getNetDevInfo();
        databaseManager = DatabaseManager.getDatabaseManager();
        powerManager = PowerManager.getPowerManager();
        networkManager = NetworkManager.getNetworkManager();
        nameplateManager = NameplateManager.getNameplateManager();

        setCurrentActivity(this);
        PromptBox.creatPromptBox(this);

        btnReturn = (Button) findViewById(R.id.return_btn);
        btnReturn.setOnClickListener(this);

        registerBroadcast();

        uiRefresh();
    }

    protected void uiRefresh(){
        stateBar.configStatusBar();
    }

    @Override
    protected void onPause() {
        super.onPause();

        unregisterBroadcast();
    }

    @Override
    public void finish() {
        super.finish();
        overridePendingTransition(R.anim.fadein, R.anim.fadeout);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
//        powerManager.unRegManagement();
        wakeLock.release();
    }

    //保持设备不锁屏
    protected void keepWake(){
        android.os.PowerManager pm = (android.os.PowerManager)getSystemService(POWER_SERVICE);
        wakeLock = pm.newWakeLock(android.os.PowerManager.SCREEN_DIM_WAKE_LOCK, "cn");
//        wakeLock.acquire(); //设置保持唤醒
    }

    @Override
    public void onClick(View view) {
        if(view.getId() == R.id.return_btn)
            retBtnClick();
    }

    protected class stateBarHandler implements NetworkManager.OnNetworkStatusListener {

        private RelativeLayout layout;
        private TextClock tcClock;
        private TextView tvBatLevel;
        private TextView tvPrompt;
        private ImageView ivBatLevel;
        private ImageView ivWifi;
        private ImageView ivEthNet;
        private ImageView ivNetwork;
        private ImageView ivPoePower;

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
            layout = (RelativeLayout)activity.findViewById(R.id.stabar_lay);
            ivPoePower = (ImageView)activity.findViewById(R.id.stabar_poe_power_iv);
            tvPrompt = (TextView)activity.findViewById(R.id.stabar_prompt_tv);
            tcClock = (TextClock)activity.findViewById(R.id.stabar_clock_tc);

            networkManager.setOnNetworkStatusListener(this);

            setBatLevel(powerManager.getLevel(), powerManager.getCharge(),powerManager.getVoltage()!=0);
            setNetworkStaDisplay(networkManager.getNetworkStatus() == Network.STA_CONNECTED);

            NetDevManager.NetDevInfo netDevInfo = networkManager.getNetDevInfo();
            if(netDevInfo != null){
//                Log.d(TAG,netDevInfo.getDevName() + " : " + netDevInfo.getDevEn());
                if(netDevInfo.getDevName().equals(EthernetManager.DEV_NAME)){
                    setWifiInfoDisplay(false,0);
                    setEthInfoDisplay(netDevInfo.getNetEn());
                }else if(netDevInfo.getDevName().equals(WifiManager.DEV_NAME)){
                    setEthInfoDisplay(false);
                    if(netDevInfo.getDevEn())
                     setWifiInfoDisplay( networkManager.getNetDevInfo().getNetEn(),netDevInfo.getRssi());
                }
            }

            if(networkManager.getIsTimeUptate())
                tcClock.setVisibility(View.VISIBLE);
            else
                tcClock.setVisibility(View.GONE);


            setPowMode(powerManager.getPowerMode());

            if(!newSMS.isEmpty() || newAdminMsgCount != 0) {
                String prompt = String.format("您有%s%s",newSMS.isEmpty() ? "" : newSMS.size()+"条未读短消息 ",newAdminMsgCount == 0 ? "" : newAdminMsgCount+"条未读管理员消息");
                setPrompt(true,prompt);
            }else
                setPrompt(false,null);

        }

        public void hide(){
            layout.setVisibility(View.GONE);
        }

        public void show(){
            layout.setVisibility(View.VISIBLE);
        }

        private void setBatLevel(int level , boolean charge,boolean isShowLevelText){

            if(isShowLevelText) {
                tvBatLevel.setVisibility(View.VISIBLE);
                tvBatLevel.setText(level + "%");
            }else
                tvBatLevel.setVisibility(View.GONE);

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

        private void setPowMode(int mode){

            switch(mode){
                case POE_MODE:
                    ivPoePower.setVisibility(View.VISIBLE);
                    tvBatLevel.setVisibility(View.GONE);
                    ivBatLevel.setVisibility(View.GONE);
                    break;
                case BATTERY_MODE:
                    ivPoePower.setVisibility(View.GONE);
                    tvBatLevel.setVisibility(View.VISIBLE);
                    ivBatLevel.setVisibility(View.VISIBLE);
                    break;

                default:
                    tvBatLevel.setVisibility(View.GONE);
                    ivBatLevel.setVisibility(View.GONE);
                    ivPoePower.setVisibility(View.GONE);
                    break;

            }

        }

        private void setWifiInfoDisplay(boolean enable,int rssi){
            if(enable){
                ivWifi.setVisibility(View.VISIBLE);

                if (rssi > -70 && rssi < 0) {//最强
                    ivWifi.setImageLevel(0);
                } else if (rssi > -80 && rssi < -70) {//较强
                    ivWifi.setImageLevel(1);
                } else if (rssi > -85 && rssi < -80) {//较弱
                    ivWifi.setImageLevel(2);
                } else if (rssi < -85) {//微弱
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

        private void setPrompt(boolean visible,String prompt){
            if(visible){
                tvPrompt.setVisibility(View.VISIBLE);
                tvPrompt.setText(prompt);
            }else
                tvPrompt.setVisibility(View.GONE);
        }

        @Override
        public void OnNetworkStatus(int sta) {
            if (sta == Network.STA_CONNECTED) {
                setNetworkStaDisplay(true);
                tcClock.setVisibility(View.VISIBLE);
            }
            else
                setNetworkStaDisplay(false);
        }

    }

    //本地广播
    protected void initBroadcast(){
        localBroadcast = LocalBroadcastManager.getInstance(this);

        intentFilter = new IntentFilter();
        intentFilter.addAction(MyApplication.ACTION_MAIN);
        intentFilter.addAction(MyApplication.ACTION_STAR_ACTIVITY);
        intentFilter.addAction(MyApplication.ACTION_FINISH_ACTIVITY);
        intentFilter.addAction(MyApplication.ACTION_REFRESH_ACTIVITY);
        intentFilter.addAction(MyApplication.ACTION_NETWORK_INFO_UPDATE);
        intentFilter.addAction(MyApplication.ACTION_POWER_INFO_UPDATE);
        intentFilter.addAction(MyApplication.ACTION_HARDFAULT_REBOOT);
        intentFilter.addAction(MyApplication.ACTION_NAMEPLATE_UPDATE);
        intentFilter.addAction(MyApplication.ACTION_REFRESH_STABAR);
    }

    //注册广播
    protected void registerBroadcast(){
        localBroadcast.registerReceiver(broadcastReceiver,intentFilter);
    }

    //广播注册机
    protected BroadcastReceiver broadcastReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            broadcastReceiveProcessor(intent);
        }
    };

    protected void unregisterBroadcast(){
        localBroadcast.unregisterReceiver(broadcastReceiver);
    }

    //广播处理器
    protected void broadcastReceiveProcessor(Intent intent){
        //启动Activity广播
        if (intent.getAction().equals(MyApplication.ACTION_STAR_ACTIVITY) && isActivityTop()) {

            Class<?> cla = (Class<?>) intent.getExtras().get("CLASS");
            if (!cla.equals(this.getClass())) {
//                Log.d(TAG, "activity is already launch");
                Intent starActIntent = new Intent(this, (Class<?>) intent.getExtras().get("CLASS"));
                starActIntent.addFlags(Intent.FLAG_ACTIVITY_REORDER_TO_FRONT);
                startActivity(starActIntent);
            }

        }

        //关闭Activity广播
        else if (intent.getAction().equals(MyApplication.ACTION_FINISH_ACTIVITY)
                && isActivityTop()
                && getClass().equals(intent.getExtras().get("CLASS"))) {

            this.finish();

        }

        //刷新Activity广播
        else if (intent.getAction().equals(MyApplication.ACTION_REFRESH_ACTIVITY)
                && isActivityTop()
                && (getClass().equals(intent.getExtras().get("CLASS")) || intent.getExtras().get("CLASS") == null)) {

//            onResume();
            uiRefresh();
        }

        //刷新状态栏广播
        else if (intent.getAction().equals(MyApplication.ACTION_REFRESH_STABAR)
                && isActivityTop()) {

            stateBar.configStatusBar();
        }

        //网络状态更新广播
        else if(intent.getAction().equals(MyApplication.ACTION_NETWORK_INFO_UPDATE)){
            Bundle bundle = intent.getBundleExtra("BUNDLE");
            String devName = bundle.getString(NET_DRIVE_NAME);

            if(devName.equals(WifiManager.DEV_NAME)) {
                stateBar.setEthInfoDisplay(false);
                stateBar.setWifiInfoDisplay(bundle.getBoolean(NETWORK_EN), bundle.getInt(WIFI_RSSI));
            }
            else if(devName.equals(EthernetManager.DEV_NAME)) {
                stateBar.setWifiInfoDisplay(false,0);
                stateBar.setEthInfoDisplay(bundle.getBoolean(NETWORK_EN));
            }
        }

        //电量状态更新广播
        else if(intent.getAction().equals(MyApplication.ACTION_POWER_INFO_UPDATE)){
            Bundle bundle = intent.getBundleExtra("BUNDLE");
            stateBar.setPowMode(bundle.getInt(PowerManager.POWER_MODE));
            stateBar.setBatLevel(bundle.getInt(PowerManager.BAT_LEVEL),bundle.getBoolean(PowerManager.BAT_CHARGE),bundle.getInt(PowerManager.BAT_VOLTAGE)!=0);
        }

        //重启广播
        else if(intent.getAction().equals(MyApplication.ACTION_HARDFAULT_REBOOT)){
                    Cmd.execCmd("reboot");
        }

        //铭牌更新
        else if(intent.getAction().equals(MyApplication.ACTION_NAMEPLATE_UPDATE) && isActivityTop()){

            String filePath = intent.getStringExtra("STRING");
            Log.d(TAG,"filePath = " + filePath);
            nameplateManager.para.setNpType(NameplateManager.TYPE_RDY_MADE_PIC);
            nameplateManager.para.setNpImg(filePath);
            nameplateManager.update(this);
        }

    }

//    @Override
//    public boolean dispatchTouchEvent(MotionEvent ev) {
//        powerManager.resetSavePowerTime();
//        return super.dispatchTouchEvent(ev);
//    }

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
                            // Log.i("meetContent", meetContent.meetName + ":"
                            // + meetContent.promText + "num=" + meetContent.time
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
//			Log.i("removePrompt", meetName);
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
        private DatabaseManager databaseManager;
        private NameplateManager nameplateManager;

        @Override
        public void handleMessage(Message msg) {
            Bundle bundle = msg.getData();
            switch (msg.what) {
                case MEETING_INFO:
//                    meetName = databaseManager.getMeetName();
//                    meetSlogan = databaseManager.getMeetSlogan();
//                    meetContent = databaseManager.getMeetContent();
//                    meetStartTime = databaseManager.getMeetStartTime();
//                    meetEndTime = databaseManager.getMeetEndTime();
                    Log.d("AppActivity","reflash activity");
                    MyApplication.LocalBroadcast.send(MyApplication.ACTION_REFRESH_ACTIVITY, MeetingInfoActivity.class);
                    MyApplication.LocalBroadcast.send(MyApplication.ACTION_REFRESH_ACTIVITY, MainActivity.class);
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
                    databaseManager = DatabaseManager.getDatabaseManager();
                    oldSMS = databaseManager.getSmsMsg();
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
                    newSMS.add(smsStr);
                    PromptBox.BuildPrompt("YOU_HAVE_A_SMS").Text("你有一条来自：" + smsStr[0]+ "的新消息").Time(1).TimeOut(5000);
                    MyApplication.LocalBroadcast.send(MyApplication.ACTION_REFRESH_ACTIVITY, SmsActivity.class);
                    MyApplication.LocalBroadcast.send(MyApplication.ACTION_REFRESH_STABAR);
                    databaseManager.saveSmsMsg(smsStr[0],smsStr[1]);
                    break;
                case SYS_SMS_MSG:
                    databaseManager = DatabaseManager.getDatabaseManager();
                    adminMsg = databaseManager.getAdminMsg();
                    String strContent = bundle.getString("strContent");
                    adminMsg.add(strContent);
                    newAdminMsgCount ++;
                    PromptBox.BuildPrompt("YOU_HAVE_A_SMS").Text("你有一条来自管理员的新消息").Time(1).TimeOut(5000);
                    MyApplication.LocalBroadcast.send(MyApplication.ACTION_REFRESH_ACTIVITY, MeetingInfoActivity.class);
                    MyApplication.LocalBroadcast.send(MyApplication.ACTION_REFRESH_STABAR);
                    databaseManager.saveAdminMsg(strContent);
                    break;
                case MEETING_END:
                    databaseManager = DatabaseManager.getDatabaseManager();
                    nameplateManager = NameplateManager.getNameplateManager();

                    PromptBox.BuildPrompt("MEETING_END").Text("会议已结束").Time(1).TimeOut(5000);

                    databaseManager.delMeetInfo();
                    if(newSMS != null && !newSMS.isEmpty())
                        newSMS.clear();
                    if(userList != null && !userList.isEmpty())
                        userList.clear();
                    newAdminMsgCount = 0;

                    nameplateManager.setDefaultNameplate();
                    nameplateManager.para.setNpType(NAMEPLATE_STYLE_DEF);

                    Cmd.execCmd("rm -rf "+NAMEPLATE_IMG_PATH);
                    Cmd.execCmd("rm -rf "+NAMEPLATE_BACKGROUND_PATH);
                    Cmd.execCmd("rm -rf "+NAMEPLATE_BIN_FILE_PATH+NAMEPLATE_BIN_FILE_NAME);

                    MyApplication.LocalBroadcast.send(MyApplication.ACTION_REFRESH_ACTIVITY, SmsActivity.class);
                    MyApplication.LocalBroadcast.send(MyApplication.ACTION_REFRESH_ACTIVITY, ShowNameActivity.class);
                    MyApplication.LocalBroadcast.send(MyApplication.ACTION_REFRESH_ACTIVITY, MeetingInfoActivity.class);
                    MyApplication.LocalBroadcast.send(MyApplication.ACTION_REFRESH_ACTIVITY, MainActivity.class);
                    break;
                case USER_INFO:
                    MyApplication.LocalBroadcast.send(MyApplication.ACTION_REFRESH_ACTIVITY, EditUserInfoActivity.class);
                    MyApplication.LocalBroadcast.send(MyApplication.ACTION_REFRESH_ACTIVITY, MainActivity.class);
                    break;

            }
        }
    };
}
