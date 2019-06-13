package com.itc.ts8209a.activity;

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

import com.itc.ts8209a.activity.view.ButtonSelectorDialog;
import com.itc.ts8209a.app.MyApplication;
import com.itc.ts8209a.module.network.NetDevManager;
import com.itc.ts8209a.module.network.WifiManager;
import com.itc.ts8209a.widget.Cmd;
import com.itc.ts8209a.widget.General;
import com.itc.ts8209a.activity.view.IpConfig;
import com.itc.ts8209a.R;

import static com.itc.ts8209a.app.AppConfig.APP_NAME;
import static com.itc.ts8209a.app.AppConfig.DIM_SCREEN_TIME;
import static com.itc.ts8209a.module.network.NetworkManager.NET_DRIVE_NAME;
import static com.itc.ts8209a.module.network.NetworkManager.NETWORK_GATEWAY;
import static com.itc.ts8209a.module.network.NetworkManager.NETWORK_LOCAL_IP;
import static com.itc.ts8209a.module.network.NetworkManager.NETWORK_MAC;
import static com.itc.ts8209a.module.network.NetworkManager.NETWORK_MASK;
import static com.itc.ts8209a.module.network.NetworkManager.WIFI_RSSI;
import static com.itc.ts8209a.module.network.NetworkManager.WIFI_SSID;
import static com.itc.ts8209a.widget.General.addrIntToStr;

/**
 * Created by kuangyt on 2018/8/21.
 */

public class SettingActivity extends AppActivity implements RadioGroup.OnCheckedChangeListener,SeekBar.OnSeekBarChangeListener {

    private final int[] layItemId = {R.id.setting_item_network_lay,R.id.setting_item_light_lay,R.id.setting_item_app_config_lay};
    private RelativeLayout[] layItem = new RelativeLayout[layItemId.length];
    private final String[] language = {"zh","en","hk"};
    private int brightness = 50;
    private String[] strDimTime;

    private View ivItemChoice;

    private RelativeLayout layWifiCfg;
    private View viLayAdj1;
    private View viLayAdj2;
//    private LinearLayout layWifiCfgContent;

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
    private Button btnScreenDimTime;

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
        btnScreenDimTime = (Button)findViewById(R.id.setting_screen_dim_time_btn);
        layWifiCfg = (RelativeLayout)findViewById(R.id.setting_wifi_cfg_lay);
        viLayAdj1 = findViewById(R.id.setting_layout_adjust_1);
        viLayAdj2 = findViewById(R.id.setting_layout_adjust_2);
//        layWifiCfgContent = (LinearLayout)findViewById(R.id.setting_wifi_content_lay);

        //亮度页面相关引用初始化
        sbBrightness = (SeekBar)findViewById(R.id.setting_item_brightness_sb);
        tvBrightness = (TextView)findViewById(R.id.setting_item_brightness_tv);

        //应用配置页面相关引用初始化
        tvVersion = (TextView)findViewById(R.id.setting_app_version_num_tv);
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

        btnServIpCfg.setOnClickListener(this);
        btnLocalIpCfg.setOnClickListener(this);

        //亮度监听器
        sbBrightness.setOnSeekBarChangeListener(this);
        btnScreenDimTime.setOnClickListener(this);

