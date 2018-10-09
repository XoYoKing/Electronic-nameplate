package com.jackie.ts8209a.Activity;

import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.os.RemoteException;
import android.util.Log;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.view.animation.TranslateAnimation;
import android.widget.Button;
import android.widget.CompoundButton;
import android.widget.LinearLayout;
import android.widget.RadioGroup;
import android.widget.RelativeLayout;
import android.widget.SeekBar;
import android.widget.Switch;
import android.widget.TextView;

import com.jackie.ts8209a.CustomView.Dialog.IpConfig;
import com.jackie.ts8209a.Managers.CmdManager;
import com.jackie.ts8209a.Managers.UserInfoManager;
import com.jackie.ts8209a.Managers.WifiManager;
import com.jackie.ts8209a.R;

import java.util.Timer;
import java.util.TimerTask;

/**
 * Created by kuangyt on 2018/8/21.
 */

public class SettingActivity extends AppActivity {

    private WifiManager wifiManager;
    private WifiManager.WifiInfo wifiInfo;

    private final int[] layItemId = {R.id.setting_item_ip_lay,R.id.setting_item_light_lay,R.id.setting_item_wifi_lay,R.id.setting_item_app_config_lay};
    private RelativeLayout[] layItem = new RelativeLayout[layItemId.length];
    private final String[] language = {"zh","en","hk"};

    private View ivItemChoice;

    private TextView tvDecId;
    private TextView tvServIp;
    private TextView tvServPort;
    private TextView tvLocalIp;
    private TextView tvDhcp;
    private Button btnIpconfig;

    private SeekBar sbBrightness;
    private TextView tvBrightness;
    private Button btnLanguage;
    private UserInfoManager userInfo;
    private TextView tvVersion;
    private TextView tvSysVersion;

    private Switch swWifi;
    private Button btnSsid;
    private TextView tvWifiMsg;

    //admin
    private Button btnSwitchDevMode;
    private Button btnAppUpdating;
    private LinearLayout layAdminMode;

