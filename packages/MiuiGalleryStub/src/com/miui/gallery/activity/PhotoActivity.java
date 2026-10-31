package com.miui.gallery.activity;

import android.Manifest;
import android.app.Activity;
import android.content.ContentUris;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Matrix;
import android.graphics.RectF;
import android.graphics.drawable.BitmapDrawable;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.provider.MediaStore;
import android.util.Log;
import android.view.GestureDetector;
import android.view.Gravity;
import android.view.MotionEvent;
import android.view.ScaleGestureDetector;
import android.view.View;
import android.view.WindowManager;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.TextView;

import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;

/** Full screen photo viewer: pinch to zoom, double tap to toggle, swipe to move. */
public class PhotoActivity extends Activity {
    private static final String TAG = "MiuiGalleryStub";
    private static final int REQ_PERM = 10;
    private static final int MAX_DIM = 2560;

    private ZoomView mView;
    private TextView mCounter;
    private TextView mEdit;
    private Uri mCurrent;
    private final List<Uri> mUris = new ArrayList<>();
    private int mIndex;
    private boolean mAskedForPermission;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
        getWindow().setStatusBarColor(0xff000000);
        getWindow().setNavigationBarColor(0xff000000);

        FrameLayout root = new FrameLayout(this);
        root.setBackgroundColor(0xff000000);

        mView = new ZoomView();
        mView.setOnSwipeListener(new ZoomView.OnSwipeListener() {
            @Override
            public void onSwipeLeft() {
                show(mIndex + 1);
            }

            @Override
            public void onSwipeRight() {
                show(mIndex - 1);
            }

            @Override
            public void onTap() {
                mCounter.setVisibility(mCounter.getVisibility() == View.VISIBLE
                        ? View.GONE : View.VISIBLE);
            }
        });
        root.addView(mView, new FrameLayout.LayoutParams(
                FrameLayout.LayoutParams.MATCH_PARENT, FrameLayout.LayoutParams.MATCH_PARENT));

        mCounter = new TextView(this);
        mCounter.setTextColor(0xffffffff);
        mCounter.setTextSize(14);
        mCounter.setShadowLayer(4, 0, 0, 0xff000000);
        FrameLayout.LayoutParams lp = new FrameLayout.LayoutParams(
                FrameLayout.LayoutParams.WRAP_CONTENT, FrameLayout.LayoutParams.WRAP_CONTENT,
                Gravity.BOTTOM | Gravity.CENTER_HORIZONTAL);
        lp.bottomMargin = 48;
        root.addView(mCounter, lp);

        // Editing stays in Gallery2: FilterShowActivity is exported for
        // ACTION_EDIT, so hand the photo over instead of reimplementing it.
        mEdit = new TextView(this);
        mEdit.setText("编辑");
        mEdit.setTextColor(0xffffffff);
        mEdit.setTextSize(15);
        mEdit.setGravity(Gravity.CENTER);
        mEdit.setBackgroundColor(0x66000000);
        FrameLayout.LayoutParams editLp = new FrameLayout.LayoutParams(160, 88,
                Gravity.TOP | Gravity.END);
        editLp.setMargins(24, 96, 24, 0);
        mEdit.setOnClickListener(v -> openEditor());
        root.addView(mEdit, editLp);

        setContentView(root);

