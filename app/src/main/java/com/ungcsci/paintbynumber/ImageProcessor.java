package com.ungcsci.paintbynumber;

import android.content.ContentResolver;
import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.util.Arrays;
import java.util.Random;

public class ImageProcessor {
    private final Context context;
    private final Bitmap original_image;
    private Bitmap posterized_image;
    private int[] palette;
    private int[][] color_grid;

    public ImageProcessor(Context context, Bitmap image, int logical_size, int num_colors) {
        this.context = context;

        original_image = image;
        posterizeImage(logical_size, num_colors);
    }

    public Bitmap getOriginalImage() {
        return original_image;
    }

    public Bitmap getPosterizedImage(int display_size) {
        if (original_image == null) return null;

        if (posterized_image != null) {
            if (display_size < posterized_image.getWidth()) return posterized_image;
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
        color_grid = new int[width][height];

        for (int x = 0; x < width; x++){
            for (int y = 0; y < height; y++){
                color_grid[x][y] = pixels[y * width + x];
            }
        }
        return color_grid;
    }

    private void posterizeImage(int logical_size, int num_colors) {
        Bitmap logical_image = cropToSquare(original_image);
//        logical_image = denoiseBilateral(logical_image, 5, 5);
        logical_image = Bitmap.createScaledBitmap(logical_image, logical_size, logical_size, true);
        posterized_image = kMeansPaletteReduction(logical_image, num_colors);
//        posterized_image = logical_image;
    }

    private Bitmap cropToSquare(Bitmap source) {
        int width = source.getWidth();
        int height = source.getHeight();
        int size = Math.min(width, height);

        int x_offset = (width - size) / 2;
        int y_offset = (height - size) / 2;

        return Bitmap.createBitmap(source, x_offset, y_offset, size, size);
    }

    private Bitmap denoiseBilateral(Bitmap logical_image, int spatial_radius, int color_radius) {
        int width = logical_image.getWidth();
        int height = logical_image.getHeight();
        int num_pixels = width * height;

        Bitmap output_image = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888);

        for (int x = 0; x < width; x++) {
            for (int y = 0; y < height; y++) {
                int curr_pixel = logical_image.getPixel(x, y);
                int curr_red = (curr_pixel >> 16) & 0xFF;
                int curr_green = (curr_pixel >> 8) & 0xFF;
                int curr_blue = curr_pixel & 0xFF;

                float total_weight = 0;
                float weighted_sum_red = 0;
                float weighted_sum_green = 0;
                float weighted_sum_blue = 0;

                for (int dx = -spatial_radius; dx <= spatial_radius; dx++) {
                    for (int dy = -spatial_radius; dy <= spatial_radius; dy++) {
                        if (x + dx < 0) continue;
                        if (y + dy < 0) continue;
                        if (x + dx >= width) continue;
                        if (y + dy >= height) continue;

                        int compare_pixel = logical_image.getPixel(x + dx, y + dy);
                        int compare_red = (compare_pixel >> 16) & 0xFF;
                        int compare_green = (compare_pixel >> 8) & 0xFF;
                        int compare_blue = compare_pixel & 0xFF;

                        int dist = (dx * dx) + (dy * dy);
                        float spatial_weight = Math.max(0, 1 - (dist / (float) (spatial_radius * spatial_radius)));

                        int dr = curr_red - compare_red;
                        int dg = curr_green - compare_green;
                        int db = curr_blue - compare_blue;

                        int color_dist = (dr * dr) + (dg * dg) + (db * db);
                        float color_weight = Math.max(0, 1 - (color_dist / (float) (color_radius * color_radius)));

                        float combined_weight = spatial_weight * color_weight;
                        total_weight += combined_weight;
                        weighted_sum_red += combined_weight * compare_red;
                        weighted_sum_green += combined_weight * compare_green;
                        weighted_sum_blue += combined_weight * compare_blue;
                    }
                }
                curr_red = (int) (weighted_sum_red / total_weight);
                curr_green = (int) (weighted_sum_green / total_weight);
                curr_blue = (int) (weighted_sum_blue / total_weight);

                int new_color = (0xFF << 24) + (curr_red << 16) + (curr_green << 8) + (curr_blue);
                output_image.setPixel(x, y, new_color);
            }
        }
        return output_image;
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