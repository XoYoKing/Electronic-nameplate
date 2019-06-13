package com.itc.ts8209a.widget;

/**
 * Created by kuangyt on 2018/8/22.
 */

import android.os.Bundle;
import android.os.Handler;
import android.os.Message;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.util.Deque;
import java.util.concurrent.LinkedBlockingDeque;

/**
 * Android运行linux命令
 */
public class Cmd {
    private static final String TAG = "Cmd";
    private static Deque<CmdBundle> cmdQueue = new LinkedBlockingDeque<CmdBundle>();

    public static final String KEY_CMD = "cmd";
    public static final String KEY_RES = "result";
    public static final String KEY_VALUE = "value";

    public static void execCmd(String cmd, cmdResultListener listener) {
        execCmd(cmd, listener,null);
    }

    public static void execCmd(String cmd) {
        execCmd(cmd, null,null);
    }

    public static void execCmd(String cmd,Handler handler){
        execCmd(cmd, null,handler);
    }

    private static void execCmd(String cmd,cmdResultListener listener,Handler handler){
        CmdBundle cmdBundle = new CmdBundle();
        cmdBundle.cmd = cmd;
        cmdBundle.listener = listener;
        cmdBundle.handler = handler;
        cmdQueue.add(cmdBundle);

        new Thread(new sendCmd()).start();
    }

    //发送命令
    private static class sendCmd implements Runnable {
        private boolean sending = false;
        private CmdBundle cmdBundle = null;
        private String result = "";
        private int value;
        private DataOutputStream dos = null;
        private DataInputStream dis = null;

        @Override
        public void run() {
            if (sending) return;

            sending = true;
            synchronized (this) {
                while (cmdQueue.size() > 0) {
//                Log.d(TAG,"size:"+cmdQueue.size());
                    cmdBundle = cmdQueue.poll();
                    result = "";
                    value = -1;
                    dos = null;
                    dis = null;

                    try {
                        Process p = Runtime.getRuntime().exec("su");// 经过Root处理的android系统即有su命令
                        dos = new DataOutputStream(p.getOutputStream());
                        dis = new DataInputStream(p.getInputStream());

//                    Log.i(TAG, cmdBundle.cmd);
                        dos.writeBytes(cmdBundle.cmd + "\n");
                        dos.flush();
                        dos.writeBytes("exit\n");
                        dos.flush();
                        String line = null;
                        while ((line = dis.readLine()) != null) {
//                Log.d("result", line);
                            result += line;
                        }
                        p.waitFor();
                        value = p.exitValue();

                        Message msg = Message.obtain();
                        Bundle bundle = new Bundle();
                        bundle.putString(KEY_CMD,cmdBundle.cmd);
                        bundle.putString(KEY_RES,result);
                        bundle.putInt(KEY_VALUE,value);
                        msg.obj = bundle;
                        if (cmdBundle.listener != null){
                            cmdBundle.listener.onResult(cmdBundle.cmd,result,value);
                        }
                        else if(cmdBundle.handler != null){
                            cmdBundle.handler.sendMessage(msg);
                        }
                    } catch (Exception e) {
//                        Log.e(TAG,e+"\nby cmd: "+cmdBundle.cmd);
                        e.printStackTrace();
                    } finally {
                        if (dos != null) {
                            try {
                                dos.close();
                            } catch (IOException e) {
                                e.printStackTrace();
                            }
                        }
                        if (dis != null) {
                            try {
                                dis.close();
                            } catch (IOException e) {
                                e.printStackTrace();
                            }
                        }
                    }
                }
            }
            sending = false;
        }
    }

    private static class CmdBundle {
        private cmdResultListener listener;
        private String cmd;
        private Handler handler;
    }

    public interface cmdResultListener {
        void onResult(String cmd, String res, int value);
    }
}