        Uri uri = getIntent().getData();
        if (uri == null) {
            finish();
            return;
        }
        mUris.add(uri);
        mIndex = 0;
        loadSiblings(uri);
        load(uri);
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] results) {
        super.onRequestPermissionsResult(requestCode, permissions, results);
        if (requestCode == REQ_PERM && !mUris.isEmpty()) {
            loadSiblings(mUris.get(mIndex));
            load(mUris.get(mIndex));
        }
    }

    /** All images of the same bucket, so the user can swipe through them. */
    private void loadSiblings(Uri uri) {
        if (!hasReadPermission()) {
            return;
        }
        try {
            long bucket = -1;
            try (Cursor c = getContentResolver().query(uri,
                    new String[]{MediaStore.Images.Media.BUCKET_ID}, null, null, null)) {
                if (c != null && c.moveToFirst()) {
                    bucket = c.getLong(0);
                }
            }
            if (bucket < 0) {
                return;
            }
            List<Uri> uris = new ArrayList<>();
            try (Cursor c = getContentResolver().query(
                    MediaStore.Images.Media.EXTERNAL_CONTENT_URI,
                    new String[]{MediaStore.MediaColumns._ID},
                    MediaStore.Images.Media.BUCKET_ID + "=?",
                    new String[]{String.valueOf(bucket)},
                    MediaStore.MediaColumns.DATE_ADDED + " ASC")) {
                if (c != null) {
                    while (c.moveToNext()) {
                        uris.add(ContentUris.withAppendedId(
                                MediaStore.Images.Media.EXTERNAL_CONTENT_URI, c.getLong(0)));
                    }
                }
            }
            int index = uris.indexOf(uri);
            if (index < 0) {
                return;
            }
            mUris.clear();
            mUris.addAll(uris);
            mIndex = index;
        } catch (Exception e) {
            Log.w(TAG, "loadSiblings failed", e);
        }
    }

    private void show(int index) {
        if (index < 0 || index >= mUris.size() || index == mIndex) {
            return;
        }
        mIndex = index;
        load(mUris.get(index));
    }

    private void load(final Uri uri) {
        mCurrent = uri;
        updateCounter();
        updateEditButton();
        mView.clear();
        new Thread(() -> {
            Bitmap bitmap = decode(uri);
            runOnUiThread(() -> {
                if (bitmap != null) {
                    mView.setPhoto(bitmap);
                    updateCounter();
                } else if (!mAskedForPermission && !hasReadPermission()) {
                    mAskedForPermission = true;
                    requestPermissions(readPermissions(), REQ_PERM);
                } else {
                    mCounter.setText("无法读取这张照片");
                    mCounter.setVisibility(View.VISIBLE);
                }
            });
        }, "photo-load").start();
    }

    private void updateCounter() {
        if (mUris.size() > 1) {
            mCounter.setText((mIndex + 1) + " / " + mUris.size());
            mCounter.setVisibility(View.VISIBLE);
        } else {
            mCounter.setText("");
        }
    }

    private Bitmap decode(Uri uri) {
        try {
            BitmapFactory.Options bounds = new BitmapFactory.Options();
            bounds.inJustDecodeBounds = true;
            try (InputStream in = getContentResolver().openInputStream(uri)) {
                if (in == null) {
                    return null;
                }
                BitmapFactory.decodeStream(in, null, bounds);
            }
            if (bounds.outWidth <= 0 || bounds.outHeight <= 0) {
                return null;
            }
            int sample = 1;
            while (Math.max(bounds.outWidth, bounds.outHeight) / (sample * 2) >= MAX_DIM) {
                sample *= 2;
            }
            BitmapFactory.Options opts = new BitmapFactory.Options();
            opts.inSampleSize = sample;
            try (InputStream in = getContentResolver().openInputStream(uri)) {
                return in == null ? null : BitmapFactory.decodeStream(in, null, opts);
            }
        } catch (Exception e) {
            Log.w(TAG, "decode failed for " + uri, e);
            return null;
        }
    }

    private void updateEditButton() {
        Intent edit = new Intent(Intent.ACTION_EDIT);
        edit.setDataAndType(mCurrent, "image/*");
        boolean resolvable = getPackageManager().resolveActivity(edit, 0) != null;
        mEdit.setVisibility(resolvable ? View.VISIBLE : View.GONE);
    }

    private void openEditor() {
        if (mCurrent == null) {
            return;
        }
        try {
            Intent edit = new Intent(Intent.ACTION_EDIT);
            edit.setDataAndType(mCurrent, "image/*");
            edit.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION
                    | Intent.FLAG_GRANT_WRITE_URI_PERMISSION);
            startActivity(edit);
        } catch (Exception e) {
            Log.w(TAG, "no editor for " + mCurrent, e);
        }
    }

    private boolean hasReadPermission() {
        for (String p : readPermissions()) {
            if (checkSelfPermission(p) != PackageManager.PERMISSION_GRANTED) {
                return false;
            }
        }
        return true;
    }

    private static String[] readPermissions() {
        if (Build.VERSION.SDK_INT >= 33) {
            return new String[]{Manifest.permission.READ_MEDIA_IMAGES};
        }
        return new String[]{Manifest.permission.READ_EXTERNAL_STORAGE};
    }

    /** ImageView that keeps a zoom/pan matrix on top of a fit-centre base. */
    private final class ZoomView extends ImageView implements View.OnTouchListener {
        interface OnSwipeListener {
            void onSwipeLeft();

            void onSwipeRight();

            void onTap();
        }

        private final Matrix mMatrix = new Matrix();
        private final Matrix mBase = new Matrix();
        private final float[] mValues = new float[9];
        private final ScaleGestureDetector mScaleDetector;
        private final GestureDetector mGestureDetector;
        private OnSwipeListener mListener;
        private float mBaseScale = 1f;
        private float mLastX;
        private float mLastY;

        ZoomView() {
            super(PhotoActivity.this);
            setScaleType(ImageView.ScaleType.MATRIX);
            setBackgroundColor(0xff000000);
            setOnTouchListener(this);
            mScaleDetector = new ScaleGestureDetector(PhotoActivity.this,
                    new ScaleGestureDetector.SimpleOnScaleGestureListener() {
                        @Override
                        public boolean onScale(ScaleGestureDetector detector) {
                            float current = scale();
                            float target = current * detector.getScaleFactor();
                            if (target < mBaseScale) {
                                target = mBaseScale;
                            } else if (target > mBaseScale * 8f) {
                                target = mBaseScale * 8f;
                            }
                            float factor = target / current;
                            mMatrix.postScale(factor, factor,
                                    detector.getFocusX(), detector.getFocusY());
                            clamp();
                            apply();
                            return true;
                        }
                    });
            mGestureDetector = new GestureDetector(PhotoActivity.this,
                    new GestureDetector.SimpleOnGestureListener() {
                        @Override
                        public boolean onDoubleTap(MotionEvent e) {
                            if (scale() > mBaseScale * 1.05f) {
                                mMatrix.set(mBase);
                            } else {
                                mMatrix.postScale(2.5f, 2.5f, e.getX(), e.getY());
                            }
                            clamp();
                            apply();
                            return true;
                        }

                        @Override
                        public boolean onFling(MotionEvent e1, MotionEvent e2, float vx, float vy) {
                            if (e1 == null || e2 == null || mListener == null) {
                                return false;
                            }
                            if (scale() > mBaseScale * 1.05f) {
                                return false;
                            }
                            float dx = e2.getX() - e1.getX();
                            float dy = e2.getY() - e1.getY();
                            if (Math.abs(dx) < 80 || Math.abs(dx) < Math.abs(dy) * 2) {
                                return false;
                            }
                            if (dx < 0) {
                                mListener.onSwipeLeft();
                            } else {
                                mListener.onSwipeRight();
                            }
                            return true;
                        }

                        @Override
                        public boolean onSingleTapConfirmed(MotionEvent e) {
                            if (mListener != null) {
                                mListener.onTap();
                            }
                            return true;
                        }
                    });
        }

        void setOnSwipeListener(OnSwipeListener listener) {
            mListener = listener;
        }

        void clear() {
            setImageDrawable(null);
        }

        void setPhoto(Bitmap bitmap) {
            setImageBitmap(bitmap);
            mMatrix.set(mBase);
            apply();
        }

        @Override
        protected void onSizeChanged(int w, int h, int oldw, int oldh) {
            super.onSizeChanged(w, h, oldw, oldh);
            updateBase();
        }

        @Override
        public void setImageBitmap(Bitmap bitmap) {
            super.setImageBitmap(bitmap);
            updateBase();
        }

        private void updateBase() {
            if (!(getDrawable() instanceof BitmapDrawable)) {
                return;
            }
            Bitmap bitmap = ((BitmapDrawable) getDrawable()).getBitmap();
            if (bitmap == null || getWidth() == 0 || getHeight() == 0) {
                return;
            }
            float sx = getWidth() / (float) bitmap.getWidth();
            float sy = getHeight() / (float) bitmap.getHeight();
            mBaseScale = Math.min(sx, sy);
            mBase.setScale(mBaseScale, mBaseScale);
            mBase.postTranslate((getWidth() - bitmap.getWidth() * mBaseScale) / 2f,
                    (getHeight() - bitmap.getHeight() * mBaseScale) / 2f);
            mMatrix.set(mBase);
            apply();
        }

        private void apply() {
            setImageMatrix(mMatrix);
            invalidate();
        }

        private void clamp() {
            if (!(getDrawable() instanceof BitmapDrawable)) {
                return;
            }
            Bitmap bitmap = ((BitmapDrawable) getDrawable()).getBitmap();
            if (bitmap == null) {
                return;
            }
            RectF r = new RectF(0, 0, bitmap.getWidth(), bitmap.getHeight());
            mMatrix.mapRect(r);
            float dx = 0;
            float dy = 0;
            if (r.width() <= getWidth()) {
                dx = (getWidth() - r.width()) / 2f - r.left;
            } else if (r.left > 0) {
                dx = -r.left;
            } else if (r.right < getWidth()) {
                dx = getWidth() - r.right;
            }
            if (r.height() <= getHeight()) {
                dy = (getHeight() - r.height()) / 2f - r.top;
            } else if (r.top > 0) {
                dy = -r.top;
            } else if (r.bottom < getHeight()) {
                dy = getHeight() - r.bottom;
            }
            mMatrix.postTranslate(dx, dy);
        }

        private float scale() {
            mMatrix.getValues(mValues);
            return mValues[Matrix.MSCALE_X];
        }

        @Override
        public boolean onTouch(View v, MotionEvent event) {
            mScaleDetector.onTouchEvent(event);
            mGestureDetector.onTouchEvent(event);
            switch (event.getActionMasked()) {
                case MotionEvent.ACTION_DOWN:
                    mLastX = event.getX();
                    mLastY = event.getY();
                    break;
                case MotionEvent.ACTION_MOVE:
                    if (!mScaleDetector.isInProgress() && scale() > mBaseScale * 1.01f) {
                        float x = event.getX();
                        float y = event.getY();
                        mMatrix.postTranslate(x - mLastX, y - mLastY);
                        clamp();
                        apply();
                        mLastX = x;
                        mLastY = y;
                    }
                    break;
                default:
                    break;
            }
            return true;
        }
    }
}
