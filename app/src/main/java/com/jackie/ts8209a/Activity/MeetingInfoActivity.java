package com.jackie.ts8209a.Activity;

import android.graphics.Color;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.RadioGroup;
import android.widget.ScrollView;
import android.widget.TextView;

import com.jackie.ts8209a.R;

/**
 * Created by kuangyt on 2018/8/21.
 */

public class MeetingInfoActivity extends AppActivity implements RadioGroup.OnCheckedChangeListener {

    private RadioGroup rgPageChoice;
    private ScrollView svInfoContent;
    private ScrollView svAdminMsg;
    private LinearLayout layAdminMsg;
    private TextView tvMeetName;
    private TextView tvMeetSlogan;
    private TextView tvMeetContent;
    private TextView tvMeetStartTime;
    private TextView tvMeetEndTime;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_meetinginfo);

        rgPageChoice = (RadioGroup) findViewById(R.id.meetinginfo_page_choice_rg);
        svInfoContent = (ScrollView) findViewById(R.id.meetinginfo_content_sv);
        svAdminMsg = (ScrollView) findViewById(R.id.meetinginfo_admin_msg_sv);
        layAdminMsg = (LinearLayout) findViewById(R.id.meetinginfo_admin_msg_lay);
        tvMeetName = (TextView) findViewById(R.id.meetinginfo_content_name);
        tvMeetSlogan = (TextView) findViewById(R.id.meetinginfo_content_slogan);
        tvMeetContent = (TextView) findViewById(R.id.meetinginfo_content_substance);
        tvMeetStartTime = (TextView) findViewById(R.id.meetinginfo_content_starttime);
        tvMeetEndTime = (TextView) findViewById(R.id.meetinginfo_content_endtime);

        svInfoContent.setVisibility(View.VISIBLE);
        svAdminMsg.setVisibility(View.GONE);

        rgPageChoice.setOnCheckedChangeListener(this);
    }

    @Override
    protected void onResume() {
        super.onResume();

        setMeetInfoContent();
        setAdminMsg();
    }


    @Override
    public void onCheckedChanged(RadioGroup group, int checkedId) {
        switch (checkedId){
            case R.id.meetinginfo_page_choice_1_rb:
                svInfoContent.setVisibility(View.VISIBLE);
                svAdminMsg.setVisibility(View.GONE);
                break;
            case R.id.meetinginfo_page_choice_2_rb:
                svInfoContent.setVisibility(View.GONE);
                svAdminMsg.setVisibility(View.VISIBLE);
                break;
        }

    }

    private void setMeetInfoContent(){
        tvMeetName.setText(infoName);
        tvMeetSlogan.setText(infoSlogan);
        tvMeetContent.setText(infoContent);
        tvMeetStartTime.setText(infoStartTime);
        tvMeetEndTime.setText(infoEndTime);
    }

    private void setAdminMsg(){
        layAdminMsg.removeAllViews();
        for (int i = 0; i < adminMsg.size(); i++) {
            String msg = adminMsg.get(i);

            LinearLayout layout = new LinearLayout(this);
            LinearLayout.LayoutParams layParams = new LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT);
            layParams.setMargins(0, 5, 0, 5);
            layout.setLayoutParams(layParams);
            layout.setPadding(10, 5, 10, 5);
            layout.setBackground(getResources().getDrawable(R.drawable.bg_sms_old_content));
            layout.setOrientation(LinearLayout.VERTICAL);
            layout.setClickable(true);

            TextView smsName = new TextView(this);
            smsName.setLayoutParams(layParams);
            smsName.setText("管理员");
            smsName.setTextColor(Color.GRAY);
            smsName.setTextSize(22);

            layout.addView(smsName);

            TextView smsContent = new TextView(this);
            smsContent.setLayoutParams(layParams);
            smsContent.setText(msg);
            smsContent.setTextColor(Color.WHITE);
            smsContent.setTextSize(24);

            layout.addView(smsContent);

            layAdminMsg.addView(layout);
        }
    }

}
