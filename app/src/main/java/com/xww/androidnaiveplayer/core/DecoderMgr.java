package com.xww.androidnaiveplayer.core;

import android.view.SurfaceView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

/**
 * Created by wenwu.xie on 2020/3/11.
 */
public class DecoderMgr {

    public static final int MODE_DECODE_AUDIO = 1;
    public static final int MODE_DECODE_VIDEO = 2;
    private static DecoderMgr sInstance;

    private AudioDecoderImpl audioDecoderImpl;
    private VideoDecoderImpl videoDecoderImpl;
    private int mode;
    private String sourcePath;

    private DecoderMgr() {

    }

    public static synchronized DecoderMgr getInstance() {
        if (sInstance == null) {
            sInstance = new DecoderMgr();
        }
        return sInstance;
    }

    public void init(@NonNull String path, @Nullable SurfaceView surfaceView) {
        if (path.equalsIgnoreCase(sourcePath)) {
            return;
        }
        release();
        if (surfaceView == null) {
            mode = MODE_DECODE_AUDIO;
            sourcePath = path;
            audioDecoderImpl = new AudioDecoderImpl(path);
        } else {
            mode = MODE_DECODE_VIDEO;
            sourcePath = path;
            audioDecoderImpl = new AudioDecoderImpl(path);
            videoDecoderImpl = new VideoDecoderImpl(path, surfaceView);
        }
    }

    public void start() {
        long startTime = System.currentTimeMillis();
        audioDecoderImpl.start(startTime);
        if (mode == MODE_DECODE_VIDEO) {
            videoDecoderImpl.start(startTime);
        }
    }

    public void release() {
        if (mode == MODE_DECODE_AUDIO) {
            audioDecoderImpl.onFinished();
        } else if (mode == MODE_DECODE_VIDEO) {
            audioDecoderImpl.onFinished();
            videoDecoderImpl.onFinished();
        }
    }
}
