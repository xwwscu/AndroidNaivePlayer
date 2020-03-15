package com.xww.androidnaiveplayer;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

import android.os.Bundle;
import android.util.Log;
import android.view.SurfaceView;
import android.view.View;

import com.xww.androidnaiveplayer.core.DecoderMgr;

public class MainActivity extends AppCompatActivity {
    private static final String TAG = "xww-player-ui";
    private SurfaceView mSurfaceView;
    private DecoderMgr mDecoderMgr;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        mDecoderMgr = DecoderMgr.getInstance();
        findViewById(R.id.test_audio_btn).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                String musicUrl = Utils.getLocalMusicFile();
                Log.i(TAG, "test audio url: " + musicUrl);
                mDecoderMgr.init(musicUrl, null);
                mDecoderMgr.start();
            }
        });

        mSurfaceView = findViewById(R.id.video_surface);
        findViewById(R.id.test_video_btn).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                String videoUrl = Utils.getLocalVideoPath();
                Log.i(TAG, "test video url: " + videoUrl);
                mDecoderMgr.init(videoUrl, mSurfaceView);
                mDecoderMgr.start();
            }
        });
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (mDecoderMgr != null) {
            mDecoderMgr.release();
        }
    }
}
