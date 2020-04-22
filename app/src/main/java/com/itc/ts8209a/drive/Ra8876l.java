package com.itc.ts8209a.drive;

import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.util.Log;
import android.view.View;

import java.util.Arrays;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

/**
 * Created by kuangyt on 2018/10/23.
 */

public class Ra8876l {
    private static String TAG = "RA8876L";

    private static boolean picSetting = false;
    private static OnInitFinishListener initFinishListener = null;
    private static OnSetPicFinishListener picFinishListener = null;
    private static OnSetPicTimeOutListener picTimeOutListener = null;
    private static OnImgDataListener imgDataListener = null;

    private static sendImageData sendImgData = new sendImageData();
    private static Bitmap viewBmp = null;
    private static byte[] imgData = null;
    private Thread sendDataThread  = null;

    static {
        System.loadLibrary("ra8876l");
    }

    private Ra8876l(){
    }

    public static void devHandshake(){
        handShake();
    }

    public static void devInit(){
        new Thread(){
            public void run() {
                devHandshake();
                init(45*1000*1000);
                if(initFinishListener != null){
                    initFinishListener.onInitFinish();
                }
            }
        }.start();
    }

    public static void setPic(final View view){
        Log.d(TAG,"w="+view.getMeasuredWidth()+",h="+view.getMeasuredHeight());

        viewBmp = getViewBitmap(view);
        imgData = getPixels(viewBmp);

        if(imgDataListener != null){
            imgDataListener.onImgData(imgData);
        }

//        Debug.d("setPic","data len="+imageData.length);

        setPic(imgData);

        viewBmp = null;
//        imgData = null;
    }

    public static void setPic(final byte[] data) {
        ExecutorService transThread = Executors.newSingleThreadExecutor();
        final Future transPending;

        sendImgData.setImageData(data);
        transPending = transThread.submit(sendImgData);

//		sendDataThread = new Thread(sendImgData);
//		sendDataThread.start();


		new Thread() {
			public void run() {
				int timeOut = 10;

				while (sendImgData.beingSend) {
					try {
						Thread.sleep(800);
					} catch (InterruptedException e) {
						e.printStackTrace();
					}
					timeOut--;
					if (timeOut <= 0) {
						try{
//							sendDataThread.destroy();

							boolean ret = transPending.cancel(true);
//							sendDataThread = null;
							Log.e("sendDataThread","cancel thread: "+ret);
							sleep(1000);
						}catch(Exception e){
							Log.e("Stop sendDataThread",""+e);
						}
//						sendImgData.imageData = null;
						if (picTimeOutListener != null)
							picTimeOutListener.onSetPicTimeOut();
//						sendImgData.beingSend = false;

						return;
					}
				}
			};
		}.start();
    }

    private static class sendImageData implements Runnable {
        public boolean beingSend = false;
        public byte[] imageData = new byte[1024*600*3];

        public void setImageData(byte[] data) {
//            imageData = data;
            System.arraycopy(data,0, imageData,0,data.length);
        }

        @Override
        public void run() {
//            synchronized ("sendImageData") {
                Log.i("sendImageData Thread","Start");
                if (imageData == null || beingSend)
                    return;
                else {
//                     displayOff();
                    beingSend = true;
                    sendData((short) 0, (short) 0, (short) 1024, (short) 600, imageData, imageData.length);
//                    imageData = null;
                    Arrays.fill(imageData,(byte)0x00);
                    beingSend = false;
//                    displayOn();
                    if (picFinishListener != null)
                        picFinishListener.onSetPicFinish();

                }
                Log.i("sendImageData Thread","End");
//            }
        }
    };

    public static void setOninitFinishListener(OnInitFinishListener listener){
        initFinishListener = listener;
    }

    public static void setOnSetPicFinishListener(OnSetPicFinishListener listener){
        picFinishListener = listener;
    }

    public static void setOnSetPicTimeOutListener(OnSetPicTimeOutListener listener){
        picTimeOutListener = listener;
    }

    public static void setOnImgDataListenerListener(OnImgDataListener listener){
        imgDataListener = listener;
    }

    public interface OnInitFinishListener{
        void onInitFinish();
    }

    public interface OnSetPicFinishListener{
        void onSetPicFinish();
    }

    public interface OnSetPicTimeOutListener{
        void onSetPicTimeOut();
    }

    public interface OnImgDataListener{
        void onImgData(byte[] data);
    }

    private static int[] pixels = null; // 通过位图的大小创建像素点数组
    //	private static byte[] bytePixels = null;
    private static byte[] getPixels(Bitmap bmp) {

        int width = bmp.getWidth(); // 获取位图的宽
        int height = bmp.getHeight(); // 获取位图的高

//        Debug.d("getPixels","w="+width+",h="+height);
        pixels = new int[width * height]; // 通过位图的大小创建像素点数组
        imgData = new byte[width * height * 3];
        // byte[] bytePixels = new byte[width * height * 2];
        // Log.d("getPixels","w="+width+",h="+height);

        bmp.getPixels(pixels, 0, width, 0, 0, width, height);

        for (int i = 0; i < height; i++) {
            for (int j = 0; j < width; j++) {
                int grey = pixels[width * i + j];

                imgData[(width * i + j) * 3 + 2] = (byte) ((grey & 0x00FF0000) >> 16);
                imgData[(width * i + j) * 3 + 1] = (byte) ((grey & 0x0000FF00) >> 8);
                imgData[(width * i + j) * 3]     = (byte) (grey & 0x000000FF);
            }
        }

        pixels = null;
        return imgData;
    }


    private static Bitmap getViewBitmap(View v) {
        int w = v.getMeasuredWidth();
        int h = v.getMeasuredHeight();
        Bitmap bmp = Bitmap.createBitmap(w, h, Bitmap.Config.ARGB_8888);
        Canvas c = new Canvas(bmp);

        c.drawColor(Color.WHITE);

        v.layout(0, 0, w, h);
        v.draw(c);
        return bmp;


//        v.destroyDrawingCache();
//        v.measure(View.MeasureSpec.makeMeasureSpec(0, View.MeasureSpec.UNSPECIFIED),
//        View.MeasureSpec.makeMeasureSpec(0, View.MeasureSpec.UNSPECIFIED));
//        v.layout(0, 0, v.getMeasuredWidth(), v.getMeasuredHeight());
//        v.setDrawingCacheEnabled(true);
//        return v.getDrawingCache(true);


    }

    private native static void sendData(short x,short y,short width,short height,byte[] data,int len);

    private native static void handShake();

    private native static void init(int speed);

    private native static void displayOn();

    private native static void displayOff();
}
