package com.jackie.ts8209a.Application;

/**
 * Created by kuangyt on 2018/9/6.
 */

public class Debug {
    public static String Byte2Str(byte [] buf){
        String string = "";
        String str = null;
        int i;

        for(i=0;i<buf.length;i++){
            str = Integer.toHexString(buf[i] & 0xFF);
            string += str + " ";
        }
        return string;
    }
}
