package com.itc.ts8209a.activity;

import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;

import com.bumptech.glide.Glide;
import com.itc.ts8209a.module.font.FontManager;
import com.itc.ts8209a.module.nameplate.NameplateManager;
import com.itc.ts8209a.R;
import com.itc.ts8209a.module.database.DatabaseManager;
import com.itc.ts8209a.widget.Cmd;

import static com.itc.ts8209a.module.nameplate.NameplateManager.*;

/**
 * Created by kuangyt on 2018/8/21.
 */

public class ShowNameActivity extends AppActivity  {

    private ImageView ivBackground;
    private View layBackground;
    private TextView[] tvNameplate;
    private Button btnExit;
    private NameplateManager nameplateManager;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_showname);

        nameplateManager = NameplateManager.getNameplateManager();
        databaseManager = DatabaseManager.getDatabaseManager();

        layBackground = findViewById(R.id.showname_background_lay);
        ivBackground = (ImageView) findViewById(R.id.showname_background_iv);
        tvNameplate = new TextView[3];
        tvNameplate[0] = (TextView) findViewById(R.id.showname_person_tv);
        tvNameplate[1] = (TextView) findViewById(R.id.showname_company_tv);
        tvNameplate[2] = (TextView) findViewById(R.id.showname_position_tv);
        btnExit = (Button)findViewById(R.id.showname_exit_btn);

        Cmd.execCmd("settings put system screen_off_timeout " +Integer.MAX_VALUE);
    }

    @Override
    protected void onResume() {
        super.onResume();
        stateBar.hide();
        btnReturn.setVisibility(View.GONE);
        btnExit.setOnClickListener(this);
    }

    @Override
    protected void uiRefresh() {
        super.uiRefresh();
        setNameplate();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        int dimTime = databaseManager.getScreenDimTime();
        Cmd.execCmd("settings put system screen_off_timeout " +(dimTime==Integer.MAX_VALUE ? dimTime : dimTime * 1000));
    }

    private void setNameplate() {

        for(int i=0;i<3;i++){
            nameplateManager.para.setPara(i,    databaseManager.getStr()[i],
                                                databaseManager.getColor()[i],
                                                databaseManager.getSize()[i],
                                                databaseManager.getStyle()[i],
                                                databaseManager.getPosX()[i],
                                                databaseManager.getPosY()[i]);
        }
        nameplateManager.para.setBgColor(databaseManager.getNamePlateBGColor());
        nameplateManager.para.setBgImg(databaseManager.getNamePlateBGImg());
        nameplateManager.para.setNpImg(databaseManager.getNamePlateImage());


        switch (nameplateManager.para.npType){
            case TYPE_CUSTOM_COLOR:
            case TYPE_CUSTOM_BG_IMG:
                if(nameplateManager.para.npType == TYPE_CUSTOM_COLOR) {
                    ivBackground.setVisibility(View.GONE);
                    layBackground.setBackgroundColor(nameplateManager.para.bgColor);
                }
                else if(nameplateManager.para.npType == TYPE_CUSTOM_BG_IMG) {
                    ivBackground.setVisibility(View.VISIBLE);
                    Glide.with(this).load(nameplateManager.para.bgImgPath).into(ivBackground);
                }

                for (int i = 0; i < 3; i++) {
                    tvNameplate[i].setText(nameplateManager.para.strContent[i]);
                    tvNameplate[i].setTextColor(nameplateManager.para.fontColor[i]);
                    tvNameplate[i].setTextSize((int) (nameplateManager.para.fontsize[i] * 2.84));
                    tvNameplate[i].setTypeface(FontManager.getFontManager().getFontType(nameplateManager.para.fontstyle[i]));
                    tvNameplate[i].setX((float) (nameplateManager.para.fontPosX[i]*1.333));
                    tvNameplate[i].setY((float) (nameplateManager.para.fontPosY[i]*1.333));
                }
                break;
            case TYPE_RDY_MADE_PIC:
                ivBackground.setVisibility(View.VISIBLE);
                Glide.with(this).load(nameplateManager.para.npImgPath).into(ivBackground);
                for (int i = 0; i < 3; i++) {
                    tvNameplate[i].setVisibility(View.GONE);
                }
                break;
        }
    }

    @Override
    public void onClick(View view) {
        super.onClick(view);

        if(view.getId() == R.id.showname_exit_btn)
            finish();
    }
}
