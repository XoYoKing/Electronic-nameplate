package com.itc.ts8209a.activity.view;

import android.content.Context;
import android.content.res.TypedArray;
import android.util.AttributeSet;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.itc.ts8209a.module.network.WifiManager;
import com.itc.ts8209a.R;

/**
 * Created by kuangyt on 2018/8/27.
 */

public class WifiSelection extends LinearLayout {

    private String ssid;
    private int rssi;
    private int encryptType;

    private TextView tvSsid;
    private LinearLayout layLock;
    private TextView tvLock;
    private ImageView ivRssi;

    public WifiSelection(Context context) {
        super(context);
        setCustomAttributes(context);
    }

    public WifiSelection(Context context, AttributeSet attrs) {
        super(context, attrs);
        setCustomAttributes(context,attrs);
    }

    public WifiSelection(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
        setCustomAttributes(context,attrs);
    }

    public WifiSelection setEncryptType(int type){
        encryptType = type;
        if(encryptType == WifiManager.NONE)
            layLock.setVisibility(View.INVISIBLE);
        else
            layLock.setVisibility(View.VISIBLE);

        tvLock.setText(getEncryptTypeStr(type));
        return this;
    }

    public WifiSelection setSsid(String ssid){
        this.ssid = ssid;
        tvSsid.setText(ssid);
        return this;
    }

    public WifiSelection setRssi(int rssi){
        if(rssi > 0)
            return this;;

        if (rssi > -50 && rssi < 0) {//最强
            ivRssi.setImageLevel(0);
        } else if (rssi > -70 && rssi < -50) {//较强
            ivRssi.setImageLevel(1);
        } else if (rssi > -80 && rssi < -70) {//较弱
            ivRssi.setImageLevel(2);
        } else if (rssi < -80) {//微弱
            ivRssi.setImageLevel(3);
        }
        this.rssi = rssi;

        return this;
    }

    public String getSsid(){
        return ssid;
    }

    public int getRssi(){
        return rssi;
    }

    public int getEncryptType(){
        return encryptType;
    }

    private String getEncryptTypeStr(int type){

        switch(type){
            case WifiManager.WEP:return "[WEP]";
            case WifiManager.WPA:return "[WPA]";
            case WifiManager.WPA2:return "[WPA2]";
            case WifiManager.WPA_WPA2:return "[WPA/WPA2]";
            case WifiManager.EAP:return "[EAP]";
            case WifiManager.NONE:return "";
            default:return "";
        }
    }

    private void setCustomAttributes(Context context, AttributeSet attrs) {
        TypedArray array = context.obtainStyledAttributes(attrs,R.styleable.WifiSelection);
        ssid = array.getString(R.styleable.WifiSelection_Ssid);
        rssi = array.getInt(R.styleable.WifiSelection_Rssi,-200);

        setCustomAttributes(context);
    }



    private void setCustomAttributes(Context context) {
        LayoutInflater.from(context).inflate(R.layout.custom_view_wifi_selection,this);
        tvSsid = (TextView)findViewById(R.id.wifi_selection_ssid_tv);
        layLock = (LinearLayout)findViewById(R.id.wifi_selection_lock_lay);
        tvLock = (TextView)findViewById(R.id.wifi_selection_lock_tv);
        ivRssi = (ImageView)findViewById(R.id.wifi_selection_rssi_iv);

        setRssi(rssi);
    }

}
