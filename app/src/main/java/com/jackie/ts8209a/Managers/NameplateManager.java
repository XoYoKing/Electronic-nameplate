package com.jackie.ts8209a.Managers;

import android.app.Dialog;
import android.content.Context;
import android.os.Environment;
import android.os.Handler;
import android.os.Message;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.TextView;

import com.jackie.ts8209a.Activity.AppActivity;
import com.jackie.ts8209a.Application.APP;
import com.jackie.ts8209a.CustomView.View.MoveTextView;
import com.jackie.ts8209a.Drive.RA8876L;
import com.jackie.ts8209a.R;

import java.io.BufferedOutputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;

/**
 * Created by kuangyt on 2018/10/23.
 */

public class NameplateManager {
    private static final String TAG = "NameplateManager";
    public static NameplatePara Para = new NameplatePara();

    private static NameplateManager Nameplate = new NameplateManager();
    private onPreviewConfirmListener previewConfirmListener = null;
    private onPositionChangeListener positionChangeListener = null;

    private static final String BYTE_FILE_PATH = "/data/Pictures";
    private static final String BYTE_FILE_NAME = "PicByteData.bin";

//	private RA8876L RA8876L = RA8876L.getRA8876L();

    private NameplateManager() {

    }

    public static NameplateManager getNameplateManager() {
        return Nameplate;
    }

    public void init(final Context context){

            new Thread() {
                public void run() {
                    try {
                        Thread.sleep(1000);

                        ProgressBar pBar = new ProgressBar(context);
                        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(30, 30);
                        pBar.setLayoutParams(params);

                        AppActivity.PromptBox.BuildPrompt("INIT_NAMEPLATE").Text("正在初始化电子铭牌").View(pBar);
//                      Thread.sleep(50);
//					    Log.i("RA8876 init", "Rst 8876");
//                      MCU.sendCommand(McuHandler.RST_8876);
                        RA8876L.devHandshake();
                        Thread.sleep(500);
                        RA8876L.setOninitFinishListener(new RA8876L.OnInitFinishListener() {

                            @Override
                            public void onInitFinish() {
                                Log.i(TAG, "Init Finish");

                                try{
//							mActivity.PromptBox.BuildPrompt("INIT_NAMEPLATE").Text((String)getResources().getText(R.string.nameplate_is_initializing)).Time(4).TimeOut(3000);
                                    RA8876L.setOnSetPicFinishListener(new RA8876L.OnSetPicFinishListener() {
                                        @Override
                                        public void onSetPicFinish() {
//                                            MCU.sendCommand(McuHandler.BACKLIGHT_88_ON);
                                            AppActivity.PromptBox.removePrompt("INIT_NAMEPLATE");
                                        }
                                    });
                                    setNamePlateByByteData();

//							Thread.sleep(15000);
                                }catch(Exception e){}
                            }
                        });
                        RA8876L.devInit();
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }

                };
            }.start();
    }

    public void preview(final Context context) {
        final nameplateDialog dialog = new nameplateDialog(context);
//		final MainApplication app = (MainApplication) ((Activity) context).getApplication();

        dialog.setPreview();
        dialog.setPreviewBtnListener(new View.OnClickListener() {

            @Override
            public void onClick(View view) {
                switch (view.getId()) {
                    case R.id.nameplate_preview_close_btn:
                        dialog.dismiss();
                        break;
                    case R.id.nameplate_preview_confirm_btn:
                        update(context,dialog);
                        break;
                }
            }
        });
        dialog.show();
    }

    public void update(final Context context){
        final nameplateDialog dialog = new nameplateDialog(context);
        dialog.setUpdating();
        dialog.show();
        update(context, dialog);

    }

    private void update(Context context,final nameplateDialog dialog){
//        final APP app = (APP) ((Activity) context).getApplication();
        dialog.setUpdating();
//        app.MCU.sendCommand(McuHandler.BACKLIGHT_88_OFF);
        dialog.getNameplate(new Handler() {
            public void handleMessage(Message msg) {
                final View view = (View) msg.obj;

                if (previewConfirmListener != null) {
                    previewConfirmListener.onPreviewConfirm(view);
                }

                RA8876L.setOnSetPicFinishListener(new RA8876L.OnSetPicFinishListener() {
                    @Override
                    public void onSetPicFinish() {
                        Log.i("NameplateManager", "onSetPicFinish");
//						try {
//							Thread.sleep(1000);
                        dialog.dismiss();
//                        app.MCU.sendCommand(McuHandler.BACKLIGHT_88_ON);
//						} catch (InterruptedException e) {
//							e.printStackTrace();
//						}
                    }
                });

                RA8876L.setOnSetPicTimeOutListener(new RA8876L.OnSetPicTimeOutListener() {

                    @Override
                    public void onSetPicTimeOut() {
                        Log.i("NameplateManager", "onSetPicTimeOut");
//						RA8876L = RA8876L.resetRA8876L();
//                        app.MCU.sendCommand(McuHandler.RST_8876);
//						try {
//							Thread.sleep(1000);
                        RA8876L.devInit();

//						} catch (InterruptedException e) {
//							e.printStackTrace();
//						}
                        dialog.dismiss();
                    }
                });
                RA8876L.setOnImgDataListenerListener(new RA8876L.OnImgDataListener() {

                    @Override
                    public void onImgData(byte[] data) {
                        createFileWithByte(data);
                    }
                });
                RA8876L.setPic(view);
            };
        });
    }

