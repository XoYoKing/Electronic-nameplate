package com.jackie.ts8209a.Activity;

import android.os.Bundle;

import com.jackie.ts8209a.R;

/**
 * Created by kuangyt on 2018/8/21.
 */

public class ShowNameActivity extends AppActivity {
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_showname);

        statusBarEnable = false;
        returnEnable = false;
    }
}
