package com.ungcsci.paintbynumber;

import android.content.ContentResolver;
import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import java.io.FileNotFoundException;
import java.io.InputStream;
import java.util.Arrays;
import java.util.Random;

public class ImageProcessor {
    private final Context context;
    private Bitmap original_image;
    private Bitmap posterized_image;
    private int[] palette;
    private int[][] color_grid;

    public ImageProcessor(Context context, Uri uri, int logical_size, int num_colors) {
        this.context = context;

        loadImageFromURI(uri);
        posterizeImage(logical_size, num_colors);
    }

    public Bitmap getOriginalImage() {
        return original_image;
    }

    public Bitmap getPosterizedImage(int display_size) {
        if (original_image == null) return null;

        if (posterized_image != null) {
            if (display_size < posterized_image.getWidth()) return posterized_image;
            // scale up to the display size using nearest neighbor interpolation to preserve hard edges
            return Bitmap.createScaledBitmap(posterized_image, display_size, display_size, false);
        }

        return null;
    }

    public int[] getPalette() {
        if (posterized_image == null) return null;
        return palette;
    }

    public int[][] getColorGrid() {
        if (posterized_image == null) return null;
        if (color_grid != null) return color_grid;

        int width = posterized_image.getWidth();
        int height = posterized_image.getHeight();

        int[] pixels = new int[width*height];
        posterized_image.getPixels(pixels, 0, width, 0, 0, width, height);

        for (int x = 0; x < width; x++){
            for (int y = 0; y < height; y++){
                color_grid[x][y] = pixels[y * width + x];
            }
        }
        return color_grid;
    }

    private void loadImageFromURI(Uri uri) {
        original_image = null;
        ContentResolver contentResolver = context.getContentResolver();
        InputStream inputStream = null;

        try {
            inputStream = contentResolver.openInputStream(uri);
            if (inputStream == null) {return;}

            original_image = BitmapFactory.decodeStream(inputStream);
        } catch (FileNotFoundException e) {e.printStackTrace();}
    }

    private void posterizeImage(int logical_size, int num_colors) {
        // crop image to square
        Bitmap logical_image = cropToSquare(original_image);
        //denoise using bilateral
        //logical_image = denoiseBilateral(logical_image, 5, 1.5f, 2.5f);
        // scale to logical size using bilinear interpolation
        logical_image = Bitmap.createScaledBitmap(logical_image, logical_size, logical_size, true);
        // use k-means to identify best colors
        posterized_image = kMeansPaletteReduction(logical_image, num_colors);
    }

    private Bitmap cropToSquare(Bitmap source) {
        int width = source.getWidth();
        int height = source.getHeight();
        int size = Math.min(width, height);

        int x_offset = (width - size) / 2;
        int y_offset = (height - size) / 2;

        return Bitmap.createBitmap(source, x_offset, y_offset, size, size);
    }

    private Bitmap denoiseBilateral(Bitmap logical_image, int radius, float sigmaColor, float sigmaSpace) {
        int width = logical_image.getWidth();
        int height = logical_image.getHeight();
        int num_pixels = width * height;
        int r = (int) (radius / 2);

        Bitmap output_image = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888);

        for (int x = 0; x < width; x++) {
            for (int y = 0; y < width; y++) {
                int curr_pixel = logical_image.getPixel(x, y);

                for (int kx = -r; kx <= r; kx++) {
                    for (int ky = -r; ky <= r; ky++) {
                        if (x + kx < 0) continue;
                        if (y + ky < 0) continue;
                        if (x + kx >= width) continue;
                        if (y + ky >= height) continue;

                        int compare_pixel = logical_image.getPixel(x + kx, y + ky);
                    }
                }
            }
        }

        // get the neighbors in the radius
        // compute
        return null;
    }

    private Bitmap kMeansPaletteReduction(Bitmap logical_image, int num_colors) {
        int width = logical_image.getWidth();
        int height = logical_image.getHeight();
        int num_pixels = width * height;

        int[] pixels = new int[num_pixels];
        logical_image.getPixels(pixels, 0, width, 0, 0, width, height);

        int[][] color_data = new int[num_pixels][3];
        for (int i = 0; i < num_pixels; i++) {
            int color = pixels[i];
            color_data[i][0] = (color >> 16) & 0xFF; // red channel
            color_data[i][1] = (color >> 8) & 0xFF; // green channel
            color_data[i][2] = color & 0xFF; // blue channel
        }

        int[][] clusters = new int[num_colors][3];
        Random rand = new Random(314159265);
        for (int k = 0; k < num_colors; k++) {
            int random_color_index = rand.nextInt(num_pixels);
            clusters[k] = color_data[random_color_index].clone();
        }

        int[] best_cluster_indexes = new int[num_pixels];
        Arrays.fill(best_cluster_indexes, -1);

        int max_iterations = 20;
        boolean modified = true;
        for (int iteration = 0; iteration < max_iterations && modified; iteration++) {
            modified = false;

            for (int i = 0; i < num_pixels; i++){
                int min_distance = Integer.MAX_VALUE;
                int best_cluster_index = 0;

                for (int k = 0; k < num_colors; k++) {
                    int delta_red = color_data[i][0] - clusters[k][0];
                    int delta_green = color_data[i][1] - clusters[k][1];
                    int delta_blue = color_data[i][2] - clusters[k][2];

                    int current_distance =
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

            int[][] new_clusters = new int[num_colors][3];
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

        palette = new int[num_colors];
        for (int k = 0; k < num_colors; k++) {
            int red = (int) clusters[k][0];
            int green = (int) clusters[k][1];
            int blue = (int) clusters[k][2];
            palette[k] = (0xFF << 24) + (red << 16) + (green << 8) + (blue);
        }

        int[] new_pixels = new int[num_pixels];
        for (int i = 0; i < num_pixels; i++) {
            new_pixels[i] = palette[best_cluster_indexes[i]];
        }

        Bitmap color_reduced_image = Bitmap.createBitmap(width, height, logical_image.getConfig());
        color_reduced_image.setPixels(new_pixels, 0, width, 0, 0, width, height);
        return color_reduced_image;
    }
}