    public void setNamePlateByByteData() {
        byte[] imgData = readFile();
        if (imgData == null){
            imgData = new byte[1024*600*3];
            for(int i=0;i<imgData.length;i++)
                imgData[i] = (byte)0xFF;
        }
//        Log.i("setNamePlateByByteData","size = "+imgData.length);
        RA8876L.setPic(imgData);
    }

    public interface onPreviewConfirmListener {
        abstract void onPreviewConfirm(View view);
    }

    public void setOnPreviewConfirmListener(onPreviewConfirmListener listener) {
        previewConfirmListener = listener;
    }

    public interface onPositionChangeListener{
        abstract void onPositionChange(int type,NameplatePara para);
    }

    public void setOnPositionChangeListener(onPositionChangeListener listener){
        positionChangeListener = listener;
    }

    public static class NameplatePara {
        public int npType;
        public String[] strContent = new String[3];
        public int[] fontColor = new int[3];
        public int[] fontsize = new int[3];
        public int[] fontstyle = new int[3];
        public float[] fontPosX = new float[3];
        public float[] fontPosY = new float[3];
        public int bgColor;
        public String bgImgPath;
        public String npImgPath;


        public void setPara(int item, String str, int color, int size, int style, float posX, float posY) {
            if (item < UserInfoManager.USER || item > UserInfoManager.POS)
                return;

                this.strContent[item] = str;
            this.fontColor[item] = color;
            this.fontsize[item] = size;
            this.fontstyle[item] = style;
            this.fontPosX[item] = posX;
            this.fontPosY[item] = posY;
        }

        public void setBgColor(int color) {
            bgColor = color;
        }

        public void setBgImg(String path){
            bgImgPath = path;
        }

        public void setNpImg(String path){
            npImgPath = path;
        }

        public void setNpType(int type){
            npType = type;
        }
    }

    private class nameplateDialog extends Dialog {

        private View dialogLayout;
        private View layBackground;
        private MoveTextView[] tvPreview = new MoveTextView[3];

        public nameplateDialog(Context context) {
            super(context, R.style.custom_dialog);

            LayoutInflater inflater = (LayoutInflater) context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
            dialogLayout = inflater.inflate(R.layout.dialog_nameplate, null);

            tvPreview[0] = (MoveTextView) dialogLayout.findViewById(R.id.nameplate_preview_user_tv);
            tvPreview[1] = (MoveTextView) dialogLayout.findViewById(R.id.nameplate_preview_comp_tv);
            tvPreview[2] = (MoveTextView) dialogLayout.findViewById(R.id.nameplate_preview_pos_tv);

            layBackground = dialogLayout.findViewById(R.id.nameplate_preview_bg_lay);

            this.setContentView(dialogLayout);
            this.setCanceledOnTouchOutside(true); // 用户能通过点击对话框之外的地方取消对话框显示
        }

        public void setPreview() {
            this.setCanceledOnTouchOutside(true);

            (dialogLayout.findViewById(R.id.nameplate_updata_lay)).setVisibility(View.GONE);
            (dialogLayout.findViewById(R.id.nameplate_preview_lay)).setVisibility(View.VISIBLE);

            layBackground.setBackgroundColor(Para.bgColor);

            for (int i = 0; i < 3; i++) {
                tvPreview[i].setText(Para.strContent[i]);
                tvPreview[i].setTextColor(Para.fontColor[i]);
                tvPreview[i].setTextSize((int) (Para.fontsize[i] * 2.13));
                tvPreview[i].setTypeface(FontManager.getFontManager().getFontType(Para.fontstyle[i]));
                tvPreview[i].setOnMovementFinishListener(moveListener);
                tvPreview[i].setX(Para.fontPosX[i]);
                tvPreview[i].setY(Para.fontPosY[i]);
//                tvPreview[i].setMoveRange(1024, 0, 256, 0);
            }
        }

