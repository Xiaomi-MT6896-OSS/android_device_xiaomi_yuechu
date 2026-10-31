package com.miui.gallery.activity;

import android.app.Activity;
import android.media.MediaPlayer;
import android.media.PlaybackParams;
import android.net.Uri;
import android.os.Bundle;
import android.util.Log;
import android.view.Gravity;
import android.view.SurfaceHolder;
import android.view.SurfaceView;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.widget.FrameLayout;
import android.widget.TextView;

/** Minimal video player for the camera review path (no editing). */
public class VideoActivity extends Activity implements SurfaceHolder.Callback {
    public static final String EXTRA_SPEED = "speed";

    private static final String TAG = "MiuiGalleryStub";

    private MediaPlayer mPlayer;
    private SurfaceView mSurface;
    private TextView mBadge;
    private float mSpeed = 1f;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
        getWindow().setStatusBarColor(0xff000000);
        getWindow().setNavigationBarColor(0xff000000);
        mSpeed = getIntent().getFloatExtra(EXTRA_SPEED, 1f);

        FrameLayout root = new FrameLayout(this);
        root.setBackgroundColor(0xff000000);
        mSurface = new SurfaceView(this);
        root.addView(mSurface, new FrameLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT));

        mBadge = new TextView(this);
        mBadge.setText(mSpeed < 0.9f ? "0.25x" : "");
        mBadge.setTextSize(18);
        mBadge.setGravity(Gravity.CENTER);
        mBadge.setBackgroundColor(0x99000000);
        mBadge.setTextColor(0xffffffff);
        FrameLayout.LayoutParams lp = new FrameLayout.LayoutParams(140, 72,
                Gravity.TOP | Gravity.END);
        lp.setMargins(24, 80, 24, 0);
        root.addView(mBadge, lp);
        setContentView(root);

        root.setOnClickListener(v -> togglePlayback());
        mSurface.setOnClickListener(v -> togglePlayback());
        mSurface.getHolder().addCallback(this);
    }

    private void togglePlayback() {
        if (mPlayer == null) {
            return;
        }
        try {
            if (mPlayer.isPlaying()) {
                mPlayer.pause();
            } else {
                mPlayer.start();
            }
        } catch (Exception e) {
            Log.w(TAG, "toggle failed", e);
        }
    }

    @Override
    public void surfaceCreated(SurfaceHolder holder) {
        Uri uri = getIntent().getData();
        if (uri == null) {
            finish();
            return;
        }
        try {
            mPlayer = new MediaPlayer();
            mPlayer.setDataSource(this, uri);
            mPlayer.setDisplay(holder);
            mPlayer.setOnCompletionListener(mp -> finish());
            mPlayer.prepare();
            if (mSpeed < 0.9f) {
                mPlayer.setPlaybackParams(new PlaybackParams().setSpeed(mSpeed));
            }
            mPlayer.start();
        } catch (Exception e) {
            Log.w(TAG, "cannot play " + uri, e);
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
