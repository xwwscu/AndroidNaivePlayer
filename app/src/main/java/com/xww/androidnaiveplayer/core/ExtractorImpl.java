package com.xww.androidnaiveplayer.core;

import android.media.MediaExtractor;
import android.media.MediaFormat;
import android.text.TextUtils;
import androidx.annotation.NonNull;
import java.io.IOException;
import java.nio.ByteBuffer;

/**
 * Created by wenwu.xie on 2020/3/13.
 */
class ExtractorImpl implements IExtractor {

    private MediaExtractor mediaExtractor;
    private MediaFormat mediaFormat;
    private String mediaType = Constants.PREFIX_AUDIO_TYPE;
    private int trackId = -1;
    private long currentSampleTime = 0;

    ExtractorImpl(@NonNull String path, String mediaType) {
        if (!TextUtils.isEmpty(mediaType)) {
            this.mediaType = mediaType;
        }
        mediaExtractor = new MediaExtractor();
        try {
            mediaExtractor.setDataSource(path);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    @Override
    public MediaFormat getMediaFormat() {
        checkExtractor();
        if (mediaFormat != null) {
            return mediaFormat;
        }
        int trackCount = mediaExtractor.getTrackCount();
        for (int i = 0; i < trackCount; i ++) {
            MediaFormat format = mediaExtractor.getTrackFormat(i);
            String mime = format.getString(MediaFormat.KEY_MIME);
            if (TextUtils.isEmpty(mime)) {
                continue;
            }
            if (mime.startsWith(mediaType)) {
                trackId = i;
                mediaFormat = format;
                break;
            }
        }
        return mediaFormat;
    }

    @Override
    public void seekTo(long position) {
        checkExtractor();
        mediaExtractor.seekTo(position, MediaExtractor.SEEK_TO_PREVIOUS_SYNC);
    }

    @Override
    public void setStartPosition(long position) {
        // todo impl later
    }

    @Override
    public long getCurrentTimeStamp() {
        return currentSampleTime;
    }

    @Override
    public int readBuffer(ByteBuffer byteBuf) {
        checkExtractor();
        if (trackId < 0) {
            throw new RuntimeException("read buffer trackId invalid.");
        }
        byteBuf.clear();
        mediaExtractor.selectTrack(trackId);
        int dataLen = mediaExtractor.readSampleData(byteBuf, 0);
        mediaExtractor.advance();
        currentSampleTime = mediaExtractor.getSampleTime();
        return dataLen;
    }

    @Override
    public void stop() {
        if (mediaExtractor != null) {
            mediaExtractor.release();
            mediaExtractor = null;
        }
    }

    private void checkExtractor() {
        if (mediaExtractor == null) {
            throw new IllegalStateException("Extractor not init!");
        }
    }
}
