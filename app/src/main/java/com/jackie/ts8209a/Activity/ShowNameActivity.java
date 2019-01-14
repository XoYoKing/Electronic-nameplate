package com.jackie.ts8209a.Activity;

import android.os.Bundle;
import android.os.Handler;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;

import com.bumptech.glide.Glide;
import com.jackie.ts8209a.AppModule.Basics.FontManager;
import com.jackie.ts8209a.AppModule.Basics.NameplateManager;
import com.jackie.ts8209a.R;

import java.util.jar.Attributes;

import static com.jackie.ts8209a.AppModule.Basics.NameplateManager.*;

/**
 * Created by kuangyt on 2018/8/21.
 */

public class ShowNameActivity extends AppActivity implements View.OnClickListener {

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
        layBackground = findViewById(R.id.showname_background_lay);
        ivBackground = (ImageView) findViewById(R.id.showname_background_iv);
        tvNameplate = new TextView[3];
        tvNameplate[0] = (TextView) findViewById(R.id.showname_person_tv);
        tvNameplate[1] = (TextView) findViewById(R.id.showname_company_tv);
        tvNameplate[2] = (TextView) findViewById(R.id.showname_position_tv);
        btnExit = (Button)findViewById(R.id.showname_exit_btn);
    }

    @Override
    protected void onResume() {
        super.onResume();
        stateBar.hide();
        btnReturn.setVisibility(View.GONE);
        btnExit.setOnClickListener(this);
        setNameplate();
    }

    private void setNameplate() {

        switch (nameplateManager.para.npType){
            case NP_TYPE_CUSTOM_COLOR:
            case NP_TYPE_CUSTOM_BG_IMG:
                if(nameplateManager.para.npType == NP_TYPE_CUSTOM_COLOR) {
                    ivBackground.setVisibility(View.GONE);
                    layBackground.setBackgroundColor(nameplateManager.para.bgColor);
                }
                else if(nameplateManager.para.npType == NP_TYPE_CUSTOM_BG_IMG) {
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
            case NP_TYPE_RDY_MADE_IMG:
                ivBackground.setVisibility(View.VISIBLE);
                Glide.with(this).load(nameplateManager.para.npImgPath).into(ivBackground);
                for (int i = 0; i < 3; i++) {
                    tvNameplate[i].setVisibility(View.GONE);
                }
                break;
        }
    }

    @Override
    public void onClick(View v) {
        finish();
    }
}
