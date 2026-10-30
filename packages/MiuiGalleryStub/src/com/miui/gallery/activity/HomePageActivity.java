package com.miui.gallery.activity;

import android.app.Activity;
import android.content.ComponentName;
import android.content.Intent;
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.provider.OpenableColumns;
import android.util.Log;

public class HomePageActivity extends Activity {
    private static final String TAG = "MiuiGalleryStub";
    private static final ComponentName GALLERY = new ComponentName(
            "com.android.gallery3d", "com.android.gallery3d.app.GalleryActivity");

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Intent intent = getIntent();
        if (isSlowMotionVideo(intent)) {
            startActivity(new Intent(intent)
                    .setClass(this, SlowMotionActivity.class)
                    .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK));
            finish();
            return;
        }
        if (launchLineageGallery()) {
            return;
        }
        if (Intent.ACTION_VIEW.equals(intent.getAction()) && intent.getData() != null) {
            showImageFallback(intent);
        } else {
            showEmpty();
        }
    }

    private boolean isSlowMotionVideo(Intent intent) {
        Uri uri = intent.getData();
        if (uri == null) return false;
        String type = intent.getType();
        if (type == null || !type.startsWith("video/")) return false;
        try (Cursor c = getContentResolver().query(uri,
                new String[]{OpenableColumns.DISPLAY_NAME}, null, null, null)) {
            if (c != null && c.moveToFirst()) {
                String name = c.getString(0);
                return name != null && name.contains("HSR_120");
            }
        } catch (Exception e) {
            Log.w(TAG, "cannot query display name", e);
        }
        return false;
    }

    private boolean launchLineageGallery() {
        try {
            Intent target = new Intent(getIntent());
            target.setComponent(GALLERY);
            target.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            startActivity(target);
            finish();
            return true;
        } catch (Exception e) {
            Log.w(TAG, "cannot launch Lineage Gallery", e);
            return false;
        }
    }

    private void showImageFallback(Intent intent) {
        android.widget.ImageView view = new android.widget.ImageView(this);
        view.setScaleType(android.widget.ImageView.ScaleType.FIT_CENTER);
        try {
            view.setImageBitmap(getContentResolver().loadThumbnail(
                    intent.getData(), new android.util.Size(1080, 1920), null));
        } catch (Exception e) {
            view.setImageURI(intent.getData());
        }
        setContentView(view);
    }

    private void showEmpty() {
        android.widget.TextView view = new android.widget.TextView(this);
        view.setText("Gallery unavailable");
        view.setGravity(android.view.Gravity.CENTER);
        setContentView(view);
    }
}
