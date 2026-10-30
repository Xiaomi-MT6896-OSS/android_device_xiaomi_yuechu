package com.miui.gallery.activity;

import android.app.Activity;
import android.content.ComponentName;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;

public class HomePageActivity extends Activity {
    private static final String TAG = "MiuiGalleryStub";
    private static final ComponentName GALLERY = new ComponentName(
            "com.android.gallery3d", "com.android.gallery3d.app.GalleryActivity");

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (launchLineageGallery()) {
            return;
        }
        Intent intent = getIntent();
        if (Intent.ACTION_VIEW.equals(intent.getAction()) && intent.getData() != null) {
            showImageFallback(intent);
        } else {
            showEmpty();
        }
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
