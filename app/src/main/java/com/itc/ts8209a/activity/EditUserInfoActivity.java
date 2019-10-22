package com.itc.ts8209a.activity;

import android.graphics.Color;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.GradientDrawable;
import android.graphics.drawable.StateListDrawable;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.support.annotation.IdRes;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.Gravity;
import android.view.MotionEvent;
import android.view.View;
import android.view.animation.AlphaAnimation;
import android.view.animation.TranslateAnimation;
import android.widget.AbsoluteLayout;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.PopupWindow;
import android.widget.RadioGroup;
import android.widget.RelativeLayout;
import android.widget.TextView;

import com.bumptech.glide.Glide;
import com.itc.ts8209a.activity.view.ColorPickerDialog;
import com.itc.ts8209a.activity.view.CustomDialog;
import com.itc.ts8209a.activity.view.ImagePickerDialog;
import com.itc.ts8209a.activity.view.SeekbarDialog;
import com.itc.ts8209a.module.font.FontManager;
import com.itc.ts8209a.module.nameplate.NameplateManager;
import com.itc.ts8209a.R;
import com.itc.ts8209a.widget.Debug;

import java.util.Timer;
import java.util.TimerTask;

import static com.itc.ts8209a.app.AppConfig.*;
import static com.itc.ts8209a.module.nameplate.NameplateManager.*;


/**
 * Created by kuangyt on 2018/8/21.
 */

public class EditUserInfoActivity extends AppActivity implements View.OnTouchListener, RadioGroup.OnCheckedChangeListener, View.OnLongClickListener {
    private final int EDIT_USER = 0;
    private final int EDIT_COMP = 1;
    private final int EDIT_POS = 2;
    private final int EDIT_BG_COLOR = 3;
    private final int EDIT_BG_IMG = 4;
    private final int EDIT_NP_IMG = 5;

    private boolean btnLongClk = false;

    private FontManager Font;

    private RelativeLayout layCustomNpPara;
    private RelativeLayout layRdyMadeNpPara;
    private AbsoluteLayout layCustomNpPreview;

    private TextView tvRdyMadeNpName;
    private TextView tvPreviewHint;
    private TextView tvLongClickHint;
    private EditText etUserName;
    private EditText etCompName;
    private EditText etUserPos;

    private RadioGroup rgSelectTick;

    private Button btnPreview;
    private Button btnFontStyle;
    private Button btnFontSize;
    private Button btnFontColor;
    private Button btnSave;
    private Button btnSelectRdyMadeNp;

    private ImageButton ibtnBgSetting;

    private ImageView ivCustomNpBg;
    private ImageView ivRdyMadeNpImg;


    private CustomDialog customdialog = null;

    //铭牌背景属性弹窗
    private PopupWindow bgStylePop = null;
    private Timer hintSwitch;

    // 颜色参数缓存
    private String[] strTemp = new String[3];
    private int[] fontColorTemp = new int[3];
    private int[] fontsizeTemp = new int[3];
    private int[] fontstyleTemp = new int[3];
    private float[] fontPosX;
    private float[] fontPosY;

    private int npTypeTemp;
    private int bgColorTemp;
    private String bgImgPath;
    private String npImgPath;

