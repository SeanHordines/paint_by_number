package com.ungcsci.paintbynumber;

import android.annotation.SuppressLint;
import android.content.ContentResolver;
import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Color;
import android.net.Uri;
import android.os.Build;
import androidx.annotation.RequiresApi;
import java.io.FileNotFoundException;
import java.io.InputStream;
import java.util.Random;

public class ImageProcessor {
    private final Context context;
    private Bitmap original_image;
    private Bitmap posterized_image;
    private Color[] palette;
    private Color[][] color_grid;

    public ImageProcessor(Context context) {
        this.context = context;
    }

    public boolean loadImageFromURI(Uri uri) {
        original_image = null;
        ContentResolver contentResolver = context.getContentResolver();
        InputStream inputStream = null;

        try {
            inputStream = contentResolver.openInputStream(uri);
            if (inputStream == null) {return false;}

            original_image = BitmapFactory.decodeStream(inputStream);
        } catch (FileNotFoundException e) {e.printStackTrace();}

        return original_image != null;
    }

    @RequiresApi(api = Build.VERSION_CODES.O)
    public boolean loadImageFromURI(Uri uri, int logical_size, int num_colors) {
        loadImageFromURI(uri);
        if (original_image == null) return false;

        posterizeImage(logical_size, num_colors);
        return posterized_image != null;
    }

    public Bitmap getOriginalImage() {
        return original_image;
    }

    public Bitmap getPosterizedImage(int display_size) {
        if (posterized_image == null) return null;
        return Bitmap.createScaledBitmap(posterized_image, display_size, display_size, false);
    }

    public Color[] getPalette() {
        return palette;
    }

    @RequiresApi(api = Build.VERSION_CODES.O)
    private void posterizeImage(int logical_size, int num_colors) {
        // crop image to square
        Bitmap logical_image = cropToSquare(original_image);
        // scale to logical sBitmap logical_imageize using cubic
        logical_image = Bitmap.createScaledBitmap(logical_image, logical_size, logical_size, true);
        // use k-means to identify best colors
        kMeansPaletteReduction(logical_image, num_colors);
    }

    private Bitmap cropToSquare(Bitmap source) {
        int width = source.getWidth();
        int height = source.getHeight();
        int size = Math.min(width, height);

        int x_offset = (width - size) / 2;
        int y_offset = (height - size) / 2;

        return Bitmap.createBitmap(source, x_offset, y_offset, size, size);
    }

    @RequiresApi(api = Build.VERSION_CODES.O)
    private void kMeansPaletteReduction(Bitmap logical_image, int num_colors) {
        int width = logical_image.getWidth();
        int height = logical_image.getHeight();
        int num_pixels = width*height;

        int[] pixels = new int[num_pixels];
        logical_image.getPixels(pixels, 0, width, 0, 0, width, height);

        float[][] color_data = new float[num_pixels][3];
        for (int i = 0; i < num_pixels; i++) {
            int color = pixels[i];
            color_data[i][0] = (color >> 16) & 0xFF; // red channel
            color_data[i][1] = (color >> 8) & 0xFF; // green channel
            color_data[i][2] = color & 0xFF; // blue channel
        }

        float[][] clusters = new float[num_colors][3];
        Random rand = new Random(314159265);
        for (int k = 0; k < num_colors; k++) {
            int random_color_index = rand.nextInt(num_pixels);
            clusters[k] = color_data[random_color_index].clone();
        }

        int[] best_cluster_indexes = new int[num_pixels];

        int max_iterations = 20;
        boolean modified = true;
        for (int iteration = 0; iteration < max_iterations && modified; iteration++) {
            modified = false;

            for (int i = 0; i < num_pixels; i++){
                float min_distance = Float.MAX_VALUE;
                int best_cluster_index = 0;

                for (int k = 0; k < num_colors; k++) {
                    float delta_red = color_data[i][0] - clusters[k][0];
                    float delta_green = color_data[i][1] - clusters[k][1];
                    float delta_blue = color_data[i][2] - clusters[k][2];

                    float current_distance =
                            (delta_red * delta_red) +
                            (delta_green * delta_green) +
                            (delta_blue * delta_blue);

                    if (current_distance < min_distance){
                        min_distance = current_distance;
                        best_cluster_index = k;
                    }
                }

                if (best_cluster_indexes[i] != best_cluster_index) {
                    best_cluster_indexes[i] = best_cluster_index;
                    modified = true;
                }
            }

            float[][] new_clusters = new float[num_colors][3];
            int[] cluster_counts = new int[num_colors];

            for (int i = 0; i < num_pixels; i++) {
                int k = best_cluster_indexes[i];
                new_clusters[k][0] += color_data[i][0];
                new_clusters[k][1] += color_data[i][1];
                new_clusters[k][2] += color_data[i][2];
                cluster_counts[k]++;
            }

            for (int k = 0; k < num_colors; k++) {
                if (cluster_counts[k] > 0) {
                    clusters[k][0] = new_clusters[k][0] / cluster_counts[k];
                    clusters[k][1] = new_clusters[k][1] / cluster_counts[k];
                    clusters[k][2] = new_clusters[k][2] / cluster_counts[k];
                } else {
                    int random_color_index = rand.nextInt(num_pixels);
                    clusters[k] = color_data[random_color_index].clone();
                }
            }
        }

        palette = new Color[num_colors];
        for (int k = 0; k < num_colors; k++) {
            float red = clusters[k][0] / 255f;
            float green = clusters[k][1] / 255f;
            float blue = clusters[k][2] / 255f;
            palette[k] = Color.valueOf(red, green, blue);
        }

        int[] new_pixels = new int[num_pixels];
        for (int i = 0; i < num_pixels; i++) {
            int palette_index = best_cluster_indexes[i];
            Color new_color = palette[palette_index];
            new_pixels[i] = new_color.toArgb();
        }

        posterized_image = Bitmap.createBitmap(width, height, logical_image.getConfig());
        posterized_image.setPixels(new_pixels, 0, width, 0, 0, width, height);
    }
}
