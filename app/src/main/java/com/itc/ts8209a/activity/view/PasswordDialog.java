package com.itc.ts8209a.activity.view;

import android.app.Dialog;
import android.content.Context;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;

import com.itc.ts8209a.R;

/**
 * Created by kuangyt on 2019/1/17.
 */

public class PasswordDialog extends Dialog {
    private static final int MAX_LEN = 10;

    private String inputPwd = "";
    private OnPwdConfirmListener pwdConfirmListener = null;

    private View dialogLayout;
    private int numBtnId[] = { R.id.password_num_0_btn, R.id.password_num_1_btn, R.id.password_num_2_btn, R.id.password_num_3_btn, R.id.password_num_4_btn,
            R.id.password_num_5_btn, R.id.password_num_6_btn, R.id.password_num_7_btn, R.id.password_num_8_btn, R.id.password_num_9_btn};
    private Button numBtn[] = null;
    private TextView tvPwdNum = null;

    public PasswordDialog(Context context) {
        super(context, R.style.custom_dialog);
        LayoutInflater inflater = (LayoutInflater) context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        dialogLayout = inflater.inflate(R.layout.dialog_password_input,null);

        this.setContentView(dialogLayout);
        this.setCanceledOnTouchOutside(false);			//用户不能通过点击对话框之外的地方取消对话框显示
        new Thread(){
            public void run() {
                try {
                    Thread.sleep(800);
                    setCanceledOnTouchOutside(true);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }

            };
        }.start();

        pwdButtonOnClickListener pwdListener = new pwdButtonOnClickListener();
        numBtn = new Button[10];
        for(int i=0;i<10;i++){
            numBtn[i] = (Button) findViewById(numBtnId[i]);
            numBtn[i].setOnClickListener(pwdListener);
        }
        ((Button)findViewById(R.id.password_cancel_btn)).setOnClickListener(pwdListener);
        ((Button)findViewById(R.id.password_confirm_btn)).setOnClickListener(pwdListener);
        tvPwdNum = (TextView)findViewById(R.id.password_pwd_num_tv);
    }


    public void setOnPwdConfirmListener(OnPwdConfirmListener listener){
        pwdConfirmListener = listener;
    }

    public interface OnPwdConfirmListener{
        abstract public void onPweConfirm(String pwd);
    }

    private class pwdButtonOnClickListener implements android.view.View.OnClickListener{

        @Override
        public void onClick(View view) {
            try{
                if (inputPwd.length() < MAX_LEN) {
                    switch (view.getId()) {
                        case R.id.password_num_0_btn:
                            inputPwd += "0";
                            break;
                        case R.id.password_num_1_btn:
                            inputPwd += "1";
                            break;
                        case R.id.password_num_2_btn:
                            inputPwd += "2";
                            break;
                        case R.id.password_num_3_btn:
                            inputPwd += "3";
                            break;
                        case R.id.password_num_4_btn:
                            inputPwd += "4";
                            break;
                        case R.id.password_num_5_btn:
                            inputPwd += "5";
                            break;
                        case R.id.password_num_6_btn:
                            inputPwd += "6";
                            break;
                        case R.id.password_num_7_btn:
                            inputPwd += "7";
                            break;
                        case R.id.password_num_8_btn:
                            inputPwd += "8";
                            break;
                        case R.id.password_num_9_btn:
                            inputPwd += "9";
                            break;
                    }
                }

                if(inputPwd.length() > 0 && view.getId() == R.id.password_cancel_btn){
                    inputPwd = inputPwd.substring(0,inputPwd.length()-1);
                }

                if(view.getId() == R.id.password_confirm_btn){
                    if(pwdConfirmListener != null)
                        pwdConfirmListener.onPweConfirm(inputPwd);

                    Log.i("password confirm",""+inputPwd);
                    dismiss();
                }


                tvPwdNum.setText(inputPwd);
//			Log.i("password",""+inputPwd);
            }catch(Exception e){
//                Log.e("pwdButtonOnClickListener",""+e);
            }
        }

    }
}
