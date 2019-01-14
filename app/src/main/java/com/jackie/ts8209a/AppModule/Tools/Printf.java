package com.jackie.ts8209a.AppModule.Tools;

import android.util.Log;

import com.jackie.ts8209a.Drive.SerialPort;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;

/**
 * Created by kuangyt on 2018/12/28.
 */

public class Printf {
    private static final boolean isLog = false;
    private static final boolean isSerial = true;
    private static SerialPort uart1;

    public static void init(){
        if(uart1 != null)
            return;

        new Thread(){
            @Override
            public void run() {
                try {
                    uart1 = new SerialPort(SerialPort.UART1,SerialPort.BAUD_115200_);
                    d("APP","Init finish!");
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }.start();
    }

    public static void d(final String tag, final String content) {
        if (isLog)
            Log.d(tag, content);

        if (isSerial) {
            new Thread() {
                @Override
                public void run() {
                    Calendar cal = Calendar.getInstance();
                    String time = String.format("%02d-%02d-%02d %02d:%02d:%02d",
                            cal.get(Calendar.YEAR),
                            cal.get(Calendar.MONTH) + 1,
                            cal.get(Calendar.DAY_OF_MONTH),
                            cal.get(Calendar.HOUR_OF_DAY),
                            cal.get(Calendar.MINUTE),
                            cal.get(Calendar.SECOND));
                    String print = String.format("[%s] D/%s:%s\r\n", time, tag, content);
                    uart1.send(print);
                }
            }.start();
        }
    }
}
