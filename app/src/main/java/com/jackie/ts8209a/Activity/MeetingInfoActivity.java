package com.jackie.ts8209a.Activity;

import android.os.Bundle;
import android.util.Log;

import com.jackie.ts8209a.R;

/**
 * Created by kuangyt on 2018/8/21.
 */

public class MeetingInfoActivity extends AppActivity {


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_meetinginfo);
    }

    @Override
    protected void onResume() {
        super.onResume();
        Log.d(TAG, infoName +"\n"+ infoSlogan +"\n"+ infoContent +"\n"+ infoStartTime +"\n"+ infoEndTime +"\n");
    }


}
