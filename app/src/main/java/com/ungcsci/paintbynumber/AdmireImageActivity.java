package com.ungcsci.paintbynumber;

import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.Bundle;
import android.provider.MediaStore;
import android.widget.ImageView;
import androidx.appcompat.app.AppCompatActivity;

import java.io.IOException;

public class AdmireImageActivity extends AppCompatActivity {
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        ImageView imageView = new ImageView(this);

        Intent intent = getIntent();
        String imageUriString = intent.getStringExtra(DefaultImageActivity.EXTRA_IMAGE_URI);

        Bitmap bitmap = null;

        Uri imageUri = Uri.parse(imageUriString);
        try {
            bitmap = MediaStore.Images.Media.getBitmap(getContentResolver(), imageUri);
        } catch (IOException e) {e.printStackTrace();}

        imageView.setImageBitmap(bitmap);
        setContentView(imageView);
    }
}
