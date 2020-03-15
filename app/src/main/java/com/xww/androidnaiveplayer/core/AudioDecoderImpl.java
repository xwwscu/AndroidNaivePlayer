package com.xww.androidnaiveplayer.core;

import android.media.AudioAttributes;
import android.media.AudioFormat;
import android.media.AudioManager;
import android.media.AudioTrack;
import android.media.MediaCodec;
import android.media.MediaFormat;
import android.os.Message;
import android.util.Log;
import java.nio.ByteBuffer;
import androidx.annotation.NonNull;

/**
 * Created by wenwu.xie on 2020/3/13.
 */
public class AudioDecoderImpl extends AbsDecoder {

    private static final String TAG = Constants.TAG;
    /**音频播放器*/
    private AudioTrack mAudioTrack = null;
    /**音频数据缓存*/
    private short[] mAudioOutTempBuf = null;

    AudioDecoderImpl(@NonNull String path) {
        super(path, null);
    }

    @Override
    boolean initRender(MediaFormat mediaFormat) {
        try {
            int sampleRate;
            int channels;
            int pCMEncodeBit;
            channels = mediaFormat.getInteger(MediaFormat.KEY_CHANNEL_COUNT);
            int sourceSampleRate = mediaFormat.getInteger(MediaFormat.KEY_SAMPLE_RATE);
            if (mediaFormat.containsKey(MediaFormat.KEY_PCM_ENCODING)) {
                pCMEncodeBit = mediaFormat.getInteger(MediaFormat.KEY_PCM_ENCODING);
            } else {
                //如果没有这个参数，默认为16位采样
                pCMEncodeBit = AudioFormat.ENCODING_PCM_16BIT;
            }
            int channel = AudioFormat.CHANNEL_OUT_STEREO;
            if (channels == 1) { //单声道
                channel = AudioFormat.CHANNEL_OUT_MONO;
            }
            sampleRate = sourceSampleRate * 2;
            if (sampleRate > Constants.MAX_SAMPLE_RATE_FOR_HUMAN) {
                sampleRate = Constants.MAX_SAMPLE_RATE_FOR_HUMAN;
            }//获取最小缓冲区
            int minBufferSize = AudioTrack.getMinBufferSize(sampleRate, channel, pCMEncodeBit);
            Log.i(TAG, "initRender channels: " + channels + " sourceSampleRate: " + sourceSampleRate
                    + " sampleRate: " + sampleRate + " PCMEncodeBit: " + pCMEncodeBit + " minBufSize: " + minBufferSize);
            // todo shortBuffer
            mAudioOutTempBuf = new short[minBufferSize/2];
            AudioAttributes audioAttributes = new AudioAttributes.Builder()
                    .setLegacyStreamType(AudioManager.STREAM_MUSIC)
                    .build();
            AudioFormat audioFormat = new AudioFormat.Builder()
                    .setChannelMask(channel)
                    .setEncoding(pCMEncodeBit)
                    .setSampleRate(sampleRate)
                    .build();
            mAudioTrack = new AudioTrack(audioAttributes, audioFormat,
                    minBufferSize, //缓冲区大小
                    AudioTrack.MODE_STREAM, //播放模式：数据流动态写入，另一种是一次性写入
                    AudioManager.AUDIO_SESSION_ID_GENERATE);
            return true;
        } catch (Exception exp) {
            Log.e(TAG, "initRender exp: ", exp);
        }
        return false;
    }

    @Override
    protected boolean initCodec(MediaFormat format) {
        boolean isSuc = super.initCodec(format);
        Message msg = Message.obtain();
        msg.what = MSG_CODER_CONFIGURE;
        msg.obj = format;
        mHandler.sendMessage(msg);
        return isSuc;
    }

    @Override
    boolean configCodec(MediaCodec codec, MediaFormat mediaFormat) {
        codec.configure(mediaFormat, null, null, 0);
        mHandler.sendEmptyMessage(AbsDecoder.MSG_CODER_START);
        return true;
    }

    @Override
    void render(ByteBuffer byteBuf, int size, int index, long delayTime) {
        // Log.i(TAG, "audio render size: " + size + " delay: " + delayTime);
        if (size < 0) {
            releaseBufSafely(index, false);
            return;
        }
        if (delayTime > Constants.DEFAULT_EXEC_DIF) {
            Message msg = Message.obtain();
            msg.what = AbsDecoder.MSG_CODER_RENDER_DELAY;
            msg.obj = byteBuf;
            msg.arg1 = size;
            msg.arg2 = index;
            mHandler.sendMessageDelayed(msg, delayTime);
        } else {
            mAudioTrack.write(byteBuf, size, AudioTrack.WRITE_BLOCKING);
            releaseBufSafely(index, false);
        }
    }

    @Override
    protected void onStarted() {
        super.onStarted();
        mAudioTrack.play();
    }

    @Override
    protected void onRenderDelay(Message msg) {
        ByteBuffer outputBuf = (ByteBuffer) msg.obj;
        int size = msg.arg1;
        int bufIndx = msg.arg2;
        mAudioTrack.write(outputBuf, size, AudioTrack.WRITE_BLOCKING);
        releaseBufSafely(bufIndx, false);
    }

    @Override
    void onFinished() {
        if (mAudioTrack != null) {
            if (mAudioTrack.getState() != AudioTrack.STATE_UNINITIALIZED) {
                mAudioTrack.stop();
                mAudioTrack.release();
            }
        }
        release();
    }
}
