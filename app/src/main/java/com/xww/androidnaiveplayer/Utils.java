package com.xww.androidnaiveplayer;

import android.os.Environment;
import android.os.Looper;
import android.util.Log;

import com.xww.androidnaiveplayer.core.Constants;

import java.io.File;

/**
 * Created by wenwu.xie on 2019/9/6.
 */
public class Utils {

    public static boolean isUIthread() {
        return Looper.myLooper() == Looper.getMainLooper();
    }

    public static String getSdCardPath() {
        String directoryPath = Environment.getExternalStorageDirectory().getPath();
        String state = Environment.getExternalStorageState();
        Log.i(Constants.TAG, "exoplayer getSdCardPath: " + directoryPath + " dir state: " + state);
        return directoryPath;
    }

    public static String getLocalMusicFile() {
        String dir = getSdCardPath();
        String musicName1= "5863_46322_l.m4a";
        String musicName2 = "meihao_xiangkou_bosong.mp3";
        String musicName3 = "3599309_1438250002_h.flac";
        String fullPath = dir + File.separator + "Music" + File.separator + musicName3;
        Log.i(Constants.TAG, "getLocalMusicFile fullpath: " + fullPath);
        return fullPath;
    }

    public static String getLocalVideoPath() {
        String dir = getSdCardPath();
        String videoName = "lrh_babamama.mp4";
        return dir + File.separator + "DCIM/Camera" + File.separator + videoName;
    }

    public static boolean isVideo(String mediaType) {
        return mediaType != null && mediaType.startsWith(Constants.PREFIX_VIDEO_TYPE);
    }
}
