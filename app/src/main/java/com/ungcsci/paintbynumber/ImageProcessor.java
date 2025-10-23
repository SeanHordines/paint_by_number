package com.ungcsci.paintbynumber;

import android.content.ContentResolver;
import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;

import java.io.FileNotFoundException;
import java.io.InputStream;
import java.lang.reflect.Array;

public class ImageProcessor {
    private Context context;
    private Bitmap image;
    private Bitmap posterized_image;
    int[][] pixel_grid;
    private int[] palette;

    public void imageProcessor(Context context) {
        this.context = context;
    }

    public boolean loadImageFromURI(Uri uri) {
        image = null;
        ContentResolver contentResolver = context.getContentResolver();
        InputStream inputStream = null;

        try {
            inputStream = contentResolver.openInputStream(uri);
            if (inputStream == null) {return false;}

            image = BitmapFactory.decodeStream(inputStream)
        } catch (FileNotFoundException e) {e.printStackTrace();}

        return true;
    }

    public Bitmap getOriginalImage() {
        return image;
    }

    public Bitmap getPosterizedImage() {
        return posterized_image;
    }

    public int[][] getPixelGrid() {

    }

    public int[] getPalette() {
        return palette;
    }

    public void posterizeImage(int logical_size, int num_colors, int display_size) {
        // crop image to square
        Bitmap temp_image = cropToSquare(image);
        // scale to logical size using cubic
        // use k-means to identify best colors
        // replace pixels with nearest color from palette
        // scale to display size with nearest neighbor
    }

    private Bitmap cropToSquare(Bitmap source) {
        int width = source.getWidth();
        int height = source.getHeight();
        int size = Math.min(width, height);

        int x_offset = (width - size) / 2;
        int y_offset = (height - size) / 2;

        return Bitmap.createBitmap(source, x_offset, y_offset, size, size);
    }
}