    private Timer wifiUpdateTime;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_setting);

        try {
            wifiManager = WifiManager.getWifiManager();
            wifiInfo = wifiManager.getWifiInfo();
            userInfo = UserInfoManager.getUserInfoManager();
        } catch (Exception e) {
            e.printStackTrace();
        }

        //页面选项
        for(int i=0;i<layItemId.length;i++){
            layItem[i] = (RelativeLayout)findViewById(layItemId[i]);
        }

        ivItemChoice = findViewById(R.id.setting_item_choice_iv);
        ((RadioGroup)findViewById(R.id.setting_item_choice_rg)).setOnCheckedChangeListener(new settingItemChangeListener());

        //IP地址设置页面相关引用初始化
        tvDecId = ((TextView)findViewById(R.id.setting_dev_id_num_tv));
        tvServIp = ((TextView)findViewById(R.id.setting_serv_ip_num_tv));
        tvServPort = ((TextView)findViewById(R.id.setting_serv_port_num_tv));
        tvLocalIp = ((TextView)findViewById(R.id.setting_local_ip_num_tv));
        tvDhcp = (TextView)findViewById(R.id.setting_local_dhcp_tv);
        btnIpconfig = (Button)findViewById(R.id.setting_ip_config_btn);

        //亮度页面相关引用初始化
        sbBrightness = (SeekBar)findViewById(R.id.setting_item_brightness_sb);
        tvBrightness = (TextView)findViewById(R.id.setting_item_brightness_tv);

        //wifi设置页面相关引用初始化
        swWifi = (Switch)findViewById(R.id.setting_wifi_sw);
        btnSsid = (Button)findViewById(R.id.setting_ssid_btn);
        tvWifiMsg = (TextView)findViewById(R.id.setting_wifi_msg_tv);

        //应用配置页面相关引用初始化
        tvVersion = (TextView)findViewById(R.id.setting_version_num_tv);
        tvSysVersion = (TextView)findViewById(R.id.setting_sys_version_num_tv);
        btnLanguage = (Button)findViewById(R.id.setting_language_btn);


        //应用配置->管理员页面相关引用初始化
        layAdminMode = (LinearLayout) findViewById(R.id.setting_admin_config_lay);
        btnSwitchDevMode = (Button)findViewById(R.id.setting_switch_dev_mode_btn);
        btnAppUpdating = (Button)findViewById(R.id.setting_app_updating_btn);

        wifiUpdateTime = new Timer();
    }

    @Override
    protected void onResume() {
        super.onResume();

        //IP地址参数设置
        int[] servIp = userInfo.getServIp();
        int[] localIp = userInfo.getLocalIp();

        tvDecId.setText(userInfo.getDeviceID() + "");
        tvServIp.setText(servIp[0] + "." + servIp[1] + "." + servIp[2] + "." + servIp[3]);
        tvServPort.setText(userInfo.getServPort() + "");
        tvLocalIp.setText(localIp[0] + "." + localIp[1] + "." + localIp[2] + "." + localIp[3]);
        tvDhcp.setText(userInfo.getDhcp() ? "启用" : "未启用");
        btnIpconfig.setOnClickListener(new ipConfigOnClickListener());

        //亮度监听器&参数
        int brightness = userInfo.getBrightness();
        sbBrightness.setProgress(brightness);
        tvBrightness.setText(String.format("%02d", brightness));
        sbBrightness.setOnSeekBarChangeListener(new lightSeekBarChangeListener());

        //wifi设置监听器
        btnSsid.setOnClickListener(new wifiSelectClickListener());
        swWifi.setOnCheckedChangeListener(new wifiSwitchListener());

        //应用配置参数及监听器
        try {
            PackageInfo packInfo = getPackageManager().getPackageInfo("com.jackie.ts8209a", PackageManager.GET_CONFIGURATIONS);
            tvVersion.setText(packInfo.versionName + " v" + packInfo.versionCode);
            tvSysVersion.setText(Build.DISPLAY);
            btnLanguage.setOnClickListener(new setLanguageButtonOnClickListener());
            versionTextViewOnClickListener vtListener = new versionTextViewOnClickListener();
            tvVersion.setOnClickListener(vtListener);
            tvSysVersion.setOnClickListener(vtListener);
        } catch (PackageManager.NameNotFoundException e) {
            e.printStackTrace();
        }

        wifiUpdateTime.schedule(wifiMsgUpdateTask,0,5000);
    }

    @Override
    protected void onPause() {
        super.onPause();

        wifiUpdateTime.cancel();
    }

    private Handler wifiMsgUpdate = new Handler() {
        @Override
        public void handleMessage(Message msg) {
            swWifi.setChecked(wifiInfo.wifiEnable());
            int[] ip = wifiInfo.IP();
            String wifiMsg = "SSID :" + wifiInfo.SSID() + "\nRSSI :" + wifiInfo.RSSI() + "\nMAC :" + wifiInfo.MAC() +
                    "\nIP :" + ip[0] + "." + ip[1] + "." + ip[2] + "." + ip[3];
            tvWifiMsg.setText(wifiMsg);
        }
    };

    private TimerTask wifiMsgUpdateTask = new TimerTask() {
        @Override
        public void run() {
            wifiMsgUpdate.sendEmptyMessage(0);
        }
    };

    private class wifiSelectClickListener implements View.OnClickListener {

        @Override
        public void onClick(View v) {
            Message msg = wifiManager.handler.obtainMessage();
            msg.what = 1;
            msg.obj = SettingActivity.this;
            wifiManager.handler.sendMessage(msg);
        }
    }

    private class wifiSwitchListener implements CompoundButton.OnCheckedChangeListener{

        @Override
        public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
            if(isChecked){
                wifiManager.startup();
            }else{
                wifiManager.shutdown();
            }
        }
    }

    private class settingItemChangeListener implements RadioGroup.OnCheckedChangeListener {
        private int location = 0;

        @Override
        public void onCheckedChanged(RadioGroup group, int checkId) {
            for(int i=0;i<layItem.length;i++){
                layItem[i].setVisibility(View.GONE);
            }
            int lay = 0;
            int offset = 0;
            switch(checkId){
                case R.id.setting_item_ip_rbtn:
                    lay = 0;
                    offset = 0 - location;
                    location = 0;
                    break;
                case R.id.setting_item_light_rbtn:
                    lay = 1;
                    offset = 172 - location;
                    location = 172;
                    break;
                case R.id.setting_item_wifi_rbtn:
                    lay = 2;
                    offset = 344 - location;
                    location = 344;
                    break;
                case R.id.setting_item_app_config_rbtn:
                    lay = 3;
                    offset = 516 - location;
                    location = 516;
                    break;
            }
            TranslateAnimation anim = new TranslateAnimation(0 - offset, 0, 0, 0);
            anim.setDuration(300);
            ivItemChoice.setAnimation(anim);
            ivItemChoice.scrollTo(0 - location, 0);
            layItem[lay].setVisibility(View.VISIBLE);
        }
    }

    private void setBrightness(int brightness) {
        Window window = this.getWindow();
        WindowManager.LayoutParams lp = window.getAttributes();
        if (brightness == -1) {
            lp.screenBrightness = WindowManager.LayoutParams.BRIGHTNESS_OVERRIDE_NONE;
        } else {
            lp.screenBrightness = (brightness <= 0 ? 1 : brightness) / 255f;
        }
        window.setAttributes(lp);
    }

    private void setSysBrightness(final int brightness){
        new Thread(){
            @Override
            public void run() {
                CmdManager.execCmdSilent("settings put system screen_brightness "+brightness);
            }
        }.start();

    }

    private class lightSeekBarChangeListener implements SeekBar.OnSeekBarChangeListener {
        private int num = 50;

        @Override
        public void onProgressChanged(SeekBar bar, int num, boolean bl) {
            tvBrightness.setText(String.format("%02d",num));
            setBrightness(num);
            this.num = num;
        }

        @Override
        public void onStartTrackingTouch(SeekBar bar) {
//			Log.d(TAG,"onStart"+bar.toString());
        }

        @Override
        public void onStopTrackingTouch(SeekBar bar) {
			Log.d(TAG,"onStop"+bar.toString());
            userInfo.setBrightness(num);
            setSysBrightness(num);
        }
    }

    private class versionTextViewOnClickListener implements View.OnClickListener {
        public boolean counting = false;
        public int count = 0;

        @Override
        public void onClick(View view) {
//			Log.i("versionTextView","OnClick");
            count++;
            if(count >= 5){
                count = 0;
//                PasswordDialog dialog = new PasswordDialog(ActivityContext);
//                dialog.setOnPwdConfirmListener(new OnPwdConfirmListener() {
//
//                    @Override
//                    public void onPweConfirm(String pwd) {
//                        if(pwd.equals(MainApplication.ADMIN_PWD)){
////							Log.i("ADMIN","admin mode");
//                            layAdminMode.setVisibility(View.VISIBLE);
//                        }
//                    }
//                });
//                dialog.show();
            }
            (new Thread(clickCount)).start();
        }

        private Runnable clickCount = new Runnable() {
            @Override
            public void run() {
                if(counting) return;
                else{
                    counting = true;
                    try {
                        Thread.sleep(2000);
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                    count = 0;
                    counting = false;
                }
            }
        };

    }

    private class setLanguageButtonOnClickListener implements View.OnClickListener {
        private final String[] langStr = { "简体中文", "English", "繁體中文" };

        @Override
        public void onClick(View view) {
//            AlertDialog.Builder Builder = new AlertDialog.Builder(ActivityContext);
//            Builder.setItems(langStr, new DialogInterface.OnClickListener() {
//
//                @Override
//                public void onClick(DialogInterface dialog, int witch) {
////					Log.i("Language select", "" + witch);
//                    userInfo.setLanguage(language[witch]);
//                    try {
//                        Intent intent = new Intent(ActivityContext, MainActivity.class).setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP
//                                | Intent.FLAG_ACTIVITY_NEW_TASK);
//                        ActivityContext.startActivity(intent);
//                    } catch (Exception e) {}
//                }
//            });
////			Builder.setSingleChoiceItems(langStr, langChecked, );
//            AlertDialog langPicker = Builder.create();
//            langPicker.show();
        }

    }

    private class ipConfigOnClickListener implements View.OnClickListener{

        @Override
        public void onClick(View v) {
            IpConfig.ipConfigPara para = new IpConfig.ipConfigPara();
            para.devId = userInfo.getDeviceID();
            para.servIP = userInfo.getServIp();
            para.servPort = userInfo.getServPort();
            para.localIP = userInfo.getLocalIp();
            para.dhcp = userInfo.getDhcp();

            IpConfig.Builder builder = new IpConfig.Builder(SettingActivity.this);
            IpConfig dialog = builder.setOnIpConfigConfirmListener(new IpConfig.OnIpConfigConfirmListener() {
                @Override
                public void ipConfigConfirm(IpConfig.ipConfigPara para) {
                    tvDecId.setText(String.valueOf(para.devId));
                    tvServIp.setText(para.servIP[0]+"."+para.servIP[1]+"."+para.servIP[2]+"."+para.servIP[3]);
                    tvLocalIp.setText(para.localIP[0]+"."+para.localIP[1]+"."+para.localIP[2]+"."+para.localIP[3]);
                    tvServPort.setText(String.valueOf(para.servPort));
                    tvDhcp.setText(para.dhcp ? "启用" : "未启用");

                    userInfo.setServIp(para.servIP);
                    userInfo.setLocalIp(para.localIP);
                    userInfo.setServPort(para.servPort);
                    userInfo.setDeviceId(para.devId);
                    userInfo.setDhcp(para.dhcp);

                    networkManager.sendDevInfo();
                    networkManager.resetNetwork();
                }
            }).creatIpConfig(para);
            dialog.show();
        }
    }
}
