package com.xww.androidnaiveplayer.core;

import android.media.MediaCodec;
import android.media.MediaFormat;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.Message;
import android.text.TextUtils;
import android.util.Log;
import java.io.File;
import java.io.IOException;
import java.nio.ByteBuffer;
import androidx.annotation.NonNull;

import com.xww.androidnaiveplayer.Utils;

/**
 * Created by wenwu.xie on 2020/3/13.
 */
public abstract class AbsDecoder {
    private static final String TAG = Constants.TAG;
    protected static final int MSG_CODER_INIT = 1;
    protected static final int MSG_CODER_CONFIGURE = 2;
    protected static final int MSG_CODER_START = 3;
    protected static final int MSG_CODER_RENDER_DELAY = 4;
    protected static final int MSG_CODER_FINISH = 5;
    private String mFilePath;
    private String mediaType;
    private long mStartTime = -1;
    private long mRunTime = -1;
    private volatile DecodeState mState;
    protected MediaCodec mMediaCodec = null;
    protected IExtractor mMediaExtractor = null;
    private HandlerThread mWorkThread;
    protected Handler mHandler;

    public AbsDecoder(@NonNull String path, String mediaType) {
        this.mFilePath = path;
        if (TextUtils.isEmpty(mediaType)) {
            this.mediaType = Constants.PREFIX_AUDIO_TYPE;
        } else {
            this.mediaType = mediaType;
        }
        mWorkThread = new HandlerThread("thread-" + this.mediaType);
        mWorkThread.start();
        mHandler = new Handler(mWorkThread.getLooper()) {
            @Override
            public void handleMessage(@NonNull Message msg) {
                processMsg(msg);
            }
        };
        mHandler.sendEmptyMessage(MSG_CODER_INIT);
    }

    private void processMsg(Message msg) {
        if (msg.what == MSG_CODER_CONFIGURE) {
            onConfigured(msg);
        } else if (msg.what == MSG_CODER_START) {
            onStarted();
        } else if (msg.what == MSG_CODER_FINISH) {
            onFinished();
        } else if (msg.what == MSG_CODER_INIT) {
            init();
        } else if (msg.what == MSG_CODER_RENDER_DELAY) {
            onRenderDelay(msg);
        }
    }

    private boolean init() {
        if (TextUtils.isEmpty(mFilePath) || !new File(mFilePath).exists()) {
            Log.e(TAG, "file path : " + mFilePath + " invalid!!!");
            return false;
        }
        mStartTime = -1;
        mState = DecodeState.INITIAL;
        mMediaExtractor = initExtractor();
        MediaFormat mediaFormat = mMediaExtractor == null ? null : mMediaExtractor.getMediaFormat();
        if (mMediaExtractor == null || mediaFormat == null) {
            return false;
        }
        if (!initRender(mediaFormat)) {
            return false;
        }
        if (!initCodec(mediaFormat)) {
            return false;
        }
        mState = DecodeState.INITED;
        return true;
    }

    private IExtractor initExtractor() {
        ExtractorImpl extractor = new ExtractorImpl(mFilePath, mediaType);
        return extractor;
    }

    protected boolean initCodec(final MediaFormat format) {
        try {
            String mediaType = format.getString(MediaFormat.KEY_MIME);
            Log.i(TAG, "initCodec media type: " + mediaType + " thread: " + Thread.currentThread().getName() + " tid: " + Thread.currentThread().getId());
            if (TextUtils.isEmpty(mediaType)) {
                return false;
            }
            mMediaCodec = MediaCodec.createDecoderByType(mediaType);
            mMediaCodec.setCallback(new MediaCodec.Callback() {
                @Override
                public void onInputBufferAvailable(@NonNull MediaCodec codec, int index) {
                    // Log.i(TAG, "initCodec Callback onInputBufferAvailable: " + index);
                    ByteBuffer inputBuffer = codec.getInputBuffer(index);
                    // extractor readData
                    int dataLen = mMediaExtractor.readBuffer(inputBuffer);
                    if (dataLen < 0) {
                        codec.queueInputBuffer(index, 0, 0, 0, MediaCodec.BUFFER_FLAG_END_OF_STREAM);
                    } else {
                        codec.queueInputBuffer(index, 0, dataLen, mMediaExtractor.getCurrentTimeStamp(), 0);
                    }
                }

                @Override
                public void onOutputBufferAvailable(@NonNull MediaCodec codec, int index, @NonNull MediaCodec.BufferInfo info) {
                    if (index < 0) {
                        Log.e(TAG, "onOutputBufferAvailable index inValid value: " + index);
                        return;
                    }
                    if (info.flags == MediaCodec.BUFFER_FLAG_END_OF_STREAM) {
                        Log.i(TAG, "Callback onOutputBufferAvailable endOfStream!!!");
                        mHandler.sendEmptyMessage(MSG_CODER_FINISH);
                        return;
                    }
                    // 获取输出缓冲(其中包含编解码后数据)
                    ByteBuffer outputBuf = codec.getOutputBuffer(index);
                    // MediaFormat bufFormat = codec.getOutputFormat(index);
                    // 处理编解码后的数据
                    long difTime  = getDifTime(info);
                    render(outputBuf, info.size, index, difTime);
                    // 返还输出缓冲给 codec
                    // releaseBufSafely(codec, index);
                }

                @Override
                public void onError(@NonNull MediaCodec codec, @NonNull MediaCodec.CodecException e) {
                    Log.e(TAG, "MediaCodec onErr: ", e);
                }

                @Override
                public void onOutputFormatChanged(@NonNull MediaCodec codec, @NonNull MediaFormat format) {
                    Log.i(TAG, "onOutputFormatChanged format: " + format.getString(MediaFormat.KEY_MIME));
                }
            });
        } catch (IOException e) {
            Log.e(TAG, "create Codec err,", e);
            return false;
        }
        return true;
    }

    protected void releaseBufSafely(int bufIndex, boolean isDiscard) {
        try {
            mMediaCodec.releaseOutputBuffer(bufIndex, !isDiscard & Utils.isVideo(mediaType));
        } catch (Exception exp) {
            Log.e(TAG, "releaseCodecBufSafely exp: ", exp);
        }
    }

    protected void release() {
        try {
            if (mMediaExtractor != null) {
                mMediaExtractor.stop();
            }
            if (mMediaCodec != null) {
                mMediaCodec.stop();
                mMediaCodec.release();
            }
        } catch (Exception exp) {
            Log.e(TAG, "release player exp: ", exp);
        }
    }

    private long getDifTime(MediaCodec.BufferInfo bufInfo) {
        long passTime = System.currentTimeMillis() - mRunTime;
        long curTime = bufInfo.presentationTimeUs / 1000;
        long timeDif = curTime - passTime;
        Log.i(TAG, "syncRender time: " + timeDif + ", mediaType: " + mediaType);
        return timeDif;
    }

    protected void start(long startTime) {
        mStartTime = startTime;
    }

    protected void onConfigured(Message msg) {
        MediaFormat format = (MediaFormat) msg.obj;
        configCodec(mMediaCodec, format);
    }

    protected void onStarted() {
        mRunTime = System.currentTimeMillis();
        mState = DecodeState.START;
        mMediaCodec.start();
    }

    protected void onRenderDelay(Message msg) {

    }

    abstract boolean initRender(MediaFormat mediaFormat);

    abstract boolean configCodec(MediaCodec codec, MediaFormat mediaFormat);

    abstract void render(ByteBuffer byteBuf, int size, int bufIndx, long delayTime);

    abstract void onFinished();
}
