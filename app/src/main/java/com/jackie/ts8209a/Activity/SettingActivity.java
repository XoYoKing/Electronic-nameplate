package com.jackie.ts8209a.Activity;

import android.content.Intent;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.view.animation.TranslateAnimation;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.RadioGroup;
import android.widget.RelativeLayout;
import android.widget.SeekBar;
import android.widget.TextView;

import com.jackie.ts8209a.AppModule.APP;
import com.jackie.ts8209a.AppModule.Network.NetDevManager;
import com.jackie.ts8209a.AppModule.Network.WifiManager;
import com.jackie.ts8209a.AppModule.Tools.Cmd;
import com.jackie.ts8209a.AppModule.Tools.General;
import com.jackie.ts8209a.CustomView.Dialog.IpConfig;
import com.jackie.ts8209a.R;

import static com.jackie.ts8209a.AppModule.Network.NetworkManager.DEV_NAME;
import static com.jackie.ts8209a.AppModule.Network.NetworkManager.GATEWAY;
import static com.jackie.ts8209a.AppModule.Network.NetworkManager.IPADDR;
import static com.jackie.ts8209a.AppModule.Network.NetworkManager.MAC;
import static com.jackie.ts8209a.AppModule.Network.NetworkManager.NETMASK;
import static com.jackie.ts8209a.AppModule.Network.NetworkManager.RSSI;
import static com.jackie.ts8209a.AppModule.Network.NetworkManager.SSID;
import static com.jackie.ts8209a.AppModule.Tools.General.addrIntToStr;

/**
 * Created by kuangyt on 2018/8/21.
 */

public class SettingActivity extends AppActivity implements RadioGroup.OnCheckedChangeListener,View.OnClickListener {

    private final int[] layItemId = {R.id.setting_item_network_lay,R.id.setting_item_light_lay,R.id.setting_item_app_config_lay};
    private RelativeLayout[] layItem = new RelativeLayout[layItemId.length];
    private final String[] language = {"zh","en","hk"};

    private View ivItemChoice;

    private LinearLayout layWifiCfgTitle;
    private LinearLayout layWifiCfgContent;

    private TextView tvDevId;
    private TextView tvServIp;
    private TextView tvServPort;
    private TextView tvLocalIp;
    private TextView tvNetType;
    private TextView tvGatewey;
    private TextView tvNetmask;
    private TextView tvSsid;
    private TextView tvRssi;
    private TextView tvMac;

    private Button btnServIpCfg;
    private Button btnLocalIpCfg;
    private Button btnWifiCfg;
    private Button btnWifiDisconnect;

    private SeekBar sbBrightness;
    private TextView tvBrightness;
    private Button btnLanguage;
    private TextView tvVersion;
    private TextView tvSysVersion;

    //admin
    private Button btnSwitchDevMode;
    private Button btnAppUpdating;
    private LinearLayout layAdminMode;

    //save power
//    private Button btnSavePower;
//    private Button btnNormalPower;
//    private TextView tvCpuFreq;
//    private TextView tvCpuPowerMode;

//    private Timer wifiUpdateTime;

    private int location = 0;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_setting);

        //页面选项
        for(int i=0;i<layItemId.length;i++){
            layItem[i] = (RelativeLayout)findViewById(layItemId[i]);
        }

        ivItemChoice = findViewById(R.id.setting_item_choice_iv);
        ((RadioGroup)findViewById(R.id.setting_item_choice_rg)).setOnCheckedChangeListener(this);

        //IP地址设置页面相关引用初始化
        tvDevId = ((TextView)findViewById(R.id.setting_dev_id_num_tv));
        tvServIp = ((TextView)findViewById(R.id.setting_serv_ip_num_tv));
        tvServPort = ((TextView)findViewById(R.id.setting_serv_port_num_tv));
        tvLocalIp = ((TextView)findViewById(R.id.setting_local_ip_tv));
        tvNetType = ((TextView)findViewById(R.id.setting_net_type_tv));
        tvGatewey = ((TextView)findViewById(R.id.setting_gateway_tv));
        tvNetmask = ((TextView)findViewById(R.id.setting_netmask_tv));
        tvSsid = ((TextView)findViewById(R.id.setting_ssid_tv));
        tvRssi = ((TextView)findViewById(R.id.setting_rssi_tv));
        tvMac = ((TextView)findViewById(R.id.setting_mac_tv));
        btnServIpCfg = (Button)findViewById(R.id.setting_serv_ip_cfg_btn);
        btnLocalIpCfg = (Button)findViewById(R.id.setting_local_ip_btn);
        btnWifiCfg = (Button)findViewById(R.id.setting_wifi_cfg_btn);
        btnWifiDisconnect = (Button)findViewById(R.id.setting_wifi_disconnect_btn);
        layWifiCfgTitle = (LinearLayout)findViewById(R.id.setting_wifi_cfg_title_lay);
        layWifiCfgContent = (LinearLayout)findViewById(R.id.setting_wifi_content_lay);

        //亮度页面相关引用初始化
        sbBrightness = (SeekBar)findViewById(R.id.setting_item_brightness_sb);
        tvBrightness = (TextView)findViewById(R.id.setting_item_brightness_tv);

        //应用配置页面相关引用初始化
        tvVersion = (TextView)findViewById(R.id.setting_version_num_tv);
        tvSysVersion = (TextView)findViewById(R.id.setting_sys_version_num_tv);
        btnLanguage = (Button)findViewById(R.id.setting_language_btn);


        //应用配置->管理员页面相关引用初始化
        layAdminMode = (LinearLayout) findViewById(R.id.setting_admin_config_lay);
        btnSwitchDevMode = (Button)findViewById(R.id.setting_switch_dev_mode_btn);
        btnAppUpdating = (Button)findViewById(R.id.setting_app_updating_btn);


        //save power
