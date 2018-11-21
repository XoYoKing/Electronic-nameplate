package com.jackie.ts8209a.Activity;

import android.content.Intent;
import android.os.Bundle;
import android.os.Message;
import android.os.RemoteException;
import android.text.TextUtils;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;

import com.jackie.ts8209a.R;

import java.util.regex.Pattern;

public class MainActivity extends AppActivity {

    private Button btnCallService;
    private Button btnEditUserInfo;
    private Button btnMeetingInfo;
    private Button btnSetting;
//    private Button btnShowName;
    private Button btnSms;
    private TextView tvMeetInfoAbs;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        returnEnable = false;

        btnCallService = (Button)findViewById(R.id.main_call_service_btn);
        btnEditUserInfo = (Button)findViewById(R.id.main_edit_name_btn);
        btnMeetingInfo = (Button)findViewById(R.id.main_meeting_info_btn);
        btnSetting = (Button)findViewById(R.id.main_setting_btn);
//        btnShowName = (Button)findViewById(R.id.main_show_name_btn);
        btnSms = (Button)findViewById(R.id.main_sms_btn);
        tvMeetInfoAbs = (TextView)findViewById(R.id.menu_meetinfo_abstract_tv);
    }

    @Override
    protected void onResume() {
        super.onResume();

        mainButtonListener listener = new mainButtonListener();
        btnCallService.setOnClickListener(listener);
        btnEditUserInfo.setOnClickListener(listener);
        btnMeetingInfo.setOnClickListener(listener);
        btnSetting.setOnClickListener(listener);
//        btnShowName.setOnClickListener(listener);
        btnSms.setOnClickListener(listener);
        setInfoAbstract();
    }

    private void setInfoAbstract(){
        final int lenNameSlogan = 10;
        final int lenContent = 40;
        String absInfoStr = "";

        if(!infoName.equals("") || !infoSlogan.equals("") || !infoContent.equals("") || !infoStartTime.equals("")) {
            int len;

            len = infoName.length();
            absInfoStr += "名称：" + infoName.substring(0, len > lenNameSlogan ? lenNameSlogan : len) + (len > lenNameSlogan ? "..." : "") + "\n\n";

            len = infoSlogan.length();
            absInfoStr += "主题："+infoSlogan.substring(0, len > lenNameSlogan ? lenNameSlogan : len) + (len > lenNameSlogan ? "..." : "")+"\n\n";

            len = infoContent.length();
            String absContent = Pattern.compile("\n").matcher(infoContent).replaceAll("   ");
            absInfoStr += "内容："+absContent.substring(0, len > lenContent ? lenContent : len) + (len > lenContent ? "..." : "")+"\n\n";

            absInfoStr += "时间："+infoStartTime+"\n\n";
        }

        tvMeetInfoAbs.setText(absInfoStr);
    }

    private class mainButtonListener implements View.OnClickListener {

        @Override
        public void onClick(View view) {
            Intent intent = null;

            switch (view.getId()) {
                case R.id.main_edit_name_btn:
                    intent = new Intent(MainActivity.this, EditUserInfoActivity.class);
                    break;
                case R.id.main_meeting_info_btn:
                    intent = new Intent(MainActivity.this, MeetingInfoActivity.class);
                    break;
                case R.id.main_call_service_btn:
                    intent = new Intent(MainActivity.this, CallServiceActivity.class);
                    break;
                case R.id.main_sms_btn:
                    intent = new Intent(MainActivity.this, SmsActivity.class);
                    break;
                case R.id.main_setting_btn:
                    intent = new Intent(MainActivity.this, SettingActivity.class);
                    break;
//                case R.id.main_show_name_btn:
//                    intent = new Intent(MainActivity.this, ShowNameActivity.class);
//                    intent = new Intent(android.provider.Settings.ACTION_WIFI_SETTINGS);
//                    break;
                default:
                    break;
            }

            if (intent != null) {
                startActivity(intent);
            }
        }
    }
}
