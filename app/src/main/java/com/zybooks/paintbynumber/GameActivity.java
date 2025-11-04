package com.zybooks.paintbynumber;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.widget.TextView;
import androidx.appcompat.app.AppCompatActivity;

public class GameActivity extends AppCompatActivity {


    private static final String TAG = "GameActivity";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_game);


        Intent intent = getIntent();


        int imageId = intent.getIntExtra(ImageSelectionActivity.EXTRA_IMAGE_ID, -1);
        int gridSize = intent.getIntExtra(ImageSelectionActivity.EXTRA_GRID_SIZE, -1);
        int colorCount = intent.getIntExtra(ImageSelectionActivity.EXTRA_COLOR_COUNT, -1);

        Log.d(TAG, "Received Image ID: " + imageId);
        Log.d(TAG, "Received Grid Size: " + gridSize);
        Log.d(TAG, "Received Color Count: " + colorCount);


        TextView debugTextView = findViewById(R.id.debug_text_view);
        if (imageId != -1) {

            String debugText = "Image Resource ID: " + imageId +
                    "\nGrid Size: " + gridSize +
                    "\nNumber of Colors: " + colorCount;
            debugTextView.setText(debugText);
        } else {

            debugTextView.setText("Error: Could not retrieve game data.");
        }
    }
}
