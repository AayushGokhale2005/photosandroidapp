package com.example.photos;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;

import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;

public final class ImageLoader {

    private ImageLoader() {}

    public static Bitmap loadScaled(Context context, String uriString, int maxDim) {
        try {
            BitmapFactory.Options bounds = new BitmapFactory.Options();
            bounds.inJustDecodeBounds = true;
            try (InputStream is = open(context, uriString)) {
                if (is == null) return null;
                BitmapFactory.decodeStream(is, null, bounds);
            }

            int inSample = 1;
            while (bounds.outWidth / inSample > maxDim || bounds.outHeight / inSample > maxDim) {
                inSample *= 2;
            }

            BitmapFactory.Options opts = new BitmapFactory.Options();
            opts.inSampleSize = inSample;
            try (InputStream is2 = open(context, uriString)) {
                if (is2 == null) return null;
                return BitmapFactory.decodeStream(is2, null, opts);
            }
        } catch (IOException | SecurityException e) {
            return null;
        }
    }

    private static InputStream open(Context context, String uriString) throws IOException {
        Uri uri = Uri.parse(uriString);
        if ("file".equals(uri.getScheme())) {
            return new FileInputStream(uri.getPath());
        }
        return context.getContentResolver().openInputStream(uri);
    }
}