//        btnSavePower = (Button)findViewById(R.id.setting_cpu_savepower_btn);
//        btnNormalPower = (Button)findViewById(R.id.setting_cpu_normalpower_btn);
//        tvCpuFreq = (TextView)findViewById(R.id.setting_cpu_freq_tv);
//        tvCpuPowerMode = (TextView)findViewById(R.id.setting_cpu_power_mode_tv);
    }

    @Override
    protected void onResume() {
        super.onResume();

        //IP地址参数设置->服务器参数
        tvDevId.setText(String.valueOf(userInfo.getDeviceID()));
        tvServIp.setText(addrIntToStr(userInfo.getServIp()));
        tvServPort.setText(String.valueOf(userInfo.getServPort()));
        //IP地址参数设置->本地参数
        tvNetType.setText(netDevInfo.getNetType() == NetDevManager.TYPE_WIER_NET ?
                            getResources().getString(R.string.wire_network) :
                            getResources().getString(R.string.wireless_network));
        tvLocalIp.setText(addrIntToStr(netDevInfo.getIp()));
        tvGatewey.setText(addrIntToStr(netDevInfo.getGw()));
        tvNetmask.setText(addrIntToStr(netDevInfo.getMask()));

        int type = netDevInfo.getNetType();
        if(type == NetDevManager.TYPE_WIER_NET){
            layWifiCfgContent.setVisibility(View.GONE);
            layWifiCfgTitle.setVisibility(View.GONE);
            btnWifiCfg.setVisibility(View.GONE);
            btnWifiDisconnect.setVisibility(View.GONE);
        }else if(type == NetDevManager.TYPE_WIRELESS){
            layWifiCfgContent.setVisibility(View.VISIBLE);
            layWifiCfgTitle.setVisibility(View.VISIBLE);

            tvSsid.setText(netDevInfo.getSsid());
            tvRssi.setText(String.valueOf(netDevInfo.getRssi()));
            tvMac.setText(netDevInfo.getMac());

            btnWifiCfg.setVisibility(View.VISIBLE);
            btnWifiDisconnect.setVisibility(View.VISIBLE);

            btnWifiCfg.setOnClickListener(this);
            btnWifiDisconnect.setOnClickListener(this);
        }
        btnServIpCfg.setOnClickListener(this);
        btnLocalIpCfg.setOnClickListener(this);

        //亮度监听器&参数
        int brightness = userInfo.getBrightness();
        sbBrightness.setProgress(brightness);
        tvBrightness.setText(String.format("%02d", brightness));
        sbBrightness.setOnSeekBarChangeListener(new lightSeekBarChangeListener());

        //应用配置参数及监听器
        try {
            PackageInfo packInfo = getPackageManager().getPackageInfo("com.jackie.ts8209a", PackageManager.GET_CONFIGURATIONS);
            tvVersion.setText(packInfo.versionName + " v" + packInfo.versionCode);
            tvSysVersion.setText(Build.DISPLAY);
            btnLanguage.setOnClickListener(this);
            versionTextViewContinuousClickListener vtListener = new versionTextViewContinuousClickListener();
            tvVersion.setOnClickListener(vtListener);
            tvSysVersion.setOnClickListener(vtListener);
        } catch (PackageManager.NameNotFoundException e) {
            e.printStackTrace();
        }

//        btnSavePower.setOnClickListener(this);
//        btnNormalPower.setOnClickListener(this);
    }

    @Override
    public void onClick(View view) {
        IpConfig.ipConfigPara para;
        IpConfig.Builder builder;
        IpConfig dialog;

        switch (view.getId()) {
            case R.id.setting_serv_ip_cfg_btn:
                para = new IpConfig.ipConfigPara();
                para.devId = userInfo.getDeviceID();
                para.servIP = userInfo.getServIp();
                para.servPort = userInfo.getServPort();

                builder = new IpConfig.Builder(SettingActivity.this);
                dialog = builder.setType(IpConfig.TYPE_SERV).setOnIpConfigConfirmListener(new IpConfig.OnIpConfigConfirmListener() {
                    @Override
                    public void ipConfigConfirm(IpConfig.ipConfigPara para) {
                        tvDevId.setText(String.valueOf(para.devId));
                        tvServIp.setText(General.addrIntToStr(para.servIP));
                        tvServPort.setText(String.valueOf(para.servPort));

                        userInfo.setServIp(para.servIP);
                        userInfo.setServPort(para.servPort);
                        userInfo.setDeviceId(para.devId);

                        networkManager.sendDevInfo();
                        networkManager.resetNetwork();

                    }
                }).creatIpConfig(para);
                dialog.show();
                break;
            case R.id.setting_local_ip_btn:
                para = new IpConfig.ipConfigPara();

                para.dhcp = userInfo.getDhcp();
                if (para.dhcp) {
                    para.localIP = netDevInfo.getIp();
                    para.gateway = netDevInfo.getGw();
                    para.mask = netDevInfo.getMask();
                } else {
                    para.localIP = userInfo.getLocalIp();
                    para.gateway = userInfo.getGateway();
                    para.mask = userInfo.getMask();
                }

                builder = new IpConfig.Builder(SettingActivity.this);
                dialog = builder.setType(IpConfig.TYPE_LOCAL).setOnIpConfigConfirmListener(new IpConfig.OnIpConfigConfirmListener() {
                    @Override
                    public void ipConfigConfirm(IpConfig.ipConfigPara para) {
                        userInfo.setDhcp(para.dhcp);
                        if (para.dhcp) {
                            networkManager.setDhcpEn();

                            tvLocalIp.setText(General.addrIntToStr(netDevInfo.getIp()));
                            tvGatewey.setText(General.addrIntToStr(netDevInfo.getGw()));
                            tvNetmask.setText(General.addrIntToStr(netDevInfo.getMask()));
                        } else {
                            networkManager.setNetworkInfo(para.localIP,para.mask,para.gateway);

                            userInfo.setLocalIp(para.localIP);
                            userInfo.setGateway(para.gateway);
                            userInfo.setMask(para.mask);

                            tvLocalIp.setText(General.addrIntToStr(para.localIP));
                            tvGatewey.setText(General.addrIntToStr(para.gateway));
                            tvNetmask.setText(General.addrIntToStr(para.mask));
                        }
                        networkManager.resetNetwork();
                    }
                }).creatIpConfig(para);
                dialog.show();
                break;
            case R.id.setting_wifi_cfg_btn:
                networkManager.creatWifiSelection(this);
                break;
            case R.id.setting_wifi_disconnect_btn:
                networkManager.removeAllWifi();
                break;
        }
    }

    @Override
    public void onCheckedChanged(RadioGroup group, int checkId) {
        for (int i = 0; i < layItem.length; i++) {
            layItem[i].setVisibility(View.GONE);
        }
        int lay = 0;
        int offset = 0;
        switch (checkId) {
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
            case R.id.setting_item_app_config_rbtn:
                lay = 2;
                offset = 344 - location;
                location = 344;
                break;
        }
        TranslateAnimation anim = new TranslateAnimation(0 - offset, 0, 0, 0);
        anim.setDuration(300);
        ivItemChoice.setAnimation(anim);
        ivItemChoice.scrollTo(0 - location, 0);
        layItem[lay].setVisibility(View.VISIBLE);
    }

    @Override
    protected void broadcastProcessing(Intent intent) {
        super.broadcastProcessing(intent);

        if(intent.getAction().equals(APP.ACTION_NETWORK_INFO_UPDATE)){
            Bundle bundle = intent.getBundleExtra("BUNDLE");
            String devName = bundle.getString(DEV_NAME);

            if(devName.equals(WifiManager.DEV_NAME)) {
                tvRssi.setText(String.valueOf(bundle.getInt(RSSI)));
                tvSsid.setText(bundle.getString(SSID));
                tvMac.setText(bundle.getString(MAC));
            }
            tvLocalIp.setText(General.addrIntToStr(bundle.getIntArray(IPADDR)));
            tvGatewey.setText(General.addrIntToStr(bundle.getIntArray(GATEWAY)));
            tvNetmask.setText(General.addrIntToStr(bundle.getIntArray(NETMASK)));
        }else if(intent.getAction().equals(APP.ACTION_POWER_INFO_UPDATE)){
            Bundle bundle = intent.getBundleExtra("BUNDLE");
//            tvCpuPowerMode.setText(bundle.getBoolean(PowerManager.SAVE_POWER) ? "savepower" : "normalpower");
//            tvCpuFreq.setText(bundle.getString(PowerManager.CPU_FREQ));
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

    private void setSysBrightness(final int brightness) {
        Cmd.execCmd("settings put system screen_brightness " + brightness);
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
        }

        @Override
        public void onStopTrackingTouch(SeekBar bar) {
//			Log.d(TAG,"onStop"+bar.toString());
            userInfo.setBrightness(num);
            setSysBrightness(num);
        }
    }

    private class versionTextViewContinuousClickListener implements View.OnClickListener {
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


    //    private class setLanguageButtonOnClickListener implements View.OnClickListener {
//        private final String[] langStr = { "简体中文", "English", "繁體中文" };
//
//        @Override
//        public void onClick(View view) {
////            AlertDialog.Builder Builder = new AlertDialog.Builder(ActivityContext);
////            Builder.setItems(langStr, new DialogInterface.OnClickListener() {
////
////                @Override
////                public void onClick(DialogInterface dialog, int witch) {
//////					Log.i("Language select", "" + witch);
////                    userInfo.setLanguage(language[witch]);
////                    try {
////                        Intent intent = new Intent(ActivityContext, MainActivity.class).setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP
////                                | Intent.FLAG_ACTIVITY_NEW_TASK);
////                        ActivityContext.startActivity(intent);
////                    } catch (Exception e) {}
////                }
////            });
//////			Builder.setSingleChoiceItems(langStr, langChecked, );
////            AlertDialog langPicker = Builder.create();
////            langPicker.show();
//        }
//
//    }

//    private class ipConfigOnClickListener implements View.OnClickListener{
//
//        @Override
//        public void onClick(View v) {
//            IpConfig.ipConfigPara para = new IpConfig.ipConfigPara();
//            para.devId = userInfo.getDeviceID();
//            para.servIP = userInfo.getServIp();
//            para.servPort = userInfo.getServPort();
//            para.localIP = userInfo.getLocalIp();
//            para.dhcp = userInfo.getDhcp();
//
//            IpConfig.Builder builder = new IpConfig.Builder(SettingActivity.this);
//            IpConfig dialog = builder.setOnIpConfigConfirmListener(new IpConfig.OnIpConfigConfirmListener() {
//                @Override
//                public void ipConfigConfirm(IpConfig.ipConfigPara para) {
//                    tvDevId.setText(String.valueOf(para.devId));
//                    tvServIp.setText(para.servIP[0]+"."+para.servIP[1]+"."+para.servIP[2]+"."+para.servIP[3]);
//                    tvLocalIp.setText(para.localIP[0]+"."+para.localIP[1]+"."+para.localIP[2]+"."+para.localIP[3]);
//                    tvServPort.setText(String.valueOf(para.servPort));
////                    tvDhcp.setText(para.dhcp ? "启用" : "未启用");
//
//                    userInfo.setServIp(para.servIP);
//                    userInfo.setLocalIp(para.localIP);
//                    userInfo.setServPort(para.servPort);
//                    userInfo.setDeviceId(para.devId);
//                    userInfo.setDhcp(para.dhcp);
//
//                    networkManager.sendDevInfo();
//                    networkManager.resetNetwork();
////                    ethernetManager.setIp(para.localIP);
//
//                    int[] mask = {255,255,255,0};
//                    int[] gw = {para.localIP[0],para.localIP[1],para.localIP[2],254};
////                    ethernetManager.setNetDevInfo(para.localIP,mask,gw);
//                }
//            }).creatIpConfig(para);
//            dialog.show();
//        }
//    }
}
