package com.ungcsci.paintbynumber;

import android.annotation.SuppressLint;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.Bundle;
import androidx.appcompat.app.AppCompatActivity;

import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;

public class TestImageProcessing extends AppCompatActivity{
    private static final String TEST_IMAGE_PATH = "C:\\Sean\\programmingshit\\paint_by_number\\app\\src\\main\\res\\drawable\\scream4.jpg";

    @SuppressLint("WrongThread")
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        try {
            // --- Load image from file path ---
            Bitmap original_image = BitmapFactory.decodeFile(TEST_IMAGE_PATH);
            if (original_image == null) {
                throw new RuntimeException("Could not decode input image. Check the path.");
            }

            ImageProcessor ip = new ImageProcessor(TestImageProcessing.this, original_image, 32, 8);
            Bitmap new_image = ip.getPosterizedImage(512);

            File input = new File(TEST_IMAGE_PATH);
            String name = input.getName();
            int dotIndex = name.lastIndexOf(".");
            String outName = (dotIndex > 0)
                    ? name.substring(0, dotIndex) + "_posterized" + name.substring(dotIndex)
                    : name + "_posterized";

            File output = new File(input.getParent(), outName);

            // --- Save the result ---
            FileOutputStream fos = new FileOutputStream(output);
            new_image.compress(Bitmap.CompressFormat.JPEG, 100, fos);
            fos.flush();
            fos.close();

            System.out.println("Posterized image saved to: " + output.getAbsolutePath());

        } catch (Exception e) {
            e.printStackTrace();
        }

        finish(); // close activity once done
    }
}
