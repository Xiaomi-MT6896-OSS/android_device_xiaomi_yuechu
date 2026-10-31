package com.miui.gallery.activity;

import android.app.Activity;
import android.content.ContentUris;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.provider.MediaStore;
import android.util.Log;

/**
 * Entry point that replaces com.miui.gallery for MiuiCamera.
 *
 * The camera sends a review intent (ACTION_VIEW with a content:// URI, or
 * ACTION_MAIN + setPackage("com.miui.gallery") for the "open gallery" button).
 * This stub only *views*: photos go to PhotoActivity, videos to VideoActivity.
 * Editing/cropping still lives in Gallery2's exported FilterShowActivity,
 * CropActivity and TrimVideo.
 */
public class HomePageActivity extends Activity {
    private static final String TAG = "MiuiGalleryStub";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Intent intent = getIntent();
        Uri uri = intent != null ? intent.getData() : null;

        if (uri == null) {
            uri = latestImage();
        }
        if (uri == null) {
            Log.w(TAG, "no media to show");
            finish();
            return;
        }

        String type = intent != null ? intent.getType() : null;
        if (type == null) {
            type = getContentResolver().getType(uri);
        }
        boolean isVideo = type != null && type.startsWith("video/");

        Intent next = new Intent(this, isVideo ? VideoActivity.class : PhotoActivity.class);
        next.setData(uri);
        if (isVideo) {
            next.putExtra(VideoActivity.EXTRA_SPEED, isSlowMotion(uri) ? 0.25f : 1.0f);
        }
        startActivity(next);
        finish();
    }

    /** Newest photo on the device, used when the camera asks for the gallery home. */
    private Uri latestImage() {
        try (android.database.Cursor c = getContentResolver().query(
                MediaStore.Images.Media.EXTERNAL_CONTENT_URI,
                new String[]{MediaStore.MediaColumns._ID},
                null, null,
                MediaStore.MediaColumns.DATE_ADDED + " DESC")) {
            if (c != null && c.moveToFirst()) {
                return ContentUris.withAppendedId(
                        MediaStore.Images.Media.EXTERNAL_CONTENT_URI, c.getLong(0));
            }
        } catch (Exception e) {
            Log.w(TAG, "latestImage failed", e);
        }
        return null;
    }

    private boolean isSlowMotion(Uri uri) {
        String name = FileNames.displayName(getContentResolver(), uri);
        return name != null && name.contains("HSR_120");
    }
}
