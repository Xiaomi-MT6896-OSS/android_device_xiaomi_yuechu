package com.miui.gallery.activity;

import android.content.ContentResolver;
import android.database.Cursor;
import android.net.Uri;
import android.provider.OpenableColumns;
import android.util.Log;

/** Small helper shared by the stub activities. */
final class FileNames {
    private static final String TAG = "MiuiGalleryStub";

    private FileNames() {
    }

    static String displayName(ContentResolver resolver, Uri uri) {
        try (Cursor c = resolver.query(uri, new String[]{OpenableColumns.DISPLAY_NAME},
                null, null, null)) {
            if (c != null && c.moveToFirst()) {
                return c.getString(0);
            }
        } catch (Exception e) {
            Log.w(TAG, "cannot query " + uri, e);
        }
        return null;
    }
}
