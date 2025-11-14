package com.ungcsci.paintbynumber;

import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.Bundle;
import android.provider.MediaStore;
import android.widget.Button;
import android.widget.ImageButton;

import androidx.appcompat.app.AppCompatActivity;

import java.io.IOException;

public class GameActivity extends AppCompatActivity {
    private PaintView paintView;
    private Button toggleNumbersButton;
    private Button admireButton;
    private Button mainMenuButton;
    private Button shareButton;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_game);

        Intent intent = getIntent();
        String imageUriString = intent.getStringExtra(DefaultImageActivity.EXTRA_IMAGE_URI);
        int imageId = intent.getIntExtra(DefaultImageActivity.EXTRA_IMAGE_ID, -1);
        int gridSize = intent.getIntExtra(DefaultImageActivity.EXTRA_GRID_SIZE, -1);
        int colorCount = intent.getIntExtra(DefaultImageActivity.EXTRA_COLOR_COUNT, -1);

        Bitmap bitmap = null;

        if (imageUriString != null) {
            Uri imageUri = Uri.parse(imageUriString);
            try {
                bitmap = MediaStore.Images.Media.getBitmap(getContentResolver(), imageUri);
            } catch (IOException e) {e.printStackTrace();}
        }
        else if (imageId != -1) {
            bitmap = BitmapFactory.decodeResource(getResources(), imageId);
        }

        ImageProcessor ip = new ImageProcessor(GameActivity.this, bitmap, gridSize, colorCount);

        int[] palette = ip.getPalette();
        int[][] grid = ip.getColorGrid();
        Bitmap posterized_image = ip.getPosterizedImage(512);

        paintView = findViewById(R.id.paintActivity);
        paintView.loadImageData(gridSize, palette, grid, posterized_image);

        //XML references
        toggleNumbersButton = findViewById(R.id.toggleNumbersButton);
        admireButton = findViewById(R.id.admireButton);
        mainMenuButton = findViewById(R.id.mainMenuButton);
        shareButton = findViewById(R.id.shareButton);
        ImageButton drawModeButton = findViewById(R.id.drawModeButton);
        ImageButton zoomModeButton = findViewById(R.id.zoomModeButton);



        //PaintView Button Connection
        paintView.setAdmireButton(admireButton, this);
        paintView.setMainMenuButton(mainMenuButton);
        paintView.setShareButton(shareButton);
        paintView.setDrawModeButton(drawModeButton);
        paintView.setZoomModeButton(zoomModeButton);


        //Toggle Numbers Button
        toggleNumbersButton.setOnClickListener(v -> paintView.toggleNumbers());
    }
}