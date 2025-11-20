package com.ungcsci.paintbynumber;

import android.content.DialogInterface;
import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.AdapterView;
import android.widget.GridView;
import android.widget.SeekBar;
import android.widget.TextView;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import java.lang.reflect.Field;
import java.util.ArrayList;
import android.net.Uri;

import com.google.android.material.slider.Slider;

public class DefaultImageActivity extends AppCompatActivity {

    public static final String EXTRA_IMAGE_URI = "com.ungcsci.paintbynumber.image_uri";
    public static final String EXTRA_IMAGE_ID = "com.ungcsci.paintbynumber.image_id";
    public static final String EXTRA_GRID_SIZE = "com.ungcsci.paintbynumber.grid_size";
    public static final String EXTRA_COLOR_COUNT = "com.ungcsci.paintbynumber.color_count";

    private final ArrayList<Integer> mImageIds = new ArrayList<>();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_default_image);

        GridView mGridView = findViewById(R.id.image_grid_view);
        loadImageIds();

        ImageAdapter adapter = new ImageAdapter(this, mImageIds);
        mGridView.setAdapter(adapter);

        mGridView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                int selectedImageId = mImageIds.get(position);
                showGameOptionsDialog(selectedImageId);
            }
        });
    }

    private void showGameOptionsDialog(final int selectedImageId) {
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

                        launchGame(selectedImageId, gridSize, colorCount);
                    }
                })
                .setNegativeButton("Cancel", (dialog, id) -> dialog.cancel());

        AlertDialog dialog = builder.create();
        dialog.show();
    }

    private void launchGame(int imageId, int gridSize, int colorCount) {
        Intent intent = new Intent(this, GameActivity.class);
        intent.putExtra(EXTRA_IMAGE_ID, imageId);
        intent.putExtra(EXTRA_GRID_SIZE, gridSize);
        intent.putExtra(EXTRA_COLOR_COUNT, colorCount);
        startActivity(intent);
    }

    private void loadImageIds() {
        Field[] drawables = R.drawable.class.getFields();
        for (Field field : drawables) {
            try {
                if (java.lang.reflect.Modifier.isStatic(field.getModifiers()) &&
                        field.getType().equals(int.class)) {

                    String name = field.getName();

                    // Only include drawables with your chosen prefix
                    if (name.startsWith("myimg")) {
                        mImageIds.add(field.getInt(null));
                    }
                }
            } catch (IllegalAccessException e) {
                e.printStackTrace();
            }
        }
    }
}