    private int tickType = EDIT_USER;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_edit_userinfo);

        //获取控件
        etUserName = (EditText) findViewById(R.id.edit_user_name_et);
        etCompName = (EditText) findViewById(R.id.edit_comp_name_et);
        etUserPos = (EditText) findViewById(R.id.edit_user_pos_et);
        rgSelectTick = (RadioGroup) findViewById(R.id.edit_select_tick_rg);
        btnPreview = (Button) findViewById(R.id.edit_preview_set_btn);
        btnFontStyle = (Button) findViewById(R.id.edit_font_style_btn);
        btnFontSize = (Button) findViewById(R.id.edit_font_size_btn);
        btnFontColor = (Button) findViewById(R.id.edit_font_color_btn);
        ibtnBgSetting = (ImageButton) findViewById(R.id.edit_select_setting_bg_ibtn);
        btnSave = (Button) findViewById(R.id.edit_save);
        layCustomNpPara = (RelativeLayout) findViewById(R.id.edti_custom_np_para_lay);
        layRdyMadeNpPara = (RelativeLayout) findViewById(R.id.edti_ready_made_np_para_lay);
        layCustomNpPreview = (AbsoluteLayout) findViewById(R.id.edit_custom_np_preview_lay);
        ivCustomNpBg = (ImageView) findViewById(R.id.edit_custom_np_bg_iv);
        ivRdyMadeNpImg = (ImageView) findViewById(R.id.edit_rdy_made_np_preview_iv);
        btnSelectRdyMadeNp = (Button) findViewById(R.id.edit_select_rdy_made_np_btn);
        tvRdyMadeNpName = (TextView)findViewById(R.id.edit_rdy_made_np_name_tv);
        tvPreviewHint = (TextView)findViewById(R.id.edit_preview_hint_tv);
        tvLongClickHint = (TextView)findViewById(R.id.edit_long_click_hint_tv);

        Font = FontManager.getFontManager();

        hintSwitch = new Timer();
        hintSwitch.schedule(new TimerTask() {
            @Override
            public void run() {
                hintSwitchHandler.sendEmptyMessage(0);
            }
        },3000,5000);
    }

    @Override
    protected void onResume() {
        super.onResume();

        //绑定监听器
        btnFontColor.setOnClickListener(this);
        btnFontStyle.setOnClickListener(this);
        btnFontSize.setOnClickListener(this);
        ibtnBgSetting.setOnClickListener(this);
        btnSave.setOnClickListener(this);
        btnSelectRdyMadeNp.setOnClickListener(this);
        btnPreview.setOnClickListener(this);

        btnPreview.setOnLongClickListener(this);

        etUserName.setOnTouchListener(this);
        etCompName.setOnTouchListener(this);
        etUserPos.setOnTouchListener(this);

        rgSelectTick.setOnCheckedChangeListener(this);

        etUserName.addTextChangedListener(new editTextChangeListener(etUserName));
        etCompName.addTextChangedListener(new editTextChangeListener(etCompName));
        etUserPos.addTextChangedListener(new editTextChangeListener(etUserPos));
    }

    @Override
    protected void uiRefresh() {
        super.uiRefresh();

        strTemp = databaseManager.getStr();
        fontColorTemp = databaseManager.getColor();
        fontsizeTemp = databaseManager.getSize();
        fontstyleTemp = databaseManager.getStyle();
        fontPosX = databaseManager.getPosX();
        fontPosY = databaseManager.getPosY();
        bgColorTemp = databaseManager.getNamePlateBGColor();
        npTypeTemp = databaseManager.getNamePlateType();
        bgImgPath = databaseManager.getNamePlateBGImg();
        npImgPath = databaseManager.getNamePlateImage();

        //初始化控件状态
        rgSelectTick.check(R.id.edit_name_tick_rbtn);
        etUserName.setText(strTemp[EDIT_USER]);
        etCompName.setText(strTemp[EDIT_COMP]);
        etUserPos.setText(strTemp[EDIT_POS]);
        btnFontStyle.setText(FontManager.NAME[fontstyleTemp[EDIT_USER]]);
        btnFontSize.setText(String.format("%02d", fontsizeTemp[EDIT_USER]));
        setBtnColor(btnFontColor, fontColorTemp[EDIT_USER]);
        btnSave.setEnabled(false);

        editNameplatePreview();
        switchNameplatePage(npTypeTemp,false);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        new Thread(){
            @Override
            public void run() {
                Glide.get(EditUserInfoActivity.this).clearDiskCache();
            }
        }.start();
        hintSwitch.cancel();
    }

    @Override
    public void onClick(View view) {
        super.onClick(view);

        Button btn;
        switch (view.getId()) {
            case R.id.edit_font_color_btn:
                btn = (Button) view;
                colorPicker(btn);
                break;
            case R.id.edit_select_setting_bg_ibtn:
                bgStylePopShow();
                break;
            case R.id.edit_font_size_btn:
//                Debug.d(TAG,"Font size btn");
                btn = (Button) view;
                fontSizePicker(btn);
                break;
            case R.id.edit_font_style_btn:
                btn = (Button) view;
                fontStylePicker(btn);
                break;
            case R.id.edit_save:
                infoSave();
                break;
            case R.id.edit_preview_set_btn:
                if (btnLongClk) {
                    btnLongClk = false;
                    break;
                }
                editNameplatePreview();
                nameplateManager.setOnPreviewConfirmListener(new NameplateManager.onPreviewConfirmListener() {

                    @Override
                    public void onPreviewConfirm(View view) {
                        infoSave();
                    }
                });
                nameplateManager.setOnPositionChangeListener(new NameplateManager.onPositionChangeListener() {

                    @Override
                    public void onPositionChange(int type, NameplateManager.NameplatePara para) {
                        fontPosX[type] = para.fontPosX[type];
                        fontPosY[type] = para.fontPosY[type];
                        editNameplatePreview(type);
                        btnSave.setEnabled(true);
                    }
                });
                nameplateManager.preview(this);
                break;
            case R.id.edit_pop_bg_style_exit_btn:
                bgStylePopClose();
                break;
            case R.id.edit_pop_bg_style_color_btn:
                bgStylePopClose();
                colorPicker(ibtnBgSetting);
                break;
            case R.id.edit_pop_bg_style_img_btn:
                bgStylePopClose();
                npBackgroundImgPicker();
                break;
            case R.id.edit_select_rdy_made_np_btn:
                npRdyMadeImgPicker();
                break;
        }
    }

    @Override
    public boolean onLongClick(View view) {
        btnLongClk = true;
//        npTypeTemp = (npTypeTemp == TYPE_CUSTOM_BG_IMG || npTypeTemp == TYPE_CUSTOM_COLOR) ? TYPE_RDY_MADE_PIC : TYPE_CUSTOM_COLOR;
        if(npTypeTemp == TYPE_CUSTOM_BG_IMG || npTypeTemp == TYPE_CUSTOM_COLOR){
            if (npImgPath.equals("")) {
                ImagePickerDialog picker = new ImagePickerDialog(this);
                picker.creatPicker("请选择预设铭牌图片", NAMEPLATE_IMG_PATH + "id_" + databaseManager.getDeviceID() + "/", new ImagePickerDialog.OnImageConfirmListener() {
                    @Override
                    public void imgConfirm(String imgPath) {
                        if (imgPath == null) {
                            PromptBox.BuildPrompt("NP_PICTURE_PATH_EMPTY").Text("设备无已下载的铭牌图片").Time(1).TimeOut(3000);
                        } else {
                            setBtnImage(ibtnBgSetting, R.drawable.ico_wenj_h);
                            npImgPath = imgPath;
                            npTypeTemp = TYPE_RDY_MADE_PIC;
                            switchNameplatePage(npTypeTemp,true);
                            editNameplatePreview();
                            btnSave.setEnabled(true);
                        }
                    }
                });
            }
            else {
                npTypeTemp = TYPE_RDY_MADE_PIC;
                switchNameplatePage(npTypeTemp, true);
            }
        }else{
            npTypeTemp = TYPE_CUSTOM_COLOR;
            switchNameplatePage(npTypeTemp,true);
        }
        editNameplatePreview();
        btnSave.setEnabled(true);
        return false;
    }

    @Override
    public boolean onTouch(View view, MotionEvent event) {
        if (event.getAction() == MotionEvent.ACTION_DOWN) {
            switch (view.getId()) {
                case R.id.edit_user_name_et:
                    tickType = EDIT_USER;
                    rgSelectTick.check(R.id.edit_name_tick_rbtn);
                    break;
                case R.id.edit_comp_name_et:
                    tickType = EDIT_COMP;
                    rgSelectTick.check(R.id.edit_comp_tick_rbtn);
                    break;
                case R.id.edit_user_pos_et:
                    tickType = EDIT_POS;
                    rgSelectTick.check(R.id.edit_pos_tick_rbtn);
                    break;
            }
        }
        return false;
    }

    @Override
    public void onCheckedChanged(RadioGroup group, @IdRes int checkedId) {
        switch (checkedId) {
            case R.id.edit_name_tick_rbtn:
                tickType = EDIT_USER;
                etUserName.requestFocus();
                break;
            case R.id.edit_comp_tick_rbtn:
                tickType = EDIT_COMP;
                etCompName.requestFocus();
                break;
            case R.id.edit_pos_tick_rbtn:
                tickType = EDIT_POS;
                etUserPos.requestFocus();
                break;
        }
        btnFontStyle.setText(FontManager.NAME[fontstyleTemp[tickType]]);
        btnFontSize.setText(String.format("%02d", fontsizeTemp[tickType]));
        setBtnColor(btnFontColor, fontColorTemp[tickType]);
    }

    @Override
    protected void retBtnClick() {
        if(btnSave.isEnabled()) {
            CustomDialog.Builder builder = new CustomDialog.Builder(this, CustomDialog.TRIPLE_BTN);
            builder.setTitle(false,null)
                    .setContent("设置未保存，是否退出？")
                    .setFisrtBtn("退出")
                    .setSecondBtn("保存并退出")
                    .setThirdBtn("取消")
                    .setButtonClickListerner(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    if (customdialog != null) {
                        customdialog.dismiss();
                        customdialog = null;
                    }
                    switch (v.getId()) {
                        case R.id.custom_dialog_first_btn:
                            finish();
                            break;
                        case R.id.custom_dialog_second_btn:
                            infoSave();
                            finish();
                            break;
                        case R.id.custom_dialog_third_btn:
                            break;
                    }
                }
            });
            customdialog = builder.creatDialog();
            customdialog.show();
        }else
            finish();
    }

    //设置按钮颜色
    private void setBtnColor(Button btn, int color) {
        setViewColor(btn, color, 255);
    }

    //设置按钮颜色
    private void setBtnColor(ImageButton ibtn, int color) {
        ibtn.setImageDrawable(null);
        setViewColor(ibtn, color, 255);
    }

    private void setBtnImage(ImageButton ibtn, int resId) {
        setViewColor(ibtn, getResources().getColor(R.color.transparent_color), 0);
        ibtn.setImageDrawable(getResources().getDrawable(resId));
    }

    private void setViewColor(View view, int color, int alpha) {
        GradientDrawable normalDraw = new GradientDrawable();
        GradientDrawable pressDraw = new GradientDrawable();
        StateListDrawable btnDraw = new StateListDrawable();

        normalDraw.setColor(color);
        normalDraw.setAlpha(alpha);
        normalDraw.setShape(GradientDrawable.RECTANGLE);
        normalDraw.setCornerRadius(2);
        normalDraw.setStroke(1, Color.GRAY);

        pressDraw.setColor(color);
        pressDraw.setAlpha(alpha);
        pressDraw.setShape(GradientDrawable.RECTANGLE);
        pressDraw.setCornerRadius(2);
        pressDraw.setStroke(2, Color.GRAY);

        btnDraw.addState(new int[]{android.R.attr.state_pressed}, pressDraw);
        btnDraw.addState(new int[]{}, normalDraw);
//        btnDraw.setAlpha(alpha);

        view.setBackground(btnDraw);
    }

    //打开会话框设置铭牌背景类型（图片or纯色）
    private void bgStylePopShow() {
        View layout = View.inflate(this, R.layout.pop_bg_style_select, null);
        bgStylePop = new PopupWindow(layout, 366, 210);

        layout.findViewById(R.id.edit_pop_bg_style_exit_btn).setOnClickListener(this);
        layout.findViewById(R.id.edit_pop_bg_style_color_btn).setOnClickListener(this);
        layout.findViewById(R.id.edit_pop_bg_style_img_btn).setOnClickListener(this);

        bgStylePop.setBackgroundDrawable(new BitmapDrawable());
        bgStylePop.setFocusable(true);
        bgStylePop.setOutsideTouchable(true);
        bgStylePop.update();

        bgStylePop.setAnimationStyle(R.style.anim_popupwindow);
//        pop.showAsDropDown(ibtnBgSetting);
        bgStylePop.showAtLocation(ibtnBgSetting, Gravity.NO_GRAVITY, 608, 316);
    }

    //关闭铭牌类型设置会话框
    private void bgStylePopClose() {
        if (bgStylePop != null && bgStylePop.isShowing())
            bgStylePop.dismiss();
        bgStylePop = null;
    }

    //保存用户信息参数
    private void infoSave() {
        databaseManager.setStr(strTemp)
                .setStyle(fontstyleTemp)
                .setSize(fontsizeTemp)
                .setColor(fontColorTemp)
                .setNamePlateBGColor(bgColorTemp)
                .setstrPos(fontPosX, fontPosY)
                .setNamePlateType(npTypeTemp)
                .setNamePlateBGImg(bgImgPath)
                .setNamePlateImgPath(npImgPath)
                .save();

        btnSave.setEnabled(false);
        PromptBox.BuildPrompt("SAVE_SUCCESS").Text(getString(R.string.save_successfully)).Time(1).TimeOut(3000);
    }

	//更新预览区域铭牌内容
    private void editNameplatePreview(int editItem) {
        int[] idTemp = {R.id.edit_preview_user_tv, R.id.edit_preview_comp_tv, R.id.edit_preview_pos_tv};

        switch (editItem) {
            case EDIT_USER:
            case EDIT_COMP:
            case EDIT_POS:
                TextView tvPreviwUser = (TextView) findViewById(idTemp[editItem]);
                tvPreviwUser.setText(strTemp[editItem]);
                tvPreviwUser.setTextColor(fontColorTemp[editItem]);
                tvPreviwUser.setTextSize((int) (fontsizeTemp[editItem] * 0.625));
                tvPreviwUser.setTypeface(Font.getFontType(fontstyleTemp[editItem]));
                tvPreviwUser.setX(fontPosX[editItem] / 3);
                tvPreviwUser.setY(fontPosY[editItem] / 3);
                nameplateManager.para.setPara(editItem, strTemp[editItem], fontColorTemp[editItem], fontsizeTemp[editItem], fontstyleTemp[editItem], fontPosX[editItem], fontPosY[editItem]);
                break;
            case EDIT_BG_COLOR:
                ivRdyMadeNpImg.setVisibility(View.INVISIBLE);
                layCustomNpPreview.setVisibility(View.VISIBLE);
                ivCustomNpBg.setVisibility(View.INVISIBLE);
                layCustomNpPreview.setBackgroundColor(bgColorTemp);
                setBtnColor(ibtnBgSetting,bgColorTemp);
                nameplateManager.para.setBgColor(bgColorTemp);
                break;
            case EDIT_BG_IMG:
                ivRdyMadeNpImg.setVisibility(View.INVISIBLE);
                layCustomNpPreview.setVisibility(View.VISIBLE);
                ivCustomNpBg.setVisibility(View.VISIBLE);
                layCustomNpPreview.setBackgroundColor(getResources().getColor(R.color.transparent_color));
                setBtnImage(ibtnBgSetting,R.drawable.ico_wenj_h);
                nameplateManager.para.setBgImg(bgImgPath);

                Glide.with(getApplicationContext()).load(bgImgPath).into(ivCustomNpBg);
                break;
            case EDIT_NP_IMG:
                layCustomNpPreview.setVisibility(View.INVISIBLE);
                ivRdyMadeNpImg.setVisibility(View.VISIBLE);
                nameplateManager.para.setNpImg(npImgPath);
                tvRdyMadeNpName.setText(getString(R.string.file_name)+"\n"+npImgPath.substring(npImgPath.lastIndexOf("/")+1,npImgPath.length()));

                Glide.with(getApplicationContext()).load(npImgPath).into(ivRdyMadeNpImg);
                break;
        }
    }

    //加载预览区域铭牌全部内容
    private void editNameplatePreview() {
        switch (npTypeTemp) {
            case TYPE_CUSTOM_COLOR:
                for (int i = 0; i < 4; i++) {
                    editNameplatePreview(i);
                }
                break;
            case TYPE_CUSTOM_BG_IMG:
                for (int i = 0; i < 3; i++) {
                    editNameplatePreview(i);
                }
                editNameplatePreview(EDIT_BG_IMG);
                break;
            case TYPE_RDY_MADE_PIC:
                editNameplatePreview(EDIT_NP_IMG);
                break;
            default:
                npTypeTemp = TYPE_CUSTOM_COLOR;
                editNameplatePreview();
                break;
        }
        nameplateManager.para.setNpType(npTypeTemp);
    }

    //切换铭牌设置页面
    private void switchNameplatePage(int type, boolean isAnim){
        TranslateAnimation showAnim = null;
        TranslateAnimation hideAnim = null;
        if(isAnim) {
            showAnim = new TranslateAnimation(0, -370, 0, 0);
            hideAnim = new TranslateAnimation(370, 0, 0, 0);
            showAnim.setDuration(500);
            hideAnim.setDuration(500);
        }

        if(type == TYPE_CUSTOM_COLOR || type == TYPE_CUSTOM_BG_IMG){
            layRdyMadeNpPara.setVisibility(View.GONE);
            layCustomNpPara.setVisibility(View.VISIBLE);
            if(isAnim) {
                layRdyMadeNpPara.setAnimation(showAnim);
                layCustomNpPara.setAnimation(hideAnim);
            }
        }else if(type == TYPE_RDY_MADE_PIC){
            layRdyMadeNpPara.setVisibility(View.VISIBLE);
            layCustomNpPara.setVisibility(View.GONE);
            if(isAnim) {
                layRdyMadeNpPara.setAnimation(hideAnim);
                layCustomNpPara.setAnimation(showAnim);
            }
        }
    }

	//启动颜色选择器
    private void colorPicker(final View view) {
        ColorPickerDialog dialog = new ColorPickerDialog(
                EditUserInfoActivity.this,
                (view.getId() == R.id.edit_select_setting_bg_ibtn ? bgColorTemp : fontColorTemp[tickType]), null,
                new ColorPickerDialog.OnColorChangedListener() {

                    @Override
                    public void colorChanged(int color) {
//						Log.i("ColorPicker","Color: "+color);
//                        setViewColor(view, color,255);
                        if (view.getId() == R.id.edit_font_color_btn) {
                            fontColorTemp[tickType] = color;
                            editNameplatePreview(tickType);
                            setBtnColor((Button) view, color);
                        } else if (view.getId() == R.id.edit_select_setting_bg_ibtn) {
                            bgColorTemp = color;
                            editNameplatePreview(EDIT_BG_COLOR);
//                            setBtnColor((ImageButton) view, color);
                            npTypeTemp = TYPE_CUSTOM_COLOR;
                        }
                        btnSave.setEnabled(true);
                    }
                });
        dialog.show();
    }

    //启动背景图选择器
    private void npBackgroundImgPicker() {
        ImagePickerDialog picker = new ImagePickerDialog(this);
        picker.creatPicker("请选择背景图", NAMEPLATE_BACKGROUND_PATH+"id_"+ databaseManager.getDeviceID()+"/", new ImagePickerDialog.OnImageConfirmListener() {
            @Override
            public void imgConfirm(String imgPath) {
                if (imgPath == null) {
                    PromptBox.BuildPrompt("NP_PICTURE_PATH_EMPTY").Text("设备无已下载的铭牌背景").Time(1).TimeOut(3000);
                } else {
                    setBtnImage(ibtnBgSetting, R.drawable.ico_wenj_h);
                    bgImgPath = imgPath;
                    npTypeTemp = TYPE_CUSTOM_BG_IMG;
                    editNameplatePreview();
                    btnSave.setEnabled(true);
                }
            }
        });
    }

    // 启动铭牌图选择器
    private void npRdyMadeImgPicker() {
        ImagePickerDialog picker = new ImagePickerDialog(this);
        picker.creatPicker("请选择预设铭牌图片", NAMEPLATE_IMG_PATH+"id_"+ databaseManager.getDeviceID()+"/", new ImagePickerDialog.OnImageConfirmListener() {
            @Override
            public void imgConfirm(String imgPath) {
                if (imgPath == null) {
                    PromptBox.BuildPrompt("NP_PICTURE_PATH_EMPTY").Text("设备无已下载的铭牌图片").Time(1).TimeOut(3000);
                } else {
                    setBtnImage(ibtnBgSetting, R.drawable.ico_wenj_h);
                    npImgPath = imgPath;
                    npTypeTemp = TYPE_RDY_MADE_PIC;
                    editNameplatePreview();
                    btnSave.setEnabled(true);
                }
            }
        });
    }

	// 字体大小选择器
    private void fontSizePicker(final Button btn) {

        final SeekbarDialog dialog;
        int size = fontsizeTemp[tickType];

        dialog = new SeekbarDialog(EditUserInfoActivity.this, getResources().getString(R.string.set_font_size));
        dialog.setProgress(size, 12, 65);
        dialog.setonSeekbarConfirmListener(new SeekbarDialog.onSeekbarConfirmListener() {

            @Override
            public void onConfirm(int progress) {
                btn.setText(progress + "");
                fontsizeTemp[tickType] = progress;
                editNameplatePreview(tickType);
                btnSave.setEnabled(true);
                dialog.dismiss();
            }
        });
        dialog.show();
    }

	//字体风格选择器
    private void fontStylePicker(final Button btn) {
        FontManager.Picker fontPicker;
        int type = fontstyleTemp[tickType];

        fontPicker = Font.creatPicker();
        fontPicker.checkedItem(type).setConfirmListener(new FontManager.pickerConfirmListener() {

            @Override
            public void fontPick(int select, String content) {
                if (content != null) {
                    btn.setText(content);
                    fontstyleTemp[tickType] = select;
                    editNameplatePreview(tickType);
                    btnSave.setEnabled(true);
                }
            }
        }).show(EditUserInfoActivity.this);
    }

    private Handler hintSwitchHandler = new Handler(){

        @Override
        public void handleMessage(Message msg) {
            AlphaAnimation showAnim = new AlphaAnimation(0,1);
            AlphaAnimation hideAnim = new AlphaAnimation(1,0);
            showAnim.setDuration(800);
            hideAnim.setDuration(500);
            if(tvPreviewHint.getVisibility() == View.VISIBLE){
                tvPreviewHint.setVisibility(View.GONE);
                tvLongClickHint.setVisibility(View.VISIBLE);
                tvPreviewHint.setAnimation(hideAnim);
                tvLongClickHint.setAnimation(showAnim);
            }else{
                tvPreviewHint.setVisibility(View.VISIBLE);
                tvLongClickHint.setVisibility(View.GONE);
                tvPreviewHint.setAnimation(showAnim);
                tvLongClickHint.setAnimation(hideAnim);
            }
        }
    };
    //EditText监听器
    private class editTextChangeListener implements TextWatcher {

        private EditText edittext;

        public editTextChangeListener(EditText edittext) {
            this.edittext = edittext;
        }

        @Override
        public void afterTextChanged(Editable s) {
            switch (edittext.getId()) {
                case R.id.edit_user_name_et:
                    strTemp[EDIT_USER] = s.toString();
                    break;
                case R.id.edit_comp_name_et:
                    strTemp[EDIT_COMP] = s.toString();
                    break;
                case R.id.edit_user_pos_et:
                    strTemp[EDIT_POS] = s.toString();
                    break;
                default:
                    break;
            }
            editNameplatePreview(tickType);
            btnSave.setEnabled(true);
        }

        @Override
        public void beforeTextChanged(CharSequence s, int start, int count, int after) {
        }

        @Override
        public void onTextChanged(CharSequence s, int start, int before, int count) {
        }

    }
}
