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
        Arrays.sort(palette);
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
//        posterized_image = kMeansPaletteReductionRGB(logical_image, num_colors);
        posterized_image = kMeansPaletteReductionLAB(logical_image, num_colors);
//        posterized_image = logical_image;
    }

    private float labF(float t){
        if (t > 0.008856f) {
            return (float) Math.pow(t, (1f / 3f));
        }
        else {
            t *= 7.787f;
            t += (16f/116f);
            return t;
        }
    }
    private float[] rgb2lab(int red, int green, int blue) {
        // normalize
        float r = red / 255f;
        float g = green / 255f;
        float b = blue / 255f;

        // apply gamma correction
        if (r > 0.04045f) {
            r = (float) Math.pow((r + 0.055f) / 1.055f, 2.4f);
        } else {r /= 12.92f;}

        if (g > 0.04045f) {
            g = (float) Math.pow((g + 0.055f) / 1.055f, 2.4f);
        } else {g /= 12.92f;}

        if (b > 0.04045f) {
            b = (float) Math.pow((b + 0.055f) / 1.055f, 2.4f);
        } else {b /= 12.92f;}

        // convert to intermediate XYZ color space
        float x = (r * 0.4124564f) + (g * 0.3575761f) + (b * 0.1804375f);
        float y = (r * 0.2126729f) + (g * 0.7151522f) + (b * 0.0721750f);
        float z = (r * 0.0193339f) + (g * 0.1191920f) + (b * 0.9503041f);

        // reference white (D65)
        float x_n = 0.95047f;
        float y_n = 1.00000f;
        float z_n = 1.08883f;

        // convert XYZ to lab
        float L = 116f * labF(y / y_n) - 16f;
        float A = 500f * (labF(x / x_n) - labF(y / y_n));
        float B = 200f * (labF(y / y_n) - labF(z / z_n));

        return new float[]{L, A, B};
    }

    private float labInvF(float t) {
        float t3 = t * t * t;
        if (t3 > 0.008856f) {
            return t3;
        } else {
            return (t - 16f / 116f) / 7.787f;
        }
    }

    private int[] lab2rgb(float L, float A, float B) {

        // reference white (D65)
        float Xn = 0.95047f;
        float Yn = 1.00000f;
        float Zn = 1.08883f;

        // convert LAB to XYZ
        float fy = (L + 16f) / 116f;
        float fx = A / 500f + fy;
        float fz = fy - B / 200f;

        float x = Xn * labInvF(fx);
        float y = Yn * labInvF(fy);
        float z = Zn * labInvF(fz);

        // convert XYZ to RGB
        float r =  3.2404542f * x - 1.5371385f * y - 0.4985314f * z;
        float g = -0.9692660f * x + 1.8760108f * y + 0.0415560f * z;
        float b =  0.0556434f * x - 0.2040259f * y + 1.0572252f * z;

        // gamma correction
        r = (r > 0.0031308f)
                ? (1.055f * (float)Math.pow(r, 1f / 2.4f) - 0.055f)
                : (12.92f * r);

        g = (g > 0.0031308f)
                ? (1.055f * (float)Math.pow(g, 1f / 2.4f) - 0.055f)
                : (12.92f * g);

        b = (b > 0.0031308f)
                ? (1.055f * (float)Math.pow(b, 1f / 2.4f) - 0.055f)
                : (12.92f * b);

        // clamp to [0, 255]
        int red = Math.max(0, Math.min(255, Math.round(r * 255f)));
        int green = Math.max(0, Math.min(255, Math.round(g * 255f)));
        int blue = Math.max(0, Math.min(255, Math.round(b * 255f)));

        return new int[]{red, green, blue};
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

    private Bitmap kMeansPaletteReductionRGB(Bitmap logical_image, int num_colors) {
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
    private Bitmap kMeansPaletteReductionLAB(Bitmap logical_image, int num_colors) {
        int width = logical_image.getWidth();
        int height = logical_image.getHeight();
        int num_pixels = width * height;

        int[] pixels = new int[num_pixels];
        logical_image.getPixels(pixels, 0, width, 0, 0, width, height);

        float[][] color_data = new float[num_pixels][3];
        for (int i = 0; i < num_pixels; i++) {
            int color = pixels[i];
            float[] lab = rgb2lab(
                    (color >> 16) & 0xFF,
                    (color >> 8) & 0xFF,
                    color & 0xFF);
            color_data[i][0] = lab[0];
            color_data[i][1] = lab[1];
            color_data[i][2] = lab[2];
        }

        float[][] clusters = new float[num_colors][3];
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
                float min_distance = Float.MAX_VALUE;
                int best_cluster_index = 0;

                for (int k = 0; k < num_colors; k++) {
                    float delta_L = color_data[i][0] - clusters[k][0];
                    float delta_A = color_data[i][1] - clusters[k][1];
                    float delta_B = color_data[i][2] - clusters[k][2];

                    float current_distance =
                            (delta_L * delta_L) +
                                    (delta_A * delta_A) +
                                    (delta_B * delta_B);

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

        palette = new int[num_colors];
        for (int k = 0; k < num_colors; k++) {
            int[] rgb = lab2rgb(
                    clusters[k][0],
                    clusters[k][1],
                    clusters[k][2]
            );
            int red = rgb[0];
            int green = rgb[1];
            int blue = rgb[2];
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