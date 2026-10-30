package com.miui.gallery.activity;

import android.app.Activity;
import android.content.Intent;
import android.media.MediaPlayer;
import android.media.PlaybackParams;
import android.os.Bundle;
import android.view.Gravity;
import android.view.SurfaceHolder;
import android.view.SurfaceView;
import android.view.ViewGroup;
import android.widget.FrameLayout;
import android.widget.TextView;

public class SlowMotionActivity extends Activity implements SurfaceHolder.Callback {
    private MediaPlayer mPlayer;
    private SurfaceView mSurface;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        FrameLayout root = new FrameLayout(this);
        mSurface = new SurfaceView(this);
        root.addView(mSurface, new FrameLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT));
        TextView badge = new TextView(this);
        badge.setText("0.25x");
        badge.setTextSize(24);
        badge.setGravity(Gravity.CENTER);
        badge.setBackgroundColor(0x99000000);
        badge.setTextColor(0xffffffff);
        FrameLayout.LayoutParams lp = new FrameLayout.LayoutParams(180, 90,
                Gravity.TOP | Gravity.END);
        lp.setMargins(24, 80, 24, 0);
        root.addView(badge, lp);
        setContentView(root);
        mSurface.getHolder().addCallback(this);
    }

    @Override
    public void surfaceCreated(SurfaceHolder holder) {
        try {
            mPlayer = new MediaPlayer();
            mPlayer.setDataSource(this, getIntent().getData());
            mPlayer.setDisplay(holder);
            mPlayer.setOnCompletionListener(mp -> finish());
            mPlayer.prepare();
            mPlayer.setPlaybackParams(new PlaybackParams().setSpeed(0.25f));
            mPlayer.start();
        } catch (Exception e) {
            finish();
        }
    }

    @Override
    public void surfaceChanged(SurfaceHolder holder, int format, int width, int height) {
    }

    @Override
    public void surfaceDestroyed(SurfaceHolder holder) {
    }

    @Override
    protected void onDestroy() {
        if (mPlayer != null) {
            mPlayer.release();
            mPlayer = null;
        }
        super.onDestroy();
    }
}
