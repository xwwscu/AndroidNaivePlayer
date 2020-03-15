package com.xww.androidnaiveplayer.core;

import android.media.MediaFormat;

import java.nio.ByteBuffer;

/**
 * Created by wenwu.xie on 2020/3/9.
 */
public interface IExtractor {

    MediaFormat getMediaFormat();

    void seekTo(long position);

    void setStartPosition(long position);

    long getCurrentTimeStamp();

    int readBuffer(ByteBuffer byteBuf);

    void stop();
}
