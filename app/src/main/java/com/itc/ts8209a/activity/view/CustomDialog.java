package com.itc.ts8209a.activity.view;

import android.app.Dialog;
import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.itc.ts8209a.R;

/**
 * Created by kuangyt on 2018/8/28.
 */

public class CustomDialog extends Dialog {
    public static final int SINGLE_BTN = 0;
    public static final int DOUBLE_BTN = 1;
    public static final int TRIPLE_BTN = 2;
    public static final int VERTICAL_LIST = 3;
    public static final int HORIZONTAL_LIST = 4;

    protected CustomDialog(Context context) {
        super(context);
    }

    protected CustomDialog(Context context, int theme) {
        super(context, theme);
    }

    public static class Builder{

        private static final int[] layoutList = {R.layout.dialog_single_btn, R.layout.dialog_double_btn, R.layout.dialog_triple_btn,
                R.layout.dialog_vertical_list, R.layout.dialog_horizontal_list};

        private CustomDialog dialog;
        private int type;
        private Context context;
        private View layout;
        private boolean touchCancel = true;

        private LinearLayout layTitle = null;
        private TextView tvTitle = null;
        private TextView tvContent = null;
        private Button btnFirst = null;
        private Button btnSecond = null;
        private Button btnThird = null;
        private LinearLayout layList = null;

        public Builder(Context context,int type){
            this.context = context;
            dialog = new CustomDialog(context, R.style.custom_dialog);
            LayoutInflater inflater = (LayoutInflater) context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
            this.type = (type >= 0 && type < layoutList.length) ? type : 0;
            layout = inflater.inflate(layoutList[this.type], null);
//            dialog.addContentView(layout, new ViewGroup.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT));
            viewInit();
        }

        private void viewInit(){
            layTitle = (LinearLayout) layout.findViewById(R.id.custom_dialog_title_lay);
            tvTitle = (TextView) layout.findViewById(R.id.custom_dialog_title_tv);

            if(type == SINGLE_BTN || type == DOUBLE_BTN || type == TRIPLE_BTN){
                btnFirst = (Button) layout.findViewById(R.id.custom_dialog_first_btn);
                tvContent = (TextView)layout.findViewById(R.id.custom_dialog_content_tv);
                if(type == DOUBLE_BTN || type == TRIPLE_BTN){
                    btnSecond = (Button) layout.findViewById(R.id.custom_dialog_second_btn);
                    if(type == TRIPLE_BTN)
                        btnThird = (Button) layout.findViewById(R.id.custom_dialog_third_btn);
                }
            }else if(type == VERTICAL_LIST || type == HORIZONTAL_LIST){
                btnFirst = (Button) layout.findViewById(R.id.custom_dialog_first_btn);
                layList = (LinearLayout)layout.findViewById(R.id.custom_dialog_list_lay);
            }
        }

        public Builder setTitle(boolean showTitle,String title){
            if(layTitle == null || tvTitle == null)
                return this;
            if(showTitle){
                layTitle.setVisibility(View.VISIBLE);
                if(title != null)
                    tvTitle.setText(title);
            }else{
                layTitle.setVisibility(View.GONE);
            }
            return this;
        }

        public Builder setContent(String content){
            if(tvContent == null)
                return this;
            tvContent.setText(content);
            return this;
        }

        public Builder setFisrtBtn(String content){
            if(btnFirst == null)
                return this;
            btnFirst.setVisibility(View.VISIBLE);
            btnFirst.setText(content);
            return this;
        }

        public Builder setSecondBtn(String content){
            if(btnSecond == null)
                return this;
            btnSecond.setText(content);
            return this;
        }

        public Builder setThirdBtn(String content){
            if(btnThird == null)
                return this;
            btnThird.setText(content);
            return this;
        }

        public Builder addListView(View view){
            if(layList == null)
                return this;
            layList.addView(view);
            return this;
        }

        public Builder setCanceledOnTouchOutside(boolean enable){
            touchCancel = enable;
            return this;
        }

        public Builder setButtonClickListerner(View.OnClickListener listener){
            if(btnFirst != null)
                btnFirst.setOnClickListener(listener);
            if(btnSecond != null)
                btnSecond.setOnClickListener(listener);
            if(btnThird != null)
                btnThird.setOnClickListener(listener);
            return this;
        }
//
//        public Builder setConfirmButton(boolean btnEnable){
//            if((type == VERTICAL_LIST || type == HORIZONTAL_LIST) && btnFirst != null)
//                btnFirst.setVisibility(btnEnable ? View.VISIBLE : View.GONE);
//            return this;
//        }

        public CustomDialog creatDialog(){
            dialog.setContentView(layout);
//            dialog.setContentView(layout, new ViewGroup.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT));
            dialog.setCanceledOnTouchOutside(touchCancel);
            return dialog;
        }
    }

}