        //应用配置参数及监听器
        try {
            PackageInfo packInfo = getPackageManager().getPackageInfo(APP_NAME, PackageManager.GET_CONFIGURATIONS);
            tvVersion.setText(packInfo.versionName + " v" + packInfo.versionCode);
            tvSysVersion.setText(Build.DISPLAY);
            btnLanguage.setOnClickListener(this);
            versionTextViewContinuousClickListener vtListener = new versionTextViewContinuousClickListener();
            tvVersion.setOnClickListener(vtListener);
            tvSysVersion.setOnClickListener(vtListener);
        } catch (PackageManager.NameNotFoundException e) {
            e.printStackTrace();
        }
    }

    @Override
    protected void uiRefresh() {
        super.uiRefresh();

        try {
            //IP地址参数设置->服务器参数
            tvDevId.setText(String.valueOf(databaseManager.getDeviceID()));
            tvServIp.setText(addrIntToStr(databaseManager.getServIp()));
            tvServPort.setText(String.valueOf(databaseManager.getServPort()));

            tvNetType.setText(netDevInfo.getNetType() == NetDevManager.TYPE_WIER_NET ?
                    getResources().getString(R.string.wire_network) :
                    getResources().getString(R.string.wireless_network));
            tvLocalIp.setText(addrIntToStr(netDevInfo.getIp()));
            tvGatewey.setText(addrIntToStr(netDevInfo.getGw()));
            tvNetmask.setText(addrIntToStr(netDevInfo.getMask()));

            int type = netDevInfo.getNetType();
            if (type == NetDevManager.TYPE_WIER_NET) {
                layWifiCfg.setVisibility(View.GONE);
                viLayAdj1.setVisibility(View.VISIBLE);
                viLayAdj2.setVisibility(View.VISIBLE);
            } else if (type == NetDevManager.TYPE_WIRELESS) {
                layWifiCfg.setVisibility(View.VISIBLE);
                viLayAdj1.setVisibility(View.GONE);
                viLayAdj2.setVisibility(View.GONE);

                tvSsid.setText(netDevInfo.getSsid());
                tvRssi.setText(String.valueOf(netDevInfo.getRssi()) + " db");

                btnWifiCfg.setVisibility(View.VISIBLE);
                btnWifiDisconnect.setVisibility(View.VISIBLE);

                btnWifiCfg.setOnClickListener(this);
                btnWifiDisconnect.setOnClickListener(this);
            }
            tvMac.setText(netDevInfo.getMac());
        }catch(Exception e){
            e.printStackTrace();
            MyApplication.LocalBroadcast.send(MyApplication.ACTION_HARDFAULT_REBOOT);
        }

        //亮度参数
        int brightness = databaseManager.getBrightness();
        sbBrightness.setProgress(brightness);
        tvBrightness.setText(String.format("%02d", brightness));
        //关屏幕时间
        strDimTime = new String[DIM_SCREEN_TIME.length];
        for(int i=0;i<DIM_SCREEN_TIME.length;i++){
            strDimTime[i] = (DIM_SCREEN_TIME[i] == Integer.MAX_VALUE) ? (getString(R.string.never)) :
                    (DIM_SCREEN_TIME[i] < 60) ? (DIM_SCREEN_TIME[i] + getString(R.string.second)) :
                            (DIM_SCREEN_TIME[i] < 3600) ? ((DIM_SCREEN_TIME[i] / 60) + getString(R.string.minute)) :
                                    ((DIM_SCREEN_TIME[i] / (60 * 60)) + getString(R.string.hour));
            if(databaseManager.getScreenDimTime() == DIM_SCREEN_TIME[i])
                btnScreenDimTime.setText(strDimTime[i]);
        }
    }

    @Override
    public void onClick(View view) {
        super.onClick(view);

        IpConfig.ipConfigPara para;
        IpConfig.Builder builder;
        IpConfig dialog;

        switch (view.getId()) {
            case R.id.setting_serv_ip_cfg_btn:
                para = new IpConfig.ipConfigPara();
                para.devId = databaseManager.getDeviceID();
                para.servIP = databaseManager.getServIp();
                para.servPort = databaseManager.getServPort();

                builder = new IpConfig.Builder(SettingActivity.this);
                dialog = builder.setType(IpConfig.TYPE_SERV).setOnIpConfigConfirmListener(new IpConfig.OnIpConfigConfirmListener() {
                    @Override
                    public void ipConfigConfirm(IpConfig.ipConfigPara para) {
                        int id = databaseManager.getDeviceID();

                        tvDevId.setText(String.valueOf(para.devId));
                        tvServIp.setText(General.addrIntToStr(para.servIP));
                        tvServPort.setText(String.valueOf(para.servPort));

                        databaseManager.setServIp(para.servIP)
                                .setServPort(para.servPort)
                                .setDevId(para.devId)
                                .save();

                        networkManager.sendDevInfo();
                        networkManager.resetNetwork();

                        if(id != para.devId)
                            databaseManager.defMeetInfo();

                    }
                }).creatIpConfig(para);
                dialog.show();
                break;
            case R.id.setting_local_ip_btn:
                para = new IpConfig.ipConfigPara();

                para.dhcp = databaseManager.getDhcp();
                if (para.dhcp) {
                    para.localIP = netDevInfo.getIp();
                    para.gateway = netDevInfo.getGw();
                    para.mask = netDevInfo.getMask();
                } else {
                    para.localIP = databaseManager.getLocalIp();
                    para.gateway = databaseManager.getGateway();
                    para.mask = databaseManager.getMask();
                }

                builder = new IpConfig.Builder(SettingActivity.this);
                dialog = builder.setType(IpConfig.TYPE_LOCAL).setOnIpConfigConfirmListener(new IpConfig.OnIpConfigConfirmListener() {
                    @Override
                    public void ipConfigConfirm(IpConfig.ipConfigPara para) {
                        databaseManager.setDhcp(para.dhcp);
                        if (para.dhcp) {
                            networkManager.setDhcpEn();

                            tvLocalIp.setText(General.addrIntToStr(netDevInfo.getIp()));
                            tvGatewey.setText(General.addrIntToStr(netDevInfo.getGw()));
                            tvNetmask.setText(General.addrIntToStr(netDevInfo.getMask()));
                        } else {
                            networkManager.setNetworkInfo(para.localIP,para.mask,para.gateway);

                            databaseManager.setLocalIp(para.localIP)
                                    .setGateway(para.gateway)
                                    .setMask(para.mask);

                            tvLocalIp.setText(General.addrIntToStr(para.localIP));
                            tvGatewey.setText(General.addrIntToStr(para.gateway));
                            tvNetmask.setText(General.addrIntToStr(para.mask));
                        }
                        databaseManager.save();
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
            case R.id.setting_screen_dim_time_btn:
                ButtonSelectorDialog timeDialog = new ButtonSelectorDialog(this,strDimTime);
                timeDialog.setOnSelectConfirmListener(new ButtonSelectorDialog.OnSelectConfirmListener() {
                    @Override
                    public void selectConfirm(int choice, String content) {
                        Cmd.execCmd("settings put system screen_off_timeout " +(DIM_SCREEN_TIME[choice]==Integer.MAX_VALUE ? DIM_SCREEN_TIME[choice] : DIM_SCREEN_TIME[choice] * 1000));
                        btnScreenDimTime.setText(strDimTime[choice]);
                        databaseManager.setScreenDimTime(DIM_SCREEN_TIME[choice]).save();
                    }
                }).setTitle(getString(R.string.dim_screen_time)).show();
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
    protected void broadcastReceiveProcessor(Intent intent) {
        super.broadcastReceiveProcessor(intent);

        if(intent.getAction().equals(MyApplication.ACTION_NETWORK_INFO_UPDATE)){
            Bundle bundle = intent.getBundleExtra("BUNDLE");
            String devName = bundle.getString(NET_DRIVE_NAME);

            if(devName.equals(WifiManager.DEV_NAME)) {
                tvRssi.setText(String.valueOf(bundle.getInt(WIFI_RSSI)) + " db");
                tvSsid.setText(bundle.getString(WIFI_SSID));
            }
            tvMac.setText(bundle.getString(NETWORK_MAC));
            tvLocalIp.setText(General.addrIntToStr(bundle.getIntArray(NETWORK_LOCAL_IP)));
            tvGatewey.setText(General.addrIntToStr(bundle.getIntArray(NETWORK_GATEWAY)));
            tvNetmask.setText(General.addrIntToStr(bundle.getIntArray(NETWORK_MASK)));
        }

        else if(intent.getAction().equals(MyApplication.ACTION_POWER_INFO_UPDATE)){
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

    @Override
    public void onProgressChanged(SeekBar bar, int num, boolean bl) {
        tvBrightness.setText(String.format("%02d", num));
        setBrightness(num);
        brightness = num;
    }

    @Override
    public void onStartTrackingTouch(SeekBar bar) {
    }

    @Override
    public void onStopTrackingTouch(SeekBar bar) {
        databaseManager.setBrightness(brightness).save();
        setSysBrightness(brightness);
    }

    private class versionTextViewContinuousClickListener implements View.OnClickListener {
        public boolean counting = false;
        public int count = 0;

        @Override
        public void onClick(View view) {
            count++;
            if(count >= 5){
                count = 0;
//                PasswordDialog dialog = new PasswordDialog(SettingActivity.this);
//                dialog.setOnPwdConfirmListener(new PasswordDialog.OnPwdConfirmListener() {
//                    @Override
//                    public void onPweConfirm(String pwd) {
//                        if(pwd.equals(ADMIN_PASSWORD)){
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
                if (!counting) {
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
////                    databaseManager.setLanguage(language[witch]);
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

}
