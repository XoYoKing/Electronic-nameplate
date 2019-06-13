package com.itc.ts8209a.activity;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.TextView;

import com.itc.ts8209a.R;

import java.util.regex.Pattern;

public class MainActivity extends AppActivity  {

    private Button btnCallService;
    private Button btnEditUserInfo;
    private Button btnMeetingInfo;
    private Button btnSetting;
    private ImageButton ibtnShowName;
    private Button btnSms;
    private TextView tvMeetInfoAbs;
    private TextView tvWelcomeWord;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

//        returnEnable = false;

        btnCallService = (Button)findViewById(R.id.main_call_service_btn);
        btnEditUserInfo = (Button)findViewById(R.id.main_edit_name_btn);
        btnMeetingInfo = (Button)findViewById(R.id.main_meeting_info_btn);
        btnSetting = (Button)findViewById(R.id.main_setting_btn);
        ibtnShowName = (ImageButton) findViewById(R.id.main_show_name_ibtn);
        btnSms = (Button)findViewById(R.id.main_sms_btn);
        tvMeetInfoAbs = (TextView)findViewById(R.id.menu_meetinfo_abstract_tv);
        tvWelcomeWord = (TextView)findViewById(R.id.main_welcome_word_tv);

    }

    @Override
    protected void onResume() {
        super.onResume();
        btnCallService.setOnClickListener(this);
        btnEditUserInfo.setOnClickListener(this);
        btnMeetingInfo.setOnClickListener(this);
        btnSetting.setOnClickListener(this);
        ibtnShowName.setOnClickListener(this);
        btnSms.setOnClickListener(this);
    }

    @Override
    protected void uiRefresh() {
        super.uiRefresh();

        btnReturn.setVisibility(View.GONE);
        tvWelcomeWord.setText(databaseManager.getStr()[0]);
        meetName = databaseManager.getMeetName();
        meetSlogan = databaseManager.getMeetSlogan();
        meetContent = databaseManager.getMeetContent();
        meetStartTime = databaseManager.getMeetStartTime();
        meetEndTime = databaseManager.getMeetEndTime();
        setInfoAbstract();
    }

    private void setInfoAbstract(){
        final int lenNameSlogan = 10;
        final int lenContent = 40;
        String absInfoStr = "";

        if(!meetName.equals("") || !meetSlogan.equals("") || !meetContent.equals("") || !meetStartTime.equals("")) {
            int len;

            len = meetName.length();
            absInfoStr += "名称：" + meetName.substring(0, len > lenNameSlogan ? lenNameSlogan : len) + (len > lenNameSlogan ? "..." : "") + "\n\n";

            len = meetSlogan.length();
            absInfoStr += "主题："+ meetSlogan.substring(0, len > lenNameSlogan ? lenNameSlogan : len) + (len > lenNameSlogan ? "..." : "")+"\n\n";

            len = meetContent.length();
            String absContent = Pattern.compile("\n").matcher(meetContent).replaceAll("   ");
            absInfoStr += "内容："+absContent.substring(0, len > lenContent ? lenContent : len) + (len > lenContent ? "..." : "")+"\n\n";

            absInfoStr += "时间："+ meetStartTime +"\n\n";
        }

        tvMeetInfoAbs.setText(absInfoStr);
    }

    @Override
    public void onClick(View view) {
        super.onClick(view);

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
            case R.id.main_show_name_ibtn:
                intent = new Intent(MainActivity.this, ShowNameActivity.class);
                break;
            default:
                break;
        }

        if (intent != null) {
            startActivity(intent);
        }
    }
}
