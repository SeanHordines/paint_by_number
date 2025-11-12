package com.zybooks.paintbynumber;

import android.os.Bundle;
import android.widget.Button;
import androidx.appcompat.app.AppCompatActivity;

import com.ungcsci.paintbynumber.R;

public class GameActivity extends AppCompatActivity {

    private PaintView paintView;
    private Button toggleNumbersButton;
    private Button admireButton;
    private Button mainMenuButton;
    private Button shareButton;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_paint);
        //XML references
        paintView = findViewById(R.id.paintActivity);
        toggleNumbersButton = findViewById(R.id.toggleNumbersButton);
        admireButton = findViewById(R.id.admireButton);
        mainMenuButton = findViewById(R.id.mainMenuButton);
        shareButton = findViewById(R.id.admireButton);

        //PaintView Button Connection
        paintView.setAdmireButton(admireButton);
        paintView.setMainMenuButton(mainMenuButton);
        paintView.setShareButton(shareButton);

        //Toggle Numbers Button
        toggleNumbersButton.setOnClickListener(v -> paintView.toggleNumbers());
    }
}
