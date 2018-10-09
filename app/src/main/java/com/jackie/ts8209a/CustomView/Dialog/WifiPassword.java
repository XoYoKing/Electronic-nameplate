package com.jackie.ts8209a.CustomView.Dialog;

import android.app.Dialog;
import android.content.Context;
import android.content.DialogInterface;
import android.text.Editable;
import android.text.InputType;
import android.text.TextWatcher;
import android.text.method.HideReturnsTransformationMethod;
import android.text.method.PasswordTransformationMethod;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.jackie.ts8209a.R;

/**
 * Created by kuangyt on 2018/8/29.
 */

public class WifiPassword extends Dialog {

    private static final String TAG = "WifiPassword";

    public static final int CANCEL = 0;
    public static final int CONNECT = 1;

    private static OnPasswordResultListener passwordResListener;

    private WifiPassword(Context context) {
        super(context);
    }

    private WifiPassword(Context context, int theme) {
        super(context, theme);
    }

    public interface OnPasswordResultListener{
        abstract void passwordResult(int result,String password);
    }

    public void setOnPasswordResultListener(OnPasswordResultListener listener){
        passwordResListener = listener;
    }

    public static class Builder{
        private View layout;
        private Context context;
        private WifiPassword dialog;
        private String password;

        private TextView tvSsid;
        private TextView tvRssi;
        private EditText etPwd;
        private Button btnConnect;
        private Button btnCancle;
        private CheckBox cbPwdVisi;

        public Builder(Context context){
            this.context = context;
            dialog = new WifiPassword(context, R.style.custom_dialog);
            LayoutInflater inflater = (LayoutInflater) context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
            layout = inflater.inflate(R.layout.dialog_wifi_password, null);
            viewInit();
        }

        public Builder setSsid(String ssid){
            tvSsid.setText(ssid);
            return this;
        }

        public Builder setRssi(int rssi){
            if(rssi > 0)
                rssi = -45;
            tvRssi.setText(rssi+"");
            return this;
        }

        public Builder setOnPasswordResultListener(OnPasswordResultListener listener){
            dialog.setOnPasswordResultListener(listener);
            return this;
        }

        public WifiPassword creatWifiPassword(){
            dialog.setContentView(layout);
            dialog.setCanceledOnTouchOutside(false);
            return dialog;
        }

        private class inputTypeCheckListener implements CompoundButton.OnCheckedChangeListener{

            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                if(isChecked)
                    etPwd.setTransformationMethod(HideReturnsTransformationMethod.getInstance());
                else
                    etPwd.setTransformationMethod(PasswordTransformationMethod.getInstance());

                etPwd.setSelection(etPwd.length());
            }
        }

        private class pwdInputListener implements TextWatcher {


            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {
            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
            }

            @Override
            public void afterTextChanged(Editable s) {
                password = s.toString();
            }
        }

        private class btnOnclickListener implements View.OnClickListener{

            @Override
            public void onClick(View view) {
                switch (view.getId()){
                    case R.id.wifi_password_connect_btn:
                        if(passwordResListener != null){
                            passwordResListener.passwordResult(CONNECT,password);
                        }
                        break;
                    case R.id.wifi_password_cancel_btn:
                        if(passwordResListener != null){
                            passwordResListener.passwordResult(CANCEL,password);
                        }
                        break;
                }
            }
        }

        private void viewInit(){
            tvSsid = (TextView) layout.findViewById(R.id.wifi_password_ssid_tv);
            tvRssi = (TextView) layout.findViewById(R.id.wifi_password_rssi_tv);
            etPwd = (EditText)layout.findViewById(R.id.wifi_password_input_et);
            btnConnect = (Button)layout.findViewById(R.id.wifi_password_connect_btn);
            btnCancle = (Button)layout.findViewById(R.id.wifi_password_cancel_btn);
            cbPwdVisi = (CheckBox)layout.findViewById(R.id.wifi_password_pwd_visi_cb);

            cbPwdVisi.setOnCheckedChangeListener(new inputTypeCheckListener());
            etPwd.addTextChangedListener(new pwdInputListener());
            btnOnclickListener listener = new btnOnclickListener();
            btnCancle.setOnClickListener(listener);
            btnConnect.setOnClickListener(listener);
        }
    }
}
