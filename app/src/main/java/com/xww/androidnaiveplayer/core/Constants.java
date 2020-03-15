package com.xww.androidnaiveplayer.core;

/**
 * Created by wenwu.xie on 2020/3/13.
 */
public class Constants {

    public static final String TAG = "xww-player";
    public static final String PREFIX_AUDIO_TYPE = "audio";
    public static final String PREFIX_VIDEO_TYPE = "video";
    public static final String MSG_KEY_BUF_OFFSET = "data_buf_info_offset";
    public static final String MSG_KEY_BUF_SIZE = "data_buf_info_size";
    public static final String MSG_KEY_BUF_FLAG = "data_buf_info_flag";
    public static final String MSG_KEY_BUF_PTS = "data_buf_info_pts";
    public static final int THRESH_HOLD_DATA_TIME_DELAY = -3000;
    public static final int DEFAULT_EXEC_DIF = 5;
    public static final int MAX_SAMPLE_RATE_FOR_HUMAN = 44100; // 44.1kHz
}
