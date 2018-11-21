package com.jackie.ts8209a.CustomView.View;

import android.content.Context;
import android.content.res.TypedArray;
import android.support.annotation.Nullable;
import android.util.AttributeSet;
import android.view.MotionEvent;
import android.view.View;
import android.widget.TextView;

import com.jackie.ts8209a.R;


/**
 * Created by kuangyt on 2018/10/23.
 */

public class MoveTextView extends TextView {
    private OnMovementFinishListener moveListener = null;

    private int xMax = 0;
    private int xMin = 0;
    private int yMax = 0;
    private int yMin = 0;

    private float moveX = 0;
    private float moveY = 0;

    private float tranX = 0;
    private float tranY = 0;

    public MoveTextView(Context context) {
        super(context);
    }

    public MoveTextView(Context context, @Nullable AttributeSet attrs) {
        super(context, attrs);
        setCustomAttributes(context,attrs);
    }

    public MoveTextView(Context context, @Nullable AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
        setCustomAttributes(context,attrs);
    }

    public interface OnMovementFinishListener{
        void onMovement(View view, float x, float y);
    }

    public void setOnMovementFinishListener(OnMovementFinishListener listener){
        moveListener = listener;
    }

    public void setMoveRange(int xMax,int xMin ,int yMax,int yMin){
        this.xMax = xMax;
        this.xMin = xMin;
        this.yMax = yMax;
        this.yMin = yMin;
    }

    private void setCustomAttributes(Context context, AttributeSet attrs) {
        TypedArray array = context.obtainStyledAttributes(attrs, R.styleable.MoveTextView);
        xMax = array.getInt(R.styleable.MoveTextView_xMax,0);
        yMax = array.getInt(R.styleable.MoveTextView_yMax,0);
        xMin = array.getInt(R.styleable.MoveTextView_xMin,0);
        yMin = array.getInt(R.styleable.MoveTextView_yMin,0);
    }


    @Override
    public boolean onTouchEvent(MotionEvent event) {
        switch (event.getAction()) {
            case MotionEvent.ACTION_DOWN:
                moveX = event.getX();
                moveY = event.getY();
                break;
            case MotionEvent.ACTION_MOVE:
                tranX = (getX() + event.getX() - moveX) > xMin ? (getX() + event.getX() - moveX) : xMin;
                tranX = tranX > xMax - this.getWidth() ? xMax - this.getWidth() : tranX;

                tranY = getY() + event.getY() - moveY  > yMin ? (getY() + event.getY() - moveY) : yMin;
                tranY = tranY > yMax - this.getHeight() ? yMax - this.getHeight() : tranY;

//			if(tranX >= (xMax - this.getWidth()))


                setTranslationX(tranX);
                setTranslationY(tranY);
//			Log.d("MoveTextView", "ACTION_MOVE  ex:" + event.getX() + " ey:" + event.getY()+" gx:"+getX()+" gy:"+getY());
                break;
            case MotionEvent.ACTION_UP:
//			Log.d("MoveTextView", "ACTION_MOVE  tx:" + tranX + " ty:" + tranY);
                if(moveListener != null)
                    moveListener.onMovement(this, tranX, tranY);
                break;
        }

        return true;
    }
}
