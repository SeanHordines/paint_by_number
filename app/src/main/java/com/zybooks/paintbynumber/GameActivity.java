package com.zybooks.paintbynumber;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.widget.TextView; // Import the TextView class
import androidx.appcompat.app.AppCompatActivity;

public class GameActivity extends AppCompatActivity {

    // logging for debug messages
    private static final String TAG = "GameActivity";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_game); // connects to activity_game.xml layout

        //  intent that was used to start activity
        Intent intent = getIntent();

        // get the data sent from MainActivity.
        // second parameter is a default value if data isn't found.
        int imageId = intent.getIntExtra(MainActivity.EXTRA_IMAGE_ID, -1);
        int gridSize = intent.getIntExtra(MainActivity.EXTRA_GRID_SIZE, -1);
        int colorCount = intent.getIntExtra(MainActivity.EXTRA_COLOR_COUNT, -1);

        // log values to the "Logcat" window in Android Studio to confirm they arrived.

        Log.d(TAG, "Received Image ID: " + imageId);
        Log.d(TAG, "Received Grid Size: " + gridSize);
        Log.d(TAG, "Received Color Count: " + colorCount);


        TextView debugTextView = findViewById(R.id.debug_text_view);
        if (imageId != -1) {
            // if imageId found, display info.
            String debugText = "Image Resource ID: " + imageId +
                    "\nGrid Size: " + gridSize +
                    "\nNumber of Colors: " + colorCount;
            debugTextView.setText(debugText);
        } else {
            //no imageId was found, show an error.
            debugTextView.setText("Error: Could not retrieve game data.");
        }
    }
}
