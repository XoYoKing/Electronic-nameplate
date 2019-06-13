package com.itc.ts8209a.drive;

import java.io.File;
import java.io.FileDescriptor;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;

/**
 * Created by kuangyt on 2018/12/28.
 */

public class SerialPort {
    private final String TAG = "SerialPort";

    public static final String UART1 = "/dev/ttyS1";
    public static final String UART2 = "/dev/ttyS2";
    public static final String UART3 = "/dev/ttyS3";
    public static final int BAUD_9600_ = 9600;
    public static final int BAUD_57600_ = 57600;
    public static final int BAUD_115200_ = 115200;

    //Do not remove or rename the field mFd: it is used by native method close();
    private FileDescriptor mFd;
    private FileInputStream inputStream;
    private FileOutputStream outputStream;
    private Thread receiveThread;
    private OnReceiveListener listener;

    public SerialPort(String port, int baudrate) throws SecurityException, IOException {
        File device = new File(port);

		/* Check access permission */
        if (!device.canRead() || !device.canWrite()) {
            try {
				/* Missing read/write permission, trying to chmod the file */
                Process su;
                su = Runtime.getRuntime().exec("/system/bin/su");
                String cmd = "chmod 666 " + device.getAbsolutePath() + "\n" + "exit\n";
                su.getOutputStream().write(cmd.getBytes());
                if ((su.waitFor() != 0) || !device.canRead()
                        || !device.canWrite()) {
                    throw new SecurityException();
                }
            } catch (Exception e) {
                e.printStackTrace();
                throw new SecurityException();
            }
        }

        mFd = open(device.getAbsolutePath(), baudrate, 0);
        if (mFd == null) {
//            Log.e(TAG, "native open returns null");
            throw new IOException();
        }
        inputStream = new FileInputStream(mFd);
        outputStream = new FileOutputStream(mFd);

        receiveThread = new Thread(receive);
        receiveThread.start();
    }

    private Runnable receive = new Runnable() {
        byte[] buffer = new byte[1024];

        @Override
        public void run() {
            int size;
            synchronized (this){
                while(true){
                    try {

                        if (inputStream == null)
                            return;
                        size = inputStream.read(buffer);
                        if (size > 0) {
//                            Log.d(TAG, General.ByteBuf2String(buffer,size));
                            if(listener != null) {
                                byte[] buf = new byte[size];
                                System.arraycopy(buffer,0,buf,0,size);
                                listener.receive(buf);
                            }
                        }
                        Thread.sleep(200);
                    } catch (IOException e) {
                        e.printStackTrace();
                        return;
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                        return;
                    }
                }
            }
        }
    };

    public interface OnReceiveListener{
        void receive(byte[] buf);
    }

    public void setOnReceiveListener(OnReceiveListener listener){
        this.listener = listener;
    }

    public void send(final byte[] buf, int len){
        if(len > buf.length) {
            len = buf.length;
        }
        try {
            outputStream.write(buf, 0, len);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public void send(final byte[] buf) {
        try {
            outputStream.write(buf);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public void send(String str){
        byte [] buf = str.getBytes();
        try {
            outputStream.write(buf);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public void closeport(){
        receiveThread.interrupt();
        close();
    }

    //JNI
    static {
        System.loadLibrary("serialport");
    }

    private native static FileDescriptor open(String path, int baudrate, int flags);

    public native void close();

}
