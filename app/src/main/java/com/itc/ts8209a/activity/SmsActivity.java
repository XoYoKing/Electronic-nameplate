package com.itc.ts8209a.activity;

import android.graphics.Color;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;
import android.view.Gravity;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;
import android.widget.ScrollView;
import android.widget.TextView;

import com.itc.ts8209a.activity.view.CheckSelectorDialog;
import com.itc.ts8209a.R;
import com.itc.ts8209a.module.network.WifiManager;
import com.itc.ts8209a.server.Network;
import com.itc.ts8209a.widget.Debug;

import java.util.ArrayList;

/**
 * Created by kuangyt on 2018/8/21.
 */

public class SmsActivity extends AppActivity implements TextWatcher{

    private Button btnAddressee;
    private ImageButton btnMsgRecord;
    private ImageButton btnSend;
    private LinearLayout layAddressee;
    private EditText etContent;
    private LinearLayout layReceContent;

    private String[] smsAddressee = {""};
    private int[] smsAddresseeId;
    private boolean smsReceiveContent = false;
    private boolean addrChecked[] = null;
    private ArrayList<String> addresseeChoiceList = new ArrayList<String>();
    private String msgContent = "";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_sms);

        btnAddressee = (Button) findViewById(R.id.sms_choice_addressee_btn);
        btnSend = (ImageButton) findViewById(R.id.sms_send_btn);
        btnMsgRecord = (ImageButton) findViewById(R.id.sms_msg_record_btn);
        layAddressee = (LinearLayout) findViewById(R.id.sms_addressee_lay);
        etContent = (EditText) findViewById(R.id.sms_content_et);
        layReceContent = (LinearLayout) findViewById(R.id.sms_receive_content_lay);
    }

    @Override
    protected void onResume() {
        super.onResume();

        btnMsgRecord.setOnClickListener(this);
        btnAddressee.setOnClickListener(this);
        btnSend.setOnClickListener(this);
        etContent.addTextChangedListener(this);
    }

    @Override
    protected void uiRefresh() {
        super.uiRefresh();

        getUserList();
        oldSMS = databaseManager.getSmsMsg();

        if (smsAddressee != null) {
//            Log.i("SMS Resume","addressee length = "+smsAddressee.length);
            addrChecked = new boolean[smsAddressee.length];
        } else
            btnAddressee.setEnabled(false);

        if (!newSMS.isEmpty()) {
            displaySmsRecord();
        }
        PromptBox.removePrompt("YOU_HAVE_A_SMS");
    }

    private void displaySmsRecord() {
        smsReceiveContent = true;
        ( findViewById(R.id.sms_edit_send_lay)).setVisibility(View.GONE);
        ( findViewById(R.id.sms_receive_content_sv)).setVisibility(View.VISIBLE);
        layReceContent.removeAllViews();

        if(newSMS != null) {
            for (int i = 0; i < newSMS.size(); i++) {
                String[] sms = newSMS.get(i);

                LinearLayout layout = new LinearLayout(this);
                LinearLayout.LayoutParams layParams = new LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT);
                layParams.setMargins(0, 5, 0, 5);
                layout.setLayoutParams(layParams);
                layout.setPadding(10, 5, 10, 5);
                layout.setBackground(getResources().getDrawable(R.drawable.bg_sms_new_content));
                layout.setOrientation(LinearLayout.VERTICAL);
                layout.setClickable(true);

                TextView smsNew = new TextView(this);
                smsNew.setText("新消息");
                smsNew.setGravity(Gravity.CENTER);
                smsNew.setTextColor(Color.WHITE);
                smsNew.setTextSize(15);

                layout.addView(smsNew);

                TextView smsName = new TextView(this);
                smsName.setLayoutParams(layParams);
                smsName.setText("来自 : " + sms[0]);
                smsName.setTextColor(Color.GRAY);
                smsName.setTextSize(20);

                layout.addView(smsName);

                TextView smsContent = new TextView(this);
                smsContent.setLayoutParams(layParams);
                smsContent.setText(sms[1]);
                smsContent.setTextColor(Color.WHITE);
                smsContent.setTextSize(24);

                layout.addView(smsContent);

                layReceContent.addView(layout);

//			newSMS.remove(i);
//			oldSMS.add(sms);
            }
        }

        if(oldSMS != null) {
            for (int i = 0; i < oldSMS.size(); i++) {
                String[] sms = oldSMS.get(i);

                LinearLayout layout = new LinearLayout(this);
                LinearLayout.LayoutParams layParams = new LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT);
                layParams.setMargins(0, 5, 0, 5);
                layout.setLayoutParams(layParams);
                layout.setPadding(10, 5, 10, 5);
                layout.setBackground(getResources().getDrawable(R.drawable.bg_sms_old_content));
                layout.setOrientation(LinearLayout.VERTICAL);
                layout.setClickable(true);

                TextView smsName = new TextView(this);
                smsName.setLayoutParams(layParams);
                smsName.setText(sms[0]);
                smsName.setTextColor(Color.GRAY);
                smsName.setTextSize(22);

                layout.addView(smsName);

                TextView smsContent = new TextView(this);
                smsContent.setLayoutParams(layParams);
                smsContent.setText(sms[1]);
                smsContent.setTextColor(Color.WHITE);
                smsContent.setTextSize(24);

                layout.addView(smsContent);

                layReceContent.addView(layout);
            }
        }

        if (!newSMS.isEmpty()) {
            for (int i = 0; i < newSMS.size(); i++) {
                String[] sms = newSMS.get(i);
//				newSMS.remove(0);
                oldSMS.add(sms);
            }
            newSMS.removeAll(newSMS);
        }
    }

    private void undisplaySmsRecord() {
        smsReceiveContent = false;
        (findViewById(R.id.sms_receive_content_sv)).setVisibility(View.GONE);
        (findViewById(R.id.sms_edit_send_lay)).setVisibility(View.VISIBLE);
    }

    private void getUserList() {
        if (userList == null || userList.size() < 1)
            return;
        smsAddressee = new String[userList.size()];
        smsAddresseeId = new int[userList.size()];

        int i = 0;
        for (Integer key : userList.keySet()) {
            Log.d(TAG,"Key = " + key + " Value: "+userList.get(key));
            smsAddressee[i] = userList.get(key);
            smsAddresseeId[i] = key;
            i++;
        }
    }

    private int getUserID(String name) {
        int id = 0;

        for (Integer key : userList.keySet()) {
//            Log.d(TAG,"Key = " + key + " Value: "+userList.get(key));
            if (name.equals(userList.get(key)))
                id = key;
        }

        return id;
    }

    private int[] getUserID(){
        ArrayList<Integer> idList = new ArrayList<Integer>();

        for(int i=0;i<addrChecked.length;i++){
            if(addrChecked[i]){
                idList.add(smsAddresseeId[i]);
            }
        }

        int[] id = new int[idList.size()];

        for(int j=0;j<idList.size();j++){
            id[j] = idList.get(j);
        }

        return id;
    }


    @Override
    public void onClick(View view) {
        super.onClick(view);

        switch (view.getId()){
            case R.id.sms_choice_addressee_btn:
                if (userList == null|| smsAddressee == null || userList.size() == 0 ) {
                    btnAddressee.setEnabled(false);
                    return;
                }

                CheckSelectorDialog dialog = new CheckSelectorDialog(SmsActivity.this, smsAddressee);
                dialog.setMode(CheckSelectorDialog.MULTI_SELECTION).setOnSelectConfirmListener(new addresseeConfirmListener()).setChecked(addrChecked).show();
                break;
            case R.id.sms_send_btn:
                if (addresseeChoiceList.size() == 0) {
                    PromptBox.BuildPrompt("PLEASE_CHOISE_ADDRESSEE").Text(getString(R.string.please_select_the_addressee)).Time(1).TimeOut(5000);
                } else if (msgContent.equals("")) {
                    PromptBox.BuildPrompt("CONTENT_CAN_NOT_EMPTY").Text(getString(R.string.text_messaging_cannot_be_empty)).Time(1).TimeOut(5000);
                } else if(networkManager.getNetworkStatus() != Network.STA_CONNECTED){
                    PromptBox.BuildPrompt("SERVER_NOT_CONNECTED").Text(getString(R.string.server_dose_not_connect)).Time(1).TimeOut(5000);
                }
                else {
                    PromptBox.BuildPrompt("SMS_SEND_SUCCESS").Text(getString(R.string.text_messaging_success)).Time(1).TimeOut(5000);

                    int id[] = getUserID();

                    if (id != null) {
                        for (int i = 0; i < id.length; i++) {
                            Debug.d(TAG, id[i] + " . " + userList.get(id[i]));
                            networkManager.sendSms(id[i], msgContent);
                        }
                    }

//                    String[] addresseeChoice = addresseeChoiceList.toArray(new String[addresseeChoiceList.size()]);
//                    for (String anAddresseeChoice : addresseeChoice) {
//                        if (anAddresseeChoice == null) {
//                        }
//                        else {
//                            int id[] = getUserID();
//
//                            if (id != null) {
//                                for(int i=0;i<id.length;i++) {
//                                    Debug.d(TAG, id[i] + " . " + anAddresseeChoice);
//                                    networkManager.sendSms(id[i], msgContent);
//                                }
//                            }
//                        }
//                    }
                    layAddressee.removeAllViews();
                    etContent.setText("");
                    msgContent = "";
                    addresseeChoiceList.removeAll(addresseeChoiceList);
                    for (int i = 0; i < addrChecked.length; i++)
                        addrChecked[i] = false;

                }
                break;
            case R.id.sms_msg_record_btn:
                ImageButton btn = (ImageButton) view;

                if (smsReceiveContent) {
                    undisplaySmsRecord();
                    btn.setImageResource(R.drawable.ico_lsjl_n);
                    ((TextView) findViewById(R.id.sms_msg_record_tv)).setText(getString(R.string.sms_record));
                } else {
                    displaySmsRecord();
                    btn.setImageResource(R.drawable.ico_fasong_n);
                    ((TextView) findViewById(R.id.sms_msg_record_tv)).setText(getString(R.string.edit_the_message));
                }
                break;
        }
    }

    @Override
    public void beforeTextChanged(CharSequence s, int start, int count, int after) {
    }

    @Override
    public void onTextChanged(CharSequence s, int start, int before, int count) {
    }

    @Override
    public void afterTextChanged(Editable s) {
        msgContent = s.toString();
    }

    private class addresseeConfirmListener implements CheckSelectorDialog.OnSelectConfirmListener,View.OnClickListener {

        @Override
        public void multiSelect(boolean[] checked, String[] content) {
            addrChecked = checked;

            LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(LinearLayout.LayoutParams.WRAP_CONTENT, LinearLayout.LayoutParams.WRAP_CONTENT);
            params.setMargins(5, 0, 0, 0);
            Drawable drawRemove = getResources().getDrawable(R.drawable.ico_yichu_n);
            drawRemove.setBounds(0, 0, drawRemove.getMinimumWidth(), drawRemove.getMinimumHeight());

//            addresseeRemoveClickListener listener = new addresseeRemoveClickListener();

            layAddressee.removeAllViews();
            addresseeChoiceList.clear();
            for (int i = 0; i < checked.length; i++) {
                if (checked[i]) {
                    // smsAddresseeStr[i] = new String(smsAddressee[i]);
                    addresseeChoiceList.add(smsAddressee[i]);

                    Button btn = new Button(SmsActivity.this);
                    btn.setLayoutParams(params);
                    btn.setText(smsAddressee[i]);
                    btn.setTextSize(20);
                    btn.setTextColor(Color.WHITE);
                    btn.setCompoundDrawables(null, null, drawRemove, null);
                    btn.setBackground(getResources().getDrawable(R.drawable.btn_addressee_chosen));
                    btn.setOnClickListener(this);

                    layAddressee.addView(btn);
                }
            }
        }

        @Override
        public void singleSelect(int choice, String content) {
        }

        @Override
        public void onClick(View view) {
            Button btn = (Button) view;

            for (int i = 0; i < addresseeChoiceList.size(); i++) {
                if (addresseeChoiceList.get(i).equals(btn.getText())) {
                    addresseeChoiceList.remove(i);
//					choiced[i] = false;
//					Log.d("addresseeRemoveClickListener","Remove: "+btn.getText());
                }
            }

            for (int i = 0; i < smsAddressee.length; i++) {
                if (btn.getText().equals(smsAddressee[i])) {
                    addrChecked[i] = false;
                }
            }
            layAddressee.removeView(view);
        }
    }

}
