package com.jackie.ts8209a.Managers;

import android.content.Context;
import android.graphics.Typeface;
import android.util.Log;

import com.jackie.ts8209a.CustomView.Dialog.SelectorDialog;

/**
 * Created by kuangyt on 2018/10/23.
 */

public class FontManager {
    public static final int FANGSONG = 0;
    public static final int HEITI = 1;
    public static final int HUPO = 2;
    public static final int KAISHU = 3;
    public static final int LISHU = 4;
    public static final int SHUTI = 5;
    public static final int SONGTI = 6;
    public static final int WEIBEI = 7;
    public static final int XINGCAO = 8;
    public static final int XINGKAI = 9;
    public static final int YAHEI = 10;
    public static final int YANTI = 11;
    public static final int ZHUNYUAN = 12;

    //字体中文名集合
    public static final String[] NAME = { "仿宋", "黑体", "琥珀", "楷书", "隶书", "舒体",
            "宋体", "魏碑", "行草", "行楷", "微软雅黑", "颜体" , "标准幼圆" };

    //字体ID集合
    private static final int[] FontType = { FANGSONG, HEITI, HUPO, KAISHU,
            LISHU, SHUTI, SONGTI, WEIBEI, XINGCAO, XINGKAI, YAHEI, YANTI,
            ZHUNYUAN };
    //字体类集合
    private Typeface[] typefaceCN = new Typeface[FontType.length];
    //字体文件绝对路径
    private static final String PATH = "/system/fonts/";
    //字体文件名集合
    private static final String FILE[] = { "fangsong.ttf", "heiti.ttf",
            "hupo.ttf", "kaishu.ttf", "lishu.ttf", "shuti.ttf", "songti.ttf",
            "weibei.ttf", "xingcao.ttf", "xingkai.ttf", "yahei.ttf",
            "yanti.ttf", "zhunyuan.ttf" };

    private static FontManager Font = new FontManager();


    private FontManager(){

    }

    public static FontManager getFontManager(){
        return Font;
    }

    public void loadFontType(){
        for(int i=0;i<FontType.length;i++){
            try{
                typefaceCN[i] = Typeface.createFromFile(PATH+FILE[i]);
            }catch(Exception e){
                typefaceCN[i] = null;
//                Log.e("FontManager","Font "+FILE[i]+"load fail : "+e);
            }

        }
    }

    public Typeface getFontType(int type){
        if(typefaceCN[type] != null){
            return typefaceCN[type];
        }
        else
            return null;
    }

    public Picker creatPicker(){
        return (new Picker());
    }

    public class Picker{
        private SelectorDialog pickDialog  = null;
        private pickerConfirmListener listener = null;
        private boolean[] checked;

        public void show(Context context){
            pickDialog = new SelectorDialog(context,NAME);
            pickDialog.setTitle("字体设置").setMode(SelectorDialog.SINGLE_SELECTION).setChecked(checked).show();
            pickDialog.setOnSelectConfirmListener(new SelectorDialog.OnSelectConfirmListener() {

                @Override
                public void singleSelect(int choice, String content) {
                    if(listener != null)
                        listener.fontPick(choice,content);
                }

                @Override
                public void multiSelect(boolean[] checked, String[] content) {
                }
            });
        }

        public Picker setConfirmListener(pickerConfirmListener listener){
            this.listener = listener;
            return this;
        }

        public Picker checkedItem(int type){
            checked = new boolean[NAME.length];
            for(int i=0;i<checked.length;i++){
                checked[i] = false;
            }
            checked[type] = true;
            return this;
        }


    }

    public interface pickerConfirmListener{
        void fontPick(int choice,String content);
    }


}
