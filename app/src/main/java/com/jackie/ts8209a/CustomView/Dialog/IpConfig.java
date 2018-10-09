package com.jackie.ts8209a.CustomView.Dialog;

import android.app.Dialog;
import android.content.Context;
import android.os.Bundle;
import android.text.Editable;
import android.text.Layout;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnFocusChangeListener;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Switch;
import android.widget.TextView;

import com.jackie.ts8209a.R;

/**
 * Created by kuangyt on 2018/9/10.
 */

public class IpConfig extends Dialog {

    private OnIpConfigConfirmListener listener = null;

    private IpConfig(Context context) {
        super(context);
    }

    private IpConfig(Context context, int theme) {
        super(context, theme);
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        getWindow().setSoftInputMode( WindowManager.LayoutParams.SOFT_INPUT_STATE_HIDDEN);
    }

    public interface OnIpConfigConfirmListener{
        void ipConfigConfirm(ipConfigPara para);
    }

    public void setOnIpConfigConfirmListener(OnIpConfigConfirmListener listener){
        this.listener = listener;
    }

    public static class ipConfigPara{
        public int devId = 0;
        public int[] servIP = {0,0,0,0};
        public int[] localIP = {0,0,0,0};
        public int servPort = 0;
        public boolean dhcp = false;
    }

    public static class Builder{
        private View layout;
//        private Context context;
        private IpConfig dialog;

        private EditText etDevId;
        private EditText[] etServIp = new EditText[4];
        private EditText[] etLocalIp = new EditText[4];
        private EditText etServPort;
        private Switch swDhcp;

        private Button btnCancel;
        private Button btnConfirm;

        private TextView tvWarning;

        public Builder(Context context){
//            this.context = context;
            dialog = new IpConfig(context, R.style.custom_dialog);
            LayoutInflater inflater = (LayoutInflater) context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
            layout = inflater.inflate(R.layout.dialog_ip_config, null);
//            dialog.getWindow().clearFlags(WindowManager.LayoutParams.SOFT_INPUT_STATE_VISIBLE|WindowManager.LayoutParams.SOFT_INPUT_ADJUST_PAN);
            viewInit();
        }

        public IpConfig creatIpConfig(ipConfigPara para){
            etDevId.setText(String.valueOf(para.devId));
            etServPort.setText(String.valueOf(para.servPort));

            for(int i=0;i<4;i++){
                etServIp[i].setText(String.valueOf(para.servIP[i]));
                etLocalIp[i].setText(String.valueOf(para.localIP[i]));
            }

            swDhcp.setChecked(para.dhcp);
            dialog.setContentView(layout);
            dialog.setCanceledOnTouchOutside(true);

            return dialog;
        }

        private void viewInit(){
            int[] ridServIp ={R.id.ip_config_serv_ip0_et,R.id.ip_config_serv_ip1_et,R.id.ip_config_serv_ip2_et,R.id.ip_config_serv_ip3_et};
            int[] ridLocalIp ={R.id.ip_config_local_ip0_et,R.id.ip_config_local_ip1_et,R.id.ip_config_local_ip2_et,R.id.ip_config_local_ip3_et};
            ipconfigInputTextListener listener =  new ipconfigInputTextListener();

            etDevId = (EditText)layout.findViewById(R.id.ip_config_dev_id_et);
            etServPort = (EditText)layout.findViewById(R.id.ip_config_serv_port_et);
            swDhcp = (Switch)layout.findViewById(R.id.ip_config_dhcp_sw);

            etDevId.addTextChangedListener(listener);
            etDevId.setOnFocusChangeListener(listener.focusListener);
            etServPort.addTextChangedListener(listener);
            etServPort.setOnFocusChangeListener(listener.focusListener);

            for(int i = 0;i<4;i++) {
                etServIp[i] = (EditText) layout.findViewById(ridServIp[i]);
                etServIp[i].addTextChangedListener(listener);
                etServIp[i].setOnFocusChangeListener(listener.focusListener);

                etLocalIp[i] = (EditText) layout.findViewById(ridLocalIp[i]);
                etLocalIp[i].addTextChangedListener(listener);
                etLocalIp[i].setOnFocusChangeListener(listener.focusListener);
            }

            btnCancel = (Button)layout.findViewById(R.id.ip_config_cancel_btn);
            btnConfirm = (Button)layout.findViewById(R.id.ip_config_confirm_btn);

            ipconfigButtonListener btnListener = new ipconfigButtonListener();
            btnCancel.setOnClickListener(btnListener);
            btnConfirm.setOnClickListener(btnListener);

            tvWarning = (TextView)layout.findViewById(R.id.ip_config_warning_tv);
        }

