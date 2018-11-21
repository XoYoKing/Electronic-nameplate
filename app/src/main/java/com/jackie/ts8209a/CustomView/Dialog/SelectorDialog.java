package com.jackie.ts8209a.CustomView.Dialog;

import android.app.Dialog;
import android.content.Context;
import android.graphics.Color;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.CheckedTextView;
import android.widget.CompoundButton;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.jackie.ts8209a.R;

import java.util.ArrayList;

/**
 * Created by kuangyt on 2018/10/10.
 */

public class SelectorDialog extends Dialog {
    public static final int SINGLE_SELECTION = 1;
    public static final int MULTI_SELECTION = 2;

    private Context context;
    private int selectMode = SINGLE_SELECTION;

    private View dialogLayout;
    private Button btnConfirm;
    private LinearLayout layContent;
    private TextView tvTitle;
    private CheckBox cbSelectAll;

    private String[] strContent;
    private CheckedTextView[] ctvContent;
    private boolean[] checked;

    private OnSelectConfirmListener confirmListener = null;

    public SelectorDialog(Context context,String[] content) {
        super(context, R.style.custom_dialog);

        this.context = context;

        LayoutInflater inflater = (LayoutInflater) context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        dialogLayout = inflater.inflate(R.layout.dialog_selector,null);


        btnConfirm = (Button)dialogLayout.findViewById(R.id.selector_dialog_confirm_btn);
        layContent = (LinearLayout)dialogLayout.findViewById(R.id.selector_dialog_content_lay);
        tvTitle = (TextView)dialogLayout.findViewById(R.id.selector_dialog_title_tv);
        cbSelectAll = (CheckBox)dialogLayout.findViewById(R.id.selector_dialog_check_all_cb);

        cbSelectAll.setOnCheckedChangeListener(new allSelectCheckListener());
        btnConfirm.setOnClickListener(new confirmButtonClickListener());

        modeConfig();

        setContent(content);

        this.setContentView(dialogLayout);
        this.setCanceledOnTouchOutside(true);
    }


    private void setContent(String[] content){
        strContent = content;

        ctvContent = new CheckedTextView[strContent.length];
        checked = new boolean[strContent.length];
        contentCheckClickListener listener = new contentCheckClickListener();

        for(int i=0;i<strContent.length;i++){
            ctvContent[i] = new CheckedTextView(context);
            LinearLayout.LayoutParams para = new LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT);

            para.setMargins(2,2,2,2);

            ctvContent[i].setLayoutParams(para);
            ctvContent[i].setText(strContent[i]);
            ctvContent[i].setTextSize(20);
            ctvContent[i].setTextColor(Color.WHITE);
            ctvContent[i].setPadding(15,5,10,5);
            ctvContent[i].setCheckMarkDrawable(R.drawable.btn_edit_choice_tick_32x32);
            ctvContent[i].setMinHeight(58);
            ctvContent[i].setGravity(Gravity.CENTER_VERTICAL);
            ctvContent[i].setBackground(context.getResources().getDrawable(R.drawable.btn_11ffffff_5r));
            ctvContent[i].setClickable(true);
            ctvContent[i].setOnClickListener(listener);


            layContent.addView(ctvContent[i]);
        }
    }

    public SelectorDialog setChecked(boolean[] checked){
        if(ctvContent.length == checked.length){
            this.checked = checked;
            for(int i=0;i<checked.length;i++){
                ctvContent[i].setChecked(checked[i]);
            }
        }
        return this;
    }

//	public boolean[] getChecked(){
//		return checked;
//	}

    public SelectorDialog setTitle(String title){
        tvTitle.setText(title);
        return this;
    }

    public SelectorDialog setMode(int mode){
        selectMode = mode;
        modeConfig();
        return this;
    }

    public interface OnSelectConfirmListener{
        abstract void multiSelect(boolean[] checked,String[] content);
        abstract void singleSelect(int choice,String content);
    }

    public SelectorDialog setOnSelectConfirmListener(OnSelectConfirmListener listener){
        confirmListener = listener;
        return this;
    }

    private void modeConfig(){
        switch (selectMode) {
            case SINGLE_SELECTION:
                cbSelectAll.setVisibility(View.GONE);
                break;
            case MULTI_SELECTION:
                cbSelectAll.setVisibility(View.VISIBLE);
                break;
            default:
                break;
        }
    }

    private class contentCheckClickListener implements View.OnClickListener{

        @Override
        public void onClick(View view) {
            CheckedTextView ctv = (CheckedTextView)view;
            switch (selectMode) {
                case SINGLE_SELECTION:
                    for(int i = 0;i<ctvContent.length;i++){
                        ctvContent[i].setChecked(false);
                    }
                    ctv.setChecked(true);

                    break;
                case MULTI_SELECTION:

                    ctv.setChecked(!ctv.isChecked());

                    break;
                default:
                    break;
            }
        }

    }

    private class allSelectCheckListener implements CompoundButton.OnCheckedChangeListener {

        @Override
        public void onCheckedChanged(CompoundButton btn, boolean bl) {
            if(selectMode != MULTI_SELECTION)
                return;

            for(int i = 0;i<ctvContent.length;i++){
                ctvContent[i].setChecked(bl);
            }
        }

    }

    private class confirmButtonClickListener implements android.view.View.OnClickListener{

        @Override
        public void onClick(View view) {
            if(confirmListener != null){
                switch (selectMode) {
                    case MULTI_SELECTION:
                        ArrayList<String> strConfirm = new ArrayList<String>();

                        for (int i = 0; i < ctvContent.length; i++) {
                            checked[i] = ctvContent[i].isChecked();
                            if (checked[i]) {
                                strConfirm.add((String) ctvContent[i].getText());
                            }
                        }

                        confirmListener.multiSelect(checked, strConfirm.toArray(new String[strConfirm.size()]));
                        break;
                    case SINGLE_SELECTION:
                        int choice;

                        for (int i = 0; i < ctvContent.length; i++) {
                            if(ctvContent[i].isChecked()){
                                choice = i;
                                confirmListener.singleSelect(choice, (String) ctvContent[choice].getText());
                                break;
                            }
                        }
                        break;
                    default:
                        break;
                }

            }
            dismiss();
        }

    }
}
