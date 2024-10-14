package com.x_viria.app.light.corelens.core.callback;

import android.media.ImageReader;

public class ImageReaderCallback {
    interface Callback {
        public void onImageAvailable(ImageReader reader);
        public void saveToMediaStore(byte[] bytes);
    }

    public void onImageAvailable(ImageReader reader) {}

    public void saveToMediaStore(byte[] bytes) {}
}