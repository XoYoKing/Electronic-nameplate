package com.jackie.ts8209a.CustomView.Dialog;

import android.content.Context;
import android.content.DialogInterface;
import android.view.View;
import android.widget.ImageButton;
import android.widget.LinearLayout;

import com.bumptech.glide.Glide;
import com.jackie.ts8209a.Application.Func;
import com.jackie.ts8209a.R;

import java.io.File;

/**
 * Created by kuangyt on 2018/10/26.
 */

public class ImagePickerDialog implements View.OnClickListener,DialogInterface.OnDismissListener{
    private Context context;
    private ImageButton[] ibtn;
    private String imgPath = "";
    private OnImageConfirmListener listener;
    private CustomDialog dialog;

    public ImagePickerDialog(Context context){
        this.context = context;
    }

    public void creatPicker(String title, String path,OnImageConfirmListener listener) {
        CustomDialog.Builder builder = new CustomDialog.Builder(context, CustomDialog.HORIZONTAL_LIST);
        this.listener = listener;

        builder.setTitle(true, title);
        builder.setFisrtBtn("确定");
        builder.setButtonClickListerner(this);

        String[] allNameplateBG = Func.FilePath.getFilesAbsolutePath(path, ".jpg");
        ibtn = new ImageButton[allNameplateBG.length];

        for (int i = 0; i < allNameplateBG.length; i++) {
            File file = new File(allNameplateBG[i]);
            ibtn[i] = new ImageButton(context);
            LinearLayout.LayoutParams para = new LinearLayout.LayoutParams(266, 160);
            para.setMargins(5, 5, 5, 5);
            ibtn[i].setLayoutParams(para);
            ibtn[i].setPadding(5, 5, 5, 5);
            ibtn[i].setContentDescription(allNameplateBG[i]);
            ibtn[i].setBackgroundResource(R.drawable.btn_img_picker);
            ibtn[i].setOnClickListener(this);
            Glide.with(context).load(file).override(256, 150).into(ibtn[i]);
            builder.addListView(ibtn[i]);
        }

        dialog = builder.creatDialog();
        dialog.setOnDismissListener(this);
        dialog.show();
    }

    @Override
    public void onClick(View view) {
        if(view.getId() == R.id.custom_dialog_first_btn){
            if(listener != null)
                listener.imgConfirm(imgPath);
            dialog.dismiss();
        }else {
            for(int i=0;i<ibtn.length;i++){
                ibtn[i].setSelected(false);
            }
            view.setSelected(true);
            imgPath = view.getContentDescription().toString();
        }
    }

    @Override
    public void onDismiss(DialogInterface dialog) {
        Glide.get(context).clearMemory();
    }

    public interface OnImageConfirmListener{
        void imgConfirm(String imgPath);
    }
}
