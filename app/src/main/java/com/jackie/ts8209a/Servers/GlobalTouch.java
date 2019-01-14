package com.jackie.ts8209a.Servers;

import android.app.Service;
import android.content.Intent;
import android.graphics.PixelFormat;
import android.os.IBinder;
import android.util.Log;
import android.view.Gravity;
import android.view.MotionEvent;
import android.view.View;
import android.view.WindowManager;
import android.widget.LinearLayout;

/**
 * Created by kuangyt on 2018/12/25.
 */

public class GlobalTouch extends Service implements View.OnTouchListener {
    private final String TAG = this.getClass().getSimpleName();

    private WindowManager windowManager;
    private LinearLayout touchLayout;

    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    @Override
    public void onCreate() {
        super.onCreate();

        Log.d(TAG,"Start server");
        touchLayout = new LinearLayout(this);
        LinearLayout.LayoutParams para = new LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.MATCH_PARENT);
        touchLayout.setLayoutParams(para);
        touchLayout.setClickable(true);
        touchLayout.setOnTouchListener(this);

        windowManager = (WindowManager) getSystemService(WINDOW_SERVICE);
        WindowManager.LayoutParams wPara = new WindowManager.LayoutParams(WindowManager.LayoutParams.MATCH_PARENT,
                WindowManager.LayoutParams.MATCH_PARENT,
                WindowManager.LayoutParams.TYPE_PHONE,
                WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE,
                PixelFormat.TRANSLUCENT);
        wPara.gravity = Gravity.CENTER;

        windowManager.addView(touchLayout,wPara);
    }

    @Override
    public void onDestroy() {
        if(windowManager != null && touchLayout != null)
            windowManager.removeView(touchLayout);
        super.onDestroy();
    }

    @Override
    public boolean onTouch(View v, MotionEvent event) {
        if(event.getAction() == MotionEvent.ACTION_DOWN || event.getAction() == MotionEvent.ACTION_UP)
            Log.i(TAG, "Action :" + event.getAction() + "\t X :" + event.getRawX() + "\t Y :"+ event.getRawY());
        return false;

    }
}
