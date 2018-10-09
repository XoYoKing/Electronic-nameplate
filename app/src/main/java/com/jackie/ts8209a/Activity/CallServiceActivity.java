package com.jackie.ts8209a.Activity;

import android.os.Bundle;
import android.view.View;
import android.widget.Button;

import com.jackie.ts8209a.CustomView.Dialog.CustomDialog;
import com.jackie.ts8209a.R;

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

        servButtonClickListener listener = new servButtonClickListener();

        btnCoffee.setOnClickListener(listener);
        btnTea.setOnClickListener(listener);
        btnWater.setOnClickListener(listener);
        btnMicphone.setOnClickListener(listener);
        btnFlower.setOnClickListener(listener);
        btnPen.setOnClickListener(listener);
        btnPaper.setOnClickListener(listener);
        btnArtificial.setOnClickListener(listener);
    }

    private class servButtonClickListener implements View.OnClickListener {
        private CustomDialog askDialog = null;

        @Override
        public void onClick(View view) {
            final Button button = (Button) view;

            CustomDialog.Builder builder = new CustomDialog.Builder(actContext, CustomDialog.DOUBLE_BTN);
            builder.setTitle(true,getResources().getString(R.string.call_services)).setContent("是否呼叫服务："+button.getText()).setFisrtBtn("取消").setSecondBtn("确定");
            builder.setButtonClickListerner(new View.OnClickListener() {

                @Override
                public void onClick(View view) {
                    switch (view.getId()) {
                        case R.id.custom_dialog_first_btn:
                            if (askDialog != null)
                                askDialog.dismiss();
                            break;
                        case R.id.custom_dialog_second_btn:
                            networkManager.callService((String) button.getText());
//						Log.d("Call Service", "Call: " + button.getText());
                            PromptBox.BuildPrompt("CALLING_SERVICE").Text("正在为您呼叫服务").Time(1).TimeOut(2000);
                            if (askDialog != null)
                                askDialog.dismiss();
                            break;
                    }
                }
            });
            askDialog = builder.creatDialog();
            askDialog.show();
        }
    }
}
