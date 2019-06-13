package com.itc.ts8209a.activity.view;

import android.app.Dialog;
import android.content.Context;
import android.graphics.Color;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.annotation.StyleRes;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.Button;
import android.widget.CheckedTextView;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.itc.ts8209a.R;

import org.w3c.dom.Text;

/**
 * Created by kuangyt on 2019/4/25.
 */

public class ButtonSelectorDialog extends Dialog {

    private Context context;

    private View dialogLayout;
    private LinearLayout layContent;
    private LinearLayout layDialog;
    private TextView tvTitle;

    private String[] strContent;
    private Button[] btnContent;

    private OnSelectConfirmListener confirmListener;

    public ButtonSelectorDialog(Context context, String[] content) {
        super(context, R.style.custom_dialog);

        this.context = context;
        LayoutInflater inflater = (LayoutInflater) context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        dialogLayout = inflater.inflate(R.layout.dialog_button_selector,null);

        layDialog = (LinearLayout)dialogLayout.findViewById(R.id.buttonselector_dialog_lay);
        layContent = (LinearLayout)dialogLayout.findViewById(R.id.buttonselector_dialog_content_lay);
        tvTitle = (TextView)dialogLayout.findViewById(R.id.buttonselector_dialog_title_tv);

        setContent(content);
        setContentView(dialogLayout);
        setCanceledOnTouchOutside(true);
    }

    private void setContent(String[] content){
        strContent = content;

        btnContent = new Button[strContent.length];
        contentButtonClickListener listener = new contentButtonClickListener();

        for(int i=0;i<strContent.length;i++){
            btnContent[i] = new Button(context);
            LinearLayout.LayoutParams para = new LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT);

            para.setMargins(2,2,2,2);

            btnContent[i].setLayoutParams(para);
            btnContent[i].setText(strContent[i]);
            btnContent[i].setTextSize(20);
            btnContent[i].setTextColor(Color.WHITE);
            btnContent[i].setPadding(15,5,10,5);
            btnContent[i].setGravity(Gravity.CENTER_VERTICAL);
            btnContent[i].setBackground(context.getResources().getDrawable(R.drawable.btn_11ffffff_5r));
            btnContent[i].setOnClickListener(listener);
            btnContent[i].setContentDescription(""+i);

            layContent.addView(btnContent[i]);
        }
    }

    private class contentButtonClickListener implements View.OnClickListener{

        @Override
        public void onClick(View view) {
            Button btn = (Button)view;
            if(confirmListener != null){
                for(int i=0;i<strContent.length;i++){
                    if(btn.getContentDescription().equals(""+i))
                        confirmListener.selectConfirm(i,strContent[i]);
                }
            }
            dismiss();
        }
    }

    public ButtonSelectorDialog setTitle(String title){
        tvTitle.setText(title);
        return this;
    }

    public interface OnSelectConfirmListener{
        void selectConfirm(int choice,String content);
    }

    public ButtonSelectorDialog setOnSelectConfirmListener(OnSelectConfirmListener listener){
        confirmListener = listener;
        return this;
    }
}
