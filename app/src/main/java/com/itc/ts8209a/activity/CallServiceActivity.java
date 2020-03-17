package com.itc.ts8209a.activity;

import android.content.DialogInterface;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;

import com.itc.ts8209a.activity.view.CustomDialog;
import com.itc.ts8209a.R;
import com.itc.ts8209a.server.Network;

/**
 * Created by kuangyt on 2018/8/21.
 */

public class CallServiceActivity extends AppActivity {

    private Button btnCoffee;
    private Button btnTea;
    private Button btnWater;
    private Button btnMicphone;
    private Button btnFlower;
    private Button btnPen;
    private Button btnPaper;
    private Button btnArtificial;
    private CustomDialog askDialog;

    @Override
    protected void onCreate(Bundle savedInstanceState) {

        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_callservice);

        btnCoffee = ((Button) findViewById(R.id.callservice_coffee));
        btnTea = ((Button) findViewById(R.id.callservice_tea));
        btnWater = ((Button) findViewById(R.id.callservice_water));
        btnMicphone = ((Button) findViewById(R.id.callservice_micphone));
        btnFlower = ((Button) findViewById(R.id.callservice_flower));
        btnPen = ((Button) findViewById(R.id.callservice_pen));
        btnPaper = ((Button) findViewById(R.id.callservice_paper));
        btnArtificial = ((Button) findViewById(R.id.callservice_artificial));

    }

    @Override
    protected void onResume() {
        super.onResume();

        btnCoffee.setOnClickListener(this);
        btnTea.setOnClickListener(this);
        btnWater.setOnClickListener(this);
        btnMicphone.setOnClickListener(this);
        btnFlower.setOnClickListener(this);
        btnPen.setOnClickListener(this);
        btnPaper.setOnClickListener(this);
        btnArtificial.setOnClickListener(this);
    }

    @Override
    public void onClick(View view) {
        super.onClick(view);

        final Button button = (Button) view;

        switch(button.getId())
        {
            case R.id.callservice_water:
            case R.id.callservice_tea:
            case R.id.callservice_pen:
            case R.id.callservice_paper:
            case R.id.callservice_micphone:
            case R.id.callservice_flower:
            case R.id.callservice_coffee:
            case R.id.callservice_artificial:
                if(askDialog != null)
                    break;
                CustomDialog.Builder builder = new CustomDialog.Builder(CallServiceActivity.this, CustomDialog.DOUBLE_BTN);
                builder.setTitle(true,getResources().getString(R.string.call_services)).setContent("是否呼叫服务："+button.getText()).setFisrtBtn("取消").setSecondBtn("确定");
                builder.setButtonClickListerner(new View.OnClickListener() {

                    @Override
                    public void onClick(View view) {
                        switch (view.getId()) {
                            case R.id.custom_dialog_first_btn:
                                if (askDialog != null) {
                                    askDialog.dismiss();
                                    askDialog = null;
                                }
                                break;
                            case R.id.custom_dialog_second_btn:
                                networkManager.callService((String) button.getText());
                                if (networkManager.getNetworkStatus() != Network.SOC_STA_CONNECTED)
                                    PromptBox.BuildPrompt("SERVER_NOT_CONNECTED").Text(getString(R.string.server_dose_not_connect)).Time(1).TimeOut(3000);
                                else
                                    PromptBox.BuildPrompt("CALLING_SERVICE").Text("正在为您呼叫服务").Time(1).TimeOut(2000);
                                if (askDialog != null) {
                                    askDialog.dismiss();
                                    askDialog = null;
                                }
                                break;
                        }
                    }
                });
                askDialog = builder.creatDialog();
                askDialog.setOnDismissListener(new DialogInterface.OnDismissListener() {
                    @Override
                    public void onDismiss(DialogInterface dialog) {
                        askDialog = null;
                    }
                });
                askDialog.show();
                break;

            default:break;

        }
    }
}
