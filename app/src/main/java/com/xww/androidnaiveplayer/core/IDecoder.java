package com.xww.androidnaiveplayer.core;

import android.media.MediaFormat;

/**
 * Created by wenwu.xie on 2020/3/9.
 */
public interface IDecoder {

    void start();

    void stop();

    boolean isDecoding();

    boolean isSeeking();

    long getDuration();

    int getWidth();

    int getHeight();

    int getTrack();

    MediaFormat getMediaFormat();

    String getFilePath();
}
