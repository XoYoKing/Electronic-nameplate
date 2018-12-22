package com.jackie.ts8209a.AppModule.Tools;

import android.util.Log;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.File;
import java.util.ArrayList;

/**
 * Created by kuangyt on 2018/12/7.
 */

public class FilePath {
    private static final String TAG = "FilePath";
    private static ArrayList<String> searchFileList = new ArrayList<String>();

    public static String[] getFiles(String string,String suffix) {
        File file = new File(string);
        File[] files = file.listFiles();
        ArrayList<String> filelist = new ArrayList<String>();
        for (int j = 0; j < files.length; j++) {
            try {
                String name = files[j].getName();
                if (files[j].isFile() & name.endsWith(suffix)) {
                    filelist.add(name);
                }
            } catch (Exception e) {
                Log.d("getFile", "err");
            }
        }
        String[] str = filelist.toArray(new String[filelist.size()]);
        return str;
    }

    public static String[] getFilesAbsolutePath(String string,String suffix) {
        File file = new File(string);
        File[] files = file.listFiles();
        ArrayList<String> filelist = new ArrayList<String>();
        if(files == null)
            return null;
        for (int j = 0; j < files.length; j++) {
            try {
                String name = files[j].getAbsolutePath();
                if (files[j].isFile() & name.endsWith(suffix)) {
                    filelist.add(name);
                }
            } catch (Exception e) {
                Log.d("getFilesAbsolutePath", "err");
            }
        }
        String[] str = filelist.toArray(new String[filelist.size()]);
        return str;
    }

    public static String[] searchFile(String path, String suffix) {
        searchFileList.clear();
        search(path,suffix);
        String[] result = searchFileList.toArray(new String[searchFileList.size()]);
        return result;
    }

    private static void search(String path, String suffix) {
        File file = new File(path);
        File[] files = file.listFiles();
        if (files.length > 0) {
            for (int j = 0; j < files.length; j++) {
                if (!files[j].isDirectory()) {
                    String name = files[j].getAbsolutePath();
                    if (files[j].isFile() & name.endsWith(suffix)) {
                        searchFileList.add(name);
                    }
                } else {
                    search(files[j].getAbsolutePath(), suffix);
                }
            }
        }
    }

    public static String[] getFolder(String string) {
        try{
            File file = new File(string);
            File[] files = file.listFiles();
            ArrayList<String> filelist = new ArrayList<String>();
            for (int j = 0; j < files.length; j++) {
                try {
                    String name = files[j].getName();
                    if (files[j].isDirectory()) {
                        filelist.add(name);
                    }
                } catch (Exception e) {
                    Log.d("getFolder", "err");
                }
            }
            String[] str = filelist.toArray(new String[filelist.size()]);
//			Log.d("getFolder", "str length="+str.length);
            return str;
        }catch(Exception e){
            Log.e("getFolder open file",e+"");
        }
        return null;
    }

    public static String[] getFolderAbsolutePath(String string) {
        try{
            File file = new File(string);
            File[] files = file.listFiles();
            ArrayList<String> filelist = new ArrayList<String>();
            for (int j = 0; j < files.length; j++) {
                try {
                    String name = files[j].getAbsolutePath();;
                    if (files[j].isDirectory()) {
                        filelist.add(name);
                    }
                } catch (Exception e) {
                    Log.d(TAG, "err");
                }
            }
            String[] str = filelist.toArray(new String[filelist.size()]);
//			Log.d("getFolder", "str length="+str.length);
            return str;
        }catch(Exception e){
            Log.e("getFolder open file",e+"");
        }
        return null;
    }

    public static JSONArray getAllFiles(String dirPath, String _type) {
        File f = new File(dirPath);
        if (!f.exists()) {//判断路径是否存在
            return null;
        }

        File[] files = f.listFiles();

        if(files==null){//判断权限
            return null;
        }

        JSONArray fileList = new JSONArray();
        for (File _file : files) {//遍历目录
            if(_file.isFile() && _file.getName().endsWith(_type)){
//                String _name=_file.getName();
                String filePath = _file.getAbsolutePath();//获取文件路径
//                String fileName = _file.getName().substring(0,_name.length()-4);//获取文件名
                int end=_file.getName().lastIndexOf('.');
                String fileName = _file.getName().substring(0,end);//获取文件名
//                Log.d("LOGCAT","fileName:"+fileName);
//                Log.d("LOGCAT","filePath:"+filePath);
                try {
                    JSONObject _fInfo = new JSONObject();
                    _fInfo.put("name", fileName);
                    _fInfo.put("path", filePath);
                    fileList.put(_fInfo);
                }catch (Exception e){
                }
            } else if(_file.isDirectory()){//查询子目录
                getAllFiles(_file.getAbsolutePath(), _type);
            } else{
            }
        }
        return fileList;
    }
}
