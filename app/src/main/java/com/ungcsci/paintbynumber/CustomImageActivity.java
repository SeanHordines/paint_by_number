package com.ungcsci.paintbynumber;

import android.annotation.SuppressLint;
import android.content.DialogInterface;
import android.content.Intent;
import android.graphics.Bitmap;
import android.net.Uri;
import android.os.Bundle;
import android.provider.MediaStore;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;
import androidx.activity.EdgeToEdge;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.google.android.material.slider.Slider;

import java.io.IOException;

public class CustomImageActivity extends AppCompatActivity {

    private ImageView customImageView;
    private Uri selectedImageUri = null;

    private final ActivityResultLauncher<Intent> imagePickerLauncher =
            registerForActivityResult(new ActivityResultContracts.StartActivityForResult(), result -> {
                if (result.getResultCode() == RESULT_OK && result.getData() != null) {
                    Intent data = result.getData();
                    selectedImageUri = data.getData();
                    if (selectedImageUri != null) {
                        try {
                            Bitmap bitmap = MediaStore.Images.Media.getBitmap(getContentResolver(), selectedImageUri);
                            customImageView.setImageBitmap(bitmap);

                            TextView uploadText = findViewById(R.id.upload_label);
                            uploadText.setText(R.string.change_upload);
                        } catch (IOException e) {
                            e.printStackTrace();
                            Toast.makeText(this, "Failed to load image", Toast.LENGTH_SHORT).show();
                        }
                    }
                }
            });

    @SuppressLint("MissingInflatedId")
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_custom_image);

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        customImageView = findViewById(R.id.custom_image_view);

        findViewById(R.id.upload_button).setOnClickListener(v -> openImagePicker());
    }

    private void openImagePicker() {
        Intent intent = new Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI);
        intent.setType("image/*");
        imagePickerLauncher.launch(Intent.createChooser(intent, "Select Picture"));
    }

    public void onGenerateButtonClick(View view) {
        if (selectedImageUri != null) {
            showGameOptionsDialog();
        } else {
            Toast.makeText(this, "Please upload an image first", Toast.LENGTH_SHORT).show();
        }
    }

    private void showGameOptionsDialog() {
        final int[] gridSizes = getResources().getIntArray(R.array.grid_sizes);
        final int[] colorCounts = getResources().getIntArray(R.array.color_counts);

        LayoutInflater inflater = this.getLayoutInflater();
        View dialogView = inflater.inflate(R.layout.dialog_game_options, null);

        final Slider gridSizeSlider = dialogView.findViewById(R.id.grid_size_slider);
        final Slider colorCountSlider = dialogView.findViewById(R.id.color_count_slider);
        final TextView gridSizeValueText = dialogView.findViewById(R.id.grid_size_value_text);
        final TextView colorCountValueText = dialogView.findViewById(R.id.color_count_value_text);

        gridSizeSlider.setValueFrom(0);
        gridSizeSlider.setValueTo(gridSizes.length - 1);
        gridSizeSlider.setStepSize(1);

        colorCountSlider.setValueFrom(0);
        colorCountSlider.setValueTo(colorCounts.length - 1);
        colorCountSlider.setStepSize(1);

        gridSizeSlider.setValue(3);
        gridSizeValueText.setText(String.valueOf(gridSizes[3]));

        colorCountSlider.setValue(3);
        colorCountValueText.setText(String.valueOf(colorCounts[3]));

        gridSizeSlider.addOnChangeListener((slider, value, fromUser) -> {
            int index = (int) value;
            gridSizeValueText.setText(String.valueOf(gridSizes[index]));
        });

        colorCountSlider.addOnChangeListener((slider, value, fromUser) -> {
            int index = (int) value;
            colorCountValueText.setText(String.valueOf(colorCounts[index]));
        });

        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setView(dialogView)
                .setTitle("Game Options")
                .setPositiveButton("Start", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int id) {

                        int gridSize = gridSizes[(int) gridSizeSlider.getValue()];
                        int colorCount = colorCounts[(int) colorCountSlider.getValue()];

                        launchGame(selectedImageUri, gridSize, colorCount);
                    }
                })
                .setNegativeButton("Cancel", (dialog, id) -> dialog.cancel());

        AlertDialog dialog = builder.create();
        dialog.show();
    }

    private void launchGame(Uri selectedImageUri, int gridSize, int colorCount) {
        Intent intent = new Intent(this, GameActivity.class);
        intent.putExtra(DefaultImageActivity.EXTRA_IMAGE_URI, selectedImageUri.toString());
        intent.putExtra(DefaultImageActivity.EXTRA_GRID_SIZE, gridSize);
        intent.putExtra(DefaultImageActivity.EXTRA_COLOR_COUNT, colorCount);
        startActivity(intent);
    }
}