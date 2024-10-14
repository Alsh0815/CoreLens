package com.x_viria.app.light.corelens.core;

import android.content.ContentUris;
import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.net.Uri;
import android.provider.MediaStore;

import java.io.IOException;
import java.io.OutputStream;

public class Media {

    private final Context CONTEXT;

    public Media(Context context) {
        this.CONTEXT = context;
    }

    public Uri getLastTakenImageUri() {
        String[] projection = { MediaStore.Images.Media._ID, MediaStore.Images.Media.DATE_TAKEN };
        String sortOrder = MediaStore.Images.Media.DATE_TAKEN + " DESC";

        try (Cursor cursor = CONTEXT.getContentResolver().query(
                MediaStore.Images.Media.EXTERNAL_CONTENT_URI,
                projection,
                null,
                null,
                sortOrder
        )) {
            if (cursor != null && cursor.moveToFirst()) {
                int idColumn = cursor.getColumnIndexOrThrow(MediaStore.Images.Media._ID);
                long id = cursor.getLong(idColumn);

                return ContentUris.withAppendedId(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, id);
            }
        }
        return null;
    }

    public void savePicture(byte[] bytes) {
        ContentValues values = new ContentValues();
        values.put(MediaStore.Images.Media.DISPLAY_NAME, "IMG_" + System.currentTimeMillis() + ".jpg");
        values.put(MediaStore.Images.Media.MIME_TYPE, "image/jpeg");
        values.put(MediaStore.Images.Media.RELATIVE_PATH, "DCIM/Core Lens");

        Uri uri = CONTEXT.getContentResolver().insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, values);
        try {
            assert uri != null;
            try (OutputStream outputStream = CONTEXT.getContentResolver().openOutputStream(uri)) {
                assert outputStream != null;
                outputStream.write(bytes);
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

}
