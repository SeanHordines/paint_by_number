package com.ungcsci.paintbynumber;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

public class MainMenuActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_main_menu);

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });
    }

    //Opens HowToPlayActivity
    public void onHowToPlayButtonClick(View view) {
        Intent intent = new Intent(this, HowToPlayActivity.class);
        startActivity(intent);
    }


    //Opens DefaultActivity
    public void onDefaultActivityClick(View view) {
        Intent intent = new Intent(this, DefaultImageActivity.class);
        startActivity(intent);
    }

    // Opens CustomActivity
    public void onCustomActivityClick(View view) {
        Intent intent = new Intent(this, CustomImageActivity.class);
        startActivity(intent);
    }
}