        public void getNameplate(final Handler handler) {

            (dialogLayout.findViewById(R.id.nameplate_layout)).setBackgroundColor(Para.bgColor);

            TextView[] tvPreview1 = new TextView[3];
            tvPreview1[0] = (TextView) dialogLayout.findViewById(R.id.nameplate_Person);
            tvPreview1[1] = (TextView) dialogLayout.findViewById(R.id.nameplate_Company);
            tvPreview1[2] = (TextView) dialogLayout.findViewById(R.id.nameplate_Position);

            for (int i = 0; i < 3; i++) {
                tvPreview1[i].setText(Para.strContent[i]);
                tvPreview1[i].setTextColor(Para.fontColor[i]);
                tvPreview1[i].setTextSize((int) (Para.fontsize[i] * 2.84));
                tvPreview1[i].setTypeface(FontManager.getFontManager().getFontType(Para.fontstyle[i]));
                tvPreview1[i].setX((float) (Para.fontPosX[i]*1.333));
                tvPreview1[i].setY((float) (Para.fontPosY[i]*1.333));
            }

            new Thread() {
                public void run() {
                    try {
                        Thread.sleep(300);
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                    // RA8876L.setPic(dialogLayout.findViewById(R.id.nameplate_layout));
                    Message msg = new Message();
                    msg.obj = dialogLayout.findViewById(R.id.nameplate_layout);
                    handler.sendMessage(msg);
                };
            }.start();
        }

        public void setPreviewBtnListener(android.view.View.OnClickListener listener) {
            ((Button) dialogLayout.findViewById(R.id.nameplate_preview_confirm_btn)).setOnClickListener(listener);
            ((Button) dialogLayout.findViewById(R.id.nameplate_preview_close_btn)).setOnClickListener(listener);
        }

        public void setUpdating() {
            this.setCanceledOnTouchOutside(false);
            (dialogLayout.findViewById(R.id.nameplate_preview_lay)).setVisibility(View.GONE);
            (dialogLayout.findViewById(R.id.nameplate_updata_lay)).setVisibility(View.VISIBLE);
        }

        private MoveTextView.OnMovementFinishListener moveListener = new MoveTextView.OnMovementFinishListener() {

            @Override
            public void onMovement(View view, float x, float y) {
                MoveTextView mtv = (MoveTextView) view;
                int type = 99;
                switch (mtv.getId()) {
                    case R.id.nameplate_preview_user_tv:
                        type = UserInfoManager.USER;
                        break;
                    case R.id.nameplate_preview_comp_tv:
                        type = UserInfoManager.COMP;
                        break;
                    case R.id.nameplate_preview_pos_tv:
                        type = UserInfoManager.POS;
                        break;
                }

                Para.fontPosX[type] = x;
                Para.fontPosY[type] = y;

                if(positionChangeListener != null)
                    positionChangeListener.onPositionChange(type,Para);
            }
        };
    }

    private void createFileWithByte(byte[] bytes) {
        // TODO Auto-generated method stub

        /**
         * 创建File对象，其中包含文件所在的目录以及文件的命名
         */
        File file = null;
        try {
            file = new File(Environment.getExternalStorageDirectory(), BYTE_FILE_NAME);
        } catch (Exception e) {
            Log.e("createFileWithByte", "" + e);
        }

        // 创建FileOutputStream对象
        FileOutputStream outputStream = null;
        // 创建BufferedOutputStream对象
        BufferedOutputStream bufferedOutputStream = null;
        try {
            // 如果文件存在则删除
            if (file.exists()) {
                file.delete();
            }
            // 在文件系统中根据路径创建一个新的空文件
            file.createNewFile();
            // 获取FileOutputStream对象
            outputStream = new FileOutputStream(file);
            // 获取BufferedOutputStream对象
            bufferedOutputStream = new BufferedOutputStream(outputStream);
            // 往文件所在的缓冲输出流中写byte数据
            bufferedOutputStream.write(bytes);
            // 刷出缓冲输出流，该步很关键，要是不执行flush()方法，那么文件的内容是空的。
            bufferedOutputStream.flush();
        } catch (Exception e) {
            // 打印异常信息
            e.printStackTrace();
        } finally {
            // 关闭创建的流对象
            if (outputStream != null) {
                try {
                    outputStream.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
            if (bufferedOutputStream != null) {
                try {
                    bufferedOutputStream.close();
                } catch (Exception e2) {
                    e2.printStackTrace();
                }
            }
        }
    }

    private byte[] readFile() {
        File file = new File(Environment.getExternalStorageDirectory(), BYTE_FILE_NAME);

        // 需要读取的文件，参数是文件的路径名加文件名
        if (file.isFile()) {
            // 以字节流方法读取文件

            FileInputStream fis = null;
            try {
                fis = new FileInputStream(file);
                // 设置一个，每次 装载信息的容器
                byte[] buffer = new byte[1024];
                ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
                // 开始读取数据
                int len = 0;// 每次读取到的数据的长度
                while ((len = fis.read(buffer)) != -1) {// len值为-1时，表示没有数据了
                    // append方法往sb对象里面添加数据
                    outputStream.write(buffer, 0, len);
                }
                // 输出字符串
                return outputStream.toByteArray();
            } catch (IOException e) {
                e.printStackTrace();
            } finally {
                if (fis != null) {
                    try {
                        fis.close();
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                }
            }
        } else {
            Log.e("readFile", "file not found");
        }
        return null;
    }
}
