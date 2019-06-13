package com.itc.ts8209a.widget;

import android.util.Log;

import com.itc.ts8209a.drive.SerialPort;

import java.io.IOException;
import java.util.Calendar;

import static com.itc.ts8209a.app.AppConfig.*;

/**
 * Created by kuangyt on 2018/12/28.
 */

public class Debug {
    private static final String TAG = "Debug";
    private static SerialPort uart1;

    public static void init(){
        if(uart1 != null)
            return;

        new Thread(){
            @Override
            public void run() {
                try {
                    uart1 = new SerialPort(SerialPort.UART1,SerialPort.BAUD_115200_);
                    d(TAG,"Init finish!");
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }.start();
    }

    public static void d(final String tag, final String content) {
        if (DEBUG_LOG_EN)
            Log.d(tag, content);

        if (DEBUG_UART_EN) {
            new Thread() {
                @Override
                public void run() {
                    synchronized (this) {
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
                }
            }.start();
        }
    }
}
