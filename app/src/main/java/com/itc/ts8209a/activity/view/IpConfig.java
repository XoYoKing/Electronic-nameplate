package com.itc.ts8209a.activity.view;

import android.app.Dialog;
import android.content.Context;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnFocusChangeListener;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.CompoundButton;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.Switch;
import android.widget.TextView;

import com.itc.ts8209a.R;

/**
 * Created by kuangyt on 2018/9/10.
 */

public class IpConfig extends Dialog {
    private static final String TAG = "IpConfig";
    public static final int TYPE_SERV = 1;
    public static final int TYPE_LOCAL = 2;

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
        getWindow().setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_STATE_HIDDEN);
    }

    public interface OnIpConfigConfirmListener {
        void ipConfigConfirm(ipConfigPara para);
    }

    public void setOnIpConfigConfirmListener(OnIpConfigConfirmListener listener) {
        this.listener = listener;
    }

    public static class ipConfigPara {
        public int devId = 0;
        public int[] servIP = {0, 0, 0, 0};
        public int[] localIP = {0, 0, 0, 0};
        public int[] gateway = {0, 0, 0, 0};
        public int[] mask = {0, 0, 0, 0};
        public int servPort = 0;
        public boolean dhcp = false;
    }

    public static class Builder implements TextWatcher, OnFocusChangeListener, View.OnClickListener, CompoundButton.OnCheckedChangeListener {
        private View layout;
        private Context context;
        private IpConfig dialog;
        private int type = TYPE_SERV;
        private LinearLayout layServIpCfg;
        private LinearLayout layLocalIpCfg;
        private LinearLayout layLocalIpEdit;

        private EditText etDevId;
        private EditText[] etServIp = new EditText[4];
        private EditText[] etLocalIp = new EditText[4];
        private EditText[] etGateway = new EditText[4];
        private EditText[] etMask = new EditText[4];
        private EditText etServPort;
        private Switch swDhcp;
        private Button btnCancel;
        private Button btnConfirm;
        private TextView tvWarning;


        private EditText currentEditText = null;

        public Builder(Context context) {
            this.context = context;
            dialog = new IpConfig(context, R.style.custom_dialog);
            LayoutInflater inflater = (LayoutInflater) context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
            layout = inflater.inflate(R.layout.dialog_ip_config, null);
//            dialog.getWindow().clearFlags(WindowManager.LayoutParams.SOFT_INPUT_STATE_VISIBLE|WindowManager.LayoutParams.SOFT_INPUT_ADJUST_PAN);
            viewInit();
        }

        public Builder setType(int type) {
            this.type = type;
            return this;
        }

        public IpConfig creatIpConfig(ipConfigPara para) {
            if (type == TYPE_SERV) {
                layLocalIpCfg.setVisibility(View.GONE);
                layServIpCfg.setVisibility(View.VISIBLE);

                etDevId.setText(String.valueOf(para.devId));
                etServPort.setText(String.valueOf(para.servPort));
                for (int i = 0; i < 4; i++) {
                    etServIp[i].setText(String.valueOf(para.servIP[i]));
                }
            } else if (type == TYPE_LOCAL) {
                layServIpCfg.setVisibility(View.GONE);
                layLocalIpCfg.setVisibility(View.VISIBLE);

                swDhcp.setChecked(para.dhcp);

                for (int i = 0; i < 4; i++) {
                    etLocalIp[i].setText(String.valueOf(para.localIP[i]));
                    etGateway[i].setText(String.valueOf(para.gateway[i]));
                    etMask[i].setText(String.valueOf(para.mask[i]));
                }
                if (para.dhcp)
                    layLocalIpEdit.setVisibility(View.GONE);
                else
                    layLocalIpEdit.setVisibility(View.VISIBLE);
            }

            dialog.setContentView(layout);
            dialog.setCanceledOnTouchOutside(true);

            return dialog;
        }

        private void viewInit() {
            int[] ridServIp = {R.id.ip_config_serv_ip0_et, R.id.ip_config_serv_ip1_et, R.id.ip_config_serv_ip2_et, R.id.ip_config_serv_ip3_et};
            int[] ridLocalIp = {R.id.ip_config_local_ip0_et, R.id.ip_config_local_ip1_et, R.id.ip_config_local_ip2_et, R.id.ip_config_local_ip3_et};
            int[] ridGateway = {R.id.ip_config_gateway0_et, R.id.ip_config_gateway1_et, R.id.ip_config_gateway2_et, R.id.ip_config_gateway3_et};
            int[] ridMask = {R.id.ip_config_mask0_et, R.id.ip_config_mask1_et, R.id.ip_config_mask2_et, R.id.ip_config_mask3_et};

            layServIpCfg = (LinearLayout) layout.findViewById(R.id.ip_config_serv_lay);
            layLocalIpCfg = (LinearLayout) layout.findViewById(R.id.ip_config_local_lay);
            layLocalIpEdit = (LinearLayout) layout.findViewById(R.id.ip_config_local_edit_lay);

            btnCancel = (Button) layout.findViewById(R.id.ip_config_cancel_btn);
            btnConfirm = (Button) layout.findViewById(R.id.ip_config_confirm_btn);
            etDevId = (EditText) layout.findViewById(R.id.ip_config_dev_id_et);
            etServPort = (EditText) layout.findViewById(R.id.ip_config_serv_port_et);
            swDhcp = (Switch) layout.findViewById(R.id.ip_config_dhcp_sw);

            etDevId.addTextChangedListener(this);
            etDevId.setOnFocusChangeListener(this);

            etServPort.addTextChangedListener(this);
            etServPort.setOnFocusChangeListener(this);

            for (int i = 0; i < 4; i++) {
                etServIp[i] = (EditText) layout.findViewById(ridServIp[i]);
                etServIp[i].addTextChangedListener(this);
                etServIp[i].setOnFocusChangeListener(this);

                etLocalIp[i] = (EditText) layout.findViewById(ridLocalIp[i]);
                etLocalIp[i].addTextChangedListener(this);
                etLocalIp[i].setOnFocusChangeListener(this);

                etGateway[i] = (EditText) layout.findViewById(ridGateway[i]);
                etGateway[i].addTextChangedListener(this);
                etGateway[i].setOnFocusChangeListener(this);

                etMask[i] = (EditText) layout.findViewById(ridMask[i]);
                etMask[i].addTextChangedListener(this);
                etMask[i].setOnFocusChangeListener(this);
            }

            swDhcp.setOnCheckedChangeListener(this);

            btnCancel.setOnClickListener(this);
            btnConfirm.setOnClickListener(this);

            tvWarning = (TextView) layout.findViewById(R.id.ip_config_warning_tv);
        }

        public Builder setOnIpConfigConfirmListener(OnIpConfigConfirmListener listener) {
            dialog.listener = listener;
            return this;
        }

        @Override
        public void beforeTextChanged(CharSequence s, int start, int count, int after) {

        }

        @Override
        public void onTextChanged(CharSequence s, int start, int before, int count) {

        }

        @Override
        public void afterTextChanged(Editable s) {
            if (currentEditText == null)
                return;
            int num = Integer.valueOf(s.toString().equals("") ? "0" : s.toString());
            switch (currentEditText.getId()) {
                case R.id.ip_config_serv_ip0_et:
                case R.id.ip_config_serv_ip1_et:
                case R.id.ip_config_serv_ip2_et:
                case R.id.ip_config_serv_ip3_et:
                case R.id.ip_config_local_ip0_et:
                case R.id.ip_config_local_ip1_et:
                case R.id.ip_config_local_ip2_et:
                case R.id.ip_config_local_ip3_et:
                case R.id.ip_config_gateway0_et:
                case R.id.ip_config_gateway1_et:
                case R.id.ip_config_gateway2_et:
                case R.id.ip_config_gateway3_et:
                case R.id.ip_config_mask0_et:
                case R.id.ip_config_mask1_et:
                case R.id.ip_config_mask2_et:
                    if (num > 255) {
                        num = 255;
                        currentEditText.setText(String.valueOf(num));
                    }
                    if (s.toString().length() == 3) {
                        try {
                            EditText edittext = (EditText) layout.findViewById(currentEditText.getNextFocusForwardId());
                            edittext.setSelection(edittext.getText().toString().length());
                            edittext.requestFocus();
                        }catch (Exception e){
                            e.printStackTrace();
                        }
                    }
                    break;
                case R.id.ip_config_mask3_et:
                    if (num > 255) {
                        num = 255;
                        currentEditText.setText(String.valueOf(num));
                    }
                    break;
                case R.id.ip_config_dev_id_et:
                    if (num > 1000) {
                        num = 1000;
                        currentEditText.setText(String.valueOf(num));
                    }
                    if (s.toString().length() == 3) {
                        EditText edittext = (EditText) layout.findViewById(currentEditText.getNextFocusForwardId());
                        edittext.setSelection(edittext.getText().toString().length());
                        edittext.requestFocus();
                    }
                    break;
                case R.id.ip_config_serv_port_et:
                    if (num > 65535) {
                        num = 65535;
                        currentEditText.setText(String.valueOf(num));
                    }
                    break;
                default:
                    break;
            }
        }

        @Override
        public void onFocusChange(View v, boolean hasFocus) {
            if (hasFocus)
                currentEditText = (EditText) v;
        }

        @Override
        public void onClick(View view) {
            Button btn = (Button) view;

            switch (btn.getId()) {
                case R.id.ip_config_confirm_btn:
                    if (!checkLegality())
                        break;
                    if (dialog.listener != null) {
                        ipConfigPara para = new ipConfigPara();
                        if (type == TYPE_SERV) {
                            para.devId = Integer.valueOf(etDevId.getText().toString());
                            para.servPort = Integer.valueOf(etServPort.getText().toString());
                            for (int i = 0; i < 4; i++) {
                                para.servIP[i] = Integer.valueOf(etServIp[i].getText().toString());
                            }
                        } else if (type == TYPE_LOCAL) {
                            para.dhcp = swDhcp.isChecked();
                            if (!para.dhcp) {
                                for (int i = 0; i < 4; i++) {
                                    para.localIP[i] = Integer.valueOf(etLocalIp[i].getText().toString());
                                    para.gateway[i] = Integer.valueOf(etGateway[i].getText().toString());
                                    para.mask[i] = Integer.valueOf(etMask[i].getText().toString());
                                }
                            }
                        }
                        dialog.listener.ipConfigConfirm(para);
                    }
                    dialog.dismiss();
                    break;
                case R.id.ip_config_cancel_btn:
                    dialog.dismiss();
                    break;
            }
        }

        @Override
        public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
//            Log.d(TAG,isChecked+"");
            if (isChecked) {
                layLocalIpEdit.setVisibility(View.GONE);
            } else {
                layLocalIpEdit.setVisibility(View.VISIBLE);
            }
        }

        private boolean checkLegality() {
            String warning = "";


            if (type == TYPE_SERV) {
                if (etDevId.getText().toString().equals("")) {
                    warning = context.getResources().getString(R.string.dev_id);
                    etDevId.requestFocus();
                } else if (etServPort.getText().toString().equals("")) {
                    warning = context.getResources().getString(R.string.server_port);
                    etServPort.requestFocus();
                } else if (etServIp[0].getText().toString().equals("") || etServIp[1].getText().toString().equals("") ||
                        etServIp[2].getText().toString().equals("") || etServIp[3].getText().toString().equals("")) {
                    warning = context.getResources().getString(R.string.server_ip);
                    etServIp[0].requestFocus();
                } else {
                    return true;
                }

//                tvWarning.setVisibility(View.VISIBLE);
//                warning += context.getString(R.string.can_not_be_empty);
//                tvWarning.setText(warning);

            } else if (type == TYPE_LOCAL) {
                if (etLocalIp[0].getText().toString().equals("") || etLocalIp[1].getText().toString().equals("") ||
                        etLocalIp[2].getText().toString().equals("") || etLocalIp[3].getText().toString().equals("")) {
                    warning = context.getResources().getString(R.string.local_ip);
                    etLocalIp[0].requestFocus();
                } else if (etGateway[0].getText().toString().equals("") || etGateway[1].getText().toString().equals("") ||
                        etGateway[2].getText().toString().equals("") || etGateway[3].getText().toString().equals("")) {
                    warning = context.getResources().getString(R.string.gateway);
                    etGateway[0].requestFocus();
                } else if (etMask[0].getText().toString().equals("") || etMask[1].getText().toString().equals("") ||
                        etMask[2].getText().toString().equals("") || etMask[3].getText().toString().equals("")) {
                    warning = context.getResources().getString(R.string.netmask);
                    etMask[0].requestFocus();
                } else {
                    return true;
                }

            }

            tvWarning.setVisibility(View.VISIBLE);
            warning += context.getString(R.string.can_not_be_empty);
            tvWarning.setText(warning);

            return false;
        }


    }

}
