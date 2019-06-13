package com.itc.ts8209a.widget;

import static java.lang.String.format;

/**
 * Created by kuangyt on 2018/12/7.
 */

public class General {

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

    public static String ByteBuf2String(byte [] buf,int len){
        String string = "";
        String str = null;
        int i;

        for(i=0;i<len;i++){
            str = Integer.toHexString(buf[i] & 0xFF);
            string += str + " ";
        }

        return string;
    }

    public static boolean ContrastArray(int[] arr1,int[] arr2){
        if(arr1.length != arr2.length) return false;

        for(int i=0;i<arr1.length;i++){
            if(arr1[i] != arr2[i]) return false;
        }

        return true;
    }

    //地址类数据字符串转整形数组函数
    public static int[] addrStrToInt(String addr) {
        int[] result = {0,0,0,0};

        String[] addrStrArray = addr.split("\\.");

        for (int i = 0; i < 4; i++) {
            result[i] = Integer.valueOf(addrStrArray[i]);
        }
        return result;
    }

    //地址类数据整形数组转字符串函数
    public static String addrIntToStr(int[] addr){
        if(addr.length != 4)
            return "";

        String result = addr[0]+"."+addr[1]+"."+addr[2]+"."+addr[3];
        return result;
    }

    // 将int类型的IP转换成字符串形式的IP
    public static int[] ipIntToArr(int ip) {
        int[] arr = new int[4];
        arr[0] = (int) (0xff & ip);
        arr[1] = (int) ((0xff00 & ip) >> 8);
        arr[2] = (int) ((0xff0000 & ip) >> 16);
        arr[3] = (int) ((0xff000000 & ip) >> 24);
        return arr;
    }


}