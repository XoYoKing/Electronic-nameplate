package com.jackie.ts8209a.Activity;

import android.app.AlertDialog;
import android.content.DialogInterface;
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

import com.jackie.ts8209a.CustomView.Dialog.SelectorDialog;
import com.jackie.ts8209a.R;

import java.io.UnsupportedEncodingException;
import java.util.ArrayList;

/**
 * Created by kuangyt on 2018/8/21.
 */

public class SmsActivity extends AppActivity {

    private static Button btnAddressee;
    private ImageButton btnSend;
    private LinearLayout layAddressee;
    private EditText etContent;
    private LinearLayout layReceContent;

    private String[] smsAddressee = {""};
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
        layAddressee = (LinearLayout) findViewById(R.id.sms_addressee_lay);
        etContent = (EditText) findViewById(R.id.sms_content_et);
        layReceContent = (LinearLayout) findViewById(R.id.sms_receive_content_lay);
        (findViewById(R.id.sms_receive_content_btn)).setOnClickListener(new smsReceiveContentButtonListener());

        btnAddressee.setOnClickListener(new addresseeChoiceClickLisgener());
        btnSend.setOnClickListener(new smsSendButtonListener());
        etContent.addTextChangedListener(new smsContentListener());
    }

    @Override
    protected void onResume() {
        super.onResume();

        smsAddressee = getUserList();

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

//			newSMS.remove(i);
//			oldSMS.add(sms);
        }

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
        ((ScrollView) findViewById(R.id.sms_receive_content_sv)).setVisibility(View.GONE);
        ((RelativeLayout) findViewById(R.id.sms_edit_send_lay)).setVisibility(View.VISIBLE);
    }

    private String[] getUserList() {
        if (userList == null || userList.size() < 1)
            return null;
        String[] retStr = new String[userList.size()];

        int i = 0;
        for (Integer key : userList.keySet()) {
//            Log.d(TAG,"Key = " + key + " Value: "+userList.get(key));
            retStr[i++] = userList.get(key);
        }
        return retStr;
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

    private class addresseeChoiceClickLisgener implements View.OnClickListener {

        @Override
        public void onClick(View view) {
            if (userList.size() == 0 || smsAddressee == null) {
                btnAddressee.setEnabled(false);
                return;
            }

            SelectorDialog dialog = new SelectorDialog(SmsActivity.this, smsAddressee);
            dialog.setMode(SelectorDialog.MULTI_SELECTION).setOnSelectConfirmListener(new addresseeConfirmListener()).setChecked(addrChecked).show();
        }
    }

//    private class addresseeMultiChoiceListener implements DialogInterface.OnMultiChoiceClickListener {
//
//        @Override
//        public void onClick(DialogInterface dialog, int witch, boolean bl) {
//            addrChecked[witch] = bl;
//        }
//    }

    private class addresseeConfirmListener implements SelectorDialog.OnSelectConfirmListener {

        @Override
        public void multiSelect(boolean[] checked, String[] content) {
            addrChecked = checked;

            LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(LinearLayout.LayoutParams.WRAP_CONTENT, LinearLayout.LayoutParams.WRAP_CONTENT);
            params.setMargins(5, 0, 0, 0);
            Drawable drawRemove = getResources().getDrawable(R.drawable.ico_yichu_n);
            drawRemove.setBounds(0, 0, drawRemove.getMinimumWidth(), drawRemove.getMinimumHeight());

            addresseeRemoveClickListener listener = new addresseeRemoveClickListener();

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
                    btn.setOnClickListener(listener);

                    layAddressee.addView(btn);
                }
            }
        }

        @Override
        public void singleSelect(int choice, String content) {
        }
    }

    private class addresseeRemoveClickListener implements View.OnClickListener {

        @Override
        public void onClick(View view) {
            Button btn = (Button) view;
//			int remove = Integer.parseInt((String) btn.getContentDescription());

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

    private class smsReceiveContentButtonListener implements View.OnClickListener {

        @Override
        public void onClick(View view) {
            ImageButton btn = (ImageButton) view;

            if (smsReceiveContent) {
                undisplaySmsRecord();
                btn.setImageResource(R.drawable.ico_lsjl_n);
                ((TextView) findViewById(R.id.sms_receive_content_tv)).setText(getResources().getString(R.string.sms_record));
            } else {
                displaySmsRecord();
                btn.setImageResource(R.drawable.ico_fasong_n);
                ((TextView) findViewById(R.id.sms_receive_content_tv)).setText(getResources().getString(R.string.edit_the_message));
            }
        }

    }

    private class smsSendButtonListener implements View.OnClickListener {

        @Override
        public void onClick(View view) {
            if (addresseeChoiceList.size() == 0) {
                PromptBox.BuildPrompt("PLEASE_CHOISE_ADDRESSEE").Text((String) getResources().getText(R.string.please_select_the_addressee)).Time(1).TimeOut(5000);
            } else if (msgContent.equals("")) {
                PromptBox.BuildPrompt("CONTENT_CAN_NOT_EMPTY").Text((String) getResources().getText(R.string.text_messaging_cannot_be_empty)).Time(1).TimeOut(5000);
            } else {
                PromptBox.BuildPrompt("SMS_SEND_SUCCESS").Text((String) getResources().getText(R.string.text_messaging_success)).Time(1).TimeOut(5000);


                String[] addresseeChoice = addresseeChoiceList.toArray(new String[addresseeChoiceList.size()]);
                for (int i = 0; i < addresseeChoice.length; i++) {
                    if (addresseeChoice[i] == null)
                        continue;
                    else {
                        int id = getUserID(addresseeChoice[i]);

                        if (id >= 0) {
                            Log.d(TAG, id + " . " + addresseeChoice[i]);
                            networkManager.sendSms(id, msgContent);
                        }
                    }
                }
                layAddressee.removeAllViews();
                etContent.setText("");
                msgContent = "";
                addresseeChoiceList.removeAll(addresseeChoiceList);
                for (int i = 0; i < addrChecked.length; i++)
                    addrChecked[i] = false;

            }
        }
    }

    private class smsContentListener implements TextWatcher {

        @Override
        public void afterTextChanged(Editable str) {
            msgContent = str.toString();
//			Log.d("msg Content",""+msgContent);
        }

        @Override
        public void beforeTextChanged(CharSequence arg0, int arg1, int arg2, int arg3) {
        }

        @Override
        public void onTextChanged(CharSequence arg0, int arg1, int arg2, int arg3) {
        }

    }
}
