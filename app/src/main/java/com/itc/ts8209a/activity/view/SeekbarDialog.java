package com.itc.ts8209a.activity.view;

import android.app.Dialog;
import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.Button;
import android.widget.SeekBar;
import android.widget.TextView;

import com.itc.ts8209a.R;

/**
 * Created by kuangyt on 2018/10/24.
 */

public class SeekbarDialog extends Dialog {
    private SeekBar seekBar;
    private Button btnConfirm;
    private TextView tvTitle;
    private TextView tvProgressNum;
    private View dialogLayout;

    private onProgressChangeListener progressListener = null;
    private onSeekbarConfirmListener confirmListener = null;

    private int progressNum;
    private int pMin = 0;
    private int pMax = 100;


    public SeekbarDialog(Context context, String title) {
        super(context, R.style.custom_dialog);
        LayoutInflater inflater = (LayoutInflater) context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        dialogLayout = inflater.inflate(R.layout.dialog_seekbar,null);

        tvTitle = (TextView) dialogLayout.findViewById(R.id.seekbar_dialog_title_tv);
        seekBar = (SeekBar) dialogLayout.findViewById(R.id.seekbar_dialog_seekbar);
        btnConfirm = (Button)dialogLayout.findViewById(R.id.seekbar_dialog_confirm_btn);
        tvProgressNum = (TextView)dialogLayout.findViewById(R.id.seekbar_dialog_progress_num_tv);

        this.setContentView(dialogLayout);
        this.setCanceledOnTouchOutside(true);			//用户不能通过点击对话框之外的地方取消对话框显示

//		progressNum = progress;
//		seekBar.setProgress((progressNum-pMin)*100/(pMax-pMin));
        tvTitle.setText(title);
//		tvProgressNum.setText(progress+"");


        seekBar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {

            @Override
            public void onStopTrackingTouch(SeekBar seekbar) {
                // TODO Auto-generated method stub

            }

            @Override
            public void onStartTrackingTouch(SeekBar seekbar) {
                // TODO Auto-generated method stub

            }

            @Override
            public void onProgressChanged(SeekBar seekBar, int progress,boolean fromUser) {
                progressNum = (pMax-pMin) * progress / 100 + pMin;
                tvProgressNum.setText(""+progressNum);
                if (progressListener != null)
                    progressListener.onChange(progressNum);
            }
        });

        btnConfirm.setOnClickListener(new View.OnClickListener() {

            @Override
            public void onClick(View view) {
                if(confirmListener != null)
                    confirmListener.onConfirm(progressNum);
            }
        });
    }


    @Override
    public void setTitle(CharSequence title) {
        tvTitle.setText(title);
    }

    public void setProgress(int progress,int min,int max){
        if(min >= max || min < 0 || max > 100)
            return;

        pMin = min;
        pMax = max;
        progressNum = progress;


        progressNum = progressNum < pMin ? pMin : progressNum;
        progressNum = progressNum > pMax ? pMax : progressNum;

        seekBar.setProgress((progressNum-pMin)*100/(pMax-pMin));
        tvProgressNum.setText(progress+"");
    }


    public void setSeekBarkTextView(int num)
    {
        tvProgressNum.setText(num+"");
    }

    /* 设置监听对象 */
    public void setOnProgressChangeListener(onProgressChangeListener listener) {
        this.progressListener = listener;
    }

    public void setonSeekbarConfirmListener(onSeekbarConfirmListener listener) {
        this.confirmListener = listener;
    }

    public interface onProgressChangeListener {
        public void onChange(int progress);
    }

    public interface onSeekbarConfirmListener {
        public void onConfirm(int progress);
    }



}
