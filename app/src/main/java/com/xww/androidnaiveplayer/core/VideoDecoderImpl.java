package com.xww.androidnaiveplayer.core;

import android.media.MediaCodec;
import android.media.MediaFormat;
import android.os.Message;
import android.util.Log;
import android.view.Surface;
import android.view.SurfaceHolder;
import android.view.SurfaceView;

import androidx.annotation.NonNull;

import java.nio.ByteBuffer;

/**
 * Created by wenwu.xie on 2020/3/13.
 */
class VideoDecoderImpl extends AbsDecoder {
    private static final String TAG = Constants.TAG;
    private SurfaceView surfaceView;
    private Surface surface;

    VideoDecoderImpl(@NonNull String path, @NonNull SurfaceView surfaceView) {
        super(path, Constants.PREFIX_VIDEO_TYPE);
        this.surfaceView = surfaceView;
    }

    @Override
    boolean initRender(final MediaFormat mediaFormat) {
        if (surfaceView == null && surface == null) {
            throw new IllegalStateException("init video render but surface null!");
        }
        final Message msg = Message.obtain();
        msg.what = MSG_CODER_CONFIGURE;
        msg.obj = mediaFormat;
        if (surface == null) {
            Surface viewSFV = surfaceView.getHolder().getSurface();
            if (viewSFV != null) {
                surface = viewSFV;
                mHandler.sendMessage(msg);
            } else {
                surfaceView.getHolder().addCallback(new SurfaceHolder.Callback() {
                    @Override
                    public void surfaceCreated(SurfaceHolder holder) {
                        Log.i(TAG, "video decoder surface Created!");
                        surface = holder.getSurface();
                        mHandler.sendMessage(msg);
                    }

                    @Override
                    public void surfaceChanged(SurfaceHolder holder, int format, int width, int height) {
                        Log.i(TAG, "video decoder surfaceChanged!");
                    }

                    @Override
                    public void surfaceDestroyed(SurfaceHolder holder) {
                        Log.i(TAG, "video decoder surfaceDestroyed!");
                    }
                });
            }
        } else {
            mHandler.sendMessage(msg);
        }
        return true;
    }

    @Override
    boolean configCodec(MediaCodec codec, MediaFormat mediaFormat) {
        if (surface == null) {
            throw new IllegalStateException("config video decoder but surface null!");
        }
        Log.i(TAG, "VideoDecoder configCodec ");
        codec.configure(mediaFormat, surface, null, 0);
        mHandler.sendEmptyMessage(AbsDecoder.MSG_CODER_START);
        return true;
    }

    @Override
    protected void onConfigured(Message msg) {
        super.onConfigured(msg);
    }

    @Override
    void render(ByteBuffer byteBuf, int size, int bufIndx, long delayTime) {
        // Log.i(TAG, "video render size: " + size + " delay: " + delayTime);
        if (size <= 0) {
            return;
        }
        if (delayTime > Constants.DEFAULT_EXEC_DIF) {
            Message msg = Message.obtain();
            msg.what = AbsDecoder.MSG_CODER_RENDER_DELAY;
            msg.obj = byteBuf;
            msg.arg1 = size;
            msg.arg2 = bufIndx;
            mHandler.sendMessageDelayed(msg, delayTime);
        } else if (delayTime < Constants.THRESH_HOLD_DATA_TIME_DELAY) {
            releaseBufSafely(bufIndx, true);
        } else {
            releaseBufSafely(bufIndx, false);
        }
    }

    @Override
    protected void onRenderDelay(Message msg) {
        int bufIndx = msg.arg2;
        releaseBufSafely(bufIndx, false);
    }

    @Override
    void onFinished() {
        release();
    }
}