        public Builder setOnIpConfigConfirmListener(OnIpConfigConfirmListener listener){
            dialog.listener = listener;
            return this;
        }

        private class ipconfigInputTextListener implements TextWatcher{
            private EditText currentEditText = null;

            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {

            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {

            }

            @Override
            public void afterTextChanged(Editable s) {
                if(currentEditText == null)
                    return;
                int num = Integer.valueOf(s.toString().equals("") ? "0" : s.toString());
                switch(currentEditText.getId()){
                    case R.id.ip_config_serv_ip0_et:
                    case R.id.ip_config_serv_ip1_et:
                    case R.id.ip_config_serv_ip2_et:
                    case R.id.ip_config_serv_ip3_et:
                    case R.id.ip_config_local_ip0_et:
                    case R.id.ip_config_local_ip1_et:
                    case R.id.ip_config_local_ip2_et:
                        if(num > 255) {
                            num = 255;
                            currentEditText.setText(String.valueOf(num));
                        }
                        if(s.toString().length() == 3) {
                            EditText edittext = (EditText) layout.findViewById(currentEditText.getNextFocusForwardId());
                            edittext.setSelection(edittext.getText().toString().length());
                            edittext.requestFocus();
                        }
                        break;
                    case R.id.ip_config_local_ip3_et:
                        if(num > 255) {
                            num = 255;
                            currentEditText.setText(String.valueOf(num));
                        }
                        break;
                    case R.id.ip_config_dev_id_et:
                        if(num > 1000) {
                            num = 1000;
                            currentEditText.setText(String.valueOf(num));
                        }
                        if(s.toString().length() == 3) {
                            EditText edittext = (EditText) layout.findViewById(currentEditText.getNextFocusForwardId());
                            edittext.setSelection(edittext.getText().toString().length());
                            edittext.requestFocus();
                        }
                        break;
                    case R.id.ip_config_serv_port_et:
                        if(num > 65535) {
                            num = 65535;
                            currentEditText.setText(String.valueOf(num));
                        }
                        if(s.toString().length() == 5) {
                            EditText edittext = (EditText) layout.findViewById(currentEditText.getNextFocusForwardId());
                            edittext.setSelection(edittext.getText().toString().length());
                            edittext.requestFocus();
                        }
                        break;
                    default:break;
                }
            }

            public OnFocusChangeListener focusListener = new OnFocusChangeListener() {
                @Override
                public void onFocusChange(View v, boolean hasFocus) {
                    if(hasFocus)
                        currentEditText = (EditText)v;
                }
            };
        }

        private class ipconfigButtonListener implements View.OnClickListener{

            @Override
            public void onClick(View view) {
                Button btn = (Button)view;

                switch (btn.getId()){
                    case R.id.ip_config_confirm_btn:
                        if(!checkLegality())
                            break;
                        if(dialog.listener != null){
                            ipConfigPara para = new ipConfigPara();
                            para.devId = Integer.valueOf(etDevId.getText().toString());
                            para.servPort = Integer.valueOf(etServPort.getText().toString());
                            for(int i=0;i<4;i++){
                                para.servIP[i] = Integer.valueOf(etServIp[i].getText().toString());
                                para.localIP[i] = Integer.valueOf(etLocalIp[i].getText().toString());
                            }
                            para.dhcp = swDhcp.isChecked();

                            dialog.listener.ipConfigConfirm(para);
                        }
                        dialog.dismiss();
                        break;
                    case R.id.ip_config_cancel_btn:
                        dialog.dismiss();
                        break;
                }
            }
        }

        private boolean checkLegality(){
            if(etDevId.getText().toString().equals("")){
                tvWarning.setText("终端ID不能为空！");
                tvWarning.setVisibility(View.VISIBLE);
                etDevId.requestFocus();
                return false;
            }

            if(etServPort.getText().toString().equals("")){
                tvWarning.setText("服务器端口号不能为空！");
                tvWarning.setVisibility(View.VISIBLE);
                etServPort.requestFocus();
                return false;
            }else if(Integer.valueOf(etServPort.getText().toString()) == 0){
                tvWarning.setText("服务器端口号不能为0");
                tvWarning.setVisibility(View.VISIBLE);
                etServPort.requestFocus();
                return false;
            }


            for(int i=0;i<4;i++){
                if(etServIp[i].getText().toString().equals("")){
                    tvWarning.setText("服务器IP不能为空！");
                    tvWarning.setVisibility(View.VISIBLE);
                    etServIp[i].requestFocus();
                    return false;
                }

                if(etLocalIp[i].getText().toString().equals("")){
                    tvWarning.setText("本地IP不能为空！");
                    tvWarning.setVisibility(View.VISIBLE);
                    etLocalIp[i].requestFocus();
                    return false;
                }
            }

            return true;
        }
    }

}
